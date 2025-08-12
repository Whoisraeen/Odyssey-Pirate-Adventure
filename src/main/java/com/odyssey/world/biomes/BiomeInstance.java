package com.odyssey.world.biomes;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents an active instance of a biome with dynamic properties.
 * Contains runtime data and state that can change over time.
 */
public class BiomeInstance {
    private final BiomeType type;
    private final int centerX;
    private final int centerZ;
    private final BiomeVariant variant;
    private final Map<String, Object> dynamicProperties;
    private final List<ActiveFeature> activeFeatures;
    private final BiomeState state;
    
    private float timeOfDay = 0.0f;
    private float weatherIntensity = 0.5f;
    private float seasonalModifier = 1.0f;
    private long lastUpdateTime = System.currentTimeMillis();
    
    public BiomeInstance(BiomeType type, int centerX, int centerZ, BiomeVariant variant) {
        this.type = type;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.variant = variant;
        this.dynamicProperties = new HashMap<>();
        this.activeFeatures = new ArrayList<>();
        this.state = new BiomeState();
        
        initializeDynamicProperties();
        generateActiveFeatures();
    }
    
    /**
     * Initializes dynamic properties based on biome type
     */
    private void initializeDynamicProperties() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Environmental properties
        dynamicProperties.put("current_temperature", getEffectiveTemperature());
        dynamicProperties.put("current_humidity", getEffectiveHumidity());
        dynamicProperties.put("wind_speed", random.nextFloat() * 10.0f);
        dynamicProperties.put("wind_direction", random.nextFloat() * 360.0f);
        dynamicProperties.put("wave_height", type.getProperties().getWaveIntensity() * (0.5f + random.nextFloat()));
        dynamicProperties.put("current_direction", random.nextFloat() * 360.0f);
        dynamicProperties.put("visibility", type.getProperties().getVisibility() * (0.8f + random.nextFloat() * 0.4f));
        
        // Biological properties
        dynamicProperties.put("population_density", 0.5f + random.nextFloat() * 0.5f);
        dynamicProperties.put("food_availability", type.getProperties().getFertility() * (0.7f + random.nextFloat() * 0.6f));
        dynamicProperties.put("breeding_season", random.nextBoolean());
        dynamicProperties.put("migration_active", false);
        
        // Resource properties
        dynamicProperties.put("resource_richness", variant.getRarityBonus());
        dynamicProperties.put("mineral_deposits", type.hasFeature("mineral_deposits") ? random.nextFloat() : 0.0f);
        dynamicProperties.put("fresh_water", type.getCategory() == BiomeType.Category.ISLAND ? random.nextFloat() : 0.0f);
        
        // Special properties
        if (type.getProperties().getMysticalEnergy() > 0) {
            dynamicProperties.put("mystical_flux", random.nextFloat() * type.getProperties().getMysticalEnergy());
            dynamicProperties.put("ley_line_strength", random.nextFloat());
        }
        
