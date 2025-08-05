package com.odyssey.world.ocean;

import com.odyssey.core.GameConfig;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced wave simulation system for The Odyssey.
 * Implements realistic Gerstner wave physics with multiple wave components,
 * foam generation, and dynamic wave interactions.
 */
public class WaveSystem {
    private static final Logger logger = LoggerFactory.getLogger(WaveSystem.class);
    
    private final GameConfig config;
    
    // Multi-wave system
    private final List<WaveComponent> waveComponents = new ArrayList<>();
    private static final int MAX_WAVE_COMPONENTS = 8;
    
    // Environmental factors
    private Vector3f windDirection = new Vector3f(1, 0, 0);
    private float windStrength = 5.0f;
    private float waterDepth = 100.0f; // Average water depth
    
    // Wave statistics
    private float significantWaveHeight = 1.0f;
    private float averagePeriod = 8.0f;
    private float waveAge = 1.0f; // How developed the waves are
    
    // Foam and whitecaps
    private final Map<Vector2f, FoamPatch> foamPatches = new HashMap<>();
    private float foamThreshold = 2.5f;
    
    // Swell system
    private final List<SwellComponent> swellComponents = new ArrayList<>();
    
    public static class WaveComponent {
        public float amplitude;
        public float frequency;
        public float phase;
        public Vector2f direction;
        public float steepness;
        
        public WaveComponent(float amplitude, float frequency, Vector2f direction) {
            this.amplitude = amplitude;
            this.frequency = frequency;
            this.direction = new Vector2f(direction).normalize();
            this.phase = (float)(Math.random() * 2 * Math.PI);
            this.steepness = 0.8f; // Gerstner wave steepness
        }
    }
    
    public static class SwellComponent {
        public float amplitude;
        public float wavelength;
        public Vector2f direction;
        public float age;
        
        public SwellComponent(float amplitude, float wavelength, Vector2f direction) {
            this.amplitude = amplitude;
            this.wavelength = wavelength;
            this.direction = new Vector2f(direction).normalize();
            this.age = 0.0f;
        }
    }
    
    public static class FoamPatch {
        public Vector2f position;
        public float intensity;
        public float lifetime;
        public float maxLifetime;
        
        public FoamPatch(Vector2f position, float intensity) {
            this.position = new Vector2f(position);
            this.intensity = intensity;
            this.lifetime = 0.0f;
            this.maxLifetime = 5.0f + (float)Math.random() * 10.0f;
        }
    }
    
    public WaveSystem(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing advanced wave system");
        
        // Initialize base wave components
        generateWaveSpectrum();
        
        // Initialize swell components
        generateInitialSwell();
        
        logger.info("Wave system initialized with {} wave components and {} swell components", 
                   waveComponents.size(), swellComponents.size());
    }
    
    private void generateWaveSpectrum() {
        waveComponents.clear();
        
        // Generate Pierson-Moskowitz spectrum approximation
        for (int i = 0; i < MAX_WAVE_COMPONENTS; i++) {
            float frequency = 0.1f + i * 0.15f; // 0.1 to 1.15 Hz
            float amplitude = calculateSpectrumAmplitude(frequency);
            
            // Add some randomness to direction (±30 degrees from wind)
            float directionVariation = (float)(Math.random() - 0.5) * (float)Math.PI / 3;
            Vector2f direction = new Vector2f(
                (float)Math.cos(Math.atan2(windDirection.z, windDirection.x) + directionVariation),
                (float)Math.sin(Math.atan2(windDirection.z, windDirection.x) + directionVariation)
            );
            
            waveComponents.add(new WaveComponent(amplitude, frequency, direction));
        }
    }
    
    private float calculateSpectrumAmplitude(float frequency) {
        // Simplified Pierson-Moskowitz spectrum
        float alpha = 0.0081f; // Phillips constant
        float g = 9.81f; // Gravity
        float peakFrequency = 0.4f * g / windStrength;
        
        float spectrum = alpha * g * g / (float)Math.pow(2 * Math.PI * frequency, 5) * 
                        (float)Math.exp(-1.25f * (float)Math.pow(peakFrequency / frequency, 4));
        
        return (float)Math.sqrt(2 * spectrum * 0.1f); // Convert to amplitude
    }
    
