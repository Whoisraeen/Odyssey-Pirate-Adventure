package com.odyssey.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the day/night cycle, time progression, and celestial events.
 * Provides time-based lighting, weather transitions, and gameplay mechanics.
 */
public class DayNightCycle {
    private static final Logger logger = LoggerFactory.getLogger(DayNightCycle.class);
    
    // Time constants (in ticks, 20 ticks per second)
    public static final int TICKS_PER_SECOND = 20;
    public static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;
    public static final int TICKS_PER_HOUR = TICKS_PER_MINUTE * 60;
    public static final int TICKS_PER_DAY = 24000; // Minecraft-style 20-minute day
    
    // Time periods
    public static final int SUNRISE_START = 23000;
    public static final int SUNRISE_END = 1000;
    public static final int DAY_START = 1000;
    public static final int DAY_END = 13000;
    public static final int SUNSET_START = 13000;
    public static final int SUNSET_END = 15000;
    public static final int NIGHT_START = 15000;
    public static final int NIGHT_END = 23000;
    
    private final World world;
    private long worldTime = 0; // Total ticks since world creation
    private long dayTime = 6000; // Time of day (0-23999)
    private int dayCount = 0;
    private boolean doDaylightCycle = true;
    private float timeSpeed = 1.0f;
    
    // Celestial data
    private float sunAngle = 0.0f;
    private float moonAngle = 0.0f;
    private int moonPhase = 0; // 0-7, full cycle
    private float skyBrightness = 1.0f;
    private float starBrightness = 0.0f;
    
    // Weather integration
    private boolean isRaining = false;
    private boolean isThundering = false;
    private float rainStrength = 0.0f;
    private float thunderStrength = 0.0f;
    
    public DayNightCycle(World world) {
        this.world = world;
        updateCelestialData();
        logger.info("Day/Night cycle initialized for world");
    }
    
    /**
     * Updates the day/night cycle
     */
    public void tick() {
        if (doDaylightCycle) {
            // Advance time
            worldTime++;
            dayTime = (long) (dayTime + timeSpeed) % TICKS_PER_DAY;
            
            // Check for new day
            if (dayTime == 0) {
                dayCount++;
                onNewDay();
            }
            
            // Update celestial data
            updateCelestialData();
            
            // Update lighting
            updateWorldLighting();
            
            // Handle time-based events
            handleTimeEvents();
        }
    }
    
    /**
     * Updates celestial positions and brightness values
     */
    private void updateCelestialData() {
        // Calculate sun angle (0 = noon, PI = midnight)
        sunAngle = (float) (dayTime / (double) TICKS_PER_DAY * 2 * Math.PI);
        
        // Moon is opposite to sun
        moonAngle = sunAngle + (float) Math.PI;
        if (moonAngle > 2 * Math.PI) {
            moonAngle -= 2 * Math.PI;
        }
        
        // Calculate sky brightness based on sun position
        float sunHeight = (float) Math.sin(sunAngle);
        
        if (sunHeight > 0) {
            // Daytime
            skyBrightness = Math.min(1.0f, sunHeight + 0.1f);
            starBrightness = 0.0f;
        } else {
            // Nighttime
            skyBrightness = Math.max(0.1f, sunHeight + 0.3f);
            starBrightness = Math.min(1.0f, -sunHeight + 0.2f);
        }
        
        // Adjust for weather
        if (isRaining) {
            skyBrightness *= (1.0f - rainStrength * 0.5f);
        }
        if (isThundering) {
            skyBrightness *= (1.0f - thunderStrength * 0.3f);
        }
        
        // Update moon phase (changes every 8 days)
        moonPhase = (dayCount % 8);
    }
    
    /**
     * Updates world lighting based on time of day
     */
    private void updateWorldLighting() {
        int lightLevel = calculateSkyLightLevel();
        
        // Update sky light in all loaded chunks
        // This would integrate with the lighting system
        world.setSkyLightLevel(lightLevel);
        
        // Schedule lighting updates for loaded chunks
        world.getChunkManager().scheduleAllLightingUpdates();
    }
    
