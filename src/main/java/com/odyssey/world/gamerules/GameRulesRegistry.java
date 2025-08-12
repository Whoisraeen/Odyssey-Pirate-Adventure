package com.odyssey.world.gamerules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Data-driven game rules registry for managing configurable game behavior.
 * Provides a centralized system for defining, validating, and managing game rules.
 */
public class GameRulesRegistry {
    private static final Logger logger = LoggerFactory.getLogger(GameRulesRegistry.class);
    
    private final Map<String, GameRule<?>> rules;
    private final Map<String, Object> values;
    private final Map<String, Set<GameRuleListener>> listeners;
    private final Map<String, String> categories;
    private final Set<String> lockedRules;
    private boolean allowDynamicRules = true;
    
    public GameRulesRegistry() {
        this.rules = new ConcurrentHashMap<>();
        this.values = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
        this.categories = new ConcurrentHashMap<>();
        this.lockedRules = ConcurrentHashMap.newKeySet();
        
        registerDefaultRules();
        logger.info("Game rules registry initialized with {} default rules", rules.size());
    }
    
    /**
     * Registers default game rules
     */
    private void registerDefaultRules() {
        // World behavior rules
        registerRule("doDaylightCycle", true, "World", "Whether the daylight cycle advances");
        registerRule("doWeatherCycle", true, "World", "Whether weather changes naturally");
        registerRule("doMobSpawning", true, "Mobs", "Whether mobs spawn naturally");
        registerRule("doMobLoot", true, "Mobs", "Whether mobs drop items when killed");
        registerRule("doMobGriefing", true, "Mobs", "Whether mobs can modify the world");
        registerRule("doFireTick", true, "World", "Whether fire spreads and burns out naturally");
        registerRule("doTileDrops", true, "Drops", "Whether blocks drop items when broken");
        registerRule("keepInventory", false, "Player", "Whether players keep inventory on death");
        registerRule("mobGriefing", true, "Mobs", "Whether mobs can pick up items and change blocks");
        
        // Spawn rules
        registerRule("spawnRadius", 10, "Spawn", "Radius around spawn where players appear", 0, 100);
        registerRule("maxEntityCramming", 24, "Mobs", "Maximum entities that can be crammed together", 0, 100);
        registerRule("randomTickSpeed", 3, "World", "Rate of random block ticks", 0, 4096);
        registerRule("reducedDebugInfo", false, "Debug", "Whether debug info is reduced");
        registerRule("sendCommandFeedback", true, "Commands", "Whether command feedback is sent");
        registerRule("showDeathMessages", true, "Player", "Whether death messages are shown");
        registerRule("logAdminCommands", true, "Commands", "Whether admin commands are logged");
        registerRule("commandBlockOutput", true, "Commands", "Whether command blocks show output");
        registerRule("disableElytraMovementCheck", false, "Player", "Whether elytra movement is checked");
        registerRule("maxCommandChainLength", 65536, "Commands", "Maximum command chain length", 0, Integer.MAX_VALUE);
        
        // Ocean and maritime specific rules
        registerRule("doOceanCurrents", true, "Ocean", "Whether ocean currents affect movement");
        registerRule("doTidalCycles", true, "Ocean", "Whether tidal cycles occur");
        registerRule("doStormGeneration", true, "Weather", "Whether storms generate naturally");
        registerRule("doShipDecay", false, "Ships", "Whether abandoned ships decay over time");
        registerRule("doTreasureRespawn", true, "Treasure", "Whether treasure chests respawn");
        registerRule("doSeaCreatureSpawning", true, "Ocean", "Whether sea creatures spawn");
        registerRule("doUnderwaterAmbience", true, "Ocean", "Whether underwater ambience plays");
        registerRule("doFloatingDebris", true, "Ocean", "Whether floating debris appears");
        registerRule("doCoralGrowth", true, "Ocean", "Whether coral grows naturally");
        registerRule("doKelpGrowth", true, "Ocean", "Whether kelp grows naturally");
        
        // Performance rules
        registerRule("maxTickTime", 60000L, "Performance", "Maximum tick time in milliseconds", 0L, Long.MAX_VALUE);
        registerRule("viewDistance", 10, "Performance", "Server view distance", 3, 32);
        registerRule("simulationDistance", 10, "Performance", "Simulation distance", 5, 32);
        registerRule("entityActivationRange", 32, "Performance", "Entity activation range", 16, 128);
        registerRule("chunkGCPeriod", 600, "Performance", "Chunk garbage collection period in ticks", 100, 12000);
        
        // PvP and combat rules
        registerRule("pvp", true, "Combat", "Whether player vs player combat is enabled");
        registerRule("doInsomnia", true, "Player", "Whether phantoms spawn from insomnia");
        registerRule("doPatrolSpawning", true, "Mobs", "Whether patrols spawn");
        registerRule("doTraderSpawning", true, "Mobs", "Whether wandering traders spawn");
        registerRule("doWardenSpawning", true, "Mobs", "Whether wardens spawn");
        
        // Difficulty and gameplay rules
        registerRule("naturalRegeneration", true, "Player", "Whether players regenerate health naturally");
        registerRule("doImmediateRespawn", false, "Player", "Whether players respawn immediately");
        registerRule("drowningDamage", true, "Player", "Whether drowning causes damage");
        registerRule("fallDamage", true, "Player", "Whether falling causes damage");
        registerRule("fireDamage", true, "Player", "Whether fire causes damage");
        registerRule("freezeDamage", true, "Player", "Whether freezing causes damage");
        
        // Chat and social rules
        registerRule("announceAdvancements", true, "Player", "Whether advancements are announced");
        registerRule("doLimitedCrafting", false, "Player", "Whether crafting is limited to known recipes");
        registerRule("spectatorGenerateChunks", true, "World", "Whether spectators generate chunks");
        registerRule("doEntityDrops", true, "Drops", "Whether entities drop items");
        registerRule("doVinesSpread", true, "World", "Whether vines spread");
        
        // Custom maritime rules
        registerRule("shipCollisionDamage", true, "Ships", "Whether ship collisions cause damage");
        registerRule("cannonballGriefing", true, "Combat", "Whether cannonballs can destroy blocks");
        registerRule("treasureMapAccuracy", 0.8f, "Treasure", "Accuracy of treasure maps", 0.0f, 1.0f);
        registerRule("seaMonsterSpawnRate", 0.1f, "Ocean", "Rate of sea monster spawning", 0.0f, 1.0f);
        registerRule("stormIntensity", 1.0f, "Weather", "Intensity of storms", 0.0f, 2.0f);
        registerRule("currentStrength", 1.0f, "Ocean", "Strength of ocean currents", 0.0f, 3.0f);
        registerRule("windStrength", 1.0f, "Weather", "Strength of wind effects", 0.0f, 3.0f);
        
        logger.debug("Registered {} default game rules", rules.size());
    }
    
