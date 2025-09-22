package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import org.joml.Vector3f;

/**
 * Volcanic crater feature representing the central crater of a volcano.
 * May be active or dormant, affecting the surrounding environment.
 */
public class VolcanicCrater extends Feature {
    
    public enum ActivityLevel {
        DORMANT("Dormant", "No current volcanic activity", 0.0f),
        SMOKING("Smoking", "Minor steam and gas emissions", 0.3f),
        ACTIVE("Active", "Regular eruptions and lava flows", 0.7f),
        ERUPTING("Erupting", "Major eruption in progress", 1.0f);
        
        private final String displayName;
        private final String description;
        private final float dangerLevel;
        
        ActivityLevel(String displayName, String description, float dangerLevel) {
            this.displayName = displayName;
            this.description = description;
            this.dangerLevel = dangerLevel;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public float getDangerLevel() { return dangerLevel; }
    }
    
    private float radius;
    private float depth;
    private ActivityLevel activityLevel;
    private boolean hasLava;
    private float temperature;
    private float gasEmissionRate;
    private long lastEruptionTime;
    private float eruptionFrequency;
    
    public VolcanicCrater(float x, float y, float z, float radius, float depth) {
        super(FeatureType.VOLCANIC_CRATER, x, y, z);
        this.radius = Math.max(5.0f, radius);
        this.depth = Math.max(2.0f, depth);
        this.activityLevel = ActivityLevel.DORMANT;
        this.hasLava = false;
        this.temperature = 20.0f; // Ambient temperature
        this.gasEmissionRate = 0.0f;
        this.lastEruptionTime = System.currentTimeMillis();
        this.eruptionFrequency = 0.1f; // 10% chance per hour
    }
    
    public VolcanicCrater(Vector3f position, float radius, float depth) {
        this(position.x, position.y, position.z, radius, depth);
    }
    
    /**
     * Updates the crater's activity level and effects
     */
    public void update(float deltaTime) {
        // Check for eruption events
        if (activityLevel != ActivityLevel.ERUPTING) {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastEruption = currentTime - lastEruptionTime;
            
            // Random eruption based on frequency
            if (Math.random() < eruptionFrequency * deltaTime / 3600.0f) {
                triggerEruption();
            }
        }
        
        // Update temperature based on activity
        updateTemperature(deltaTime);
        
        // Update gas emissions
        updateGasEmissions(deltaTime);
    }
    
