package com.odyssey.events;

import com.odyssey.crafting.Recipe;
import com.odyssey.items.ItemStack;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all crafting-related events.
 * Provides common functionality for crafting event handling.
 */
public abstract class CraftingEvent extends Event {
    protected final UUID playerId;
    protected final Recipe recipe;
    
    protected CraftingEvent(UUID playerId, Recipe recipe) {
        this.playerId = playerId;
        this.recipe = recipe;
    }
    
    /**
     * Gets the player ID associated with this crafting event
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the recipe associated with this crafting event
     */
    public Recipe getRecipe() {
        return recipe;
    }
    
    /**
     * Event fired when a player starts crafting
     */
    public static class Started extends CraftingEvent {
        private final UUID sessionId;
        
        public Started(UUID playerId, Recipe recipe, UUID sessionId) {
            super(playerId, recipe);
            this.sessionId = sessionId;
        }
        
        public UUID getSessionId() {
            return sessionId;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.Started{player=%s, recipe=%s, session=%s}", 
                playerId, recipe.getId(), sessionId);
        }
    }
    
    /**
     * Event fired before crafting begins (can be cancelled)
     */
    public static class PreCraft extends CraftingEvent implements Cancellable {
        private boolean cancelled = false;
        private String cancelReason;
        
        public PreCraft(UUID playerId, Recipe recipe) {
            super(playerId, recipe);
        }
        
        @Override
        public boolean isCancelled() {
            return cancelled;
        }
        
        @Override
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
        
        public void setCancelled(boolean cancelled, String reason) {
            this.cancelled = cancelled;
            this.cancelReason = reason;
        }
        
        public String getCancelReason() {
            return cancelReason;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.PreCraft{player=%s, recipe=%s, cancelled=%s}", 
                playerId, recipe.getId(), cancelled);
        }
    }
    
    /**
     * Event fired when crafting is completed successfully
     */
    public static class Completed extends CraftingEvent {
        private final List<ItemStack> results;
        private final int experienceGained;
        
        public Completed(UUID playerId, Recipe recipe, List<ItemStack> results, int experienceGained) {
            super(playerId, recipe);
            this.results = results;
            this.experienceGained = experienceGained;
        }
        
        public List<ItemStack> getResults() {
            return results;
        }
        
        public int getExperienceGained() {
            return experienceGained;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.Completed{player=%s, recipe=%s, results=%d, exp=%d}", 
                playerId, recipe.getId(), results.size(), experienceGained);
        }
    }
    
    /**
     * Event fired when crafting fails
     */
    public static class Failed extends CraftingEvent {
        private final String reason;
        
        public Failed(UUID playerId, Recipe recipe, String reason) {
            super(playerId, recipe);
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.Failed{player=%s, recipe=%s, reason='%s'}", 
                playerId, recipe.getId(), reason);
        }
    }
    
    /**
     * Event fired when crafting is cancelled
     */
    public static class Cancelled extends CraftingEvent {
        private final UUID sessionId;
        
        public Cancelled(UUID playerId, Recipe recipe, UUID sessionId) {
            super(playerId, recipe);
            this.sessionId = sessionId;
        }
        
        public UUID getSessionId() {
            return sessionId;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.Cancelled{player=%s, recipe=%s, session=%s}", 
                playerId, recipe.getId(), sessionId);
        }
    }
    
    /**
     * Event fired when crafting progress updates
     */
    public static class ProgressUpdate extends CraftingEvent {
        private final UUID sessionId;
        private final int progress;
        private final int totalTime;
        
        public ProgressUpdate(UUID playerId, Recipe recipe, UUID sessionId, int progress, int totalTime) {
            super(playerId, recipe);
            this.sessionId = sessionId;
            this.progress = progress;
            this.totalTime = totalTime;
        }
        
        public UUID getSessionId() {
            return sessionId;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public int getTotalTime() {
            return totalTime;
        }
        
        public float getProgressPercentage() {
            return (float) progress / totalTime * 100f;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.ProgressUpdate{player=%s, recipe=%s, progress=%d/%d}", 
                playerId, recipe.getId(), progress, totalTime);
        }
    }
    
    /**
     * Event fired when there are overflow items that couldn't fit in inventory
     */
    public static class Overflow extends Event {
        private final UUID playerId;
        private final List<ItemStack> overflowItems;
        
        public Overflow(UUID playerId, List<ItemStack> overflowItems) {
            this.playerId = playerId;
            this.overflowItems = overflowItems;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public List<ItemStack> getOverflowItems() {
            return overflowItems;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.Overflow{player=%s, items=%d}", 
                playerId, overflowItems.size());
        }
    }
    
    /**
     * Event fired when a recipe is learned
     */
    public static class RecipeLearned extends Event {
        private final UUID playerId;
        private final Recipe recipe;
        private final String source;
        
        public RecipeLearned(UUID playerId, Recipe recipe, String source) {
            this.playerId = playerId;
            this.recipe = recipe;
            this.source = source;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public Recipe getRecipe() {
            return recipe;
        }
        
        public String getSource() {
            return source;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.RecipeLearned{player=%s, recipe=%s, source='%s'}", 
                playerId, recipe.getId(), source);
        }
    }
    
    /**
     * Event fired when a recipe is forgotten
     */
    public static class RecipeForgotten extends Event {
        private final UUID playerId;
        private final Recipe recipe;
        private final String reason;
        
        public RecipeForgotten(UUID playerId, Recipe recipe, String reason) {
            this.playerId = playerId;
            this.recipe = recipe;
            this.reason = reason;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public Recipe getRecipe() {
            return recipe;
        }
        
        public String getReason() {
            return reason;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.RecipeForgotten{player=%s, recipe=%s, reason='%s'}", 
                playerId, recipe.getId(), reason);
        }
    }
    
    /**
     * Event fired when a crafting station is used
     */
    public static class StationUsed extends Event {
        private final UUID playerId;
        private final String stationType;
        private final Recipe recipe;
        
        public StationUsed(UUID playerId, String stationType, Recipe recipe) {
            this.playerId = playerId;
            this.stationType = stationType;
            this.recipe = recipe;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
        
        public String getStationType() {
            return stationType;
        }
        
        public Recipe getRecipe() {
            return recipe;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.StationUsed{player=%s, station='%s', recipe=%s}", 
                playerId, stationType, recipe != null ? recipe.getId() : "none");
        }
    }
    
    /**
     * Event fired when crafting requirements are not met
     */
    public static class RequirementsNotMet extends CraftingEvent {
        private final List<String> missingRequirements;
        
        public RequirementsNotMet(UUID playerId, Recipe recipe, List<String> missingRequirements) {
            super(playerId, recipe);
            this.missingRequirements = missingRequirements;
        }
        
        public List<String> getMissingRequirements() {
            return missingRequirements;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.RequirementsNotMet{player=%s, recipe=%s, missing=%s}", 
                playerId, recipe.getId(), missingRequirements);
        }
    }
    
    /**
     * Event fired when a bulk crafting operation starts
     */
    public static class BulkCraftingStarted extends CraftingEvent {
        private final int quantity;
        private final UUID batchId;
        
        public BulkCraftingStarted(UUID playerId, Recipe recipe, int quantity, UUID batchId) {
            super(playerId, recipe);
            this.quantity = quantity;
            this.batchId = batchId;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public UUID getBatchId() {
            return batchId;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.BulkCraftingStarted{player=%s, recipe=%s, quantity=%d, batch=%s}", 
                playerId, recipe.getId(), quantity, batchId);
        }
    }
    
    /**
     * Event fired when a bulk crafting operation completes
     */
    public static class BulkCraftingCompleted extends CraftingEvent {
        private final int quantity;
        private final int actualCrafted;
        private final UUID batchId;
        private final List<ItemStack> totalResults;
        
        public BulkCraftingCompleted(UUID playerId, Recipe recipe, int quantity, int actualCrafted, 
                                   UUID batchId, List<ItemStack> totalResults) {
            super(playerId, recipe);
            this.quantity = quantity;
            this.actualCrafted = actualCrafted;
            this.batchId = batchId;
            this.totalResults = totalResults;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public int getActualCrafted() {
            return actualCrafted;
        }
        
        public UUID getBatchId() {
            return batchId;
        }
        
        public List<ItemStack> getTotalResults() {
            return totalResults;
        }
        
        @Override
        public String toString() {
            return String.format("CraftingEvent.BulkCraftingCompleted{player=%s, recipe=%s, crafted=%d/%d, batch=%s}", 
                playerId, recipe.getId(), actualCrafted, quantity, batchId);
        }
    }
}