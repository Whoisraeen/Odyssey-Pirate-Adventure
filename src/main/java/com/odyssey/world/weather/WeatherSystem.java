package com.odyssey.world.weather;

import org.joml.Vector2f;
import com.odyssey.util.NoiseGenerator;
import com.odyssey.util.Logger;
import com.odyssey.world.WorldConfig;
import java.util.*;

/**
 * Manages dynamic weather patterns, wind systems, and atmospheric conditions
 */
public class WeatherSystem {
    
    private final long seed; // Used for noise generator initialization
    private final Random random;
    private final WorldConfig.WeatherConfig config;
    
    // Noise generators for weather patterns
    private final NoiseGenerator windNoise;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final NoiseGenerator pressureNoise;
    private final NoiseGenerator stormNoise;
    
    // Global weather patterns
    private final List<WeatherPattern> globalPatterns;
    private final List<Storm> activeStorms;
    private final Map<Vector2f, WeatherCell> weatherCells;
    
    // Weather state
    private float globalTime = 0;
    private float seasonalFactor = 0; // 0-1 representing seasonal variation
    private Vector2f globalWindDirection = new Vector2f(1, 0);
    private float globalWindStrength = 5.0f;
    
    // Weather cell size (in world units) - reserved for future weather cell implementation
    private static final float CELL_SIZE = 100.0f;
    private static final float STORM_DURATION = 300.0f; // 5 minutes
    private static final float STORM_SPAWN_CHANCE = 0.001f;
    
    public WeatherSystem(long seed, WorldConfig.WeatherConfig config) {
        this.seed = seed;
        this.config = config;
        this.random = new Random(seed);
        
        // Initialize noise generators
        this.windNoise = new NoiseGenerator(seed);
        this.temperatureNoise = new NoiseGenerator(seed + 1000);
        this.humidityNoise = new NoiseGenerator(seed + 2000);
        this.pressureNoise = new NoiseGenerator(seed + 3000);
        this.stormNoise = new NoiseGenerator(seed + 4000);
        
        // Initialize collections
        this.globalPatterns = new ArrayList<>();
        this.activeStorms = new ArrayList<>();
        this.weatherCells = new HashMap<>();
        
        Logger.world("Initialized weather system with seed: {}", seed);
    }
    
    /**
     * Updates the weather system
     */
    public void update(float deltaTime) {
        globalTime += deltaTime;
        
        // Update seasonal factor (simplified seasonal cycle)
        seasonalFactor = (float) (0.5 + 0.5 * Math.sin(globalTime * 0.0001)); // Very slow seasonal change
        
        // Update global wind patterns
        updateGlobalWind(deltaTime);
        
        // Update storms
        updateStorms(deltaTime);
        
        // Spawn new storms
        spawnStorms(deltaTime);
        
        // Update weather cells
        updateWeatherCells(deltaTime);
    }
    
    /**
     * Gets wind velocity at a specific world position
     */
    public Vector2f getWindAt(float worldX, float worldZ) {
        // Base wind from global patterns
        Vector2f wind = new Vector2f(globalWindDirection);
        wind.mul(globalWindStrength);
        
        // Add local wind variations
        float localWindX = windNoise.noise(worldX * 0.001f, worldZ * 0.001f, globalTime * 0.1f);
        float localWindZ = windNoise.noise(worldX * 0.001f + 1000, worldZ * 0.001f + 1000, globalTime * 0.1f);
        
        wind.x += localWindX * config.getWindStrength() * 0.3f;
        wind.y += localWindZ * config.getWindStrength() * 0.3f;
        
        // Check for storm influence
        for (Storm storm : activeStorms) {
            float distance = storm.getDistanceTo(worldX, worldZ);
            if (distance < storm.getRadius()) {
                Vector2f stormWind = storm.getWindAt(worldX, worldZ);
                float influence = 1.0f - (distance / storm.getRadius());
                
                wind.x += stormWind.x * influence;
                wind.y += stormWind.y * influence;
            }
        }
        
        return wind;
    }
    
