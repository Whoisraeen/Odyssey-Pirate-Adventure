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
    
    @Override
    public String toString() {
        return String.format("GameConfig{resolution=%dx%d, fullscreen=%s, seed=%d, debug=%s}", 
                           windowWidth, windowHeight, fullscreen, worldSeed, debugMode);
    }
}