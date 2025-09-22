package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Watchtower structure - strategic observation point for spotting ships and threats
 */
public class Watchtower extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final int height; // Height in meters
    private final boolean isOperational;
    private final boolean hasSignalFire;
    private final float viewRange;
    
    public Watchtower(float x, float y, float z) {
        super(StructureType.WATCHTOWER, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.height = random.nextInt(20) + 15; // 15-35 meters tall
        this.isOperational = random.nextFloat() < 0.6f; // 60% chance of being operational
        this.hasSignalFire = isOperational && random.nextFloat() < 0.8f; // 80% of operational towers have signal fires
        this.viewRange = height * 2.5f; // View range based on height
        initializeLoot();
    }
    
    public Watchtower(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private void initializeLoot() {
        // Military equipment
        availableLoot.add("Spyglass");
        availableLoot.add("Signal Horn");
        availableLoot.add("Military Maps");
        
        if (isOperational) {
            availableLoot.add("Fresh Supplies");
            availableLoot.add("Guard Equipment");
            if (hasSignalFire) {
                availableLoot.add("Signal Flares");
                availableLoot.add("Fire Oil");
            }
        } else {
            // Abandoned watchtowers
            availableLoot.add("Rusty Weapons");
            availableLoot.add("Old Logbook");
        }
        
        // Valuable items
        if (random.nextFloat() < 0.4f) {
            availableLoot.add("Naval Charts");
        }
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Gold Coins");
        }
        if (random.nextFloat() < 0.2f) {
            availableLoot.add("Officer's Sword");
        }
        
        // Rare strategic items
        if (random.nextFloat() < 0.15f) {
            availableLoot.add("Coded Messages");
        }
        if (random.nextFloat() < 0.1f) {
            availableLoot.add("Master Key");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered a " + (isOperational ? "well-maintained" : "abandoned") + " watchtower!");
        }
        
        System.out.println("You climb the " + height + " meter tall watchtower.");
        
        if (isOperational) {
            System.out.println("This watchtower is still in use. Guards may return at any time!");
            if (hasSignalFire) {
                System.out.println("A signal fire burns at the top, visible for miles around.");
            }
        } else {
            System.out.println("This watchtower has been abandoned. Vines and weathering mark its walls.");
        }
        
        System.out.println("From this height, you can see for " + (int)viewRange + " meters in all directions!");
    }
    
    @Override
    public void onExit() {
        System.out.println("You carefully climb down from the watchtower.");
        
        if (isOperational && random.nextFloat() < 0.2f) {
            System.out.println("You hear voices approaching - the guards are returning!");
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
            // Operational watchtowers may have consequences for looting
            if (isOperational && random.nextFloat() < 0.3f) {
                System.out.println("Taking the " + lootType + " may alert the guards to your presence!");
            }
            
            lootedItems.add(lootType);
            
            // Special messages for certain items
            switch (lootType) {
                case "Spyglass":
                    System.out.println("This spyglass will help you spot distant ships and landmarks!");
                    break;
                case "Naval Charts":
                    System.out.println("These detailed charts show safe passages and dangerous waters.");
                    break;
                case "Signal Flares":
                    System.out.println("These flares can be used to signal ships or call for help.");
                    break;
                case "Coded Messages":
                    System.out.println("These encrypted messages contain valuable intelligence!");
                    break;
                case "Master Key":
                    System.out.println("This key might open other military installations.");
                    break;
                case "Officer's Sword":
                    System.out.println("A finely crafted sword bearing military insignia.");
                    break;
                case "Old Logbook":
                    System.out.println("The logbook contains records of ships and suspicious activities.");
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
     * Get the height of the watchtower
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Check if the watchtower is operational
     */
    public boolean isOperational() {
        return isOperational;
    }
    
    /**
     * Check if the watchtower has a signal fire
     */
    public boolean hasSignalFire() {
        return hasSignalFire;
    }
    
    /**
     * Get the view range from this watchtower
     */
    public float getViewRange() {
        return viewRange;
    }
    
    /**
     * Use the watchtower to scan the horizon
     */
    public String scanHorizon() {
        String[] sightings = {
            "You spot a merchant ship on the distant horizon.",
            "A pirate vessel lurks near a neighboring island.",
            "Storm clouds are gathering to the east.",
            "You see smoke rising from a distant island.",
            "A naval patrol is making its rounds.",
            "Seabirds circle above what might be a shipwreck.",
            "The waters appear calm and safe for travel.",
            "You notice unusual activity at a nearby port.",
            "A mysterious fog bank approaches from the north.",
            "You spot other watchtowers signaling in the distance."
        };
        
        if (isOperational && hasSignalFire) {
            return "From the operational watchtower: " + sightings[random.nextInt(sightings.length)];
        } else {
            return "Through the weathered windows: " + sightings[random.nextInt(sightings.length)];
        }
    }
    
    /**
     * Light the signal fire (if available)
     */
    public boolean lightSignalFire() {
        if (!hasSignalFire) {
            System.out.println("This watchtower has no signal fire to light.");
            return false;
        }
        
        if (!isOperational) {
            System.out.println("The signal fire mechanism is too damaged to use.");
            return false;
        }
        
        System.out.println("You light the signal fire! It blazes brightly, visible for miles.");
        System.out.println("This may attract both friendly ships and unwanted attention...");
        return true;
    }
    
    /**
     * Get the strategic value of this watchtower
     */
    public int getStrategicValue() {
        int value = height * 2; // Base value from height
        
        if (isOperational) value += 100;
        if (hasSignalFire) value += 50;
        if (viewRange > 75) value += 30; // Exceptional view range
        
        return value;
    }
    
    /**
     * Check if guards are likely to return soon
     */
    public boolean guardsReturning() {
        return isOperational && random.nextFloat() < 0.25f; // 25% chance if operational
    }
}