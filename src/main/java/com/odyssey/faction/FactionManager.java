package com.odyssey.faction;

import com.odyssey.faction.FactionReputation.ReputationAction;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Central manager for all faction-related systems.
 * Handles reputation tracking, faction interactions, consequences, and diplomatic relationships.
 */
public class FactionManager {
    
    private Map<FactionType, FactionReputation> playerReputations;
    private Map<String, FactionEvent> activeEvents;
    private Random random;
    private long lastUpdateTime;
    
    // Event and consequence settings
    private static final long UPDATE_INTERVAL = 60000; // 1 minute
    private static final int MAX_ACTIVE_EVENTS = 5;
    
    public FactionManager() {
        this.playerReputations = new HashMap<>();
        this.activeEvents = new HashMap<>();
        this.random = new Random();
        this.lastUpdateTime = System.currentTimeMillis();
        
        // Initialize reputation for all factions
        for (FactionType faction : FactionType.values()) {
            playerReputations.put(faction, new FactionReputation(faction));
        }
    }
    
    /**
     * Updates all faction systems - call this regularly from the game loop
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL) {
            // Update reputation decay
            for (FactionReputation reputation : playerReputations.values()) {
                reputation.updateDecay();
            }
            
            // Process active events
            processActiveEvents();
            
            // Generate new events
            generateRandomEvents();
            
            lastUpdateTime = currentTime;
        }
    }
    
    /**
     * Adds reputation with a faction
     */
    public void addReputation(FactionType faction, int amount, String reason, 
                             ReputationAction.ActionType actionType) {
        FactionReputation reputation = playerReputations.get(faction);
        if (reputation != null) {
            reputation.addReputation(amount, reason, actionType);
            
            // Apply relationship effects to other factions
            applyRelationshipEffects(faction, amount, reason, actionType);
            
            // Check for immediate consequences
            checkImmediateConsequences(faction, amount, actionType);
        }
    }
    
    /**
     * Applies reputation changes to related factions based on relationships
     */
    private void applyRelationshipEffects(FactionType sourceFaction, int amount, 
                                        String reason, ReputationAction.ActionType actionType) {
        for (FactionType otherFaction : FactionType.values()) {
            if (otherFaction == sourceFaction) continue;
            
            // Get base relationship between factions (simplified approach)
            // For now, use a simple calculation based on faction types
            float relationship = calculateBaseRelationship(sourceFaction, otherFaction);
            
            // Calculate secondary reputation change
            int secondaryChange = (int) (amount * relationship * 0.3f); // 30% of original change
            
            if (Math.abs(secondaryChange) >= 1) {
                FactionReputation otherReputation = playerReputations.get(otherFaction);
                if (otherReputation != null) {
                    String secondaryReason = "Relationship with " + sourceFaction.getDisplayName() + ": " + reason;
                    otherReputation.addReputation(secondaryChange, secondaryReason, actionType);
                }
            }
        }
    }
    
    /**
     * Calculates base relationship between two factions
     */
    private float calculateBaseRelationship(FactionType faction1, FactionType faction2) {
        // Simple relationship calculation - can be expanded later
        // Return 0.0 for neutral, positive for allies, negative for enemies
        return 0.0f;
    }
    
    /**
     * Checks for immediate consequences of reputation changes
     */
    private void checkImmediateConsequences(FactionType faction, int reputationChange, 
                                          ReputationAction.ActionType actionType) {
        FactionReputation reputation = playerReputations.get(faction);
        if (reputation == null) return;
        
        ReputationLevel level = reputation.getLevel();
        
        // Hostile factions may send immediate threats
        if (level == ReputationLevel.HOSTILE && reputationChange < 0) {
            if (random.nextFloat() < 0.3f) { // 30% chance
                createFactionEvent(faction, FactionEvent.EventType.BOUNTY_PLACED,
                    "A bounty has been placed on your head by " + faction.getDisplayName());
            }
        }
        
        // Allied factions may offer immediate help
        if (level == ReputationLevel.ALLIED && reputationChange > 0) {
            if (random.nextFloat() < 0.2f) { // 20% chance
                createFactionEvent(faction, FactionEvent.EventType.ASSISTANCE_OFFERED,
                    faction.getDisplayName() + " offers assistance in your endeavors");
            }
        }
        
        // Special consequences for specific actions
        switch (actionType) {
            case COMBAT:
                if (reputationChange < -10) {
                    createFactionEvent(faction, FactionEvent.EventType.INCREASED_PATROLS,
                        faction.getDisplayName() + " increases patrols in response to your aggression");
                }
                break;
            case CRIME:
                if (reputationChange < -5) {
                    createFactionEvent(faction, FactionEvent.EventType.WANTED_LEVEL,
                        "You are now wanted by " + faction.getDisplayName());
                }
                break;
            case RESCUE:
                if (reputationChange > 10) {
                    createFactionEvent(faction, FactionEvent.EventType.SPECIAL_REWARD,
                        faction.getDisplayName() + " offers a special reward for your heroism");
                }
                break;
        }
    }
    
