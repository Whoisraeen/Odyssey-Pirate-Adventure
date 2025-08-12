package com.odyssey.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder statistics system.
 * TODO: Implement proper game statistics tracking.
 */
public class Statistics {
    private static final Logger logger = LoggerFactory.getLogger(Statistics.class);
    
    private long totalGameTime = 0;
    private long dayCount = 0;
    
    public Statistics() {
        logger.debug("Statistics placeholder initialized");
    }
    
    /**
     * Updates statistics.
     * 
     * @param deltaTime time since last update
     */
    public void update(double deltaTime) {
        totalGameTime += (long)(deltaTime * 1000); // Convert to milliseconds
    }
    
    /**
     * Records a new day.
     */
    public void recordNewDay() {
        dayCount++;
        logger.debug("New day recorded. Total days: {}", dayCount);
    }
    
    /**
     * Gets the total game time in milliseconds.
     * 
     * @return total game time
     */
    public long getTotalGameTime() {
        return totalGameTime;
    }
    
    /**
     * Gets the total number of days.
     * 
     * @return day count
     */
    public long getDayCount() {
        return dayCount;
    }
}