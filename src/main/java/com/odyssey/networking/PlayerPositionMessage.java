package com.odyssey.networking;

/**
 * Network message for player position and movement updates.
 * Contains player location, rotation, velocity, and movement state.
 */
public class PlayerPositionMessage extends NetworkMessage {
    
    private static final long serialVersionUID = 1L;
    
    /** Player's world position */
    private final double x, y, z;
    
    /** Player's rotation (yaw, pitch, roll) */
    private final float yaw, pitch, roll;
    
    /** Player's velocity */
    private final double velocityX, velocityY, velocityZ;
    
    /** Player's movement state flags */
    private final boolean isWalking, isRunning, isSwimming, isOnShip;
    
    /** ID of the ship the player is on (null if not on ship) */
    private final String shipId;
    
    /**
     * Constructor for PlayerPositionMessage.
     * 
     * @param x Player's X coordinate
     * @param y Player's Y coordinate  
     * @param z Player's Z coordinate
     * @param yaw Player's yaw rotation
     * @param pitch Player's pitch rotation
     * @param roll Player's roll rotation
     * @param velocityX Player's X velocity
     * @param velocityY Player's Y velocity
     * @param velocityZ Player's Z velocity
     * @param isWalking True if player is walking
     * @param isRunning True if player is running
     * @param isSwimming True if player is swimming
     * @param isOnShip True if player is on a ship
     * @param shipId ID of the ship (null if not on ship)
     */
    public PlayerPositionMessage(double x, double y, double z, 
                               float yaw, float pitch, float roll,
                               double velocityX, double velocityY, double velocityZ,
                               boolean isWalking, boolean isRunning, 
                               boolean isSwimming, boolean isOnShip, 
                               String shipId) {
        super(MessageType.PLAYER_POSITION);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.isWalking = isWalking;
        this.isRunning = isRunning;
        this.isSwimming = isSwimming;
        this.isOnShip = isOnShip;
        this.shipId = shipId;
    }
    
    // Position getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    // Rotation getters
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    
    // Velocity getters
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    
    // State getters
    public boolean isWalking() { return isWalking; }
    public boolean isRunning() { return isRunning; }
    public boolean isSwimming() { return isSwimming; }
    public boolean isOnShip() { return isOnShip; }
    public String getShipId() { return shipId; }
    
    /**
     * Calculate the distance from this position to another position.
     * 
     * @param other The other position message
     * @return The distance between positions
     */
    public double distanceTo(PlayerPositionMessage other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Get the speed of the player (magnitude of velocity vector).
     * 
     * @return The player's speed
     */
    public double getSpeed() {
        return Math.sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ);
    }
    
    @Override
    public String toString() {
        return String.format("PlayerPositionMessage{pos=(%.2f,%.2f,%.2f), rot=(%.1f,%.1f,%.1f), " +
                           "vel=(%.2f,%.2f,%.2f), walking=%b, running=%b, swimming=%b, onShip=%b, shipId=%s}", 
                           x, y, z, yaw, pitch, roll, velocityX, velocityY, velocityZ, 
                           isWalking, isRunning, isSwimming, isOnShip, shipId);
    }
}