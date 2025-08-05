package com.odyssey.world.ocean;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced tidal system implementation for The Odyssey.
 * Simulates realistic tidal cycles that affect water levels, reveal/hide areas,
 * and create dynamic exploration opportunities.
 */
public class TidalSystem {
    private static final Logger logger = LoggerFactory.getLogger(TidalSystem.class);
    
    // Tidal cycle properties
    private static final double TIDAL_CYCLE_DURATION = 1200.0; // 20 minutes in seconds
    private static final float MAX_TIDAL_RANGE = 4.5f; // Maximum tide height variation
    private static final double SPRING_TIDE_CYCLE = 14.0 * 24.0 * 60.0; // 14 days in minutes
    
    // Advanced tidal mechanics
    private double tidalTime = 0.0;
    private double lunarCycle = 0.0;
    private float currentTidalOffset = 0.0f;
    private float springTideModifier = 1.0f;
    private TidalPhase currentPhase = TidalPhase.RISING;
    
    // Tidal pools and revealed areas
    private final Map<Vector2f, TidalPool> tidalPools = new HashMap<>();
    private final Set<Vector2f> revealedAreas = new HashSet<>();
    private final Random random = new Random();
    
    // Tidal power generation
    private float tidalPowerGenerated = 0.0f;
    private final List<Vector3f> tidalGenerators = new ArrayList<>();
    
    public enum TidalPhase {
        RISING, HIGH_TIDE, FALLING, LOW_TIDE
    }
    
    public static class TidalPool {
        public final Vector2f position;
        public final float baseDepth;
        public final List<String> resources;
        public final List<String> creatures;
        public boolean isRevealed;
        
        public TidalPool(Vector2f position, float baseDepth) {
            this.position = new Vector2f(position);
            this.baseDepth = baseDepth;
            this.resources = new ArrayList<>();
            this.creatures = new ArrayList<>();
            this.isRevealed = false;
        }
    }
    
    public void initialize() {
        logger.info("Initializing advanced tidal system with cycle duration: {} seconds", TIDAL_CYCLE_DURATION);
        
        // Generate tidal pools
        generateTidalPools(100); // Generate 100 tidal pools
        
        logger.info("Generated {} tidal pools for dynamic exploration", tidalPools.size());
    }
    
    private void generateTidalPools(int count) {
        for (int i = 0; i < count; i++) {
            // Generate random positions around coastlines
            Vector2f position = new Vector2f(
                random.nextFloat() * 2000 - 1000, // -1000 to 1000
                random.nextFloat() * 2000 - 1000
            );
            
            float depth = 0.5f + random.nextFloat() * 2.0f; // 0.5 to 2.5 blocks deep
            TidalPool pool = new TidalPool(position, depth);
            
            // Add resources based on biome (simplified)
            if (random.nextFloat() < 0.3f) pool.resources.add("sea_anemone");
            if (random.nextFloat() < 0.2f) pool.resources.add("rare_shells");
            if (random.nextFloat() < 0.15f) pool.resources.add("tidal_crystals");
            if (random.nextFloat() < 0.1f) pool.resources.add("ancient_coral");
            
            // Add creatures
            if (random.nextFloat() < 0.4f) pool.creatures.add("hermit_crab");
            if (random.nextFloat() < 0.25f) pool.creatures.add("sea_urchin");
            if (random.nextFloat() < 0.15f) pool.creatures.add("juvenile_octopus");
            if (random.nextFloat() < 0.05f) pool.creatures.add("rare_starfish");
            
            tidalPools.put(position, pool);
        }
    }
    
    public void update(double deltaTime) {
        tidalTime += deltaTime;
        lunarCycle += deltaTime;
        
        // Calculate spring tide modifier (varies over lunar cycle)
        double lunarProgress = (lunarCycle % SPRING_TIDE_CYCLE) / SPRING_TIDE_CYCLE;
        springTideModifier = 0.7f + 0.6f * (float)Math.abs(Math.sin(lunarProgress * 2 * Math.PI));
        
        // Calculate primary tidal offset
        double cycleProgress = (tidalTime % TIDAL_CYCLE_DURATION) / TIDAL_CYCLE_DURATION;
        float primaryTide = (float) Math.sin(cycleProgress * 2 * Math.PI);
        
        // Add secondary harmonic for more realistic tidal patterns
        float secondaryTide = (float) Math.sin(cycleProgress * 4 * Math.PI) * 0.3f;
        
        currentTidalOffset = (primaryTide + secondaryTide) * MAX_TIDAL_RANGE * springTideModifier;
        
        // Update tidal phase
        updateTidalPhase(cycleProgress);
        
        // Update revealed areas and tidal pools
        updateRevealedAreas();
        
        // Calculate tidal power generation
        updateTidalPower(deltaTime);
    }
    
