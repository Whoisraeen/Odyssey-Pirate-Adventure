package com.odyssey.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration management for The Odyssey game.
 * 
 * This class handles loading and managing all game configuration settings,
 * including graphics, audio, gameplay, and system preferences. It supports
 * both default configurations and user-customized settings.
 */
public class GameConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GameConfig.class);
    
    // Configuration file paths
    private static final String DEFAULT_CONFIG_PATH = "/config/default.properties";
    private static final String USER_CONFIG_PATH = "config/user.properties";
    
    // Singleton instance
    private static GameConfig instance;
    
    // Configuration properties
    private final Properties properties;
    
    // Display settings
    private int windowWidth;
    private int windowHeight;
    private boolean fullscreen;
    private boolean vsync;
    private int targetFPS;
    private float renderScale;
    
    // Graphics settings
    private int maxChunkRenderDistance;
    private int maxEntityRenderDistance;
    private boolean enableShadows;
    private boolean enableReflections;
    private boolean enableParticles;
    private int shadowMapResolution;
    private float fogDensity;
    
    // Ocean settings
    private int oceanChunkSize;
    private int oceanSimulationDetail;
    private boolean enableWavePhysics;
    private float waveHeight;
    private float waveSpeed;
    
    // Performance settings
    private int maxThreads;
    private boolean enableMultithreading;
    private int memoryPoolSize;
    private boolean enableProfiling;
    
    // Audio settings
    private float masterVolume;
    private float musicVolume;
    private float effectsVolume;
    private float ambientVolume;
    
    // Gameplay settings
    private float mouseSensitivity;
    private boolean invertMouseY;
    private float controllerSensitivity;
    private boolean invertControllerY;
    private float controllerDeadzone;
    private float gameSpeed;
    private boolean enableAutosave;
    private int autosaveInterval;
    
    // Debug settings
    private boolean debugMode;
    private boolean showFPS;
    private boolean showChunkBorders;
    private boolean wireframeMode;
    
    /**
     * Private constructor for singleton pattern.
     */
    private GameConfig() {
        this.properties = new Properties();
        loadConfiguration();
    }
    
    /**
     * Get the singleton instance of GameConfig.
     * 
     * @return The GameConfig instance
     */
    public static synchronized GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
        }
        return instance;
    }
    
    /**
     * Load configuration from files.
     */
    private void loadConfiguration() {
        // Load default configuration from resources
        try (InputStream defaultStream = getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (defaultStream != null) {
                properties.load(defaultStream);
                logger.info("Loaded default configuration");
            } else {
                logger.warn("Default configuration file not found, using hardcoded defaults");
                setHardcodedDefaults();
            }
        } catch (IOException e) {
            logger.error("Failed to load default configuration", e);
            setHardcodedDefaults();
        }
        
        // Load user configuration if it exists
        Path userConfigPath = Paths.get(USER_CONFIG_PATH);
        if (Files.exists(userConfigPath)) {
            try (InputStream userStream = Files.newInputStream(userConfigPath)) {
                properties.load(userStream);
                logger.info("Loaded user configuration from {}", userConfigPath);
            } catch (IOException e) {
                logger.error("Failed to load user configuration", e);
            }
        }
        
        // Parse and validate configuration values
        parseConfiguration();
    }
    
    /**
     * Set hardcoded default values when configuration files are not available.
     */
    private void setHardcodedDefaults() {
        // Display defaults
        properties.setProperty("display.width", "1920");
        properties.setProperty("display.height", "1080");
        properties.setProperty("display.fullscreen", "false");
        properties.setProperty("display.vsync", "true");
        properties.setProperty("display.targetFPS", "60");
        properties.setProperty("display.renderScale", "1.0");
        
        // Graphics defaults
        properties.setProperty("graphics.chunkRenderDistance", "16");
        properties.setProperty("graphics.entityRenderDistance", "128");
        properties.setProperty("graphics.enableShadows", "true");
        properties.setProperty("graphics.enableReflections", "true");
        properties.setProperty("graphics.enableParticles", "true");
        properties.setProperty("graphics.shadowMapResolution", "2048");
        properties.setProperty("graphics.fogDensity", "0.01");
        
        // Ocean defaults
        properties.setProperty("ocean.chunkSize", "64");
        properties.setProperty("ocean.simulationDetail", "2");
        properties.setProperty("ocean.enableWavePhysics", "true");
        properties.setProperty("ocean.waveHeight", "2.0");
        properties.setProperty("ocean.waveSpeed", "1.0");
        
        // Performance defaults
        properties.setProperty("performance.maxThreads", "0"); // 0 = auto-detect
        properties.setProperty("performance.enableMultithreading", "true");
        properties.setProperty("performance.memoryPoolSize", "256");
        properties.setProperty("performance.enableProfiling", "false");
        
        // Audio defaults
        properties.setProperty("audio.masterVolume", "1.0");
        properties.setProperty("audio.musicVolume", "0.8");
        properties.setProperty("audio.effectsVolume", "1.0");
        properties.setProperty("audio.ambientVolume", "0.6");
        
        // Gameplay defaults
        properties.setProperty("gameplay.mouseSensitivity", "1.0");
        properties.setProperty("gameplay.invertMouseY", "false");
        properties.setProperty("gameplay.controllerSensitivity", "0.8");
        properties.setProperty("gameplay.invertControllerY", "false");
        properties.setProperty("gameplay.controllerDeadzone", "0.15");
        properties.setProperty("gameplay.gameSpeed", "1.0");
        properties.setProperty("gameplay.enableAutosave", "true");
        properties.setProperty("gameplay.autosaveInterval", "300"); // 5 minutes
        
        // Debug defaults
        properties.setProperty("debug.debugMode", "false");
        properties.setProperty("debug.showFPS", "false");
        properties.setProperty("debug.showChunkBorders", "false");
        properties.setProperty("debug.wireframeMode", "false");
    }
    
    /**
     * Parse configuration properties into typed fields.
     */
    private void parseConfiguration() {
        // Display settings
        windowWidth = getIntProperty("display.width", 1920);
        windowHeight = getIntProperty("display.height", 1080);
        fullscreen = getBooleanProperty("display.fullscreen", false);
        vsync = getBooleanProperty("display.vsync", true);
        targetFPS = getIntProperty("display.targetFPS", 60);
        renderScale = getFloatProperty("display.renderScale", 1.0f);
        
        // Graphics settings
        maxChunkRenderDistance = getIntProperty("graphics.chunkRenderDistance", 16);
        maxEntityRenderDistance = getIntProperty("graphics.entityRenderDistance", 128);
        enableShadows = getBooleanProperty("graphics.enableShadows", true);
        enableReflections = getBooleanProperty("graphics.enableReflections", true);
        enableParticles = getBooleanProperty("graphics.enableParticles", true);
        shadowMapResolution = getIntProperty("graphics.shadowMapResolution", 2048);
        fogDensity = getFloatProperty("graphics.fogDensity", 0.01f);
        
        // Ocean settings
        oceanChunkSize = getIntProperty("ocean.chunkSize", 64);
        oceanSimulationDetail = getIntProperty("ocean.simulationDetail", 2);
        enableWavePhysics = getBooleanProperty("ocean.enableWavePhysics", true);
        waveHeight = getFloatProperty("ocean.waveHeight", 2.0f);
        waveSpeed = getFloatProperty("ocean.waveSpeed", 1.0f);
        
        // Performance settings
        maxThreads = getIntProperty("performance.maxThreads", 0);
        if (maxThreads <= 0) {
            maxThreads = Runtime.getRuntime().availableProcessors();
        }
        enableMultithreading = getBooleanProperty("performance.enableMultithreading", true);
        memoryPoolSize = getIntProperty("performance.memoryPoolSize", 256);
        enableProfiling = getBooleanProperty("performance.enableProfiling", false);
        
        // Audio settings
        masterVolume = getFloatProperty("audio.masterVolume", 1.0f);
        musicVolume = getFloatProperty("audio.musicVolume", 0.8f);
        effectsVolume = getFloatProperty("audio.effectsVolume", 1.0f);
        ambientVolume = getFloatProperty("audio.ambientVolume", 0.6f);
        
        // Gameplay settings
        mouseSensitivity = getFloatProperty("gameplay.mouseSensitivity", 1.0f);
        invertMouseY = getBooleanProperty("gameplay.invertMouseY", false);
        controllerSensitivity = getFloatProperty("gameplay.controllerSensitivity", 0.8f);
        invertControllerY = getBooleanProperty("gameplay.invertControllerY", false);
        controllerDeadzone = getFloatProperty("gameplay.controllerDeadzone", 0.15f);
        gameSpeed = getFloatProperty("gameplay.gameSpeed", 1.0f);
        enableAutosave = getBooleanProperty("gameplay.enableAutosave", true);
        autosaveInterval = getIntProperty("gameplay.autosaveInterval", 300);
        
        // Debug settings
        debugMode = getBooleanProperty("debug.debugMode", false);
        showFPS = getBooleanProperty("debug.showFPS", false);
        showChunkBorders = getBooleanProperty("debug.showChunkBorders", false);
        wireframeMode = getBooleanProperty("debug.wireframeMode", false);
        
        logger.info("Configuration loaded successfully");
    }
    
    /**
     * Get an integer property with a default value.
     */
    private int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get a float property with a default value.
     */
    private float getFloatProperty(String key, float defaultValue) {
        try {
            return Float.parseFloat(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid float value for property {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * Get a boolean property with a default value.
     */
    private boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
    
    // Getters for all configuration values
    
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public boolean isFullscreen() { return fullscreen; }
    public boolean isVsync() { return vsync; }
    public int getTargetFPS() { return targetFPS; }
    public float getRenderScale() { return renderScale; }
    
    public int getMaxChunkRenderDistance() { return maxChunkRenderDistance; }
    public int getMaxEntityRenderDistance() { return maxEntityRenderDistance; }
    public boolean isEnableShadows() { return enableShadows; }
    public boolean isEnableReflections() { return enableReflections; }
    public boolean isEnableParticles() { return enableParticles; }
    public int getShadowMapResolution() { return shadowMapResolution; }
    public float getFogDensity() { return fogDensity; }
    
    public int getOceanChunkSize() { return oceanChunkSize; }
    public int getOceanSimulationDetail() { return oceanSimulationDetail; }
    public boolean isEnableWavePhysics() { return enableWavePhysics; }
    public float getWaveHeight() { return waveHeight; }
    public float getWaveSpeed() { return waveSpeed; }
    
    public int getMaxThreads() { return maxThreads; }
    public boolean isEnableMultithreading() { return enableMultithreading; }
    public int getMemoryPoolSize() { return memoryPoolSize; }
    public boolean isEnableProfiling() { return enableProfiling; }
    
    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    public float getEffectsVolume() { return effectsVolume; }
    public float getAmbientVolume() { return ambientVolume; }
    
    public float getMouseSensitivity() { return mouseSensitivity; }
    public boolean isInvertMouseY() { return invertMouseY; }
    public float getControllerSensitivity() { return controllerSensitivity; }
    public boolean isInvertControllerY() { return invertControllerY; }
    public float getControllerDeadzone() { return controllerDeadzone; }
    public float getGameSpeed() { return gameSpeed; }
    public boolean isEnableAutosave() { return enableAutosave; }
    public int getAutosaveInterval() { return autosaveInterval; }
    
    public boolean isDebugMode() { return debugMode; }
    public boolean isShowFPS() { return showFPS; }
    public boolean isShowChunkBorders() { return showChunkBorders; }
    public boolean isWireframeMode() { return wireframeMode; }
    
    // Generic configuration access methods
    public float getFloat(String key) {
        return getFloatProperty(key, 0.0f);
    }
    
    public float getFloat(String key, float defaultValue) {
        return getFloatProperty(key, defaultValue);
    }

    public String getString(String key) {
        return properties.getProperty(key, "");
    }
    
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key) {
        return getIntProperty(key, 0);
    }
    
    public int getInt(String key, int defaultValue) {
        return getIntProperty(key, defaultValue);
    }
    
    public boolean getBoolean(String key) {
        return getBooleanProperty(key, false);
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBooleanProperty(key, defaultValue);
    }
    
    public boolean isMultiplayerEnabled() {
        return getBooleanProperty("multiplayer.enabled", false);
    }
    
    /**
     * Save current configuration to user config file.
     */
    public void saveConfiguration() {
        try {
            Path userConfigPath = Paths.get(USER_CONFIG_PATH);
            Files.createDirectories(userConfigPath.getParent());
            
            // Update properties with current values
            updatePropertiesFromFields();
            
            try (var outputStream = Files.newOutputStream(userConfigPath)) {
                properties.store(outputStream, "The Odyssey User Configuration");
                logger.info("Configuration saved to {}", userConfigPath);
            }
        } catch (IOException e) {
            logger.error("Failed to save configuration", e);
        }
    }
    
    /**
     * Update properties object with current field values.
     */
    private void updatePropertiesFromFields() {
        properties.setProperty("display.width", String.valueOf(windowWidth));
        properties.setProperty("display.height", String.valueOf(windowHeight));
        properties.setProperty("display.fullscreen", String.valueOf(fullscreen));
        properties.setProperty("display.vsync", String.valueOf(vsync));
        properties.setProperty("display.targetFPS", String.valueOf(targetFPS));
        properties.setProperty("display.renderScale", String.valueOf(renderScale));
        
        properties.setProperty("graphics.chunkRenderDistance", String.valueOf(maxChunkRenderDistance));
        properties.setProperty("graphics.entityRenderDistance", String.valueOf(maxEntityRenderDistance));
        properties.setProperty("graphics.enableShadows", String.valueOf(enableShadows));
        properties.setProperty("graphics.enableReflections", String.valueOf(enableReflections));
        properties.setProperty("graphics.enableParticles", String.valueOf(enableParticles));
        properties.setProperty("graphics.shadowMapResolution", String.valueOf(shadowMapResolution));
        properties.setProperty("graphics.fogDensity", String.valueOf(fogDensity));
        
        properties.setProperty("ocean.chunkSize", String.valueOf(oceanChunkSize));
        properties.setProperty("ocean.simulationDetail", String.valueOf(oceanSimulationDetail));
        properties.setProperty("ocean.enableWavePhysics", String.valueOf(enableWavePhysics));
        properties.setProperty("ocean.waveHeight", String.valueOf(waveHeight));
        properties.setProperty("ocean.waveSpeed", String.valueOf(waveSpeed));
        
        properties.setProperty("performance.maxThreads", String.valueOf(maxThreads));
        properties.setProperty("performance.enableMultithreading", String.valueOf(enableMultithreading));
        properties.setProperty("performance.memoryPoolSize", String.valueOf(memoryPoolSize));
        properties.setProperty("performance.enableProfiling", String.valueOf(enableProfiling));
        
        properties.setProperty("audio.masterVolume", String.valueOf(masterVolume));
        properties.setProperty("audio.musicVolume", String.valueOf(musicVolume));
        properties.setProperty("audio.effectsVolume", String.valueOf(effectsVolume));
        properties.setProperty("audio.ambientVolume", String.valueOf(ambientVolume));
        
        properties.setProperty("gameplay.mouseSensitivity", String.valueOf(mouseSensitivity));
        properties.setProperty("gameplay.invertMouseY", String.valueOf(invertMouseY));
        properties.setProperty("gameplay.controllerSensitivity", String.valueOf(controllerSensitivity));
        properties.setProperty("gameplay.invertControllerY", String.valueOf(invertControllerY));
        properties.setProperty("gameplay.controllerDeadzone", String.valueOf(controllerDeadzone));
        properties.setProperty("gameplay.gameSpeed", String.valueOf(gameSpeed));
        properties.setProperty("gameplay.enableAutosave", String.valueOf(enableAutosave));
        properties.setProperty("gameplay.autosaveInterval", String.valueOf(autosaveInterval));
        
        properties.setProperty("debug.debugMode", String.valueOf(debugMode));
        properties.setProperty("debug.showFPS", String.valueOf(showFPS));
        properties.setProperty("debug.showChunkBorders", String.valueOf(showChunkBorders));
        properties.setProperty("debug.wireframeMode", String.valueOf(wireframeMode));
    }
}