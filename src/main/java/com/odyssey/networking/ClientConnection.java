package com.odyssey.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a client connection on the server side.
 * Handles communication with a single connected client.
 */
public class ClientConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnection.class);
    
    private final String clientId;
    private final Socket socket;
    private final NetworkManager networkManager;
    private final AtomicBoolean connected = new AtomicBoolean(true);
    
    private ObjectOutputStream output;
    private ObjectInputStream input;
    
    /**
     * Create a new client connection.
     * 
     * @param clientId Unique identifier for this client
     * @param socket The client's socket connection
     * @param networkManager Reference to the network manager
     */
    public ClientConnection(String clientId, Socket socket, NetworkManager networkManager) {
        this.clientId = clientId;
        this.socket = socket;
        this.networkManager = networkManager;
        
        try {
            // Set up input/output streams
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
            
            LOGGER.info("Client connection established: {}", clientId);
        } catch (IOException e) {
            LOGGER.error("Failed to set up client streams for: {}", clientId, e);
            connected.set(false);
        }
    }
    
    /**
     * Handle client communication in a separate thread.
     */
    public void handleClient() {
        LOGGER.info("Starting client handler for: {}", clientId);
        
        while (connected.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Read incoming message from client
                NetworkMessage message = (NetworkMessage) input.readObject();
                
                // Set the sender ID
                message.setSenderId(clientId);
                
                // Forward message to network manager for processing
                networkManager.addIncomingMessage(message);
                
                LOGGER.debug("Received message from client {}: {}", clientId, message.getMessageType());
                
            } catch (IOException | ClassNotFoundException e) {
                if (connected.get()) {
                    LOGGER.warn("Error reading from client {}: {}", clientId, e.getMessage());
                    disconnect();
                }
                break;
            }
        }
        
        LOGGER.info("Client handler stopped for: {}", clientId);
    }
    
    /**
     * Send a message to this client.
     * 
     * @param message The message to send
     * @throws IOException If sending fails
     */
    public void sendMessage(NetworkMessage message) throws IOException {
        if (!connected.get()) {
            throw new IOException("Client is not connected: " + clientId);
        }
        
        try {
            output.writeObject(message);
            output.flush();
            LOGGER.debug("Sent message to client {}: {}", clientId, message.getMessageType());
        } catch (IOException e) {
            LOGGER.error("Failed to send message to client {}: {}", clientId, e.getMessage());
            disconnect();
            throw e;
        }
    }
    
    /**
     * Disconnect this client.
     */
    public void disconnect() {
        if (!connected.compareAndSet(true, false)) {
            return; // Already disconnected
        }
        
        LOGGER.info("Disconnecting client: {}", clientId);
        
        try {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Error closing client connection {}: {}", clientId, e.getMessage());
        }
        
        LOGGER.info("Client disconnected: {}", clientId);
    }
    
    /**
     * Get the client ID.
     * 
     * @return The unique client identifier
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Check if the client is still connected.
     * 
     * @return True if connected, false otherwise
     */
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }
    
    /**
     * Get the client's remote address.
     * 
     * @return The client's address as a string
     */
    public String getRemoteAddress() {
        if (socket != null) {
            return socket.getRemoteSocketAddress().toString();
        }
        return "Unknown";
    }
}