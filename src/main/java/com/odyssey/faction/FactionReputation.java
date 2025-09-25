package com.odyssey.faction;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages reputation data for a single faction.
 * Tracks reputation value, level, recent actions, and provides methods for reputation changes.
 */
public class FactionReputation {
    
    private final FactionType faction;
    private int reputation;
    private ReputationLevel level;
    private List<ReputationAction> recentActions;
    private long lastInteraction;
    private boolean hasMetFaction;
    
    // Reputation decay settings
    private static final int REPUTATION_DECAY_INTERVAL = 86400000; // 24 hours in milliseconds
    private static final int REPUTATION_DECAY_AMOUNT = 1;
    private static final int MAX_RECENT_ACTIONS = 10;
    
    /**
     * Creates a new faction reputation tracker
     */
    public FactionReputation(FactionType faction) {
        this.faction = faction;
        this.reputation = 0; // Start neutral
        this.level = ReputationLevel.NEUTRAL;
        this.recentActions = new ArrayList<>();
        this.lastInteraction = System.currentTimeMillis();
        this.hasMetFaction = false;
    }
    
    /**
     * Creates a faction reputation with initial values
     */
    public FactionReputation(FactionType faction, int initialReputation) {
        this.faction = faction;
        this.reputation = initialReputation;
        this.level = ReputationLevel.fromReputation(reputation, faction);
        this.recentActions = new ArrayList<>();
        this.lastInteraction = System.currentTimeMillis();
        this.hasMetFaction = initialReputation != 0;
    }
    
    // Getters
    public FactionType getFaction() { return faction; }
    public int getReputation() { return reputation; }
    public ReputationLevel getLevel() { return level; }
    public List<ReputationAction> getRecentActions() { return new ArrayList<>(recentActions); }
    public long getLastInteraction() { return lastInteraction; }
    public boolean hasMetFaction() { return hasMetFaction; }
    
    /**
     * Adds reputation change and updates level
     */
    public void addReputation(int amount, String reason, ReputationAction.ActionType actionType) {
        if (amount == 0) return;
        
        int oldReputation = reputation;
        ReputationLevel oldLevel = level;
        
        // Apply reputation change
        reputation += amount;
        
        // Clamp reputation to reasonable bounds
        reputation = Math.max(-1000, Math.min(1000, reputation));
        
        // Update level
        level = ReputationLevel.fromReputation(reputation, faction);
        
        // Record the action
        ReputationAction action = new ReputationAction(
            actionType, amount, reason, System.currentTimeMillis()
        );
        addRecentAction(action);
        
        // Update interaction time
        lastInteraction = System.currentTimeMillis();
        hasMetFaction = true;
        
        // Log significant changes
        if (oldLevel != level) {
            System.out.println("Reputation with " + faction.getDisplayName() + 
                             " changed from " + oldLevel.getDisplayName() + 
                             " to " + level.getDisplayName());
        }
    }
    
    /**
     * Adds a recent action to the history
     */
    private void addRecentAction(ReputationAction action) {
        recentActions.add(0, action); // Add to beginning
        
        // Keep only recent actions
        if (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions = recentActions.subList(0, MAX_RECENT_ACTIONS);
        }
    }
    
    /**
     * Updates reputation based on time decay
     */
    public void updateDecay() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastInteraction = currentTime - lastInteraction;
        
