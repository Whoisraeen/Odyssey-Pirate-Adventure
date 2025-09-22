package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ancient Ruins structure - mysterious remnants of lost civilizations
 */
public class AncientRuins extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final String civilization;
    private final int age; // Age in years
    private final boolean hasInscriptions;
    private final boolean isStable;
    
    public AncientRuins(float x, float y, float z) {
        super(StructureType.ANCIENT_RUINS, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.civilization = generateCivilizationName();
        this.age = random.nextInt(2000) + 500; // 500-2500 years old
        this.hasInscriptions = random.nextFloat() < 0.7f; // 70% chance of inscriptions
        this.isStable = random.nextFloat() < 0.6f; // 60% chance of being stable
        initializeLoot();
    }
    
    public AncientRuins(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private String generateCivilizationName() {
        String[] civilizations = {"Atlantean", "Lemurian", "Mu", "Hyperborean", "Thulean", "Avalonian", "Shambhalan"};
        return civilizations[random.nextInt(civilizations.length)];
    }
    
    private void initializeLoot() {
        // Ancient artifacts
        availableLoot.add("Stone Tablet");
        availableLoot.add("Ancient Pottery");
        availableLoot.add("Carved Figurine");
        availableLoot.add("Weathered Tools");
        
        // Valuable archaeological finds
        if (random.nextFloat() < 0.5f) {
            availableLoot.add("Ancient Coins");
        }
        if (random.nextFloat() < 0.4f) {
            availableLoot.add("Precious Metals");
        }
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Rare Crystals");
        }
        
        // Special items based on civilization
        if (hasInscriptions) {
            availableLoot.add("Inscribed Stone");
            availableLoot.add("Ancient Map");
        }
        
        // Rare finds
        if (random.nextFloat() < 0.2f) {
            availableLoot.add("Lost Technology");
        }
        if (random.nextFloat() < 0.15f) {
            availableLoot.add("Ancient Weapon");
        }
        if (random.nextFloat() < 0.1f) {
            availableLoot.add("Civilization Key");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered ancient ruins from the " + civilization + " civilization!");
        }
        
        System.out.println("You step into the crumbling remains of an ancient structure.");
        System.out.println("These ruins are approximately " + age + " years old.");
        
        if (!isStable) {
            System.out.println("Warning: The structure appears unstable. Proceed with caution!");
        }
        
        if (hasInscriptions) {
            System.out.println("Strange inscriptions cover the weathered walls.");
        }
    }
    
    @Override
    public void onExit() {
        System.out.println("You carefully leave the ancient ruins, mindful of their fragile state.");
        
        if (!isStable && random.nextFloat() < 0.1f) {
            System.out.println("Some stones shift and fall as you exit. The ruins grow more unstable.");
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
            // Unstable ruins may collapse when disturbed
            if (!isStable && random.nextFloat() < 0.2f) {
                System.out.println("The structure groans ominously as you disturb the " + lootType + "!");
            }
            
            lootedItems.add(lootType);
            
            // Special messages for certain items
            switch (lootType) {
                case "Stone Tablet":
                    System.out.println("The stone tablet bears ancient " + civilization + " markings.");
                    break;
                case "Lost Technology":
                    System.out.println("This device is far more advanced than anything from its time period!");
                    break;
                case "Ancient Weapon":
                    System.out.println("This weapon shows remarkable craftsmanship from the " + civilization + " era.");
                    break;
                case "Civilization Key":
                    System.out.println("This ornate key might unlock other " + civilization + " sites!");
                    break;
                case "Inscribed Stone":
                    System.out.println("The inscriptions might reveal secrets of the " + civilization + " people.");
                    break;
                case "Ancient Map":
                    System.out.println("This map shows locations that no longer exist on modern charts.");
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
     * Get the civilization that built these ruins
     */
    public String getCivilization() {
        return civilization;
    }
    
    /**
     * Get the age of the ruins in years
     */
    public int getAge() {
        return age;
    }
    
    /**
     * Check if the ruins have readable inscriptions
     */
    public boolean hasInscriptions() {
        return hasInscriptions;
    }
    
    /**
     * Check if the ruins are structurally stable
     */
    public boolean isStable() {
        return isStable;
    }
    
    /**
     * Attempt to decipher inscriptions (if present)
     */
    public String decipherInscriptions() {
        if (!hasInscriptions) {
            return "There are no inscriptions to decipher here.";
        }
        
        String[] messages = {
            "The inscriptions speak of a great flood that destroyed their homeland.",
            "Ancient warnings about sea monsters in the deep waters.",
            "References to a hidden vault containing their greatest treasures.",
            "Astronomical calculations showing the positions of stars long ago.",
            "A prophecy about the return of their lost king.",
            "Instructions for a ritual to calm the angry seas.",
            "The location of their sacred burial grounds.",
            "Trade routes to distant lands across the ocean."
        };
        
        return "You decipher the ancient text: \"" + messages[random.nextInt(messages.length)] + "\"";
    }
    
    /**
     * Get the archaeological value of these ruins
     */
    public int getArchaeologicalValue() {
        int value = age / 10; // Base value from age
        
        if (hasInscriptions) value += 100;
        if (civilization.equals("Atlantean")) value += 200; // Atlantean ruins are most valuable
        if (!lootedItems.isEmpty()) value += lootedItems.size() * 50; // Discovered artifacts add value
        
        return value;
    }
    
    /**
     * Get the historical period of these ruins
     */
    public String getHistoricalPeriod() {
        if (age > 2000) return "Ancient Era";
        if (age > 1500) return "Classical Period";
        if (age > 1000) return "Medieval Period";
        return "Recent Historical Period";
    }
}