package com.odyssey.world.dungeons;

import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a procedurally generated dungeon with rooms, corridors, traps, and treasures.
 * Dungeons can be caves, grottos, temples, or shipwrecks.
 */
public class Dungeon {
    private final String dungeonId;
    private final DungeonType type;
    private final Vector3i entrance;
    private final List<DungeonRoom> rooms;
    private final List<DungeonCorridor> corridors;
    private final List<DungeonTrap> traps;
    private final List<DungeonTreasure> treasures;
    private final int difficulty;
    private final long seed;
    
    public Dungeon(String dungeonId, DungeonType type, Vector3i entrance, int difficulty, long seed) {
        this.dungeonId = dungeonId;
        this.type = type;
        this.entrance = new Vector3i(entrance);
        this.difficulty = difficulty;
        this.seed = seed;
        this.rooms = new ArrayList<>();
        this.corridors = new ArrayList<>();
        this.traps = new ArrayList<>();
        this.treasures = new ArrayList<>();
        
        generateDungeon();
    }
    
    /**
     * Generates the dungeon layout, rooms, and contents.
     */
    private void generateDungeon() {
        Random random = new Random(seed);
        
        // Generate rooms based on dungeon type and difficulty
        generateRooms(random);
        
        // Connect rooms with corridors
        generateCorridors(random);
        
        // Place traps throughout the dungeon
        generateTraps(random);
        
        // Place treasures in appropriate locations
        generateTreasures(random);
    }
    
    /**
     * Generates rooms for the dungeon.
     */
    private void generateRooms(Random random) {
        int roomCount = 3 + difficulty + random.nextInt(3); // 3-9 rooms based on difficulty
        
        // Always start with an entrance room
        DungeonRoom entranceRoom = new DungeonRoom(
            "entrance",
            new Vector3i(entrance),
            new Vector3i(8, 4, 8),
            DungeonRoom.RoomType.ENTRANCE
        );
        rooms.add(entranceRoom);
        
        // Generate additional rooms
        for (int i = 1; i < roomCount; i++) {
            Vector3i roomPosition = generateRoomPosition(random, i);
            Vector3i roomSize = generateRoomSize(random);
            DungeonRoom.RoomType roomType = generateRoomType(random, i == roomCount - 1);
            
            DungeonRoom room = new DungeonRoom(
                "room_" + i,
                roomPosition,
                roomSize,
                roomType
            );
            
            rooms.add(room);
        }
    }
    
    /**
     * Generates a position for a new room.
     */
    private Vector3i generateRoomPosition(Random random, int roomIndex) {
        // Position rooms in a rough spiral pattern around the entrance
        double angle = roomIndex * Math.PI * 0.7; // Golden angle for good distribution
        int distance = 15 + roomIndex * 8; // Increasing distance from entrance
        
        int x = entrance.x + (int)(Math.cos(angle) * distance);
        int z = entrance.z + (int)(Math.sin(angle) * distance);
        int y = entrance.y - random.nextInt(10) - 5; // Rooms go deeper underground
        
        return new Vector3i(x, y, z);
    }
    
    /**
     * Generates a size for a room based on its purpose.
     */
    private Vector3i generateRoomSize(Random random) {
        int width = 6 + random.nextInt(8); // 6-13 blocks wide
        int height = 3 + random.nextInt(3); // 3-5 blocks high
        int depth = 6 + random.nextInt(8); // 6-13 blocks deep
        
        return new Vector3i(width, height, depth);
    }
    
    /**
     * Generates a room type based on position and dungeon characteristics.
     */
    private DungeonRoom.RoomType generateRoomType(Random random, boolean isFinalRoom) {
        if (isFinalRoom) {
            return DungeonRoom.RoomType.TREASURE_CHAMBER;
        }
        
        DungeonRoom.RoomType[] possibleTypes = {
            DungeonRoom.RoomType.CORRIDOR_JUNCTION,
            DungeonRoom.RoomType.TRAP_ROOM,
            DungeonRoom.RoomType.PUZZLE_ROOM,
            DungeonRoom.RoomType.STORAGE,
            DungeonRoom.RoomType.GUARD_POST
        };
        
        return possibleTypes[random.nextInt(possibleTypes.length)];
    }
    
    /**
     * Generates corridors connecting the rooms.
     */
    private void generateCorridors(Random random) {
        // Connect each room to at least one other room
        for (int i = 1; i < rooms.size(); i++) {
            DungeonRoom currentRoom = rooms.get(i);
            DungeonRoom targetRoom = rooms.get(random.nextInt(i)); // Connect to a previous room
            
            DungeonCorridor corridor = new DungeonCorridor(
                "corridor_" + i,
                currentRoom.getPosition(),
                targetRoom.getPosition(),
                2 + random.nextInt(2) // Width 2-3 blocks
            );
            
            corridors.add(corridor);
        }
        
        // Add some additional connections for complexity
        int extraConnections = difficulty / 2;
        for (int i = 0; i < extraConnections; i++) {
            DungeonRoom room1 = rooms.get(random.nextInt(rooms.size()));
            DungeonRoom room2 = rooms.get(random.nextInt(rooms.size()));
            
            if (!room1.equals(room2)) {
                DungeonCorridor corridor = new DungeonCorridor(
                    "extra_corridor_" + i,
                    room1.getPosition(),
                    room2.getPosition(),
                    2
                );
                corridors.add(corridor);
            }
        }
    }
    