    private void generateInitialSwell() {
        // Add 2-3 swell components representing distant storms
        for (int i = 0; i < 3; i++) {
            float wavelength = 100 + i * 50; // Long wavelength swells
            float amplitude = 0.5f + (float)Math.random() * 1.0f;
            
            // Random direction not aligned with local wind
            float angle = (float)Math.random() * 2 * (float)Math.PI;
            Vector2f direction = new Vector2f((float)Math.cos(angle), (float)Math.sin(angle));
            
            swellComponents.add(new SwellComponent(amplitude, wavelength, direction));
        }
    }
    
    public void update(double deltaTime, Vector3f windDirection, float windStrength) {
        this.windDirection.set(windDirection);
        this.windStrength = windStrength;
        
        // Update wave age (how long the wind has been blowing)
        float targetWaveAge = calculateWaveAge(windStrength);
        waveAge += (targetWaveAge - waveAge) * (float)deltaTime * 0.1f;
        
        // Update significant wave height based on wind
        float targetWaveHeight = calculateSignificantWaveHeight(windStrength);
        significantWaveHeight += (targetWaveHeight - significantWaveHeight) * (float)deltaTime * 0.05f;
        
        // Update wave components
        updateWaveComponents(deltaTime);
        
        // Update swell components
        updateSwellComponents(deltaTime);
        
        // Update foam patches
        updateFoamPatches(deltaTime);
        
        // Generate new foam where waves are breaking
        generateFoam();
    }
    
    private float calculateWaveAge(float windSpeed) {
        // Wave age = wave speed / wind speed
        float waveSpeed = 1.56f * windSpeed; // Deep water approximation
        return Math.min(3.0f, waveSpeed / Math.max(1.0f, windSpeed));
    }
    
    private float calculateSignificantWaveHeight(float windSpeed) {
        // Simplified relationship: Hs ≈ 0.2 * U^2 / g (for fully developed seas)
        return Math.min(8.0f, 0.2f * windSpeed * windSpeed / 9.81f);
    }
    
    private void updateWaveComponents(double deltaTime) {
        for (WaveComponent wave : waveComponents) {
            // Update phase
            wave.phase += wave.frequency * 2 * Math.PI * (float)deltaTime;
            
            // Update amplitude based on wind alignment
            Vector2f windDir2D = new Vector2f(windDirection.x, windDirection.z).normalize();
            float alignment = wave.direction.dot(windDir2D);
            
            float targetAmplitude = significantWaveHeight * 0.3f * Math.max(0.1f, alignment);
            wave.amplitude += (targetAmplitude - wave.amplitude) * (float)deltaTime * 0.1f;
        }
    }
    
    private void updateSwellComponents(double deltaTime) {
        Iterator<SwellComponent> iterator = swellComponents.iterator();
        while (iterator.hasNext()) {
            SwellComponent swell = iterator.next();
            swell.age += (float)deltaTime;
            
            // Swell decays over time
            swell.amplitude *= 1.0f - (float)deltaTime * 0.001f;
            
            // Remove very weak swell
            if (swell.amplitude < 0.05f) {
                iterator.remove();
            }
        }
    }
    
