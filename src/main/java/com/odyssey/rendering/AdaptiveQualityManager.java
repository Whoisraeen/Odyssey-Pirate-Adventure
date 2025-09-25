package com.odyssey.rendering;

import com.odyssey.util.PerformanceProfiler;
import java.util.logging.Logger;

/**
 * Adaptive Quality Manager for automatic performance optimization.
 * 
 * This class monitors frame rate performance and automatically adjusts
 * graphics settings to maintain target FPS while maximizing visual quality.
 * It uses a sophisticated algorithm to balance performance and quality.
 */
public class AdaptiveQualityManager {
    private static final Logger logger = Logger.getLogger(AdaptiveQualityManager.class.getName());
    
    // Performance targets
    private float targetFPS = 60.0f;
    private float minFPS = 30.0f;
    private float maxFPS = 120.0f;
    
    // Adaptation parameters
    private float adaptationSpeed = 0.1f;
    private float stabilityThreshold = 5.0f;
    private int stabilityFrames = 60; // Frames to wait before adjusting
    private int currentStabilityCount = 0;
    
    // Quality adjustment thresholds
    private float downgradeThreshold = 0.85f; // Downgrade if FPS < target * threshold
    private float upgradeThreshold = 1.15f;   // Upgrade if FPS > target * threshold
    
    // References
    private final GraphicsSettings graphicsSettings;
    private final PerformanceProfiler profiler;
    
    // State tracking
    private boolean adaptiveQualityEnabled = true;
    private QualityLevel currentQualityLevel = QualityLevel.MEDIUM;
    private long lastAdjustmentTime = 0;
    private float lastAverageFPS = 0.0f;
    
    // Adjustment cooldown (prevent rapid changes)
    private static final long ADJUSTMENT_COOLDOWN_MS = 2000; // 2 seconds
    
    /**
     * Quality levels for adaptive scaling
     */
    public enum QualityLevel {
        ULTRA(4, "Ultra"),
        HIGH(3, "High"), 
        MEDIUM(2, "Medium"),
        LOW(1, "Low"),
        MINIMAL(0, "Minimal");
        
        private final int level;
        private final String displayName;
        
        QualityLevel(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
        
        public int getLevel() { return level; }
        public String getDisplayName() { return displayName; }
        
        public QualityLevel lower() {
            switch (this) {
                case ULTRA: return HIGH;
                case HIGH: return MEDIUM;
                case MEDIUM: return LOW;
                case LOW: return MINIMAL;
                case MINIMAL: return MINIMAL;
                default: return MEDIUM;
            }
        }
        
        public QualityLevel higher() {
            switch (this) {
                case MINIMAL: return LOW;
                case LOW: return MEDIUM;
                case MEDIUM: return HIGH;
                case HIGH: return ULTRA;
                case ULTRA: return ULTRA;
                default: return MEDIUM;
            }
        }
    }
    
    /**
     * Constructor
     * @param graphicsSettings The graphics settings to manage
     */
    public AdaptiveQualityManager(GraphicsSettings graphicsSettings) {
        this.graphicsSettings = graphicsSettings;
        this.profiler = PerformanceProfiler.getInstance();
        
        // Initialize based on current preset
        initializeQualityLevel();
        
        logger.info("Adaptive Quality Manager initialized with target FPS: " + targetFPS);
    }
    
    /**
     * Initialize quality level based on current graphics settings
     */
    private void initializeQualityLevel() {
        GraphicsSettings.QualityPreset preset = graphicsSettings.getCurrentPreset();
        switch (preset) {
            case ULTRA:
                currentQualityLevel = QualityLevel.ULTRA;
                break;
            case HIGH:
                currentQualityLevel = QualityLevel.HIGH;
                break;
            case MEDIUM:
                currentQualityLevel = QualityLevel.MEDIUM;
                break;
            case LOW:
                currentQualityLevel = QualityLevel.LOW;
                break;
            case CUSTOM:
                // Estimate quality level based on settings
                currentQualityLevel = estimateQualityLevel();
                break;
        }
    }
    
    /**
     * Estimate quality level based on current settings
     * @return Estimated quality level
     */
    private QualityLevel estimateQualityLevel() {
        int score = 0;
        
        // Count enabled features
        if (graphicsSettings.isPostProcessingEnabled()) score++;
        if (graphicsSettings.isBloomEnabled()) score++;
        if (graphicsSettings.isShadowsEnabled()) score++;
        if (graphicsSettings.isVolumetricFogEnabled()) score++;
        if (graphicsSettings.isVolumetricCloudsEnabled()) score++;
        if (graphicsSettings.isAmbientOcclusionEnabled()) score++;
        
        // Add quality-based scoring
        score += graphicsSettings.getShadowQuality().ordinal();
        score += graphicsSettings.getWaterQuality().ordinal();
        score += graphicsSettings.getPostProcessingQuality().ordinal();
        
        // Map score to quality level
        if (score >= 12) return QualityLevel.ULTRA;
        if (score >= 9) return QualityLevel.HIGH;
        if (score >= 6) return QualityLevel.MEDIUM;
        if (score >= 3) return QualityLevel.LOW;
        return QualityLevel.MINIMAL;
    }
    
