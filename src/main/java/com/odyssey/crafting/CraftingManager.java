package com.odyssey.crafting;

import com.odyssey.items.ItemStack;
import com.odyssey.inventory.Inventory;
import com.odyssey.events.EventBus;
import com.odyssey.events.CraftingEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages crafting operations and crafting sessions.
 * Handles recipe execution, progress tracking, and crafting events.
 */
public class CraftingManager {
    private static final CraftingManager INSTANCE = new CraftingManager();
    
    private final Map<UUID, CraftingSession> activeSessions = new ConcurrentHashMap<>();
    private final RecipeRegistry recipeRegistry = RecipeRegistry.getInstance();
    private final EventBus eventBus = EventBus.getInstance();
    
    private CraftingManager() {}
    
    /**
     * Gets the singleton instance
     */
    public static CraftingManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Starts a new crafting session
     */
    public CraftingSession startCrafting(UUID playerId, Recipe recipe, Inventory craftingInventory, 
                                       Inventory outputInventory) {
        if (recipe == null || craftingInventory == null || outputInventory == null) {
            throw new IllegalArgumentException("Recipe and inventories cannot be null");
        }
        
        // Check if recipe matches the crafting inventory
        if (!recipe.matches(craftingInventory)) {
            return null;
        }
        
        // Check if there's space in output inventory
        if (!canFitResults(recipe, outputInventory)) {
            return null;
        }
        
        // Create and start session
        CraftingSession session = new CraftingSession(playerId, recipe, craftingInventory, outputInventory);
        activeSessions.put(session.getId(), session);
        
        // Fire crafting start event
        eventBus.post(new CraftingEvent.Started(playerId, recipe, session.getId()));
        
        return session;
    }
    
    /**
     * Attempts instant crafting (no time delay)
     */
    public CraftingResult craftInstant(UUID playerId, Recipe recipe, Inventory craftingInventory, 
                                     Inventory outputInventory) {
        if (recipe == null || craftingInventory == null || outputInventory == null) {
            return CraftingResult.failure("Invalid parameters");
        }
        
        // Check if recipe matches
        if (!recipe.matches(craftingInventory)) {
            return CraftingResult.failure("Recipe doesn't match inventory");
        }
        
        // Check if there's space for results
        if (!canFitResults(recipe, outputInventory)) {
            return CraftingResult.failure("Not enough space in output inventory");
        }
        
        // Fire pre-craft event
        CraftingEvent.PreCraft preCraftEvent = new CraftingEvent.PreCraft(playerId, recipe);
        eventBus.post(preCraftEvent);
        
        if (preCraftEvent.isCancelled()) {
            return CraftingResult.failure("Crafting was cancelled");
        }
        
        // Consume ingredients
        if (!recipe.consumeIngredients(craftingInventory)) {
            return CraftingResult.failure("Failed to consume ingredients");
        }
        
        // Add results to output inventory
        List<ItemStack> results = recipe.getAllResults();
        List<ItemStack> overflow = new ArrayList<>();
        
        for (ItemStack result : results) {
            ItemStack remaining = outputInventory.addItem(result);
            if (!remaining.isEmpty()) {
                overflow.add(remaining);
            }
        }
        
        // Handle remaining items (containers, etc.)
        List<ItemStack> remainingItems = recipe.getRemainingItems(craftingInventory);
        for (ItemStack remaining : remainingItems) {
            ItemStack leftover = craftingInventory.addItem(remaining);
            if (!leftover.isEmpty()) {
                overflow.add(leftover);
            }
        }
        
        // Fire post-craft event
        eventBus.post(new CraftingEvent.Completed(playerId, recipe, results, recipe.getExperienceReward()));
        
        return CraftingResult.success(results, overflow, recipe.getExperienceReward());
    }
    
