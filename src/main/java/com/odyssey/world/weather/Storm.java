package com.odyssey.world.weather;

import org.joml.Vector2f;

/**
 * Represents a weather storm system that moves across the world
 */
public class Storm {
    
    private Vector2f position;
    private Vector2f velocity;
    private float intensity; // 0.0 to 1.0
    private float radius; // In world units
    private StormType type;
    private long creationTime;
    private long duration; // In milliseconds
    private boolean active;
    
    // Storm properties
    private float maxWindSpeed;
    private float minPressure;
    private float precipitationIntensity;
    private float lightningFrequency; // Strikes per minute
    
    public Storm(Vector2f position, StormType type, float intensity) {
        this.position = new Vector2f(position);
        this.type = type;
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        this.creationTime = System.currentTimeMillis();
        this.active = true;
        
        initializeStormProperties();
        generateMovement();
    }
    
    /**
     * Constructor for WeatherSystem compatibility
     */
    public Storm(String name, Vector2f position, StormType type, float duration) {
        this.position = new Vector2f(position);
        this.type = type;
        this.intensity = 0.8f; // Default intensity
        this.creationTime = System.currentTimeMillis();
        this.duration = (long)(duration * 1000); // Convert to milliseconds
        this.active = true;
        
        initializeStormProperties();
        generateMovement();
    }
    
    private void initializeStormProperties() {
        // Base properties from storm type
        this.radius = type.getBaseRadius() * (0.5f + intensity * 0.5f);
        this.duration = (long)(type.getBaseDuration() * (0.7f + intensity * 0.6f));
        this.maxWindSpeed = type.getMaxWindSpeed() * intensity;
        this.minPressure = type.getMinPressure() - (intensity * 50.0f);
        this.precipitationIntensity = type.getPrecipitationIntensity() * intensity;
        this.lightningFrequency = type.getLightningFrequency() * intensity;
    }
    
    private void generateMovement() {
        // Generate random movement direction and speed
        float angle = (float)(Math.random() * Math.PI * 2);
        float speed = type.getMovementSpeed() * (0.8f + (float)Math.random() * 0.4f);
        
        this.velocity = new Vector2f(
            (float)Math.cos(angle) * speed,
            (float)Math.sin(angle) * speed
        );
    }
    
    /**
     * Updates the storm's position and properties
     */
    public void update(float deltaTime) {
        if (!active) return;
        
        // Move the storm
        position.x += velocity.x * deltaTime;
        position.y += velocity.y * deltaTime;
        
        // Check if storm has expired
        long currentTime = System.currentTimeMillis();
        if (currentTime - creationTime > duration) {
            active = false;
            return;
        }
        
        // Update intensity based on age (storms weaken over time)
        float ageRatio = (float)(currentTime - creationTime) / duration;
        float currentIntensity = intensity * getIntensityCurve(ageRatio);
        
        // Update properties based on current intensity
        this.maxWindSpeed = type.getMaxWindSpeed() * currentIntensity;
        this.precipitationIntensity = type.getPrecipitationIntensity() * currentIntensity;
        this.lightningFrequency = type.getLightningFrequency() * currentIntensity;
    }
    
    /**
     * Gets the intensity curve over the storm's lifetime
     * Storms typically start weak, peak in the middle, then weaken
     */
    private float getIntensityCurve(float ageRatio) {
        if (ageRatio < 0.3f) {
            // Growing phase
            return ageRatio / 0.3f;
        } else if (ageRatio < 0.7f) {
            // Peak phase
            return 1.0f;
        } else {
            // Weakening phase
            return 1.0f - ((ageRatio - 0.7f) / 0.3f);
        }
    }
    
    /**
     * Gets the storm's influence at a specific position
     * Returns 0.0 if outside storm, 1.0 at center
     */
    public float getInfluenceAt(Vector2f worldPos) {
        if (!active) return 0.0f;
        
        float distance = position.distance(worldPos);
        if (distance > radius) return 0.0f;
        
        // Smooth falloff from center to edge
        float normalizedDistance = distance / radius;
        return 1.0f - (normalizedDistance * normalizedDistance);
    }
    
