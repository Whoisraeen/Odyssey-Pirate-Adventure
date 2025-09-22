package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import org.joml.Vector3f;
import java.util.*;

/**
 * Spring feature representing freshwater springs that provide clean water.
 * Springs are vital resources for survival and can have special properties.
 */
public class SpringFeature extends Feature {
    
    public enum SpringType {
        NATURAL("Natural Spring", "A natural freshwater spring", 1.0f, false),
        HOT_SPRING("Hot Spring", "A geothermally heated spring with healing properties", 1.5f, true),
        SACRED_SPRING("Sacred Spring", "A mystical spring with purifying properties", 2.0f, true),
        MINERAL_SPRING("Mineral Spring", "A spring rich in beneficial minerals", 1.3f, true),
        HIDDEN_SPRING("Hidden Spring", "A concealed spring in a secluded location", 1.2f, false);
        
        private final String displayName;
        private final String description;
        private final float qualityMultiplier;
        private final boolean hasSpecialProperties;
        
        SpringType(String displayName, String description, float qualityMultiplier, boolean hasSpecialProperties) {
            this.displayName = displayName;
            this.description = description;
            this.qualityMultiplier = qualityMultiplier;
            this.hasSpecialProperties = hasSpecialProperties;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public float getQualityMultiplier() { return qualityMultiplier; }
        public boolean hasSpecialProperties() { return hasSpecialProperties; }
    }
    
    private final SpringType springType;
    private final float flowRate; // Liters per minute
    private final int waterQuality; // 1-10 scale
    private final Map<String, Integer> resources;
    private final boolean isAccessible;
    private final float temperature; // Celsius
    private int currentWaterLevel;
    private final int maxWaterLevel;
    private long lastRefillTime;
    
    public SpringFeature(float x, float y, float z, SpringType springType) {
        super(FeatureType.SPRING, x, y, z);
        this.springType = springType;
        this.resources = new HashMap<>();
        
        Random random = new Random((long)(x * 1000 + z * 1000));
        
        // Calculate properties based on spring type
        this.flowRate = 5.0f + random.nextFloat() * 15.0f * springType.qualityMultiplier;
        this.waterQuality = Math.min(10, (int)(6 + random.nextInt(3) * springType.qualityMultiplier));
        this.isAccessible = random.nextFloat() < 0.8f || springType == SpringType.HIDDEN_SPRING;
        
        // Set temperature based on spring type
        if (springType == SpringType.HOT_SPRING) {
            this.temperature = 35.0f + random.nextFloat() * 25.0f; // 35-60°C
        } else {
            this.temperature = 10.0f + random.nextFloat() * 15.0f; // 10-25°C
        }
        
        // Calculate water capacity
        this.maxWaterLevel = (int)(flowRate * 10 * springType.qualityMultiplier);
        this.currentWaterLevel = maxWaterLevel;
        this.lastRefillTime = System.currentTimeMillis();
        
        initializeResources();
    }
    
    public SpringFeature(Vector3f position, SpringType springType) {
        this(position.x, position.y, position.z, springType);
    }
    
    /**
     * Initialize the resources available from this spring
     */
    private void initializeResources() {
        Random random = new Random((long)(position.x * 1000 + position.z * 1000));
        
        // Base water resource
        resources.put("FRESH_WATER", currentWaterLevel);
        
        // Special resources based on spring type
        switch (springType) {
            case HOT_SPRING:
                resources.put("HEALING_WATER", 10 + random.nextInt(20));
                break;
            case SACRED_SPRING:
                resources.put("BLESSED_WATER", 5 + random.nextInt(10));
                resources.put("PURIFIED_WATER", 15 + random.nextInt(25));
                break;
            case MINERAL_SPRING:
                resources.put("MINERAL_WATER", 20 + random.nextInt(30));
                resources.put("SALT", 5 + random.nextInt(10));
                break;
            case HIDDEN_SPRING:
                resources.put("PURE_WATER", 25 + random.nextInt(35));
                break;
            default:
                // Natural spring has only basic fresh water
                break;
        }
        
        // Rare chance for special items
        if (random.nextFloat() < 0.1f) {
            resources.put("WATER_CRYSTAL", 1 + random.nextInt(3));
        }
    }
    
