package com.odyssey.commands;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command sender implementation for the server console.
 * The console has maximum permissions and no position.
 */
public class ConsoleSender implements CommandSender {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleSender.class);
    
    private static final ConsoleSender INSTANCE = new ConsoleSender();
    
    private ConsoleSender() {
        // Singleton
    }
    
    public static ConsoleSender getInstance() {
        return INSTANCE;
    }
    
    @Override
    public String getName() {
        return "CONSOLE";
    }
    
    @Override
    public int getPermissionLevel() {
        return Integer.MAX_VALUE; // Console has all permissions
    }
    
    @Override
    public void sendMessage(String message) {
        logger.info("[CONSOLE] {}", message);
    }
    
    @Override
    public Vector3f getPosition() {
        return null; // Console has no position
    }
    
    @Override
    public boolean isConsole() {
        return true;
    }
    
    @Override
    public boolean isPlayer() {
        return false;
    }
}