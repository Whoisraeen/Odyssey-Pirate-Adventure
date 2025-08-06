package com.odyssey.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Represents a batch of renderable objects that share the same material and mesh.
 * Enables efficient instanced rendering to reduce draw calls and improve performance.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class RenderBatch {
    private static final Logger logger = LoggerFactory.getLogger(RenderBatch.class);
    
    // Maximum number of instances per batch
    private static final int MAX_INSTANCES = 1000;
    
    // Batch properties
    private final Material material;
    private final Mesh mesh;
    private final String batchId;
    
    // Instance data
    private final List<RenderInstance> instances;
    private final float[] instanceMatrices;
    private boolean dirty;
    
    // OpenGL resources
    private int instanceVBO;
    private boolean initialized;
    
    /**
     * Represents a single instance in the batch.
     */
    public static class RenderInstance {
        public Matrix4f transform;
        public Vector3f color;
        public float alpha;
        public boolean visible;
        
        public RenderInstance() {
            this.transform = new Matrix4f();
            this.color = new Vector3f(1.0f, 1.0f, 1.0f);
            this.alpha = 1.0f;
            this.visible = true;
        }
        
        public RenderInstance(Matrix4f transform) {
            this();
            this.transform.set(transform);
        }
        
        public RenderInstance(Vector3f position, Vector3f scale, Vector3f rotation) {
            this();
            this.transform.identity()
                         .translate(position)
                         .rotateXYZ(rotation.x, rotation.y, rotation.z)
                         .scale(scale);
        }
    }
    
    /**
     * Creates a new render batch.
     * 
     * @param material The material for all instances in this batch
     * @param mesh The mesh for all instances in this batch
     */
    public RenderBatch(Material material, Mesh mesh) {
        this.material = material;
        this.mesh = mesh;
        this.batchId = generateBatchId(material, mesh);
        
        this.instances = new ArrayList<>();
        this.instanceMatrices = new float[MAX_INSTANCES * 16]; // 4x4 matrix per instance
        this.dirty = true;
        this.initialized = false;
        
        logger.debug("Created render batch: {}", batchId);
    }
    
    /**
     * Initializes OpenGL resources for instanced rendering.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        // Create instance VBO for transformation matrices
        instanceVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, instanceMatrices.length * Float.BYTES, GL_DYNAMIC_DRAW);
        
        // Set up instanced vertex attributes (bind mesh VAO manually)
        glBindVertexArray(mesh.getVaoId());
        
        // Matrix attribute (takes 4 attribute locations)
        for (int i = 0; i < 4; i++) {
            int location = 3 + i; // Assuming positions, normals, texCoords use 0-2
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, 4, GL_FLOAT, false, 16 * Float.BYTES, i * 4 * Float.BYTES);
            glVertexAttribDivisor(location, 1); // One per instance
        }
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        initialized = true;
        logger.debug("Initialized render batch: {}", batchId);
    }
    
    /**
     * Adds an instance to this batch.
     * 
     * @param instance The instance to add
     * @return true if added successfully, false if batch is full
     */
    public boolean addInstance(RenderInstance instance) {
        if (instances.size() >= MAX_INSTANCES) {
            logger.warn("Render batch {} is full, cannot add more instances", batchId);
            return false;
        }
        
        instances.add(instance);
        dirty = true;
        return true;
    }
    
    /**
     * Removes an instance from this batch.
     * 
     * @param instance The instance to remove
     * @return true if removed successfully
     */
    public boolean removeInstance(RenderInstance instance) {
        boolean removed = instances.remove(instance);
        if (removed) {
            dirty = true;
        }
        return removed;
    }
    
    /**
     * Clears all instances from this batch.
     */
    public void clearInstances() {
        instances.clear();
        dirty = true;
    }
    
    /**
     * Updates the instance data if it has changed.
     */
    public void updateInstanceData() {
        if (!dirty || !initialized) {
            return;
        }
        
        // Pack visible instances into the matrix array
        int matrixIndex = 0;
        for (RenderInstance instance : instances) {
            if (instance.visible && matrixIndex < MAX_INSTANCES) {
                // Copy matrix data
                instance.transform.get(instanceMatrices, matrixIndex * 16);
                matrixIndex++;
            }
        }
        
        // Upload to GPU
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferSubData(GL_ARRAY_BUFFER, 0, instanceMatrices);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        dirty = false;
    }
    
    /**
     * Renders all instances in this batch.
     * 
     * @param shader The shader to use for rendering
     */
    public void render(Shader shader) {
        if (!initialized) {
            initialize();
        }
        
        int visibleCount = getVisibleInstanceCount();
        if (visibleCount == 0) {
            return;
        }
        
        // Update instance data if needed
        updateInstanceData();
        
        // Bind material
        material.bind(shader);
        
        // Render instances using mesh's built-in VAO binding
        if (visibleCount == 1) {
            // Single instance - use regular draw call
            mesh.render();
        } else {
            // Multiple instances - use instanced draw call with manual VAO binding
            glBindVertexArray(mesh.getVaoId());
            glDrawElementsInstanced(GL_TRIANGLES, mesh.getIndexCount(), GL_UNSIGNED_INT, 0, visibleCount);
            glBindVertexArray(0);
        }
    }
    
    /**
     * Gets the number of visible instances in this batch.
     * 
     * @return The visible instance count
     */
    public int getVisibleInstanceCount() {
        int count = 0;
        for (RenderInstance instance : instances) {
            if (instance.visible) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the total number of instances in this batch.
     * 
     * @return The total instance count
     */
    public int getInstanceCount() {
        return instances.size();
    }
    
    /**
     * Checks if this batch can accept more instances.
     * 
     * @return true if there's space for more instances
     */
    public boolean hasSpace() {
        return instances.size() < MAX_INSTANCES;
    }
    
    /**
     * Checks if this batch is compatible with the given material and mesh.
     * 
     * @param material The material to check
     * @param mesh The mesh to check
     * @return true if compatible
     */
    public boolean isCompatible(Material material, Mesh mesh) {
        return this.material.equals(material) && this.mesh.equals(mesh);
    }
    
    /**
     * Gets the material used by this batch.
     * 
     * @return The material
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Gets the mesh used by this batch.
     * 
     * @return The mesh
     */
    public Mesh getMesh() {
        return mesh;
    }
    
    /**
     * Gets the batch ID.
     * 
     * @return The batch ID
     */
    public String getBatchId() {
        return batchId;
    }
    
    /**
     * Gets all instances in this batch.
     * 
     * @return A list of instances
     */
    public List<RenderInstance> getInstances() {
        return new ArrayList<>(instances);
    }
    
    /**
     * Marks the batch as dirty, requiring an update.
     */
    public void markDirty() {
        dirty = true;
    }
    
    /**
     * Checks if the batch needs updating.
     * 
     * @return true if the batch is dirty
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (initialized && instanceVBO != 0) {
            glDeleteBuffers(instanceVBO);
            instanceVBO = 0;
        }
        
        instances.clear();
        initialized = false;
        
        logger.debug("Cleaned up render batch: {}", batchId);
    }
    
    /**
     * Generates a unique batch ID based on material and mesh.
     * 
     * @param material The material
     * @param mesh The mesh
     * @return A unique batch ID
     */
    private static String generateBatchId(Material material, Mesh mesh) {
        return String.format("batch_%s_%d", material.getName(), mesh.hashCode());
    }
    
    @Override
    public String toString() {
        return String.format("RenderBatch{id='%s', material='%s', instances=%d/%d, visible=%d}", 
                           batchId, material.getName(), instances.size(), MAX_INSTANCES, getVisibleInstanceCount());
    }
}