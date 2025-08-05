package com.odyssey.world.weather;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced weather simulation system for The Odyssey.
 * Implements realistic weather patterns including storms, fog, rain,
 * hurricanes, and seasonal changes that affect gameplay.
 */
public class WeatherSystem {
    private static final Logger logger = LoggerFactory.getLogger(WeatherSystem.class);
    
    private final GameConfig config;
    
    // Current weather state
    private WeatherType currentWeather = WeatherType.CLEAR;
    private float weatherIntensity = 0.0f;
    private double weatherTimer = 0.0;
    private double weatherDuration = 300.0; // 5 minutes default
    
    // Environmental factors
    private float temperature = 20.0f; // Celsius
    private float humidity = 50.0f; // Percentage
    private float pressure = 1013.25f; // hPa (sea level)
    private Vector3f windDirection = new Vector3f(1, 0, 0);
    private float windSpeed = 5.0f; // m/s
    
    // Weather systems
    private final List<WeatherFront> weatherFronts = new ArrayList<>();
    private final List<Storm> activeStorms = new ArrayList<>();
    private final Map<Vector2f, LocalWeather> localWeatherCells = new HashMap<>();
    
    // Seasonal data
    private Season currentSeason = Season.SPRING;
    private double yearProgress = 0.0; // 0.0 to 1.0 through the year
    
    // Climate zones
    private final Map<ClimateZone, ClimateData> climateZones = new EnumMap<>(ClimateZone.class);
    
