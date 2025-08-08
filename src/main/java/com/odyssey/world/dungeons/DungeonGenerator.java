package com.odyssey.world.dungeons;

import org.joml.Vector3i;

import java.util.*;

/**
 * Generates procedural dungeons with rooms, corridors, traps, and treasures.
 */
public class DungeonGenerator {
    private final Random random;
    private final DungeonGenerationConfig config;
    
    public DungeonGenerator(long seed) {
        this.random = new Random(seed);
        this.config = new DungeonGenerationConfig();
    }
    
    public DungeonGenerator(long seed, DungeonGenerationConfig config) {
        this.random = new Random(seed);
        this.config = config;
    }
    
    /**
     * Generates a complete dungeon at the specified entrance position.
     */
    public Dungeon generateDungeon(Vector3i entrance, Dungeon.DungeonType type, int difficulty) {
        String dungeonId = "dungeon_" + entrance.x + "_" + entrance.y + "_" + entrance.z;
        long dungeonSeed = random.nextLong();
        
        Dungeon dungeon = new Dungeon(dungeonId, type, entrance, difficulty, dungeonSeed);
        
        // Generate the dungeon layout
        generateLayout(dungeon);
        
        return dungeon;
    }
    
    /**
     * Generates the complete layout for a dungeon.
     */
    private void generateLayout(Dungeon dungeon) {
        // Step 1: Generate rooms
        generateRooms(dungeon);
        
        // Step 2: Connect rooms with corridors
        generateCorridors(dungeon);
        
        // Step 3: Place traps
        generateTraps(dungeon);
        
        // Step 4: Place treasures
        generateTreasures(dungeon);
    }
    
    /**
     * Generates rooms for the dungeon.
     * Note: This method provides room generation logic but the actual Dungeon class
     * handles room creation internally during construction.
     */
    private void generateRooms(Dungeon dungeon) {
        // The Dungeon class already generates its own rooms during construction
        // This method is kept for potential future enhancements
        // where external room generation might be needed
        
        // For now, we can validate or analyze the generated rooms
        List<DungeonRoom> rooms = dungeon.getRooms();
        System.out.println("Dungeon generated with " + rooms.size() + " rooms:");
        for (DungeonRoom room : rooms) {
            System.out.println("  - " + room.getType().getDisplayName() + " at " + room.getPosition());
        }
    }
    
    /**
     * Generates a single room for the dungeon.
     */
    private DungeonRoom generateRoom(Dungeon dungeon, Set<Vector3i> occupiedPositions, int roomIndex) {
        int attempts = 0;
        int maxAttempts = 50;
        
        while (attempts < maxAttempts) {
            // Generate random position relative to entrance
            Vector3i entrance = dungeon.getEntrance();
            int offsetX = random.nextInt(config.getMaxDungeonSize() * 2) - config.getMaxDungeonSize();
            int offsetZ = random.nextInt(config.getMaxDungeonSize() * 2) - config.getMaxDungeonSize();
            int offsetY = random.nextInt(5) - 2; // Allow some vertical variation
            
            Vector3i position = new Vector3i(
                entrance.x + offsetX,
                entrance.y + offsetY,
                entrance.z + offsetZ
            );
            
            // Generate random size
            int width = config.getMinRoomSize() + random.nextInt(config.getMaxRoomSize() - config.getMinRoomSize() + 1);
            int height = 3 + random.nextInt(3); // 3-5 blocks high
            int depth = config.getMinRoomSize() + random.nextInt(config.getMaxRoomSize() - config.getMinRoomSize() + 1);
            Vector3i size = new Vector3i(width, height, depth);
            
            // Check if position is valid (doesn't overlap with existing rooms)
            if (isValidRoomPosition(position, size, occupiedPositions)) {
                DungeonRoom.RoomType type = selectRoomType(dungeon, roomIndex);
                return new DungeonRoom("room_" + roomIndex, position, size, type);
            }
            
            attempts++;
        }
        
        return null; // Failed to find valid position
    }
    
