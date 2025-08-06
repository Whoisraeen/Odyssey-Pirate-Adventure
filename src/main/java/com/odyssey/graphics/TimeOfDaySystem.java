package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Advanced time of day system with celestial body movement and dynamic lighting.
 * Provides realistic sun and moon positioning, day/night cycles, and seasonal variations.
 */
public class TimeOfDaySystem {
    private static final Logger logger = LoggerFactory.getLogger(TimeOfDaySystem.class);
    
    // Time constants
    private static final double DAY_LENGTH_SECONDS = 1200.0; // 20 minutes real time = 1 game day
    private static final double YEAR_LENGTH_DAYS = 365.0;
    private static final double LUNAR_CYCLE_DAYS = 29.5; // Lunar month
    
    // Celestial body properties
    private static final float SUN_DISTANCE = 1000.0f;
    private static final float MOON_DISTANCE = 800.0f;
    private static final float EARTH_AXIAL_TILT = (float) Math.toRadians(23.5); // Earth's axial tilt
    
    // Time tracking
    private double gameTime = 0.0; // Time in seconds since world creation
    private double dayTime = 0.0; // Time within current day (0.0 to 1.0)
    private double yearTime = 0.0; // Time within current year (0.0 to 1.0)
    private double lunarTime = 0.0; // Time within lunar cycle (0.0 to 1.0)
    
    // Celestial positions
    private final Vector3f sunPosition = new Vector3f();
    private final Vector3f moonPosition = new Vector3f();
    private final Vector3f sunDirection = new Vector3f();
    private final Vector3f moonDirection = new Vector3f();
    