    /**
     * Triggers a volcanic eruption
     */
    public void triggerEruption() {
        if (activityLevel == ActivityLevel.ERUPTING) return;
        
        activityLevel = ActivityLevel.ERUPTING;
        hasLava = true;
        temperature = 1000.0f; // Lava temperature
        gasEmissionRate = 1.0f;
        lastEruptionTime = System.currentTimeMillis();
        
        // Eruption duration (1-6 hours)
        long eruptionDuration = (long)(Math.random() * 5 + 1) * 3600000L;
        
        // Schedule end of eruption
        new Thread(() -> {
            try {
                Thread.sleep(eruptionDuration);
                endEruption();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Ends the current eruption
     */
    private void endEruption() {
        activityLevel = ActivityLevel.SMOKING;
        hasLava = false;
        temperature = 200.0f; // Cooling lava
        gasEmissionRate = 0.3f;
    }
    
    /**
     * Updates the crater's temperature
     */
    private void updateTemperature(float deltaTime) {
        float targetTemperature = getTargetTemperature();
        float coolingRate = 0.1f; // Degrees per second
        
        if (temperature > targetTemperature) {
            temperature = Math.max(targetTemperature, temperature - coolingRate * deltaTime);
        } else if (temperature < targetTemperature) {
            temperature = Math.min(targetTemperature, temperature + coolingRate * deltaTime);
        }
    }
    
    /**
     * Gets the target temperature based on activity level
     */
    private float getTargetTemperature() {
        switch (activityLevel) {
            case DORMANT: return 20.0f;
            case SMOKING: return 100.0f;
            case ACTIVE: return 300.0f;
            case ERUPTING: return 1000.0f;
            default: return 20.0f;
        }
    }
    
    /**
     * Updates gas emissions
     */
    private void updateGasEmissions(float deltaTime) {
        float targetEmissionRate = getTargetGasEmissionRate();
        float changeRate = 0.05f;
        
        if (gasEmissionRate > targetEmissionRate) {
            gasEmissionRate = Math.max(targetEmissionRate, gasEmissionRate - changeRate * deltaTime);
        } else if (gasEmissionRate < targetEmissionRate) {
            gasEmissionRate = Math.min(targetEmissionRate, gasEmissionRate + changeRate * deltaTime);
        }
    }
    
    /**
     * Gets the target gas emission rate
     */
    private float getTargetGasEmissionRate() {
        switch (activityLevel) {
            case DORMANT: return 0.0f;
            case SMOKING: return 0.3f;
            case ACTIVE: return 0.7f;
            case ERUPTING: return 1.0f;
            default: return 0.0f;
        }
    }
    
    /**
     * Checks if the crater is safe to approach
     */
    public boolean isSafeToApproach() {
        return activityLevel == ActivityLevel.DORMANT && temperature < 50.0f;
    }
    
    /**
     * Gets the danger level (0-10 scale)
     */
    @Override
    public int getDangerLevel() {
        float danger = activityLevel.getDangerLevel() * 10.0f;
        if (hasLava) {
            danger += 2.0f;
        }
        if (temperature > 100.0f) {
            danger += 1.0f;
        }
        return Math.min(10, (int)danger);
    }
    
    /**
     * Called when a player or entity interacts with this feature
     */
    @Override
    public void onInteract() {
        // Volcanic craters are dangerous to interact with directly
        if (activityLevel == ActivityLevel.ERUPTING || activityLevel == ActivityLevel.ACTIVE) {
            // Player takes damage from heat and toxic gases
            System.out.println("The volcanic crater is too dangerous to approach!");
        } else {
            // Player can observe the crater safely
            System.out.println("You observe the " + activityLevel.getDisplayName().toLowerCase() + " volcanic crater.");
        }
    }
    
    /**
     * Get the resources that can be extracted from this feature
     */
    @Override
    public String[] getAvailableResources() {
        if (activityLevel == ActivityLevel.DORMANT) {
            return new String[]{"Volcanic Rock", "Sulfur", "Obsidian"};
        } else if (activityLevel == ActivityLevel.SMOKING) {
            return new String[]{"Sulfur", "Volcanic Ash"};
        } else {
            return new String[]{}; // Too dangerous to extract resources
        }
    }
    
    /**
     * Extract resources from this feature
     */
    @Override
    public boolean extractResource(String resourceType, int amount) {
        if (activityLevel == ActivityLevel.ACTIVE || activityLevel == ActivityLevel.ERUPTING) {
            return false; // Too dangerous
        }
        
        String[] availableResources = getAvailableResources();
        for (String resource : availableResources) {
            if (resource.equals(resourceType)) {
                // Simulate resource extraction
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the visibility reduction due to gases
     */
    public float getVisibilityReduction() {
        return gasEmissionRate * 0.4f;
    }
    
    /**
     * Gets the air quality (0.0 = toxic, 1.0 = clean)
     */
    public float getAirQuality() {
        return Math.max(0.0f, 1.0f - gasEmissionRate * 0.8f);
    }
    
    // Getters and setters
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = Math.max(5.0f, radius); }
    
    public float getDepth() { return depth; }
    public void setDepth(float depth) { this.depth = Math.max(2.0f, depth); }
    
    public ActivityLevel getActivityLevel() { return activityLevel; }
    public void setActivityLevel(ActivityLevel activityLevel) { this.activityLevel = activityLevel; }
    
    public boolean hasLava() { return hasLava; }
    public void setHasLava(boolean hasLava) { this.hasLava = hasLava; }
    
    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }
    
    public float getGasEmissionRate() { return gasEmissionRate; }
    public void setGasEmissionRate(float gasEmissionRate) { this.gasEmissionRate = gasEmissionRate; }
    
    public long getLastEruptionTime() { return lastEruptionTime; }
    public void setLastEruptionTime(long lastEruptionTime) { this.lastEruptionTime = lastEruptionTime; }
    
    public float getEruptionFrequency() { return eruptionFrequency; }
    public void setEruptionFrequency(float eruptionFrequency) { this.eruptionFrequency = eruptionFrequency; }
    
    @Override
    public String toString() {
        return String.format("VolcanicCrater[%s, radius=%.1f, depth=%.1f, temp=%.1f°C]",
                           activityLevel.getDisplayName(), radius, depth, temperature);
    }
}
