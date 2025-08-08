package com.odyssey.world.dungeons;

import org.joml.Vector3i;

import java.util.Random;

/**
 * Represents a room within a dungeon with specific purpose and characteristics.
 */
public class DungeonRoom {
    private final String roomId;
    private final Vector3i position;
    private final Vector3i size;
    private final RoomType type;
    
    public DungeonRoom(String roomId, Vector3i position, Vector3i size, RoomType type) {
        this.roomId = roomId;
        this.position = new Vector3i(position);
        this.size = new Vector3i(size);
        this.type = type;
    }
    
    /**
     * Gets a random position within this room.
     */
    public Vector3i getRandomPosition(Random random) {
        int x = position.x + 1 + random.nextInt(Math.max(1, size.x - 2));
        int y = position.y + 1;
        int z = position.z + 1 + random.nextInt(Math.max(1, size.z - 2));
        
        return new Vector3i(x, y, z);
    }
    
    /**
     * Gets the center position of this room.
     */
    public Vector3i getCenterPosition() {
        return new Vector3i(
            position.x + size.x / 2,
            position.y + size.y / 2,
            position.z + size.z / 2
        );
    }
    
    /**
     * Checks if a position is within this room.
     */
    public boolean contains(Vector3i pos) {
        return pos.x >= position.x && pos.x < position.x + size.x &&
               pos.y >= position.y && pos.y < position.y + size.y &&
               pos.z >= position.z && pos.z < position.z + size.z;
    }
    
    /**
     * Gets the floor area of this room.
     */
    public int getFloorArea() {
        return size.x * size.z;
    }
    
    /**
     * Gets the volume of this room.
     */
    public int getVolume() {
        return size.x * size.y * size.z;
    }
    
    // Getters
    public String getRoomId() { return roomId; }
    public Vector3i getPosition() { return new Vector3i(position); }
    public Vector3i getSize() { return new Vector3i(size); }
    public RoomType getType() { return type; }
    
    @Override
    public String toString() {
        return String.format("DungeonRoom{id='%s', type=%s, pos=(%d,%d,%d), size=(%d,%d,%d)}", 
                           roomId, type, position.x, position.y, position.z, size.x, size.y, size.z);
    }
    
    /**
     * Types of rooms that can exist in a dungeon.
     */
    public enum RoomType {
        ENTRANCE("Entrance Chamber"),
        TREASURE_CHAMBER("Treasure Chamber"),
        TRAP_ROOM("Trap Room"),
        PUZZLE_ROOM("Puzzle Room"),
        CORRIDOR_JUNCTION("Junction"),
        STORAGE("Storage Room"),
        GUARD_POST("Guard Post"),
        SHRINE("Ancient Shrine"),
        LIBRARY("Forgotten Library"),
        ARMORY("Weapon Cache"),
        LABORATORY("Alchemist Lab"),
        PRISON("Prison Cell"),
        THRONE_ROOM("Throne Room"),
        RITUAL_CHAMBER("Ritual Chamber");
        
        private final String displayName;
        
        RoomType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        /**
         * Checks if this room type typically contains treasures.
         */
        public boolean isLootRoom() {
            return this == TREASURE_CHAMBER || this == STORAGE || 
                   this == ARMORY || this == SHRINE || this == THRONE_ROOM;
        }
        
        /**
         * Checks if this room type is dangerous.
         */
        public boolean isDangerous() {
            return this == TRAP_ROOM || this == GUARD_POST || 
                   this == RITUAL_CHAMBER || this == PRISON;
        }
        
        /**
         * Gets the recommended minimum size for this room type.
         */
        public Vector3i getMinimumSize() {
            return switch (this) {
                case ENTRANCE -> new Vector3i(8, 4, 8);
                case TREASURE_CHAMBER -> new Vector3i(10, 5, 10);
                case THRONE_ROOM -> new Vector3i(12, 6, 12);
                case LIBRARY -> new Vector3i(8, 4, 12);
                case RITUAL_CHAMBER -> new Vector3i(10, 6, 10);
                case CORRIDOR_JUNCTION -> new Vector3i(4, 3, 4);
                case PRISON -> new Vector3i(3, 3, 3);
                default -> new Vector3i(6, 3, 6);
            };
        }
    }
}