    /**
     * Update adaptive quality based on current performance
     * Call this every frame to monitor and adjust quality
     */
    public void update() {
        if (!adaptiveQualityEnabled) return;
        
        float currentFPS = profiler.getCurrentFPS();
        if (currentFPS <= 0) return; // Skip if no valid FPS data
        
        // Check if enough time has passed since last adjustment
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAdjustmentTime < ADJUSTMENT_COOLDOWN_MS) {
            return;
        }
        
        // Check for stability
        if (Math.abs(currentFPS - lastAverageFPS) < stabilityThreshold) {
            currentStabilityCount++;
        } else {
            currentStabilityCount = 0;
        }
        
        lastAverageFPS = currentFPS;
        
        // Only adjust after stability period
        if (currentStabilityCount < stabilityFrames) {
            return;
        }
        
        // Determine if adjustment is needed
        boolean needsDowngrade = currentFPS < targetFPS * downgradeThreshold;
        boolean canUpgrade = currentFPS > targetFPS * upgradeThreshold;
        
        if (needsDowngrade && currentQualityLevel != QualityLevel.MINIMAL) {
            downgradeQuality();
            lastAdjustmentTime = currentTime;
            currentStabilityCount = 0;
        } else if (canUpgrade && currentQualityLevel != QualityLevel.ULTRA) {
            upgradeQuality();
            lastAdjustmentTime = currentTime;
            currentStabilityCount = 0;
        }
    }
    
    /**
     * Downgrade quality settings to improve performance
     */
    private void downgradeQuality() {
        QualityLevel newLevel = currentQualityLevel.lower();
        
        logger.info(String.format("Downgrading quality from %s to %s (FPS: %.2f, Target: %.2f)",
                                currentQualityLevel.getDisplayName(),
                                newLevel.getDisplayName(),
                                profiler.getCurrentFPS(),
                                targetFPS));
        
        applyQualityLevel(newLevel);
        currentQualityLevel = newLevel;
    }
    
    /**
     * Upgrade quality settings when performance allows
     */
    private void upgradeQuality() {
        QualityLevel newLevel = currentQualityLevel.higher();
        
        logger.info(String.format("Upgrading quality from %s to %s (FPS: %.2f, Target: %.2f)",
                                currentQualityLevel.getDisplayName(),
                                newLevel.getDisplayName(),
                                profiler.getCurrentFPS(),
                                targetFPS));
        
        applyQualityLevel(newLevel);
        currentQualityLevel = newLevel;
    }
    
    /**
     * Apply specific quality level settings
     * @param level The quality level to apply
     */
    private void applyQualityLevel(QualityLevel level) {
        switch (level) {
            case MINIMAL:
                applyMinimalSettings();
                break;
            case LOW:
                applyLowSettings();
                break;
            case MEDIUM:
                applyMediumSettings();
                break;
            case HIGH:
                applyHighSettings();
                break;
            case ULTRA:
                applyUltraSettings();
                break;
        }
        
        // Mark as custom preset since we're overriding
        graphicsSettings.setCurrentPreset(GraphicsSettings.QualityPreset.CUSTOM);
    }
    
    /**
     * Apply minimal quality settings for maximum performance
     */
    private void applyMinimalSettings() {
        graphicsSettings.setPostProcessingEnabled(false);
        graphicsSettings.setBloomEnabled(false);
        graphicsSettings.setShadowsEnabled(false);
        graphicsSettings.setVolumetricFogEnabled(false);
        graphicsSettings.setVolumetricCloudsEnabled(false);
        graphicsSettings.setAmbientOcclusionEnabled(false);
        graphicsSettings.setSkyRenderingEnabled(true); // Keep basic sky
        graphicsSettings.setWaterRenderingEnabled(true); // Keep basic water
        graphicsSettings.setRenderDistance(64);
        graphicsSettings.setWaterQuality(GraphicsSettings.WaterQuality.LOW);
    }
    
    /**
     * Apply low quality settings
     */
    private void applyLowSettings() {
        graphicsSettings.setPostProcessingEnabled(false);
        graphicsSettings.setBloomEnabled(false);
        graphicsSettings.setShadowsEnabled(true);
        graphicsSettings.setShadowQuality(GraphicsSettings.ShadowQuality.LOW);
        graphicsSettings.setVolumetricFogEnabled(false);
        graphicsSettings.setVolumetricCloudsEnabled(false);
        graphicsSettings.setAmbientOcclusionEnabled(false);
        graphicsSettings.setSkyRenderingEnabled(true);
        graphicsSettings.setWaterRenderingEnabled(true);
        graphicsSettings.setRenderDistance(96);
        graphicsSettings.setWaterQuality(GraphicsSettings.WaterQuality.LOW);
    }
    
