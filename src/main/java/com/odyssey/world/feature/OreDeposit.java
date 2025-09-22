package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import com.odyssey.world.OreType;
import org.joml.Vector3f;
import java.util.*;

/**
 * Ore deposit feature representing mineable ore veins in the world.
 * Deposits contain various types of ores with different quantities and qualities.
 */
public class OreDeposit extends Feature {
    
    public enum DepositType {
        SURFACE_OUTCROP("Surface Outcrop", "Ore visible on the surface", 0.8f, 1.0f, true),
        SHALLOW_VEIN("Shallow Vein", "A vein close to the surface", 1.0f, 1.2f, true),
        DEEP_VEIN("Deep Vein", "A rich vein deep underground", 1.5f, 2.0f, false),
        MOTHER_LODE("Mother Lode", "A massive ore deposit", 2.0f, 3.0f, false),
        PLACER_DEPOSIT("Placer Deposit", "Ore concentrated by water flow", 0.9f, 1.1f, true),
        VOLCANIC_DEPOSIT("Volcanic Deposit", "Ore formed by volcanic activity", 1.3f, 1.8f, false),
        METAMORPHIC_VEIN("Metamorphic Vein", "Ore transformed by heat and pressure", 1.2f, 1.6f, false);
        
        private final String displayName;
        private final String description;
        private final float richness; // Ore quantity multiplier
        private final float difficulty; // Mining difficulty multiplier
        private final boolean isAccessible; // Can be mined without special equipment
        
        DepositType(String displayName, String description, float richness, float difficulty, boolean isAccessible) {
            this.displayName = displayName;
            this.description = description;
            this.richness = richness;
            this.difficulty = difficulty;
            this.isAccessible = isAccessible;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public float getRichness() { return richness; }
        public float getDifficulty() { return difficulty; }
        public boolean isAccessible() { return isAccessible; }
    }
    
    private final DepositType depositType;
    private final OreType primaryOre;
    private final Map<OreType, Integer> oreQuantities;
    private final Map<OreType, Float> oreQualities; // 0.0 to 1.0
    private final int totalOreAmount;
    private final float miningDifficulty;
    private final boolean requiresSpecialTools;
    private int currentOreAmount;
    private boolean isDepleted;
    private long lastRegenerationTime;
    
    public OreDeposit(float x, float y, float z, DepositType depositType, OreType primaryOre) {
        super(FeatureType.ORE_DEPOSIT, x, y, z);
        this.depositType = depositType;
        this.primaryOre = primaryOre;
        this.oreQuantities = new HashMap<>();
        this.oreQualities = new HashMap<>();
        this.lastRegenerationTime = System.currentTimeMillis();
        
        Random random = new Random((long)(x * 1000 + z * 1000));
        
        // Calculate mining difficulty
        this.miningDifficulty = depositType.difficulty * primaryOre.getHardness();
        this.requiresSpecialTools = miningDifficulty > 5.0f || !depositType.isAccessible;
        
        // Generate ore quantities
        generateOreDeposit(random);
        
        this.totalOreAmount = oreQuantities.values().stream().mapToInt(Integer::intValue).sum();
        this.currentOreAmount = totalOreAmount;
        this.isDepleted = false;
    }
    
    public OreDeposit(Vector3f position, DepositType depositType, OreType primaryOre) {
        this(position.x, position.y, position.z, depositType, primaryOre);
    }
    