    /**
     * Gets weather conditions at a specific world position
     */
    public WeatherCondition getWeatherAt(float worldX, float worldZ) {
        WeatherCondition condition = new WeatherCondition();
        
        // Base temperature and humidity
        float baseTemp = 20.0f + seasonalFactor * config.getTemperatureVariation();
        float baseHumidity = 0.6f + seasonalFactor * config.getHumidityVariation();
        
        // Add noise variations
        float tempNoise = temperatureNoise.noise(worldX * 0.002f, worldZ * 0.002f, globalTime * 0.05f);
        float humidityNoise = this.humidityNoise.noise(worldX * 0.003f, worldZ * 0.003f, globalTime * 0.03f);
        
        condition.setTemperature(baseTemp + tempNoise * 10.0f);
        condition.setHumidity(Math.max(0, Math.min(1, baseHumidity + humidityNoise * 0.3f)));
        
        // Atmospheric pressure
        float pressure = pressureNoise.noise(worldX * 0.001f, worldZ * 0.001f, globalTime * 0.02f);
        condition.setPressure(1013.25f + pressure * 50.0f); // Standard pressure ± 50 hPa
        
        // Check for precipitation
        float precipitationChance = condition.getHumidity() * 0.8f;
        if (pressure < -0.2f) precipitationChance += 0.3f; // Low pressure increases rain chance
        
        condition.setPrecipitation(precipitationChance > 0.6f ? PrecipitationType.RAIN : PrecipitationType.NONE);
        condition.setPrecipitationIntensity(Math.max(0, precipitationChance - 0.6f) * 2.5f);
        
        // Check for storm influence
        for (Storm storm : activeStorms) {
            float distance = storm.getDistanceTo(worldX, worldZ);
            if (distance < storm.getRadius()) {
                WeatherCondition stormCondition = storm.getWeatherAt(worldX, worldZ);
                float influence = 1.0f - (distance / storm.getRadius());
                
                // Blend storm conditions
                condition = blendWeatherConditions(condition, stormCondition, influence);
            }
        }
        
        // Set visibility based on weather
        float visibility = 1000.0f; // Base visibility in meters
        if (condition.getPrecipitation() != PrecipitationType.NONE) {
            visibility *= (1.0f - condition.getPrecipitationIntensity() * 0.7f);
        }
        condition.setVisibility(Math.max(50.0f, visibility));
        
        return condition;
    }
    
    /**
     * Generates global weather patterns
     */
    public void generateGlobalPatterns() {
        Logger.world("Generating global weather patterns...");
        
        // Generate trade wind patterns
        WeatherPattern tradeWinds = new WeatherPattern(
            "Trade Winds",
            new Vector2f(1, 0.2f).normalize(),
            config.getWindStrength() * 0.8f,
            WeatherPattern.PatternType.PERSISTENT
        );
        globalPatterns.add(tradeWinds);
        
        // Generate seasonal monsoon pattern
        WeatherPattern monsoon = new WeatherPattern(
            "Monsoon",
            new Vector2f(-0.5f, 1).normalize(),
            config.getWindStrength() * 1.2f,
            WeatherPattern.PatternType.SEASONAL
        );
        globalPatterns.add(monsoon);
        
        Logger.world("Generated {} global weather patterns", globalPatterns.size());
    }
    
    private void updateGlobalWind(float deltaTime) {
        // Combine global patterns
        Vector2f combinedWind = new Vector2f(0, 0);
        float totalStrength = 0;
        
        for (WeatherPattern pattern : globalPatterns) {
            float influence = pattern.getInfluence(globalTime, seasonalFactor);
            
            Vector2f patternWind = pattern.getDirection().mul(pattern.getStrength() * influence);
            combinedWind.add(patternWind);
            totalStrength += pattern.getStrength() * influence;
        }
        
        if (totalStrength > 0) {
            globalWindDirection = combinedWind.normalize();
            globalWindStrength = totalStrength / globalPatterns.size();
        }
    }
    
    private void updateStorms(float deltaTime) {
        Iterator<Storm> stormIterator = activeStorms.iterator();
        
        while (stormIterator.hasNext()) {
            Storm storm = stormIterator.next();
            storm.update(deltaTime);
            
            if (storm.isExpired()) {
                Logger.world("Storm '{}' has dissipated", storm.getName());
                stormIterator.remove();
            }
        }
    }
    
