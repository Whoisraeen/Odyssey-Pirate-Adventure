package com.odyssey.core;

import org.joml.Vector3f;

/**
 * Manages the global time-of-day system, including lighting changes,
 * environmental effects, and day-night cycles for the game world.
 */
public class TimeOfDaySystem {
    
    // Atmospheric parameters for sky rendering
    private float turbidity = 2.0f; // Clear sky
    private float groundAlbedo = 0.1f; // Ocean surface
    
    // Time constants
    private static final float SECONDS_PER_GAME_HOUR = 60.0f; // 1 game hour = 1 real minute
    private static final float HOURS_PER_DAY = 24.0f;
    private static final float SECONDS_PER_DAY = SECONDS_PER_GAME_HOUR * HOURS_PER_DAY;
    
    // Current time state
    private float currentTime = 12.0f; // Start at noon (12:00)
    private float timeScale = 1.0f;    // Time multiplier (1.0 = normal speed)
    private boolean paused = false;
    
    // Lighting parameters
    private Vector3f ambientLight = new Vector3f(0.3f, 0.3f, 0.4f);
    private Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.8f);
    private Vector3f moonColor = new Vector3f(0.2f, 0.3f, 0.5f);
    private Vector3f sunDirection = new Vector3f(0.0f, 1.0f, 0.0f);
    private float sunIntensity = 1.0f;
    
    // Environmental parameters
    private float fogDensity = 0.01f;
    private Vector3f fogColor = new Vector3f(0.7f, 0.8f, 0.9f);
    private float windStrength = 1.0f;
    private Vector3f windDirection = new Vector3f(1.0f, 0.0f, 0.0f);
    
    // Time periods
    public enum TimePeriod {
        DAWN(5.0f, 7.0f),
        MORNING(7.0f, 11.0f),
        NOON(11.0f, 13.0f),
        AFTERNOON(13.0f, 17.0f),
        DUSK(17.0f, 19.0f),
        EVENING(19.0f, 22.0f),
        NIGHT(22.0f, 5.0f);
        
        private final float startHour;
        private final float endHour;
        
        TimePeriod(float startHour, float endHour) {
            this.startHour = startHour;
            this.endHour = endHour;
        }
        
        public float getStartHour() { return startHour; }
        public float getEndHour() { return endHour; }
        
        public boolean contains(float hour) {
            if (this == NIGHT) {
                // Night wraps around midnight
                return hour >= startHour || hour < endHour;
            }
            return hour >= startHour && hour < endHour;
        }
    }
    
    public TimeOfDaySystem() {
        updateLighting();
    }
    
    /**
     * Update the time-of-day system
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(float deltaTime) {
        if (paused) return;
        
        // Update current time
        float timeIncrement = (deltaTime * timeScale) / SECONDS_PER_GAME_HOUR;
        currentTime += timeIncrement;
        
        // Wrap around 24 hours
        if (currentTime >= HOURS_PER_DAY) {
            currentTime -= HOURS_PER_DAY;
        } else if (currentTime < 0) {
            currentTime += HOURS_PER_DAY;
        }
        
        // Update lighting and environmental effects
        updateLighting();
        updateEnvironment();
    }
    
    /**
     * Update lighting based on current time
     */
    private void updateLighting() {
        // Calculate sun position
        float solarAngle = (float) ((currentTime - 12.0f) * Math.PI / 12.0f);
        float sunHeight = (float) Math.sin(solarAngle);
        float sunAzimuth = (float) Math.cos(solarAngle);
        
        // Update sun direction
        sunDirection.set(sunAzimuth * 0.7f, sunHeight, 0.3f);
        sunDirection.normalize();
        
        // Calculate sun intensity with atmospheric extinction
        if (sunHeight > 0.0f) {
            float airMass = 1.0f / Math.max(sunHeight, 0.01f);
            float extinction = (float) Math.exp(-0.1f * airMass);
            sunIntensity = extinction * sunHeight;
        } else {
            sunIntensity = 0.0f;
        }
        
        // Update sun/moon color based on time period
        TimePeriod period = getCurrentTimePeriod();
        switch (period) {
            case DAWN:
                // Warm sunrise colors
                float dawnProgress = getTimePeriodProgress();
                sunColor.set(
                    1.0f,
                    0.4f + 0.5f * dawnProgress,
                    0.2f + 0.6f * dawnProgress
                );
                break;
                
            case MORNING:
            case NOON:
            case AFTERNOON:
                // Bright daylight
                sunColor.set(1.0f, 0.95f, 0.8f);
                break;
                
            case DUSK:
                // Warm sunset colors
                float duskProgress = 1.0f - getTimePeriodProgress();
                sunColor.set(
                    1.0f,
                    0.4f + 0.5f * duskProgress,
                    0.2f + 0.6f * duskProgress
                );
                break;
                
            case EVENING:
            case NIGHT:
                // Cool moonlight
                sunColor.set(moonColor);
                break;
        }
        
        // Update ambient light based on time
        updateAmbientLight();
    }
    
    /**
     * Update ambient lighting based on time of day
     */
    private void updateAmbientLight() {
        TimePeriod period = getCurrentTimePeriod();
        float progress = getTimePeriodProgress();
        
        switch (period) {
            case DAWN:
                // Transition from night to day
                Vector3f nightAmbient = new Vector3f(0.1f, 0.1f, 0.2f);
                Vector3f dayAmbient = new Vector3f(0.3f, 0.3f, 0.4f);
                ambientLight.set(nightAmbient).lerp(dayAmbient, progress);
                break;
                
            case MORNING:
            case NOON:
            case AFTERNOON:
                // Bright daylight ambient
                ambientLight.set(0.3f, 0.3f, 0.4f);
                break;
                
            case DUSK:
                // Transition from day to night
                Vector3f dayAmb = new Vector3f(0.3f, 0.3f, 0.4f);
                Vector3f nightAmb = new Vector3f(0.1f, 0.1f, 0.2f);
                ambientLight.set(dayAmb).lerp(nightAmb, progress);
                break;
                
            case EVENING:
            case NIGHT:
                // Dark night ambient
                ambientLight.set(0.05f, 0.05f, 0.15f);
                break;
        }
    }
    
    /**
     * Update environmental effects based on time
     */
    private void updateEnvironment() {
        TimePeriod period = getCurrentTimePeriod();
        
        // Update fog based on time of day
        switch (period) {
            case DAWN:
                // Morning mist
                fogDensity = 0.02f + 0.01f * (1.0f - getTimePeriodProgress());
                fogColor.set(0.9f, 0.8f, 0.7f);
                break;
                
            case MORNING:
            case NOON:
            case AFTERNOON:
                // Clear day
                fogDensity = 0.005f;
                fogColor.set(0.7f, 0.8f, 0.9f);
                break;
                
            case DUSK:
                // Evening haze
                fogDensity = 0.015f;
                fogColor.set(0.8f, 0.6f, 0.5f);
                break;
                
            case EVENING:
            case NIGHT:
                // Night fog
                fogDensity = 0.01f;
                fogColor.set(0.2f, 0.2f, 0.4f);
                break;
        }
        
        // Update wind patterns (simplified)
        float windVariation = (float) Math.sin(currentTime * 0.1f) * 0.3f;
        windStrength = 1.0f + windVariation;
        
        // Rotate wind direction slowly over time
        float windAngle = currentTime * 0.05f;
        windDirection.set(
            (float) Math.cos(windAngle),
            0.0f,
            (float) Math.sin(windAngle)
        );
        windDirection.normalize();
    }
    
    /**
     * Get the current time period
     */
    public TimePeriod getCurrentTimePeriod() {
        for (TimePeriod period : TimePeriod.values()) {
            if (period.contains(currentTime)) {
                return period;
            }
        }
        return TimePeriod.DAY; // Fallback
    }
    
    /**
     * Get progress through current time period (0.0 to 1.0)
     */
    public float getTimePeriodProgress() {
        TimePeriod period = getCurrentTimePeriod();
        float duration;
        float elapsed;
        
        if (period == TimePeriod.NIGHT) {
            // Handle night period wrapping around midnight
            if (currentTime >= period.getStartHour()) {
                elapsed = currentTime - period.getStartHour();
            } else {
                elapsed = (24.0f - period.getStartHour()) + currentTime;
            }
            duration = (24.0f - period.getStartHour()) + period.getEndHour();
        } else {
            elapsed = currentTime - period.getStartHour();
            duration = period.getEndHour() - period.getStartHour();
        }
        
        return Math.max(0.0f, Math.min(1.0f, elapsed / duration));
    }
    
    /**
     * Check if it's currently daytime
     */
    public boolean isDaytime() {
        return sunIntensity > 0.1f;
    }
    
    /**
     * Check if it's currently nighttime
     */
    public boolean isNighttime() {
        return sunIntensity < 0.05f;
    }
    
    /**
     * Get formatted time string (HH:MM)
     */
    public String getFormattedTime() {
        int hours = (int) currentTime;
        int minutes = (int) ((currentTime - hours) * 60);
        return String.format("%02d:%02d", hours, minutes);
    }
    
    // Getters and setters
    
    public float getCurrentTime() { return currentTime; }
    public void setCurrentTime(float time) { 
        this.currentTime = time % 24.0f;
        if (this.currentTime < 0) this.currentTime += 24.0f;
        updateLighting();
    }
    
    public float getTimeScale() { return timeScale; }
    public void setTimeScale(float scale) { this.timeScale = Math.max(0.0f, scale); }
    
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    
    public Vector3f getAmbientLight() { return new Vector3f(ambientLight); }
    public Vector3f getSunColor() { return new Vector3f(sunColor); }
    public Vector3f getSunDirection() { return new Vector3f(sunDirection); }
    public float getSunIntensity() { return sunIntensity; }
    
    public float getFogDensity() { return fogDensity; }
    public Vector3f getFogColor() { return new Vector3f(fogColor); }
    
    public float getWindStrength() { return windStrength; }
    public Vector3f getWindDirection() { return new Vector3f(windDirection); }
    
    public float getTurbidity() { return turbidity; }
    public void setTurbidity(float turbidity) { this.turbidity = Math.max(1.0f, Math.min(10.0f, turbidity)); }
    
    public float getAlbedo() { return groundAlbedo; }
    public void setAlbedo(float albedo) { this.groundAlbedo = Math.max(0.0f, Math.min(1.0f, albedo)); }
    
    /**
     * Fast forward time to a specific hour
     */
    public void setTimeOfDay(float hour) {
        setCurrentTime(hour);
    }
    
    /**
     * Advance time by a specific number of hours
     */
    public void advanceTime(float hours) {
        setCurrentTime(currentTime + hours);
    }
}