package com.odyssey.achievement;

import com.odyssey.util.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all achievements in the game, including tracking progress and unlocking rewards.
 */
public class AchievementManager {
    private static final Logger LOGGER = Logger.getLogger(AchievementManager.class);
    
    /** All available achievements by their unique ID */
    private final Map<String, Achievement> allAchievements;
    
    /** Unlocked achievements */
    private final Map<String, Achievement> unlockedAchievements;
    
    /** Achievement progress tracking */
    private final Map<String, Integer> achievementProgress;
    
    /** Achievement unlock timestamps */
    private final Map<String, Long> unlockTimestamps;
    
    /**
     * Creates a new AchievementManager instance.
     */
    public AchievementManager() {
        this.allAchievements = new ConcurrentHashMap<>();
        this.unlockedAchievements = new ConcurrentHashMap<>();
        this.achievementProgress = new ConcurrentHashMap<>();
        this.unlockTimestamps = new ConcurrentHashMap<>();
        
        // Initialize default achievements
        initializeDefaultAchievements();
        
        LOGGER.info("AchievementManager initialized with {} achievements", allAchievements.size());
    }
    
    /**
     * Registers an achievement with the manager.
     * 
     * @param achievement The achievement to register
     * @return true if registration was successful
     */
    public boolean registerAchievement(Achievement achievement) {
        if (achievement == null || achievement.getId() == null) {
            LOGGER.warn("Cannot register null achievement or achievement with null ID");
            return false;
        }
        
        if (allAchievements.containsKey(achievement.getId())) {
            LOGGER.warn("Achievement with ID '{}' is already registered", achievement.getId());
            return false;
        }
        
        allAchievements.put(achievement.getId(), achievement);
        achievementProgress.put(achievement.getId(), 0);
        
        LOGGER.info("Registered achievement '{}': {}", achievement.getId(), achievement.getTitle());
        return true;
    }
    
    /**
     * Unlocks an achievement.
     * 
     * @param achievementId The achievement ID
     * @return true if the achievement was unlocked
     */
    public boolean unlockAchievement(String achievementId) {
        Achievement achievement = allAchievements.get(achievementId);
        if (achievement == null) {
            LOGGER.warn("Cannot unlock achievement '{}' - achievement not found", achievementId);
            return false;
        }
        
        if (unlockedAchievements.containsKey(achievementId)) {
            LOGGER.debug("Achievement '{}' is already unlocked", achievementId);
            return false;
        }
        
        unlockedAchievements.put(achievementId, achievement);
        unlockTimestamps.put(achievementId, System.currentTimeMillis());
        achievementProgress.put(achievementId, achievement.getMaxProgress());
        
        LOGGER.info("🏆 Achievement Unlocked: '{}' - {}", achievement.getTitle(), achievement.getDescription());
        
        // Award points if applicable
        if (achievement.getPoints() > 0) {
            LOGGER.info("Awarded {} achievement points", achievement.getPoints());
        }
        
        return true;
    }
    
    /**
     * Updates achievement progress.
     * 
     * @param achievementId The achievement ID
     * @param progress The new progress value
     */
    public void setAchievementProgress(String achievementId, int progress) {
        Achievement achievement = allAchievements.get(achievementId);
        if (achievement == null) {
            LOGGER.warn("Cannot set progress for unknown achievement '{}'", achievementId);
            return;
        }
        
        if (unlockedAchievements.containsKey(achievementId)) {
            return; // Already unlocked
        }
        
        int oldProgress = achievementProgress.getOrDefault(achievementId, 0);
        int newProgress = Math.max(0, Math.min(progress, achievement.getMaxProgress()));
        achievementProgress.put(achievementId, newProgress);
        
        // Check for unlock
        if (newProgress >= achievement.getMaxProgress()) {
            unlockAchievement(achievementId);
        }
        
        if (oldProgress != newProgress) {
            LOGGER.debug("Updated achievement '{}' progress: {} -> {}", achievementId, oldProgress, newProgress);
        }
    }
    
    /**
     * Advances achievement progress by a certain amount.
     * 
     * @param achievementId The achievement ID
     * @param amount The amount to advance
     */
    public void advanceAchievementProgress(String achievementId, int amount) {
        int currentProgress = achievementProgress.getOrDefault(achievementId, 0);
        setAchievementProgress(achievementId, currentProgress + amount);
    }
    
    /**
     * Gets the progress of an achievement.
     * 
     * @param achievementId The achievement ID
     * @return The achievement progress, or 0 if not found
     */
    public int getAchievementProgress(String achievementId) {
        return achievementProgress.getOrDefault(achievementId, 0);
    }
    
    /**
     * Checks if an achievement is unlocked.
     * 
     * @param achievementId The achievement ID
     * @return true if the achievement is unlocked
     */
    public boolean isAchievementUnlocked(String achievementId) {
        return unlockedAchievements.containsKey(achievementId);
    }
    
    /**
     * Gets all achievements for saving.
     * 
     * @return Map of achievement unlock status
     */
    public Map<String, Boolean> getAllAchievements() {
        Map<String, Boolean> achievements = new HashMap<>();
        for (String achievementId : allAchievements.keySet()) {
            achievements.put(achievementId, unlockedAchievements.containsKey(achievementId));
        }
        return achievements;
    }
    
