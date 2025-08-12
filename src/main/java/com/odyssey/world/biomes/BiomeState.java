package com.odyssey.world.biomes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the dynamic state of a biome instance.
 * Handles temporary effects, state changes, and event tracking.
 */
public class BiomeState {
    private final Map<String, TemporaryEffect> temporaryEffects;
    private final Map<String, Object> stateData;
    private final List<StateEvent> eventHistory;
    private final Set<String> activeFlags;
    
    private float stateStability = 1.0f;
    private float changeRate = 0.0f;
    private long lastEventTime = System.currentTimeMillis();
    
    public BiomeState() {
        this.temporaryEffects = new ConcurrentHashMap<>();
        this.stateData = new ConcurrentHashMap<>();
        this.eventHistory = new ArrayList<>();
        this.activeFlags = ConcurrentHashMap.newKeySet();
        
        initializeDefaultState();
    }
    
    /**
     * Initializes default state values
     */
    private void initializeDefaultState() {
        stateData.put("pollution_level", 0.0f);
        stateData.put("disturbance_level", 0.0f);
        stateData.put("recovery_rate", 1.0f);
        stateData.put("adaptation_level", 0.5f);
        stateData.put("stress_level", 0.0f);
        stateData.put("biodiversity_index", 0.5f);
        stateData.put("ecosystem_health", 1.0f);
        stateData.put("human_impact", 0.0f);
        stateData.put("natural_events", 0.0f);
        stateData.put("climate_stress", 0.0f);
    }
    
    /**
     * Updates the biome state
     */
    public void update(float deltaTime) {
        updateTemporaryEffects(deltaTime);
        updateStateStability(deltaTime);
        updateStateData(deltaTime);
        cleanupOldEvents();
    }
    