        if (type.getProperties().getGeothermalActivity() > 0) {
            dynamicProperties.put("thermal_output", type.getProperties().getGeothermalActivity() * (0.8f + random.nextFloat() * 0.4f));
            dynamicProperties.put("volcanic_activity", random.nextFloat() * 0.3f);
        }
    }
    
    /**
     * Generates active features for this biome instance
     */
    private void generateActiveFeatures() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (Map.Entry<String, Float> feature : type.getFeatures().entrySet()) {
            String featureName = feature.getKey();
            float baseProbability = feature.getValue();
            float adjustedProbability = baseProbability * variant.getFeatureIntensity();
            
            if (random.nextFloat() < adjustedProbability) {
                ActiveFeature activeFeature = createActiveFeature(featureName, adjustedProbability);
                if (activeFeature != null) {
                    activeFeatures.add(activeFeature);
                }
            }
        }
    }
    
    /**
     * Creates an active feature instance
     */
    private ActiveFeature createActiveFeature(String featureName, float intensity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        ActiveFeature feature = new ActiveFeature(featureName, intensity);
        
        // Set feature-specific properties
        switch (featureName) {
            case "coral_reefs":
                feature.setProperty("health", 0.7f + random.nextFloat() * 0.3f);
                feature.setProperty("diversity", random.nextFloat());
                feature.setProperty("bleaching_risk", getEffectiveTemperature() > 0.8f ? 0.3f : 0.1f);
                break;
                
            case "kelp_forest":
            case "giant_kelp":
                feature.setProperty("density", 0.5f + random.nextFloat() * 0.5f);
                feature.setProperty("height", 5.0f + random.nextFloat() * 15.0f);
                feature.setProperty("growth_rate", getEffectiveTemperature() * 0.5f);
                break;
                
            case "shipwrecks":
                feature.setProperty("age", random.nextFloat() * 500.0f);
                feature.setProperty("condition", random.nextFloat());
                feature.setProperty("treasure_chance", variant.getRarityBonus() * 0.3f);
                feature.setProperty("haunted", random.nextFloat() < 0.1f);
                break;
                
            case "volcanic_activity":
            case "lava_flows":
                feature.setProperty("activity_level", random.nextFloat());
                feature.setProperty("temperature", 800.0f + random.nextFloat() * 400.0f);
                feature.setProperty("gas_emissions", random.nextFloat());
                break;
                
            case "ice_formations":
            case "icebergs":
                feature.setProperty("thickness", 1.0f + random.nextFloat() * 10.0f);
                feature.setProperty("stability", 0.5f + random.nextFloat() * 0.5f);
                feature.setProperty("melt_rate", Math.max(0.0f, getEffectiveTemperature() - 0.2f));
                break;
                
            case "mangrove_trees":
                feature.setProperty("root_density", 0.6f + random.nextFloat() * 0.4f);
                feature.setProperty("canopy_coverage", 0.7f + random.nextFloat() * 0.3f);
                feature.setProperty("wildlife_shelter", random.nextFloat());
                break;
                
            case "thermal_vents":
                feature.setProperty("temperature", 200.0f + random.nextFloat() * 200.0f);
                feature.setProperty("mineral_output", random.nextFloat());
                feature.setProperty("bacterial_mats", random.nextFloat());
                break;
                
            default:
                feature.setProperty("intensity", intensity);
                feature.setProperty("stability", 0.5f + random.nextFloat() * 0.5f);
                break;
        }
        
        return feature;
    }
    
    /**
     * Updates the biome instance
     */
    public void update(float deltaTime) {
        long currentTime = System.currentTimeMillis();
        float timeDelta = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        
        // Update environmental conditions
        updateEnvironmentalConditions(timeDelta);
        
        // Update active features
        updateActiveFeatures(timeDelta);
        
        // Update biological systems
        updateBiologicalSystems(timeDelta);
        
        // Update state
        state.update(timeDelta);
    }
    
    /**
     * Updates environmental conditions
     */
    private void updateEnvironmentalConditions(float deltaTime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Update weather
        float weatherChange = (random.nextFloat() - 0.5f) * deltaTime * 0.1f;
        weatherIntensity = Math.max(0.0f, Math.min(1.0f, weatherIntensity + weatherChange));
        
        // Update wind
        float windSpeed = (Float) dynamicProperties.get("wind_speed");
        float windChange = (random.nextFloat() - 0.5f) * deltaTime * 2.0f;
        windSpeed = Math.max(0.0f, Math.min(20.0f, windSpeed + windChange));
        dynamicProperties.put("wind_speed", windSpeed);
        
        // Update wind direction
        float windDirection = (Float) dynamicProperties.get("wind_direction");
        float directionChange = (random.nextFloat() - 0.5f) * deltaTime * 10.0f;
        windDirection = (windDirection + directionChange + 360.0f) % 360.0f;
        dynamicProperties.put("wind_direction", windDirection);
        
        // Update waves based on wind
        float baseWaveHeight = type.getProperties().getWaveIntensity();
        float windEffect = windSpeed / 20.0f;
        float waveHeight = baseWaveHeight * (1.0f + windEffect) * (0.8f + weatherIntensity * 0.4f);
        dynamicProperties.put("wave_height", waveHeight);
        
        // Update visibility based on weather
        float baseVisibility = type.getProperties().getVisibility();
        float weatherEffect = 1.0f - (weatherIntensity * 0.5f);
        float visibility = baseVisibility * weatherEffect;
        dynamicProperties.put("visibility", visibility);
        
        // Update temperature and humidity
        dynamicProperties.put("current_temperature", getEffectiveTemperature());
        dynamicProperties.put("current_humidity", getEffectiveHumidity());
    }
    
    /**
     * Updates active features
     */
    private void updateActiveFeatures(float deltaTime) {
        for (ActiveFeature feature : activeFeatures) {
            feature.update(deltaTime, this);
        }
    }
    
    /**
     * Updates biological systems
     */
    private void updateBiologicalSystems(float deltaTime) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        // Update population density
        float populationDensity = (Float) dynamicProperties.get("population_density");
        float foodAvailability = (Float) dynamicProperties.get("food_availability");
        float populationChange = (foodAvailability - populationDensity) * deltaTime * 0.01f;
        populationChange += (random.nextFloat() - 0.5f) * deltaTime * 0.005f;
        populationDensity = Math.max(0.0f, Math.min(1.0f, populationDensity + populationChange));
        dynamicProperties.put("population_density", populationDensity);
        
        // Update food availability based on season and fertility
        float fertility = type.getProperties().getFertility();
        float seasonalEffect = 0.8f + seasonalModifier * 0.4f;
        float newFoodAvailability = fertility * seasonalEffect * (0.9f + random.nextFloat() * 0.2f);
        dynamicProperties.put("food_availability", newFoodAvailability);
        
        // Check for migration events
        if (random.nextFloat() < 0.001f) { // 0.1% chance per update
            dynamicProperties.put("migration_active", !((Boolean) dynamicProperties.get("migration_active")));
        }
        
        // Update breeding season
        if (random.nextFloat() < 0.0005f) { // 0.05% chance per update
            dynamicProperties.put("breeding_season", !((Boolean) dynamicProperties.get("breeding_season")));
        }
    }
    
    /**
     * Gets effective temperature including modifiers
     */
    public float getEffectiveTemperature() {
        float baseTemp = type.getTemperature();
        float variantMod = variant.getTemperatureModifier();
        float seasonalMod = (seasonalModifier - 1.0f) * 0.2f;
        float timeOfDayMod = (float) Math.sin(timeOfDay * Math.PI * 2) * 0.1f;
        
        return baseTemp + variantMod + seasonalMod + timeOfDayMod;
    }
    
    /**
     * Gets effective humidity including modifiers
     */
    public float getEffectiveHumidity() {
        float baseHumidity = type.getHumidity();
        float variantMod = variant.getHumidityModifier();
        float weatherMod = weatherIntensity * 0.2f;
        
        return Math.max(0.0f, Math.min(1.0f, baseHumidity + variantMod + weatherMod));
    }
    
    /**
     * Gets effective elevation including modifiers
     */
    public float getEffectiveElevation() {
        float baseElevation = type.getElevation();
        float variantMod = variant.getElevationModifier();
        
        return baseElevation + variantMod;
    }
    
    /**
     * Gets a dynamic property value
     */
    @SuppressWarnings("unchecked")
    public <T> T getDynamicProperty(String key, Class<T> type) {
        Object value = dynamicProperties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Sets a dynamic property value
     */
    public void setDynamicProperty(String key, Object value) {
        dynamicProperties.put(key, value);
    }
    
    /**
     * Gets active features of a specific type
     */
    public List<ActiveFeature> getActiveFeatures(String featureType) {
        return activeFeatures.stream()
            .filter(feature -> feature.getName().equals(featureType))
            .collect(ArrayList::new, (list, feature) -> list.add(feature), ArrayList::addAll);
    }
    
    /**
     * Checks if biome has an active feature
     */
    public boolean hasActiveFeature(String featureType) {
        return activeFeatures.stream().anyMatch(feature -> feature.getName().equals(featureType));
    }
    
    /**
     * Gets the total intensity of a feature type
     */
    public float getFeatureIntensity(String featureType) {
        return (float) activeFeatures.stream()
            .filter(feature -> feature.getName().equals(featureType))
            .mapToDouble(ActiveFeature::getIntensity)
            .sum();
    }
    
    /**
     * Adds a temporary effect to the biome
     */
    public void addTemporaryEffect(String effectName, float duration, Map<String, Object> effectData) {
        state.addTemporaryEffect(effectName, duration, effectData);
    }
    
    /**
     * Removes a temporary effect
     */
    public void removeTemporaryEffect(String effectName) {
        state.removeTemporaryEffect(effectName);
    }
    
    /**
     * Checks if biome has a temporary effect
     */
    public boolean hasTemporaryEffect(String effectName) {
        return state.hasTemporaryEffect(effectName);
    }
    
    // Getters
    public BiomeType getType() { return type; }
    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public BiomeVariant getVariant() { return variant; }
    public List<ActiveFeature> getActiveFeatures() { return Collections.unmodifiableList(activeFeatures); }
    public BiomeState getState() { return state; }
    public float getTimeOfDay() { return timeOfDay; }
    public float getWeatherIntensity() { return weatherIntensity; }
    public float getSeasonalModifier() { return seasonalModifier; }
    
    // Setters
    public void setTimeOfDay(float timeOfDay) { this.timeOfDay = timeOfDay; }
    public void setWeatherIntensity(float weatherIntensity) { this.weatherIntensity = weatherIntensity; }
    public void setSeasonalModifier(float seasonalModifier) { this.seasonalModifier = seasonalModifier; }
    
    @Override
    public String toString() {
        return String.format("BiomeInstance{type=%s, center=(%d,%d), features=%d, temp=%.2f, humidity=%.2f}", 
            type.getName(), centerX, centerZ, activeFeatures.size(), getEffectiveTemperature(), getEffectiveHumidity());
    }
}