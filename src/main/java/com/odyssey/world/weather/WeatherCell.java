package com.odyssey.world.weather;

import org.joml.Vector2f;

/**
 * Represents a localized weather pattern in a grid cell
 * Weather cells create regional weather variations across the world
 */
public class WeatherCell {
    
    private Vector2f center;
    private float radius;
    private WeatherCondition baseCondition;
    private float intensity; // 0.0 to 1.0
    private WeatherCellType type;
    private long creationTime;
    private long lifespan; // In milliseconds
    private boolean active;
    
    // Movement properties
    private Vector2f velocity;
    private float rotationSpeed; // Degrees per second
    private float currentRotation;
    
    // Evolution properties
    private float growthRate; // Radius change per second
    private float intensityChangeRate; // Intensity change per second
    private float maxRadius;
    private float minRadius;
    
    public WeatherCell(Vector2f center, WeatherCellType type, float intensity) {
        this.center = new Vector2f(center);
        this.type = type;
        this.intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        this.creationTime = System.currentTimeMillis();
        this.active = true;
        
        initializeCellProperties();
        generateBaseCondition();
        generateMovement();
    }
    
    private void initializeCellProperties() {
        // Set properties based on cell type
        this.radius = type.getBaseRadius() * (0.7f + intensity * 0.6f);
        this.maxRadius = radius * 2.0f;
        this.minRadius = radius * 0.3f;
        this.lifespan = (long)(type.getBaseLifespan() * (0.8f + intensity * 0.4f));
        
        // Growth and change rates
        this.growthRate = type.getGrowthRate() * (0.5f + intensity * 0.5f);
        this.intensityChangeRate = type.getIntensityChangeRate();
        this.rotationSpeed = type.getRotationSpeed() * (0.8f + intensity * 0.4f);
    }
    
    private void generateBaseCondition() {
        this.baseCondition = new WeatherCondition();
        
        // Modify base conditions based on cell type and intensity
        switch (type) {
            case HIGH_PRESSURE:
                baseCondition.setPressure(1020.0f + intensity * 15.0f);
                baseCondition.setCloudCover(0.1f - intensity * 0.05f);
                baseCondition.setWindSpeed(3.0f + intensity * 2.0f);
                baseCondition.setPrecipitation(PrecipitationType.NONE);
                break;
                
            case LOW_PRESSURE:
                baseCondition.setPressure(995.0f - intensity * 20.0f);
                baseCondition.setCloudCover(0.7f + intensity * 0.3f);
                baseCondition.setWindSpeed(8.0f + intensity * 7.0f);
                baseCondition.setHumidity(0.8f + intensity * 0.2f);
                if (intensity > 0.5f) {
                    baseCondition.setPrecipitation(PrecipitationType.RAIN);
                    baseCondition.setPrecipitationIntensity(intensity * 0.6f);
                }
                break;
                
            case WARM_FRONT:
                baseCondition.setTemperature(22.0f + intensity * 8.0f);
                baseCondition.setHumidity(0.6f + intensity * 0.3f);
                baseCondition.setCloudCover(0.5f + intensity * 0.4f);
                baseCondition.setWindSpeed(5.0f + intensity * 5.0f);
                break;
                
            case COLD_FRONT:
                baseCondition.setTemperature(15.0f - intensity * 10.0f);
                baseCondition.setHumidity(0.4f + intensity * 0.4f);
                baseCondition.setCloudCover(0.6f + intensity * 0.4f);
                baseCondition.setWindSpeed(10.0f + intensity * 10.0f);
                if (intensity > 0.4f) {
                    baseCondition.setPrecipitation(PrecipitationType.RAIN);
                    baseCondition.setPrecipitationIntensity(intensity * 0.5f);
                }
                break;
                
            case THERMAL_LOW:
                baseCondition.setTemperature(28.0f + intensity * 12.0f);
                baseCondition.setPressure(1005.0f - intensity * 10.0f);
                baseCondition.setHumidity(0.3f + intensity * 0.2f);
                baseCondition.setWindSpeed(2.0f + intensity * 3.0f);
                break;
                
            case CONVERGENCE_ZONE:
                baseCondition.setHumidity(0.8f + intensity * 0.2f);
                baseCondition.setCloudCover(0.8f + intensity * 0.2f);
                baseCondition.setWindSpeed(6.0f + intensity * 8.0f);
                if (intensity > 0.3f) {
                    baseCondition.setPrecipitation(PrecipitationType.THUNDERSTORM);
                    baseCondition.setPrecipitationIntensity(intensity * 0.8f);
                }
                break;
        }
    }
    