    /**
     * Selects an appropriate room type based on dungeon progress.
     */
    private DungeonRoom.RoomType selectRoomType(Dungeon dungeon, int roomIndex) {
        List<DungeonRoom.RoomType> availableTypes = new ArrayList<>();
        
        // Add common room types
        availableTypes.add(DungeonRoom.RoomType.GUARD_POST);
        availableTypes.add(DungeonRoom.RoomType.PUZZLE_ROOM);
        availableTypes.add(DungeonRoom.RoomType.STORAGE);
        availableTypes.add(DungeonRoom.RoomType.CORRIDOR_JUNCTION);
        
        // Add special room types based on conditions
        if (roomIndex > 2) { // Only after a few rooms
            availableTypes.add(DungeonRoom.RoomType.TREASURE_CHAMBER);
            availableTypes.add(DungeonRoom.RoomType.THRONE_ROOM);
            availableTypes.add(DungeonRoom.RoomType.RITUAL_CHAMBER);
        }
        
        if (random.nextFloat() < 0.3f) { // 30% chance for special rooms
            availableTypes.add(DungeonRoom.RoomType.SHRINE);
            availableTypes.add(DungeonRoom.RoomType.TRAP_ROOM);
            availableTypes.add(DungeonRoom.RoomType.LIBRARY);
            availableTypes.add(DungeonRoom.RoomType.ARMORY);
        }
        
        return availableTypes.get(random.nextInt(availableTypes.size()));
    }
    