        // Apply reputation decay if enough time has passed
        if (timeSinceLastInteraction >= REPUTATION_DECAY_INTERVAL) {
            int decayIntervals = (int) (timeSinceLastInteraction / REPUTATION_DECAY_INTERVAL);
            int totalDecay = decayIntervals * REPUTATION_DECAY_AMOUNT;
            
            // Decay towards neutral (0)
            if (reputation > 0) {
                reputation = Math.max(0, reputation - totalDecay);
            } else if (reputation < 0) {
                reputation = Math.min(0, reputation + totalDecay);
            }
            
            // Update level after decay
            ReputationLevel oldLevel = level;
            level = ReputationLevel.fromReputation(reputation, faction);
            
            if (oldLevel != level) {
                addRecentAction(new ReputationAction(
                    ReputationAction.ActionType.DECAY,
                    reputation > 0 ? -totalDecay : totalDecay,
                    "Reputation decay over time",
                    currentTime
                ));
            }
            
            lastInteraction = currentTime;
        }
    }
    
    /**
     * Gets the reputation needed to reach the next level
     */
    public int getReputationToNextLevel() {
        return level.getReputationToNextLevel(reputation, faction);
    }
    
    /**
     * Gets the reputation needed to avoid dropping to the previous level
     */
    public int getReputationToPreviousLevel() {
        switch (level) {
            case REVERED:
                return reputation - faction.getReputationThreshold(ReputationLevel.ALLIED);
            case ALLIED:
                return reputation - faction.getReputationThreshold(ReputationLevel.FRIENDLY);
            case FRIENDLY:
                return reputation - faction.getReputationThreshold(ReputationLevel.NEUTRAL);
            case NEUTRAL:
                return reputation - faction.getReputationThreshold(ReputationLevel.UNFRIENDLY);
            case UNFRIENDLY:
                return reputation - faction.getReputationThreshold(ReputationLevel.HOSTILE);
            case HOSTILE:
                return Integer.MAX_VALUE; // Can't go lower
            default:
                return 0;
        }
    }
    
    /**
     * Checks if the player can perform a specific action with this faction
     */
    public boolean canPerformAction(String action) {
        return level.hasAccess(action);
    }
    
    /**
     * Gets the trade price modifier for this faction
     */
    public float getTradeModifier() {
        return level.getTradeBonus();
    }
    
    /**
     * Gets the service cost modifier for this faction
     */
    public float getServiceCostModifier() {
        return level.getServiceCostMultiplier();
    }
    
    /**
     * Gets the quest reward modifier for this faction
     */
    public float getQuestRewardModifier() {
        return level.getQuestRewardMultiplier();
    }
    
    /**
     * Checks if this faction will attack the player on sight
     */
    public boolean isHostile() {
        return level.isAttackOnSight();
    }
    
    /**
     * Gets the chance of hostile encounters with this faction
     */
    public float getHostilityChance() {
        return level.getHostilityChance();
    }
    
    /**
     * Gets a summary of the current reputation status
     */
    public String getReputationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(faction.getDisplayName()).append(": ");
        summary.append(level.getDisplayName()).append(" (").append(reputation).append(")");
        
        if (level != ReputationLevel.REVERED) {
            int toNext = getReputationToNextLevel();
            summary.append(" - ").append(toNext).append(" to next level");
        }
        
        return summary.toString();
    }
    
    /**
     * Gets detailed reputation information including recent actions
     */
    public String getDetailedStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== ").append(faction.getDisplayName()).append(" Reputation ===\n");
        status.append("Level: ").append(level.getDisplayName()).append(" (").append(reputation).append(")\n");
        status.append("Description: ").append(level.getDescription()).append("\n");
        
        if (level != ReputationLevel.REVERED) {
            status.append("To Next Level: ").append(getReputationToNextLevel()).append("\n");
        }
        
        status.append("Available Services: ");
        String[] services = level.getAvailableServices();
        if (services.length > 0) {
            status.append(String.join(", ", services));
        } else {
            status.append("None");
        }
        status.append("\n");
        
        if (!recentActions.isEmpty()) {
            status.append("\nRecent Actions:\n");
            for (int i = 0; i < Math.min(5, recentActions.size()); i++) {
                ReputationAction action = recentActions.get(i);
                status.append("- ").append(action.toString()).append("\n");
            }
        }
        
        return status.toString();
    }
    
    /**
     * Represents a reputation-affecting action
     */
    public static class ReputationAction {
        public enum ActionType {
            TRADE, QUEST, COMBAT, DIPLOMACY, CRIME, RESCUE, DECAY, OTHER
        }
        
        private final ActionType type;
        private final int reputationChange;
        private final String description;
        private final long timestamp;
        
        public ReputationAction(ActionType type, int reputationChange, String description, long timestamp) {
            this.type = type;
            this.reputationChange = reputationChange;
            this.description = description;
            this.timestamp = timestamp;
        }
        
        // Getters
        public ActionType getType() { return type; }
        public int getReputationChange() { return reputationChange; }
        public String getDescription() { return description; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            String sign = reputationChange >= 0 ? "+" : "";
            return type.name() + ": " + sign + reputationChange + " - " + description;
        }
    }
}