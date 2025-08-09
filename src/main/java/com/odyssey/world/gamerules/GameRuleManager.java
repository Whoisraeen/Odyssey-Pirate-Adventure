package com.odyssey.world.gamerules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages game rules for a world.
 * Handles storage, validation, change notifications, and persistence.
 */
public class GameRuleManager {
    private static final Logger logger = LoggerFactory.getLogger(GameRuleManager.class);

    private final Map<GameRule, Object> gameRuleValues = new ConcurrentHashMap<>();
    private final List<GameRuleChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Interface for listening to game rule changes.
     */
    public interface GameRuleChangeListener {
        /**
         * Called when a game rule value changes.
         * @param rule The game rule that changed
         * @param oldValue The previous value
         * @param newValue The new value
         */
        void onGameRuleChanged(GameRule rule, Object oldValue, Object newValue);
    }

    /**
     * Creates a new GameRuleManager with default values.
     */
    public GameRuleManager() {
        initializeDefaults();
    }

    /**
     * Creates a new GameRuleManager with values from a map.
     * @param initialValues Map of rule names to values
     */
    public GameRuleManager(Map<String, Object> initialValues) {
        initializeDefaults();
        loadFromMap(initialValues);
    }

    /**
     * Initializes all game rules with their default values.
     */
    private void initializeDefaults() {
        for (GameRule rule : GameRule.values()) {
            gameRuleValues.put(rule, rule.getDefaultValue());
        }
    }

