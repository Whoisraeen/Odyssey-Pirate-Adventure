package com.odyssey.faction;

/**
 * Represents a dynamic faction event that affects gameplay.
 * Events can be triggered by player actions, reputation changes, or random occurrences.
 */
public class FactionEvent {
    
    public enum EventType {
        BOUNTY_PLACED("Bounty Placed", "A bounty has been placed on the player"),
        INCREASED_PATROLS("Increased Patrols", "Faction increases patrol activity"),
        ASSISTANCE_OFFERED("Assistance Offered", "Faction offers help to the player"),
        WANTED_LEVEL("Wanted Level", "Player is actively hunted by faction"),
        SPECIAL_REWARD("Special Reward", "Faction offers unique rewards"),
        TRADE_OPPORTUNITY("Trade Opportunity", "Exclusive trading opportunities available"),
        DIPLOMATIC_MISSION("Diplomatic Mission", "Special diplomatic quest available"),
        FACTION_WAR("Faction War", "War declared between factions"),
        PEACE_TREATY("Peace Treaty", "Peace agreement between factions"),
        TERRITORY_EXPANSION("Territory Expansion", "Faction expands their territory"),
        RESOURCE_SHORTAGE("Resource Shortage", "Faction experiences resource problems"),
        CELEBRATION("Celebration", "Faction celebrates a victory or achievement");
        
        private final String displayName;
        private final String description;
        
        EventType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public enum EventSeverity {
        LOW(1.0f, "#00FF00"),      // Green - minor effects
        MEDIUM(2.0f, "#FFFF00"),   // Yellow - moderate effects  
        HIGH(3.0f, "#FF8000"),     // Orange - significant effects
        CRITICAL(5.0f, "#FF0000"); // Red - major effects
        
        private final float multiplier;
        private final String color;
        
        EventSeverity(float multiplier, String color) {
            this.multiplier = multiplier;
            this.color = color;
        }
        
        public float getMultiplier() { return multiplier; }
        public String getColor() { return color; }
    }
    
    private final String eventId;
    private final FactionType faction;
    private final EventType type;
    private final EventSeverity severity;
    private final String description;
    private final long startTime;
    private final long expirationTime;
    private final long duration;
    private boolean isActive;
    private boolean hasBeenProcessed;
    
    /**
     * Creates a new faction event with default severity
     */
    public FactionEvent(String eventId, FactionType faction, EventType type, 
                       String description, long expirationTime) {
        this(eventId, faction, type, EventSeverity.MEDIUM, description, 
             System.currentTimeMillis(), expirationTime);
    }
    
    /**
     * Creates a new faction event with specified parameters
     */
    public FactionEvent(String eventId, FactionType faction, EventType type, 
                       EventSeverity severity, String description, 
                       long startTime, long expirationTime) {
        this.eventId = eventId;
        this.faction = faction;
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.startTime = startTime;
        this.expirationTime = expirationTime;
        this.duration = expirationTime - startTime;
        this.isActive = true;
        this.hasBeenProcessed = false;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public FactionType getFaction() { return faction; }
    public EventType getType() { return type; }
    public EventSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public long getStartTime() { return startTime; }
    public long getExpirationTime() { return expirationTime; }
    public long getDuration() { return duration; }
    public boolean isActive() { return isActive; }
    public boolean hasBeenProcessed() { return hasBeenProcessed; }
    
    /**
     * Checks if the event has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTime;
    }
    
    /**
     * Gets the remaining time for this event in milliseconds
     */
    public long getRemainingTime() {
        return Math.max(0, expirationTime - System.currentTimeMillis());
    }
    
    /**
     * Gets the progress of this event (0.0 to 1.0)
     */
    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, (float) elapsed / duration);
    }
    
    /**
     * Marks the event as processed
     */
    public void markAsProcessed() {
        this.hasBeenProcessed = true;
    }
    
    /**
     * Deactivates the event
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * Gets the effect multiplier based on severity and progress
     */
    public float getEffectMultiplier() {
        float baseMultiplier = severity.getMultiplier();
        float progressFactor = 1.0f - (getProgress() * 0.3f); // Diminishing effect over time
        return baseMultiplier * progressFactor;
    }
    
