package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import org.joml.Vector3f;
import java.util.*;

/**
 * Coral reef feature representing underwater coral formations.
 * Reefs provide marine resources, shelter for sea life, and navigation hazards.
 */
public class CoralReef extends Feature {
    
    public enum ReefType {
        SHALLOW_REEF("Shallow Coral Reef", "A vibrant reef in shallow waters", 0.8f, 2.0f, true),
        DEEP_REEF("Deep Coral Reef", "A mysterious reef in deeper waters", 1.2f, 8.0f, false),
        BARRIER_REEF("Barrier Reef", "A large protective reef formation", 1.5f, 15.0f, true),
        ATOLL_REEF("Atoll Reef", "A circular reef surrounding a lagoon", 1.3f, 12.0f, true),
        BRAIN_CORAL("Brain Coral Formation", "A unique brain-shaped coral structure", 1.0f, 5.0f, false),
        STAGHORN_REEF("Staghorn Coral Reef", "A reef dominated by staghorn corals", 1.1f, 6.0f, true),
        DEAD_REEF("Dead Coral Reef", "A bleached and dying reef system", 0.3f, 3.0f, false);
        
        private final String displayName;
        private final String description;
        private final float biodiversityMultiplier;
        private final float size; // Radius in blocks
        private final boolean isHealthy;
        
        ReefType(String displayName, String description, float biodiversityMultiplier, float size, boolean isHealthy) {
            this.displayName = displayName;
            this.description = description;
            this.biodiversityMultiplier = biodiversityMultiplier;
            this.size = size;
            this.isHealthy = isHealthy;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public float getBiodiversityMultiplier() { return biodiversityMultiplier; }
        public float getSize() { return size; }
        public boolean isHealthy() { return isHealthy; }
    }
    
    public enum MarineLife {
        TROPICAL_FISH("Tropical Fish", 5, 0.7f),
        ANGELFISH("Angelfish", 8, 0.5f),
        PARROTFISH("Parrotfish", 12, 0.4f),
        CLOWNFISH("Clownfish", 6, 0.6f),
        SEA_TURTLE("Sea Turtle", 25, 0.2f),
        OCTOPUS("Octopus", 20, 0.3f),
        LOBSTER("Lobster", 15, 0.35f),
        CRAB("Crab", 8, 0.5f),
        SEA_URCHIN("Sea Urchin", 3, 0.8f),
        STARFISH("Starfish", 4, 0.7f),
        SHARK("Reef Shark", 50, 0.1f),
        MANTA_RAY("Manta Ray", 40, 0.15f);
        
        private final String displayName;
        private final int value;
        private final float spawnChance;
        
        MarineLife(String displayName, int value, float spawnChance) {
            this.displayName = displayName;
            this.value = value;
            this.spawnChance = spawnChance;
        }
        
        public String getDisplayName() { return displayName; }
        public int getValue() { return value; }
        public float getSpawnChance() { return spawnChance; }
    }
    
    private final ReefType reefType;
    private final float depth;
    private final int healthLevel; // 0-100
    private final Map<String, Integer> resources;
    private final Set<MarineLife> marineLife;
    private final boolean isNavigationHazard;
    private final float visibility; // Underwater visibility in blocks
    private long lastRegenerationTime;
    
    public CoralReef(float x, float y, float z, ReefType reefType, float depth) {
        super(FeatureType.CORAL_REEF, x, y, z);
        this.reefType = reefType;
        this.depth = Math.max(1.0f, depth);
        this.resources = new HashMap<>();
        this.marineLife = new HashSet<>();
        this.lastRegenerationTime = System.currentTimeMillis();
        
        Random random = new Random((long)(x * 1000 + z * 1000));
        
        // Calculate health based on reef type and depth
        if (reefType.isHealthy) {
            this.healthLevel = 70 + random.nextInt(31); // 70-100
        } else {
            this.healthLevel = 10 + random.nextInt(41); // 10-50
        }
        
        // Shallow reefs are more likely to be navigation hazards
        this.isNavigationHazard = depth < 5.0f && reefType.size > 8.0f;
        
        // Calculate visibility based on depth and health
        this.visibility = Math.max(3.0f, 15.0f - (depth * 0.5f) + (healthLevel * 0.1f));
        
        generateMarineLife();
        generateResources();
    }
    
    public CoralReef(Vector3f position, ReefType reefType, float depth) {
        this(position.x, position.y, position.z, reefType, depth);
    }
    
    /**
     * Generate marine life based on reef type and health
     */
    private void generateMarineLife() {
        Random random = new Random((long)(position.x * 1000 + position.z * 1000));
        float biodiversityFactor = reefType.biodiversityMultiplier * (healthLevel / 100.0f);
        
        for (MarineLife life : MarineLife.values()) {
            float adjustedChance = life.spawnChance * biodiversityFactor;
            if (random.nextFloat() < adjustedChance) {
                marineLife.add(life);
            }
        }
        
        // Ensure at least some basic marine life exists
        if (marineLife.isEmpty() && healthLevel > 20) {
            marineLife.add(MarineLife.TROPICAL_FISH);
            marineLife.add(MarineLife.CRAB);
        }
    }
    
