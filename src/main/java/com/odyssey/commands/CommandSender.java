package com.odyssey.commands;

import org.joml.Vector3f;

/**
 * Represents an entity that can send commands.
 * This could be a player, the server console, or a command block.
 */
public interface CommandSender {
    
    /**
     * Gets the name of this command sender.
     * @return The sender's name
     */
    String getName();
    
    /**
     * Gets the permission level of this command sender.
     * @return Permission level (0 = normal player, higher = more permissions)
     */
    int getPermissionLevel();
    
    /**
     * Sends a message to this command sender.
     * @param message The message to send
     */
    void sendMessage(String message);
    
    /**
     * Sends a formatted message to this command sender.
     * @param format The format string
     * @param args Format arguments
     */
    default void sendMessage(String format, Object... args) {
        sendMessage(String.format(format, args));
    }
    
    /**
     * Gets the world position of this command sender.
     * @return Position vector, or null if sender has no position
     */
    Vector3f getPosition();
    
    /**
     * Checks if this sender has the specified permission level.
     * @param level Required permission level
     * @return true if sender has sufficient permissions
     */
    default boolean hasPermission(int level) {
        return getPermissionLevel() >= level;
    }
    
    /**
     * Checks if this sender is the server console.
     * @return true if this is the console sender
     */
    default boolean isConsole() {
        return false;
    }
    
    /**
     * Checks if this sender is a player.
     * @return true if this is a player sender
     */
    default boolean isPlayer() {
        return false;
    }
}