    /**
     * Loads game rule values from a map.
     * @param values Map of rule names to values
     */
    public void loadFromMap(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            GameRule rule = GameRule.getByName(entry.getKey());
            if (rule != null) {
                Object value = entry.getValue();
                if (rule.isValidValue(value)) {
                    gameRuleValues.put(rule, value);
                } else {
                    logger.warn("Invalid value for game rule {}: {}. Using default value.", 
                               rule.getName(), value);
                }
            } else {
                logger.warn("Unknown game rule: {}", entry.getKey());
            }
        }
    }

    /**
     * Exports game rule values to a map.
     * @return Map of rule names to values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<GameRule, Object> entry : gameRuleValues.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue());
        }
        return result;
    }

    /**
     * Gets the value of a game rule.
     * @param rule The game rule
     * @return The current value of the rule
     */
    public Object getValue(GameRule rule) {
        return gameRuleValues.get(rule);
    }

    /**
     * Gets the value of a game rule by name.
     * @param ruleName The name of the game rule
     * @return The current value of the rule, or null if the rule doesn't exist
     */
    public Object getValue(String ruleName) {
        GameRule rule = GameRule.getByName(ruleName);
        return rule != null ? getValue(rule) : null;
    }

    /**
     * Gets a boolean game rule value.
     * @param rule The game rule
     * @return The boolean value
     * @throws IllegalArgumentException if the rule is not a boolean type
     */
    public boolean getBooleanValue(GameRule rule) {
        if (rule.getType() != GameRuleType.BOOLEAN) {
            throw new IllegalArgumentException("Game rule " + rule.getName() + " is not a boolean");
        }
        return (Boolean) getValue(rule);
    }

    /**
     * Gets an integer game rule value.
     * @param rule The game rule
     * @return The integer value
     * @throws IllegalArgumentException if the rule is not an integer type
     */
    public int getIntValue(GameRule rule) {
        if (rule.getType() != GameRuleType.INTEGER) {
            throw new IllegalArgumentException("Game rule " + rule.getName() + " is not an integer");
        }
        return (Integer) getValue(rule);
    }

    /**
     * Gets a float game rule value.
     * @param rule The game rule
     * @return The float value
     * @throws IllegalArgumentException if the rule is not a float type
     */
    public float getFloatValue(GameRule rule) {
        if (rule.getType() != GameRuleType.FLOAT) {
            throw new IllegalArgumentException("Game rule " + rule.getName() + " is not a float");
        }
        Object value = getValue(rule);
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        }
        throw new IllegalStateException("Unexpected value type for float rule: " + value.getClass());
    }

    /**
     * Gets a string game rule value.
     * @param rule The game rule
     * @return The string value
     * @throws IllegalArgumentException if the rule is not a string type
     */
    public String getStringValue(GameRule rule) {
        if (rule.getType() != GameRuleType.STRING) {
            throw new IllegalArgumentException("Game rule " + rule.getName() + " is not a string");
        }
        return (String) getValue(rule);
    }

    /**
     * Sets the value of a game rule.
     * @param rule The game rule
     * @param value The new value
     * @return true if the value was set successfully, false if invalid
     */
    public boolean setValue(GameRule rule, Object value) {
        if (!rule.isValidValue(value)) {
            logger.warn("Invalid value for game rule {}: {}", rule.getName(), value);
            return false;
        }

        Object oldValue = gameRuleValues.get(rule);
        if (!Objects.equals(oldValue, value)) {
            gameRuleValues.put(rule, value);
            notifyListeners(rule, oldValue, value);
            logger.info("Game rule {} changed from {} to {}", rule.getName(), oldValue, value);
        }
        return true;
    }

    /**
     * Sets the value of a game rule by name.
     * @param ruleName The name of the game rule
     * @param value The new value
     * @return true if the value was set successfully, false if invalid or rule doesn't exist
     */
    public boolean setValue(String ruleName, Object value) {
        GameRule rule = GameRule.getByName(ruleName);
        if (rule == null) {
            logger.warn("Unknown game rule: {}", ruleName);
            return false;
        }
        return setValue(rule, value);
    }

    /**
     * Sets the value of a game rule from a string.
     * @param rule The game rule
     * @param stringValue The string representation of the value
     * @return true if the value was parsed and set successfully, false otherwise
     */
    public boolean setValueFromString(GameRule rule, String stringValue) {
        Object value = rule.parseValue(stringValue);
        if (value == null) {
            logger.warn("Failed to parse value for game rule {}: {}", rule.getName(), stringValue);
            return false;
        }
        return setValue(rule, value);
    }

    /**
     * Sets the value of a game rule from a string by name.
     * @param ruleName The name of the game rule
     * @param stringValue The string representation of the value
     * @return true if the value was parsed and set successfully, false otherwise
     */
    public boolean setValueFromString(String ruleName, String stringValue) {
        GameRule rule = GameRule.getByName(ruleName);
        if (rule == null) {
            logger.warn("Unknown game rule: {}", ruleName);
            return false;
        }
        return setValueFromString(rule, stringValue);
    }

    /**
     * Resets a game rule to its default value.
     * @param rule The game rule to reset
     */
    public void resetToDefault(GameRule rule) {
        setValue(rule, rule.getDefaultValue());
    }

    /**
     * Resets all game rules to their default values.
     */
    public void resetAllToDefaults() {
        for (GameRule rule : GameRule.values()) {
            resetToDefault(rule);
        }
    }

    /**
     * Gets all game rules and their current values.
     * @return Map of game rules to their current values
     */
    public Map<GameRule, Object> getAllValues() {
        return new HashMap<>(gameRuleValues);
    }

    /**
     * Gets all game rules of a specific type.
     * @param type The game rule type
     * @return List of game rules of the specified type
     */
    public List<GameRule> getRulesByType(GameRuleType type) {
        List<GameRule> result = new ArrayList<>();
        for (GameRule rule : GameRule.values()) {
            if (rule.getType() == type) {
                result.add(rule);
            }
        }
        return result;
    }

    /**
     * Adds a listener for game rule changes.
     * @param listener The listener to add
     */
    public void addListener(GameRuleChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for game rule changes.
     * @param listener The listener to remove
     */
    public void removeListener(GameRuleChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all listeners of a game rule change.
     * @param rule The game rule that changed
     * @param oldValue The previous value
     * @param newValue The new value
     */
    private void notifyListeners(GameRule rule, Object oldValue, Object newValue) {
        for (GameRuleChangeListener listener : listeners) {
            try {
                listener.onGameRuleChanged(rule, oldValue, newValue);
            } catch (Exception e) {
                logger.error("Error notifying game rule change listener", e);
            }
        }
    }

    /**
     * Gets a formatted string representation of a game rule value.
     * @param rule The game rule
     * @return Formatted string representation
     */
    public String getValueAsString(GameRule rule) {
        Object value = getValue(rule);
        return rule.getType().toString(value);
    }

    /**
     * Checks if a game rule exists.
     * @param ruleName The name of the game rule
     * @return true if the rule exists, false otherwise
     */
    public boolean hasRule(String ruleName) {
        return GameRule.getByName(ruleName) != null;
    }
}