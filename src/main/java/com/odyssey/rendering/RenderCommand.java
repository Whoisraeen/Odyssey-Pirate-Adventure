package com.odyssey.rendering;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single render command in The Odyssey rendering pipeline.
 * 
 * Encapsulates all the information needed to render a single object,
 * including mesh, shader, textures, and transformation matrices.
 */
public class RenderCommand {
    
    /**
     * Render queue types for sorting and batching.
     */
    public enum RenderQueue {
        OPAQUE(1000),       // Opaque objects (rendered first)
        TRANSPARENT(2000),  // Transparent objects (rendered after opaque)
        UI(3000);          // UI elements (rendered last)
        
        private final int priority;
        
        RenderQueue(int priority) {
            this.priority = priority;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    // Core rendering components
    private Mesh mesh;
    private Shader shader;
    private List<Texture> textures;
    
    // Transformation matrices
    private Matrix4f modelMatrix;
    private Vector3f worldPosition;
    
    // Rendering properties
    private RenderQueue renderQueue;
    private int sortingOrder;
    private float distanceFromCamera;
    
    // Material properties
    private Vector3f color;
    private float alpha;
    private boolean castShadows;
    private boolean receiveShadows;
    
    // Culling and visibility
    private boolean visible;
    private float boundingRadius;
    
    /**
     * Create a new render command.
     */
    public RenderCommand() {
        this.textures = new ArrayList<>();
        this.modelMatrix = new Matrix4f();
        this.worldPosition = new Vector3f();
        this.color = new Vector3f(1.0f, 1.0f, 1.0f);
        this.alpha = 1.0f;
        this.renderQueue = RenderQueue.OPAQUE;
        this.sortingOrder = 0;
        this.visible = true;
        this.castShadows = true;
        this.receiveShadows = true;
        this.boundingRadius = 1.0f;
    }
    
    /**
     * Create a render command with basic components.
     */
    public RenderCommand(Mesh mesh, Shader shader, Matrix4f modelMatrix) {
        this();
        this.mesh = mesh;
        this.shader = shader;
        this.modelMatrix.set(modelMatrix);
        
        // Extract world position from model matrix
        modelMatrix.getTranslation(worldPosition);
    }
    
    /**
     * Create a render command with mesh, shader, and texture.
     */
    public RenderCommand(Mesh mesh, Shader shader, Texture texture, Matrix4f modelMatrix) {
        this(mesh, shader, modelMatrix);
        if (texture != null) {
            this.textures.add(texture);
        }
    }
    
    /**
     * Set the mesh to render.
     */
    public RenderCommand setMesh(Mesh mesh) {
        this.mesh = mesh;
        return this;
    }
    
    /**
     * Get the mesh.
     */
    public Mesh getMesh() {
        return mesh;
    }
    
    /**
     * Set the shader to use.
     */
    public RenderCommand setShader(Shader shader) {
        this.shader = shader;
        return this;
    }
    
    /**
     * Get the shader.
     */
    public Shader getShader() {
        return shader;
    }
    
    /**
     * Add a texture.
     */
    public RenderCommand addTexture(Texture texture) {
        if (texture != null) {
            textures.add(texture);
        }
        return this;
    }
    
    /**
     * Set textures (replaces existing list).
     */
    public RenderCommand setTextures(List<Texture> textures) {
        this.textures.clear();
        if (textures != null) {
            this.textures.addAll(textures);
        }
        return this;
    }
    
    /**
     * Get textures list.
     */
    public List<Texture> getTextures() {
        return textures;
    }
    
    /**
     * Set model matrix.
     */
    public RenderCommand setModelMatrix(Matrix4f modelMatrix) {
        this.modelMatrix.set(modelMatrix);
        
        // Update world position
        modelMatrix.getTranslation(worldPosition);
        
        return this;
    }
    
    /**
     * Get model matrix.
     */
    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }
    
    /**
     * Get world position.
     */
    public Vector3f getWorldPosition() {
        return worldPosition;
    }
    
    /**
     * Set world position and update model matrix.
     */
    public RenderCommand setWorldPosition(Vector3f position) {
        this.worldPosition.set(position);
        this.modelMatrix.setTranslation(position);
        return this;
    }
    
    /**
     * Set world position and update model matrix.
     */
    public RenderCommand setWorldPosition(float x, float y, float z) {
        this.worldPosition.set(x, y, z);
        this.modelMatrix.setTranslation(x, y, z);
        return this;
    }
    
    /**
     * Set render queue.
     */
    public RenderCommand setRenderQueue(RenderQueue queue) {
        this.renderQueue = queue;
        return this;
    }
    
    /**
     * Get render queue.
     */
    public RenderQueue getRenderQueue() {
        return renderQueue;
    }
    
    /**
     * Set sorting order within the render queue.
     */
    public RenderCommand setSortingOrder(int order) {
        this.sortingOrder = order;
        return this;
    }
    
    /**
     * Get sorting order.
     */
    public int getSortingOrder() {
        return sortingOrder;
    }
    
    /**
     * Set distance from camera (used for sorting).
     */
    public RenderCommand setDistanceFromCamera(float distance) {
        this.distanceFromCamera = distance;
        return this;
    }
    
    /**
     * Get distance from camera.
     */
    public float getDistanceFromCamera() {
        return distanceFromCamera;
    }
    
    /**
     * Set color tint.
     */
    public RenderCommand setColor(Vector3f color) {
        this.color.set(color);
        return this;
    }
    
