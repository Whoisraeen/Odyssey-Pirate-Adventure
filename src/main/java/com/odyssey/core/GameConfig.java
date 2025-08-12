package com.odyssey.core;

/**
 * Configuration class for The Odyssey game settings.
 * Contains all configurable parameters for the game engine.
 */
public class GameConfig {
    // Display settings
    private int windowWidth = 1920;
    private int windowHeight = 1080;
    private boolean fullscreen = false;
    private boolean vsync = true;
    private int targetFPS = 60;
    
    // Game settings
    private boolean debugMode = false;
    private long worldSeed = System.currentTimeMillis();
    private float renderDistance = 16.0f; // In chunks
    private int maxChunkUpdatesPerFrame = 4;
    
    // Audio settings
    private float masterVolume = 1.0f;
    private float musicVolume = 0.8f;
    private float effectsVolume = 1.0f;
    private float ambientVolume = 0.6f;
    
    // Input settings
    private float mouseSensitivity = 0.008f; // Default mouse sensitivity for raw mouse input feel
    
    // Ocean simulation settings
    private boolean enableTidalSystem = true;
    private boolean enableWeatherSystem = true;
    private boolean enableDynamicWaves = true;
    private float oceanSimulationQuality = 1.0f; // 0.5 = low, 1.0 = normal, 2.0 = high
    
    // Performance settings
    private boolean enableMultithreading = true;
    private int workerThreads = Runtime.getRuntime().availableProcessors() - 1;
    private boolean enableFrustumCulling = true;
    private boolean enableOcclusionCulling = false; // Advanced feature
    
    // UI settings
    private boolean enableUIRendering = false; // Disabled to prevent crashes
    
    // Job System settings
    private int jobSystemWorkerCount = 0; // 0 = auto-detect
    private int jobQueueCapacity = 1024;
    private int globalJobQueueCapacity = 2048;
    private boolean jobSystemStatisticsEnabled = false;
    