    /**
     * Gets specific effects this event has on gameplay
     */
    public EventEffects getEffects() {
        EventEffects effects = new EventEffects();
        float multiplier = getEffectMultiplier();
        
        switch (type) {
            case BOUNTY_PLACED:
                effects.hostilityIncrease = 0.5f * multiplier;
                effects.encounterChanceIncrease = 0.3f * multiplier;
                effects.reputationDecayRate = 1.5f * multiplier;
                break;
                
            case INCREASED_PATROLS:
                effects.encounterChanceIncrease = 0.4f * multiplier;
                effects.detectionRangeIncrease = 0.2f * multiplier;
                break;
                
            case ASSISTANCE_OFFERED:
                effects.tradeBonus = 0.2f * multiplier;
                effects.serviceDiscount = 0.15f * multiplier;
                effects.questRewardBonus = 0.25f * multiplier;
                break;
                
            case WANTED_LEVEL:
                effects.hostilityIncrease = 0.8f * multiplier;
                effects.encounterChanceIncrease = 0.6f * multiplier;
                effects.tradePenalty = 0.3f * multiplier;
                break;
                
            case SPECIAL_REWARD:
                effects.questRewardBonus = 0.5f * multiplier;
                effects.uniqueItemChance = 0.2f * multiplier;
                break;
                
            case TRADE_OPPORTUNITY:
                effects.tradeBonus = 0.3f * multiplier;
                effects.rareCommodityChance = 0.25f * multiplier;
                break;
                
            case DIPLOMATIC_MISSION:
                effects.questRewardBonus = 0.4f * multiplier;
                effects.reputationGainBonus = 0.3f * multiplier;
                break;
                
            case FACTION_WAR:
                effects.hostilityIncrease = 0.6f * multiplier;
                effects.encounterChanceIncrease = 0.5f * multiplier;
                effects.tradePenalty = 0.4f * multiplier;
                break;
                
            case PEACE_TREATY:
                effects.hostilityDecrease = 0.4f * multiplier;
                effects.tradeBonus = 0.15f * multiplier;
                effects.reputationGainBonus = 0.2f * multiplier;
                break;
                
            case TERRITORY_EXPANSION:
                effects.encounterChanceIncrease = 0.2f * multiplier;
                effects.questAvailabilityIncrease = 0.3f * multiplier;
                break;
                
            case RESOURCE_SHORTAGE:
                effects.tradePenalty = 0.25f * multiplier;
                effects.questRewardBonus = 0.2f * multiplier; // Higher rewards for supply missions
                break;
                
            case CELEBRATION:
                effects.tradeBonus = 0.1f * multiplier;
                effects.questRewardBonus = 0.15f * multiplier;
                effects.reputationGainBonus = 0.2f * multiplier;
                break;
        }
        
        return effects;
    }
    
    /**
     * Gets a formatted display string for the event
     */
    public String getDisplayString() {
        StringBuilder display = new StringBuilder();
        display.append("[").append(severity.name()).append("] ");
        display.append(faction.getDisplayName()).append(": ");
        display.append(type.getDisplayName());
        
        long remainingMinutes = getRemainingTime() / 60000;
        if (remainingMinutes > 0) {
            display.append(" (").append(remainingMinutes).append("m remaining)");
        }
        
        return display.toString();
    }
    
    /**
     * Gets detailed information about the event
     */
    public String getDetailedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Event: ").append(type.getDisplayName()).append("\n");
        info.append("Faction: ").append(faction.getDisplayName()).append("\n");
        info.append("Severity: ").append(severity.name()).append("\n");
        info.append("Description: ").append(description).append("\n");
        info.append("Progress: ").append(String.format("%.1f%%", getProgress() * 100)).append("\n");
        
        long remainingMinutes = getRemainingTime() / 60000;
        info.append("Time Remaining: ").append(remainingMinutes).append(" minutes\n");
        
        EventEffects effects = getEffects();
        info.append("Effects:\n").append(effects.toString());
        
        return info.toString();
    }
    
    @Override
    public String toString() {
        return getDisplayString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FactionEvent that = (FactionEvent) obj;
        return eventId.equals(that.eventId);
    }
    
    @Override
    public int hashCode() {
        return eventId.hashCode();
    }
    
    /**
     * Represents the gameplay effects of a faction event
     */
    public static class EventEffects {
        public float hostilityIncrease = 0.0f;
        public float hostilityDecrease = 0.0f;
        public float encounterChanceIncrease = 0.0f;
        public float detectionRangeIncrease = 0.0f;
        public float tradeBonus = 0.0f;
        public float tradePenalty = 0.0f;
        public float serviceDiscount = 0.0f;
        public float questRewardBonus = 0.0f;
        public float reputationGainBonus = 0.0f;
        public float reputationDecayRate = 1.0f;
        public float uniqueItemChance = 0.0f;
        public float rareCommodityChance = 0.0f;
        public float questAvailabilityIncrease = 0.0f;
        
        @Override
        public String toString() {
            StringBuilder effects = new StringBuilder();
            
            if (hostilityIncrease > 0) effects.append("- Hostility +").append(String.format("%.1f%%", hostilityIncrease * 100)).append("\n");
            if (hostilityDecrease > 0) effects.append("- Hostility -").append(String.format("%.1f%%", hostilityDecrease * 100)).append("\n");
            if (encounterChanceIncrease > 0) effects.append("- Encounter Rate +").append(String.format("%.1f%%", encounterChanceIncrease * 100)).append("\n");
            if (tradeBonus > 0) effects.append("- Trade Bonus +").append(String.format("%.1f%%", tradeBonus * 100)).append("\n");
            if (tradePenalty > 0) effects.append("- Trade Penalty -").append(String.format("%.1f%%", tradePenalty * 100)).append("\n");
            if (questRewardBonus > 0) effects.append("- Quest Rewards +").append(String.format("%.1f%%", questRewardBonus * 100)).append("\n");
            if (reputationGainBonus > 0) effects.append("- Reputation Gain +").append(String.format("%.1f%%", reputationGainBonus * 100)).append("\n");
            
            return effects.length() > 0 ? effects.toString() : "No active effects\n";
        }
    }
}