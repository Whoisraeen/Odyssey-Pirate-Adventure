package com.odyssey.world.save.dimensions;

import com.odyssey.world.save.entity.EntityDataManager;
import com.odyssey.world.save.poi.PoiDataManager;

import java.nio.file.Path;

/**
 * Holds dimension-specific managers and metadata.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class DimensionData {
    private final DimensionType type;
    private final Path path;
    private final EntityDataManager entityManager;
    private final PoiDataManager poiManager;
    
    public DimensionData(DimensionType type, Path path, EntityDataManager entityManager, PoiDataManager poiManager) {
        this.type = type;
        this.path = path;
        this.entityManager = entityManager;
        this.poiManager = poiManager;
    }
    
    public DimensionType getType() { return type; }
    public Path getPath() { return path; }
    public EntityDataManager getEntityManager() { return entityManager; }
    public PoiDataManager getPoiManager() { return poiManager; }
}