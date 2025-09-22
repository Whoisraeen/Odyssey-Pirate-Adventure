package com.odyssey.networking;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
            // TODO: Initialize server socket and start listening
            
            LOGGER.info("Server started successfully on port: {}", port);
            
        } catch (Exception e) {
            LOGGER.error("Failed to start server", e);
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
            
            // TODO: Initialize client socket and connect to server
            
            LOGGER.info("Connected to server successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to connect to server", e);
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
            // TODO: Stop server and disconnect all clients
            LOGGER.info("Server stopped");
        }
        
        if (isClient) {
            // TODO: Disconnect from server
            LOGGER.info("Disconnected from server");
        }
        
        isServer = false;
        isClient = false;
    }
    
    /**
     * Send data to connected clients (if server) or to server (if client).
     */
    public void sendData(byte[] data) {
        if (!initialized || (!isServer && !isClient)) return;
        
        LOGGER.debug("Sending {} bytes of data", data.length);
        // TODO: Implement data sending
    }
    
    /**
     * Process incoming network messages.
     */
    public void processMessages() {
        if (!initialized || (!isServer && !isClient)) return;
        
        // TODO: Process incoming network messages
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
}