    // Getters and Setters
    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }
    
    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }
    
    public boolean isFullscreen() { return fullscreen; }
    public void setFullscreen(boolean fullscreen) { this.fullscreen = fullscreen; }
    
    public boolean isVsync() { return vsync; }
    public void setVsync(boolean vsync) { this.vsync = vsync; }
    
    public int getTargetFPS() { return targetFPS; }
    public void setTargetFPS(int targetFPS) { this.targetFPS = targetFPS; }
    
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    
    public long getWorldSeed() { return worldSeed; }
    public void setWorldSeed(long worldSeed) { this.worldSeed = worldSeed; }
    
    public float getRenderDistance() { return renderDistance; }
    public void setRenderDistance(float renderDistance) { this.renderDistance = renderDistance; }
    
    public int getMaxChunkUpdatesPerFrame() { return maxChunkUpdatesPerFrame; }
    public void setMaxChunkUpdatesPerFrame(int maxChunkUpdatesPerFrame) { 
        this.maxChunkUpdatesPerFrame = maxChunkUpdatesPerFrame; 
    }
    
    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
    
    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float musicVolume) { this.musicVolume = musicVolume; }
    
    public float getEffectsVolume() { return effectsVolume; }
    public void setEffectsVolume(float effectsVolume) { this.effectsVolume = effectsVolume; }
    
    public float getAmbientVolume() { return ambientVolume; }
    public void setAmbientVolume(float ambientVolume) { this.ambientVolume = ambientVolume; }
    
    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float mouseSensitivity) { 
        this.mouseSensitivity = Math.max(0.001f, Math.min(0.02f, mouseSensitivity)); 
    }
    
    public boolean isEnableTidalSystem() { return enableTidalSystem; }
    public void setEnableTidalSystem(boolean enableTidalSystem) { this.enableTidalSystem = enableTidalSystem; }
    
    public boolean isEnableWeatherSystem() { return enableWeatherSystem; }
    public void setEnableWeatherSystem(boolean enableWeatherSystem) { this.enableWeatherSystem = enableWeatherSystem; }
    
    public boolean isEnableDynamicWaves() { return enableDynamicWaves; }
    public void setEnableDynamicWaves(boolean enableDynamicWaves) { this.enableDynamicWaves = enableDynamicWaves; }
    
    public float getOceanSimulationQuality() { return oceanSimulationQuality; }
    public void setOceanSimulationQuality(float oceanSimulationQuality) { 
        this.oceanSimulationQuality = oceanSimulationQuality; 
    }
    
    public boolean isEnableMultithreading() { return enableMultithreading; }
    public void setEnableMultithreading(boolean enableMultithreading) { 
        this.enableMultithreading = enableMultithreading; 
    }
    
    public int getWorkerThreads() { return workerThreads; }
    public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
    
    public boolean isEnableFrustumCulling() { return enableFrustumCulling; }
    public void setEnableFrustumCulling(boolean enableFrustumCulling) { 
        this.enableFrustumCulling = enableFrustumCulling; 
    }
    
    public boolean isEnableOcclusionCulling() { return enableOcclusionCulling; }
    public void setEnableOcclusionCulling(boolean enableOcclusionCulling) {
        this.enableOcclusionCulling = enableOcclusionCulling;
    }

    public boolean isEnableUIRendering() {
        return enableUIRendering;
    }

    public void setEnableUIRendering(boolean enableUIRendering) {
        this.enableUIRendering = enableUIRendering;
    }
    
    public int getJobSystemWorkerCount() { return jobSystemWorkerCount; }
    public void setJobSystemWorkerCount(int jobSystemWorkerCount) { 
        this.jobSystemWorkerCount = jobSystemWorkerCount; 
    }
    
    public int getJobQueueCapacity() { return jobQueueCapacity; }
    public void setJobQueueCapacity(int jobQueueCapacity) { 
        this.jobQueueCapacity = jobQueueCapacity; 
    }
    
    public int getGlobalJobQueueCapacity() { return globalJobQueueCapacity; }
    public void setGlobalJobQueueCapacity(int globalJobQueueCapacity) { 
        this.globalJobQueueCapacity = globalJobQueueCapacity; 
    }
    
    public boolean isJobSystemStatisticsEnabled() { return jobSystemStatisticsEnabled; }
    public void setJobSystemStatisticsEnabled(boolean jobSystemStatisticsEnabled) { 
        this.jobSystemStatisticsEnabled = jobSystemStatisticsEnabled; 
    }
    
    /**
     * Returns a map of all game settings for serialization.
     * 
     * @return a map containing all game settings
     */
    public java.util.Map<String, Object> getGameSettings() {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("windowWidth", windowWidth);
        settings.put("windowHeight", windowHeight);
        settings.put("fullscreen", fullscreen);
        settings.put("vsync", vsync);
        settings.put("targetFPS", targetFPS);
        settings.put("debugMode", debugMode);
        settings.put("worldSeed", worldSeed);
        settings.put("renderDistance", renderDistance);
        settings.put("maxChunkUpdatesPerFrame", maxChunkUpdatesPerFrame);
        settings.put("masterVolume", masterVolume);
        settings.put("musicVolume", musicVolume);
        settings.put("effectsVolume", effectsVolume);
        settings.put("ambientVolume", ambientVolume);
        settings.put("mouseSensitivity", mouseSensitivity);
        settings.put("enableTidalSystem", enableTidalSystem);
        settings.put("enableWeatherSystem", enableWeatherSystem);
        settings.put("enableDynamicWaves", enableDynamicWaves);
        settings.put("oceanSimulationQuality", oceanSimulationQuality);
        settings.put("enableMultithreading", enableMultithreading);
        settings.put("workerThreads", workerThreads);
        settings.put("enableFrustumCulling", enableFrustumCulling);
        settings.put("enableOcclusionCulling", enableOcclusionCulling);
        settings.put("jobSystemWorkerCount", jobSystemWorkerCount);
        settings.put("jobQueueCapacity", jobQueueCapacity);
        settings.put("globalJobQueueCapacity", globalJobQueueCapacity);
        settings.put("jobSystemStatisticsEnabled", jobSystemStatisticsEnabled);
        return settings;
    }
    
    public void applyGameSettings(java.util.Map<String, Object> settings) {
        if (settings == null) {
            return;
        }
        
        // Display settings
        if (settings.containsKey("windowWidth")) {
            setWindowWidth(((Number) settings.get("windowWidth")).intValue());
        }
        if (settings.containsKey("windowHeight")) {
            setWindowHeight(((Number) settings.get("windowHeight")).intValue());
        }
        if (settings.containsKey("fullscreen")) {
            setFullscreen((Boolean) settings.get("fullscreen"));
        }
        if (settings.containsKey("vsync")) {
            setVsync((Boolean) settings.get("vsync"));
        }
        if (settings.containsKey("targetFPS")) {
            setTargetFPS(((Number) settings.get("targetFPS")).intValue());
        }
        
        // Game settings
        if (settings.containsKey("debugMode")) {
            setDebugMode((Boolean) settings.get("debugMode"));
        }
        if (settings.containsKey("worldSeed")) {
            setWorldSeed(((Number) settings.get("worldSeed")).longValue());
        }
        if (settings.containsKey("renderDistance")) {
            setRenderDistance(((Number) settings.get("renderDistance")).floatValue());
        }
        if (settings.containsKey("maxChunkUpdatesPerFrame")) {
            setMaxChunkUpdatesPerFrame(((Number) settings.get("maxChunkUpdatesPerFrame")).intValue());
        }
        
        // Audio settings
        if (settings.containsKey("masterVolume")) {
            setMasterVolume(((Number) settings.get("masterVolume")).floatValue());
        }
        if (settings.containsKey("musicVolume")) {
            setMusicVolume(((Number) settings.get("musicVolume")).floatValue());
        }
        if (settings.containsKey("effectsVolume")) {
            setEffectsVolume(((Number) settings.get("effectsVolume")).floatValue());
        }
        if (settings.containsKey("ambientVolume")) {
            setAmbientVolume(((Number) settings.get("ambientVolume")).floatValue());
        }
        
        // Input settings
        if (settings.containsKey("mouseSensitivity")) {
            setMouseSensitivity(((Number) settings.get("mouseSensitivity")).floatValue());
        }
        
        // Ocean simulation settings
        if (settings.containsKey("enableTidalSystem")) {
            setEnableTidalSystem((Boolean) settings.get("enableTidalSystem"));
        }
        if (settings.containsKey("enableWeatherSystem")) {
            setEnableWeatherSystem((Boolean) settings.get("enableWeatherSystem"));
        }
        if (settings.containsKey("enableDynamicWaves")) {
            setEnableDynamicWaves((Boolean) settings.get("enableDynamicWaves"));
        }
        if (settings.containsKey("oceanSimulationQuality")) {
            setOceanSimulationQuality(((Number) settings.get("oceanSimulationQuality")).floatValue());
        }
        
        // Performance settings
        if (settings.containsKey("enableMultithreading")) {
            setEnableMultithreading((Boolean) settings.get("enableMultithreading"));
        }
        if (settings.containsKey("workerThreads")) {
            setWorkerThreads(((Number) settings.get("workerThreads")).intValue());
        }
        if (settings.containsKey("enableFrustumCulling")) {
            setEnableFrustumCulling((Boolean) settings.get("enableFrustumCulling"));
        }
        if (settings.containsKey("enableOcclusionCulling")) {
            setEnableOcclusionCulling((Boolean) settings.get("enableOcclusionCulling"));
        }
        
        // Job System settings
        if (settings.containsKey("jobSystemWorkerCount")) {
            setJobSystemWorkerCount(((Number) settings.get("jobSystemWorkerCount")).intValue());
        }
        if (settings.containsKey("jobQueueCapacity")) {
            setJobQueueCapacity(((Number) settings.get("jobQueueCapacity")).intValue());
        }
        if (settings.containsKey("globalJobQueueCapacity")) {
            setGlobalJobQueueCapacity(((Number) settings.get("globalJobQueueCapacity")).intValue());
        }
        if (settings.containsKey("jobSystemStatisticsEnabled")) {
            setJobSystemStatisticsEnabled((Boolean) settings.get("jobSystemStatisticsEnabled"));
        }
    }
    
    @Override
    public String toString() {
        return String.format("GameConfig{resolution=%dx%d, fullscreen=%s, seed=%d, debug=%s}", 
                           windowWidth, windowHeight, fullscreen, worldSeed, debugMode);
    }
}