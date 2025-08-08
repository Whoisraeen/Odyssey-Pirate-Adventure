package com.odyssey.world.save.poi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages POIs within a 32x32 chunk region for efficient storage and spatial queries.
 * Provides spatial indexing and fast lookup operations for POI management.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PoiRegion {
    private static final int REGION_SIZE = 32;
    
    private Map<UUID, PoiData> pois;
    private Map<String, Set<UUID>> poisByChunk;
    private Map<PoiType, Set<UUID>> poisByType;
    private long lastModified;
    private int version;
    
    /**
     * Creates a new empty POI region.
     */
    public PoiRegion() {
        this.pois = new ConcurrentHashMap<>();
        this.poisByChunk = new ConcurrentHashMap<>();
        this.poisByType = new ConcurrentHashMap<>();
        this.lastModified = System.currentTimeMillis();
        this.version = 1;
    }
    
    /**
     * Adds a POI to this region.
     * 
     * @param poi the POI to add
     */
    public void addPoi(PoiData poi) {
        if (poi == null || poi.getId() == null) {
            return;
        }
        
        // Remove from old indices if it exists
        PoiData existing = pois.get(poi.getId());
        if (existing != null) {
            removeFromIndices(existing);
        }
        
        // Add to POIs map
        pois.put(poi.getId(), poi);
        
        // Add to indices
        addToIndices(poi);
        
        updateModificationTime();
    }
    
    /**
     * Removes a POI from this region.
     * 
     * @param poiId the POI ID to remove
     * @return the removed POI, or null if not found
     */
    public PoiData removePoi(UUID poiId) {
        PoiData removed = pois.remove(poiId);
        if (removed != null) {
            removeFromIndices(removed);
            updateModificationTime();
        }
        return removed;
    }
    
    /**
     * Gets a POI by ID.
     * 
     * @param poiId the POI ID
     * @return the POI, or null if not found
     */
    public PoiData getPoi(UUID poiId) {
        return pois.get(poiId);
    }
    
    /**
     * Gets all POIs in a specific chunk.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return list of POIs in the chunk
     */
    public List<PoiData> getPoisInChunk(int chunkX, int chunkZ) {
        String chunkKey = getChunkKey(chunkX, chunkZ);
        Set<UUID> poiIds = poisByChunk.get(chunkKey);
        
        if (poiIds == null || poiIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return poiIds.stream()
            .map(pois::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all POIs of a specific type.
     * 
     * @param type the POI type
     * @return list of POIs of the specified type
     */
    public List<PoiData> getPoisByType(PoiType type) {
        Set<UUID> poiIds = poisByType.get(type);
        
        if (poiIds == null || poiIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return poiIds.stream()
            .map(pois::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets POIs within a specific radius of a position.
     * 
     * @param centerX the center X coordinate
     * @param centerZ the center Z coordinate
     * @param radius the search radius
     * @return list of POIs within the radius
     */
    public List<PoiData> getPoisInRadius(double centerX, double centerZ, double radius) {
        return pois.values().stream()
            .filter(poi -> poi.distanceTo(centerX, centerZ) <= radius)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets POIs of a specific type within a radius.
     * 
     * @param centerX the center X coordinate
     * @param centerZ the center Z coordinate
     * @param radius the search radius
     * @param type the POI type
     * @return list of POIs of the specified type within the radius
     */
    public List<PoiData> getPoisByTypeInRadius(double centerX, double centerZ, double radius, PoiType type) {
        Set<UUID> typePoiIds = poisByType.get(type);
        if (typePoiIds == null || typePoiIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        return typePoiIds.stream()
            .map(pois::get)
            .filter(Objects::nonNull)
            .filter(poi -> poi.distanceTo(centerX, centerZ) <= radius)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all POIs in this region.
     * 
     * @return collection of all POIs
     */
    public Collection<PoiData> getAllPois() {
        return new ArrayList<>(pois.values());
    }
    
    /**
     * Gets all active POIs in this region.
     * 
     * @return list of active POIs
     */
    public List<PoiData> getActivePois() {
        return pois.values().stream()
            .filter(PoiData::isActive)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets POIs owned by a specific player.
     * 
     * @param ownerId the owner UUID
     * @return list of POIs owned by the player
     */
    public List<PoiData> getPoisByOwner(UUID ownerId) {
        return pois.values().stream()
            .filter(poi -> poi.isOwnedBy(ownerId))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all POI IDs in this region.
     * 
     * @return set of all POI IDs
     */
    public Set<UUID> getAllPoiIds() {
        return new HashSet<>(pois.keySet());
    }
    
    /**
     * Checks if this region contains a POI.
     * 
     * @param poiId the POI ID
     * @return true if the POI exists in this region
     */
    public boolean containsPoi(UUID poiId) {
        return pois.containsKey(poiId);
    }
    
    /**
     * Gets the number of POIs in this region.
     * 
     * @return the POI count
     */
    public int getPoiCount() {
        return pois.size();
    }
    
    /**
     * Gets the number of active POIs in this region.
     * 
     * @return the active POI count
     */
    public int getActivePoiCount() {
        return (int) pois.values().stream().filter(PoiData::isActive).count();
    }
    
    /**
     * Checks if this region is empty.
     * 
     * @return true if no POIs are stored
     */
    public boolean isEmpty() {
        return pois.isEmpty();
    }
    
    /**
     * Gets POIs within a specific area.
     * 
     * @param minX minimum X coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxZ maximum Z coordinate
     * @return list of POIs within the area
     */
    public List<PoiData> getPoisInArea(double minX, double minZ, double maxX, double maxZ) {
        return pois.values().stream()
            .filter(poi -> poi.getX() >= minX && poi.getX() <= maxX &&
                          poi.getZ() >= minZ && poi.getZ() <= maxZ)
            .collect(Collectors.toList());
    }
    
    /**
     * Removes inactive POIs from this region.
     * 
     * @return number of POIs removed
     */
    public int removeInactivePois() {
        List<UUID> inactivePois = pois.values().stream()
            .filter(poi -> !poi.isActive())
            .map(PoiData::getId)
            .collect(Collectors.toList());
        
        for (UUID poiId : inactivePois) {
            removePoi(poiId);
        }
        
        return inactivePois.size();
    }
    
    /**
     * Removes stale POIs that haven't been accessed recently.
     * 
     * @param maxAgeMs maximum age in milliseconds
     * @return number of POIs removed
     */
    public int removeStalePois(long maxAgeMs) {
        List<UUID> stalePois = pois.values().stream()
            .filter(poi -> !poi.isRecentlyAccessed(maxAgeMs))
            .map(PoiData::getId)
            .collect(Collectors.toList());
        
        for (UUID poiId : stalePois) {
            removePoi(poiId);
        }
        
        return stalePois.size();
    }
    
    /**
     * Optimizes the region by cleaning up empty indices.
     */
    public void optimize() {
        // Remove empty chunk indices
        poisByChunk.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Remove empty type indices
        poisByType.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Verify index consistency
        Set<UUID> actualPois = pois.keySet();
        
        // Clean up chunk indices
        for (Set<UUID> chunkPois : poisByChunk.values()) {
            chunkPois.retainAll(actualPois);
        }
        
        // Clean up type indices
        for (Set<UUID> typePois : poisByType.values()) {
            typePois.retainAll(actualPois);
        }
        
        // Rebuild missing indices
        for (PoiData poi : pois.values()) {
            addToIndices(poi);
        }
        
        updateModificationTime();
    }
    
    /**
     * Adds a POI to the spatial indices.
     * 
     * @param poi the POI to index
     */
    private void addToIndices(PoiData poi) {
        // Add to chunk index
        String chunkKey = getChunkKey(poi.getChunkX(), poi.getChunkZ());
        poisByChunk.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet())
            .add(poi.getId());
        
        // Add to type index
        poisByType.computeIfAbsent(poi.getType(), k -> ConcurrentHashMap.newKeySet())
            .add(poi.getId());
    }
    
    /**
     * Removes a POI from the spatial indices.
     * 
     * @param poi the POI to remove from indices
     */
    private void removeFromIndices(PoiData poi) {
        // Remove from chunk index
        String chunkKey = getChunkKey(poi.getChunkX(), poi.getChunkZ());
        Set<UUID> chunkPois = poisByChunk.get(chunkKey);
        if (chunkPois != null) {
            chunkPois.remove(poi.getId());
            if (chunkPois.isEmpty()) {
                poisByChunk.remove(chunkKey);
            }
        }
        
        // Remove from type index
        Set<UUID> typePois = poisByType.get(poi.getType());
        if (typePois != null) {
            typePois.remove(poi.getId());
            if (typePois.isEmpty()) {
                poisByType.remove(poi.getType());
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
        Map<PoiType, Integer> poisByTypeCount = new HashMap<>();
        int activePois = 0;
        
        for (PoiData poi : pois.values()) {
            PoiType type = poi.getType();
            poisByTypeCount.put(type, poisByTypeCount.getOrDefault(type, 0) + 1);
            
            if (poi.isActive()) {
                activePois++;
            }
        }
        
        return new RegionStatistics(
            pois.size(),
            activePois,
            poisByChunk.size(),
            poisByTypeCount,
            lastModified,
            version
        );
    }
    
    /**
     * Statistics about a POI region.
     */
    public static class RegionStatistics {
        private final int totalPois;
        private final int activePois;
        private final int occupiedChunks;
        private final Map<PoiType, Integer> poisByType;
        private final long lastModified;
        private final int version;
        
        public RegionStatistics(int totalPois, int activePois, int occupiedChunks,
                              Map<PoiType, Integer> poisByType, long lastModified, int version) {
            this.totalPois = totalPois;
            this.activePois = activePois;
            this.occupiedChunks = occupiedChunks;
            this.poisByType = new HashMap<>(poisByType);
            this.lastModified = lastModified;
            this.version = version;
        }
        
        public int getTotalPois() { return totalPois; }
        public int getActivePois() { return activePois; }
        public int getInactivePois() { return totalPois - activePois; }
        public int getOccupiedChunks() { return occupiedChunks; }
        public Map<PoiType, Integer> getPoisByType() { return poisByType; }
        public long getLastModified() { return lastModified; }
        public int getVersion() { return version; }
    }
}