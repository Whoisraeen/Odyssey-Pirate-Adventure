package com.odyssey.quest;

import com.odyssey.util.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all quests in the game, including quest progression, completion, and rewards.
 * Handles both main story quests and side quests.
 */
public class QuestManager {
    private static final Logger LOGGER = Logger.getLogger(QuestManager.class);
    
    /** All available quests by their unique ID */
    private final Map<String, Quest> allQuests;
    
    /** Active quests that the player has accepted */
    private final Map<String, Quest> activeQuests;
    
    /** Completed quests */
    private final Map<String, Quest> completedQuests;
    
    /** Quest progress tracking */
    private final Map<String, Integer> questProgress;
    
    /** Quest objectives tracking */
    private final Map<String, Map<String, Boolean>> questObjectives;
    
    /**
     * Creates a new QuestManager instance.
     */
    public QuestManager() {
        this.allQuests = new ConcurrentHashMap<>();
        this.activeQuests = new ConcurrentHashMap<>();
        this.completedQuests = new ConcurrentHashMap<>();
        this.questProgress = new ConcurrentHashMap<>();
        this.questObjectives = new ConcurrentHashMap<>();
        
        // Initialize default quests
        initializeDefaultQuests();
        
        LOGGER.info("QuestManager initialized");
    }
    
    /**
     * Registers a quest with the manager.
     * 
     * @param quest The quest to register
     * @return true if registration was successful
     */
    public boolean registerQuest(Quest quest) {
        if (quest == null || quest.getId() == null) {
            LOGGER.warn("Cannot register null quest or quest with null ID");
            return false;
        }
        
        if (allQuests.containsKey(quest.getId())) {
            LOGGER.warn("Quest with ID '{}' is already registered", quest.getId());
            return false;
        }
        
        allQuests.put(quest.getId(), quest);
        LOGGER.info("Registered quest '{}': {}", quest.getId(), quest.getTitle());
        
        return true;
    }
    
    /**
     * Starts a quest for the player.
     * 
     * @param questId The ID of the quest to start
     * @return true if the quest was started successfully
     */
    public boolean startQuest(String questId) {
        Quest quest = allQuests.get(questId);
        if (quest == null) {
            LOGGER.warn("Cannot start quest '{}' - quest not found", questId);
            return false;
        }
        
        if (activeQuests.containsKey(questId)) {
            LOGGER.warn("Quest '{}' is already active", questId);
            return false;
        }
        
        if (completedQuests.containsKey(questId)) {
            LOGGER.warn("Quest '{}' is already completed", questId);
            return false;
        }
        
        activeQuests.put(questId, quest);
        questProgress.put(questId, 0);
        questObjectives.put(questId, new HashMap<>());
        
        LOGGER.info("Started quest '{}': {}", questId, quest.getTitle());
        return true;
    }
    
    /**
     * Updates quest progress.
     * 
     * @param questId The quest ID
     * @param progress The new progress value
     */
    public void setQuestProgress(String questId, int progress) {
        if (!activeQuests.containsKey(questId)) {
            LOGGER.warn("Cannot set progress for inactive quest '{}'", questId);
            return;
        }
        
        int oldProgress = questProgress.getOrDefault(questId, 0);
        questProgress.put(questId, progress);
        
        Quest quest = activeQuests.get(questId);
        if (quest != null && progress >= quest.getMaxProgress()) {
            completeQuest(questId);
        }
        
        LOGGER.debug("Updated quest '{}' progress: {} -> {}", questId, oldProgress, progress);
    }
    
    /**
     * Advances quest progress by a certain amount.
     * 
     * @param questId The quest ID
     * @param amount The amount to advance
     */
    public void advanceQuestProgress(String questId, int amount) {
        int currentProgress = questProgress.getOrDefault(questId, 0);
        setQuestProgress(questId, currentProgress + amount);
    }
    
    /**
     * Completes a quest.
     * 
     * @param questId The quest ID
     * @return true if the quest was completed successfully
     */
    public boolean completeQuest(String questId) {
        Quest quest = activeQuests.remove(questId);
        if (quest == null) {
            LOGGER.warn("Cannot complete quest '{}' - quest not active", questId);
            return false;
        }
        
        completedQuests.put(questId, quest);
        
        // Award rewards
        if (quest.getExperienceReward() > 0) {
            LOGGER.info("Quest '{}' completed! Awarded {} experience", questId, quest.getExperienceReward());
        }
        
        if (quest.getGoldReward() > 0) {
            LOGGER.info("Quest '{}' completed! Awarded {} gold", questId, quest.getGoldReward());
        }
        
        LOGGER.info("Completed quest '{}': {}", questId, quest.getTitle());
        return true;
    }
    
