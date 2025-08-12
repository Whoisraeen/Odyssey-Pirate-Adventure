package com.odyssey.world.biomes;

import java.util.*;

/**
 * Manages smooth transitions between different biomes.
 * Handles blending of properties, gradual changes, and transition zones.
 */
public class BiomeTransition {
    private final BiomeType fromBiome;
    private final BiomeType toBiome;
    private final TransitionType transitionType;
    private final float transitionDistance;
    private final Map<String, TransitionRule> transitionRules;
    
    // Transition parameters
    private float blendFactor = 0.5f;
    private float transitionSpeed = 1.0f;
    private boolean isActive = false;
    private long startTime = 0;
    private float duration = 0.0f;
    
    public BiomeTransition(BiomeType fromBiome, BiomeType toBiome, TransitionType type, float distance) {
        this.fromBiome = fromBiome;
        this.toBiome = toBiome;
        this.transitionType = type;
        this.transitionDistance = distance;
        this.transitionRules = new HashMap<>();
        
        initializeDefaultRules();
    }
    
    /**
     * Initializes default transition rules
     */
    private void initializeDefaultRules() {
        // Temperature transition
        addTransitionRule("temperature", new LinearTransitionRule(0.8f));
        
        // Humidity transition
        addTransitionRule("humidity", new LinearTransitionRule(0.7f));
        
        // Elevation transition
        addTransitionRule("elevation", new SmoothTransitionRule(0.9f));
        
        // Color transition
        addTransitionRule("color", new ColorTransitionRule(0.6f));
        
        // Feature density transition
        addTransitionRule("feature_density", new ExponentialTransitionRule(0.5f));
        
        // Wave intensity transition
        addTransitionRule("wave_intensity", new LinearTransitionRule(0.8f));
        
        // Danger level transition
        addTransitionRule("danger_level", new StepTransitionRule(0.3f));
        
        // Mystical flux transition
        addTransitionRule("mystical_flux", new SmoothTransitionRule(0.4f));
    }
    
    /**
     * Adds a transition rule for a specific property
     */
    public void addTransitionRule(String property, TransitionRule rule) {
        transitionRules.put(property, rule);
    }
    
    /**
     * Starts the transition
     */
    public void startTransition(float duration) {
        this.isActive = true;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.blendFactor = 0.0f;
    }
    
    /**
     * Updates the transition
     */
    public void update(float deltaTime) {
        if (!isActive) return;
        
        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f;
        float progress = Math.min(1.0f, elapsed / duration);
        
        // Update blend factor based on transition type
        switch (transitionType) {
            case LINEAR:
                blendFactor = progress;
                break;
            case SMOOTH:
                blendFactor = smoothStep(progress);
                break;
            case EXPONENTIAL:
                blendFactor = 1.0f - (float) Math.exp(-3.0f * progress);
                break;
            case SINE:
                blendFactor = (float) Math.sin(progress * Math.PI * 0.5);
                break;
            case STEP:
                blendFactor = progress > 0.5f ? 1.0f : 0.0f;
                break;
        }
        
        // Check if transition is complete
        if (progress >= 1.0f) {
            completeTransition();
        }
    }
    
    /**
     * Completes the transition
     */
    private void completeTransition() {
        isActive = false;
        blendFactor = 1.0f;
    }
    
    /**
     * Calculates blended property value
     */
    public float blendProperty(String property, float fromValue, float toValue) {
        TransitionRule rule = transitionRules.get(property);
        if (rule != null) {
            return rule.blend(fromValue, toValue, blendFactor);
        }
        
        // Default linear blending
        return fromValue + (toValue - fromValue) * blendFactor;
    }
    
    /**
     * Calculates blended color
     */
    public int blendColor(int fromColor, int toColor) {
        TransitionRule rule = transitionRules.get("color");
        if (rule instanceof ColorTransitionRule) {
            return ((ColorTransitionRule) rule).blendColor(fromColor, toColor, blendFactor);
        }
        
        // Default color blending
        return blendColorDefault(fromColor, toColor, blendFactor);
    }
    
