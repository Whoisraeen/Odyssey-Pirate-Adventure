package com.odyssey.rendering;

import com.odyssey.util.Logger;

/**
 * Comprehensive graphics settings system for managing rendering quality and performance.
 * Provides centralized control over all advanced rendering effects with preset quality levels.
 */
public class GraphicsSettings {
    private static final Logger logger = new Logger(GraphicsSettings.class);
    
    // Quality presets
    public enum QualityPreset {
        LOW("Low", "Optimized for performance"),
        MEDIUM("Medium", "Balanced quality and performance"),
        HIGH("High", "High quality visuals"),
        ULTRA("Ultra", "Maximum visual fidelity"),
        CUSTOM("Custom", "User-defined settings");
        
        private final String displayName;
        private final String description;
        
        QualityPreset(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Post-processing quality levels
    public enum PostProcessingQuality {
        DISABLED(0, 0, 0),
        LOW(2, 4, 2),
        MEDIUM(4, 8, 4),
        HIGH(6, 12, 6),
        ULTRA(8, 16, 8);
        
        private final int bloomPasses;
        private final int motionBlurSamples;
        private final int taaHistoryFrames;
        
        PostProcessingQuality(int bloomPasses, int motionBlurSamples, int taaHistoryFrames) {
            this.bloomPasses = bloomPasses;
            this.motionBlurSamples = motionBlurSamples;
            this.taaHistoryFrames = taaHistoryFrames;
        }
        
        public int getBloomPasses() { return bloomPasses; }
        public int getMotionBlurSamples() { return motionBlurSamples; }
        public int getTaaHistoryFrames() { return taaHistoryFrames; }
    }
    
    // Shadow quality levels
    public enum ShadowQuality {
        DISABLED(0, 0, false, 0),
        LOW(1024, 1, false, 2),
        MEDIUM(2048, 2, true, 4),
        HIGH(4096, 3, true, 8),
        ULTRA(8192, 4, true, 16);
        
        private final int resolution;
        private final int cascades;
        private final boolean pcfEnabled;
        private final int pcfSamples;
        
        ShadowQuality(int resolution, int cascades, boolean pcfEnabled, int pcfSamples) {
            this.resolution = resolution;
            this.cascades = cascades;
            this.pcfEnabled = pcfEnabled;
            this.pcfSamples = pcfSamples;
        }
        
        public int getResolution() { return resolution; }
        public int getCascades() { return cascades; }
        public boolean isPcfEnabled() { return pcfEnabled; }
        public int getPcfSamples() { return pcfSamples; }
    }
    
    // Water quality levels
    public enum WaterQuality {
        LOW(256, 1, false, false),
        MEDIUM(512, 2, true, false),
        HIGH(1024, 4, true, true),
        ULTRA(2048, 8, true, true);
        
        private final int reflectionResolution;
        private final int waveDetail;
        private final boolean refractionEnabled;
        private final boolean causticEnabled;
        
        WaterQuality(int reflectionResolution, int waveDetail, boolean refractionEnabled, boolean causticEnabled) {
            this.reflectionResolution = reflectionResolution;
            this.waveDetail = waveDetail;
            this.refractionEnabled = refractionEnabled;
            this.causticEnabled = causticEnabled;
        }
        
        public int getReflectionResolution() { return reflectionResolution; }
        public int getWaveDetail() { return waveDetail; }
        public boolean isRefractionEnabled() { return refractionEnabled; }
        public boolean isCausticEnabled() { return causticEnabled; }
    }
    
    // Current settings
    private QualityPreset currentPreset = QualityPreset.MEDIUM;
    
    // Rendering toggles
    private boolean postProcessingEnabled = true;
    private boolean shadowsEnabled = true;
    private boolean ambientOcclusionEnabled = true;
    private boolean bloomEnabled = true;
    private boolean taaEnabled = true;
    private boolean motionBlurEnabled = true;
    private boolean depthOfFieldEnabled = true;
    private boolean volumetricFogEnabled = true;
    private boolean volumetricCloudsEnabled = true;
    private boolean waterRenderingEnabled = true;
    private boolean skyRenderingEnabled = true;
    private boolean iblEnabled = true;
    private boolean voxelAOEnabled = true;
    
    // Quality levels
    private PostProcessingQuality postProcessingQuality = PostProcessingQuality.MEDIUM;
    private ShadowQuality shadowQuality = ShadowQuality.MEDIUM;
    private WaterQuality waterQuality = WaterQuality.MEDIUM;
    
    // Performance settings
    private float renderDistance = 256.0f;
    private int maxFPS = 60;
    private boolean vsyncEnabled = true;
    private boolean adaptiveQuality = false;
    private float targetFrameTime = 16.67f; // 60 FPS
    
    // Advanced settings
    private float bloomThreshold = 1.0f;
    private float bloomIntensity = 0.8f;
    private float exposureCompensation = 0.0f;
    private float gamma = 2.2f;
    private float saturation = 1.0f;
    private float contrast = 1.0f;
    
    /**
     * Apply a quality preset
     */
    public void applyPreset(QualityPreset preset) {
        this.currentPreset = preset;
        
        switch (preset) {
            case LOW:
                applyLowQualitySettings();
                break;
            case MEDIUM:
                applyMediumQualitySettings();
                break;
            case HIGH:
                applyHighQualitySettings();
                break;
            case ULTRA:
                applyUltraQualitySettings();
                break;
            case CUSTOM:
                // Keep current settings
                break;
        }
        
        logger.info("Applied " + preset.getDisplayName() + " quality preset");
    }
    
    private void applyLowQualitySettings() {
        postProcessingEnabled = false;
        shadowsEnabled = true;
        ambientOcclusionEnabled = false;
        bloomEnabled = false;
        taaEnabled = false;
        motionBlurEnabled = false;
        depthOfFieldEnabled = false;
        volumetricFogEnabled = false;
        volumetricCloudsEnabled = false;
        waterRenderingEnabled = true;
        skyRenderingEnabled = true;
        iblEnabled = false;
        voxelAOEnabled = false;
        
        postProcessingQuality = PostProcessingQuality.DISABLED;
        shadowQuality = ShadowQuality.LOW;
        waterQuality = WaterQuality.LOW;
        
        renderDistance = 128.0f;
        maxFPS = 60;
    }
    
    private void applyMediumQualitySettings() {
        postProcessingEnabled = true;
        shadowsEnabled = true;
        ambientOcclusionEnabled = true;
        bloomEnabled = true;
        taaEnabled = true;
        motionBlurEnabled = false;
        depthOfFieldEnabled = false;
        volumetricFogEnabled = true;
        volumetricCloudsEnabled = false;
        waterRenderingEnabled = true;
        skyRenderingEnabled = true;
        iblEnabled = true;
        voxelAOEnabled = true;
        
        postProcessingQuality = PostProcessingQuality.MEDIUM;
        shadowQuality = ShadowQuality.MEDIUM;
        waterQuality = WaterQuality.MEDIUM;
        
        renderDistance = 256.0f;
        maxFPS = 60;
    }
    
    private void applyHighQualitySettings() {
        postProcessingEnabled = true;
        shadowsEnabled = true;
        ambientOcclusionEnabled = true;
        bloomEnabled = true;
        taaEnabled = true;
        motionBlurEnabled = true;
        depthOfFieldEnabled = true;
        volumetricFogEnabled = true;
        volumetricCloudsEnabled = true;
        waterRenderingEnabled = true;
        skyRenderingEnabled = true;
        iblEnabled = true;
        voxelAOEnabled = true;
        
        postProcessingQuality = PostProcessingQuality.HIGH;
        shadowQuality = ShadowQuality.HIGH;
        waterQuality = WaterQuality.HIGH;
        
        renderDistance = 384.0f;
        maxFPS = 60;
    }
    
    private void applyUltraQualitySettings() {
        postProcessingEnabled = true;
        shadowsEnabled = true;
        ambientOcclusionEnabled = true;
        bloomEnabled = true;
        taaEnabled = true;
        motionBlurEnabled = true;
        depthOfFieldEnabled = true;
        volumetricFogEnabled = true;
        volumetricCloudsEnabled = true;
        waterRenderingEnabled = true;
        skyRenderingEnabled = true;
        iblEnabled = true;
        voxelAOEnabled = true;
        
        postProcessingQuality = PostProcessingQuality.ULTRA;
        shadowQuality = ShadowQuality.ULTRA;
        waterQuality = WaterQuality.ULTRA;
        
        renderDistance = 512.0f;
        maxFPS = 120;
    }
    
    /**
     * Auto-adjust quality based on performance
     */
    public void autoAdjustQuality(float currentFrameTime) {
        if (!adaptiveQuality) return;
        
        float performanceRatio = currentFrameTime / targetFrameTime;
        
        if (performanceRatio > 1.5f) {
            // Performance is poor, reduce quality
            downgradeQuality();
        } else if (performanceRatio < 0.8f) {
            // Performance is good, try to increase quality
            upgradeQuality();
        }
    }
    
    private void downgradeQuality() {
        if (currentPreset == QualityPreset.ULTRA) {
            applyPreset(QualityPreset.HIGH);
        } else if (currentPreset == QualityPreset.HIGH) {
            applyPreset(QualityPreset.MEDIUM);
        } else if (currentPreset == QualityPreset.MEDIUM) {
            applyPreset(QualityPreset.LOW);
        }
    }
    
    private void upgradeQuality() {
        if (currentPreset == QualityPreset.LOW) {
            applyPreset(QualityPreset.MEDIUM);
        } else if (currentPreset == QualityPreset.MEDIUM) {
            applyPreset(QualityPreset.HIGH);
        } else if (currentPreset == QualityPreset.HIGH) {
            applyPreset(QualityPreset.ULTRA);
        }
    }
    
    // Getters and setters
    public QualityPreset getCurrentPreset() { return currentPreset; }
    
    public boolean isPostProcessingEnabled() { return postProcessingEnabled; }
    public void setPostProcessingEnabled(boolean enabled) { 
        this.postProcessingEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isShadowsEnabled() { return shadowsEnabled; }
    public void setShadowsEnabled(boolean enabled) { 
        this.shadowsEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isAmbientOcclusionEnabled() { return ambientOcclusionEnabled; }
    public void setAmbientOcclusionEnabled(boolean enabled) { 
        this.ambientOcclusionEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isBloomEnabled() { return bloomEnabled; }
    public void setBloomEnabled(boolean enabled) { 
        this.bloomEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isTaaEnabled() { return taaEnabled; }
    public void setTaaEnabled(boolean enabled) { 
        this.taaEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isMotionBlurEnabled() { return motionBlurEnabled; }
    public void setMotionBlurEnabled(boolean enabled) { 
        this.motionBlurEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isDepthOfFieldEnabled() { return depthOfFieldEnabled; }
    public void setDepthOfFieldEnabled(boolean enabled) { 
        this.depthOfFieldEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isVolumetricFogEnabled() { return volumetricFogEnabled; }
    public void setVolumetricFogEnabled(boolean enabled) { 
        this.volumetricFogEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isVolumetricCloudsEnabled() { return volumetricCloudsEnabled; }
    public void setVolumetricCloudsEnabled(boolean enabled) { 
        this.volumetricCloudsEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isWaterRenderingEnabled() { return waterRenderingEnabled; }
    public void setWaterRenderingEnabled(boolean enabled) { 
        this.waterRenderingEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isSkyRenderingEnabled() { return skyRenderingEnabled; }
    public void setSkyRenderingEnabled(boolean enabled) { 
        this.skyRenderingEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isIblEnabled() { return iblEnabled; }
    public void setIblEnabled(boolean enabled) { 
        this.iblEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isVoxelAOEnabled() { return voxelAOEnabled; }
    public void setVoxelAOEnabled(boolean enabled) { 
        this.voxelAOEnabled = enabled;
        if (enabled) currentPreset = QualityPreset.CUSTOM;
    }
    
    public PostProcessingQuality getPostProcessingQuality() { return postProcessingQuality; }
    public void setPostProcessingQuality(PostProcessingQuality quality) { 
        this.postProcessingQuality = quality;
        currentPreset = QualityPreset.CUSTOM;
    }
    
    public ShadowQuality getShadowQuality() { return shadowQuality; }
    public void setShadowQuality(ShadowQuality quality) { 
        this.shadowQuality = quality;
        currentPreset = QualityPreset.CUSTOM;
    }
    
    public WaterQuality getWaterQuality() { return waterQuality; }
    public void setWaterQuality(WaterQuality quality) { 
        this.waterQuality = quality;
        currentPreset = QualityPreset.CUSTOM;
    }
    
    public float getRenderDistance() { return renderDistance; }
    public void setRenderDistance(float distance) { 
        this.renderDistance = Math.max(64.0f, Math.min(1024.0f, distance));
        currentPreset = QualityPreset.CUSTOM;
    }
    
    public int getMaxFPS() { return maxFPS; }
    public void setMaxFPS(int fps) { 
        this.maxFPS = Math.max(30, Math.min(240, fps));
        this.targetFrameTime = 1000.0f / fps;
        currentPreset = QualityPreset.CUSTOM;
    }
    
    public boolean isVsyncEnabled() { return vsyncEnabled; }
    public void setVsyncEnabled(boolean enabled) { this.vsyncEnabled = enabled; }
    
    public boolean isAdaptiveQuality() { return adaptiveQuality; }
    public void setAdaptiveQuality(boolean enabled) { this.adaptiveQuality = enabled; }
    
    public float getTargetFrameTime() { return targetFrameTime; }
    
    // Advanced settings getters/setters
    public float getBloomThreshold() { return bloomThreshold; }
    public void setBloomThreshold(float threshold) { this.bloomThreshold = Math.max(0.0f, threshold); }
    
    public float getBloomIntensity() { return bloomIntensity; }
    public void setBloomIntensity(float intensity) { this.bloomIntensity = Math.max(0.0f, intensity); }
    
    public float getExposureCompensation() { return exposureCompensation; }
    public void setExposureCompensation(float exposure) { this.exposureCompensation = exposure; }
    
    public float getGamma() { return gamma; }
    public void setGamma(float gamma) { this.gamma = Math.max(1.0f, Math.min(3.0f, gamma)); }
    
    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) { this.saturation = Math.max(0.0f, Math.min(2.0f, saturation)); }
    
    public float getContrast() { return contrast; }
    public void setContrast(float contrast) { this.contrast = Math.max(0.0f, Math.min(2.0f, contrast)); }
}