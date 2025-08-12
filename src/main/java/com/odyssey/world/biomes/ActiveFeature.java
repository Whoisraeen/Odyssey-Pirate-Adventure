package com.odyssey.world.biomes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents an active feature within a biome instance.
 * Features can change over time and respond to environmental conditions.
 */
public class ActiveFeature {
    private final String name;
    private float intensity;
    private final Map<String, Object> properties;
    private float age = 0.0f;
    private float health = 1.0f;
    private boolean isActive = true;
    
    public ActiveFeature(String name, float intensity) {
        this.name = name;
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        this.properties = new HashMap<>();
    }
    
    /**
     * Updates the feature
     */
    public void update(float deltaTime, BiomeInstance biome) {
        age += deltaTime;
        
        // Update based on feature type
        switch (name) {
            case "coral_reefs":
                updateCoralReef(deltaTime, biome);
                break;
            case "kelp_forest":
            case "giant_kelp":
                updateKelpForest(deltaTime, biome);
                break;
            case "volcanic_activity":
            case "lava_flows":
                updateVolcanicActivity(deltaTime, biome);
                break;
            case "ice_formations":
            case "icebergs":
                updateIceFormations(deltaTime, biome);
                break;
            case "mangrove_trees":
                updateMangroves(deltaTime, biome);
                break;
            case "shipwrecks":
                updateShipwrecks(deltaTime, biome);
                break;
            default:
                updateGenericFeature(deltaTime, biome);
                break;
        }
        
        // Update health based on environmental conditions
        updateHealth(deltaTime, biome);
    }
    
    /**
     * Updates coral reef features
     */
    private void updateCoralReef(float deltaTime, BiomeInstance biome) {
        float temperature = biome.getEffectiveTemperature();
        float bleachingRisk = getProperty("bleaching_risk", Float.class, 0.1f);
        
        // Temperature stress
        if (temperature > 0.8f) {
            bleachingRisk += deltaTime * 0.01f;
            health -= deltaTime * 0.02f;
        } else if (temperature < 0.6f) {
            bleachingRisk = Math.max(0.0f, bleachingRisk - deltaTime * 0.005f);
            health += deltaTime * 0.01f;
        }
        
        setProperty("bleaching_risk", Math.max(0.0f, Math.min(1.0f, bleachingRisk)));
        
        // Diversity changes
        float diversity = getProperty("diversity", Float.class, 0.5f);
        if (health > 0.7f) {
            diversity += deltaTime * 0.001f;
        } else if (health < 0.3f) {
            diversity -= deltaTime * 0.002f;
        }
        setProperty("diversity", Math.max(0.0f, Math.min(1.0f, diversity)));
    }
    
    /**
     * Updates kelp forest features
     */
    private void updateKelpForest(float deltaTime, BiomeInstance biome) {
        float temperature = biome.getEffectiveTemperature();
        float growthRate = getProperty("growth_rate", Float.class, 0.1f);
        float height = getProperty("height", Float.class, 10.0f);
        float density = getProperty("density", Float.class, 0.5f);
        
        // Optimal growth temperature
        if (temperature > 0.3f && temperature < 0.7f) {
            growthRate = temperature * 0.5f;
            height += growthRate * deltaTime;
            density += deltaTime * 0.001f;
        } else {
            growthRate *= 0.5f;
            if (temperature > 0.8f) {
                height -= deltaTime * 0.1f;
                density -= deltaTime * 0.002f;
            }
        }
        
        setProperty("growth_rate", growthRate);
        setProperty("height", Math.max(1.0f, Math.min(25.0f, height)));
        setProperty("density", Math.max(0.0f, Math.min(1.0f, density)));
    }
    
    /**
     * Updates volcanic activity features
     */
    private void updateVolcanicActivity(float deltaTime, BiomeInstance biome) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float activityLevel = getProperty("activity_level", Float.class, 0.5f);
        float temperature = getProperty("temperature", Float.class, 1000.0f);
        
        // Random volcanic events
        if (random.nextFloat() < 0.001f) {
            activityLevel += random.nextFloat() * 0.3f - 0.1f;
            activityLevel = Math.max(0.0f, Math.min(1.0f, activityLevel));
        }
        
        // Temperature fluctuations
        temperature += (random.nextFloat() - 0.5f) * deltaTime * 50.0f;
        temperature = Math.max(500.0f, Math.min(1500.0f, temperature));
        
        setProperty("activity_level", activityLevel);
        setProperty("temperature", temperature);
        
