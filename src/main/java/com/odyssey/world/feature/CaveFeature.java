package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import com.odyssey.world.OreType;
import org.joml.Vector3f;
import java.util.*;

/**
 * Cave feature representing underground cave systems that can be explored.
 * Caves may contain ores, treasures, and other valuable resources.
 */
public class CaveFeature extends Feature {
    
    private final int depth;
    private final int roomCount;
    private final List<CaveRoom> rooms;
    private final Set<OreType> availableOres;
    private final Map<String, Integer> resources;
    private boolean hasWater;
    private boolean hasAir;
    private int dangerLevel;
    
    public CaveFeature(float x, float y, float z, int depth, int roomCount) {
        super(FeatureType.CAVE, x, y, z);
        this.depth = Math.max(1, depth);
        this.roomCount = Math.max(1, roomCount);
        this.rooms = new ArrayList<>();
        this.availableOres = new HashSet<>();
        this.resources = new HashMap<>();
        this.hasWater = false;
        this.hasAir = true;
        this.dangerLevel = calculateDangerLevel();
        
        generateCaveSystem();
        populateResources();
    }
    
    public CaveFeature(Vector3f position, int depth, int roomCount) {
        this(position.x, position.y, position.z, depth, roomCount);
    }
    
    /**
     * Generate the cave system with rooms and connections
     */
    private void generateCaveSystem() {
        Random random = new Random((long)(position.x * 1000 + position.z * 1000));
        
        // Create main entrance room
        CaveRoom entrance = new CaveRoom(0, position.x, position.y, position.z, 
                                       CaveRoom.RoomType.ENTRANCE, 3.0f + random.nextFloat() * 2.0f);
        rooms.add(entrance);
        
        // Generate additional rooms
        for (int i = 1; i < roomCount; i++) {
            float roomX = position.x + (random.nextFloat() - 0.5f) * depth * 2;
            float roomY = position.y - random.nextFloat() * depth;
            float roomZ = position.z + (random.nextFloat() - 0.5f) * depth * 2;
            float roomSize = 2.0f + random.nextFloat() * 4.0f;
            
            CaveRoom.RoomType roomType = CaveRoom.RoomType.values()[random.nextInt(CaveRoom.RoomType.values().length)];
            CaveRoom room = new CaveRoom(i, roomX, roomY, roomZ, roomType, roomSize);
            
            // Add special features to some rooms
            if (random.nextFloat() < 0.3f) {
                room.hasWater = true;
                this.hasWater = true;
            }
            if (random.nextFloat() < 0.2f) {
                room.hasOres = true;
            }
            if (random.nextFloat() < 0.1f) {
                room.hasTreasure = true;
            }
            
            rooms.add(room);
        }
        
        // Determine if cave has air pockets (affects diving requirements)
        this.hasAir = depth < 10 || new Random().nextFloat() < 0.7f;
    }
    
    /**
     * Populate the cave with resources based on depth and room types
     */
    private void populateResources() {
        Random random = new Random((long)(position.x * 1000 + position.z * 1000));
        
        // Add ores based on depth
        if (depth >= 5) {
            availableOres.add(OreType.IRON);
            availableOres.add(OreType.COPPER);
        }
        if (depth >= 10) {
            availableOres.add(OreType.SILVER);
            availableOres.add(OreType.COAL);
        }
        if (depth >= 15) {
            availableOres.add(OreType.GOLD);
            if (random.nextFloat() < 0.3f) {
                availableOres.add(OreType.EMERALD);
            }
        }
        if (depth >= 20) {
            if (random.nextFloat() < 0.1f) {
                availableOres.add(OreType.DIAMOND);
            }
            if (random.nextFloat() < 0.05f) {
                availableOres.add(OreType.CURSED_GOLD);
            }
        }
        
        // Initialize resource quantities
        for (OreType ore : availableOres) {
            int quantity = 5 + random.nextInt(15) + (depth / 5);
            resources.put(ore.name(), quantity);
        }
        
        // Add other resources
        if (hasWater) {
            resources.put("FRESH_WATER", 50 + random.nextInt(100));
        }
        resources.put("STONE", 20 + random.nextInt(30));
        
        // Rare resources for deep caves
        if (depth >= 15 && random.nextFloat() < 0.2f) {
            resources.put("ANCIENT_ARTIFACT", 1 + random.nextInt(3));
        }
    }
    
