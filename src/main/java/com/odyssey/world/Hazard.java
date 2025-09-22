package com.odyssey.world;

import org.joml.Vector2f;
import com.odyssey.world.weather.WeatherCondition;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Represents dangerous areas and obstacles in the world that affect navigation and trade.
 * 
 * <p>Hazards are dynamic environmental features that can significantly impact ship navigation,
 * crew safety, and cargo security. They range from natural phenomena like storms and whirlpools
 * to supernatural threats like siren waters and ghost ships, as well as human-made dangers
 * such as pirate ambushes and naval patrols.
 * 
 * <p>Each hazard has several key properties:
 * <ul>
 *   <li><strong>Type</strong> - Defines the nature and behavior of the hazard</li>
 *   <li><strong>Severity</strong> - Determines the intensity and danger level</li>
 *   <li><strong>Position & Radius</strong> - Defines the area of effect</li>
 *   <li><strong>Duration</strong> - How long the hazard persists (permanent or temporary)</li>
 *   <li><strong>Intensity</strong> - Current strength level (0.0 to 1.0)</li>
 *   <li><strong>Environmental Effects</strong> - Impact on visibility, speed, damage risk, etc.</li>
 * </ul>
 * 
 * <p>Hazards interact with the game world in several ways:
 * <ul>
 *   <li><strong>Weather Interaction</strong> - Some hazards are affected by weather conditions</li>
 *   <li><strong>Dynamic Movement</strong> - Moving hazards can change position over time</li>
 *   <li><strong>Growth/Shrinkage</strong> - Hazards can expand or contract based on various factors</li>
 *   <li><strong>Intensity Patterns</strong> - Different hazard types follow different intensity curves</li>
 *   <li><strong>Hazard Interactions</strong> - Some hazards can enhance or modify others</li>
 * </ul>
 * 
 * <p>The hazard system supports multiple detection methods:
 * <ul>
 *   <li><strong>Visual Detection</strong> - Based on visibility and distance</li>
 *   <li><strong>Sonar Detection</strong> - For physical hazards detectable by sonar</li>
 *   <li><strong>Magical Detection</strong> - For supernatural hazards requiring magic</li>
 *   <li><strong>Experience Detection</strong> - Based on crew skill and experience</li>
 * </ul>
 * 
 * <p>Hazards are designed to create dynamic, challenging gameplay scenarios that require
 * strategic navigation, crew management, and risk assessment. They add depth to the
 * exploration and trading aspects of the game while providing opportunities for
 * player skill development and strategic decision-making.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since 1.0
 */
public class Hazard {
    
    private String hazardId;
    private String name;
    private HazardType type;
    private HazardSeverity severity;
    private Vector2f position;
    private float radius;
    private boolean isActive;
    private boolean isPermanent;
    
    // Temporal properties
    private long creationTime;
    private long duration; // In milliseconds, -1 for permanent
    private float intensity; // 0.0 to 1.0
    private float growthRate; // How fast the hazard grows/shrinks
    
    // Environmental effects
    private float visibilityReduction; // 0.0 to 1.0
    private float speedReduction; // 0.0 to 1.0
    private float damageRisk; // 0.0 to 1.0 chance per hour
    private float navigationDifficulty; // 0.0 to 1.0
    
    // Dynamic properties
    private Vector2f velocity; // For moving hazards
    private float rotationSpeed; // For rotating hazards
    private float currentRotation;
    private Map<String, Float> customProperties;
    
    // Weather interaction
    private boolean affectedByWeather;
    private float weatherSensitivity; // How much weather affects this hazard
    
