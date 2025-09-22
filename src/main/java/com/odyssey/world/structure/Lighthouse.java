package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lighthouse structure - coastal navigation aid with utility and occasional loot
 */
public class Lighthouse extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final boolean isOperational;
    private final int height;
    private final float lightRange;
    
    public Lighthouse(float x, float y, float z) {
        super(StructureType.LIGHTHOUSE, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.isOperational = random.nextFloat() < 0.7f; // 70% chance of being operational
        this.height = 15 + random.nextInt(25); // 15-40 meters tall
        this.lightRange = isOperational ? 500.0f + random.nextFloat() * 1000.0f : 0.0f;
        initializeLoot();
    }
    
    public Lighthouse(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private void initializeLoot() {
        // Navigation equipment
        availableLoot.add("Navigation Charts");
        availableLoot.add("Compass");
        availableLoot.add("Spyglass");
        availableLoot.add("Lighthouse Logbook");
        
        // Maintenance supplies
        if (isOperational) {
            availableLoot.add("Lamp Oil");
            availableLoot.add("Lighthouse Lens");
        } else {
            availableLoot.add("Broken Lens Fragments");
            availableLoot.add("Rusty Tools");
        }
        
        // Occasional valuable items
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Keeper's Savings");
        }
        if (random.nextFloat() < 0.2f) {
            availableLoot.add("Rare Navigation Instrument");
        }
        
        // Emergency supplies
        availableLoot.add("Emergency Rations");
        availableLoot.add("Fresh Water");
        availableLoot.add("Blankets");
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered a lighthouse standing tall against the coast!");
        }
        
        if (isOperational) {
            System.out.println("The lighthouse beacon sweeps across the waters, guiding ships to safety.");
        } else {
            System.out.println("This lighthouse appears abandoned, its light long extinguished.");
        }
        
        System.out.println("You climb the spiral staircase to the top of the lighthouse.");
    }
    
    @Override
    public void onExit() {
        System.out.println("You descend from the lighthouse, having enjoyed the panoramic view.");
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
            
            // Special messages for certain items
            switch (lootType) {
                case "Lighthouse Logbook":
                    System.out.println("The logbook contains records of ships that passed by this coast.");
                    break;
                case "Rare Navigation Instrument":
                    System.out.println("This precision instrument will greatly aid your navigation!");
                    break;
                case "Lighthouse Lens":
                    System.out.println("The massive lens could be valuable to collectors or other lighthouses.");
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
     * Check if the lighthouse is operational
     */
    public boolean isOperational() {
        return isOperational;
    }
    
    /**
     * Get the height of the lighthouse in meters
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Get the range of the lighthouse beam in meters
     */
    public float getLightRange() {
        return lightRange;
    }
    
    /**
     * Check if the lighthouse provides navigation aid to ships
     */
    public boolean providesNavigationAid() {
        return isOperational && lightRange > 0;
    }
    
    /**
     * Get the visibility bonus this lighthouse provides to nearby ships
     */
    public float getVisibilityBonus() {
        return isOperational ? 0.3f : 0.0f; // 30% visibility bonus if operational
    }
    
    /**
     * Calculate the safe harbor radius around this lighthouse
     */
    public float getSafeHarborRadius() {
        return isOperational ? lightRange * 0.5f : 50.0f; // Half light range or minimum 50m
    }
    
    /**
     * Attempt to repair the lighthouse (if broken)
     */
    public boolean repair() {
        if (isOperational) {
            System.out.println("The lighthouse is already operational.");
            return false;
        }
        
        // Check if player has necessary materials (simplified)
        System.out.println("You would need lamp oil and replacement parts to repair this lighthouse.");
        return false; // In a full game, this would check inventory
    }
}