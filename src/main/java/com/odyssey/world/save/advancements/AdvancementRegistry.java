package com.odyssey.world.save.advancements;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing maritime advancements in The Odyssey.
 * Handles advancement definitions, dependencies, and availability.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AdvancementRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AdvancementRegistry.class);
    
    private final Map<String, Advancement> advancements;
    private final Map<String, Set<String>> dependencies; // advancement -> required advancements
    private final Map<String, Set<String>> unlocks; // advancement -> unlocked advancements
    
    public AdvancementRegistry() {
        this.advancements = new ConcurrentHashMap<>();
        this.dependencies = new ConcurrentHashMap<>();
        this.unlocks = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers default maritime advancements.
     */
    public void registerDefaultAdvancements() {
        // Basic maritime advancements
        registerAdvancement(createAdvancement("first_sail", "First Sail", 
            "Set sail for the first time", AdvancementType.NAVIGATION));
        
        registerAdvancement(createAdvancement("treasure_hunter", "Treasure Hunter", 
            "Find your first treasure chest", AdvancementType.TREASURE));
        
        registerAdvancement(createAdvancement("sea_legs", "Sea Legs", 
            "Sail for 1000 blocks", AdvancementType.NAVIGATION));
        
        registerAdvancement(createAdvancement("master_navigator", "Master Navigator", 
            "Visit 10 different islands", AdvancementType.NAVIGATION));
        
        registerAdvancement(createAdvancement("pirate_lord", "Pirate Lord", 
            "Defeat 100 enemy ships", AdvancementType.COMBAT));
        
        registerAdvancement(createAdvancement("merchant_prince", "Merchant Prince", 
            "Complete 50 trade routes", AdvancementType.TREASURE));
        
        registerAdvancement(createAdvancement("storm_rider", "Storm Rider", 
            "Survive a hurricane", AdvancementType.SURVIVAL));
        
        registerAdvancement(createAdvancement("deep_sea_explorer", "Deep Sea Explorer", 
            "Dive to the ocean floor", AdvancementType.NAVIGATION));
        
        // Set up dependencies
        addDependency("sea_legs", "first_sail");
        addDependency("master_navigator", "sea_legs");
        addDependency("pirate_lord", "first_sail");
        addDependency("merchant_prince", "first_sail");
        addDependency("storm_rider", "sea_legs");
        addDependency("deep_sea_explorer", "master_navigator");
        
        logger.info("Registered {} default maritime advancements", advancements.size());
    }
    
    /**
     * Registers an advancement.
     * 
     * @param advancement the advancement to register
     */
    public void registerAdvancement(Advancement advancement) {
        advancements.put(advancement.getId(), advancement);
        dependencies.putIfAbsent(advancement.getId(), new HashSet<>());
        unlocks.putIfAbsent(advancement.getId(), new HashSet<>());
    }
    
    /**
     * Gets an advancement by ID.
     * 
     * @param advancementId the advancement identifier
     * @return the advancement, or null if not found
     */
    public Advancement getAdvancement(String advancementId) {
        return advancements.get(advancementId);
    }
    
    /**
     * Gets all registered advancements.
     * 
     * @return map of advancement IDs to advancements
     */
    public Map<String, Advancement> getAllAdvancements() {
        return new HashMap<>(advancements);
    }
    
    /**
     * Gets the number of registered advancements.
     * 
     * @return the advancement count
     */
    public int getAdvancementCount() {
        return advancements.size();
    }
    
    /**
     * Gets available advancements for a player.
     * 
     * @param playerAdvancements the player's advancement data
     * @return map of available advancement IDs to advancements
     */
    public Map<String, Advancement> getAvailableAdvancements(PlayerAdvancements playerAdvancements) {
        Map<String, Advancement> available = new HashMap<>();
        
        for (Advancement advancement : advancements.values()) {
            if (isAvailable(advancement.getId(), playerAdvancements)) {
                available.put(advancement.getId(), advancement);
            }
        }
        
        return available;
    }
    
    /**
     * Gets newly available advancements for a player.
     * This method returns advancements that have become available since the last check.
     * 
     * @param playerAdvancements the player's advancement data
     * @return map of newly available advancement IDs to advancements
     */
    public Map<String, Advancement> getNewlyAvailableAdvancements(PlayerAdvancements playerAdvancements) {
        Map<String, Advancement> newlyAvailable = new HashMap<>();
        
        // Get all currently available advancements
        Map<String, Advancement> currentlyAvailable = getAvailableAdvancements(playerAdvancements);
        
        // Filter out advancements that were already available
        for (Map.Entry<String, Advancement> entry : currentlyAvailable.entrySet()) {
            String advancementId = entry.getKey();
            
            // Check if this advancement was recently unlocked by checking dependencies
            if (wasRecentlyUnlocked(advancementId, playerAdvancements)) {
                newlyAvailable.put(advancementId, entry.getValue());
            }
        }
        
        return newlyAvailable;
    }
    
    /**
     * Checks if an advancement is available to a player.
     * 
     * @param advancementId the advancement identifier
     * @param playerAdvancements the player's advancement data
     * @return true if the advancement is available
     */
    public boolean isAvailable(String advancementId, PlayerAdvancements playerAdvancements) {
        // Already completed
        if (playerAdvancements.isCompleted(advancementId)) {
            return false;
        }
        
        // Check dependencies
        Set<String> requiredAdvancements = dependencies.get(advancementId);
        if (requiredAdvancements != null) {
            for (String required : requiredAdvancements) {
                if (!playerAdvancements.isCompleted(required)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Adds a dependency between advancements.
     * 
     * @param advancementId the advancement that requires another
     * @param requiredId the required advancement
     */
    public void addDependency(String advancementId, String requiredId) {
        dependencies.computeIfAbsent(advancementId, k -> new HashSet<>()).add(requiredId);
        unlocks.computeIfAbsent(requiredId, k -> new HashSet<>()).add(advancementId);
    }
    
    /**
     * Gets advancements unlocked by completing a specific advancement.
     * 
     * @param advancementId the completed advancement
     * @return set of unlocked advancement IDs
     */
    public Set<String> getUnlockedAdvancements(String advancementId) {
        return new HashSet<>(unlocks.getOrDefault(advancementId, Collections.emptySet()));
    }
    
    /**
     * Gets the dependencies for an advancement.
     * 
     * @param advancementId the advancement identifier
     * @return set of required advancement IDs
     */
    public Set<String> getDependencies(String advancementId) {
        return new HashSet<>(dependencies.getOrDefault(advancementId, Collections.emptySet()));
    }
    
    /**
     * Checks if an advancement was recently unlocked.
     * This is a simplified implementation that checks if all dependencies are completed.
     * 
     * @param advancementId the advancement identifier
     * @param playerAdvancements the player's advancement data
     * @return true if the advancement was recently unlocked
     */
    private boolean wasRecentlyUnlocked(String advancementId, PlayerAdvancements playerAdvancements) {
        // For now, we consider an advancement "recently unlocked" if it's available
        // but not yet started. In a more sophisticated implementation, we could
        // track when dependencies were completed and compare timestamps.
        
        if (playerAdvancements.hasStarted(advancementId) || playerAdvancements.isCompleted(advancementId)) {
            return false; // Already started or completed
        }
        
        // Check if all dependencies are completed
        Set<String> requiredAdvancements = dependencies.get(advancementId);
        if (requiredAdvancements != null) {
            for (String required : requiredAdvancements) {
                if (!playerAdvancements.isCompleted(required)) {
                    return false; // Dependencies not met
                }
            }
        }
        
        return true; // Available and not yet started
    }
    
    /**
     * Creates a basic advancement.
     * 
     * @param id the advancement ID
     * @param title the advancement title
     * @param description the advancement description
     * @param type the advancement type
     * @return the created advancement
     */
    private Advancement createAdvancement(String id, String title, String description, AdvancementType type) {
        Advancement advancement = new Advancement(id, title, description, type);
        
        // Add basic criteria (can be expanded later)
        Map<String, AdvancementCriteria> criteria = new HashMap<>();
        criteria.put("main", new AdvancementCriteria("main", CriteriaType.CUSTOM, id, 1));
        advancement.setCriteria(criteria);
        
        // Add basic rewards (can be expanded later)
        List<AdvancementReward> rewards = new ArrayList<>();
        rewards.add(new AdvancementReward(AdvancementReward.Type.EXPERIENCE, 100));
        advancement.setRewards(rewards);
        
        return advancement;
    }
}