    private void updateTidalPhase(double cycleProgress) {
        TidalPhase previousPhase = currentPhase;
        
        if (cycleProgress < 0.25) {
            currentPhase = TidalPhase.RISING;
        } else if (cycleProgress < 0.5) {
            currentPhase = TidalPhase.HIGH_TIDE;
        } else if (cycleProgress < 0.75) {
            currentPhase = TidalPhase.FALLING;
        } else {
            currentPhase = TidalPhase.LOW_TIDE;
        }
        
        if (previousPhase != currentPhase) {
            logger.debug("Tidal phase changed to: {}", currentPhase);
        }
    }
    
    private void updateRevealedAreas() {
        revealedAreas.clear();
        
        for (TidalPool pool : tidalPools.values()) {
            // Pool is revealed when tide is low enough
            float effectiveDepth = pool.baseDepth + currentTidalOffset;
            pool.isRevealed = effectiveDepth <= 0.5f; // Revealed when less than 0.5 blocks deep
            
            if (pool.isRevealed) {
                revealedAreas.add(pool.position);
            }
        }
    }
    
    private void updateTidalPower(double deltaTime) {
        // Calculate power based on tidal flow rate
        float tidalVelocity = getTidalVelocity();
        tidalPowerGenerated = 0.0f;
        
        for (Vector3f generator : tidalGenerators) {
            // Each generator produces power based on local tidal velocity
            float localPower = tidalVelocity * tidalVelocity * 0.5f; // Simplified power calculation
            tidalPowerGenerated += localPower;
        }
    }
    
    public float getTidalOffset() {
        return currentTidalOffset;
    }
    
    public float getTidalVelocity() {
        // Rate of change of tidal height
        double cycleProgress = (tidalTime % TIDAL_CYCLE_DURATION) / TIDAL_CYCLE_DURATION;
        return (float) (Math.cos(cycleProgress * 2 * Math.PI) * MAX_TIDAL_RANGE * springTideModifier * (2 * Math.PI / TIDAL_CYCLE_DURATION));
    }
    
    public TidalPhase getCurrentPhase() {
        return currentPhase;
    }
    
    public float getSpringTideModifier() {
        return springTideModifier;
    }
    
    public boolean isHighTide() {
        return currentTidalOffset > MAX_TIDAL_RANGE * springTideModifier * 0.6f;
    }
    
    public boolean isLowTide() {
        return currentTidalOffset < -MAX_TIDAL_RANGE * springTideModifier * 0.6f;
    }
    
    public boolean isSpringTide() {
        return springTideModifier > 1.1f;
    }
    
    public boolean isNeapTide() {
        return springTideModifier < 0.9f;
    }
    
    public Set<Vector2f> getRevealedAreas() {
        return new HashSet<>(revealedAreas);
    }
    
    public Collection<TidalPool> getTidalPools() {
        return tidalPools.values();
    }
    
    public TidalPool getTidalPoolAt(Vector2f position) {
        return tidalPools.get(position);
    }
    
    public void addTidalGenerator(Vector3f position) {
        tidalGenerators.add(new Vector3f(position));
        logger.info("Added tidal generator at position: {}", position);
    }
    
    public float getTidalPowerGenerated() {
        return tidalPowerGenerated;
    }
    
    public double getTimeUntilNextPhase() {
        double cycleProgress = (tidalTime % TIDAL_CYCLE_DURATION) / TIDAL_CYCLE_DURATION;
        double nextPhasePoint = Math.ceil(cycleProgress * 4) / 4.0;
        if (nextPhasePoint == 1.0) nextPhasePoint = 0.0;
        
        double timeToNext = (nextPhasePoint - cycleProgress) * TIDAL_CYCLE_DURATION;
        if (timeToNext <= 0) timeToNext += TIDAL_CYCLE_DURATION / 4.0;
        
        return timeToNext;
    }
    
    public String getTidalStatus() {
        StringBuilder status = new StringBuilder();
        status.append(String.format("Phase: %s, ", currentPhase));
        status.append(String.format("Offset: %.2f, ", currentTidalOffset));
        status.append(String.format("Spring Modifier: %.2f, ", springTideModifier));
        status.append(String.format("Revealed Areas: %d, ", revealedAreas.size()));
        status.append(String.format("Power: %.2f", tidalPowerGenerated));
        return status.toString();
    }
    
    public void cleanup() {
        logger.info("Cleaning up advanced tidal system");
        tidalPools.clear();
        revealedAreas.clear();
        tidalGenerators.clear();
    }
}