    /**
     * Gets weather conditions at a specific position affected by this storm
     */
    public WeatherCondition getWeatherAt(Vector2f worldPos, WeatherCondition baseWeather) {
        float influence = getInfluenceAt(worldPos);
        if (influence <= 0.0f) return baseWeather;
        
        WeatherCondition stormWeather = new WeatherCondition();
        
        // Interpolate between base weather and storm conditions
        stormWeather.setTemperature(lerp(baseWeather.getTemperature(), 
            baseWeather.getTemperature() - 5.0f, influence));
        
        stormWeather.setHumidity(lerp(baseWeather.getHumidity(), 0.9f, influence));
        
        stormWeather.setPressure(lerp(baseWeather.getPressure(), minPressure, influence));
        
        stormWeather.setVisibility(lerp(baseWeather.getVisibility(), 
            type.getMinVisibility(), influence));
        
        stormWeather.setCloudCover(lerp(baseWeather.getCloudCover(), 1.0f, influence));
        
        stormWeather.setWindSpeed(lerp(baseWeather.getWindSpeed(), maxWindSpeed, influence));
        
        // Wind direction points away from storm center
        Vector2f windDirection = new Vector2f(worldPos.x - position.x, worldPos.y - position.y);
        windDirection.normalize();
        float windAngle = (float)Math.atan2(windDirection.y, windDirection.x);
        stormWeather.setWindDirection((float)Math.toDegrees(windAngle));
        
        // Set precipitation
        if (influence > 0.3f) {
            stormWeather.setPrecipitation(type.getPrecipitationType());
            stormWeather.setPrecipitationIntensity(precipitationIntensity * influence);
        }
        
        return stormWeather;
    }
    
    /**
     * Checks if lightning should strike at the given position
     */
    public boolean shouldStrikeLightning(Vector2f worldPos, float deltaTime) {
        if (type != StormType.THUNDERSTORM) return false;
        
        float influence = getInfluenceAt(worldPos);
        if (influence <= 0.5f) return false;
        
        // Calculate probability based on influence and frequency
        float strikeProbability = (lightningFrequency / 60.0f) * deltaTime * influence;
        return Math.random() < strikeProbability;
    }
    
    /**
     * Gets the storm's threat level at a position (0.0 = safe, 1.0 = extremely dangerous)
     */
    public float getThreatLevel(Vector2f worldPos) {
        float influence = getInfluenceAt(worldPos);
        if (influence <= 0.0f) return 0.0f;
        
        float threat = 0.0f;
        
        // Wind threat
        threat += (maxWindSpeed / 30.0f) * 0.4f * influence;
        
        // Precipitation threat
        threat += precipitationIntensity * 0.3f * influence;
        
        // Lightning threat
        if (type == StormType.THUNDERSTORM) {
            threat += lightningFrequency / 10.0f * 0.3f * influence;
        }
        
        return Math.min(1.0f, threat);
    }
    
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    // Getters
    public Vector2f getPosition() { return new Vector2f(position); }
    public Vector2f getVelocity() { return new Vector2f(velocity); }
    public float getIntensity() { return intensity; }
    public float getRadius() { return radius; }
    public StormType getType() { return type; }
    public boolean isActive() { return active; }
    public long getAge() { return System.currentTimeMillis() - creationTime; }
    public long getDuration() { return duration; }
    public float getMaxWindSpeed() { return maxWindSpeed; }
    public float getMinPressure() { return minPressure; }
    public float getPrecipitationIntensity() { return precipitationIntensity; }
    public float getLightningFrequency() { return lightningFrequency; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    public void setVelocity(Vector2f velocity) { this.velocity = new Vector2f(velocity); }
    
    /**
     * Gets the distance from storm center to a world position
     */
    public float getDistanceTo(float worldX, float worldZ) {
        Vector2f worldPos = new Vector2f(worldX, worldZ);
        return position.distance(worldPos);
    }
    
    /**
     * Gets wind velocity at a specific world position
     */
    public Vector2f getWindAt(float worldX, float worldZ) {
        Vector2f worldPos = new Vector2f(worldX, worldZ);
        float influence = getInfluenceAt(worldPos);
        
        if (influence <= 0.0f) {
            return new Vector2f(0, 0);
        }
        
        // Wind direction points away from storm center
        Vector2f windDirection = new Vector2f(worldPos.x - position.x, worldPos.y - position.y);
        windDirection.normalize();
        
        // Wind strength based on influence and storm intensity
        float windStrength = maxWindSpeed * influence;
        windDirection.mul(windStrength);
        
        return windDirection;
    }
    
    /**
     * Gets weather conditions at a specific world position
     */
    public WeatherCondition getWeatherAt(float worldX, float worldZ) {
        Vector2f worldPos = new Vector2f(worldX, worldZ);
        WeatherCondition baseWeather = new WeatherCondition();
        return getWeatherAt(worldPos, baseWeather);
    }
    
    /**
     * Checks if the storm has expired
     */
    public boolean isExpired() {
        return !active || (System.currentTimeMillis() - creationTime > duration);
    }
    
    /**
     * Gets the storm's name
     */
    public String getName() {
        return type.getDisplayName() + "-" + System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return String.format("Storm[%s, intensity=%.2f, pos=(%.1f,%.1f), radius=%.1f, active=%s]",
            type, intensity, position.x, position.y, radius, active);
    }
}