    /**
     * Updates all active crafting sessions
     */
    public void tick() {
        Iterator<Map.Entry<UUID, CraftingSession>> iterator = activeSessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, CraftingSession> entry = iterator.next();
            CraftingSession session = entry.getValue();
            
            session.tick();
            
            if (session.isCompleted() || session.isCancelled()) {
                iterator.remove();
                
                if (session.isCompleted()) {
                    handleSessionCompletion(session);
                } else {
                    handleSessionCancellation(session);
                }
            }
        }
    }
    
    /**
     * Cancels a crafting session
     */
    public boolean cancelCrafting(UUID sessionId) {
        CraftingSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.cancel();
            return true;
        }
        return false;
    }
    
    /**
     * Gets an active crafting session
     */
    public CraftingSession getSession(UUID sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Gets all active sessions for a player
     */
    public List<CraftingSession> getPlayerSessions(UUID playerId) {
        return activeSessions.values().stream()
            .filter(session -> session.getPlayerId().equals(playerId))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Gets all active sessions
     */
    public Collection<CraftingSession> getAllSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Checks if results can fit in the output inventory
     */
    private boolean canFitResults(Recipe recipe, Inventory outputInventory) {
        // Create a copy of the inventory to test
        Inventory testInventory = new Inventory(outputInventory.getSize(), "test");
        for (int i = 0; i < outputInventory.getSize(); i++) {
            testInventory.setItem(i, outputInventory.getItem(i).copy());
        }
        
        // Try to add all results
        for (ItemStack result : recipe.getAllResults()) {
            ItemStack remaining = testInventory.addItem(result);
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Handles completion of a crafting session
     */
    private void handleSessionCompletion(CraftingSession session) {
        Recipe recipe = session.getRecipe();
        
        // Consume ingredients
        if (recipe.consumeIngredients(session.getCraftingInventory())) {
            // Add results to output inventory
            List<ItemStack> results = recipe.getAllResults();
            List<ItemStack> overflow = new ArrayList<>();
            
            for (ItemStack result : results) {
                ItemStack remaining = session.getOutputInventory().addItem(result);
                if (!remaining.isEmpty()) {
                    overflow.add(remaining);
                }
            }
            
            // Handle remaining items
            List<ItemStack> remainingItems = recipe.getRemainingItems(session.getCraftingInventory());
            for (ItemStack remaining : remainingItems) {
                ItemStack leftover = session.getCraftingInventory().addItem(remaining);
                if (!leftover.isEmpty()) {
                    overflow.add(leftover);
                }
            }
            
            // Fire completion event
            eventBus.post(new CraftingEvent.Completed(session.getPlayerId(), recipe, results, 
                                                    recipe.getExperienceReward()));
            
            // Handle overflow items (drop them or return to player)
            if (!overflow.isEmpty()) {
                eventBus.post(new CraftingEvent.Overflow(session.getPlayerId(), overflow));
            }
        } else {
            // Failed to consume ingredients - fire failure event
            eventBus.post(new CraftingEvent.Failed(session.getPlayerId(), recipe, "Failed to consume ingredients"));
        }
    }
    
    /**
     * Handles cancellation of a crafting session
     */
    private void handleSessionCancellation(CraftingSession session) {
        eventBus.post(new CraftingEvent.Cancelled(session.getPlayerId(), session.getRecipe(), session.getId()));
    }
    
    /**
     * Represents an active crafting session
     */
    public static class CraftingSession {
        private final UUID id;
        private final UUID playerId;
        private final Recipe recipe;
        private final Inventory craftingInventory;
        private final Inventory outputInventory;
        private final long startTime;
        
        private int progress;
        private boolean completed;
        private boolean cancelled;
        
        public CraftingSession(UUID playerId, Recipe recipe, Inventory craftingInventory, 
                             Inventory outputInventory) {
            this.id = UUID.randomUUID();
            this.playerId = playerId;
            this.recipe = recipe;
            this.craftingInventory = craftingInventory;
            this.outputInventory = outputInventory;
            this.startTime = System.currentTimeMillis();
            this.progress = 0;
            this.completed = false;
            this.cancelled = false;
        }
        
        public UUID getId() { return id; }
        public UUID getPlayerId() { return playerId; }
        public Recipe getRecipe() { return recipe; }
        public Inventory getCraftingInventory() { return craftingInventory; }
        public Inventory getOutputInventory() { return outputInventory; }
        public long getStartTime() { return startTime; }
        public int getProgress() { return progress; }
        public boolean isCompleted() { return completed; }
        public boolean isCancelled() { return cancelled; }
        
        /**
         * Gets the progress as a percentage (0-100)
         */
        public float getProgressPercentage() {
            return (float) progress / recipe.getCraftingTime() * 100f;
        }
        
        /**
         * Gets the remaining time in ticks
         */
        public int getRemainingTime() {
            return Math.max(0, recipe.getCraftingTime() - progress);
        }
        
        /**
         * Gets the elapsed time in milliseconds
         */
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        /**
         * Updates the crafting progress
         */
        public void tick() {
            if (completed || cancelled) {
                return;
            }
            
            progress++;
            
            if (progress >= recipe.getCraftingTime()) {
                completed = true;
            }
        }
        
        /**
         * Cancels the crafting session
         */
        public void cancel() {
            if (!completed) {
                cancelled = true;
            }
        }
        
        /**
         * Checks if the session is still valid
         */
        public boolean isValid() {
            return !completed && !cancelled && recipe.matches(craftingInventory);
        }
        
        @Override
        public String toString() {
            return String.format("CraftingSession{id=%s, recipe=%s, progress=%d/%d, completed=%s}", 
                id, recipe.getId(), progress, recipe.getCraftingTime(), completed);
        }
    }
    
    /**
     * Represents the result of a crafting operation
     */
    public static class CraftingResult {
        private final boolean success;
        private final String message;
        private final List<ItemStack> results;
        private final List<ItemStack> overflow;
        private final int experience;
        
        private CraftingResult(boolean success, String message, List<ItemStack> results, 
                             List<ItemStack> overflow, int experience) {
            this.success = success;
            this.message = message;
            this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
            this.overflow = overflow != null ? new ArrayList<>(overflow) : new ArrayList<>();
            this.experience = experience;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<ItemStack> getResults() { return new ArrayList<>(results); }
        public List<ItemStack> getOverflow() { return new ArrayList<>(overflow); }
        public int getExperience() { return experience; }
        
        public static CraftingResult success(List<ItemStack> results, List<ItemStack> overflow, int experience) {
            return new CraftingResult(true, "Crafting successful", results, overflow, experience);
        }
        
        public static CraftingResult failure(String message) {
            return new CraftingResult(false, message, null, null, 0);
        }
        
        @Override
        public String toString() {
            return String.format("CraftingResult{success=%s, message='%s', results=%d, overflow=%d, exp=%d}", 
                success, message, results.size(), overflow.size(), experience);
        }
    }
}