    private void generateMovement() {
        // Generate movement based on cell type
        float speed = type.getMovementSpeed() * (0.8f + (float)Math.random() * 0.4f);
        float angle = (float)(Math.random() * Math.PI * 2);
        
        // Modify movement based on type
        switch (type) {
            case HIGH_PRESSURE:
                speed *= 0.7f; // High pressure systems move slower
                break;
            case LOW_PRESSURE:
                speed *= 1.2f; // Low pressure systems move faster
                break;
            case WARM_FRONT:
            case COLD_FRONT:
                speed *= 1.5f; // Fronts move quickly
                break;
        }
        
        this.velocity = new Vector2f(
            (float)Math.cos(angle) * speed,
            (float)Math.sin(angle) * speed
        );
    }
    
    /**
     * Updates the weather cell's position, size, and properties
     */
    public void update(float deltaTime) {
        if (!active) return;
        
        // Move the cell
        center.x += velocity.x * deltaTime;
        center.y += velocity.y * deltaTime;
        
        // Rotate the cell
        currentRotation += rotationSpeed * deltaTime;
        currentRotation = currentRotation % 360.0f;
        
        // Update size
        radius += growthRate * deltaTime;
        radius = Math.max(minRadius, Math.min(maxRadius, radius));
        
        // Update intensity based on age
        long age = System.currentTimeMillis() - creationTime;
        float ageRatio = (float)age / lifespan;
        
        if (ageRatio > 1.0f) {
            active = false;
            return;
        }
        
        // Intensity follows a lifecycle curve
        float targetIntensity = getIntensityForAge(ageRatio);
        float intensityDiff = targetIntensity - intensity;
        intensity += intensityDiff * intensityChangeRate * deltaTime;
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));
        
        // Deactivate if intensity becomes too low
        if (intensity < 0.1f && ageRatio > 0.5f) {
            active = false;
        }
    }
    
    /**
     * Gets the target intensity for a given age ratio
     */
    private float getIntensityForAge(float ageRatio) {
        if (ageRatio < 0.2f) {
            // Growing phase
            return ageRatio / 0.2f;
        } else if (ageRatio < 0.6f) {
            // Mature phase
            return 1.0f;
        } else {
            // Declining phase
            return 1.0f - ((ageRatio - 0.6f) / 0.4f);
        }
    }
    
    /**
     * Gets the cell's influence at a specific position
     */
    public float getInfluenceAt(Vector2f worldPos) {
        if (!active) return 0.0f;
        
        float distance = center.distance(worldPos);
        if (distance > radius) return 0.0f;
        
        // Calculate influence with smooth falloff
        float normalizedDistance = distance / radius;
        float influence = 1.0f - (normalizedDistance * normalizedDistance);
        
        // Apply intensity modifier
        return influence * intensity;
    }
    
    /**
     * Gets weather conditions at a specific position affected by this cell
     */
    public WeatherCondition getWeatherAt(Vector2f worldPos, WeatherCondition ambientWeather) {
        float influence = getInfluenceAt(worldPos);
        if (influence <= 0.0f) return ambientWeather;
        
        WeatherCondition cellWeather = new WeatherCondition();
        
        // Interpolate between ambient and cell conditions
        cellWeather.setTemperature(lerp(ambientWeather.getTemperature(), 
            baseCondition.getTemperature(), influence));
        
        cellWeather.setHumidity(lerp(ambientWeather.getHumidity(), 
            baseCondition.getHumidity(), influence));
        
        cellWeather.setPressure(lerp(ambientWeather.getPressure(), 
            baseCondition.getPressure(), influence));
        
        cellWeather.setVisibility(lerp(ambientWeather.getVisibility(), 
            baseCondition.getVisibility(), influence));
        
        cellWeather.setCloudCover(lerp(ambientWeather.getCloudCover(), 
            baseCondition.getCloudCover(), influence));
        
        cellWeather.setWindSpeed(lerp(ambientWeather.getWindSpeed(), 
            baseCondition.getWindSpeed(), influence));
        
        // Wind direction influenced by cell rotation and type
        float windAngle = currentRotation;
        if (type == WeatherCellType.LOW_PRESSURE) {
            // Cyclonic rotation (counterclockwise in northern hemisphere)
            Vector2f relativePos = new Vector2f(worldPos.x - center.x, worldPos.y - center.y);
            windAngle = (float)Math.toDegrees(Math.atan2(relativePos.y, relativePos.x)) + 90.0f;
        }
        cellWeather.setWindDirection(windAngle);
        
        // Precipitation
        if (baseCondition.getPrecipitation() != PrecipitationType.NONE && influence > 0.3f) {
            cellWeather.setPrecipitation(baseCondition.getPrecipitation());
            cellWeather.setPrecipitationIntensity(
                baseCondition.getPrecipitationIntensity() * influence);
        } else {
            cellWeather.setPrecipitation(ambientWeather.getPrecipitation());
            cellWeather.setPrecipitationIntensity(ambientWeather.getPrecipitationIntensity());
        }
        
        return cellWeather;
    }
    
    /**
     * Checks if this cell can interact with another cell
     */
    public boolean canInteractWith(WeatherCell other) {
        if (!active || !other.active) return false;
        
        float distance = center.distance(other.center);
        float interactionRange = (radius + other.radius) * 1.2f;
        
        return distance <= interactionRange;
    }
    
    /**
     * Interacts with another weather cell, potentially creating new phenomena
     */
    public void interactWith(WeatherCell other) {
        if (!canInteractWith(other)) return;
        
        // Different interactions based on cell types
        if (type == WeatherCellType.WARM_FRONT && other.type == WeatherCellType.COLD_FRONT) {
            // Warm and cold fronts can create storms
            enhanceIntensity(0.2f);
            other.enhanceIntensity(0.2f);
        } else if (type == WeatherCellType.HIGH_PRESSURE && other.type == WeatherCellType.LOW_PRESSURE) {
            // Pressure differences create wind
            increaseWindSpeed(5.0f);
            other.increaseWindSpeed(5.0f);
        }
    }
    
    private void enhanceIntensity(float amount) {
        intensity = Math.min(1.0f, intensity + amount);
    }
    
    private void increaseWindSpeed(float amount) {
        baseCondition.setWindSpeed(baseCondition.getWindSpeed() + amount);
    }
    
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    // Getters
    public Vector2f getCenter() { return new Vector2f(center); }
    public float getRadius() { return radius; }
    public WeatherCondition getBaseCondition() { return baseCondition; }
    public float getIntensity() { return intensity; }
    public WeatherCellType getType() { return type; }
    public boolean isActive() { return active; }
    public Vector2f getVelocity() { return new Vector2f(velocity); }
    public float getCurrentRotation() { return currentRotation; }
    public long getAge() { return System.currentTimeMillis() - creationTime; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    public void setVelocity(Vector2f velocity) { this.velocity = new Vector2f(velocity); }
    
    /**
     * Checks if this weather cell should be removed
     */
    public boolean shouldRemove() {
        return !active || (System.currentTimeMillis() - creationTime > lifespan);
    }
    
    @Override
    public String toString() {
        return String.format("WeatherCell[%s, intensity=%.2f, pos=(%.1f,%.1f), radius=%.1f, active=%s]",
            type, intensity, center.x, center.y, radius, active);
    }
}