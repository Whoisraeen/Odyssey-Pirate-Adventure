package com.odyssey.world.dungeons;

import org.joml.Vector3i;

/**
 * Represents a trap within a dungeon that can harm or hinder players.
 */
public class DungeonTrap {
    private final String trapId;
    private final Vector3i position;
    private final TrapType type;
    private final int difficulty;
    private boolean triggered = false;
    private boolean disabled = false;
    
    public DungeonTrap(String trapId, Vector3i position, TrapType type, int difficulty) {
        this.trapId = trapId;
        this.position = new Vector3i(position);
        this.type = type;
        this.difficulty = difficulty;
    }
    
    /**
     * Triggers the trap if it hasn't been triggered or disabled.
     * @return true if the trap was successfully triggered
     */
    public boolean trigger() {
        if (triggered || disabled) {
            return false;
        }
        
        triggered = true;
        return true;
    }
    
    /**
     * Disables the trap (e.g., through player skill or tools).
     * @return true if the trap was successfully disabled
     */
    public boolean disable() {
        if (triggered) {
            return false; // Cannot disable an already triggered trap
        }
        
        disabled = true;
        return true;
    }
    
    /**
     * Resets the trap to its initial state.
     */
    public void reset() {
        triggered = false;
        disabled = false;
    }
    
    /**
     * Gets the damage this trap would deal based on its type and difficulty.
     */
    public float getDamage() {
        float baseDamage = type.getBaseDamage();
        float difficultyMultiplier = 1.0f + (difficulty - 1) * 0.5f;
        return baseDamage * difficultyMultiplier;
    }
    
    /**
     * Gets the detection difficulty for this trap.
     */
    public int getDetectionDifficulty() {
        return type.getBaseDetectionDifficulty() + difficulty;
    }
    
    /**
     * Gets the disarm difficulty for this trap.
     */
    public int getDisarmDifficulty() {
        return type.getBaseDisarmDifficulty() + difficulty;
    }
    
    /**
     * Checks if this trap is currently active (not triggered or disabled).
     */
    public boolean isActive() {
        return !triggered && !disabled;
    }
    
    /**
     * Gets the trigger radius for this trap.
     */
    public float getTriggerRadius() {
        return type.getTriggerRadius();
    }
    
    // Getters
    public String getTrapId() { return trapId; }
    public Vector3i getPosition() { return new Vector3i(position); }
    public TrapType getType() { return type; }
    public int getDifficulty() { return difficulty; }
    public boolean isTriggered() { return triggered; }
    public boolean isDisabled() { return disabled; }
    
    @Override
    public String toString() {
        String status = disabled ? "disabled" : (triggered ? "triggered" : "active");
        return String.format("DungeonTrap{id='%s', type=%s, pos=(%d,%d,%d), difficulty=%d, status=%s}", 
                           trapId, type, position.x, position.y, position.z, difficulty, status);
    }
    
    /**
     * Types of traps that can be found in dungeons.
     */
    public enum TrapType {
        PRESSURE_PLATE(3.0f, 2, 3, 1.5f, "A hidden pressure plate that triggers when stepped on"),
        ARROW_TRAP(4.0f, 3, 4, 2.0f, "Shoots arrows from hidden mechanisms in the walls"),
        SPIKE_PIT(6.0f, 2, 5, 1.0f, "A concealed pit filled with sharp spikes"),
        POISON_DART(2.0f, 4, 3, 2.5f, "Fires poisoned darts from tiny holes in the wall"),
        FALLING_BLOCK(5.0f, 1, 2, 1.0f, "Heavy blocks fall from the ceiling when triggered"),
        FIRE_TRAP(4.0f, 3, 4, 2.0f, "Shoots jets of flame from floor or wall vents"),
        ACID_SPRAY(3.0f, 4, 5, 2.0f, "Sprays corrosive acid that damages equipment"),
        TELEPORT_TRAP(0.0f, 5, 6, 1.0f, "Teleports the victim to a random location"),
        CONFUSION_TRAP(1.0f, 4, 4, 3.0f, "Causes disorientation and reversed controls"),
        ALARM_TRAP(0.0f, 2, 3, 5.0f, "Alerts nearby enemies to the player's presence"),
        SLOW_TRAP(1.0f, 3, 3, 2.5f, "Slows movement speed for a duration"),
        BLIND_TRAP(0.0f, 3, 4, 2.0f, "Temporarily blinds the victim with bright light"),
        CURSE_TRAP(2.0f, 5, 6, 1.0f, "Applies a temporary curse with negative effects"),
        EXPLOSIVE_TRAP(8.0f, 2, 5, 3.0f, "Explodes when triggered, damaging a large area"),
        ICE_TRAP(2.0f, 3, 3, 2.0f, "Freezes the victim in place temporarily");
        
        private final float baseDamage;
        private final int baseDetectionDifficulty;
        private final int baseDisarmDifficulty;
        private final float triggerRadius;
        private final String description;
        
        TrapType(float baseDamage, int baseDetectionDifficulty, int baseDisarmDifficulty, 
                float triggerRadius, String description) {
            this.baseDamage = baseDamage;
            this.baseDetectionDifficulty = baseDetectionDifficulty;
            this.baseDisarmDifficulty = baseDisarmDifficulty;
            this.triggerRadius = triggerRadius;
            this.description = description;
        }
        
        public float getBaseDamage() { return baseDamage; }
        public int getBaseDetectionDifficulty() { return baseDetectionDifficulty; }
        public int getBaseDisarmDifficulty() { return baseDisarmDifficulty; }
        public float getTriggerRadius() { return triggerRadius; }
        public String getDescription() { return description; }
        
        /**
         * Checks if this trap type deals damage.
         */
        public boolean isDamaging() {
            return baseDamage > 0;
        }
        
        /**
         * Checks if this trap type has area of effect.
         */
        public boolean isAreaOfEffect() {
            return this == EXPLOSIVE_TRAP || this == FIRE_TRAP || this == ACID_SPRAY;
        }
        
        /**
         * Checks if this trap type has lasting effects.
         */
        public boolean hasLastingEffects() {
            return this == POISON_DART || this == CONFUSION_TRAP || 
                   this == SLOW_TRAP || this == BLIND_TRAP || this == CURSE_TRAP;
        }
    }
}