    /**
     * Apply medium quality settings
     */
    private void applyMediumSettings() {
        graphicsSettings.setPostProcessingEnabled(true);
        graphicsSettings.setPostProcessingQuality(GraphicsSettings.PostProcessingQuality.MEDIUM);
        graphicsSettings.setBloomEnabled(false);
        graphicsSettings.setShadowsEnabled(true);
        graphicsSettings.setShadowQuality(GraphicsSettings.ShadowQuality.MEDIUM);
        graphicsSettings.setVolumetricFogEnabled(false);
        graphicsSettings.setVolumetricCloudsEnabled(false);
        graphicsSettings.setAmbientOcclusionEnabled(true);
        graphicsSettings.setSkyRenderingEnabled(true);
        graphicsSettings.setWaterRenderingEnabled(true);
        graphicsSettings.setRenderDistance(128);
        graphicsSettings.setWaterQuality(GraphicsSettings.WaterQuality.MEDIUM);
    }
    
    /**
     * Apply high quality settings
     */
    private void applyHighSettings() {
        graphicsSettings.setPostProcessingEnabled(true);
        graphicsSettings.setPostProcessingQuality(GraphicsSettings.PostProcessingQuality.HIGH);
        graphicsSettings.setBloomEnabled(true);
        graphicsSettings.setShadowsEnabled(true);
        graphicsSettings.setShadowQuality(GraphicsSettings.ShadowQuality.HIGH);
        graphicsSettings.setVolumetricFogEnabled(true);
        graphicsSettings.setVolumetricCloudsEnabled(false);
        graphicsSettings.setAmbientOcclusionEnabled(true);
        graphicsSettings.setSkyRenderingEnabled(true);
        graphicsSettings.setWaterRenderingEnabled(true);
        graphicsSettings.setRenderDistance(192);
        graphicsSettings.setWaterQuality(GraphicsSettings.WaterQuality.HIGH);
    }
    
    /**
     * Apply ultra quality settings
     */
    private void applyUltraSettings() {
        graphicsSettings.setPostProcessingEnabled(true);
        graphicsSettings.setPostProcessingQuality(GraphicsSettings.PostProcessingQuality.ULTRA);
        graphicsSettings.setBloomEnabled(true);
        graphicsSettings.setShadowsEnabled(true);
        graphicsSettings.setShadowQuality(GraphicsSettings.ShadowQuality.ULTRA);
        graphicsSettings.setVolumetricFogEnabled(true);
        graphicsSettings.setVolumetricCloudsEnabled(true);
        graphicsSettings.setAmbientOcclusionEnabled(true);
        graphicsSettings.setSkyRenderingEnabled(true);
        graphicsSettings.setWaterRenderingEnabled(true);
        graphicsSettings.setRenderDistance(256);
        graphicsSettings.setWaterQuality(GraphicsSettings.WaterQuality.ULTRA);
    }
    
    // Getters and Setters
    
    /**
     * Enable or disable adaptive quality scaling
     * @param enabled True to enable adaptive quality
     */
    public void setAdaptiveQualityEnabled(boolean enabled) {
        this.adaptiveQualityEnabled = enabled;
        logger.info("Adaptive quality " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if adaptive quality is enabled
     * @return True if adaptive quality is enabled
     */
    public boolean isAdaptiveQualityEnabled() {
        return adaptiveQualityEnabled;
    }
    
    /**
     * Set the target FPS for adaptive quality
     * @param targetFPS Target frames per second
     */
    public void setTargetFPS(float targetFPS) {
        this.targetFPS = Math.max(15.0f, Math.min(targetFPS, 240.0f));
        profiler.setTargetFPS(this.targetFPS);
        logger.info("Target FPS set to: " + this.targetFPS);
    }
    
    /**
     * Get the current target FPS
     * @return Target frames per second
     */
    public float getTargetFPS() {
        return targetFPS;
    }
    
    /**
     * Set the adaptation speed (how quickly quality changes)
     * @param speed Adaptation speed (0.0 to 1.0)
     */
    public void setAdaptationSpeed(float speed) {
        this.adaptationSpeed = Math.max(0.01f, Math.min(speed, 1.0f));
    }
    
    /**
     * Get the current quality level
     * @return Current quality level
     */
    public QualityLevel getCurrentQualityLevel() {
        return currentQualityLevel;
    }
    
    /**
     * Manually set the quality level (disables adaptive scaling temporarily)
     * @param level The quality level to set
     */
    public void setQualityLevel(QualityLevel level) {
        applyQualityLevel(level);
        currentQualityLevel = level;
        
        // Reset stability counter to prevent immediate changes
        currentStabilityCount = 0;
        lastAdjustmentTime = System.currentTimeMillis();
        
        logger.info("Quality level manually set to: " + level.getDisplayName());
    }
    
    /**
     * Get performance status information
     * @return Performance status string
     */
    public String getPerformanceStatus() {
        return String.format("Quality: %s | FPS: %.1f/%.1f | Adaptive: %s",
                           currentQualityLevel.getDisplayName(),
                           profiler.getCurrentFPS(),
                           targetFPS,
                           adaptiveQualityEnabled ? "ON" : "OFF");
    }
}