    /**
     * Generate the ore deposit with primary and secondary ores
     */
    private void generateOreDeposit(Random random) {
        // Primary ore (60-80% of deposit)
        float primaryPercentage = 0.6f + random.nextFloat() * 0.2f;
        int primaryAmount = (int)(50 * depositType.richness * primaryPercentage);
        oreQuantities.put(primaryOre, primaryAmount);
        
        // Primary ore quality
        float primaryQuality = 0.7f + random.nextFloat() * 0.3f;
        oreQualities.put(primaryOre, primaryQuality);
        
        // Add associated ores
        OreType[] associatedOres = primaryOre.getAssociatedOres();
        float remainingPercentage = 1.0f - primaryPercentage;
        
        for (OreType associatedOre : associatedOres) {
            if (random.nextFloat() < 0.4f && remainingPercentage > 0.1f) {
                float orePercentage = Math.min(remainingPercentage * 0.5f, 0.15f + random.nextFloat() * 0.1f);
                int oreAmount = (int)(50 * depositType.richness * orePercentage);
                
                if (oreAmount > 0) {
                    oreQuantities.put(associatedOre, oreAmount);
                    
                    // Associated ores typically have lower quality
                    float associatedQuality = 0.5f + random.nextFloat() * 0.4f;
                    oreQualities.put(associatedOre, associatedQuality);
                    
                    remainingPercentage -= orePercentage;
                }
            }
        }
        
        // Add trace amounts of rare ores for deep deposits
        if (depositType == DepositType.DEEP_VEIN || depositType == DepositType.MOTHER_LODE) {
            for (OreType ore : OreType.values()) {
                if (ore.isRare() && !oreQuantities.containsKey(ore) && random.nextFloat() < 0.1f) {
                    int traceAmount = 1 + random.nextInt(3);
                    oreQuantities.put(ore, traceAmount);
                    oreQualities.put(ore, 0.8f + random.nextFloat() * 0.2f);
                }
            }
        }
        
        // Ensure minimum ore amount
        if (oreQuantities.isEmpty()) {
            oreQuantities.put(primaryOre, (int)(10 * depositType.richness));
            oreQualities.put(primaryOre, 0.6f);
        }
    }
    
    /**
     * Slowly regenerate ore over very long periods (for gameplay balance)
     */
    private void regenerateOre() {
        long currentTime = System.currentTimeMillis();
        long timePassed = currentTime - lastRegenerationTime;
        
        // Regenerate very slowly (every 30 minutes) and only small amounts
        if (timePassed > 1800000 && !isDepleted && currentOreAmount < totalOreAmount * 0.3f) {
            Random random = new Random(currentTime);
            
            // Small chance to regenerate 1-2 ore
            if (random.nextFloat() < 0.2f) {
                OreType oreToRegenerate = getRandomOreType(random);
                Integer currentAmount = oreQuantities.get(oreToRegenerate);
                if (currentAmount != null && currentAmount < totalOreAmount * 0.1f) {
                    oreQuantities.put(oreToRegenerate, currentAmount + 1);
                    currentOreAmount++;
                }
            }
            
            lastRegenerationTime = currentTime;
        }
    }
    
    /**
     * Get a random ore type from this deposit
     */
    private OreType getRandomOreType(Random random) {
        List<OreType> availableOres = new ArrayList<>(oreQuantities.keySet());
        return availableOres.get(random.nextInt(availableOres.size()));
    }
    
    @Override
    public void onInteract() {
        regenerateOre();
        
        if (!discovered) {
            onDiscover();
            System.out.println("You discovered a " + depositType.displayName + "!");
            System.out.println(depositType.description);
            System.out.println("Primary ore: " + primaryOre.getDisplayName());
        } else {
            System.out.println("You examine the familiar ore deposit.");
        }
        
        System.out.println("Deposit type: " + depositType.displayName);
        System.out.println("Mining difficulty: " + String.format("%.1f", miningDifficulty));
        System.out.println("Remaining ore: " + currentOreAmount + "/" + totalOreAmount);
        
        if (isDepleted) {
            System.out.println("This deposit appears to be depleted.");
        } else if (currentOreAmount < totalOreAmount * 0.2f) {
            System.out.println("This deposit is nearly exhausted.");
        }
        
        if (requiresSpecialTools) {
            System.out.println("⚠ Special mining equipment required!");
        }
    }
    
    @Override
    public String[] getAvailableResources() {
        regenerateOre();
        
        List<String> resources = new ArrayList<>();
        for (Map.Entry<OreType, Integer> entry : oreQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                resources.add(entry.getKey().name());
            }
        }
        
        // Add stone as a byproduct of mining
        if (!isDepleted) {
            resources.add("STONE");
        }
        
        return resources.toArray(new String[0]);
    }
    
