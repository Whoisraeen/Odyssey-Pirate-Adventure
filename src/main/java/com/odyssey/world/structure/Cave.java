package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cave structure - natural underground formation with minerals and potential dangers
 */
public class Cave extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final int depth;
    private final boolean hasWater;
    private final boolean hasBats;
    
    public Cave(float x, float y, float z) {
        super(StructureType.CAVE, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.depth = 5 + random.nextInt(20); // 5-25 meters deep
        this.hasWater = random.nextFloat() < 0.3f; // 30% chance of underground water
        this.hasBats = random.nextFloat() < 0.6f; // 60% chance of bats
        initializeLoot();
    }
    
    public Cave(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private void initializeLoot() {
        // Basic cave resources
        availableLoot.add("Iron Ore");
        availableLoot.add("Stone");
        availableLoot.add("Coal");
        availableLoot.add("Crystals");
        
        // Rare minerals (chance based)
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Gold Ore");
        }
        if (random.nextFloat() < 0.2f) {
            availableLoot.add("Silver Ore");
        }
        if (random.nextFloat() < 0.1f) {
            availableLoot.add("Precious Gems");
        }
        
        // Water-related loot
        if (hasWater) {
            availableLoot.add("Fresh Water");
            availableLoot.add("Cave Pearls");
        }
        
        // Bat-related items
        if (hasBats) {
            availableLoot.add("Bat Guano"); // Fertilizer
        }
        
        // Occasional treasure
        if (random.nextFloat() < 0.15f) {
            availableLoot.add("Hidden Treasure");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered a mysterious cave entrance!");
        }
        
        System.out.println("You enter the dark cave. The air is cool and damp.");
        
        if (hasBats && random.nextFloat() < 0.5f) {
            System.out.println("A colony of bats suddenly takes flight, startling you!");
        }
        
        if (hasWater) {
            System.out.println("You hear the sound of dripping water echoing through the cave.");
        }
    }
    
    @Override
    public void onExit() {
        System.out.println("You emerge from the cave, blinking in the bright sunlight.");
    }
    
    @Override
    public String[] getAvailableLoot() {
        List<String> currentLoot = new ArrayList<>(availableLoot);
        currentLoot.removeAll(lootedItems);
        return currentLoot.toArray(new String[0]);
    }
    
    @Override
    public boolean loot(String lootType) {
        if (availableLoot.contains(lootType) && !lootedItems.contains(lootType)) {
            lootedItems.add(lootType);
            
            // Special messages for certain loot types
            switch (lootType) {
                case "Hidden Treasure":
                    System.out.println("You've found a treasure hidden deep in the cave!");
                    break;
                case "Precious Gems":
                    System.out.println("Beautiful gems glint in the torchlight!");
                    break;
                case "Gold Ore":
                    System.out.println("You've struck gold! Literally!");
                    break;
            }
            
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isLooted() {
        return lootedItems.size() >= availableLoot.size();
    }
    
    /**
     * Get the depth of the cave in meters
     */
    public int getDepth() {
        return depth;
    }
    
    /**
     * Check if the cave has underground water
     */
    public boolean hasWater() {
        return hasWater;
    }
    
    /**
     * Check if the cave has bats
     */
    public boolean hasBats() {
        return hasBats;
    }
    
    /**
     * Check if the cave requires torches or light source
     */
    public boolean requiresLight() {
        return depth > 10; // Deeper caves need light
    }
    
    /**
     * Get the cave's stability rating (0.0 to 1.0)
     */
    public float getStability() {
        return Math.max(0.3f, 1.0f - (depth * 0.02f)); // Deeper caves are less stable
    }
}