    /**
     * Default color blending
     */
    private int blendColorDefault(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * Calculates distance-based blend factor
     */
    public float calculateDistanceBlend(float distance) {
        if (distance <= 0) return 1.0f;
        if (distance >= transitionDistance) return 0.0f;
        
        float normalizedDistance = distance / transitionDistance;
        
        switch (transitionType) {
            case LINEAR:
                return 1.0f - normalizedDistance;
            case SMOOTH:
                return smoothStep(1.0f - normalizedDistance);
            case EXPONENTIAL:
                return (float) Math.exp(-3.0f * normalizedDistance);
            case SINE:
                return (float) Math.cos(normalizedDistance * Math.PI * 0.5);
            case STEP:
                return normalizedDistance < 0.5f ? 1.0f : 0.0f;
            default:
                return 1.0f - normalizedDistance;
        }
    }
    
    /**
     * Smooth step function
     */
    private float smoothStep(float t) {
        return t * t * (3.0f - 2.0f * t);
    }
    
    /**
     * Checks if biomes are compatible for transition
     */
    public boolean areCompatible() {
        // Check temperature compatibility
        float tempDiff = Math.abs(fromBiome.getTemperature() - toBiome.getTemperature());
        if (tempDiff > 0.8f) return false;
        
        // Check humidity compatibility
        float humidityDiff = Math.abs(fromBiome.getHumidity() - toBiome.getHumidity());
        if (humidityDiff > 0.9f) return false;
        
        // Check category compatibility
        BiomeType.Category fromCategory = fromBiome.getCategory();
        BiomeType.Category toCategory = toBiome.getCategory();
        
        // Some categories are incompatible
        if ((fromCategory == BiomeType.Category.SPECIAL && toCategory == BiomeType.Category.OCEAN) ||
            (fromCategory == BiomeType.Category.OCEAN && toCategory == BiomeType.Category.SPECIAL)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Gets transition difficulty
     */
    public float getTransitionDifficulty() {
        float difficulty = 0.0f;
        
        // Temperature difference
        difficulty += Math.abs(fromBiome.getTemperature() - toBiome.getTemperature()) * 0.3f;
        
        // Humidity difference
        difficulty += Math.abs(fromBiome.getHumidity() - toBiome.getHumidity()) * 0.2f;
        
        // Elevation difference
        difficulty += Math.abs(fromBiome.getElevation() - toBiome.getElevation()) * 0.1f;
        
        // Category difference
        if (fromBiome.getCategory() != toBiome.getCategory()) {
            difficulty += 0.4f;
        }
        
        return Math.min(1.0f, difficulty);
    }
    
    /**
     * Creates a transition zone biome
     */
    public BiomeType createTransitionZone() {
        String transitionName = fromBiome.getName() + "-" + toBiome.getName() + " Transition";
        
        return new BiomeType.Builder(transitionName)
            .temperature(blendProperty("temperature", fromBiome.getTemperature(), toBiome.getTemperature()))
            .humidity(blendProperty("humidity", fromBiome.getHumidity(), toBiome.getHumidity()))
            .elevation(blendProperty("elevation", fromBiome.getElevation(), toBiome.getElevation()))
            .color(blendColor(fromBiome.getColor(), toBiome.getColor()))
            .precipitation(blendPrecipitation())
            .category(selectDominantCategory())
            .build();
    }
    
    /**
     * Blends precipitation types
     */
    private BiomeType.Precipitation blendPrecipitation() {
        BiomeType.Precipitation fromPrec = fromBiome.getPrecipitation();
        BiomeType.Precipitation toPrec = toBiome.getPrecipitation();
        
        if (fromPrec == toPrec) return fromPrec;
        
        // Simple blending logic
        if (blendFactor < 0.5f) {
            return fromPrec;
        } else {
            return toPrec;
        }
    }
    
    /**
     * Selects dominant category
     */
    private BiomeType.Category selectDominantCategory() {
        if (blendFactor < 0.5f) {
            return fromBiome.getCategory();
        } else {
            return toBiome.getCategory();
        }
    }
    
    // Getters
    public BiomeType getFromBiome() { return fromBiome; }
    public BiomeType getToBiome() { return toBiome; }
    public TransitionType getTransitionType() { return transitionType; }
    public float getTransitionDistance() { return transitionDistance; }
    public float getBlendFactor() { return blendFactor; }
    public float getTransitionSpeed() { return transitionSpeed; }
    public boolean isActive() { return isActive; }
    public float getDuration() { return duration; }
    public float getProgress() { 
        if (!isActive || duration <= 0) return 0.0f;
        float elapsed = (System.currentTimeMillis() - startTime) / 1000.0f;
        return Math.min(1.0f, elapsed / duration);
    }
    
    // Setters
    public void setTransitionSpeed(float speed) { this.transitionSpeed = speed; }
    public void setBlendFactor(float factor) { this.blendFactor = Math.max(0.0f, Math.min(1.0f, factor)); }
    
    @Override
    public String toString() {
        return String.format("BiomeTransition{from='%s', to='%s', type=%s, blend=%.2f, active=%s}", 
            fromBiome.getName(), toBiome.getName(), transitionType, blendFactor, isActive);
    }
    
    /**
     * Transition type enumeration
     */
    public enum TransitionType {
        LINEAR,      // Linear interpolation
        SMOOTH,      // Smooth step interpolation
        EXPONENTIAL, // Exponential curve
        SINE,        // Sine curve
        STEP         // Step function
    }
    
    /**
     * Base transition rule interface
     */
    public interface TransitionRule {
        float blend(float fromValue, float toValue, float factor);
    }
    
    /**
     * Linear transition rule
     */
    public static class LinearTransitionRule implements TransitionRule {
        private final float smoothness;
        
        public LinearTransitionRule(float smoothness) {
            this.smoothness = smoothness;
        }
        
        @Override
        public float blend(float fromValue, float toValue, float factor) {
            float adjustedFactor = factor * smoothness + (1.0f - smoothness) * (factor > 0.5f ? 1.0f : 0.0f);
            return fromValue + (toValue - fromValue) * adjustedFactor;
        }
    }
    
    /**
     * Smooth transition rule
     */
    public static class SmoothTransitionRule implements TransitionRule {
        private final float smoothness;
        
        public SmoothTransitionRule(float smoothness) {
            this.smoothness = smoothness;
        }
        
        @Override
        public float blend(float fromValue, float toValue, float factor) {
            float smoothFactor = factor * factor * (3.0f - 2.0f * factor);
            smoothFactor = smoothFactor * smoothness + factor * (1.0f - smoothness);
            return fromValue + (toValue - fromValue) * smoothFactor;
        }
    }
    
    /**
     * Exponential transition rule
     */
    public static class ExponentialTransitionRule implements TransitionRule {
        private final float strength;
        
        public ExponentialTransitionRule(float strength) {
            this.strength = strength;
        }
        
        @Override
        public float blend(float fromValue, float toValue, float factor) {
            float expFactor = 1.0f - (float) Math.exp(-strength * 5.0f * factor);
            return fromValue + (toValue - fromValue) * expFactor;
        }
    }
    
    /**
     * Step transition rule
     */
    public static class StepTransitionRule implements TransitionRule {
        private final float threshold;
        
        public StepTransitionRule(float threshold) {
            this.threshold = threshold;
        }
        
        @Override
        public float blend(float fromValue, float toValue, float factor) {
            return factor > threshold ? toValue : fromValue;
        }
    }
    
    /**
     * Color transition rule
     */
    public static class ColorTransitionRule implements TransitionRule {
        private final float smoothness;
        
        public ColorTransitionRule(float smoothness) {
            this.smoothness = smoothness;
        }
        
        @Override
        public float blend(float fromValue, float toValue, float factor) {
            // This is used for individual color components
            float smoothFactor = factor * factor * (3.0f - 2.0f * factor);
            smoothFactor = smoothFactor * smoothness + factor * (1.0f - smoothness);
            return fromValue + (toValue - fromValue) * smoothFactor;
        }
        
        public int blendColor(int color1, int color2, float factor) {
            float smoothFactor = factor * factor * (3.0f - 2.0f * factor);
            smoothFactor = smoothFactor * smoothness + factor * (1.0f - smoothness);
            
            int r1 = (color1 >> 16) & 0xFF;
            int g1 = (color1 >> 8) & 0xFF;
            int b1 = color1 & 0xFF;
            
            int r2 = (color2 >> 16) & 0xFF;
            int g2 = (color2 >> 8) & 0xFF;
            int b2 = color2 & 0xFF;
            
            int r = (int) (r1 + (r2 - r1) * smoothFactor);
            int g = (int) (g1 + (g2 - g1) * smoothFactor);
            int b = (int) (b1 + (b2 - b1) * smoothFactor);
            
            return (r << 16) | (g << 8) | b;
        }
    }
}