    @Override
    public boolean extractResource(String resourceType, int amount) {
        regenerateOre();
        
        if ("STONE".equals(resourceType)) {
            return !isDepleted; // Can always get stone while mining
        }
        
        try {
            OreType oreType = OreType.valueOf(resourceType);
            Integer available = oreQuantities.get(oreType);
            
            if (available != null && available >= amount) {
                oreQuantities.put(oreType, available - amount);
                currentOreAmount -= amount;
                
                // Check if deposit is depleted
                if (currentOreAmount <= 0) {
                    isDepleted = true;
                }
                
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Invalid ore type
        }
        
        return false;
    }
    
    @Override
    public boolean requiresSpecialAccess() {
        return requiresSpecialTools;
    }
    
    @Override
    public int getDangerLevel() {
        int danger = 0;
        
        // Deep mining is more dangerous
        if (depositType == DepositType.DEEP_VEIN || depositType == DepositType.MOTHER_LODE) {
            danger += 2;
        }
        
        // Volcanic deposits have additional hazards
        if (depositType == DepositType.VOLCANIC_DEPOSIT) {
            danger += 1;
        }
        
        // Hard ores are more dangerous to mine
        if (miningDifficulty > 8.0f) {
            danger += 1;
        }
        
        return Math.min(4, danger);
    }
    
    /**
     * Calculate mining time based on ore type and deposit difficulty
     */
    public float calculateMiningTime(OreType oreType) {
        Float quality = oreQualities.get(oreType);
        if (quality == null) return Float.MAX_VALUE; // Ore not available
        
        float baseTime = oreType.getMiningTimeMultiplier();
        float difficultyMultiplier = depositType.difficulty;
        float qualityMultiplier = 2.0f - quality; // Higher quality = faster mining
        
        return baseTime * difficultyMultiplier * qualityMultiplier;
    }
    
    /**
     * Get the quality of a specific ore in this deposit
     */
    public float getOreQuality(OreType oreType) {
        return oreQualities.getOrDefault(oreType, 0.0f);
    }
    
    /**
     * Get the quantity of a specific ore in this deposit
     */
    public int getOreQuantity(OreType oreType) {
        return oreQuantities.getOrDefault(oreType, 0);
    }
    
    /**
     * Get all ore types present in this deposit
     */
    public Set<OreType> getAvailableOreTypes() {
        return oreQuantities.entrySet().stream()
                           .filter(entry -> entry.getValue() > 0)
                           .map(Map.Entry::getKey)
                           .collect(java.util.stream.Collectors.toSet());
    }
    
    /**
     * Get the estimated total value of this deposit
     */
    public int getEstimatedValue() {
        int totalValue = 0;
        for (Map.Entry<OreType, Integer> entry : oreQuantities.entrySet()) {
            OreType ore = entry.getKey();
            int quantity = entry.getValue();
            float quality = oreQualities.get(ore);
            
            totalValue += (int)(ore.getBaseValue() * quantity * quality);
        }
        return totalValue;
    }
    
    /**
     * Check if this deposit is worth mining
     */
    public boolean isWorthMining() {
        return !isDepleted && currentOreAmount > 5 && getEstimatedValue() > 50;
    }
    
    // Getters
    public DepositType getDepositType() { return depositType; }
    public OreType getPrimaryOre() { return primaryOre; }
    public int getTotalOreAmount() { return totalOreAmount; }
    public int getCurrentOreAmount() { return currentOreAmount; }
    public float getMiningDifficulty() { return miningDifficulty; }
    public boolean requiresSpecialTools() { return requiresSpecialTools; }
    public boolean isDepleted() { return isDepleted; }
    
    @Override
    public String toString() {
        String status = isDepleted ? "Depleted" : 
                       (currentOreAmount < totalOreAmount * 0.2f ? "Nearly Exhausted" : "Active");
        
        return String.format("%s (%s) at (%.1f, %.1f, %.1f) - %s ore, %s (%d/%d remaining)", 
                           depositType.displayName, primaryOre.getDisplayName(),
                           position.x, position.y, position.z, primaryOre.getDisplayName(),
                           status, currentOreAmount, totalOreAmount);
    }
}