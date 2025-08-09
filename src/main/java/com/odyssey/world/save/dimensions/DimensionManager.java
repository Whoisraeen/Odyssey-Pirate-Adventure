package com.odyssey.world.save.dimensions;

import com.odyssey.world.save.entity.EntityDataManager;
import com.odyssey.world.save.format.WorldSaveFormat;
import com.odyssey.world.save.poi.PoiDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages dimension-specific world data and file systems.
 * Handles Overworld, Nether, and End dimensions with separate storage.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class DimensionManager {
    private static final Logger logger = LoggerFactory.getLogger(DimensionManager.class);
    
    private final Path worldDirectory;
    private final ConcurrentHashMap<DimensionType, DimensionData> dimensions;
    
    public DimensionManager(Path worldDirectory) {
        this.worldDirectory = worldDirectory;
        this.dimensions = new ConcurrentHashMap<>();
        
        initializeDimensions();
    }
    
    /**
     * Initializes all dimension directories and managers.
     */
    private void initializeDimensions() {
        // Initialize Overworld (main world directory)
        initializeDimension(DimensionType.OVERWORLD, worldDirectory);
        
        // Initialize Nether
        Path netherPath = worldDirectory.resolve(WorldSaveFormat.DIM_NETHER);
        initializeDimension(DimensionType.NETHER, netherPath);
        
        // Initialize End
        Path endPath = worldDirectory.resolve(WorldSaveFormat.DIM_END);
        initializeDimension(DimensionType.END, endPath);
        
        logger.info("Initialized {} dimensions", dimensions.size());
    }
    
    /**
     * Initializes a single dimension.
     */
    private void initializeDimension(DimensionType type, Path dimensionPath) {
        try {
            // Create dimension directory structure
            createDimensionDirectories(dimensionPath);
            
            // Create dimension data managers
            EntityDataManager entityManager = new EntityDataManager(dimensionPath);
            PoiDataManager poiManager = new PoiDataManager(dimensionPath);
            
            DimensionData dimensionData = new DimensionData(type, dimensionPath, entityManager, poiManager);
            dimensions.put(type, dimensionData);
            
            logger.info("Initialized dimension: {} at {}", type, dimensionPath);
        } catch (Exception e) {
            logger.error("Failed to initialize dimension: {} at {}", type, dimensionPath, e);
        }
    }
    
    /**
     * Creates the directory structure for a dimension.
     */
    private void createDimensionDirectories(Path dimensionPath) throws IOException {
        // Create main dimension directory
        Files.createDirectories(dimensionPath);
        
        // Create region directory
        Files.createDirectories(dimensionPath.resolve(WorldSaveFormat.REGION_DIR));
        
        // Create entities directory
        Files.createDirectories(dimensionPath.resolve(WorldSaveFormat.ENTITIES_DIR));
        
        // Create POI directory
        Files.createDirectories(dimensionPath.resolve(WorldSaveFormat.POI_DIR));
    }
    
    /**
     * Gets dimension data for the specified type.
     */
    public DimensionData getDimension(DimensionType type) {
        return dimensions.get(type);
    }
    
    /**
     * Gets the entity manager for a dimension.
     */
    public EntityDataManager getEntityManager(DimensionType type) {
        DimensionData dimension = dimensions.get(type);
        return dimension != null ? dimension.getEntityManager() : null;
    }
    
    /**
     * Gets the POI manager for a dimension.
     */
    public PoiDataManager getPoiManager(DimensionType type) {
        DimensionData dimension = dimensions.get(type);
        return dimension != null ? dimension.getPoiManager() : null;
    }
    
    /**
     * Gets the path for a dimension.
     */
    public Path getDimensionPath(DimensionType type) {
        DimensionData dimension = dimensions.get(type);
        return dimension != null ? dimension.getPath() : null;
    }
    
    /**
     * Checks if a dimension is loaded.
     */
    public boolean isDimensionLoaded(DimensionType type) {
        return dimensions.containsKey(type);
    }
    
    /**
     * Saves all dimension data.
     */
    public void saveAllDimensions() {
        for (DimensionData dimension : dimensions.values()) {
            try {
                dimension.getEntityManager().saveAllRegions();
                dimension.getPoiManager().saveAllRegions();
                logger.debug("Saved dimension: {}", dimension.getType());
            } catch (Exception e) {
                logger.error("Failed to save dimension: {}", dimension.getType(), e);
            }
        }
        logger.info("Saved all dimension data");
    }
    
    /**
     * Unloads unused regions in all dimensions.
     * Note: This method currently performs cleanup operations.
     * Individual region unloading should be handled by the chunk management system.
     */
    public void unloadUnusedRegions() {
        for (DimensionData dimension : dimensions.values()) {
            try {
                // Perform cleanup operations instead of calling non-existent methods
                dimension.getEntityManager().cleanup();
                dimension.getPoiManager().cleanup();
                logger.debug("Performed cleanup for dimension: {}", dimension.getType());
            } catch (Exception e) {
                logger.error("Failed to cleanup dimension: {}", dimension.getType(), e);
            }
        }
    }
    
    /**
     * Gets statistics about all dimensions.
     * 
     * @return dimension statistics
     */
    public DimensionStatistics getStatistics() {
        int totalDimensions = dimensions.size();
        int totalRegions = 0;
        int totalEntities = 0;
        int totalPois = 0;
        
        for (DimensionData dimension : dimensions.values()) {
            EntityDataManager.EntityStatistics entityStats = dimension.getEntityManager().getStatistics();
            PoiDataManager.PoiStatistics poiStats = dimension.getPoiManager().getStatistics();
            
            totalRegions += entityStats.getLoadedRegions() + poiStats.getLoadedRegions();
            totalEntities += entityStats.getTotalEntities();
            totalPois += poiStats.getTotalPois();
        }
        
        return new DimensionStatistics(totalDimensions, totalRegions, totalEntities, totalPois);
    }
    
    /**
     * Shuts down all dimensions and saves data.
     */
    public void shutdown() {
        logger.info("Shutting down dimension manager...");
        
        for (DimensionData dimension : dimensions.values()) {
            dimension.getEntityManager().close();
            dimension.getPoiManager().close();
        }
        
        dimensions.clear();
        logger.info("Dimension manager shutdown complete");
    }
}

/**
 * Statistics for all dimensions.
 */
class DimensionStatistics {
    private final int dimensionCount;
    private final int totalRegions;
    private final int totalEntities;
    private final int totalPois;
    
    public DimensionStatistics(int dimensionCount, int totalRegions, int totalEntities, int totalPois) {
        this.dimensionCount = dimensionCount;
        this.totalRegions = totalRegions;
        this.totalEntities = totalEntities;
        this.totalPois = totalPois;
    }
    
    public int getDimensionCount() { return dimensionCount; }
    public int getTotalRegions() { return totalRegions; }
    public int getTotalEntities() { return totalEntities; }
    public int getTotalPois() { return totalPois; }
}