    /**
     * Generates traps throughout the dungeon.
     */
    private void generateTraps(Random random) {
        int trapCount = difficulty + random.nextInt(difficulty + 1);
        
        for (int i = 0; i < trapCount; i++) {
            // Place traps in corridors and some rooms
            Vector3i trapPosition;
            
            if (random.nextBoolean() && !corridors.isEmpty()) {
                // Place in corridor
                DungeonCorridor corridor = corridors.get(random.nextInt(corridors.size()));
                trapPosition = corridor.getRandomPosition(random);
            } else {
                // Place in room (avoid entrance and treasure chamber)
                List<DungeonRoom> validRooms = rooms.stream()
                    .filter(room -> room.getType() != DungeonRoom.RoomType.ENTRANCE &&
                                   room.getType() != DungeonRoom.RoomType.TREASURE_CHAMBER)
                    .toList();
                
                if (!validRooms.isEmpty()) {
                    DungeonRoom room = validRooms.get(random.nextInt(validRooms.size()));
                    trapPosition = room.getRandomPosition(random);
                } else {
                    continue; // Skip this trap
                }
            }
            
            DungeonTrap.TrapType trapType = generateTrapType(random);
            DungeonTrap trap = new DungeonTrap(
                "trap_" + i,
                trapPosition,
                trapType,
                difficulty
            );
            
            traps.add(trap);
        }
    }
    
    /**
     * Generates a trap type based on dungeon characteristics.
     */
    private DungeonTrap.TrapType generateTrapType(Random random) {
        DungeonTrap.TrapType[] trapTypes = DungeonTrap.TrapType.values();
        return trapTypes[random.nextInt(trapTypes.length)];
    }
    
    /**
     * Generates treasures for the dungeon.
     */
    private void generateTreasures(Random random) {
        // Always place main treasure in treasure chamber
        DungeonRoom treasureRoom = rooms.stream()
            .filter(room -> room.getType() == DungeonRoom.RoomType.TREASURE_CHAMBER)
            .findFirst()
            .orElse(rooms.get(rooms.size() - 1)); // Fallback to last room
        
        DungeonTreasure mainTreasure = new DungeonTreasure(
            "main_treasure",
            treasureRoom.getCenterPosition(),
            DungeonTreasure.TreasureType.MAJOR,
            difficulty
        );
        treasures.add(mainTreasure);
        
        // Add minor treasures throughout the dungeon
        int minorTreasureCount = 1 + difficulty / 2;
        for (int i = 0; i < minorTreasureCount; i++) {
            DungeonRoom room = rooms.get(random.nextInt(rooms.size()));
            Vector3i treasurePosition = room.getRandomPosition(random);
            
            DungeonTreasure treasure = new DungeonTreasure(
                "minor_treasure_" + i,
                treasurePosition,
                DungeonTreasure.TreasureType.MINOR,
                difficulty
            );
            treasures.add(treasure);
        }
    }
    
    // Getters
    public String getDungeonId() { return dungeonId; }
    public DungeonType getType() { return type; }
    public Vector3i getEntrance() { return new Vector3i(entrance); }
    public List<DungeonRoom> getRooms() { return new ArrayList<>(rooms); }
    public List<DungeonCorridor> getCorridors() { return new ArrayList<>(corridors); }
    public List<DungeonTrap> getTraps() { return new ArrayList<>(traps); }
    public List<DungeonTreasure> getTreasures() { return new ArrayList<>(treasures); }
    public int getDifficulty() { return difficulty; }
    public long getSeed() { return seed; }
    
    /**
     * Adds a treasure to this dungeon.
     * 
     * @param treasure The treasure to add
     */
    public void addTreasure(DungeonTreasure treasure) {
        if (treasure != null) {
            treasures.add(treasure);
        }
    }
    
    /**
     * Gets the total size of the dungeon in blocks.
     */
    public Vector3i getBounds() {
        if (rooms.isEmpty()) {
            return new Vector3i(0, 0, 0);
        }
        
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (DungeonRoom room : rooms) {
            Vector3i pos = room.getPosition();
            Vector3i size = room.getSize();
            
            minX = Math.min(minX, pos.x);
            maxX = Math.max(maxX, pos.x + size.x);
            minY = Math.min(minY, pos.y);
            maxY = Math.max(maxY, pos.y + size.y);
            minZ = Math.min(minZ, pos.z);
            maxZ = Math.max(maxZ, pos.z + size.z);
        }
        
        return new Vector3i(maxX - minX, maxY - minY, maxZ - minZ);
    }
    
    /**
     * Types of dungeons that can be generated.
     */
    public enum DungeonType {
        CAVE("Natural Cave"),
        GROTTO("Sea Grotto"),
        TEMPLE("Ancient Temple"),
        SHIPWRECK("Sunken Ship"),
        RUINS("Forgotten Ruins"),
        MINE("Abandoned Mine"),
        
        // Additional types for enhanced generation
        PIRATE_CAVE("Pirate Cave"),
        ANCIENT_TEMPLE("Ancient Temple"),
        UNDERWATER_RUIN("Underwater Ruin"),
        VOLCANIC_CHAMBER("Volcanic Chamber");
        
        private final String displayName;
        
        DungeonType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
}