    /**
     * Registers a boolean game rule
     */
    public void registerRule(String name, boolean defaultValue, String category, String description) {
        GameRule<Boolean> rule = new GameRule<>(name, Boolean.class, defaultValue, category, description);
        registerRule(rule);
    }
    
    /**
     * Registers an integer game rule with bounds
     */
    public void registerRule(String name, int defaultValue, String category, String description, int min, int max) {
        GameRule<Integer> rule = new GameRule<>(name, Integer.class, defaultValue, category, description);
        rule.setValidator(value -> value >= min && value <= max);
        registerRule(rule);
    }
    
    /**
     * Registers a long game rule with bounds
     */
    public void registerRule(String name, long defaultValue, String category, String description, long min, long max) {
        GameRule<Long> rule = new GameRule<>(name, Long.class, defaultValue, category, description);
        rule.setValidator(value -> value >= min && value <= max);
        registerRule(rule);
    }
    
    /**
     * Registers a float game rule with bounds
     */
    public void registerRule(String name, float defaultValue, String category, String description, float min, float max) {
        GameRule<Float> rule = new GameRule<>(name, Float.class, defaultValue, category, description);
        rule.setValidator(value -> value >= min && value <= max);
        registerRule(rule);
    }
    
    /**
     * Registers a string game rule
     */
    public void registerRule(String name, String defaultValue, String category, String description) {
        GameRule<String> rule = new GameRule<>(name, String.class, defaultValue, category, description);
        registerRule(rule);
    }
    
    /**
     * Registers a game rule
     */
    public <T> void registerRule(GameRule<T> rule) {
        if (rule == null || rule.getName() == null) {
            throw new IllegalArgumentException("Rule and rule name cannot be null");
        }
        
        if (rules.containsKey(rule.getName()) && !allowDynamicRules) {
            logger.warn("Rule {} already exists and dynamic rules are disabled", rule.getName());
            return;
        }
        
        rules.put(rule.getName(), rule);
        values.put(rule.getName(), rule.getDefaultValue());
        categories.put(rule.getName(), rule.getCategory());
        
        logger.debug("Registered game rule: {} = {} ({})", rule.getName(), rule.getDefaultValue(), rule.getCategory());
    }
    