    /**
     * Generate resources available from the reef
     */
    private void generateResources() {
        Random random = new Random((long)(position.x * 1000 + position.z * 1000));
        
        // Coral resources
        if (healthLevel > 50) {
            resources.put("LIVE_CORAL", 10 + random.nextInt(20));
            resources.put("CORAL_POLYPS", 5 + random.nextInt(15));
        }
        
        resources.put("DEAD_CORAL", 5 + random.nextInt(10));
        resources.put("CALCIUM_CARBONATE", 15 + random.nextInt(25));
        
        // Marine resources based on life present
        for (MarineLife life : marineLife) {
            int quantity = 1 + random.nextInt(3);
            resources.put(life.name(), quantity);
        }
        
        // Special resources for healthy reefs
        if (healthLevel > 80) {
            if (random.nextFloat() < 0.3f) {
                resources.put("PEARL", 1 + random.nextInt(3));
            }
            if (random.nextFloat() < 0.2f) {
                resources.put("RARE_SHELL", 1 + random.nextInt(2));
            }
        }
        
        // Deep reef special resources
        if (depth > 10.0f && random.nextFloat() < 0.15f) {
            resources.put("DEEP_SEA_TREASURE", 1);
        }
        
        // Navigation hazard resources
        if (isNavigationHazard && random.nextFloat() < 0.4f) {
            resources.put("SHIPWRECK_DEBRIS", 3 + random.nextInt(7));
        }
    }
    
    /**
     * Regenerate reef resources over time
     */
    private void regenerateResources() {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastRegenerationTime;
        
        // Regenerate every 5 minutes for healthy reefs
        if (timePassed > 300000 && healthLevel > 60) {
            Random random = new Random(currentTime);
            
            // Slowly regenerate coral resources
            if (random.nextFloat() < 0.3f) {
                Integer liveCoralCount = resources.get("LIVE_CORAL");
                if (liveCoralCount != null && liveCoralCount < 30) {
                    resources.put("LIVE_CORAL", liveCoralCount + 1);
                }
            }
            
            // Marine life can repopulate
            for (MarineLife life : marineLife) {
                if (random.nextFloat() < 0.2f) {
                    Integer count = resources.get(life.name());
                    if (count != null && count < 5) {
                        resources.put(life.name(), count + 1);
                    }
                }
            }
            
            lastRegenerationTime = currentTime;
        }
    }
    
    @Override
    public void onInteract() {
        regenerateResources();
        
        if (!discovered) {
            onDiscover();
            System.out.println("You discovered a " + reefType.displayName + "!");
            System.out.println(reefType.description);
        } else {
            System.out.println("You approach the familiar coral reef.");
        }
        
        System.out.println("Depth: " + String.format("%.1f", depth) + " meters");
        System.out.println("Health: " + healthLevel + "%");
        System.out.println("Visibility: " + String.format("%.1f", visibility) + " blocks");
        System.out.println("Marine life species: " + marineLife.size());
        
        if (isNavigationHazard) {
            System.out.println("⚠ Warning: This reef poses a navigation hazard to ships!");
        }
    }
    
    @Override
    public String[] getAvailableResources() {
        regenerateResources();
        return resources.keySet().toArray(new String[0]);
    }
    
    @Override
    public boolean extractResource(String resourceType, int amount) {
        regenerateResources();
        
        Integer available = resources.get(resourceType);
        if (available != null && available >= amount) {
            resources.put(resourceType, available - amount);
            
            // Extracting live coral damages the reef
            if ("LIVE_CORAL".equals(resourceType)) {
                // Reduce health slightly when harvesting live coral
                // This is handled elsewhere in the game logic
            }
            
            return true;
        }
        return false;
    }
    
    @Override
    public boolean requiresSpecialAccess() {
        return true; // Always requires diving equipment
    }
    
    @Override
    public int getDangerLevel() {
        int danger = 0;
        
        // Deep reefs are more dangerous
        if (depth > 15.0f) danger += 2;
        else if (depth > 8.0f) danger += 1;
        
        // Poor visibility increases danger
        if (visibility < 5.0f) danger += 1;
        
        // Sharks increase danger
        if (marineLife.contains(MarineLife.SHARK)) danger += 2;
        
        // Navigation hazards are dangerous for ships
        if (isNavigationHazard) danger += 1;
        
        return Math.min(5, danger);
    }
    
    /**
     * Check if the reef is suitable for diving
     */
    public boolean isSuitableForDiving() {
        return visibility > 3.0f && healthLevel > 30;
    }
    
    /**
     * Get the biodiversity score of this reef
     */
    public int getBiodiversityScore() {
        return (int)(marineLife.size() * reefType.biodiversityMultiplier * (healthLevel / 100.0f));
    }
    
    /**
     * Check if this reef blocks ship navigation
     */
    public boolean blocksNavigation() {
        return isNavigationHazard && depth < 3.0f;
    }
    
    /**
     * Get the total value of marine life in this reef
     */
    public int getMarineLifeValue() {
        return marineLife.stream().mapToInt(MarineLife::getValue).sum();
    }
    
    // Getters
    public ReefType getReefType() { return reefType; }
    public float getDepth() { return depth; }
    public int getHealthLevel() { return healthLevel; }
    public Set<MarineLife> getMarineLife() { return new HashSet<>(marineLife); }
    public boolean isNavigationHazard() { return isNavigationHazard; }
    public float getVisibility() { return visibility; }
    public float getReefSize() { return reefType.size; }
    
    @Override
    public String toString() {
        return String.format("%s at (%.1f, %.1f, %.1f) - Depth: %.1fm, Health: %d%%, Species: %d", 
                           reefType.displayName, position.x, position.y, position.z, 
                           depth, healthLevel, marineLife.size());
    }
}