    /**
     * Gets the progress of a quest.
     * 
     * @param questId The quest ID
     * @return The quest progress, or 0 if not found
     */
    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }
    
    /**
     * Gets all quest progress for saving.
     * 
     * @return Map of quest progress
     */
    public Map<String, Integer> getAllQuestProgress() {
        return new HashMap<>(questProgress);
    }
    
    /**
     * Loads quest progress from save data.
     * 
     * @param savedProgress The saved quest progress
     */
    public void loadQuestProgress(Map<String, Integer> savedProgress) {
        if (savedProgress == null) {
            return;
        }
        
        questProgress.clear();
        questProgress.putAll(savedProgress);
        
        // Restore active quests based on progress
        for (Map.Entry<String, Integer> entry : savedProgress.entrySet()) {
            String questId = entry.getKey();
            int progress = entry.getValue();
            
            Quest quest = allQuests.get(questId);
            if (quest != null) {
                if (progress >= quest.getMaxProgress()) {
                    completedQuests.put(questId, quest);
                } else if (progress > 0) {
                    activeQuests.put(questId, quest);
                }
            }
        }
        
        LOGGER.info("Loaded quest progress for {} quests", savedProgress.size());
    }
    
    /**
     * Gets all active quests.
     * 
     * @return List of active quests
     */
    public List<Quest> getActiveQuests() {
        return new ArrayList<>(activeQuests.values());
    }
    
    /**
     * Gets all completed quests.
     * 
     * @return List of completed quests
     */
    public List<Quest> getCompletedQuests() {
        return new ArrayList<>(completedQuests.values());
    }
    
    /**
     * Checks if a quest is active.
     * 
     * @param questId The quest ID
     * @return true if the quest is active
     */
    public boolean isQuestActive(String questId) {
        return activeQuests.containsKey(questId);
    }
    
    /**
     * Checks if a quest is completed.
     * 
     * @param questId The quest ID
     * @return true if the quest is completed
     */
    public boolean isQuestCompleted(String questId) {
        return completedQuests.containsKey(questId);
    }
    
    /**
     * Gets a quest by its ID.
     * 
     * @param questId The quest ID
     * @return The quest, or null if not found
     */
    public Quest getQuest(String questId) {
        return allQuests.get(questId);
    }
    
    /**
     * Initializes default quests.
     */
    private void initializeDefaultQuests() {
        // Tutorial quest
        Quest tutorialQuest = new Quest("tutorial_001", "First Steps", 
            "Learn the basics of sailing and ship management", 100, 50, 0);
        registerQuest(tutorialQuest);
        
        // Exploration quest
        Quest explorationQuest = new Quest("explore_001", "Charting the Waters", 
            "Discover 5 new islands", 200, 100, 0);
        registerQuest(explorationQuest);
        
        // Combat quest
        Quest combatQuest = new Quest("combat_001", "Defending the Seas", 
            "Defeat 3 pirate ships", 300, 150, 0);
        registerQuest(combatQuest);
        
        // Trading quest
        Quest tradingQuest = new Quest("trade_001", "Merchant Ventures", 
            "Complete 10 successful trades", 250, 200, 0);
        registerQuest(tradingQuest);
        
        LOGGER.info("Initialized {} default quests", allQuests.size());
    }
    
    /**
     * Updates the quest manager.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Update active quests
        for (Quest quest : activeQuests.values()) {
            try {
                quest.update(deltaTime);
            } catch (Exception e) {
                LOGGER.error("Error updating quest '{}'", quest.getId(), e);
            }
        }
    }
    
    /**
     * Clears all quest data.
     */
    public void clear() {
        activeQuests.clear();
        completedQuests.clear();
        questProgress.clear();
        questObjectives.clear();
        LOGGER.info("Cleared all quest data");
    }
    
    /**
     * Get the total number of quests completed by the player
     */
    public int getTotalQuestsCompleted() {
        return completedQuests.size();
    }
}