    /**
     * Sets a game rule value
     */
    @SuppressWarnings("unchecked")
    public <T> boolean setRule(String name, T value) {
        if (name == null) {
            logger.warn("Cannot set rule: name is null");
            return false;
        }
        
        if (lockedRules.contains(name)) {
            logger.warn("Cannot set rule {}: rule is locked", name);
            return false;
        }
        
        GameRule<T> rule = (GameRule<T>) rules.get(name);
        if (rule == null) {
            logger.warn("Unknown game rule: {}", name);
            return false;
        }
        
        if (value == null) {
            logger.warn("Cannot set rule {}: value is null", name);
            return false;
        }
        
        if (!rule.getType().isInstance(value)) {
            logger.warn("Cannot set rule {}: value type {} does not match rule type {}", 
                name, value.getClass().getSimpleName(), rule.getType().getSimpleName());
            return false;
        }
        
        if (rule.getValidator() != null && !rule.getValidator().test(value)) {
            logger.warn("Cannot set rule {}: value {} failed validation", name, value);
            return false;
        }
        
        T oldValue = (T) values.get(name);
        values.put(name, value);
        
        // Notify listeners
        notifyRuleChanged(name, oldValue, value);
        
        logger.debug("Set game rule {} = {} (was {})", name, value, oldValue);
        return true;
    }
    
    /**
     * Gets a game rule value
     */
    @SuppressWarnings("unchecked")
    public <T> T getRule(String name, Class<T> type) {
        if (name == null || type == null) {
            return null;
        }
        
        Object value = values.get(name);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        
        return null;
    }
    
    /**
     * Gets a boolean rule value
     */
    public boolean getBooleanRule(String name) {
        Boolean value = getRule(name, Boolean.class);
        return value != null ? value : false;
    }
    
    /**
     * Gets an integer rule value
     */
    public int getIntRule(String name) {
        Integer value = getRule(name, Integer.class);
        return value != null ? value : 0;
    }
    
    /**
     * Gets a long rule value
     */
    public long getLongRule(String name) {
        Long value = getRule(name, Long.class);
        return value != null ? value : 0L;
    }
    
    /**
     * Gets a float rule value
     */
    public float getFloatRule(String name) {
        Float value = getRule(name, Float.class);
        return value != null ? value : 0.0f;
    }
    
    /**
     * Gets a string rule value
     */
    public String getStringRule(String name) {
        String value = getRule(name, String.class);
        return value != null ? value : "";
    }
    
    /**
     * Resets a rule to its default value
     */
    public boolean resetRule(String name) {
        GameRule<?> rule = rules.get(name);
        if (rule == null) {
            logger.warn("Cannot reset unknown rule: {}", name);
            return false;
        }
        
        if (lockedRules.contains(name)) {
            logger.warn("Cannot reset rule {}: rule is locked", name);
            return false;
        }
        
        Object oldValue = values.get(name);
        values.put(name, rule.getDefaultValue());
        
        // Notify listeners
        notifyRuleChanged(name, oldValue, rule.getDefaultValue());
        
        logger.debug("Reset game rule {} to default value {}", name, rule.getDefaultValue());
        return true;
    }
    
    /**
     * Resets all rules to their default values
     */
    public void resetAllRules() {
        for (String name : rules.keySet()) {
            if (!lockedRules.contains(name)) {
                resetRule(name);
            }
        }
        logger.info("Reset all unlocked game rules to default values");
    }
    
    /**
     * Checks if a rule exists
     */
    public boolean hasRule(String name) {
        return rules.containsKey(name);
    }
    
    /**
     * Gets all rule names
     */
    public Set<String> getRuleNames() {
        return new HashSet<>(rules.keySet());
    }
    
