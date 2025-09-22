package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pirate camp structure - temporary encampment with basic loot and moderate danger
 */
public class PirateCamp extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    
    public PirateCamp(float x, float y, float z) {
        super(StructureType.PIRATE_CAMP, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        initializeLoot();
    }
    
    public PirateCamp(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private void initializeLoot() {
        availableLoot.add("Gold Coins");
        availableLoot.add("Rum Bottles");
        availableLoot.add("Cutlass");
        availableLoot.add("Pirate Map Fragment");
        availableLoot.add("Gunpowder");
        availableLoot.add("Compass");
        availableLoot.add("Spyglass");
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
        }
        // Pirate camps may have guards or traps
        if (random.nextFloat() < 0.3f) {
            // Trigger pirate encounter or trap
            System.out.println("Pirates spotted! Prepare for battle!");
        }
    }
    
    @Override
    public void onExit() {
        // Nothing special happens when leaving a pirate camp
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
            return true;
        }
        return false;
    }
    
    @Override
    public boolean isLooted() {
        return lootedItems.size() >= availableLoot.size();
    }
    
    /**
     * Get the number of pirates that might be encountered
     */
    public int getPirateCount() {
        return 2 + random.nextInt(4); // 2-5 pirates
    }
    
    /**
     * Check if the camp is currently occupied
     */
    public boolean isOccupied() {
        return random.nextFloat() < 0.6f; // 60% chance of being occupied
    }
}