    /**
     * Calculate danger level based on cave properties
     */
    private int calculateDangerLevel() {
        int danger = 0;
        
        // Depth increases danger
        danger += depth / 5;
        
        // Large caves are more dangerous
        danger += roomCount / 3;
        
        // Water adds danger (drowning risk)
        if (hasWater && !hasAir) {
            danger += 3;
        }
        
        return Math.min(10, danger);
    }
    
    @Override
    public void onInteract() {
        if (!discovered) {
            onDiscover();
            System.out.println("You discovered a cave system with " + roomCount + " rooms!");
        } else {
            System.out.println("You enter the familiar cave system.");
        }
    }
    
    @Override
    public String[] getAvailableResources() {
        return resources.keySet().toArray(new String[0]);
    }
    
    @Override
    public boolean extractResource(String resourceType, int amount) {
        Integer available = resources.get(resourceType);
        if (available != null && available >= amount) {
            resources.put(resourceType, available - amount);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean requiresSpecialAccess() {
        return !hasAir || dangerLevel >= 5;
    }
    
    @Override
    public int getDangerLevel() {
        return dangerLevel;
    }
    
    /**
     * Check if the cave requires diving equipment
     */
    public boolean requiresDivingEquipment() {
        return hasWater && !hasAir;
    }
    
    /**
     * Check if the cave has breathable air
     */
    public boolean hasBreathableAir() {
        return hasAir;
    }
    
    /**
     * Get the deepest point in the cave
     */
    public float getMaxDepth() {
        return rooms.stream()
                   .map(room -> position.y - room.position.y)
                   .max(Float::compareTo)
                   .orElse(0.0f);
    }
    
    /**
     * Get a specific room by index
     */
    public CaveRoom getRoom(int index) {
        return index >= 0 && index < rooms.size() ? rooms.get(index) : null;
    }
    
    /**
     * Get all rooms in the cave
     */
    public List<CaveRoom> getRooms() {
        return new ArrayList<>(rooms);
    }
    
    /**
     * Get the entrance room
     */
    public CaveRoom getEntrance() {
        return rooms.isEmpty() ? null : rooms.get(0);
    }
    
    // Getters
    public int getDepth() { return depth; }
    public int getRoomCount() { return roomCount; }
    public Set<OreType> getAvailableOres() { return new HashSet<>(availableOres); }
    public boolean hasWater() { return hasWater; }
    public boolean hasAir() { return hasAir; }
    
    /**
     * Inner class representing a room within the cave system
     */
    public static class CaveRoom {
        public enum RoomType {
            ENTRANCE("Cave Entrance"),
            CHAMBER("Large Chamber"),
            TUNNEL("Narrow Tunnel"),
            WATER_CHAMBER("Flooded Chamber"),
            ORE_VEIN("Ore Vein"),
            TREASURE_ROOM("Hidden Treasure Room"),
            CRYSTAL_CAVERN("Crystal Cavern");
            
            private final String displayName;
            
            RoomType(String displayName) {
                this.displayName = displayName;
            }
            
            public String getDisplayName() { return displayName; }
        }
        
        public final int id;
        public final Vector3f position;
        public final RoomType type;
        public final float size;
        public boolean hasWater;
        public boolean hasOres;
        public boolean hasTreasure;
        public boolean explored;
        
        public CaveRoom(int id, float x, float y, float z, RoomType type, float size) {
            this.id = id;
            this.position = new Vector3f(x, y, z);
            this.type = type;
            this.size = size;
            this.hasWater = false;
            this.hasOres = false;
            this.hasTreasure = false;
            this.explored = false;
        }
        
        @Override
        public String toString() {
            return String.format("%s (%.1f, %.1f, %.1f) - Size: %.1f", 
                               type.displayName, position.x, position.y, position.z, size);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Cave System at (%.1f, %.1f, %.1f) - %d rooms, %d depth, Danger: %d/10", 
                           position.x, position.y, position.z, roomCount, depth, dangerLevel);
    }
}