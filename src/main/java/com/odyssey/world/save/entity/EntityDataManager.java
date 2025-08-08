package com.odyssey.world.save.entity;

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
 * Manages persistent entity storage in the entities/ folder with region-based organization.
 * Handles entity serialization, loading, and cleanup for the world save system.
 * 
 * <p>Features:
 * <ul>
 * <li>Region-based entity storage (32x32 chunk regions)</li>
 * <li>JSON serialization with compression</li>
 * <li>Lazy loading and unloading</li>
 * <li>Thread-safe operations</li>
 * <li>Entity lifecycle management</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class EntityDataManager {
    private static final Logger logger = LoggerFactory.getLogger(EntityDataManager.class);
    
    private static final int REGION_SIZE = 32; // 32x32 chunks per region
    private static final String ENTITY_FILE_EXTENSION = ".json";
    
    private final Path entitiesDirectory;
    private final Gson gson;
    private final Map<String, EntityRegion> loadedRegions;
    private final ReadWriteLock regionsLock;
    
    /**
     * Creates a new entity data manager.
     * 
     * @param worldDirectory the world directory
     */
    public EntityDataManager(Path worldDirectory) {
        this.entitiesDirectory = worldDirectory.resolve(WorldSaveFormat.ENTITIES_DIR);
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        this.loadedRegions = new ConcurrentHashMap<>();
        this.regionsLock = new ReentrantReadWriteLock();
        
        initializeDirectory();
    }
    
    /**
     * Initializes the entities directory structure.
     */
    private void initializeDirectory() {
        try {
            Files.createDirectories(entitiesDirectory);
            logger.debug("Initialized entities directory: {}", entitiesDirectory);
        } catch (IOException e) {
            logger.error("Failed to create entities directory", e);
        }
    }
    
    /**
     * Saves an entity to persistent storage.
     * 
     * @param entity the entity to save
     */
    public void saveEntity(EntityData entity) {
        if (entity == null || entity.getUuid() == null) {
            logger.warn("Attempted to save null or invalid entity");
            return;
        }
        
        String regionKey = getRegionKey(entity.getChunkX(), entity.getChunkZ());
        
        regionsLock.readLock().lock();
        try {
            EntityRegion region = loadedRegions.computeIfAbsent(regionKey, k -> loadRegion(regionKey));
            region.addEntity(entity);
            saveRegion(regionKey, region);
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Loads entities for a specific chunk.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return list of entities in the chunk
     */
    public List<EntityData> loadEntitiesForChunk(int chunkX, int chunkZ) {
        String regionKey = getRegionKey(chunkX, chunkZ);
        
        regionsLock.readLock().lock();
        try {
            EntityRegion region = loadedRegions.computeIfAbsent(regionKey, k -> loadRegion(regionKey));
            return region.getEntitiesInChunk(chunkX, chunkZ);
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Removes an entity from persistent storage.
     * 
     * @param entityUuid the entity UUID
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     */
    public void removeEntity(UUID entityUuid, int chunkX, int chunkZ) {
        String regionKey = getRegionKey(chunkX, chunkZ);
        
        regionsLock.readLock().lock();
        try {
            EntityRegion region = loadedRegions.get(regionKey);
            if (region != null) {
                region.removeEntity(entityUuid);
                saveRegion(regionKey, region);
            }
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Updates an entity's position and saves it.
     * 
     * @param entity the entity to update
     */
    public void updateEntity(EntityData entity) {
        if (entity == null || entity.getUuid() == null) {
            return;
        }
        
        // Remove from old region if position changed significantly
        String newRegionKey = getRegionKey(entity.getChunkX(), entity.getChunkZ());
        String oldRegionKey = entity.getLastRegionKey();
        
        if (oldRegionKey != null && !oldRegionKey.equals(newRegionKey)) {
            regionsLock.readLock().lock();
            try {
                EntityRegion oldRegion = loadedRegions.get(oldRegionKey);
                if (oldRegion != null) {
                    oldRegion.removeEntity(entity.getUuid());
                    saveRegion(oldRegionKey, oldRegion);
                }
            } finally {
                regionsLock.readLock().unlock();
            }
        }
        
        // Add to new region
        entity.setLastRegionKey(newRegionKey);
        saveEntity(entity);
    }
    
    /**
     * Unloads entities for a specific region to free memory.
     * 
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     */
    public void unloadRegion(int regionX, int regionZ) {
        String regionKey = getRegionKey(regionX * REGION_SIZE, regionZ * REGION_SIZE);
        
        regionsLock.writeLock().lock();
        try {
            EntityRegion region = loadedRegions.remove(regionKey);
            if (region != null) {
                saveRegion(regionKey, region);
                logger.debug("Unloaded entity region: {}", regionKey);
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
            for (Map.Entry<String, EntityRegion> entry : loadedRegions.entrySet()) {
                saveRegion(entry.getKey(), entry.getValue());
            }
            logger.info("Saved {} entity regions", loadedRegions.size());
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
    private EntityRegion loadRegion(String regionKey) {
        Path regionFile = entitiesDirectory.resolve(regionKey + ENTITY_FILE_EXTENSION);
        
        if (!Files.exists(regionFile)) {
            return new EntityRegion();
        }
        
        try {
            String json = Files.readString(regionFile);
            EntityRegion region = gson.fromJson(json, EntityRegion.class);
            logger.debug("Loaded entity region: {}", regionKey);
            return region != null ? region : new EntityRegion();
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load entity region: {}", regionKey, e);
            return new EntityRegion();
        }
    }
    
    /**
     * Saves a region to disk.
     * 
     * @param regionKey the region key
     * @param region the region to save
     */
    private void saveRegion(String regionKey, EntityRegion region) {
        Path regionFile = entitiesDirectory.resolve(regionKey + ENTITY_FILE_EXTENSION);
        
        try {
            String json = gson.toJson(region);
            Files.writeString(regionFile, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Saved entity region: {}", regionKey);
        } catch (IOException e) {
            logger.error("Failed to save entity region: {}", regionKey, e);
        }
    }
    
    /**
     * Gets statistics about loaded entities.
     * 
     * @return entity statistics
     */
    public EntityStatistics getStatistics() {
        regionsLock.readLock().lock();
        try {
            int totalEntities = 0;
            int loadedRegions = this.loadedRegions.size();
            
            for (EntityRegion region : this.loadedRegions.values()) {
                totalEntities += region.getEntityCount();
            }
            
            return new EntityStatistics(totalEntities, loadedRegions);
        } finally {
            regionsLock.readLock().unlock();
        }
    }
    
    /**
     * Cleans up orphaned entity files and optimizes storage.
     */
    public void cleanup() {
        try {
            Files.walk(entitiesDirectory)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(ENTITY_FILE_EXTENSION))
                .forEach(this::cleanupRegionFile);
        } catch (IOException e) {
            logger.error("Failed to cleanup entity files", e);
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
            EntityRegion region = gson.fromJson(json, EntityRegion.class);
            
            if (region == null || region.isEmpty()) {
                Files.deleteIfExists(regionFile);
                logger.debug("Deleted empty entity region file: {}", regionFile.getFileName());
            }
        } catch (IOException | JsonSyntaxException e) {
            logger.warn("Failed to cleanup region file: {}", regionFile.getFileName(), e);
        }
    }
    
    /**
     * Closes the entity data manager and saves all data.
     */
    public void close() {
        saveAllRegions();
        loadedRegions.clear();
        logger.info("Entity data manager closed");
    }
    
    /**
     * Statistics about entity storage.
     */
    public static class EntityStatistics {
        private final int totalEntities;
        private final int loadedRegions;
        
        public EntityStatistics(int totalEntities, int loadedRegions) {
            this.totalEntities = totalEntities;
            this.loadedRegions = loadedRegions;
        }
        
        public int getTotalEntities() { return totalEntities; }
        public int getLoadedRegions() { return loadedRegions; }
    }
}