    /**
     * Calculates the sky light level (0-15)
     */
    public int calculateSkyLightLevel() {
        if (dayTime >= DAY_START && dayTime < DAY_END) {
            // Full daylight
            return 15;
        } else if (dayTime >= NIGHT_START || dayTime < SUNRISE_START) {
            // Full night
            return 4;
        } else if (dayTime >= SUNRISE_START || dayTime < SUNRISE_END) {
            // Sunrise transition
            float progress = calculateTransitionProgress(SUNRISE_START, SUNRISE_END, dayTime);
            return (int) (4 + (11 * progress));
        } else if (dayTime >= SUNSET_START && dayTime < SUNSET_END) {
            // Sunset transition
            float progress = calculateTransitionProgress(SUNSET_START, SUNSET_END, dayTime);
            return (int) (15 - (11 * progress));
        }
        
        return 15; // Default to full light
    }
    
    /**
     * Calculates transition progress between two time points
     */
    private float calculateTransitionProgress(int startTime, int endTime, long currentTime) {
        if (startTime > endTime) {
            // Handle wrap-around (e.g., 23000 to 1000)
            if (currentTime >= startTime) {
                return (currentTime - startTime) / (float) (TICKS_PER_DAY - startTime + endTime);
            } else {
                return (TICKS_PER_DAY - startTime + currentTime) / (float) (TICKS_PER_DAY - startTime + endTime);
            }
        } else {
            return (currentTime - startTime) / (float) (endTime - startTime);
        }
    }
    
    /**
     * Handles time-based events
     */
    private void handleTimeEvents() {
        // Spawn hostile mobs at night
        if (isNight() && !isRaining) {
            // This would integrate with the mob spawning system
            world.getMobSpawner().setNightSpawning(true);
        } else {
            world.getMobSpawner().setNightSpawning(false);
        }
        
        // Handle sleep mechanics
        if (isNight()) {
            // Players can sleep
            world.getPlayerManager().enableSleeping();
        } else {
            world.getPlayerManager().disableSleeping();
        }
        
        // Update passive mob behavior
        updateMobBehavior();
        
        // Handle crop growth acceleration during certain times
        if (isDawn() || isDusk()) {
            world.getRandomTickScheduler().setRandomTickSpeed(4); // Faster growth
        } else {
            world.getRandomTickScheduler().setRandomTickSpeed(3); // Normal speed
        }
    }
    
    /**
     * Updates mob behavior based on time of day
     */
    private void updateMobBehavior() {
        if (isDay()) {
            // Hostile mobs burn in sunlight
            world.getEntityManager().burnUndeadInSunlight();
            // Passive mobs are more active
            world.getEntityManager().setMobActivityLevel(1.2f);
        } else if (isNight()) {
            // Hostile mobs are more aggressive
            world.getEntityManager().setMobActivityLevel(1.5f);
        } else {
            // Normal activity during transitions
            world.getEntityManager().setMobActivityLevel(1.0f);
        }
    }
    
    /**
     * Called when a new day begins
     */
    private void onNewDay() {
        logger.info("New day started: Day {}", dayCount + 1);
        
        // Reset daily counters
        world.getStatistics().resetDailyStats();
        
        // Handle weather changes
        world.getWeatherSystem().onNewDay();
        
        // Update moon phase
        if (dayCount % 8 == 0) {
            logger.debug("New moon phase: {}", moonPhase);
        }
        
        // Trigger new day events
        world.getEventBus().post(new NewDayEvent(dayCount));
    }
    
    /**
     * Skips to a specific time of day
     */
    public void setTimeOfDay(long time) {
        long oldDayTime = dayTime;
        dayTime = time % TICKS_PER_DAY;
        worldTime += (dayTime - oldDayTime);
        
        updateCelestialData();
        updateWorldLighting();
        
        logger.info("Time set to {} ({})", dayTime, getTimeString());
    }
    
    /**
     * Adds time to the current day
     */
    public void addTime(long ticks) {
        worldTime += ticks;
        dayTime = (dayTime + ticks) % TICKS_PER_DAY;
        
        updateCelestialData();
        updateWorldLighting();
    }
    
    /**
     * Skips to the next day
     */
    public void skipToNextDay() {
        long ticksToAdd = TICKS_PER_DAY - dayTime;
        addTime(ticksToAdd);
        dayCount++;
        onNewDay();
    }
    
    /**
     * Skips to dawn
     */
    public void skipToDawn() {
        setTimeOfDay(SUNRISE_START);
    }
    