    /**
     * Creates a new faction event
     */
    private void createFactionEvent(FactionType faction, FactionEvent.EventType type, String description) {
        if (activeEvents.size() >= MAX_ACTIVE_EVENTS) return;
        
        String eventId = faction.name() + "_" + type.name() + "_" + System.currentTimeMillis();
        FactionEvent event = new FactionEvent(eventId, faction, type, description, 
                                            System.currentTimeMillis() + 300000); // 5 minutes duration
        
        activeEvents.put(eventId, event);
        System.out.println("Faction Event: " + description);
    }
    
    /**
     * Processes active faction events
     */
    private void processActiveEvents() {
        List<String> expiredEvents = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, FactionEvent> entry : activeEvents.entrySet()) {
            FactionEvent event = entry.getValue();
            
            if (currentTime >= event.getExpirationTime()) {
                expiredEvents.add(entry.getKey());
            } else {
                // Process ongoing event effects
                processEventEffects(event);
            }
        }
        
        // Remove expired events
        for (String eventId : expiredEvents) {
            activeEvents.remove(eventId);
        }
    }
    
    /**
     * Processes the effects of an active event
     */
    private void processEventEffects(FactionEvent event) {
        // Implementation depends on event type
        switch (event.getType()) {
            case INCREASED_PATROLS:
                // Increase chance of faction encounters
                break;
            case BOUNTY_PLACED:
                // Increase hostility from faction ships
                break;
            case ASSISTANCE_OFFERED:
                // Provide benefits when near faction territory
                break;
            case WANTED_LEVEL:
                // Faction actively hunts the player
                break;
            case SPECIAL_REWARD:
                // Offer unique items or services
                break;
            case TRADE_OPPORTUNITY:
                // Exclusive trading opportunities available
                break;
            case DIPLOMATIC_MISSION:
                // Special diplomatic quest available
                break;
            case FACTION_WAR:
                // War declared between factions
                break;
            case PEACE_TREATY:
                // Peace agreement between factions
                break;
            case TERRITORY_EXPANSION:
                // Faction expands their territory
                break;
            case RESOURCE_SHORTAGE:
                // Faction experiences resource problems
                break;
            case CELEBRATION:
                // Faction celebrates a victory or achievement
                break;
        }
    }
    
    /**
     * Generates random faction events based on current reputation levels
     */
    private void generateRandomEvents() {
        if (random.nextFloat() > 0.1f) return; // 10% chance per update
        
        // Select a random faction for the event
        FactionType[] factions = FactionType.values();
        FactionType faction = factions[random.nextInt(factions.length)];
        FactionReputation reputation = playerReputations.get(faction);
        
        if (reputation == null || !reputation.hasMetFaction()) return;
        
        ReputationLevel level = reputation.getLevel();
        
        // Generate events based on reputation level
        switch (level) {
            case HOSTILE:
                if (random.nextFloat() < 0.4f) {
                    createFactionEvent(faction, FactionEvent.EventType.BOUNTY_PLACED,
                        faction.getDisplayName() + " increases the bounty on your head");
                }
                break;
            case FRIENDLY:
                if (random.nextFloat() < 0.3f) {
                    createFactionEvent(faction, FactionEvent.EventType.TRADE_OPPORTUNITY,
                        faction.getDisplayName() + " offers exclusive trading opportunities");
                }
                break;
            case ALLIED:
                if (random.nextFloat() < 0.2f) {
                    createFactionEvent(faction, FactionEvent.EventType.ASSISTANCE_OFFERED,
                        faction.getDisplayName() + " offers military assistance");
                }
                break;
        }
    }
    
    // Getter methods
    public FactionReputation getReputation(FactionType faction) {
        return playerReputations.get(faction);
    }
    
    public ReputationLevel getReputationLevel(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null ? reputation.getLevel() : ReputationLevel.NEUTRAL;
    }
    
    public int getReputationValue(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null ? reputation.getReputation() : 0;
    }
    
    public Map<FactionType, FactionReputation> getAllReputations() {
        return new HashMap<>(playerReputations);
    }
    
    public List<FactionEvent> getActiveEvents() {
        return new ArrayList<>(activeEvents.values());
    }
    
    /**
     * Checks if the player can trade with a faction
     */
    public boolean canTradeWith(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null && reputation.getLevel().canTrade();
    }
    
    /**
     * Gets the trade price modifier for a faction
     */
    public float getTradeModifier(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null ? reputation.getTradeModifier() : 0.0f;
    }
    
    /**
     * Checks if a faction will attack the player on sight
     */
    public boolean isHostile(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null && reputation.isHostile();
    }
    
    /**
     * Gets the hostility chance for random encounters
     */
    public float getHostilityChance(FactionType faction) {
        FactionReputation reputation = playerReputations.get(faction);
        return reputation != null ? reputation.getHostilityChance() : 0.5f;
    }
    
    /**
     * Gets a summary of all faction reputations
     */
    public String getReputationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Faction Reputations ===\n");
        
        for (FactionType faction : FactionType.values()) {
            FactionReputation reputation = playerReputations.get(faction);
            if (reputation != null && reputation.hasMetFaction()) {
                summary.append(reputation.getReputationSummary()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Handles player actions that affect faction reputation
     */
    public void handlePlayerAction(PlayerAction action) {
        switch (action.getType()) {
            case ATTACK_FACTION_SHIP:
                addReputation(action.getTargetFaction(), -15, 
                    "Attacked " + action.getTargetFaction().getDisplayName() + " vessel",
                    ReputationAction.ActionType.COMBAT);
                break;
            case COMPLETE_FACTION_QUEST:
                addReputation(action.getTargetFaction(), action.getReputationReward(),
                    "Completed quest for " + action.getTargetFaction().getDisplayName(),
                    ReputationAction.ActionType.QUEST);
                break;
            case TRADE_WITH_FACTION:
                addReputation(action.getTargetFaction(), 2,
                    "Traded with " + action.getTargetFaction().getDisplayName(),
                    ReputationAction.ActionType.TRADE);
                break;
            case RESCUE_FACTION_SHIP:
                addReputation(action.getTargetFaction(), 20,
                    "Rescued " + action.getTargetFaction().getDisplayName() + " vessel",
                    ReputationAction.ActionType.RESCUE);
                break;
            case PIRACY_AGAINST_FACTION:
                addReputation(action.getTargetFaction(), -25,
                    "Committed piracy against " + action.getTargetFaction().getDisplayName(),
                    ReputationAction.ActionType.CRIME);
                break;
        }
    }
    
    /**
     * Represents a player action that affects faction reputation
     */
    public static class PlayerAction {
        public enum ActionType {
            ATTACK_FACTION_SHIP, COMPLETE_FACTION_QUEST, TRADE_WITH_FACTION,
            RESCUE_FACTION_SHIP, PIRACY_AGAINST_FACTION, DIPLOMATIC_MISSION
        }
        
        private final ActionType type;
        private final FactionType targetFaction;
        private final int reputationReward;
        private final String description;
        
        public PlayerAction(ActionType type, FactionType targetFaction, int reputationReward, String description) {
            this.type = type;
            this.targetFaction = targetFaction;
            this.reputationReward = reputationReward;
            this.description = description;
        }
        
        // Getters
        public ActionType getType() { return type; }
        public FactionType getTargetFaction() { return targetFaction; }
        public int getReputationReward() { return reputationReward; }
        public String getDescription() { return description; }
    }
}