    // Lighting properties
    private final Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.8f);
    private final Vector3f moonColor = new Vector3f(0.7f, 0.8f, 1.0f);
    private final Vector3f ambientColor = new Vector3f();
    private final Vector3f skyColor = new Vector3f();
    private final Vector3f horizonColor = new Vector3f();
    
    private float sunIntensity = 1.0f;
    private float moonIntensity = 0.3f;
    private float ambientIntensity = 0.1f;
    
    // Atmospheric properties
    private float atmosphericDensity = 1.0f;
    private float sunsetIntensity = 1.0f;
    private final Vector3f rayleighScattering = new Vector3f(0.0025f, 0.0041f, 0.0098f);
    private final Vector3f mieScattering = new Vector3f(0.004f, 0.004f, 0.004f);
    
    // Season and weather integration
    private Season currentSeason = Season.SPRING;
    private float cloudCover = 0.0f;
    private float weatherIntensity = 0.0f;
    
    public enum Season {
        SPRING(0.0f, 0.25f),
        SUMMER(0.25f, 0.5f),
        AUTUMN(0.5f, 0.75f),
        WINTER(0.75f, 1.0f);
        
        public final float startTime;
        public final float endTime;
        
        Season(float startTime, float endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
    
    public enum TimeOfDay {
        DAWN(0.2f, 0.3f),
        MORNING(0.3f, 0.45f),
        NOON(0.45f, 0.55f),
        AFTERNOON(0.55f, 0.7f),
        DUSK(0.7f, 0.8f),
        NIGHT(0.8f, 1.0f),
        MIDNIGHT(0.0f, 0.2f);
        
        public final float startTime;
        public final float endTime;
        
        TimeOfDay(float startTime, float endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
    
    /**
     * Creates a new time of day system.
     */
    public TimeOfDaySystem() {
        // Initialize with noon
        setTimeOfDay(0.5);
        updateCelestialBodies();
        updateLighting();
        logger.info("Time of day system initialized");
    }
    
    /**
     * Updates the time of day system.
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        // Update game time
        gameTime += deltaTime;
        
        // Calculate day progress (0.0 to 1.0)
        dayTime = (gameTime % DAY_LENGTH_SECONDS) / DAY_LENGTH_SECONDS;
        
        // Calculate year progress for seasonal changes
        double totalDays = gameTime / DAY_LENGTH_SECONDS;
        yearTime = (totalDays % YEAR_LENGTH_DAYS) / YEAR_LENGTH_DAYS;
        
        // Calculate lunar cycle progress
        lunarTime = (totalDays % LUNAR_CYCLE_DAYS) / LUNAR_CYCLE_DAYS;
        
        // Update season
        updateSeason();
        
        // Update celestial body positions
        updateCelestialBodies();
        
        // Update lighting based on celestial positions
        updateLighting();
        
        // Update atmospheric effects
        updateAtmosphere();
    }
    
    /**
     * Updates the current season based on year progress.
     */
    private void updateSeason() {
        Season newSeason = getCurrentSeason();
        if (newSeason != currentSeason) {
            currentSeason = newSeason;
            logger.debug("Season changed to: {}", currentSeason);
        }
    }
    
    /**
     * Updates celestial body positions based on time.
     */
    private void updateCelestialBodies() {
        // Calculate sun position
        float sunAngle = (float) (dayTime * 2.0 * Math.PI - Math.PI / 2.0); // Start at dawn
        float seasonalTilt = (float) Math.sin(yearTime * 2.0 * Math.PI) * EARTH_AXIAL_TILT;
        
        sunPosition.set(
            (float) Math.cos(sunAngle) * SUN_DISTANCE,
            (float) Math.sin(sunAngle) * SUN_DISTANCE + seasonalTilt * SUN_DISTANCE * 0.1f,
            0.0f
        );
        
        sunDirection.set(sunPosition).normalize().negate();
        
        // Calculate moon position (opposite to sun with lunar cycle offset)
        float moonAngle = sunAngle + (float) Math.PI + (float) (lunarTime * 2.0 * Math.PI * 0.1);
        
        moonPosition.set(
            (float) Math.cos(moonAngle) * MOON_DISTANCE,
            (float) Math.sin(moonAngle) * MOON_DISTANCE,
            (float) Math.sin(lunarTime * 2.0 * Math.PI) * MOON_DISTANCE * 0.2f
        );
        
        moonDirection.set(moonPosition).normalize().negate();
    }
    
    /**
     * Updates lighting properties based on celestial positions.
     */
    private void updateLighting() {
        // Calculate sun elevation
        float sunElevation = sunPosition.y / SUN_DISTANCE;
        
        // Update sun intensity based on elevation
        if (sunElevation > 0.0f) {
            sunIntensity = Math.min(1.0f, sunElevation * 2.0f);
        } else {
            sunIntensity = 0.0f;
        }
        
        // Update moon intensity (stronger when sun is down)
        float moonElevation = moonPosition.y / MOON_DISTANCE;
        if (moonElevation > 0.0f && sunElevation < 0.1f) {
            float lunarPhase = (float) Math.cos(lunarTime * 2.0 * Math.PI);
            moonIntensity = Math.max(0.1f, moonElevation * (0.5f + lunarPhase * 0.5f));
        } else {
            moonIntensity = 0.0f;
        }
        
        // Update ambient lighting
        float baseAmbient = 0.1f;
        float dayAmbient = Math.max(0.0f, sunElevation) * 0.3f;
        float nightAmbient = Math.max(0.0f, moonElevation) * moonIntensity * 0.1f;
        ambientIntensity = baseAmbient + dayAmbient + nightAmbient;
        
        // Update sun color based on elevation (redder at horizon)
        if (sunElevation > 0.0f) {
            float horizonFactor = 1.0f - Math.abs(sunElevation);
            sunColor.set(
                1.0f,
                0.95f - horizonFactor * 0.3f,
                0.8f - horizonFactor * 0.5f
            );
        }
        
        // Update ambient color based on time of day
        updateAmbientColor();
        
        // Update sky colors
        updateSkyColors();
    }
    
    /**
     * Updates ambient color based on time of day and weather.
     */
    private void updateAmbientColor() {
        TimeOfDay timeOfDay = getCurrentTimeOfDay();
        
        switch (timeOfDay) {
            case DAWN:
                ambientColor.set(0.8f, 0.6f, 0.4f);
                break;
            case MORNING:
            case AFTERNOON:
                ambientColor.set(0.9f, 0.9f, 0.8f);
                break;
            case NOON:
                ambientColor.set(1.0f, 1.0f, 1.0f);
                break;
            case DUSK:
                ambientColor.set(0.9f, 0.5f, 0.3f);
                break;
            case NIGHT:
            case MIDNIGHT:
                ambientColor.set(0.3f, 0.4f, 0.6f);
                break;
        }
        
        // Apply weather effects
        if (cloudCover > 0.0f) {
            ambientColor.lerp(new Vector3f(0.7f, 0.7f, 0.7f), cloudCover * 0.5f);
        }
    }
    
    /**
     * Updates sky and horizon colors for atmospheric rendering.
     */
    private void updateSkyColors() {
        float sunElevation = sunPosition.y / SUN_DISTANCE;
        
        if (sunElevation > 0.0f) {
            // Daytime sky
            skyColor.set(0.4f, 0.7f, 1.0f);
            horizonColor.set(0.8f, 0.9f, 1.0f);
            
            // Sunset/sunrise coloring
            if (sunElevation < 0.3f) {
                float sunsetFactor = 1.0f - (sunElevation / 0.3f);
                skyColor.lerp(new Vector3f(1.0f, 0.6f, 0.3f), sunsetFactor * 0.7f);
                horizonColor.lerp(new Vector3f(1.0f, 0.4f, 0.2f), sunsetFactor);
            }
        } else {
            // Nighttime sky
            skyColor.set(0.05f, 0.05f, 0.15f);
            horizonColor.set(0.1f, 0.1f, 0.2f);
            
            // Moon lighting
            if (moonIntensity > 0.0f) {
                skyColor.lerp(new Vector3f(0.1f, 0.15f, 0.3f), moonIntensity * 0.5f);
            }
        }
        
        // Apply cloud cover
        if (cloudCover > 0.0f) {
            skyColor.lerp(new Vector3f(0.6f, 0.6f, 0.6f), cloudCover * 0.8f);
            horizonColor.lerp(new Vector3f(0.7f, 0.7f, 0.7f), cloudCover * 0.6f);
        }
    }
    
    /**
     * Updates atmospheric effects for scattering calculations.
     */
    private void updateAtmosphere() {
        // Adjust atmospheric density based on weather
        atmosphericDensity = 1.0f + weatherIntensity * 0.5f + cloudCover * 0.3f;
        
        // Calculate sunset intensity for enhanced scattering
        float sunElevation = sunPosition.y / SUN_DISTANCE;
        if (sunElevation > -0.1f && sunElevation < 0.1f) {
            sunsetIntensity = 1.0f + (1.0f - Math.abs(sunElevation * 10.0f)) * 2.0f;
        } else {
            sunsetIntensity = 1.0f;
        }
    }
    
    /**
     * Gets the current season based on year progress.
     */
    public Season getCurrentSeason() {
        for (Season season : Season.values()) {
            if (yearTime >= season.startTime && yearTime < season.endTime) {
                return season;
            }
        }
        return Season.WINTER; // Wrap around
    }
    
    /**
     * Gets the current time of day period.
     */
    public TimeOfDay getCurrentTimeOfDay() {
        for (TimeOfDay time : TimeOfDay.values()) {
            if (time == TimeOfDay.NIGHT && dayTime >= time.startTime) {
                return time;
            } else if (time == TimeOfDay.MIDNIGHT && dayTime < time.endTime) {
                return time;
            } else if (dayTime >= time.startTime && dayTime < time.endTime) {
                return time;
            }
        }
        return TimeOfDay.MIDNIGHT; // Default
    }
    
    /**
     * Sets the time of day directly (0.0 = midnight, 0.5 = noon, 1.0 = midnight).
     */
    public void setTimeOfDay(double time) {
        dayTime = Math.max(0.0, Math.min(1.0, time));
        gameTime = dayTime * DAY_LENGTH_SECONDS;
        updateCelestialBodies();
        updateLighting();
    }
    
    /**
     * Sets the current season directly.
     */
    public void setSeason(Season season) {
        this.currentSeason = season;
        yearTime = (season.startTime + season.endTime) / 2.0f;
        updateCelestialBodies();
        updateLighting();
    }
    
    /**
     * Updates weather-related properties that affect lighting.
     */
    public void updateWeather(float cloudCover, float weatherIntensity) {
        this.cloudCover = Math.max(0.0f, Math.min(1.0f, cloudCover));
        this.weatherIntensity = Math.max(0.0f, Math.min(1.0f, weatherIntensity));
        updateLighting();
        updateAtmosphere();
    }
    
    // Getters for celestial positions
    public Vector3f getSunPosition() { return new Vector3f(sunPosition); }
    public Vector3f getMoonPosition() { return new Vector3f(moonPosition); }
    public Vector3f getSunDirection() { return new Vector3f(sunDirection); }
    public Vector3f getMoonDirection() { return new Vector3f(moonDirection); }
    
    // Getters for lighting properties
    public Vector3f getSunColor() { return new Vector3f(sunColor); }
    public Vector3f getMoonColor() { return new Vector3f(moonColor); }
    public Vector3f getAmbientColor() { return new Vector3f(ambientColor); }
    public Vector3f getSkyColor() { return new Vector3f(skyColor); }
    public Vector3f getHorizonColor() { return new Vector3f(horizonColor); }
    
    public float getSunIntensity() { return sunIntensity; }
    public float getMoonIntensity() { return moonIntensity; }
    public float getAmbientIntensity() { return ambientIntensity; }
    
    // Getters for atmospheric properties
    public float getAtmosphericDensity() { return atmosphericDensity; }
    public float getSunsetIntensity() { return sunsetIntensity; }
    public Vector3f getRayleighScattering() { return new Vector3f(rayleighScattering); }
    public Vector3f getMieScattering() { return new Vector3f(mieScattering); }
    
    // Getters for time properties
    public double getGameTime() { return gameTime; }
    public double getDayTime() { return dayTime; }
    public double getYearTime() { return yearTime; }
    public double getLunarTime() { return lunarTime; }
    public float getLunarPhase() { return (float) Math.cos(lunarTime * 2.0 * Math.PI); }
    
    // Utility methods
    public boolean isDaytime() { return sunIntensity > 0.1f; }
    public boolean isNighttime() { return sunIntensity < 0.1f; }
    public boolean isSunrise() { return getCurrentTimeOfDay() == TimeOfDay.DAWN; }
    public boolean isSunset() { return getCurrentTimeOfDay() == TimeOfDay.DUSK; }
    
    /**
     * Gets the current day number since world creation.
     */
    public int getCurrentDay() {
        return (int) (gameTime / DAY_LENGTH_SECONDS);
    }
    
    /**
     * Gets the current year number since world creation.
     */
    public int getCurrentYear() {
        return (int) (getCurrentDay() / YEAR_LENGTH_DAYS);
    }
    
    /**
     * Gets a formatted time string (HH:MM).
     */
    public String getFormattedTime() {
        int hours = (int) (dayTime * 24);
        int minutes = (int) ((dayTime * 24 * 60) % 60);
        return String.format("%02d:%02d", hours, minutes);
    }
    
    /**
     * Initializes the time of day system.
     */
    public void initialize() {
        // Initialize any resources if needed
        // For now, this is a no-op as the system is self-contained
    }
    
    /**
     * Cleans up the time of day system resources.
     */
    public void cleanup() {
        // Clean up any resources if needed
        // For now, this is a no-op as the system doesn't hold external resources
    }
}