    /**
     * Checks if a room position is valid (doesn't overlap with existing rooms).
     */
    private boolean isValidRoomPosition(Vector3i position, Vector3i size, Set<Vector3i> occupiedPositions) {
        // Check all positions this room would occupy
        for (int x = position.x; x < position.x + size.x; x++) {
            for (int y = position.y; y < position.y + size.y; y++) {
                for (int z = position.z; z < position.z + size.z; z++) {
                    Vector3i checkPos = new Vector3i(x, y, z);
                    if (occupiedPositions.contains(checkPos)) {
                        return false;
                    }
                }
            }
        }
        
        // Add buffer zone around room
        int buffer = 2;
        for (int x = position.x - buffer; x < position.x + size.x + buffer; x++) {
            for (int y = position.y - buffer; y < position.y + size.y + buffer; y++) {
                for (int z = position.z - buffer; z < position.z + size.z + buffer; z++) {
                    Vector3i checkPos = new Vector3i(x, y, z);
                    if (occupiedPositions.contains(checkPos)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Adds all positions occupied by a room to the occupied set.
     */
    private void addRoomPositions(Set<Vector3i> occupiedPositions, DungeonRoom room) {
        Vector3i pos = room.getPosition();
        Vector3i size = room.getSize();
        
        for (int x = pos.x; x < pos.x + size.x; x++) {
            for (int y = pos.y; y < pos.y + size.y; y++) {
                for (int z = pos.z; z < pos.z + size.z; z++) {
                    occupiedPositions.add(new Vector3i(x, y, z));
                }
            }
        }
    }
    
    /**
     * Generates corridors connecting all rooms.
     * Note: The Dungeon class already generates corridors during construction.
     */
    private void generateCorridors(Dungeon dungeon) {
        // The Dungeon class already generates its own corridors during construction
        // This method is kept for potential future enhancements
        
        // For now, we can validate or analyze the generated corridors
        List<DungeonCorridor> corridors = dungeon.getCorridors();
        System.out.println("Dungeon generated with " + corridors.size() + " corridors:");
        for (DungeonCorridor corridor : corridors) {
            System.out.println("  - " + corridor.getCorridorId() + " connecting rooms");
        }
    }
    
    /**
     * Generates traps throughout the dungeon.
     * Note: The Dungeon class already generates traps during construction.
     */
    private void generateTraps(Dungeon dungeon) {
        // The Dungeon class already generates its own traps during construction
        // This method is kept for potential future enhancements
        
        // For now, we can validate or analyze the generated traps
        List<DungeonTrap> traps = dungeon.getTraps();
        System.out.println("Dungeon generated with " + traps.size() + " traps:");
        for (DungeonTrap trap : traps) {
            System.out.println("  - " + trap.getType().getDescription() + " at " + trap.getPosition());
        }
    }
    
    /**
     * Generates a single trap.
     */
    private DungeonTrap generateTrap(Dungeon dungeon, List<DungeonRoom> rooms, List<DungeonCorridor> corridors, int trapIndex) {
        // Choose location: 70% in corridors, 30% in rooms
        Vector3i position;
        if (random.nextFloat() < 0.7f && !corridors.isEmpty()) {
            // Place in corridor
            DungeonCorridor corridor = corridors.get(random.nextInt(corridors.size()));
            position = corridor.getRandomPosition(random);
        } else if (!rooms.isEmpty()) {
            // Place in room (avoid entrance room)
            List<DungeonRoom> nonEntranceRooms = rooms.stream()
                .filter(r -> r.getType() != DungeonRoom.RoomType.ENTRANCE)
                .toList();
            
            if (!nonEntranceRooms.isEmpty()) {
                DungeonRoom room = nonEntranceRooms.get(random.nextInt(nonEntranceRooms.size()));
                position = room.getRandomPosition(random);
            } else {
                return null;
            }
        } else {
            return null;
        }
        
        // Select trap type
        DungeonTrap.TrapType[] trapTypes = DungeonTrap.TrapType.values();
        DungeonTrap.TrapType type = trapTypes[random.nextInt(trapTypes.length)];
        
        // Difficulty based on dungeon difficulty and distance from entrance
        int baseDifficulty = dungeon.getDifficulty();
        int trapDifficulty = baseDifficulty + random.nextInt(3) - 1; // ±1 variation
        trapDifficulty = Math.max(1, Math.min(10, trapDifficulty));
        
        String trapId = "trap_" + trapIndex;
        return new DungeonTrap(trapId, position, type, trapDifficulty);
    }
    
    /**
     * Generates treasures in appropriate locations.
     */
    private void generateTreasures(Dungeon dungeon) {
        // The Dungeon class already generates its own treasures during construction
        // This method is kept for potential future enhancements
        
        // For now, we can validate or analyze the generated treasures
        List<DungeonTreasure> treasures = dungeon.getTreasures();
        System.out.println("Dungeon generated with " + treasures.size() + " treasures:");
        for (DungeonTreasure treasure : treasures) {
            System.out.println("  - " + treasure.getType().getDisplayName() + " at " + treasure.getPosition());
        }
    }
    
    /**
     * Places treasure in a specific room.
     */
    private void placeTreasureInRoom(Dungeon dungeon, DungeonRoom room, boolean isMainTreasure) {
        // This is a placeholder - in a real implementation, this would create
        // actual treasure objects and place them in the room
        
        // For now, we'll just mark the room as containing treasure
        // The actual treasure system would be implemented separately
        
        Vector3i treasurePosition = room.getCenterPosition();
        
        // TODO: Create actual treasure objects
        // - Determine treasure type based on dungeon type and difficulty
        // - Create treasure chest or item spawns
        // - Add to dungeon's treasure list
        
        System.out.println("Placed " + (isMainTreasure ? "main" : "secondary") + 
                          " treasure in room " + room.getRoomId() + 
                          " at position " + treasurePosition);
    }
    
    /**
     * Configuration class for dungeon generation parameters.
     */
    public static class DungeonGenerationConfig {
        private int minRooms = 3;
        private int maxRooms = 8;
        private int minRoomSize = 5;
        private int maxRoomSize = 12;
        private int maxDungeonSize = 100;
        private int minTraps = 2;
        private int maxTraps = 10;
        
        // Getters and setters
        public int getMinRooms() { return minRooms; }
        public void setMinRooms(int minRooms) { this.minRooms = minRooms; }
        
        public int getMaxRooms() { return maxRooms; }
        public void setMaxRooms(int maxRooms) { this.maxRooms = maxRooms; }
        
        public int getMinRoomSize() { return minRoomSize; }
        public void setMinRoomSize(int minRoomSize) { this.minRoomSize = minRoomSize; }
        
        public int getMaxRoomSize() { return maxRoomSize; }
        public void setMaxRoomSize(int maxRoomSize) { this.maxRoomSize = maxRoomSize; }
        
        public int getMaxDungeonSize() { return maxDungeonSize; }
        public void setMaxDungeonSize(int maxDungeonSize) { this.maxDungeonSize = maxDungeonSize; }
        
        public int getMinTraps() { return minTraps; }
        public void setMinTraps(int minTraps) { this.minTraps = minTraps; }
        
        public int getMaxTraps() { return maxTraps; }
        public void setMaxTraps(int maxTraps) { this.maxTraps = maxTraps; }
    }
}