    /**
     * Loads achievement data from save.
     * 
     * @param savedAchievements The saved achievement data
     */
    public void loadAchievements(Map<String, Boolean> savedAchievements) {
        if (savedAchievements == null) {
            return;
        }
        
        unlockedAchievements.clear();
        unlockTimestamps.clear();
        
        for (Map.Entry<String, Boolean> entry : savedAchievements.entrySet()) {
            String achievementId = entry.getKey();
            boolean unlocked = entry.getValue();
            
            if (unlocked) {
                Achievement achievement = allAchievements.get(achievementId);
                if (achievement != null) {
                    unlockedAchievements.put(achievementId, achievement);
                    unlockTimestamps.put(achievementId, System.currentTimeMillis());
                    achievementProgress.put(achievementId, achievement.getMaxProgress());
                }
            }
        }
        
        LOGGER.info("Loaded {} unlocked achievements", unlockedAchievements.size());
    }
    
    /**
     * Gets all unlocked achievements.
     * 
     * @return List of unlocked achievements
     */
    public List<Achievement> getUnlockedAchievements() {
        return new ArrayList<>(unlockedAchievements.values());
    }
    
    /**
     * Gets all available achievements.
     * 
     * @return List of all achievements
     */
    public List<Achievement> getAllAvailableAchievements() {
        return new ArrayList<>(allAchievements.values());
    }
    
    /**
     * Gets achievements by category.
     * 
     * @param category The achievement category
     * @return List of achievements in the category
     */
    public List<Achievement> getAchievementsByCategory(Achievement.AchievementCategory category) {
        List<Achievement> categoryAchievements = new ArrayList<>();
        for (Achievement achievement : allAchievements.values()) {
            if (achievement.getCategory() == category) {
                categoryAchievements.add(achievement);
            }
        }
        return categoryAchievements;
    }
    
    /**
     * Gets the total achievement points earned.
     * 
     * @return Total points
     */
    public int getTotalPoints() {
        int totalPoints = 0;
        for (Achievement achievement : unlockedAchievements.values()) {
            totalPoints += achievement.getPoints();
        }
        return totalPoints;
    }
    
    /**
     * Gets the achievement unlock percentage.
     * 
     * @return Percentage of achievements unlocked (0.0 to 1.0)
     */
    public float getUnlockPercentage() {
        if (allAchievements.isEmpty()) {
            return 0.0f;
        }
        return (float) unlockedAchievements.size() / allAchievements.size();
    }
    
    /**
     * Initializes default achievements.
     */
    private void initializeDefaultAchievements() {
        // Tutorial achievements
        Achievement firstSteps = new Achievement("first_steps", "First Steps", 
            "Complete the tutorial", Achievement.AchievementCategory.TUTORIAL, 10, 1);
        registerAchievement(firstSteps);
        
        // Exploration achievements
        Achievement explorer = new Achievement("explorer", "Explorer", 
            "Discover 10 islands", Achievement.AchievementCategory.EXPLORATION, 25, 10);
        registerAchievement(explorer);
        
        Achievement worldTraveler = new Achievement("world_traveler", "World Traveler", 
            "Visit all biome types", Achievement.AchievementCategory.EXPLORATION, 50, 7);
        registerAchievement(worldTraveler);
        
        // Combat achievements
        Achievement firstVictory = new Achievement("first_victory", "First Victory", 
            "Win your first naval battle", Achievement.AchievementCategory.COMBAT, 15, 1);
        registerAchievement(firstVictory);
        
        Achievement scourgeOfTheSeas = new Achievement("scourge_of_seas", "Scourge of the Seas", 
            "Defeat 100 enemy ships", Achievement.AchievementCategory.COMBAT, 100, 100);
        registerAchievement(scourgeOfTheSeas);
        
        // Trading achievements
        Achievement merchant = new Achievement("merchant", "Merchant", 
            "Complete 50 trades", Achievement.AchievementCategory.TRADING, 30, 50);
        registerAchievement(merchant);
        
        Achievement goldRush = new Achievement("gold_rush", "Gold Rush", 
            "Earn 10,000 gold", Achievement.AchievementCategory.TRADING, 40, 10000);
        registerAchievement(goldRush);
        
        // Ship building achievements
        Achievement shipwright = new Achievement("shipwright", "Shipwright", 
            "Build your first custom ship", Achievement.AchievementCategory.CRAFTING, 20, 1);
        registerAchievement(shipwright);
        
        Achievement fleetCommander = new Achievement("fleet_commander", "Fleet Commander", 
            "Own 5 ships simultaneously", Achievement.AchievementCategory.CRAFTING, 75, 5);
        registerAchievement(fleetCommander);
        
        // Special achievements
        Achievement legendaryPirate = new Achievement("legendary_pirate", "Legendary Pirate", 
            "Reach maximum reputation with all factions", Achievement.AchievementCategory.SPECIAL, 200, 4);
        registerAchievement(legendaryPirate);
        
        LOGGER.info("Initialized {} default achievements", allAchievements.size());
    }
    
    /**
     * Updates the achievement manager.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Update achievements that need time-based tracking
        for (Achievement achievement : allAchievements.values()) {
            try {
                achievement.update(deltaTime);
            } catch (Exception e) {
                LOGGER.error("Error updating achievement '{}'", achievement.getId(), e);
            }
        }
    }
    
    /**
     * Clears all achievement data.
     */
    public void clear() {
        unlockedAchievements.clear();
        achievementProgress.clear();
        unlockTimestamps.clear();
        LOGGER.info("Cleared all achievement data");
    }
}