    /**
     * Constructs a new hazard with the specified properties.
     * 
     * @param hazardId Unique identifier for this hazard instance
     * @param name Display name for the hazard
     * @param type The type of hazard (determines behavior and properties)
     * @param severity The severity level (affects intensity and duration)
     * @param position World position of the hazard center
     * @param radius Radius of the hazard's area of effect
     */
    public Hazard(String hazardId, String name, HazardType type, HazardSeverity severity,
                  Vector2f position, float radius) {
        this.hazardId = hazardId;
        this.name = name;
        this.type = type;
        this.severity = severity;
        this.position = position;
        this.radius = radius;
        this.isActive = true;
        this.isPermanent = type.isPermanent();
        this.creationTime = System.currentTimeMillis();
        this.duration = isPermanent ? -1 : (long)(type.getBaseDuration() * severity.getDurationMultiplier());
        this.intensity = 0.5f + severity.getIntensityBonus();
        this.customProperties = new HashMap<>();
        
        // Initialize properties based on type and severity
        initializeProperties();
    }
    
    private void initializeProperties() {
        // Base properties from type
        this.visibilityReduction = type.getBaseVisibilityReduction() * severity.getEffectMultiplier();
        this.speedReduction = type.getBaseSpeedReduction() * severity.getEffectMultiplier();
        this.damageRisk = type.getBaseDamageRisk() * severity.getEffectMultiplier();
        this.navigationDifficulty = type.getBaseNavigationDifficulty() * severity.getEffectMultiplier();
        
        // Movement properties
        this.velocity = type.isMoving() ? generateMovementVector() : new Vector2f(0, 0);
        this.rotationSpeed = type.getRotationSpeed();
        this.currentRotation = 0.0f;
        this.growthRate = type.getGrowthRate() * severity.getGrowthMultiplier();
        
        // Weather sensitivity
        this.affectedByWeather = type.isWeatherSensitive();
        this.weatherSensitivity = type.getWeatherSensitivity();
        
        // Clamp values to valid ranges
        clampProperties();
    }
    
    private Vector2f generateMovementVector() {
        Random random = new Random();
        float speed = type.getMovementSpeed() * (0.8f + random.nextFloat() * 0.4f);
        float direction = random.nextFloat() * 360.0f;
        
        float radians = (float) Math.toRadians(direction);
        return new Vector2f(
            (float) (Math.cos(radians) * speed),
            (float) (Math.sin(radians) * speed)
        );
    }
    
    private void clampProperties() {
        visibilityReduction = Math.max(0.0f, Math.min(1.0f, visibilityReduction));
        speedReduction = Math.max(0.0f, Math.min(1.0f, speedReduction));
        damageRisk = Math.max(0.0f, Math.min(1.0f, damageRisk));
        navigationDifficulty = Math.max(0.0f, Math.min(1.0f, navigationDifficulty));
        intensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    /**
     * Updates the hazard state based on elapsed time and current weather conditions.
     * 
     * <p>This method handles all dynamic aspects of hazard behavior including:
     * <ul>
     *   <li>Duration management and expiration</li>
     *   <li>Position updates for moving hazards</li>
     *   <li>Rotation and growth/shrinkage</li>
     *   <li>Intensity changes based on lifecycle patterns</li>
     *   <li>Weather interaction effects</li>
     *   <li>Custom property updates</li>
     * </ul>
     * 
     * @param deltaTime Time elapsed since last update in seconds
     * @param weather Current weather conditions at the hazard location
     */
    public void update(float deltaTime, WeatherCondition weather) {
        if (!isActive) return;
        
        // Check if hazard has expired
        if (!isPermanent && duration > 0) {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed >= duration) {
                isActive = false;
                return;
            }
        }
        
        // Update position for moving hazards
        if (type.isMoving() && velocity != null) {
            position.x += velocity.x * deltaTime;
            position.y += velocity.y * deltaTime;
            
            // Apply some randomness to movement
            Random random = new Random();
            if (random.nextFloat() < 0.1f) { // 10% chance to change direction slightly
                float angleChange = (random.nextFloat() - 0.5f) * 30.0f; // ±15 degrees
                rotateVelocity(angleChange);
            }
        }
        
        // Update rotation
        if (rotationSpeed != 0) {
            currentRotation += rotationSpeed * deltaTime;
            currentRotation = currentRotation % 360.0f;
        }
        
        // Update size based on growth rate
        if (growthRate != 0) {
            radius += growthRate * deltaTime;
            radius = Math.max(type.getMinRadius(), Math.min(type.getMaxRadius(), radius));
        }
        
        // Update intensity based on lifecycle
        updateIntensity();
        
        // Apply weather effects
        if (affectedByWeather && weather != null) {
            applyWeatherEffects(weather);
        }
        
        // Update custom properties
        updateCustomProperties(deltaTime);
    }
    