    private void updateFoamPatches(double deltaTime) {
        Iterator<Map.Entry<Vector2f, FoamPatch>> iterator = foamPatches.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Vector2f, FoamPatch> entry = iterator.next();
            FoamPatch foam = entry.getValue();
            
            foam.lifetime += (float)deltaTime;
            foam.intensity *= 1.0f - (float)deltaTime * 0.2f; // Decay foam
            
            if (foam.lifetime > foam.maxLifetime || foam.intensity < 0.05f) {
                iterator.remove();
            }
        }
    }
    
    private void generateFoam() {
        // Generate foam where wave steepness is high
        for (int i = 0; i < 10; i++) { // Sample 10 random points
            Vector2f pos = new Vector2f(
                (float)(Math.random() - 0.5) * 200,
                (float)(Math.random() - 0.5) * 200
            );
            
            float steepness = getWaveSteepnessAt(pos.x, pos.y, 0);
            if (steepness > foamThreshold && !foamPatches.containsKey(pos)) {
                foamPatches.put(pos, new FoamPatch(pos, steepness - foamThreshold));
            }
        }
    }
    
    public float getWaveHeightAt(float x, float z, double time) {
        float totalHeight = 0.0f;
        
        // Add wave components (Gerstner waves)
        for (WaveComponent wave : waveComponents) {
            float dotProduct = x * wave.direction.x + z * wave.direction.y;
            float wavePhase = wave.frequency * dotProduct - wave.frequency * (float)time + wave.phase;
            totalHeight += wave.amplitude * (float)Math.sin(wavePhase);
        }
        
        // Add swell components
        for (SwellComponent swell : swellComponents) {
            float dotProduct = x * swell.direction.x + z * swell.direction.y;
            float k = 2 * (float)Math.PI / swell.wavelength;
            float omega = (float)Math.sqrt(9.81f * k); // Deep water dispersion
            float swellPhase = k * dotProduct - omega * (float)time;
            totalHeight += swell.amplitude * (float)Math.sin(swellPhase);
        }
        
        return totalHeight;
    }
    
    public Vector3f getWaveVelocityAt(float x, float z, double time) {
        Vector3f totalVelocity = new Vector3f();
        
        // Add wave component velocities
        for (WaveComponent wave : waveComponents) {
            float dotProduct = x * wave.direction.x + z * wave.direction.y;
            float wavePhase = wave.frequency * dotProduct - wave.frequency * (float)time + wave.phase;
            
            float velocityMagnitude = wave.amplitude * wave.frequency * (float)Math.cos(wavePhase);
            totalVelocity.add(
                wave.direction.x * velocityMagnitude,
                wave.amplitude * wave.frequency * (float)Math.sin(wavePhase), // Vertical component
                wave.direction.y * velocityMagnitude
            );
        }
        
        return totalVelocity;
    }
    
    public float getWaveSteepnessAt(float x, float z, double time) {
        // Calculate wave steepness (gradient magnitude)
        float dx = 0.1f;
        float dz = 0.1f;
        
        float heightCenter = getWaveHeightAt(x, z, time);
        float heightX = getWaveHeightAt(x + dx, z, time);
        float heightZ = getWaveHeightAt(x, z + dz, time);
        
        float gradX = (heightX - heightCenter) / dx;
        float gradZ = (heightZ - heightCenter) / dz;
        
        return (float)Math.sqrt(gradX * gradX + gradZ * gradZ);
    }
    
    public boolean isWaveBreaking(float x, float z, double time) {
        return getWaveSteepnessAt(x, z, time) > 0.8f; // Breaking threshold
    }
    
    public float getFoamIntensityAt(float x, float z) {
        Vector2f pos = new Vector2f(x, z);
        FoamPatch nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (FoamPatch foam : foamPatches.values()) {
            float distance = pos.distance(foam.position);
            if (distance < minDistance && distance < 10.0f) { // Foam influence radius
                minDistance = distance;
                nearest = foam;
            }
        }
        
        if (nearest != null) {
            float falloff = 1.0f - (minDistance / 10.0f);
            return nearest.intensity * falloff;
        }
        
        return 0.0f;
    }
    
    public void addSwell(float amplitude, float wavelength, Vector2f direction) {
        swellComponents.add(new SwellComponent(amplitude, wavelength, direction));
        logger.debug("Added swell component: amplitude={}, wavelength={}, direction={}", 
                    amplitude, wavelength, direction);
    }
    
    public void cleanup() {
        logger.info("Cleaning up advanced wave system");
        waveComponents.clear();
        swellComponents.clear();
        foamPatches.clear();
    }
    
    // Getters
    public float getSignificantWaveHeight() { return significantWaveHeight; }
    public float getAveragePeriod() { return averagePeriod; }
    public float getWaveAge() { return waveAge; }
    public int getFoamPatchCount() { return foamPatches.size(); }
    
    public List<WaveComponent> getWaveComponents() {
        return new ArrayList<>(waveComponents);
    }
    
    public List<SwellComponent> getSwellComponents() {
        return new ArrayList<>(swellComponents);
    }
}