    /**
     * Skips to noon
     */
    public void skipToNoon() {
        setTimeOfDay(6000);
    }
    
    /**
     * Skips to dusk
     */
    public void skipToDusk() {
        setTimeOfDay(SUNSET_START);
    }
    
    /**
     * Skips to midnight
     */
    public void skipToMidnight() {
        setTimeOfDay(18000);
    }
    
    // Time period checks
    public boolean isDay() {
        return dayTime >= DAY_START && dayTime < DAY_END;
    }
    
    public boolean isNight() {
        return dayTime >= NIGHT_START || dayTime < SUNRISE_START;
    }
    
    public boolean isDawn() {
        return (dayTime >= SUNRISE_START && dayTime < SUNRISE_END) || 
               (SUNRISE_START > SUNRISE_END && (dayTime >= SUNRISE_START || dayTime < SUNRISE_END));
    }
    
    public boolean isDusk() {
        return dayTime >= SUNSET_START && dayTime < SUNSET_END;
    }
    
    public boolean isSunrise() {
        return isDawn();
    }
    
    public boolean isSunset() {
        return isDusk();
    }
    
    // Getters and setters
    public long getWorldTime() {
        return worldTime;
    }
    
    public void setWorldTime(long worldTime) {
        this.worldTime = worldTime;
        this.dayTime = worldTime % TICKS_PER_DAY;
        this.dayCount = (int) (worldTime / TICKS_PER_DAY);
        updateCelestialData();
    }
    
    public long getDayTime() {
        return dayTime;
    }
    
    public int getDayCount() {
        return dayCount;
    }
    
    public boolean isDaylightCycleEnabled() {
        return doDaylightCycle;
    }
    
    public void setDaylightCycleEnabled(boolean enabled) {
        this.doDaylightCycle = enabled;
        logger.info("Daylight cycle {}", enabled ? "enabled" : "disabled");
    }
    
    public float getTimeSpeed() {
        return timeSpeed;
    }
    
    public void setTimeSpeed(float speed) {
        this.timeSpeed = Math.max(0.0f, speed);
        logger.info("Time speed set to {}x", this.timeSpeed);
    }
    
    public float getSunAngle() {
        return sunAngle;
    }
    
    public float getMoonAngle() {
        return moonAngle;
    }
    
    public int getMoonPhase() {
        return moonPhase;
    }
    
    public float getSkyBrightness() {
        return skyBrightness;
    }
    
    public float getStarBrightness() {
        return starBrightness;
    }
    
    public void setWeatherData(boolean raining, boolean thundering, float rainStrength, float thunderStrength) {
        this.isRaining = raining;
        this.isThundering = thundering;
        this.rainStrength = rainStrength;
        this.thunderStrength = thunderStrength;
        updateCelestialData(); // Recalculate brightness
    }
    
    /**
     * Gets a human-readable time string
     */
    public String getTimeString() {
        int hours = (int) ((dayTime + 6000) / 1000) % 24;
        int minutes = (int) (((dayTime + 6000) % 1000) * 60 / 1000);
        return String.format("%02d:%02d", hours, minutes);
    }
    
    /**
     * Gets the current time period as a string
     */
    public String getTimePeriod() {
        if (isDay()) {
            return "Day";
        } else if (isNight()) {
            return "Night";
        } else if (isDawn()) {
            return "Dawn";
        } else if (isDusk()) {
            return "Dusk";
        }
        return "Unknown";
    }
    
    /**
     * Gets moon phase name
     */
    public String getMoonPhaseName() {
        switch (moonPhase) {
            case 0: return "Full Moon";
            case 1: return "Waning Gibbous";
            case 2: return "Third Quarter";
            case 3: return "Waning Crescent";
            case 4: return "New Moon";
            case 5: return "Waxing Crescent";
            case 6: return "First Quarter";
            case 7: return "Waxing Gibbous";
            default: return "Unknown";
        }
    }
    
    /**
     * Event class for new day notifications
     */
    public static class NewDayEvent {
        private final int dayNumber;
        
        public NewDayEvent(int dayNumber) {
            this.dayNumber = dayNumber;
        }
        
        public int getDayNumber() {
            return dayNumber;
        }
    }
}