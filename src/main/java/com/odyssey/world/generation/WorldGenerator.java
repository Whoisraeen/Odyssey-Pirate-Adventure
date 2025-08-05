package com.odyssey.world.generation;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * World generation system for The Odyssey.
 */
public class WorldGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WorldGenerator.class);
    
    private final GameConfig config;
    
    public WorldGenerator(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing world generator with seed: {}", config.getWorldSeed());
    }
    
    public void cleanup() {
        logger.info("Cleaning up world generator");
    }
}