    private void rotateVelocity(float angleDegrees) {
        float radians = (float) Math.toRadians(angleDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        
        float newX = velocity.x * cos - velocity.y * sin;
        float newY = velocity.x * sin + velocity.y * cos;
        
        velocity.x = newX;
        velocity.y = newY;
    }
    
    private void updateIntensity() {
        if (isPermanent) return;
        
        long elapsed = System.currentTimeMillis() - creationTime;
        float progress = (float) elapsed / duration;
        
        switch (type.getIntensityPattern()) {
            case CONSTANT:
                // Intensity remains constant
                break;
            case GROWING:
                intensity = Math.min(1.0f, 0.3f + progress * 0.7f);
                break;
            case DECLINING:
                intensity = Math.max(0.1f, 1.0f - progress * 0.9f);
                break;
            case PEAK_MIDDLE:
                if (progress < 0.5f) {
                    intensity = 0.3f + (progress * 2.0f) * 0.7f;
                } else {
                    intensity = 1.0f - ((progress - 0.5f) * 2.0f) * 0.7f;
                }
                break;
            case FLUCTUATING:
                float baseIntensity = 0.5f + severity.getIntensityBonus();
                float fluctuation = (float) Math.sin(elapsed * 0.001f) * 0.3f;
                intensity = Math.max(0.1f, Math.min(1.0f, baseIntensity + fluctuation));
                break;
        }
    }
    
    private void applyWeatherEffects(WeatherCondition weather) {
        float weatherImpact = weatherSensitivity * intensity;
        
        switch (type) {
            case FOG_BANK:
                // Fog is enhanced by high humidity and low wind
                float humidityBonus = weather.getHumidity() * 0.3f;
                float windPenalty = weather.getWindSpeed() / 30.0f * 0.2f;
                visibilityReduction += (humidityBonus - windPenalty) * weatherImpact;
                break;
                
            case STORM_SYSTEM:
                // Storm systems are affected by pressure and precipitation
                if (weather.getPressure() < 1000.0f) {
                    intensity += 0.1f * weatherImpact;
                }
                if (weather.getPrecipitation().getIntensityFactor() > 0.5f) {
                    damageRisk += 0.15f * weatherImpact;
                }
                break;
                
            case ICE_FIELD:
                // Ice fields are affected by temperature
                if (weather.getTemperature() > 5.0f) {
                    intensity -= 0.05f * weatherImpact;
                    radius *= 0.99f; // Slowly melting
                }
                break;
                
            case WHIRLPOOL:
                // Whirlpools are enhanced by storms
                if (weather.getWindSpeed() > 20.0f) {
                    intensity += 0.08f * weatherImpact;
                    rotationSpeed *= 1.1f;
                }
                break;
                
            case REEF:
                // Reefs are not significantly affected by weather
                break;
                
            case SHALLOW_WATERS:
                // Shallow waters are not significantly affected by weather
                break;
                
            case SIREN_WATERS:
                // Siren waters may be enhanced by moonlight (not implemented yet)
                break;
                
            case CURSED_WATERS:
                // Cursed waters are not affected by normal weather
                break;
                
            case GHOST_SHIP:
                // Ghost ships may be more active during storms
                if (weather.getWindSpeed() > 15.0f) {
                    intensity += 0.05f * weatherImpact;
                }
                break;
                
            case PIRATE_AMBUSH:
                // Pirates prefer poor visibility conditions
                if (weather.getVisibility() < 500.0f) {
                    customProperties.put("concealment_bonus", 0.2f);
                }
                break;
                
            case NAVAL_PATROL:
                // Naval patrols are less active in severe weather
                if (weather.getWindSpeed() > 20.0f) {
                    intensity -= 0.1f * weatherImpact;
                }
                break;
                
            case SMUGGLER_HIDEOUT:
                // Smuggler hideouts prefer poor visibility
                if (weather.getVisibility() < 300.0f) {
                    customProperties.put("concealment_bonus", 0.3f);
                }
                break;
                
            case TOXIC_ALGAE:
                // Toxic algae blooms are enhanced by warm, calm conditions
                if (weather.getTemperature() > 25.0f && weather.getWindSpeed() < 5.0f) {
                    intensity += 0.1f * weatherImpact;
                }
                break;
                
            case VOLCANIC_ACTIVITY:
                // Volcanic activity is not significantly affected by surface weather
                break;
                
            case DEAD_ZONE:
                // Dead zones are not significantly affected by weather
                break;
        }
        
        // Re-clamp properties after weather effects
        clampProperties();
    }
    
    private void updateCustomProperties(float deltaTime) {
        switch (type) {
            case WHIRLPOOL:
                // Update whirlpool strength based on rotation
                float whirlpoolStrength = intensity * (1.0f + Math.abs(rotationSpeed) / 10.0f);
                customProperties.put("whirlpool_strength", whirlpoolStrength);
                break;
                
            case STORM_SYSTEM:
                // Update lightning frequency
                float lightningFreq = intensity * severity.getEffectMultiplier() * 0.1f;
                customProperties.put("lightning_frequency", lightningFreq);
                break;
                
            case PIRATE_AMBUSH:
                // Update ambush readiness
                float readiness = Math.min(1.0f, intensity + (System.currentTimeMillis() - creationTime) / 3600000.0f);
                customProperties.put("ambush_readiness", readiness);
                break;
                
            case SIREN_WATERS:
                // Update siren song strength (varies with time of day)
                long timeOfDay = (System.currentTimeMillis() / 1000) % 86400; // Seconds in a day
                float sirenStrength = intensity * (0.7f + 0.3f * (float) Math.sin(timeOfDay * Math.PI / 43200));
                customProperties.put("siren_strength", sirenStrength);
                break;
                
            case FOG_BANK:
                // Update fog density based on humidity and temperature
                float fogDensity = intensity * (1.0f + Math.abs(currentRotation) / 360.0f);
                customProperties.put("fog_density", fogDensity);
                break;
                
            case ICE_FIELD:
                // Update ice thickness and stability
                float iceThickness = intensity * (1.0f + (System.currentTimeMillis() - creationTime) / 3600000.0f);
                customProperties.put("ice_thickness", iceThickness);
                break;
                
            case REEF:
                // Update reef sharpness and coral health
                float reefSharpness = intensity * (1.0f + Math.abs(currentRotation) / 180.0f);
                customProperties.put("reef_sharpness", reefSharpness);
                break;
                
            case SHALLOW_WATERS:
                // Update water depth and current strength
                float waterDepth = radius * (1.0f - intensity * 0.5f);
                customProperties.put("water_depth", waterDepth);
                break;
                
            case CURSED_WATERS:
                // Update curse strength (varies with lunar cycle)
                long lunarCycle = (System.currentTimeMillis() / 1000) % 2551443; // Lunar month in seconds
                float curseStrength = intensity * (0.8f + 0.2f * (float) Math.sin(lunarCycle * Math.PI / 1275721.5));
                customProperties.put("curse_strength", curseStrength);
                break;
                
            case GHOST_SHIP:
                // Update ghostly presence and haunting intensity
                float ghostlyPresence = intensity * (1.0f + Math.abs(rotationSpeed) / 5.0f);
                customProperties.put("ghostly_presence", ghostlyPresence);
                break;
                
            case NAVAL_PATROL:
                // Update patrol alertness and readiness
                float alertness = Math.min(1.0f, intensity + (System.currentTimeMillis() - creationTime) / 7200000.0f);
                customProperties.put("patrol_alertness", alertness);
                break;
                
            case SMUGGLER_HIDEOUT:
                // Update hideout concealment and activity level
                float concealment = intensity * (1.0f - Math.abs(currentRotation) / 360.0f);
                customProperties.put("hideout_concealment", concealment);
                break;
                
            case TOXIC_ALGAE:
                // Update algae bloom intensity and toxicity
                float bloomIntensity = intensity * (1.0f + Math.abs(growthRate) / 10.0f);
                customProperties.put("bloom_intensity", bloomIntensity);
                break;
                
            case VOLCANIC_ACTIVITY:
                // Update volcanic activity level and heat
                float volcanicActivity = intensity * (1.0f + Math.abs(rotationSpeed) / 2.0f);
                customProperties.put("volcanic_activity", volcanicActivity);
                break;
                
            case DEAD_ZONE:
                // Update dead zone lifelessness and stagnation
                float lifelessness = intensity * (1.0f + Math.abs(growthRate) / 5.0f);
                customProperties.put("lifelessness", lifelessness);
                break;
        }
    }
    
    /**
     * Checks if the given position is within the hazard's area of effect.
     * 
     * @param pos The world position to check
     * @return true if the position is within the hazard's radius and the hazard is active
     */
    public boolean affectsPosition(Vector2f pos) {
        return isActive && position.distance(pos) <= radius;
    }
    
    /**
     * Gets the effect intensity at a given position, accounting for distance falloff.
     * 
     * <p>The effect intensity decreases with distance from the hazard center based on
     * the hazard type's falloff pattern. This provides a more realistic representation
     * of hazard effects that weaken as you move away from the center.
     * 
     * @param pos The world position to check
     * @return Effect intensity from 0.0 (no effect) to 1.0 (full effect)
     */
    public float getEffectIntensity(Vector2f pos) {
        if (!affectsPosition(pos)) return 0.0f;
        
        float distance = position.distance(pos);
        float normalizedDistance = distance / radius;
        
        // Different falloff patterns based on hazard type
        float falloff = type.getFalloffPattern().calculateFalloff(normalizedDistance);
        
        return intensity * falloff;
    }
    
    /**
     * Calculates the navigation difficulty at a given position
     */
    public float getNavigationDifficultyAt(Vector2f pos) {
        float effectIntensity = getEffectIntensity(pos);
        return navigationDifficulty * effectIntensity;
    }
    
    /**
     * Calculates the speed reduction at a given position
     */
    public float getSpeedReductionAt(Vector2f pos) {
        float effectIntensity = getEffectIntensity(pos);
        return speedReduction * effectIntensity;
    }
    
    /**
     * Calculates the visibility reduction at a given position
     */
    public float getVisibilityReductionAt(Vector2f pos) {
        float effectIntensity = getEffectIntensity(pos);
        return visibilityReduction * effectIntensity;
    }
    
    /**
     * Calculates the damage risk per hour at a given position
     */
    public float getDamageRiskAt(Vector2f pos) {
        float effectIntensity = getEffectIntensity(pos);
        return damageRisk * effectIntensity;
    }
    
    /**
     * Checks if the hazard can be detected by the given detection method
     */
    public boolean canBeDetected(DetectionMethod method, float detectionRange, int skillLevel) {
        switch (method) {
            case VISUAL:
                return type.isVisuallyDetectable() && 
                       (1.0f - visibilityReduction) > 0.3f &&
                       detectionRange >= radius * 0.5f;
                       
            case SONAR:
                return type.isSonarDetectable() && 
                       detectionRange >= radius * 0.8f;
                       
            case MAGICAL:
                return type.isMagicallyDetectable() && 
                       skillLevel >= type.getRequiredMagicLevel();
                       
            case EXPERIENCE:
                return skillLevel >= type.getRequiredExperienceLevel() &&
                       detectionRange >= radius * 0.3f;
                       
            default:
                return false;
        }
    }
    
    /**
     * Gets the detection difficulty (0.0 = easy, 1.0 = impossible)
     */
    public float getDetectionDifficulty(DetectionMethod method) {
        float baseDifficulty = type.getDetectionDifficulty();
        
        switch (method) {
            case VISUAL:
                return Math.min(1.0f, baseDifficulty + visibilityReduction * 0.5f);
            case SONAR:
                return baseDifficulty * 0.7f; // Sonar is generally more reliable
            case MAGICAL:
                return baseDifficulty * 0.5f; // Magic is very effective
            case EXPERIENCE:
                return baseDifficulty * 0.8f; // Experience helps
            default:
                return 1.0f;
        }
    }
    
    /**
     * Generates a description of the hazard's current state
     */
    public String generateDescription() {
        StringBuilder desc = new StringBuilder();
        
        desc.append(type.getDescription());
        
        if (intensity > 0.8f) {
            desc.append(" The ").append(name.toLowerCase()).append(" appears particularly dangerous.");
        } else if (intensity < 0.3f) {
            desc.append(" The ").append(name.toLowerCase()).append(" seems to be weakening.");
        }
        
        if (type.isMoving() && velocity != null) {
            float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
            if (speed > 0.1f) {
                desc.append(" It appears to be moving.");
            }
        }
        
        return desc.toString();
    }
    
    /**
     * Gets the warning message for approaching this hazard
     */
    public String getWarningMessage() {
        return type.getWarningMessage().replace("{name}", name)
                                      .replace("{severity}", severity.getDisplayName().toLowerCase());
    }
    
    /**
     * Checks if this hazard interacts with another hazard
     */
    public boolean interactsWith(Hazard other) {
        if (!isActive || !other.isActive) return false;
        
        float distance = position.distance(other.position);
        float interactionRange = (radius + other.radius) * 1.2f;
        
        return distance <= interactionRange && type.canInteractWith(other.type);
    }
    
    /**
     * Applies interaction effects with another hazard
     */
    public void applyInteraction(Hazard other) {
        if (!interactsWith(other)) return;
        
        // Different interaction types
        if (type == HazardType.STORM_SYSTEM && other.type == HazardType.WHIRLPOOL) {
            // Storm enhances whirlpool
            other.intensity = Math.min(1.0f, other.intensity + 0.1f);
            other.rotationSpeed *= 1.1f;
        } else if (type == HazardType.FOG_BANK && other.type == HazardType.PIRATE_AMBUSH) {
            // Fog makes ambushes more effective
            other.customProperties.put("concealment_bonus", 0.3f);
        }
        // Add more interaction types as needed
    }
    
    // Getters and setters
    public String getHazardId() { return hazardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public HazardType getType() { return type; }
    public HazardSeverity getSeverity() { return severity; }
    public Vector2f getPosition() { return position; }
    public void setPosition(Vector2f position) { this.position = position; }
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = Math.max(0, radius); }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isPermanent() { return isPermanent; }
    public long getCreationTime() { return creationTime; }
    public long getDuration() { return duration; }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = Math.max(0.0f, Math.min(1.0f, intensity)); }
    public Vector2f getVelocity() { return velocity; }
    public void setVelocity(Vector2f velocity) { this.velocity = velocity; }
    public float getCurrentRotation() { return currentRotation; }
    public Map<String, Float> getCustomProperties() { return new HashMap<>(customProperties); }
    public void setCustomProperty(String key, float value) { customProperties.put(key, value); }
    public Float getCustomProperty(String key) { return customProperties.get(key); }
    
    // Detection method enum
    public enum DetectionMethod {
        VISUAL, SONAR, MAGICAL, EXPERIENCE
    }
    
    @Override
    public String toString() {
        return String.format("Hazard[%s: %s %s at (%.1f, %.1f), radius=%.1f, intensity=%.2f]",
                           hazardId, severity.getDisplayName(), type.getDisplayName(),
                           position.x, position.y, radius, intensity);
    }
}