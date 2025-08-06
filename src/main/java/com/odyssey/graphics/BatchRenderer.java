package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple render batches to optimize rendering performance.
 * Automatically groups objects by material and mesh, and renders them efficiently
 * using instanced rendering when possible.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class BatchRenderer {
    private static final Logger logger = LoggerFactory.getLogger(BatchRenderer.class);
    
    // Batch management
    private final Map<String, RenderBatch> batches;
    private final List<RenderBatch> sortedBatches;
    private boolean batchesDirty;
    
    // Rendering statistics
    private int frameDrawCalls;
    private int frameInstancesRendered;
    private int frameBatchesRendered;
    
    // Sorting and culling
    private final Comparator<RenderBatch> batchComparator;
    private boolean enableFrustumCulling;
    private boolean enableDepthSorting;
    
    /**
     * Creates a new batch renderer.
     */
    public BatchRenderer() {
        this.batches = new ConcurrentHashMap<>();
        this.sortedBatches = new ArrayList<>();
        this.batchesDirty = true;
        
        // Create batch comparator for optimal rendering order
        this.batchComparator = createBatchComparator();
        
        this.enableFrustumCulling = true;
        this.enableDepthSorting = true;
        
        logger.info("BatchRenderer initialized");
    }
    
    /**
     * Submits an object for batch rendering.
     * 
     * @param material The material to use
     * @param mesh The mesh to render
     * @param transform The transformation matrix
     * @return The render instance that was created
     */
    public RenderBatch.RenderInstance submit(Material material, Mesh mesh, Matrix4f transform) {
        RenderBatch.RenderInstance instance = new RenderBatch.RenderInstance(transform);
        submit(material, mesh, instance);
        return instance;
    }
    
    /**
     * Submits an object for batch rendering with position, scale, and rotation.
     * 
     * @param material The material to use
     * @param mesh The mesh to render
     * @param position The position
     * @param scale The scale
     * @param rotation The rotation (in radians)
     * @return The render instance that was created
     */
    public RenderBatch.RenderInstance submit(Material material, Mesh mesh, Vector3f position, Vector3f scale, Vector3f rotation) {
        RenderBatch.RenderInstance instance = new RenderBatch.RenderInstance(position, scale, rotation);
        submit(material, mesh, instance);
        return instance;
    }
    
    /**
     * Submits a render instance for batch rendering.
     * 
     * @param material The material to use
     * @param mesh The mesh to render
     * @param instance The render instance
     */
    public void submit(Material material, Mesh mesh, RenderBatch.RenderInstance instance) {
        if (material == null || mesh == null || instance == null) {
            logger.warn("Cannot submit null material, mesh, or instance");
            return;
        }
        
        // Find or create appropriate batch
        RenderBatch batch = findOrCreateBatch(material, mesh);
        
        // Add instance to batch
        if (!batch.addInstance(instance)) {
            // Batch is full, create a new one
            batch = createNewBatch(material, mesh);
            batch.addInstance(instance);
        }
    }
    
    /**
     * Finds an existing batch or creates a new one for the given material and mesh.
     * 
     * @param material The material
     * @param mesh The mesh
     * @return A suitable render batch
     */
    private RenderBatch findOrCreateBatch(Material material, Mesh mesh) {
        // Look for existing compatible batch with space
        for (RenderBatch batch : batches.values()) {
            if (batch.isCompatible(material, mesh) && batch.hasSpace()) {
                return batch;
            }
        }
        
        // No suitable batch found, create a new one
        return createNewBatch(material, mesh);
    }
    
    /**
     * Creates a new render batch for the given material and mesh.
     * 
     * @param material The material
     * @param mesh The mesh
     * @return A new render batch
     */
    private RenderBatch createNewBatch(Material material, Mesh mesh) {
        RenderBatch batch = new RenderBatch(material, mesh);
        
        // Generate unique batch ID if there's a conflict
        String batchId = batch.getBatchId();
        int counter = 1;
        while (batches.containsKey(batchId)) {
            batchId = batch.getBatchId() + "_" + counter++;
        }
        
        batches.put(batchId, batch);
        batchesDirty = true;
        
        logger.debug("Created new render batch: {}", batchId);
        return batch;
    }
    
    /**
     * Renders all batches.
     * 
     * @param shader The shader to use for rendering
     * @param camera The camera for frustum culling (optional)
     */
    public void render(Shader shader, Camera camera) {
        if (shader == null) {
            logger.warn("Cannot render with null shader");
            return;
        }
        
        // Reset frame statistics
        frameDrawCalls = 0;
        frameInstancesRendered = 0;
        frameBatchesRendered = 0;
        
        // Update sorted batch list if needed
        if (batchesDirty) {
            updateSortedBatches();
        }
        
        // Render all batches in optimal order
        for (RenderBatch batch : sortedBatches) {
            if (shouldRenderBatch(batch, camera)) {
                renderBatch(batch, shader);
            }
        }
        
        logger.trace("Frame rendered: {} draw calls, {} instances, {} batches", 
                    frameDrawCalls, frameInstancesRendered, frameBatchesRendered);
    }
    
    /**
     * Renders a single batch.
     * 
     * @param batch The batch to render
     * @param shader The shader to use
     */
    private void renderBatch(RenderBatch batch, Shader shader) {
        int visibleInstances = batch.getVisibleInstanceCount();
        if (visibleInstances == 0) {
            return;
        }
        
        batch.render(shader);
        
        // Update statistics
        frameDrawCalls++;
        frameInstancesRendered += visibleInstances;
        frameBatchesRendered++;
    }
    
    /**
     * Determines if a batch should be rendered based on culling criteria.
     * 
     * @param batch The batch to check
     * @param camera The camera for frustum culling
     * @return true if the batch should be rendered
     */
    private boolean shouldRenderBatch(RenderBatch batch, Camera camera) {
        // Always render if no camera provided
        if (camera == null) {
            return true;
        }
        
        // TODO: Implement frustum culling when camera has frustum methods
        // For now, render all batches
        return true;
    }
    
    /**
     * Updates the sorted batch list for optimal rendering order.
     */
    private void updateSortedBatches() {
        sortedBatches.clear();
        sortedBatches.addAll(batches.values());
        
        if (enableDepthSorting) {
            sortedBatches.sort(batchComparator);
        }
        
        batchesDirty = false;
        logger.debug("Updated sorted batch list: {} batches", sortedBatches.size());
    }
    
    /**
     * Creates a comparator for optimal batch rendering order.
     * 
     * @return A batch comparator
     */
    private Comparator<RenderBatch> createBatchComparator() {
        return (batch1, batch2) -> {
            Material mat1 = batch1.getMaterial();
            Material mat2 = batch2.getMaterial();
            
            // Render opaque objects first, then transparent
            boolean transparent1 = mat1.isTransparent();
            boolean transparent2 = mat2.isTransparent();
            
            if (transparent1 != transparent2) {
                return transparent1 ? 1 : -1; // Opaque first
            }
            
            // Group by material to minimize state changes
            int materialCompare = mat1.getName().compareTo(mat2.getName());
            if (materialCompare != 0) {
                return materialCompare;
            }
            
            // Group by mesh
            return Integer.compare(batch1.getMesh().hashCode(), batch2.getMesh().hashCode());
        };
    }
    
    /**
     * Clears all batches and instances.
     */
    public void clear() {
        for (RenderBatch batch : batches.values()) {
            batch.clearInstances();
        }
        logger.debug("Cleared all batch instances");
    }
    
    /**
     * Removes empty batches to free memory.
     */
    public void removeEmptyBatches() {
        Iterator<Map.Entry<String, RenderBatch>> iterator = batches.entrySet().iterator();
        int removedCount = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<String, RenderBatch> entry = iterator.next();
            RenderBatch batch = entry.getValue();
            
            if (batch.getInstanceCount() == 0) {
                batch.cleanup();
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            batchesDirty = true;
            logger.debug("Removed {} empty batches", removedCount);
        }
    }
    
    /**
     * Gets the total number of batches.
     * 
     * @return The batch count
     */
    public int getBatchCount() {
        return batches.size();
    }
    
    /**
     * Gets the total number of instances across all batches.
     * 
     * @return The total instance count
     */
    public int getTotalInstanceCount() {
        return batches.values().stream()
                     .mapToInt(RenderBatch::getInstanceCount)
                     .sum();
    }
    
    /**
     * Gets the total number of visible instances across all batches.
     * 
     * @return The total visible instance count
     */
    public int getTotalVisibleInstanceCount() {
        return batches.values().stream()
                     .mapToInt(RenderBatch::getVisibleInstanceCount)
                     .sum();
    }
    
    /**
     * Gets rendering statistics from the last frame.
     * 
     * @return A map of statistics
     */
    public Map<String, Integer> getFrameStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("drawCalls", frameDrawCalls);
        stats.put("instancesRendered", frameInstancesRendered);
        stats.put("batchesRendered", frameBatchesRendered);
        stats.put("totalBatches", getBatchCount());
        stats.put("totalInstances", getTotalInstanceCount());
        stats.put("visibleInstances", getTotalVisibleInstanceCount());
        return stats;
    }
    
    /**
     * Enables or disables frustum culling.
     * 
     * @param enabled true to enable frustum culling
     */
    public void setFrustumCullingEnabled(boolean enabled) {
        this.enableFrustumCulling = enabled;
    }
    
    /**
     * Enables or disables depth sorting for optimal rendering order.
     * 
     * @param enabled true to enable depth sorting
     */
    public void setDepthSortingEnabled(boolean enabled) {
        this.enableDepthSorting = enabled;
        this.batchesDirty = true;
    }
    
    /**
     * Cleans up all batches and releases resources.
     */
    public void cleanup() {
        logger.info("Cleaning up BatchRenderer...");
        
        for (RenderBatch batch : batches.values()) {
            batch.cleanup();
        }
        
        batches.clear();
        sortedBatches.clear();
        
        logger.info("BatchRenderer cleaned up");
    }
    
    @Override
    public String toString() {
        return String.format("BatchRenderer{batches=%d, instances=%d, visible=%d}", 
                           getBatchCount(), getTotalInstanceCount(), getTotalVisibleInstanceCount());
    }
}