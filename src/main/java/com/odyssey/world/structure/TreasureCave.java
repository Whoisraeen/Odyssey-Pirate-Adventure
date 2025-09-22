package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Treasure Cave structure - rare cave containing valuable treasure and puzzles
 */
public class TreasureCave extends Structure {
    
    private final List<String> availableLoot;
    private final List<String> lootedItems;
    private final Random random;
    private final boolean hasTraps;
    private final boolean hasPuzzle;
    private boolean puzzleSolved;
    
    public TreasureCave(float x, float y, float z) {
        super(StructureType.TREASURE_CAVE, x, y, z);
        this.availableLoot = new ArrayList<>();
        this.lootedItems = new ArrayList<>();
        this.random = new Random();
        this.hasTraps = random.nextFloat() < 0.7f; // 70% chance of traps
        this.hasPuzzle = random.nextFloat() < 0.8f; // 80% chance of puzzle
        this.puzzleSolved = false;
        initializeLoot();
    }
    
    public TreasureCave(Vector3f position) {
        this(position.x, position.y, position.z);
    }
    
    private void initializeLoot() {
        // High-value treasure items
        availableLoot.add("Ancient Treasure Chest");
        availableLoot.add("Golden Idol");
        availableLoot.add("Pirate King's Crown");
        availableLoot.add("Legendary Cutlass");
        availableLoot.add("Bag of Doubloons");
        availableLoot.add("Ruby Necklace");
        availableLoot.add("Emerald Ring");
        availableLoot.add("Ancient Map");
        availableLoot.add("Mystical Compass");
        
        // Rare artifacts
        if (random.nextFloat() < 0.5f) {
            availableLoot.add("Cursed Medallion");
        }
        if (random.nextFloat() < 0.3f) {
            availableLoot.add("Crystal Skull");
        }
        if (random.nextFloat() < 0.4f) {
            availableLoot.add("Ancient Scroll");
        }
    }
    
    @Override
    public void onEnter() {
        if (!discovered) {
            onDiscover();
            System.out.println("You've discovered a legendary treasure cave!");
        }
        
        System.out.println("The cave glimmers with the promise of untold riches...");
        
        if (hasTraps && !explored) {
            System.out.println("Warning: You sense danger ahead. This cave may be trapped!");
        }
        
        if (hasPuzzle && !puzzleSolved) {
            System.out.println("Ancient symbols cover the walls. A puzzle must be solved to access the treasure.");
        }
    }
    
    @Override
    public void onExit() {
        System.out.println("You leave the treasure cave, your pockets heavier with riches.");
    }
    
    @Override
    public String[] getAvailableLoot() {
        // If there's a puzzle and it's not solved, no loot is available
        if (hasPuzzle && !puzzleSolved) {
            return new String[0];
        }
        
        List<String> currentLoot = new ArrayList<>(availableLoot);
        currentLoot.removeAll(lootedItems);
        return currentLoot.toArray(new String[0]);
    }
    
    @Override
    public boolean loot(String lootType) {
        // Check if puzzle needs to be solved first
        if (hasPuzzle && !puzzleSolved) {
            System.out.println("You must solve the ancient puzzle before accessing the treasure!");
            return false;
        }
        
        if (availableLoot.contains(lootType) && !lootedItems.contains(lootType)) {
            // Check for traps when looting
            if (hasTraps && random.nextFloat() < 0.3f) {
                System.out.println("A trap is triggered! You narrowly avoid danger!");
                // Still allow looting but with risk
            }
            
            lootedItems.add(lootType);
            
            // Special messages for legendary items
            switch (lootType) {
                case "Ancient Treasure Chest":
                    System.out.println("You've found the legendary treasure chest of Captain Blackbeard!");
                    break;
                case "Pirate King's Crown":
                    System.out.println("The crown of the Pirate King! You are now royalty of the seas!");
                    break;
                case "Crystal Skull":
                    System.out.println("The mystical crystal skull pulses with ancient power!");
                    break;
                case "Cursed Medallion":
                    System.out.println("The cursed medallion feels cold to the touch... perhaps it's best left alone.");
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
     * Attempt to solve the cave's puzzle
     */
    public boolean solvePuzzle(String solution) {
        if (!hasPuzzle) {
            return true; // No puzzle to solve
        }
        
        // Simple puzzle system - in a real game this would be more complex
        String[] correctSolutions = {"TREASURE", "PIRATE", "GOLD", "ADVENTURE"};
        for (String correct : correctSolutions) {
            if (correct.equalsIgnoreCase(solution)) {
                puzzleSolved = true;
                System.out.println("The ancient mechanism clicks and the treasure chamber opens!");
                return true;
            }
        }
        
        System.out.println("The symbols remain unchanged. That's not the correct solution.");
        return false;
    }
    
    /**
     * Check if the cave has traps
     */
    public boolean hasTraps() {
        return hasTraps;
    }
    
    /**
     * Check if the cave has a puzzle
     */
    public boolean hasPuzzle() {
        return hasPuzzle;
    }
    
    /**
     * Check if the puzzle has been solved
     */
    public boolean isPuzzleSolved() {
        return puzzleSolved || !hasPuzzle;
    }
    
    /**
     * Get the estimated value of all treasure in the cave
     */
    public int getTotalTreasureValue() {
        return availableLoot.size() * 1000; // Each item worth ~1000 gold
    }
}