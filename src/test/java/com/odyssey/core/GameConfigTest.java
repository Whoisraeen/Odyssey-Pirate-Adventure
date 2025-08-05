package com.odyssey.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GameConfig.
 */
public class GameConfigTest {
    
    @Test
    public void testDefaultConfiguration() {
        GameConfig config = new GameConfig();
        
        // Test default values
        assertEquals(1920, config.getWindowWidth());
        assertEquals(1080, config.getWindowHeight());
        assertFalse(config.isFullscreen());
        assertTrue(config.isVsyncEnabled());
        assertEquals(60, config.getTargetFPS());
        
        // Test audio defaults
        assertEquals(1.0f, config.getMasterVolume());
        assertEquals(0.8f, config.getMusicVolume());
        assertEquals(1.0f, config.getEffectsVolume());
        assertEquals(0.6f, config.getAmbientVolume());
        
        // Test ocean simulation defaults
        assertTrue(config.isDynamicTidesEnabled());
        assertTrue(config.isDynamicWeatherEnabled());
        assertTrue(config.isDynamicWavesEnabled());
        assertEquals(1.0f, config.getOceanSimulationQuality());
    }
    
    @Test
    public void testConfigurationModification() {
        GameConfig config = new GameConfig();
        
        // Test setters
        config.setWindowWidth(1280);
        config.setWindowHeight(720);
        config.setFullscreen(true);
        config.setDebugMode(true);
        
        assertEquals(1280, config.getWindowWidth());
        assertEquals(720, config.getWindowHeight());
        assertTrue(config.isFullscreen());
        assertTrue(config.isDebugMode());
    }
}