        // Gas emissions based on activity
        float gasEmissions = activityLevel * (0.5f + random.nextFloat() * 0.5f);
        setProperty("gas_emissions", gasEmissions);
    }
    
    /**
     * Updates ice formation features
     */
    private void updateIceFormations(float deltaTime, BiomeInstance biome) {
        float temperature = biome.getEffectiveTemperature();
        float thickness = getProperty("thickness", Float.class, 5.0f);
        float stability = getProperty("stability", Float.class, 0.5f);
        float meltRate = getProperty("melt_rate", Float.class, 0.0f);
        
        // Melting/freezing based on temperature
        if (temperature > 0.2f) {
            meltRate = (temperature - 0.2f) * 2.0f;
            thickness -= meltRate * deltaTime;
            stability -= deltaTime * 0.01f;
        } else {
            meltRate = 0.0f;
            thickness += deltaTime * 0.1f;
            stability += deltaTime * 0.005f;
        }
        
        setProperty("thickness", Math.max(0.1f, Math.min(20.0f, thickness)));
        setProperty("stability", Math.max(0.0f, Math.min(1.0f, stability)));
        setProperty("melt_rate", meltRate);
        
        // Feature becomes inactive if ice melts completely
        if (thickness < 0.5f) {
            isActive = false;
        }
    }
    
    /**
     * Updates mangrove features
     */
    private void updateMangroves(float deltaTime, BiomeInstance biome) {
        float humidity = biome.getEffectiveHumidity();
        float temperature = biome.getEffectiveTemperature();
        float rootDensity = getProperty("root_density", Float.class, 0.7f);
        float canopyCoverage = getProperty("canopy_coverage", Float.class, 0.8f);
        
        // Optimal conditions for mangroves
        if (humidity > 0.7f && temperature > 0.6f && temperature < 0.9f) {
            rootDensity += deltaTime * 0.001f;
            canopyCoverage += deltaTime * 0.0005f;
            health += deltaTime * 0.005f;
        } else {
            rootDensity -= deltaTime * 0.0005f;
            canopyCoverage -= deltaTime * 0.0002f;
            health -= deltaTime * 0.002f;
        }
        
        setProperty("root_density", Math.max(0.0f, Math.min(1.0f, rootDensity)));
        setProperty("canopy_coverage", Math.max(0.0f, Math.min(1.0f, canopyCoverage)));
        
        // Wildlife shelter capacity
        float wildlifeShelter = rootDensity * canopyCoverage;
        setProperty("wildlife_shelter", wildlifeShelter);
    }
    
    /**
     * Updates shipwreck features
     */
    private void updateShipwrecks(float deltaTime, BiomeInstance biome) {
        float condition = getProperty("condition", Float.class, 0.5f);
        float ageYears = getProperty("age", Float.class, 100.0f);
        
        // Deterioration over time
        condition -= deltaTime * 0.0001f; // Very slow deterioration
        ageYears += deltaTime / (365.25f * 24.0f * 3600.0f); // Convert to years
        
        setProperty("condition", Math.max(0.0f, condition));
        setProperty("age", ageYears);
        
        // Treasure chance decreases as condition worsens
        float treasureChance = getProperty("treasure_chance", Float.class, 0.1f);
        if (condition < 0.3f) {
            treasureChance *= 0.5f;
        }
        setProperty("treasure_chance", treasureChance);
        
        // Haunted status can change
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextFloat() < 0.0001f) {
            boolean haunted = getProperty("haunted", Boolean.class, false);
            setProperty("haunted", !haunted);
        }
    }
    
    /**
     * Updates generic features
     */
    private void updateGenericFeature(float deltaTime, BiomeInstance biome) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Small random fluctuations
        intensity += (random.nextFloat() - 0.5f) * deltaTime * 0.01f;
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        
        // Stability changes
        float stability = getProperty("stability", Float.class, 0.5f);
        stability += (random.nextFloat() - 0.5f) * deltaTime * 0.005f;
        stability = Math.max(0.0f, Math.min(1.0f, stability));
        setProperty("stability", stability);
    }
    
    /**
     * Updates feature health
     */
    private void updateHealth(float deltaTime, BiomeInstance biome) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Base health change
        float healthChange = (random.nextFloat() - 0.5f) * deltaTime * 0.001f;
        
        // Environmental factors
        float temperature = biome.getEffectiveTemperature();
        float humidity = biome.getEffectiveHumidity();
        
        // Most features prefer moderate conditions
        if (temperature > 0.3f && temperature < 0.8f && humidity > 0.4f && humidity < 0.9f) {
            healthChange += deltaTime * 0.002f;
        } else {
            healthChange -= deltaTime * 0.001f;
        }
        
        health = Math.max(0.0f, Math.min(1.0f, health + healthChange));
        
        // Feature becomes inactive if health is too low
        if (health < 0.1f) {
            isActive = false;
        }
    }
    
    /**
     * Gets a property value with type safety
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Sets a property value
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Gets a property value
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Checks if feature has a property
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    // Getters and setters
    public String getName() { return name; }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = Math.max(0.0f, Math.min(1.0f, intensity)); }
    public float getAge() { return age; }
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0.0f, Math.min(1.0f, health)); }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    @Override
    public String toString() {
        return String.format("ActiveFeature{name='%s', intensity=%.2f, health=%.2f, age=%.1f, active=%s}", 
            name, intensity, health, age, isActive);
    }
}