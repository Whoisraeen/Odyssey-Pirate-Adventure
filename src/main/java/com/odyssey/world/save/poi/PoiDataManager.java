package com.odyssey.world.save.poi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages Points of Interest (POI) storage in the poi/ folder with region-based organization.
 * Handles POI serialization, loading, and spatial queries for the world save system.
 * 
 * <p>Features:
 * <ul>
 * <li>Region-based POI storage (32x32 chunk regions)</li>
 * <li>JSON serialization with compression</li>
 * <li>Spatial queries and range searches</li>
 * <li>Thread-safe operations</li>
 * <li>POI type management</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PoiDataManager {
    private static final Logger logger = LoggerFactory.getLogger(PoiDataManager.class);
    
    private static final int REGION_SIZE = 32; // 32x32 chunks per region
    private static final String POI_FILE_EXTENSION = ".json";
    
    private final Path poiDirectory;
    private final Gson gson;
    private final Map<String, PoiRegion> loadedRegions;
    private final ReadWriteLock regionsLock;
    
    /**
     * Creates a new POI data manager.
     * 
     * @param worldDirectory the world directory
     */
    public PoiDataManager(Path worldDirectory) {
        this.poiDirectory = worldDirectory.resolve(WorldSaveFormat.POI_DIR);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        this.loadedRegions = new ConcurrentHashMap<>();
        this.regionsLock = new ReentrantReadWriteLock();
        
        initializeDirectory();
    }
    
    /**
     * Initializes the POI directory structure.
     */
    private void initializeDirectory() {
        try {
            Files.createDirectories(poiDirectory);
            logger.debug("Initialized POI directory: {}", poiDirectory);
        } catch (IOException e) {
            logger.error("Failed to create POI directory", e);
        }
    }
    
    /**
     * Adds a POI to persistent storage.
     * 
     * @param poi the POI to add
     */
    public void addPoi(PoiData poi) {
        if (poi == null || poi.getId() == null) {
            logger.warn("Attempted to add null or invalid POI");
            return;
        }
        
        String regionKey = getRegionKey(poi.getChunkX(), poi.getChunkZ());
        
        regionsLock.readLock().lock();
        try {
            PoiRegion region = loadedRegions.computeIfAbsent(regionKey, k -> loadRegion(regionKey));
            region.addPoi(poi);
            saveRegion(regionKey, region);
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Removes a POI from persistent storage.
     * 
     * @param poiId the POI ID
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the removed POI, or null if not found
     */
    public PoiData removePoi(UUID poiId, int chunkX, int chunkZ) {
        String regionKey = getRegionKey(chunkX, chunkZ);
        
        regionsLock.readLock().lock();
        try {
            PoiRegion region = loadedRegions.get(regionKey);
            if (region != null) {
                PoiData removed = region.removePoi(poiId);
                if (removed != null) {
                    saveRegion(regionKey, region);
                }
                return removed;
            }
            return null;
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets POIs for a specific chunk.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return list of POIs in the chunk
     */
    public List<PoiData> getPoisForChunk(int chunkX, int chunkZ) {
        String regionKey = getRegionKey(chunkX, chunkZ);
        
        regionsLock.readLock().lock();
        try {
            PoiRegion region = loadedRegions.computeIfAbsent(regionKey, k -> loadRegion(regionKey));
            return region.getPoisInChunk(chunkX, chunkZ);
        } finally {
            regionsLock.readLock().unlock();
        }
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
        List<PoiData> result = new ArrayList<>();
        
        // Calculate affected regions
        int minChunkX = (int) Math.floor((centerX - radius) / 16.0);
        int maxChunkX = (int) Math.floor((centerX + radius) / 16.0);
        int minChunkZ = (int) Math.floor((centerZ - radius) / 16.0);
        int maxChunkZ = (int) Math.floor((centerZ + radius) / 16.0);
        
        Set<String> regionKeys = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX += REGION_SIZE) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ += REGION_SIZE) {
                regionKeys.add(getRegionKey(chunkX, chunkZ));
            }
        }
        
        regionsLock.readLock().lock();
        try {
            for (String regionKey : regionKeys) {
                PoiRegion region = loadedRegions.computeIfAbsent(regionKey, k -> loadRegion(regionKey));
                result.addAll(region.getPoisInRadius(centerX, centerZ, radius));
            }
        } finally {
            regionsLock.readLock().unlock();
        }
        
        return result;
    }
    
    /**
     * Gets POIs of a specific type within a radius.
     * 
     * @param centerX the center X coordinate
     * @param centerZ the center Z coordinate
     * @param radius the search radius
     * @param poiType the POI type to search for
     * @return list of POIs of the specified type within the radius
     */
    public List<PoiData> getPoisByTypeInRadius(double centerX, double centerZ, double radius, PoiType poiType) {
        return getPoisInRadius(centerX, centerZ, radius).stream()
            .filter(poi -> poi.getType() == poiType)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Gets the nearest POI of a specific type.
     * 
     * @param centerX the center X coordinate
     * @param centerZ the center Z coordinate
     * @param poiType the POI type to search for
     * @param maxRadius the maximum search radius
     * @return the nearest POI, or null if none found
     */
    public PoiData getNearestPoi(double centerX, double centerZ, PoiType poiType, double maxRadius) {
        List<PoiData> pois = getPoisByTypeInRadius(centerX, centerZ, maxRadius, poiType);
        
        PoiData nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (PoiData poi : pois) {
            double distance = Math.sqrt(
                Math.pow(poi.getX() - centerX, 2) + 
                Math.pow(poi.getZ() - centerZ, 2)
            );
            
            if (distance < nearestDistance) {
                nearest = poi;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Updates a POI's position and saves it.
     * 
     * @param poi the POI to update
     */
    public void updatePoi(PoiData poi) {
        if (poi == null || poi.getId() == null) {
            return;
        }
        
        // Remove from old region if position changed significantly
        String newRegionKey = getRegionKey(poi.getChunkX(), poi.getChunkZ());
        String oldRegionKey = poi.getLastRegionKey();
        
        if (oldRegionKey != null && !oldRegionKey.equals(newRegionKey)) {
            regionsLock.readLock().lock();
            try {
                PoiRegion oldRegion = loadedRegions.get(oldRegionKey);
                if (oldRegion != null) {
                    oldRegion.removePoi(poi.getId());
                    saveRegion(oldRegionKey, oldRegion);
                }
            } finally {
                regionsLock.readLock().unlock();
            }
        }
        
        // Add to new region
        poi.setLastRegionKey(newRegionKey);
        addPoi(poi);
    }
    
    /**
     * Unloads POIs for a specific region to free memory.
     * 
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     */
    public void unloadRegion(int regionX, int regionZ) {
        String regionKey = getRegionKey(regionX * REGION_SIZE, regionZ * REGION_SIZE);
        
        regionsLock.writeLock().lock();
        try {
            PoiRegion region = loadedRegions.remove(regionKey);
            if (region != null) {
                saveRegion(regionKey, region);
                logger.debug("Unloaded POI region: {}", regionKey);
            }
        } finally {
            regionsLock.writeLock().unlock();
        }
    }
    
    /**
     * Saves all loaded regions to disk.
     */
    public void saveAllRegions() {
        regionsLock.readLock().lock();
        try {
            for (Map.Entry<String, PoiRegion> entry : loadedRegions.entrySet()) {
                saveRegion(entry.getKey(), entry.getValue());
            }
            logger.info("Saved {} POI regions", loadedRegions.size());
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the region key for the given chunk coordinates.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the region key
     */
    private String getRegionKey(int chunkX, int chunkZ) {
        int regionX = Math.floorDiv(chunkX, REGION_SIZE);
        int regionZ = Math.floorDiv(chunkZ, REGION_SIZE);
        return String.format("r.%d.%d", regionX, regionZ);
    }
    
    /**
     * Loads a region from disk.
     * 
     * @param regionKey the region key
     * @return the loaded region
     */
    private PoiRegion loadRegion(String regionKey) {
        Path regionFile = poiDirectory.resolve(regionKey + POI_FILE_EXTENSION);
        
        if (!Files.exists(regionFile)) {
            return new PoiRegion();
        }
        
        try {
            String json = Files.readString(regionFile);
            PoiRegion region = gson.fromJson(json, PoiRegion.class);
            logger.debug("Loaded POI region: {}", regionKey);
            return region != null ? region : new PoiRegion();
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load POI region: {}", regionKey, e);
            return new PoiRegion();
        }
    }
    
    /**
     * Saves a region to disk.
     * 
     * @param regionKey the region key
     * @param region the region to save
     */
    private void saveRegion(String regionKey, PoiRegion region) {
        Path regionFile = poiDirectory.resolve(regionKey + POI_FILE_EXTENSION);
        
        try {
            String json = gson.toJson(region);
            Files.writeString(regionFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Saved POI region: {}", regionKey);
        } catch (IOException e) {
            logger.error("Failed to save POI region: {}", regionKey, e);
        }
    }
    
    /**
     * Gets statistics about loaded POIs.
     * 
     * @return POI statistics
     */
    public PoiStatistics getStatistics() {
        regionsLock.readLock().lock();
        try {
            int totalPois = 0;
            int loadedRegions = this.loadedRegions.size();
            Map<PoiType, Integer> poisByType = new HashMap<>();
            
            for (PoiRegion region : this.loadedRegions.values()) {
                totalPois += region.getPoiCount();
                
                for (PoiData poi : region.getAllPois()) {
                    poisByType.put(poi.getType(), poisByType.getOrDefault(poi.getType(), 0) + 1);
                }
            }
            
            return new PoiStatistics(totalPois, loadedRegions, poisByType);
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Cleans up orphaned POI files and optimizes storage.
     */
    public void cleanup() {
        try {
            Files.walk(poiDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(POI_FILE_EXTENSION))
                .forEach(this::cleanupRegionFile);
        } catch (IOException e) {
            logger.error("Failed to cleanup POI files", e);
        }
    }
    
    /**
     * Cleans up a specific region file.
     * 
     * @param regionFile the region file to clean up
     */
    private void cleanupRegionFile(Path regionFile) {
        try {
            String json = Files.readString(regionFile);
            PoiRegion region = gson.fromJson(json, PoiRegion.class);
            
            if (region == null || region.isEmpty()) {
                Files.deleteIfExists(regionFile);
                logger.debug("Deleted empty POI region file: {}", regionFile.getFileName());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.warn("Failed to cleanup POI region file: {}", regionFile.getFileName(), e);
        }
    }
    
    /**
     * Closes the POI data manager and saves all data.
     */
    public void close() {
        saveAllRegions();
        loadedRegions.clear();
        logger.info("POI data manager closed");
    }
    
    /**
     * Statistics about POI storage.
     */
    public static class PoiStatistics {
        private final int totalPois;
        private final int loadedRegions;
        private final Map<PoiType, Integer> poisByType;
        
        public PoiStatistics(int totalPois, int loadedRegions, Map<PoiType, Integer> poisByType) {
            this.totalPois = totalPois;
            this.loadedRegions = loadedRegions;
            this.poisByType = new HashMap<>(poisByType);
        }
        
        public int getTotalPois() { return totalPois; }
        public int getLoadedRegions() { return loadedRegions; }
        public Map<PoiType, Integer> getPoisByType() { return poisByType; }
    }
}