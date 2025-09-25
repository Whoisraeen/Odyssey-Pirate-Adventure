package com.odyssey.networking;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Network manager for The Odyssey.
 * Handles multiplayer networking, server connections, and data synchronization.
 */
public class NetworkManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkManager.class);
    
    private final GameConfig config;
    private boolean initialized = false;
    private boolean isServer = false;
    private boolean isClient = false;
    private String serverAddress;
    private int serverPort;
    
    // Server components
    private ServerSocket serverSocket;
    private final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private ExecutorService serverExecutor;
    private final Map<String, ClientConnection> connectedClients = new ConcurrentHashMap<>();
    
    // Client components
    private Socket clientSocket;
    private ObjectOutputStream clientOutput;
    private ObjectInputStream clientInput;
    private final AtomicBoolean clientConnected = new AtomicBoolean(false);
    private ExecutorService clientExecutor;
    
    // Message processing
    private final BlockingQueue<NetworkMessage> incomingMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<NetworkMessage> outgoingMessages = new LinkedBlockingQueue<>();
    
    public NetworkManager(GameConfig config) {
        this.config = config;
    }
    
    /**
     * Initialize the network manager.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("NetworkManager is already initialized");
            return;
        }
        
        LOGGER.info("Initializing NetworkManager...");
        
        try {
            // Load network settings from config
            serverAddress = config.getString("network.serverAddress", "localhost");
            serverPort = config.getInt("network.serverPort", 25565);
            
            initialized = true;
            LOGGER.info("NetworkManager initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize NetworkManager", e);
            cleanup();
            throw new RuntimeException("NetworkManager initialization failed", e);
        }
    }
    
    /**
     * Start as a server.
     */
    public void startServer(int port) {
        if (!initialized) return;
        
        LOGGER.info("Starting server on port: {}", port);
        
        try {
            this.serverPort = port;
            this.isServer = true;
            
            // Initialize server socket and start listening
            serverSocket = new ServerSocket(port);
            serverRunning.set(true);
            
            // Create thread pool for handling client connections
            serverExecutor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "NetworkServer-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
            
            // Start accepting client connections
            serverExecutor.submit(this::acceptClientConnections);
            
            // Start message processing thread
            serverExecutor.submit(this::processOutgoingMessages);
            
            LOGGER.info("Server started successfully on port: {}", port);
            
        } catch (Exception e) {
            LOGGER.error("Failed to start server", e);
            cleanup();
            throw new RuntimeException("Server startup failed", e);
        }
    }
    
    /**
     * Connect to a server as a client.
     */
    public void connectToServer(String address, int port) {
        if (!initialized) return;
        
        LOGGER.info("Connecting to server: {}:{}", address, port);
        
        try {
            this.serverAddress = address;
            this.serverPort = port;
            this.isClient = true;
            
            // Initialize client socket and connect to server
            clientSocket = new Socket(address, port);
            clientConnected.set(true);
            
            // Set up input/output streams
            clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
            clientInput = new ObjectInputStream(clientSocket.getInputStream());
            
            // Create thread pool for client operations
            clientExecutor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "NetworkClient-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
            
            // Start message processing threads
            clientExecutor.submit(this::processIncomingClientMessages);
            clientExecutor.submit(this::processOutgoingMessages);
            
            LOGGER.info("Connected to server successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to connect to server", e);
            cleanup();
            throw new RuntimeException("Server connection failed", e);
        }
    }
    
    /**
     * Disconnect from server or stop server.
     */
    public void disconnect() {
        if (!initialized) return;
        
        LOGGER.info("Disconnecting from network...");
        
        if (isServer) {
            // Stop server and disconnect all clients
            serverRunning.set(false);
            
            // Disconnect all clients
            for (ClientConnection client : connectedClients.values()) {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    LOGGER.warn("Error disconnecting client: {}", client.getClientId(), e);
                }
            }
            connectedClients.clear();
            
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing server socket", e);
                }
            }
            
            // Shutdown server executor
            if (serverExecutor != null) {
                serverExecutor.shutdown();
                try {
                    if (!serverExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        serverExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    serverExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            LOGGER.info("Server stopped");
        }
        
        if (isClient) {
            // Disconnect from server
            clientConnected.set(false);
            
            // Close client streams and socket
            try {
                if (clientOutput != null) clientOutput.close();
                if (clientInput != null) clientInput.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Error closing client connection", e);
            }
            
            // Shutdown client executor
            if (clientExecutor != null) {
                clientExecutor.shutdown();
                try {
                    if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        clientExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    clientExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            LOGGER.info("Disconnected from server");
        }
        
        // Clear message queues
        incomingMessages.clear();
        outgoingMessages.clear();
        
        isServer = false;
        isClient = false;
    }
    
    /**
     * Send data over the network.
     */
    public void sendData(NetworkMessage message) {
        if (!initialized || message == null) return;
        
        try {
            // Add message to outgoing queue for processing
            outgoingMessages.offer(message);
            LOGGER.debug("Queued message for sending: {}", message.getMessageType());
        } catch (Exception e) {
            LOGGER.error("Failed to queue message for sending", e);
        }
    }
    
    /**
     * Process incoming network messages.
     */
    public void processMessages() {
        if (!initialized || (!isServer && !isClient)) return;
        
        // Process all available incoming messages
        NetworkMessage message;
        while ((message = incomingMessages.poll()) != null) {
            try {
                handleIncomingMessage(message);
            } catch (Exception e) {
                LOGGER.error("Error processing incoming message: {}", message.getMessageType(), e);
            }
        }
    }
    
    /**
     * Check if currently acting as a server.
     */
    public boolean isServer() {
        return isServer;
    }
    
    /**
     * Check if currently connected as a client.
     */
    public boolean isClient() {
        return isClient;
    }
    
    /**
     * Check if connected to network (either as server or client).
     */
    public boolean isConnected() {
        return isServer || isClient;
    }
    
    /**
     * Get server address.
     */
    public String getServerAddress() {
        return serverAddress;
    }
    
    /**
     * Get server port.
     */
    public int getServerPort() {
        return serverPort;
    }
    
    /**
     * Update the network manager (called each frame).
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        if (!initialized) return;
        
        processMessages();
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up NetworkManager resources...");
        
        disconnect();
        
        initialized = false;
        LOGGER.info("NetworkManager cleanup complete");
    }
    
    // Private helper methods
    
    /**
     * Accept incoming client connections (server mode).
     */
    private void acceptClientConnections() {
        LOGGER.info("Server listening for client connections");
        
        while (serverRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = clientSocket.getRemoteSocketAddress().toString();
                
                LOGGER.info("New client connection from: {}", clientId);
                
                ClientConnection clientConnection = new ClientConnection(clientId, clientSocket, this);
                connectedClients.put(clientId, clientConnection);
                
                // Start handling this client in a separate thread
                serverExecutor.submit(clientConnection::handleClient);
                
            } catch (IOException e) {
                if (serverRunning.get()) {
                    LOGGER.error("Error accepting client connection", e);
                }
            }
        }
        
        LOGGER.info("Server stopped accepting connections");
    }
    
    /**
     * Process outgoing messages (both server and client mode).
     */
    private void processOutgoingMessages() {
        while ((isServer && serverRunning.get()) || (isClient && clientConnected.get())) {
            try {
                NetworkMessage message = outgoingMessages.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    if (isServer) {
                        sendMessageToClients(message);
                    } else if (isClient) {
                        sendMessageToServer(message);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("Error processing outgoing message", e);
            }
        }
    }
    
    /**
     * Process incoming messages from server (client mode).
     */
    private void processIncomingClientMessages() {
        while (clientConnected.get() && !Thread.currentThread().isInterrupted()) {
            try {
                NetworkMessage message = (NetworkMessage) clientInput.readObject();
                incomingMessages.offer(message);
                LOGGER.debug("Received message from server: {}", message.getMessageType());
            } catch (IOException | ClassNotFoundException e) {
                if (clientConnected.get()) {
                    LOGGER.error("Error reading message from server", e);
                    clientConnected.set(false);
                }
                break;
            }
        }
    }
    
    /**
     * Send message to all connected clients (server mode).
     */
    private void sendMessageToClients(NetworkMessage message) {
        for (ClientConnection client : connectedClients.values()) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                LOGGER.warn("Failed to send message to client: {}", client.getClientId(), e);
            }
        }
    }
    
    /**
     * Send message to server (client mode).
     */
    private void sendMessageToServer(NetworkMessage message) {
        try {
            clientOutput.writeObject(message);
            clientOutput.flush();
            LOGGER.debug("Sent message to server: {}", message.getMessageType());
        } catch (IOException e) {
            LOGGER.error("Failed to send message to server", e);
            clientConnected.set(false);
        }
    }
    
    /**
     * Handle incoming message based on its type.
     */
    private void handleIncomingMessage(NetworkMessage message) {
        LOGGER.debug("Handling incoming message: {}", message.getMessageType());
        
        switch (message.getMessageType()) {
            case JOIN_REQUEST:
                if (isServer) {
                    handleJoinRequest((JoinRequestMessage) message);
                }
                break;
            case JOIN_RESPONSE:
                if (isClient) {
                    handleJoinResponse((JoinResponseMessage) message);
                }
                break;
            case PLAYER_POSITION:
                handlePlayerPosition((PlayerPositionMessage) message);
                break;
            case DISCONNECT:
                handleDisconnect(message);
                break;
            default:
                LOGGER.warn("Unhandled message type: {}", message.getMessageType());
                break;
        }
    }
    
    /**
     * Handle join request (server mode).
     */
    private void handleJoinRequest(JoinRequestMessage request) {
        LOGGER.info("Processing join request from player: {}", request.getPlayerName());
        
        // Create response message (accepted)
        JoinResponseMessage response = new JoinResponseMessage(
            "The Odyssey Server",    // serverName
            "1.0.0",                 // serverVersion
            100,                     // maxPlayers
            connectedClients.size(), // currentPlayers
            "Welcome to The Odyssey!", // motd
            request.getPlayerId()    // assignedPlayerId
        );
        
        sendData(response);
    }
    
    /**
     * Handle join response (client mode).
     */
    private void handleJoinResponse(JoinResponseMessage response) {
        if (response.isAccepted()) {
            LOGGER.info("Successfully joined server: {}", response.getServerName());
        } else {
            LOGGER.warn("Join request rejected: {}", response.getRejectionReason());
        }
    }
    
    /**
     * Handle player position update.
     */
    private void handlePlayerPosition(PlayerPositionMessage position) {
        LOGGER.debug("Player position update: {} at ({}, {}, {})", 
            position.getSenderId(), 
            position.getX(), 
            position.getY(), 
            position.getZ());
        
        // Forward to game engine for processing
        // This would typically update the player's position in the game world
    }
    
    /**
     * Handle disconnect message.
     */
    private void handleDisconnect(NetworkMessage message) {
        LOGGER.info("Received disconnect message from: {}", message.getSenderId());
        
        if (isServer) {
            // Remove client from connected clients
            connectedClients.remove(message.getSenderId());
        }
    }
    
    /**
     * Add incoming message to queue (called by ClientConnection).
     */
    public void addIncomingMessage(NetworkMessage message) {
        incomingMessages.offer(message);
    }
}