package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shipwreck structure - sunken vessel with valuable loot and underwater exploration
 */
public class Shipwreck extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final String shipName;
    private final boolean isUnderwater;
    
    public Shipwreck(float x, float y, float z) {
        super(StructureType.SHIPWRECK, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.shipName = generateShipName();
        this.isUnderwater = y < 0; // Below sea level
        initializeLoot();
    }
    
    public Shipwreck(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private String generateShipName() {
        String[] prefixes = {"HMS", "The", "Captain", "Black", "Golden", "Silver"};
        String[] names = {"Revenge", "Pearl", "Maiden", "Storm", "Thunder", "Fortune", "Serpent"};
        return prefixes[random.nextInt(prefixes.length)] + " " + names[random.nextInt(names.length)];
    }
    
    private void initializeLoot() {
        availableLoot.add("Treasure Chest");
        availableLoot.add("Gold Doubloons");
        availableLoot.add("Silver Pieces");
        availableLoot.add("Captain's Log");
        availableLoot.add("Navigation Charts");
        availableLoot.add("Cannon Balls");
        availableLoot.add("Ship's Bell");
        availableLoot.add("Jeweled Sword");
        
        if (isUnderwater) {
            availableLoot.add("Coral-Encrusted Artifacts");
            availableLoot.add("Waterlogged Supplies");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered the wreck of " + shipName + "!");
        }
        
        if (isUnderwater && random.nextFloat() < 0.4f) {
            System.out.println("The underwater currents are strong here. Be careful!");
        }
    }
    
    @Override
    public void onExit() {
        if (isUnderwater) {
            System.out.println("You surface from the depths of the shipwreck.");
        }
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
            
            // Special message for treasure chest
            if ("Treasure Chest".equals(lootType)) {
                System.out.println("You've found the captain's treasure chest!");
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
     * Get the name of the wrecked ship
     */
    public String getShipName() {
        return shipName;
    }
    
    /**
     * Check if this wreck is underwater
     */
    public boolean isUnderwater() {
        return isUnderwater;
    }
    
    /**
     * Get the estimated age of the wreck in years
     */
    public int getAge() {
        return 10 + random.nextInt(200); // 10-210 years old
    }
    
    /**
     * Check if the wreck requires diving equipment
     */
    public boolean requiresDivingGear() {
        return isUnderwater && position.y < -10.0f; // Deep underwater
    }
}