    /**
     * Set color tint.
     */
    public RenderCommand setColor(float r, float g, float b) {
        this.color.set(r, g, b);
        return this;
    }
    
    /**
     * Get color tint.
     */
    public Vector3f getColor() {
        return color;
    }
    
    /**
     * Set alpha transparency.
     */
    public RenderCommand setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        
        // Automatically set render queue based on alpha
        if (this.alpha < 1.0f && renderQueue == RenderQueue.OPAQUE) {
            renderQueue = RenderQueue.TRANSPARENT;
        }
        
        return this;
    }
    
    /**
     * Get alpha transparency.
     */
    public float getAlpha() {
        return alpha;
    }
    
    /**
     * Set whether this object casts shadows.
     */
    public RenderCommand setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }
    
    /**
     * Check if this object casts shadows.
     */
    public boolean getCastShadows() {
        return castShadows;
    }
    
    /**
     * Set whether this object receives shadows.
     */
    public RenderCommand setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }
    
    /**
     * Check if this object receives shadows.
     */
    public boolean getReceiveShadows() {
        return receiveShadows;
    }
    
    /**
     * Set visibility.
     */
    public RenderCommand setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    /**
     * Check if visible.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Set bounding radius for culling.
     */
    public RenderCommand setBoundingRadius(float radius) {
        this.boundingRadius = Math.max(0.0f, radius);
        return this;
    }
    
    /**
     * Get bounding radius.
     */
    public float getBoundingRadius() {
        return boundingRadius;
    }
    
    /**
     * Check if this render command is valid for rendering.
     */
    public boolean isValid() {
        return visible && mesh != null && shader != null;
    }
    
    /**
     * Calculate distance from camera and update internal value.
     */
    public void updateDistanceFromCamera(Vector3f cameraPosition) {
        this.distanceFromCamera = cameraPosition.distance(worldPosition);
    }
    
    /**
     * Check if object should be culled based on distance.
     */
    public boolean shouldCull(Vector3f cameraPosition, float maxDistance) {
        float distance = cameraPosition.distance(worldPosition);
        return distance > (maxDistance + boundingRadius);
    }
    
    /**
     * Create a copy of this render command.
     */
    public RenderCommand copy() {
        RenderCommand copy = new RenderCommand();
        
        copy.mesh = this.mesh;
        copy.shader = this.shader;
        copy.textures = new ArrayList<>(this.textures);
        copy.modelMatrix.set(this.modelMatrix);
        copy.worldPosition.set(this.worldPosition);
        copy.renderQueue = this.renderQueue;
        copy.sortingOrder = this.sortingOrder;
        copy.distanceFromCamera = this.distanceFromCamera;
        copy.color.set(this.color);
        copy.alpha = this.alpha;
        copy.castShadows = this.castShadows;
        copy.receiveShadows = this.receiveShadows;
        copy.visible = this.visible;
        copy.boundingRadius = this.boundingRadius;
        
        return copy;
    }
    
    /**
     * Reset render command to default state.
     */
    public void reset() {
        mesh = null;
        shader = null;
        textures.clear();
        modelMatrix.identity();
        worldPosition.set(0.0f);
        renderQueue = RenderQueue.OPAQUE;
        sortingOrder = 0;
        distanceFromCamera = 0.0f;
        color.set(1.0f, 1.0f, 1.0f);
        alpha = 1.0f;
        castShadows = true;
        receiveShadows = true;
        visible = true;
        boundingRadius = 1.0f;
    }
    
    /**
     * Builder pattern for fluent API.
     */
    public static class Builder {
        private RenderCommand command;
        
        public Builder() {
            this.command = new RenderCommand();
        }
        
        public Builder mesh(Mesh mesh) {
            command.setMesh(mesh);
            return this;
        }
        
        public Builder shader(Shader shader) {
            command.setShader(shader);
            return this;
        }
        
        public Builder texture(Texture texture) {
            command.addTexture(texture);
            return this;
        }
        
        public Builder modelMatrix(Matrix4f matrix) {
            command.setModelMatrix(matrix);
            return this;
        }
        
        public Builder position(Vector3f position) {
            command.setWorldPosition(position);
            return this;
        }
        
        public Builder position(float x, float y, float z) {
            command.setWorldPosition(x, y, z);
            return this;
        }
        
        public Builder queue(RenderQueue queue) {
            command.setRenderQueue(queue);
            return this;
        }
        
        public Builder color(Vector3f color) {
            command.setColor(color);
            return this;
        }
        
        public Builder color(float r, float g, float b) {
            command.setColor(r, g, b);
            return this;
        }
        
        public Builder alpha(float alpha) {
            command.setAlpha(alpha);
            return this;
        }
        
        public Builder shadows(boolean cast, boolean receive) {
            command.setCastShadows(cast);
            command.setReceiveShadows(receive);
            return this;
        }
        
        public Builder visible(boolean visible) {
            command.setVisible(visible);
            return this;
        }
        
        public RenderCommand build() {
            return command;
        }
    }
    
    @Override
    public String toString() {
        return String.format("RenderCommand{mesh=%s, shader=%s, queue=%s, pos=(%.2f,%.2f,%.2f), visible=%s}",
                mesh != null ? mesh.getClass().getSimpleName() : "null",
                shader != null ? shader.getName() : "null",
                renderQueue,
                worldPosition.x, worldPosition.y, worldPosition.z,
                visible);
    }
}