    /**
     * Refill the spring based on flow rate and time passed
     */
    private void refillSpring() {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastRefillTime;
        
        if (timePassed > 60000) { // Refill every minute
            float minutesPassed = timePassed / 60000.0f;
            int refillAmount = (int)(flowRate * minutesPassed);
            
            currentWaterLevel = Math.min(maxWaterLevel, currentWaterLevel + refillAmount);
            resources.put("FRESH_WATER", currentWaterLevel);
            
            lastRefillTime = currentTime;
        }
    }
    
    @Override
    public void onInteract() {
        refillSpring();
        
        if (!discovered) {
            onDiscover();
            System.out.println("You discovered a " + springType.displayName + "!");
            System.out.println(springType.description);
        } else {
            System.out.println("You approach the familiar " + springType.displayName.toLowerCase() + ".");
        }
        
        System.out.println("Water level: " + currentWaterLevel + "/" + maxWaterLevel + " liters");
        System.out.println("Water quality: " + waterQuality + "/10");
        System.out.println("Temperature: " + String.format("%.1f", temperature) + "°C");
    }
    
    @Override
    public String[] getAvailableResources() {
        refillSpring();
        return resources.keySet().toArray(new String[0]);
    }
    
    @Override
    public boolean extractResource(String resourceType, int amount) {
        refillSpring();
        
        Integer available = resources.get(resourceType);
        if (available != null && available >= amount) {
            resources.put(resourceType, available - amount);
            
            // Update current water level if extracting water
            if (resourceType.contains("WATER")) {
                currentWaterLevel = Math.max(0, currentWaterLevel - amount);
            }
            
            return true;
        }
        return false;
    }
    
    @Override
    public boolean requiresSpecialAccess() {
        return !isAccessible || springType == SpringType.HIDDEN_SPRING;
    }
    
    @Override
    public int getDangerLevel() {
        // Hot springs can be dangerous due to temperature
        if (springType == SpringType.HOT_SPRING && temperature > 50.0f) {
            return 2;
        }
        return 0;
    }
    
    /**
     * Check if the spring provides healing benefits
     */
    public boolean providesHealing() {
        return springType.hasSpecialProperties && 
               (springType == SpringType.HOT_SPRING || springType == SpringType.SACRED_SPRING);
    }
    
    /**
     * Check if the spring water is safe to drink
     */
    public boolean isSafeToDrink() {
        return waterQuality >= 6 && temperature < 45.0f;
    }
    
    /**
     * Get the healing amount provided by this spring
     */
    public int getHealingAmount() {
        if (!providesHealing()) return 0;
        
        switch (springType) {
            case HOT_SPRING:
                return 15 + (waterQuality * 2);
            case SACRED_SPRING:
                return 25 + (waterQuality * 3);
            default:
                return 0;
        }
    }
    
    /**
     * Get the current water availability as a percentage
     */
    public float getWaterAvailability() {
        refillSpring();
        return (float) currentWaterLevel / maxWaterLevel;
    }
    
    /**
     * Check if the spring is currently dry
     */
    public boolean isDry() {
        refillSpring();
        return currentWaterLevel <= 0;
    }
    
    /**
     * Get the time until the spring is fully refilled (in minutes)
     */
    public float getTimeToRefill() {
        if (currentWaterLevel >= maxWaterLevel) return 0.0f;
        
        int waterNeeded = maxWaterLevel - currentWaterLevel;
        return waterNeeded / flowRate;
    }
    
    // Getters
    public SpringType getSpringType() { return springType; }
    public float getFlowRate() { return flowRate; }
    public int getWaterQuality() { return waterQuality; }
    public boolean isAccessible() { return isAccessible; }
    public float getTemperature() { return temperature; }
    public int getCurrentWaterLevel() { 
        refillSpring();
        return currentWaterLevel; 
    }
    public int getMaxWaterLevel() { return maxWaterLevel; }
    
    @Override
    public String toString() {
        return String.format("%s at (%.1f, %.1f, %.1f) - Quality: %d/10, Flow: %.1f L/min, Temp: %.1f°C", 
                           springType.displayName, position.x, position.y, position.z, 
                           waterQuality, flowRate, temperature);
    }
}