    /**
     * Gets all rule names in a category
     */
    public Set<String> getRuleNames(String category) {
        Set<String> categoryRules = new HashSet<>();
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            if (category.equals(entry.getValue())) {
                categoryRules.add(entry.getKey());
            }
        }
        return categoryRules;
    }
    
    /**
     * Gets all categories
     */
    public Set<String> getCategories() {
        return new HashSet<>(categories.values());
    }
    
    /**
     * Gets the category of a rule
     */
    public String getRuleCategory(String name) {
        return categories.get(name);
    }
    
    /**
     * Gets a game rule definition
     */
    public GameRule<?> getRuleDefinition(String name) {
        return rules.get(name);
    }
    
    /**
     * Locks a rule to prevent changes
     */
    public void lockRule(String name) {
        if (hasRule(name)) {
            lockedRules.add(name);
            logger.debug("Locked game rule: {}", name);
        }
    }
    
    /**
     * Unlocks a rule to allow changes
     */
    public void unlockRule(String name) {
        lockedRules.remove(name);
        logger.debug("Unlocked game rule: {}", name);
    }
    
    /**
     * Checks if a rule is locked
     */
    public boolean isRuleLocked(String name) {
        return lockedRules.contains(name);
    }
    
    /**
     * Adds a rule change listener
     */
    public void addListener(String ruleName, GameRuleListener listener) {
        listeners.computeIfAbsent(ruleName, k -> ConcurrentHashMap.newKeySet()).add(listener);
        logger.debug("Added listener for rule: {}", ruleName);
    }
    
    /**
     * Removes a rule change listener
     */
    public void removeListener(String ruleName, GameRuleListener listener) {
        Set<GameRuleListener> ruleListeners = listeners.get(ruleName);
        if (ruleListeners != null) {
            ruleListeners.remove(listener);
            if (ruleListeners.isEmpty()) {
                listeners.remove(ruleName);
            }
        }
        logger.debug("Removed listener for rule: {}", ruleName);
    }
    
    /**
     * Notifies listeners of rule changes
     */
    private void notifyRuleChanged(String name, Object oldValue, Object newValue) {
        Set<GameRuleListener> ruleListeners = listeners.get(name);
        if (ruleListeners != null) {
            for (GameRuleListener listener : ruleListeners) {
                try {
                    listener.onRuleChanged(name, oldValue, newValue);
                } catch (Exception e) {
                    logger.error("Error in game rule listener for {}: {}", name, e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Exports all rules and values
     */
    public Map<String, Object> exportRules() {
        return new HashMap<>(values);
    }
    
    /**
     * Imports rules and values
     */
    public void importRules(Map<String, Object> ruleValues) {
        for (Map.Entry<String, Object> entry : ruleValues.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            if (hasRule(name)) {
                setRule(name, value);
            } else {
                logger.warn("Cannot import unknown rule: {}", name);
            }
        }
        logger.info("Imported {} game rules", ruleValues.size());
    }
    
    /**
     * Sets whether dynamic rules can be registered
     */
    public void setAllowDynamicRules(boolean allow) {
        this.allowDynamicRules = allow;
        logger.info("Dynamic rules {}", allow ? "enabled" : "disabled");
    }
    
    public boolean isAllowDynamicRules() {
        return allowDynamicRules;
    }
    
    /**
     * Gets the number of registered rules
     */
    public int getRuleCount() {
        return rules.size();
    }
    
    /**
     * Gets the number of locked rules
     */
    public int getLockedRuleCount() {
        return lockedRules.size();
    }
    
    /**
     * Clears all rules and values
     */
    public void clear() {
        rules.clear();
        values.clear();
        listeners.clear();
        categories.clear();
        lockedRules.clear();
        logger.info("Cleared all game rules");
    }
    
    /**
     * Game rule definition class
     */
    public static class GameRule<T> {
        private final String name;
        private final Class<T> type;
        private final T defaultValue;
        private final String category;
        private final String description;
        private Predicate<T> validator;
        private Function<T, String> formatter;
        private Consumer<T> changeHandler;
        
        public GameRule(String name, Class<T> type, T defaultValue, String category, String description) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.category = category;
            this.description = description;
        }
        
        public String getName() { return name; }
        public Class<T> getType() { return type; }
        public T getDefaultValue() { return defaultValue; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public Predicate<T> getValidator() { return validator; }
        public Function<T, String> getFormatter() { return formatter; }
        public Consumer<T> getChangeHandler() { return changeHandler; }
        
        public void setValidator(Predicate<T> validator) { this.validator = validator; }
        public void setFormatter(Function<T, String> formatter) { this.formatter = formatter; }
        public void setChangeHandler(Consumer<T> changeHandler) { this.changeHandler = changeHandler; }
        
        @Override
        public String toString() {
            return String.format("GameRule{name='%s', type=%s, default=%s, category='%s'}", 
                name, type.getSimpleName(), defaultValue, category);
        }
    }
    
    /**
     * Game rule change listener interface
     */
    @FunctionalInterface
    public interface GameRuleListener {
        void onRuleChanged(String ruleName, Object oldValue, Object newValue);
    }
}