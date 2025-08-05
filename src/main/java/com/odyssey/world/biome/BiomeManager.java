package com.odyssey.world.biome;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Biome management system for The Odyssey.
 */
public class BiomeManager {
    private static final Logger logger = LoggerFactory.getLogger(BiomeManager.class);
    
    private final GameConfig config;
    
    public BiomeManager(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing biome manager");
    }
    
    public void cleanup() {
        logger.info("Cleaning up biome manager");
    }
}