    public enum WeatherType {
        CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST,
        LIGHT_RAIN, MODERATE_RAIN, HEAVY_RAIN,
        DRIZZLE, THUNDERSTORM, HURRICANE,
        FOG, DENSE_FOG, MIST,
        SNOW, BLIZZARD, HAIL
    }
    
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }
    
    public enum ClimateZone {
        TROPICAL, SUBTROPICAL, TEMPERATE, ARCTIC, DESERT, MONSOON
    }
    
    public static class WeatherFront {
        public Vector2f position;
        public Vector2f movement;
        public WeatherType weatherType;
        public float intensity;
        public float radius;
        public float speed;
        public double lifetime;
        public double maxLifetime;
        
        public WeatherFront(Vector2f position, WeatherType type) {
            this.position = new Vector2f(position);
            this.weatherType = type;
            this.intensity = 0.5f + (float)Math.random() * 0.5f;
            this.radius = 100.0f + (float)Math.random() * 200.0f;
            this.speed = 2.0f + (float)Math.random() * 8.0f;
            this.lifetime = 0.0;
            this.maxLifetime = 600.0 + Math.random() * 1800.0; // 10-40 minutes
            
            // Random movement direction
            float angle = (float)Math.random() * 2 * (float)Math.PI;
            this.movement = new Vector2f((float)Math.cos(angle), (float)Math.sin(angle)).mul(speed);
        }
    }
    
    public static class Storm {
        public Vector2f center;
        public Vector2f movement;
        public StormType type;
        public float intensity; // 0.0 to 1.0
        public float radius;
        public float eyeRadius; // For hurricanes
        public double lifetime;
        public double maxLifetime;
        public boolean isForming;
        public boolean isDissipating;
        
        public Storm(Vector2f center, StormType type) {
            this.center = new Vector2f(center);
            this.type = type;
            this.intensity = 0.1f;
            this.lifetime = 0.0;
            this.isForming = true;
            this.isDissipating = false;
            
            switch (type) {
                case THUNDERSTORM -> {
                    this.radius = 20.0f + (float)Math.random() * 30.0f;
                    this.maxLifetime = 1800.0 + Math.random() * 3600.0; // 30-90 minutes
                }
                case HURRICANE -> {
                    this.radius = 100.0f + (float)Math.random() * 200.0f;
                    this.eyeRadius = 10.0f + (float)Math.random() * 20.0f;
                    this.maxLifetime = 7200.0 + Math.random() * 14400.0; // 2-6 hours
                }
                case BLIZZARD -> {
                    this.radius = 50.0f + (float)Math.random() * 100.0f;
                    this.maxLifetime = 3600.0 + Math.random() * 7200.0; // 1-3 hours
                }
            }
            
            // Storm movement (generally eastward with variation)
            float angle = (float)Math.random() * (float)Math.PI * 0.5f - (float)Math.PI * 0.25f; // ±45 degrees from east
            float speed = 3.0f + (float)Math.random() * 7.0f;
            this.movement = new Vector2f((float)Math.cos(angle), (float)Math.sin(angle)).mul(speed);
        }
    }
    
    public enum StormType {
        THUNDERSTORM, HURRICANE, BLIZZARD, TORNADO
    }
    
    public static class LocalWeather {
        public Vector2f position;
        public WeatherType localType;
        public float temperature;
        public float humidity;
        public float pressure;
        public Vector2f windVector;
        public float visibility;
        public float precipitation;
        
        public LocalWeather(Vector2f position) {
            this.position = new Vector2f(position);
            this.localType = WeatherType.CLEAR;
            this.temperature = 20.0f;
            this.humidity = 50.0f;
            this.pressure = 1013.25f;
            this.windVector = new Vector2f();
            this.visibility = 10000.0f; // 10km in clear weather
            this.precipitation = 0.0f;
        }
    }
    
    public static class ClimateData {
        public float baseTemperature;
        public float temperatureVariation;
        public float baseHumidity;
        public float precipitationChance;
        public WeatherType[] commonWeatherTypes;
        
        public ClimateData(float baseTemp, float tempVar, float humidity, float precipChance, WeatherType... weatherTypes) {
            this.baseTemperature = baseTemp;
            this.temperatureVariation = tempVar;
            this.baseHumidity = humidity;
            this.precipitationChance = precipChance;
            this.commonWeatherTypes = weatherTypes;
        }
    }
    
    public WeatherSystem(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing advanced weather system");
        
        // Initialize climate zones
        initializeClimateZones();
        
        // Generate initial weather fronts
        generateInitialWeatherFronts(5);
        
        // Create local weather grid
        generateLocalWeatherGrid(32); // 32x32 grid
        
        // Set initial weather
        selectInitialWeather();
        
        logger.info("Weather system initialized with {} weather fronts and {} local weather cells", 
                   weatherFronts.size(), localWeatherCells.size());
    }
    
    private void initializeClimateZones() {
        climateZones.put(ClimateZone.TROPICAL, new ClimateData(
            28.0f, 5.0f, 80.0f, 0.6f,
            WeatherType.PARTLY_CLOUDY, WeatherType.THUNDERSTORM, WeatherType.HEAVY_RAIN
        ));
        
        climateZones.put(ClimateZone.TEMPERATE, new ClimateData(
            15.0f, 15.0f, 60.0f, 0.4f,
            WeatherType.CLEAR, WeatherType.CLOUDY, WeatherType.LIGHT_RAIN
        ));
        
        climateZones.put(ClimateZone.ARCTIC, new ClimateData(
            -5.0f, 10.0f, 70.0f, 0.3f,
            WeatherType.CLOUDY, WeatherType.SNOW, WeatherType.BLIZZARD
        ));
        
        climateZones.put(ClimateZone.DESERT, new ClimateData(
            35.0f, 20.0f, 20.0f, 0.1f,
            WeatherType.CLEAR, WeatherType.PARTLY_CLOUDY
        ));
        
        climateZones.put(ClimateZone.MONSOON, new ClimateData(
            25.0f, 8.0f, 85.0f, 0.8f,
            WeatherType.HEAVY_RAIN, WeatherType.THUNDERSTORM, WeatherType.CLOUDY
        ));
    }
    
    private void generateInitialWeatherFronts(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f position = new Vector2f(
                (float)(Math.random() - 0.5) * 2000,
                (float)(Math.random() - 0.5) * 2000
            );
            
            WeatherType[] possibleTypes = {
                WeatherType.CLOUDY, WeatherType.LIGHT_RAIN, 
                WeatherType.PARTLY_CLOUDY, WeatherType.FOG
            };
            
            WeatherType type = possibleTypes[(int)(Math.random() * possibleTypes.length)];
            weatherFronts.add(new WeatherFront(position, type));
        }
    }
    
    private void generateLocalWeatherGrid(int gridSize) {
        float cellSize = 64.0f; // Each cell covers 64x64 blocks
        
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                Vector2f position = new Vector2f(
                    (x - gridSize / 2.0f) * cellSize,
                    (z - gridSize / 2.0f) * cellSize
                );
                
                localWeatherCells.put(position, new LocalWeather(position));
            }
        }
    }
    
    private void selectInitialWeather() {
        // Start with clear weather
        currentWeather = WeatherType.CLEAR;
        weatherIntensity = 0.2f;
        weatherDuration = 600.0; // 10 minutes
    }
    
    public void update(double deltaTime) {
        weatherTimer += deltaTime;
        yearProgress += deltaTime / (24.0 * 3600.0 * 365.0); // Assuming 1 real second = 1 game hour
        if (yearProgress >= 1.0) yearProgress -= 1.0;
        
        // Update season
        updateSeason();
        
        // Update weather fronts
        updateWeatherFronts(deltaTime);
        
        // Update storms
        updateStorms(deltaTime);
        
        // Update local weather
        updateLocalWeather(deltaTime);
        
        // Check for weather transitions
        if (weatherTimer >= weatherDuration) {
            transitionWeather();
        }
        
        // Update environmental factors
        updateEnvironmentalFactors(deltaTime);
        
        // Generate new weather systems
        if (Math.random() < deltaTime * 0.001) { // 0.1% chance per second
            generateNewWeatherSystem();
        }
    }
    
    private void updateSeason() {
        Season newSeason = switch ((int)(yearProgress * 4)) {
            case 0 -> Season.SPRING;
            case 1 -> Season.SUMMER;
            case 2 -> Season.AUTUMN;
            default -> Season.WINTER;
        };
        
        if (newSeason != currentSeason) {
            logger.info("Season changed from {} to {}", currentSeason, newSeason);
            currentSeason = newSeason;
        }
    }
    
    private void updateWeatherFronts(double deltaTime) {
        Iterator<WeatherFront> iterator = weatherFronts.iterator();
        
        while (iterator.hasNext()) {
            WeatherFront front = iterator.next();
            
            // Move the front
            front.position.add(front.movement.x * (float)deltaTime, front.movement.y * (float)deltaTime);
            front.lifetime += deltaTime;
            
            // Fronts weaken over time
            front.intensity *= 1.0f - (float)deltaTime * 0.0001f;
            
            // Remove expired or weak fronts
            if (front.lifetime > front.maxLifetime || front.intensity < 0.1f) {
                iterator.remove();
            }
        }
    }
    
    private void updateStorms(double deltaTime) {
        Iterator<Storm> iterator = activeStorms.iterator();
        
        while (iterator.hasNext()) {
            Storm storm = iterator.next();
            
            storm.lifetime += deltaTime;
            
            // Storm lifecycle
            if (storm.isForming) {
                storm.intensity += (float)deltaTime * 0.001f; // Gradual intensification
                if (storm.intensity >= 0.8f) {
                    storm.isForming = false;
                }
            } else if (storm.lifetime > storm.maxLifetime * 0.8 && !storm.isDissipating) {
                storm.isDissipating = true;
            }
            
            if (storm.isDissipating) {
                storm.intensity -= (float)deltaTime * 0.002f; // Faster dissipation
            }
            
            // Move the storm
            storm.center.add(storm.movement.x * (float)deltaTime, storm.movement.y * (float)deltaTime);
            
            // Remove dissipated storms
            if (storm.intensity <= 0.05f) {
                logger.info("Storm {} dissipated after {:.1f} minutes", storm.type, storm.lifetime / 60.0);
                iterator.remove();
            }
        }
    }
    
    private void updateLocalWeather(double deltaTime) {
        for (LocalWeather localWeather : localWeatherCells.values()) {
            // Update based on nearby weather fronts and storms
            updateLocalWeatherFromFronts(localWeather);
            updateLocalWeatherFromStorms(localWeather);
            
            // Apply seasonal and climate effects
            applySeasonalEffects(localWeather);
        }
    }
    
    private void updateLocalWeatherFromFronts(LocalWeather localWeather) {
        float totalInfluence = 0.0f;
        WeatherType dominantType = WeatherType.CLEAR;
        float strongestInfluence = 0.0f;
        
        for (WeatherFront front : weatherFronts) {
            float distance = localWeather.position.distance(front.position);
            
            if (distance <= front.radius) {
                float influence = (1.0f - distance / front.radius) * front.intensity;
                totalInfluence += influence;
                
                if (influence > strongestInfluence) {
                    strongestInfluence = influence;
                    dominantType = front.weatherType;
                }
            }
        }
        
        if (totalInfluence > 0.3f) {
            localWeather.localType = dominantType;
            localWeather.precipitation = Math.min(1.0f, totalInfluence);
        } else {
            localWeather.localType = WeatherType.CLEAR;
            localWeather.precipitation = 0.0f;
        }
    }
    
    private void updateLocalWeatherFromStorms(LocalWeather localWeather) {
        for (Storm storm : activeStorms) {
            float distance = localWeather.position.distance(storm.center);
            
            if (distance <= storm.radius) {
                float influence = (1.0f - distance / storm.radius) * storm.intensity;
                
                // Storms override other weather
                if (influence > 0.5f) {
                    localWeather.localType = switch (storm.type) {
                        case THUNDERSTORM -> WeatherType.THUNDERSTORM;
                        case HURRICANE -> WeatherType.HURRICANE;
                        case BLIZZARD -> WeatherType.BLIZZARD;
                        case TORNADO -> WeatherType.THUNDERSTORM; // Tornado embedded in thunderstorm
                    };
                    
                    localWeather.precipitation = influence;
                    
                    // Hurricane eye effect
                    if (storm.type == StormType.HURRICANE && distance <= storm.eyeRadius) {
                        localWeather.localType = WeatherType.CLEAR;
                        localWeather.precipitation = 0.0f;
                    }
                }
            }
        }
    }
    
    private void applySeasonalEffects(LocalWeather localWeather) {
        // Adjust temperature based on season
        float seasonalTempModifier = switch (currentSeason) {
            case SPRING -> 0.0f;
            case SUMMER -> 8.0f;
            case AUTUMN -> -3.0f;
            case WINTER -> -10.0f;
        };
        
        localWeather.temperature = 20.0f + seasonalTempModifier + (float)(Math.random() - 0.5) * 10.0f;
        
        // Adjust humidity and precipitation chances
        localWeather.humidity = switch (currentSeason) {
            case SPRING -> 60.0f + (float)(Math.random() - 0.5) * 20.0f;
            case SUMMER -> 50.0f + (float)(Math.random() - 0.5) * 30.0f;
            case AUTUMN -> 70.0f + (float)(Math.random() - 0.5) * 20.0f;
            case WINTER -> 65.0f + (float)(Math.random() - 0.5) * 25.0f;
        };
    }
    
    private void transitionWeather() {
        logger.debug("Transitioning weather from {} (intensity: {:.2f})", currentWeather, weatherIntensity);
        
        // Select new weather based on current conditions and season
        WeatherType newWeather = selectNextWeather();
        float newIntensity = 0.3f + (float)Math.random() * 0.7f;
        double newDuration = 300.0 + Math.random() * 900.0; // 5-20 minutes
        
        currentWeather = newWeather;
        weatherIntensity = newIntensity;
        weatherDuration = newDuration;
        weatherTimer = 0.0;
        
        logger.info("Weather changed to {} with intensity {:.2f} for {:.1f} minutes", 
                   currentWeather, weatherIntensity, weatherDuration / 60.0);
    }
    
    private WeatherType selectNextWeather() {
        // Weather transitions are influenced by current weather, season, and randomness
        List<WeatherType> possibleWeather = new ArrayList<>();
        
        // Add seasonal weather types
        switch (currentSeason) {
            case SPRING -> {
                possibleWeather.addAll(Arrays.asList(
                    WeatherType.CLEAR, WeatherType.PARTLY_CLOUDY, WeatherType.CLOUDY,
                    WeatherType.LIGHT_RAIN, WeatherType.THUNDERSTORM
                ));
            }
            case SUMMER -> {
                possibleWeather.addAll(Arrays.asList(
                    WeatherType.CLEAR, WeatherType.PARTLY_CLOUDY, 
                    WeatherType.THUNDERSTORM, WeatherType.HEAVY_RAIN
                ));
            }
            case AUTUMN -> {
                possibleWeather.addAll(Arrays.asList(
                    WeatherType.CLOUDY, WeatherType.OVERCAST, WeatherType.LIGHT_RAIN,
                    WeatherType.MODERATE_RAIN, WeatherType.FOG
                ));
            }
            case WINTER -> {
                possibleWeather.addAll(Arrays.asList(
                    WeatherType.OVERCAST, WeatherType.SNOW, WeatherType.FOG,
                    WeatherType.BLIZZARD
                ));
            }
        }
        
        // Avoid immediate repetition of the same weather
        if (possibleWeather.contains(currentWeather) && possibleWeather.size() > 1) {
            possibleWeather.remove(currentWeather);
        }
        
        return possibleWeather.get((int)(Math.random() * possibleWeather.size()));
    }
    
    private void updateEnvironmentalFactors(double deltaTime) {
        // Update global wind based on weather
        Vector3f targetWind = calculateWindFromWeather();
        windDirection.lerp(targetWind, (float)deltaTime * 0.1f);
        
        // Update atmospheric pressure
        float targetPressure = calculatePressureFromWeather();
        pressure += (targetPressure - pressure) * (float)deltaTime * 0.05f;
        
        // Update temperature
        float targetTemperature = calculateTemperatureFromWeather();
        temperature += (targetTemperature - temperature) * (float)deltaTime * 0.02f;
    }
    
    private Vector3f calculateWindFromWeather() {
        float windMagnitude = switch (currentWeather) {
            case CLEAR, PARTLY_CLOUDY -> 3.0f + (float)Math.random() * 5.0f;
            case CLOUDY, OVERCAST -> 5.0f + (float)Math.random() * 8.0f;
            case LIGHT_RAIN, DRIZZLE -> 8.0f + (float)Math.random() * 7.0f;
            case MODERATE_RAIN, HEAVY_RAIN -> 12.0f + (float)Math.random() * 10.0f;
            case THUNDERSTORM -> 15.0f + (float)Math.random() * 15.0f;
            case HURRICANE -> 30.0f + (float)Math.random() * 40.0f;
            case BLIZZARD -> 20.0f + (float)Math.random() * 20.0f;
            case FOG, MIST -> 1.0f + (float)Math.random() * 3.0f;
            default -> 5.0f;
        };
        
        windSpeed = windMagnitude * weatherIntensity;
        
        // Add some randomness to wind direction
        float currentAngle = (float)Math.atan2(windDirection.z, windDirection.x);
        float angleChange = ((float)Math.random() - 0.5f) * 0.2f; // ±6 degrees
        float newAngle = currentAngle + angleChange;
        
        return new Vector3f((float)Math.cos(newAngle), 0, (float)Math.sin(newAngle)).normalize();
    }
    
    private float calculatePressureFromWeather() {
        return switch (currentWeather) {
            case CLEAR, PARTLY_CLOUDY -> 1020.0f + (float)Math.random() * 10.0f;
            case CLOUDY -> 1015.0f + (float)Math.random() * 8.0f;
            case OVERCAST, LIGHT_RAIN -> 1010.0f + (float)Math.random() * 10.0f;
            case MODERATE_RAIN, HEAVY_RAIN -> 1005.0f + (float)Math.random() * 8.0f;
            case THUNDERSTORM -> 995.0f + (float)Math.random() * 15.0f;
            case HURRICANE -> 950.0f + (float)Math.random() * 30.0f;
            case BLIZZARD -> 980.0f + (float)Math.random() * 20.0f;
            default -> 1013.25f;
        };
    }
    
    private float calculateTemperatureFromWeather() {
        float baseTemp = switch (currentSeason) {
            case SPRING -> 15.0f;
            case SUMMER -> 25.0f;
            case AUTUMN -> 12.0f;
            case WINTER -> 2.0f;
        };
        
        float weatherModifier = switch (currentWeather) {
            case CLEAR -> 3.0f;
            case PARTLY_CLOUDY -> 1.0f;
            case CLOUDY, OVERCAST -> -2.0f;
            case LIGHT_RAIN, DRIZZLE -> -3.0f;
            case MODERATE_RAIN, HEAVY_RAIN -> -5.0f;
            case THUNDERSTORM -> -7.0f;
            case HURRICANE -> -10.0f;
            case SNOW, BLIZZARD -> -15.0f;
            case FOG, MIST -> -1.0f;
            default -> 0.0f;
        };
        
        return baseTemp + weatherModifier + (float)(Math.random() - 0.5) * 5.0f;
    }
    
    private void generateNewWeatherSystem() {
        if (Math.random() < 0.3) {
            // Generate new weather front
            Vector2f position = new Vector2f(
                (float)(Math.random() - 0.5) * 2000,
                (float)(Math.random() - 0.5) * 2000
            );
            
            WeatherType[] frontTypes = {
                WeatherType.CLOUDY, WeatherType.LIGHT_RAIN, WeatherType.FOG
            };
            
            WeatherType type = frontTypes[(int)(Math.random() * frontTypes.length)];
            weatherFronts.add(new WeatherFront(position, type));
            
            logger.debug("Generated new weather front: {} at {}", type, position);
        } else {
            // Generate new storm (rare)
            if (Math.random() < 0.1) {
                Vector2f center = new Vector2f(
                    (float)(Math.random() - 0.5) * 1500,
                    (float)(Math.random() - 0.5) * 1500
                );
                
                StormType stormType = switch (currentSeason) {
                    case SPRING, SUMMER -> Math.random() < 0.8 ? StormType.THUNDERSTORM : StormType.HURRICANE;
                    case AUTUMN -> Math.random() < 0.9 ? StormType.THUNDERSTORM : StormType.HURRICANE;
                    case WINTER -> Math.random() < 0.7 ? StormType.BLIZZARD : StormType.THUNDERSTORM;
                };
                
                Storm newStorm = new Storm(center, stormType);
                activeStorms.add(newStorm);
                
                logger.info("Generated new storm: {} at {} with radius {:.1f}", 
                           stormType, center, newStorm.radius);
            }
        }
    }
    
    public void render(Renderer renderer) {
        // Render weather effects
        renderWeatherEffects(renderer);
        
        // Render storms
        for (Storm storm : activeStorms) {
            renderStorm(renderer, storm);
        }
        
        // Render weather fronts (for debug or map view)
        if (config.isDebugMode()) {
            for (WeatherFront front : weatherFronts) {
                renderWeatherFront(renderer, front);
            }
        }
    }
    
    private void renderWeatherEffects(Renderer renderer) {
        // This would render the actual weather effects like rain, snow, fog, etc.
        // For now, just placeholder rendering
        
        switch (currentWeather) {
            case LIGHT_RAIN, MODERATE_RAIN, HEAVY_RAIN -> renderRain(renderer);
            case SNOW, BLIZZARD -> renderSnow(renderer);
            case FOG, DENSE_FOG, MIST -> renderFog(renderer);
            case THUNDERSTORM -> {
                renderRain(renderer);
                renderLightning(renderer);
            }
            // Add more weather rendering...
        }
    }
    
    private void renderRain(Renderer renderer) {
        // Render rain particles based on intensity
        int particleCount = (int)(weatherIntensity * 1000);
        // Implementation would create and render rain particles
    }
    
    private void renderSnow(Renderer renderer) {
        // Render snow particles
        int particleCount = (int)(weatherIntensity * 500);
        // Implementation would create and render snow particles
    }
    
    private void renderFog(Renderer renderer) {
        // Reduce visibility and add fog effects
        float fogDensity = weatherIntensity * 0.1f;
        // Implementation would modify renderer fog settings
    }
    
    private void renderLightning(Renderer renderer) {
        // Render lightning flashes during thunderstorms
        if (Math.random() < 0.01 * weatherIntensity) { // Random lightning
            // Implementation would create lightning flash effect
        }
    }
    
    private void renderStorm(Renderer renderer, Storm storm) {
        // Render storm visualization (could be particles, effects, etc.)
        // This could show the storm boundaries, wind patterns, etc.
    }
    
    private void renderWeatherFront(Renderer renderer, WeatherFront front) {
        // Debug rendering of weather fronts
        // Show front boundaries and movement direction
    }
    
    public void forceWeather(WeatherType weather, float intensity, double duration) {
        logger.info("Forcing weather to {} with intensity {:.2f} for {:.1f} minutes", 
                   weather, intensity, duration / 60.0);
        
        currentWeather = weather;
        weatherIntensity = intensity;
        weatherDuration = duration;
        weatherTimer = 0.0;
    }
    
    public void spawnStorm(StormType type, Vector2f location) {
        Storm newStorm = new Storm(location, type);
        activeStorms.add(newStorm);
        
        logger.info("Spawned {} at {}", type, location);
    }
    
    public LocalWeather getLocalWeatherAt(Vector2f position) {
        // Find the nearest local weather cell
        LocalWeather nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (LocalWeather localWeather : localWeatherCells.values()) {
            float distance = position.distance(localWeather.position);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = localWeather;
            }
        }
        
        return nearest;
    }
    
    public float getVisibilityAt(Vector2f position) {
        LocalWeather localWeather = getLocalWeatherAt(position);
        if (localWeather != null) {
            return localWeather.visibility;
        }
        
        // Default visibility based on current weather
        return switch (currentWeather) {
            case CLEAR, PARTLY_CLOUDY -> 15000.0f; // 15km
            case CLOUDY -> 10000.0f; // 10km
            case OVERCAST -> 8000.0f; // 8km
            case LIGHT_RAIN -> 5000.0f; // 5km
            case MODERATE_RAIN -> 3000.0f; // 3km
            case HEAVY_RAIN, THUNDERSTORM -> 1000.0f; // 1km
            case FOG -> 500.0f; // 500m
            case DENSE_FOG -> 100.0f; // 100m
            case BLIZZARD -> 200.0f; // 200m
            default -> 10000.0f;
        };
    }
    
    public boolean isStormNear(Vector2f position, float radius) {
        for (Storm storm : activeStorms) {
            float distance = position.distance(storm.center);
            if (distance <= storm.radius + radius) {
                return true;
            }
        }
        return false;
    }
    
    public void cleanup() {
        logger.info("Cleaning up weather system");
        weatherFronts.clear();
        activeStorms.clear();
        localWeatherCells.clear();
    }
    
    // Getters
    public WeatherType getCurrentWeather() { return currentWeather; }
    public float getWeatherIntensity() { return weatherIntensity; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getPressure() { return pressure; }
    public Vector3f getWindDirection() { return new Vector3f(windDirection); }
    public float getWindSpeed() { return windSpeed; }
    public Season getCurrentSeason() { return currentSeason; }
    public double getYearProgress() { return yearProgress; }
    public int getActiveStormCount() { return activeStorms.size(); }
    public int getWeatherFrontCount() { return weatherFronts.size(); }
    
    public List<Storm> getActiveStorms() {
        return new ArrayList<>(activeStorms);
    }
    
    public List<WeatherFront> getWeatherFronts() {
        return new ArrayList<>(weatherFronts);
    }
}