    /**
     * Updates temporary effects
     */
    private void updateTemporaryEffects(float deltaTime) {
        Iterator<Map.Entry<String, TemporaryEffect>> iterator = temporaryEffects.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, TemporaryEffect> entry = iterator.next();
            TemporaryEffect effect = entry.getValue();
            
            effect.update(deltaTime);
            
            if (effect.isExpired()) {
                iterator.remove();
                recordEvent("effect_expired", effect.getName());
            }
        }
    }
    
    /**
     * Updates state stability
     */
    private void updateStateStability(float deltaTime) {
        float targetStability = 1.0f;
        
        // Reduce stability based on active effects
        for (TemporaryEffect effect : temporaryEffects.values()) {
            if (effect.isNegative()) {
                targetStability -= effect.getIntensity() * 0.2f;
            }
        }
        
        // Reduce stability based on stress
        float stressLevel = getStateValue("stress_level", Float.class, 0.0f);
        targetStability -= stressLevel * 0.3f;
        
        // Gradual change towards target
        float stabilityChange = (targetStability - stateStability) * deltaTime * 0.1f;
        stateStability = Math.max(0.0f, Math.min(1.0f, stateStability + stabilityChange));
        
        // Update change rate
        changeRate = Math.abs(stabilityChange) / deltaTime;
    }
    
    /**
     * Updates state data values
     */
    private void updateStateData(float deltaTime) {
        // Update pollution level (natural decay)
        float pollutionLevel = getStateValue("pollution_level", Float.class, 0.0f);
        float recoveryRate = getStateValue("recovery_rate", Float.class, 1.0f);
        pollutionLevel = Math.max(0.0f, pollutionLevel - deltaTime * 0.01f * recoveryRate);
        stateData.put("pollution_level", pollutionLevel);
        
        // Update disturbance level (natural decay)
        float disturbanceLevel = getStateValue("disturbance_level", Float.class, 0.0f);
        disturbanceLevel = Math.max(0.0f, disturbanceLevel - deltaTime * 0.02f);
        stateData.put("disturbance_level", disturbanceLevel);
        
        // Update stress level
        float stressLevel = pollutionLevel * 0.5f + disturbanceLevel * 0.3f;
        for (TemporaryEffect effect : temporaryEffects.values()) {
            if (effect.isNegative()) {
                stressLevel += effect.getIntensity() * 0.2f;
            }
        }
        stateData.put("stress_level", Math.max(0.0f, Math.min(1.0f, stressLevel)));
        
        // Update ecosystem health
        float ecosystemHealth = 1.0f - stressLevel * 0.5f;
        ecosystemHealth = Math.max(0.0f, Math.min(1.0f, ecosystemHealth));
        stateData.put("ecosystem_health", ecosystemHealth);
        
        // Update biodiversity index
        float biodiversityIndex = getStateValue("biodiversity_index", Float.class, 0.5f);
        if (ecosystemHealth > 0.7f) {
            biodiversityIndex += deltaTime * 0.001f;
        } else if (ecosystemHealth < 0.3f) {
            biodiversityIndex -= deltaTime * 0.002f;
        }
        biodiversityIndex = Math.max(0.0f, Math.min(1.0f, biodiversityIndex));
        stateData.put("biodiversity_index", biodiversityIndex);
        
        // Update adaptation level
        float adaptationLevel = getStateValue("adaptation_level", Float.class, 0.5f);
        if (stressLevel > 0.5f) {
            adaptationLevel += deltaTime * 0.0005f; // Slow adaptation to stress
        }
        adaptationLevel = Math.max(0.0f, Math.min(1.0f, adaptationLevel));
        stateData.put("adaptation_level", adaptationLevel);
    }
    
    /**
     * Adds a temporary effect
     */
    public void addTemporaryEffect(String name, float duration, Map<String, Object> effectData) {
        TemporaryEffect effect = new TemporaryEffect(name, duration, effectData);
        temporaryEffects.put(name, effect);
        recordEvent("effect_added", name);
    }
    
    /**
     * Removes a temporary effect
     */
    public void removeTemporaryEffect(String name) {
        if (temporaryEffects.remove(name) != null) {
            recordEvent("effect_removed", name);
        }
    }
    
    /**
     * Checks if a temporary effect is active
     */
    public boolean hasTemporaryEffect(String name) {
        return temporaryEffects.containsKey(name);
    }
    
    /**
     * Gets a temporary effect
     */
    public TemporaryEffect getTemporaryEffect(String name) {
        return temporaryEffects.get(name);
    }
    
    /**
     * Gets all active temporary effects
     */
    public Collection<TemporaryEffect> getActiveEffects() {
        return Collections.unmodifiableCollection(temporaryEffects.values());
    }
    
    /**
     * Sets a state value
     */
    public void setStateValue(String key, Object value) {
        Object oldValue = stateData.put(key, value);
        if (!Objects.equals(oldValue, value)) {
            recordEvent("state_changed", key);
        }
    }
    
    /**
     * Gets a state value with type safety
     */
    @SuppressWarnings("unchecked")
    public <T> T getStateValue(String key, Class<T> type, T defaultValue) {
        Object value = stateData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Gets a state value
     */
    public Object getStateValue(String key) {
        return stateData.get(key);
    }
    
    /**
     * Checks if state has a value
     */
    public boolean hasStateValue(String key) {
        return stateData.containsKey(key);
    }
    
    /**
     * Adds a state flag
     */
    public void addFlag(String flag) {
        if (activeFlags.add(flag)) {
            recordEvent("flag_added", flag);
        }
    }
    
    /**
     * Removes a state flag
     */
    public void removeFlag(String flag) {
        if (activeFlags.remove(flag)) {
            recordEvent("flag_removed", flag);
        }
    }
    
    /**
     * Checks if a flag is active
     */
    public boolean hasFlag(String flag) {
        return activeFlags.contains(flag);
    }
    
    /**
     * Gets all active flags
     */
    public Set<String> getActiveFlags() {
        return Collections.unmodifiableSet(activeFlags);
    }
    
    /**
     * Records a state event
     */
    private void recordEvent(String eventType, String details) {
        StateEvent event = new StateEvent(eventType, details, System.currentTimeMillis());
        eventHistory.add(event);
        lastEventTime = event.getTimestamp();
    }
    
    /**
     * Cleans up old events
     */
    private void cleanupOldEvents() {
        long currentTime = System.currentTimeMillis();
        long maxAge = 24 * 60 * 60 * 1000; // 24 hours
        
        eventHistory.removeIf(event -> currentTime - event.getTimestamp() > maxAge);
    }
    
    /**
     * Gets recent events
     */
    public List<StateEvent> getRecentEvents(int maxCount) {
        int size = eventHistory.size();
        int fromIndex = Math.max(0, size - maxCount);
        return new ArrayList<>(eventHistory.subList(fromIndex, size));
    }
    
    /**
     * Gets events of a specific type
     */
    public List<StateEvent> getEventsByType(String eventType) {
        return eventHistory.stream()
            .filter(event -> event.getEventType().equals(eventType))
            .collect(ArrayList::new, (list, event) -> list.add(event), ArrayList::addAll);
    }
    
    /**
     * Applies environmental stress
     */
    public void applyStress(String source, float intensity, float duration) {
        Map<String, Object> stressData = new HashMap<>();
        stressData.put("source", source);
        stressData.put("intensity", intensity);
        stressData.put("negative", true);
        
        addTemporaryEffect("stress_" + source, duration, stressData);
        
        // Increase disturbance level
        float disturbanceLevel = getStateValue("disturbance_level", Float.class, 0.0f);
        disturbanceLevel = Math.min(1.0f, disturbanceLevel + intensity * 0.5f);
        setStateValue("disturbance_level", disturbanceLevel);
    }
    
    /**
     * Applies pollution
     */
    public void applyPollution(String source, float amount) {
        float pollutionLevel = getStateValue("pollution_level", Float.class, 0.0f);
        pollutionLevel = Math.min(1.0f, pollutionLevel + amount);
        setStateValue("pollution_level", pollutionLevel);
        
        recordEvent("pollution_added", source + ":" + amount);
    }
    
    /**
     * Applies healing/recovery effect
     */
    public void applyHealing(String source, float intensity, float duration) {
        Map<String, Object> healingData = new HashMap<>();
        healingData.put("source", source);
        healingData.put("intensity", intensity);
        healingData.put("negative", false);
        
        addTemporaryEffect("healing_" + source, duration, healingData);
    }
    
    // Getters
    public float getStateStability() { return stateStability; }
    public float getChangeRate() { return changeRate; }
    public long getLastEventTime() { return lastEventTime; }
    public int getActiveEffectCount() { return temporaryEffects.size(); }
    public int getEventHistorySize() { return eventHistory.size(); }
    
    @Override
    public String toString() {
        return String.format("BiomeState{stability=%.2f, effects=%d, stress=%.2f, health=%.2f}", 
            stateStability, temporaryEffects.size(), 
            getStateValue("stress_level", Float.class, 0.0f),
            getStateValue("ecosystem_health", Float.class, 1.0f));
    }
    
    /**
     * Temporary effect class
     */
    public static class TemporaryEffect {
        private final String name;
        private final Map<String, Object> data;
        private float remainingDuration;
        private final float originalDuration;
        
        public TemporaryEffect(String name, float duration, Map<String, Object> data) {
            this.name = name;
            this.remainingDuration = duration;
            this.originalDuration = duration;
            this.data = new HashMap<>(data);
        }
        
        public void update(float deltaTime) {
            remainingDuration -= deltaTime;
        }
        
        public boolean isExpired() {
            return remainingDuration <= 0.0f;
        }
        
        public boolean isNegative() {
            return (Boolean) data.getOrDefault("negative", false);
        }
        
        public float getIntensity() {
            return (Float) data.getOrDefault("intensity", 1.0f);
        }
        
        public float getProgress() {
            return 1.0f - (remainingDuration / originalDuration);
        }
        
        // Getters
        public String getName() { return name; }
        public float getRemainingDuration() { return remainingDuration; }
        public float getOriginalDuration() { return originalDuration; }
        public Map<String, Object> getData() { return Collections.unmodifiableMap(data); }
        
        @Override
        public String toString() {
            return String.format("TemporaryEffect{name='%s', remaining=%.1f, intensity=%.2f, negative=%s}", 
                name, remainingDuration, getIntensity(), isNegative());
        }
    }
    
    /**
     * State event class
     */
    public static class StateEvent {
        private final String eventType;
        private final String details;
        private final long timestamp;
        
        public StateEvent(String eventType, String details, long timestamp) {
            this.eventType = eventType;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getEventType() { return eventType; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("StateEvent{type='%s', details='%s', time=%d}", 
                eventType, details, timestamp);
        }
    }
}