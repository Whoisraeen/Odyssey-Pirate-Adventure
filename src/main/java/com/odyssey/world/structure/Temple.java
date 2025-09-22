package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Temple structure - ancient religious site with artifacts and mysteries
 */
public class Temple extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final String deity;
    private final boolean isBlessed;
    private final boolean isCursed;
    
    public Temple(float x, float y, float z) {
        super(StructureType.TEMPLE, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.deity = generateDeityName();
        this.isBlessed = random.nextFloat() < 0.4f; // 40% chance of blessing
        this.isCursed = !isBlessed && random.nextFloat() < 0.3f; // 30% chance of curse if not blessed
        initializeLoot();
    }
    
    public Temple(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private String generateDeityName() {
        String[] deities = {"Poseidon", "Neptune", "Tiamat", "Calypso", "Triton", "Amphitrite", "Oceanus"};
        return deities[random.nextInt(deities.length)];
    }
    
    private void initializeLoot() {
        // Religious artifacts
        availableLoot.add("Sacred Chalice");
        availableLoot.add("Ancient Scroll");
        availableLoot.add("Religious Icon");
        availableLoot.add("Ceremonial Dagger");
        availableLoot.add("Holy Water");
        availableLoot.add("Prayer Beads");
        
        // Valuable items
        if (random.nextFloat() < 0.6f) {
            availableLoot.add("Golden Idol");
        }
        if (random.nextFloat() < 0.4f) {
            availableLoot.add("Precious Gems");
        }
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Ancient Tome");
        }
        
        // Blessed/Cursed items
        if (isBlessed) {
            availableLoot.add("Blessed Amulet");
            availableLoot.add("Divine Blessing");
        }
        if (isCursed) {
            availableLoot.add("Cursed Artifact");
            availableLoot.add("Dark Relic");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered an ancient temple dedicated to " + deity + "!");
        }
        
        System.out.println("You enter the sacred halls of the temple. The air feels heavy with ancient power.");
        
        if (isBlessed) {
            System.out.println("A warm, peaceful energy surrounds you. This place is blessed.");
        } else if (isCursed) {
            System.out.println("An ominous chill runs down your spine. Dark forces linger here.");
        }
    }
    
    @Override
    public void onExit() {
        if (isBlessed) {
            System.out.println("You leave the temple feeling refreshed and protected.");
        } else if (isCursed) {
            System.out.println("You hurriedly exit the temple, glad to be away from its dark influence.");
        } else {
            System.out.println("You respectfully leave the ancient temple.");
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
            // Cursed temples may have consequences for looting
            if (isCursed && random.nextFloat() < 0.4f) {
                System.out.println("As you take the " + lootType + ", you feel a dark curse settle upon you!");
            }
            
            // Blessed temples may provide benefits
            if (isBlessed && random.nextFloat() < 0.3f) {
                System.out.println("The " + lootType + " glows with divine light as you take it.");
            }
            
            lootedItems.add(lootType);
            
            // Special messages for certain items
            switch (lootType) {
                case "Golden Idol":
                    System.out.println("The golden idol of " + deity + " is incredibly valuable!");
                    break;
                case "Ancient Tome":
                    System.out.println("This ancient tome contains forgotten knowledge and rituals.");
                    break;
                case "Blessed Amulet":
                    System.out.println("The blessed amulet will protect you from harm.");
                    break;
                case "Cursed Artifact":
                    System.out.println("The cursed artifact pulses with malevolent energy.");
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
     * Get the deity this temple is dedicated to
     */
    public String getDeity() {
        return deity;
    }
    
    /**
     * Check if the temple is blessed
     */
    public boolean isBlessed() {
        return isBlessed;
    }
    
    /**
     * Check if the temple is cursed
     */
    public boolean isCursed() {
        return isCursed;
    }
    
    /**
     * Attempt to pray at the temple
     */
    public boolean pray() {
        if (isBlessed) {
            System.out.println("Your prayers are answered! You feel blessed by " + deity + ".");
            return true;
        } else if (isCursed) {
            System.out.println("Your prayers echo unanswered in the cursed halls.");
            return false;
        } else {
            System.out.println("You offer a respectful prayer to " + deity + ".");
            return random.nextFloat() < 0.5f; // 50% chance of minor blessing
        }
    }
    
    /**
     * Get the spiritual power level of this temple
     */
    public float getSpiritualPower() {
        if (isBlessed) return 0.8f;
        if (isCursed) return -0.6f;
        return 0.2f;
    }
}