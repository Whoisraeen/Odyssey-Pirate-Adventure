package com.odyssey.world.save.entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages entities within a 32x32 chunk region for efficient storage and retrieval.
 * Provides spatial indexing and fast lookup operations for entity management.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class EntityRegion {
    private static final int REGION_SIZE = 32;
    
    private Map<UUID, EntityData> entities;
    private Map<String, Set<UUID>> entitiesByChunk;
    private long lastModified;
    private int version;
    
    /**
     * Creates a new empty entity region.
     */
    public EntityRegion() {
        this.entities = new ConcurrentHashMap<>();
        this.entitiesByChunk = new ConcurrentHashMap<>();
        this.lastModified = System.currentTimeMillis();
        this.version = 1;
    }
    
    /**
     * Adds an entity to this region.
     * 
     * @param entity the entity to add
     */
    public void addEntity(EntityData entity) {
        if (entity == null || entity.getUuid() == null) {
            return;
        }
        
        // Remove from old chunk if it exists
        EntityData existing = entities.get(entity.getUuid());
        if (existing != null) {
            removeFromChunkIndex(existing);
        }
        
        // Add to entities map
        entities.put(entity.getUuid(), entity);
        
        // Add to chunk index
        addToChunkIndex(entity);
        
        updateModificationTime();
    }
    
    /**
     * Removes an entity from this region.
     * 
     * @param entityUuid the entity UUID to remove
     * @return the removed entity, or null if not found
     */
    public EntityData removeEntity(UUID entityUuid) {
        EntityData removed = entities.remove(entityUuid);
        if (removed != null) {
            removeFromChunkIndex(removed);
            updateModificationTime();
        }
        return removed;
    }
    
    /**
     * Gets an entity by UUID.
     * 
     * @param entityUuid the entity UUID
     * @return the entity, or null if not found
     */
    public EntityData getEntity(UUID entityUuid) {
        return entities.get(entityUuid);
    }
    
    /**
     * Gets all entities in a specific chunk.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return list of entities in the chunk
     */
    public List<EntityData> getEntitiesInChunk(int chunkX, int chunkZ) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        Set<UUID> entityUuids = entitiesByChunk.get(chunkKey);
        
        if (entityUuids == null || entityUuids.isEmpty()) {
            return new ArrayList<>();
        }
        
        return entityUuids.stream()
            .map(entities::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all entities in this region.
     * 
     * @return collection of all entities
     */
    public Collection<EntityData> getAllEntities() {
        return new ArrayList<>(entities.values());
    }
    
    /**
     * Gets all entity UUIDs in this region.
     * 
     * @return set of all entity UUIDs
     */
    public Set<UUID> getAllEntityUuids() {
        return new HashSet<>(entities.keySet());
    }
    
    /**
     * Checks if this region contains an entity.
     * 
     * @param entityUuid the entity UUID
     * @return true if the entity exists in this region
     */
    public boolean containsEntity(UUID entityUuid) {
        return entities.containsKey(entityUuid);
    }
    
    /**
     * Gets the number of entities in this region.
     * 
     * @return the entity count
     */
    public int getEntityCount() {
        return entities.size();
    }
    
    /**
     * Checks if this region is empty.
     * 
     * @return true if no entities are stored
     */
    public boolean isEmpty() {
        return entities.isEmpty();
    }
    
    /**
     * Gets entities within a specific area.
     * 
     * @param minX minimum X coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxZ maximum Z coordinate
     * @return list of entities within the area
     */
    public List<EntityData> getEntitiesInArea(double minX, double minZ, double maxX, double maxZ) {
        return entities.values().stream()
            .filter(entity -> entity.getX() >= minX && entity.getX() <= maxX &&
                            entity.getZ() >= minZ && entity.getZ() <= maxZ)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets entities of a specific type.
     * 
     * @param entityType the entity type
     * @return list of entities of the specified type
     */
    public List<EntityData> getEntitiesByType(String entityType) {
        return entities.values().stream()
            .filter(entity -> entityType.equals(entity.getEntityType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Removes stale entities that haven't been updated recently.
     * 
     * @param maxAgeMs maximum age in milliseconds
     * @return number of entities removed
     */
    public int removeStaleEntities(long maxAgeMs) {
        List<UUID> staleEntities = entities.values().stream()
            .filter(entity -> entity.isStale(maxAgeMs))
            .map(EntityData::getUuid)
            .collect(Collectors.toList());
        
        for (UUID uuid : staleEntities) {
            removeEntity(uuid);
        }
        
        return staleEntities.size();
    }
    
    /**
     * Removes dead entities (health <= 0).
     * 
     * @return number of entities removed
     */
    public int removeDeadEntities() {
        List<UUID> deadEntities = entities.values().stream()
            .filter(entity -> !entity.isAlive())
            .map(EntityData::getUuid)
            .collect(Collectors.toList());
        
        for (UUID uuid : deadEntities) {
            removeEntity(uuid);
        }
        
        return deadEntities.size();
    }
    
    /**
     * Optimizes the region by cleaning up empty chunk indices.
     */
    public void optimize() {
        // Remove empty chunk indices
        entitiesByChunk.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Verify chunk index consistency
        Set<UUID> indexedEntities = entitiesByChunk.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
        
        Set<UUID> actualEntities = entities.keySet();
        
        // Remove orphaned indices
        for (Set<UUID> chunkEntities : entitiesByChunk.values()) {
            chunkEntities.retainAll(actualEntities);
        }
        
        // Add missing indices
        for (EntityData entity : entities.values()) {
            if (!indexedEntities.contains(entity.getUuid())) {
                addToChunkIndex(entity);
            }
        }
        
        updateModificationTime();
    }
    
    /**
     * Adds an entity to the chunk index.
     * 
     * @param entity the entity to index
     */
    private void addToChunkIndex(EntityData entity) {
        String chunkKey = getChunkKey(entity.getChunkX(), entity.getChunkZ());
        entitiesByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
            .add(entity.getUuid());
    }
    
    /**
     * Removes an entity from the chunk index.
     * 
     * @param entity the entity to remove from index
     */
    private void removeFromChunkIndex(EntityData entity) {
        String chunkKey = getChunkKey(entity.getChunkX(), entity.getChunkZ());
        Set<UUID> chunkEntities = entitiesByChunk.get(chunkKey);
        if (chunkEntities != null) {
            chunkEntities.remove(entity.getUuid());
            if (chunkEntities.isEmpty()) {
                entitiesByChunk.remove(chunkKey);
            }
        }
    }
    
    /**
     * Gets the chunk key for the given coordinates.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the chunk key
     */
    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }
    
    /**
     * Updates the last modified timestamp.
     */
    private void updateModificationTime() {
        this.lastModified = System.currentTimeMillis();
        this.version++;
    }
    
    /**
     * Gets the last modified timestamp.
     * 
     * @return the last modified timestamp
     */
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * Gets the region version.
     * 
     * @return the region version
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * Gets statistics about this region.
     * 
     * @return region statistics
     */
    public RegionStatistics getStatistics() {
        Map<String, Integer> entitiesByType = new HashMap<>();
        int aliveEntities = 0;
        
        for (EntityData entity : entities.values()) {
            String type = entity.getEntityType();
            entitiesByType.put(type, entitiesByType.getOrDefault(type, 0) + 1);
            
            if (entity.isAlive()) {
                aliveEntities++;
            }
        }
        
        return new RegionStatistics(
            entities.size(),
            aliveEntities,
            entitiesByChunk.size(),
            entitiesByType,
            lastModified,
            version
        );
    }
    
    /**
     * Statistics about an entity region.
     */
    public static class RegionStatistics {
        private final int totalEntities;
        private final int aliveEntities;
        private final int occupiedChunks;
        private final Map<String, Integer> entitiesByType;
        private final long lastModified;
        private final int version;
        
        public RegionStatistics(int totalEntities, int aliveEntities, int occupiedChunks,
                              Map<String, Integer> entitiesByType, long lastModified, int version) {
            this.totalEntities = totalEntities;
            this.aliveEntities = aliveEntities;
            this.occupiedChunks = occupiedChunks;
            this.entitiesByType = new HashMap<>(entitiesByType);
            this.lastModified = lastModified;
            this.version = version;
        }
        
        public int getTotalEntities() { return totalEntities; }
        public int getAliveEntities() { return aliveEntities; }
        public int getDeadEntities() { return totalEntities - aliveEntities; }
        public int getOccupiedChunks() { return occupiedChunks; }
        public Map<String, Integer> getEntitiesByType() { return entitiesByType; }
        public long getLastModified() { return lastModified; }
        public int getVersion() { return version; }
    }
}