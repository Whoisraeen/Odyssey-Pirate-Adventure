package com.odyssey.networking;

import java.io.Serializable;
import java.util.UUID;

/**
 * Base class for all network messages in The Odyssey.
 * Provides common functionality for message identification, timestamps, and serialization.
 */
public abstract class NetworkMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** Unique identifier for this message */
    private final UUID messageId;
    
    /** Timestamp when the message was created */
    private final long timestamp;
    
    /** Type of the message */
    private final MessageType messageType;
    
    /** ID of the sender (player/server) */
    private String senderId;
    
    /**
     * Constructor for NetworkMessage.
     * 
     * @param messageType The type of this message
     */
    protected NetworkMessage(MessageType messageType) {
        this.messageId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.messageType = messageType;
    }
    
    /**
     * Get the unique message ID.
     * 
     * @return The message ID
     */
    public UUID getMessageId() {
        return messageId;
    }
    
    /**
     * Get the timestamp when this message was created.
     * 
     * @return The timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the type of this message.
     * 
     * @return The message type
     */
    public MessageType getMessageType() {
        return messageType;
    }
    
    /**
     * Get the sender ID.
     * 
     * @return The sender ID
     */
    public String getSenderId() {
        return senderId;
    }
    
    /**
     * Set the sender ID.
     * 
     * @param senderId The sender ID
     */
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    /**
     * Get the age of this message in milliseconds.
     * 
     * @return The age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Check if this message has expired based on the given timeout.
     * 
     * @param timeoutMs The timeout in milliseconds
     * @return True if the message has expired
     */
    public boolean isExpired(long timeoutMs) {
        return getAge() > timeoutMs;
    }
    
    @Override
    public String toString() {
        return String.format("%s{id=%s, type=%s, sender=%s, timestamp=%d}", 
                getClass().getSimpleName(), messageId, messageType, senderId, timestamp);
    }
}