    private void spawnStorms(float deltaTime) {
        if (random.nextFloat() < STORM_SPAWN_CHANCE * deltaTime) {
            // Generate random storm position
            float stormX = (random.nextFloat() - 0.5f) * 5000.0f;
            float stormZ = (random.nextFloat() - 0.5f) * 5000.0f;
            
            // Determine storm type based on conditions
            StormType stormType = determineStormType(stormX, stormZ);
            
            Storm newStorm = new Storm(
                "Storm-" + System.currentTimeMillis(),
                new Vector2f(stormX, stormZ),
                stormType,
                STORM_DURATION + random.nextFloat() * STORM_DURATION
            );
            
            activeStorms.add(newStorm);
            Logger.world("New {} spawned at ({}, {})", stormType, stormX, stormZ);
        }
    }
    
    private StormType determineStormType(float worldX, float worldZ) {
        float stormIntensity = stormNoise.noise(worldX * 0.0005f, worldZ * 0.0005f);
        
        if (stormIntensity > 0.7f) {
            return StormType.HURRICANE;
        } else if (stormIntensity > 0.3f) {
            return StormType.THUNDERSTORM;
        } else {
            return StormType.SQUALL;
        }
    }
    
    private void updateWeatherCells(float deltaTime) {
        // Update existing weather cells and remove expired ones
        Iterator<Map.Entry<Vector2f, WeatherCell>> cellIterator = weatherCells.entrySet().iterator();
        
        while (cellIterator.hasNext()) {
            Map.Entry<Vector2f, WeatherCell> entry = cellIterator.next();
            WeatherCell cell = entry.getValue();
            
            cell.update(deltaTime);
            
            if (cell.shouldRemove()) {
                cellIterator.remove();
            }
        }
    }
    
    private WeatherCondition blendWeatherConditions(WeatherCondition base, WeatherCondition overlay, float factor) {
        WeatherCondition result = new WeatherCondition();
        
        result.setTemperature(base.getTemperature() * (1 - factor) + overlay.getTemperature() * factor);
        result.setHumidity(base.getHumidity() * (1 - factor) + overlay.getHumidity() * factor);
        result.setPressure(base.getPressure() * (1 - factor) + overlay.getPressure() * factor);
        result.setVisibility(base.getVisibility() * (1 - factor) + overlay.getVisibility() * factor);
        
        // Use overlay precipitation if it's more intense
        if (overlay.getPrecipitationIntensity() > base.getPrecipitationIntensity()) {
            result.setPrecipitation(overlay.getPrecipitation());
            result.setPrecipitationIntensity(overlay.getPrecipitationIntensity() * factor);
        } else {
            result.setPrecipitation(base.getPrecipitation());
            result.setPrecipitationIntensity(base.getPrecipitationIntensity());
        }
        
        return result;
    }
    
    // Getters
    public List<Storm> getActiveStorms() {
        return new ArrayList<>(activeStorms);
    }
    
    public float getGlobalTime() {
        return globalTime;
    }
    
    public float getSeasonalFactor() {
        return seasonalFactor;
    }
    
    public Vector2f getGlobalWindDirection() {
        return new Vector2f(globalWindDirection);
    }
    
    public float getGlobalWindStrength() {
        return globalWindStrength;
    }
    
    /**
     * Gets the current global wind speed
     */
    public float getWindSpeed() {
        return globalWindStrength;
    }
    
    /**
     * Gets the current wave height based on wind conditions
     */
    public float getWaveHeight() {
        // Calculate wave height based on wind strength and storm activity
        float baseWaveHeight = globalWindStrength * 0.2f; // Base wave height from wind
        
        // Add storm influence
        float stormWaveHeight = 0.0f;
        for (Storm storm : activeStorms) {
            float stormIntensity = storm.getIntensity();
            stormWaveHeight = Math.max(stormWaveHeight, stormIntensity * 5.0f);
        }
        
        return Math.max(0.5f, baseWaveHeight + stormWaveHeight);
    }
    
    public long getSeed() {
        return seed;
    }
    
    public float getCellSize() {
        return CELL_SIZE;
    }
}