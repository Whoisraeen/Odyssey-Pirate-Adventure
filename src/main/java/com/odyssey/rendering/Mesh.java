package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents a 3D mesh for The Odyssey rendering system.
 * 
 * Handles vertex data, VAO/VBO management, and rendering operations.
 * Supports various vertex attributes and rendering modes.
 */
public class Mesh {
    
    private static final Logger logger = Logger.getLogger(Mesh.class);
    
    // OpenGL objects
    private int vao; // Vertex Array Object
    private int vbo; // Vertex Buffer Object
    private int ebo; // Element Buffer Object (indices)
    
    // Mesh data
    private float[] vertices;
    private int[] indices;
    private int vertexCount;
    private int indexCount;
    private String name;
    
    // Vertex attributes
    private List<VertexAttribute> attributes;
    private int stride;
    
    // Rendering properties
    private int drawMode = GL_TRIANGLES;
    private boolean useIndices = false;
    
    /**
     * Vertex attribute descriptor.
     */
    public static class VertexAttribute {
        public final int location;
        public final int size;
        public final int type;
        public final boolean normalized;
        public final int offset;
        
        public VertexAttribute(int location, int size, int type, boolean normalized, int offset) {
            this.location = location;
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.offset = offset;
        }
        
        public static VertexAttribute position(int location, int offset) {
            return new VertexAttribute(location, 3, GL_FLOAT, false, offset);
        }
        
        public static VertexAttribute normal(int location, int offset) {
            return new VertexAttribute(location, 3, GL_FLOAT, false, offset);
        }
        
        public static VertexAttribute texCoord(int location, int offset) {
            return new VertexAttribute(location, 2, GL_FLOAT, false, offset);
        }
        
        public static VertexAttribute color(int location, int offset) {
            return new VertexAttribute(location, 4, GL_FLOAT, false, offset);
        }
        
        public static VertexAttribute tangent(int location, int offset) {
            return new VertexAttribute(location, 3, GL_FLOAT, false, offset);
        }
    }
    
    /**
     * Mesh builder for easy mesh construction.
     */
    public static class Builder {
        private List<Float> vertices = new ArrayList<>();
        private List<Integer> indices = new ArrayList<>();
        private List<VertexAttribute> attributes = new ArrayList<>();
        private String name = "Mesh";
        private int drawMode = GL_TRIANGLES;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder drawMode(int drawMode) {
            this.drawMode = drawMode;
            return this;
        }
        
        public Builder addVertex(float... values) {
            for (float value : values) {
                vertices.add(value);
            }
            return this;
        }
        
        public Builder addVertex(Vector3f position) {
            vertices.add(position.x);
            vertices.add(position.y);
            vertices.add(position.z);
            return this;
        }
        
        public Builder addVertex(Vector3f position, Vector3f normal) {
            vertices.add(position.x);
            vertices.add(position.y);
            vertices.add(position.z);
            vertices.add(normal.x);
            vertices.add(normal.y);
            vertices.add(normal.z);
            return this;
        }
        
        public Builder addVertex(Vector3f position, Vector3f normal, Vector2f texCoord) {
            vertices.add(position.x);
            vertices.add(position.y);
            vertices.add(position.z);
            vertices.add(normal.x);
            vertices.add(normal.y);
            vertices.add(normal.z);
            vertices.add(texCoord.x);
            vertices.add(texCoord.y);
            return this;
        }
        
        public Builder addIndex(int... indices) {
            for (int index : indices) {
                this.indices.add(index);
            }
            return this;
        }
        
        public Builder addTriangle(int i0, int i1, int i2) {
            indices.add(i0);
            indices.add(i1);
            indices.add(i2);
            return this;
        }
        
        public Builder addQuad(int i0, int i1, int i2, int i3) {
            // First triangle
            indices.add(i0);
            indices.add(i1);
            indices.add(i2);
            // Second triangle
            indices.add(i0);
            indices.add(i2);
            indices.add(i3);
            return this;
        }
        
        public Builder addAttribute(VertexAttribute attribute) {
            attributes.add(attribute);
            return this;
        }
        
        public Builder addPositionAttribute(int location, int offset) {
            return addAttribute(VertexAttribute.position(location, offset));
        }
        
        public Builder addNormalAttribute(int location, int offset) {
            return addAttribute(VertexAttribute.normal(location, offset));
        }
        
        public Builder addTexCoordAttribute(int location, int offset) {
            return addAttribute(VertexAttribute.texCoord(location, offset));
        }
        
        public Builder addColorAttribute(int location, int offset) {
            return addAttribute(VertexAttribute.color(location, offset));
        }
        
        public Mesh build() {
            float[] vertexArray = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                vertexArray[i] = vertices.get(i);
            }
            
            int[] indexArray = null;
            if (!indices.isEmpty()) {
                indexArray = new int[indices.size()];
                for (int i = 0; i < indices.size(); i++) {
                    indexArray[i] = indices.get(i);
                }
            }
            
            Mesh mesh = new Mesh(name, vertexArray, indexArray);
            mesh.drawMode = drawMode;
            
            for (VertexAttribute attribute : attributes) {
                mesh.addAttribute(attribute);
            }
            
            mesh.upload();
            return mesh;
        }
    }
    
    /**
     * Create mesh with vertices only.
     */
    public Mesh(String name, float[] vertices) {
        this(name, vertices, null);
    }
    
    /**
     * Create mesh with vertices and indices.
     */
    public Mesh(String name, float[] vertices, int[] indices) {
        this.name = name;
        this.vertices = vertices;
        this.indices = indices;
        this.vertexCount = vertices.length;
        this.indexCount = indices != null ? indices.length : 0;
        this.useIndices = indices != null;
        this.attributes = new ArrayList<>();
        
        // Generate OpenGL objects
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();
        if (useIndices) {
            this.ebo = glGenBuffers();
        }
        
        logger.debug("Created mesh '{}' with {} vertices and {} indices", name, vertexCount, indexCount);
    }
    
    /**
     * Add vertex attribute.
     */
    public void addAttribute(VertexAttribute attribute) {
        attributes.add(attribute);
        
        // Calculate stride if this is the first attribute or update it
        if (attributes.size() == 1) {
            stride = attribute.size * Float.BYTES;
        } else {
            // Find the maximum offset + size to determine stride
            int maxEnd = 0;
            for (VertexAttribute attr : attributes) {
                int end = attr.offset + attr.size * Float.BYTES;
                if (end > maxEnd) {
                    maxEnd = end;
                }
            }
            stride = maxEnd;
        }
    }
    
    /**
     * Add standard vertex attributes (position, normal, texCoord).
     */
    public void addStandardAttributes() {
        addAttribute(VertexAttribute.position(0, 0));
        addAttribute(VertexAttribute.normal(1, 3 * Float.BYTES));
        addAttribute(VertexAttribute.texCoord(2, 6 * Float.BYTES));
        stride = 8 * Float.BYTES; // 3 + 3 + 2 floats
    }
    
    /**
     * Upload mesh data to GPU.
     */
    public void upload() {
        glBindVertexArray(vao);
        
        // Upload vertex data
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        // Upload index data if present
        if (useIndices) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
            indexBuffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        }
        
        // Set up vertex attributes
        for (VertexAttribute attribute : attributes) {
            glEnableVertexAttribArray(attribute.location);
            glVertexAttribPointer(attribute.location, attribute.size, attribute.type, 
                                attribute.normalized, stride, attribute.offset);
        }
        
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        if (useIndices) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        }
        
        logger.debug("Uploaded mesh '{}' to GPU (VAO: {}, VBO: {}, EBO: {})", name, vao, vbo, ebo);
    }
    
    /**
     * Update vertex data.
     */
    public void updateVertices(float[] newVertices) {
        this.vertices = newVertices;
        this.vertexCount = newVertices.length;
        
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * Update index data.
     */
    public void updateIndices(int[] newIndices) {
        if (!useIndices) {
            logger.warn("Cannot update indices for mesh '{}': no EBO created", name);
            return;
        }
        
        this.indices = newIndices;
        this.indexCount = newIndices.length;
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }
    
    /**
     * Render the mesh.
     */
    public void render() {
        glBindVertexArray(vao);
        
        if (useIndices) {
            glDrawElements(drawMode, indexCount, GL_UNSIGNED_INT, 0);
        } else {
            int verticesPerElement = stride / Float.BYTES;
            glDrawArrays(drawMode, 0, vertexCount / verticesPerElement);
        }
        
        glBindVertexArray(0);
    }
    
    /**
     * Render multiple instances of the mesh.
     */
    public void renderInstanced(int instanceCount) {
        glBindVertexArray(vao);
        
        if (useIndices) {
            glDrawElements(drawMode, indexCount, GL_UNSIGNED_INT, 0);
        } else {
            int verticesPerElement = stride / Float.BYTES;
            glDrawArrays(drawMode, 0, vertexCount / verticesPerElement);
        }
        
        glBindVertexArray(0);
    }
    
    /**
     * Create a cube mesh.
     */
    public static Mesh createCube(String name, float size) {
        float half = size * 0.5f;
        
        return new Builder()
            .name(name)
            // Front face
            .addVertex(-half, -half,  half,  0.0f,  0.0f,  1.0f, 0.0f, 0.0f)
            .addVertex( half, -half,  half,  0.0f,  0.0f,  1.0f, 1.0f, 0.0f)
            .addVertex( half,  half,  half,  0.0f,  0.0f,  1.0f, 1.0f, 1.0f)
            .addVertex(-half,  half,  half,  0.0f,  0.0f,  1.0f, 0.0f, 1.0f)
            // Back face
            .addVertex(-half, -half, -half,  0.0f,  0.0f, -1.0f, 1.0f, 0.0f)
            .addVertex(-half,  half, -half,  0.0f,  0.0f, -1.0f, 1.0f, 1.0f)
            .addVertex( half,  half, -half,  0.0f,  0.0f, -1.0f, 0.0f, 1.0f)
            .addVertex( half, -half, -half,  0.0f,  0.0f, -1.0f, 0.0f, 0.0f)
            // Left face
            .addVertex(-half,  half,  half, -1.0f,  0.0f,  0.0f, 1.0f, 0.0f)
            .addVertex(-half,  half, -half, -1.0f,  0.0f,  0.0f, 1.0f, 1.0f)
            .addVertex(-half, -half, -half, -1.0f,  0.0f,  0.0f, 0.0f, 1.0f)
            .addVertex(-half, -half,  half, -1.0f,  0.0f,  0.0f, 0.0f, 0.0f)
            // Right face
            .addVertex( half,  half,  half,  1.0f,  0.0f,  0.0f, 1.0f, 0.0f)
            .addVertex( half, -half,  half,  1.0f,  0.0f,  0.0f, 0.0f, 0.0f)
            .addVertex( half, -half, -half,  1.0f,  0.0f,  0.0f, 0.0f, 1.0f)
            .addVertex( half,  half, -half,  1.0f,  0.0f,  0.0f, 1.0f, 1.0f)
            // Bottom face
            .addVertex(-half, -half, -half,  0.0f, -1.0f,  0.0f, 0.0f, 1.0f)
            .addVertex( half, -half, -half,  0.0f, -1.0f,  0.0f, 1.0f, 1.0f)
            .addVertex( half, -half,  half,  0.0f, -1.0f,  0.0f, 1.0f, 0.0f)
            .addVertex(-half, -half,  half,  0.0f, -1.0f,  0.0f, 0.0f, 0.0f)
            // Top face
            .addVertex(-half,  half, -half,  0.0f,  1.0f,  0.0f, 0.0f, 1.0f)
            .addVertex(-half,  half,  half,  0.0f,  1.0f,  0.0f, 0.0f, 0.0f)
            .addVertex( half,  half,  half,  0.0f,  1.0f,  0.0f, 1.0f, 0.0f)
            .addVertex( half,  half, -half,  0.0f,  1.0f,  0.0f, 1.0f, 1.0f)
            // Indices
            .addQuad(0, 1, 2, 3)     // Front
            .addQuad(4, 5, 6, 7)     // Back
            .addQuad(8, 9, 10, 11)   // Left
            .addQuad(12, 13, 14, 15) // Right
            .addQuad(16, 17, 18, 19) // Bottom
            .addQuad(20, 21, 22, 23) // Top
            // Attributes
            .addPositionAttribute(0, 0)
            .addNormalAttribute(1, 3 * Float.BYTES)
            .addTexCoordAttribute(2, 6 * Float.BYTES)
            .build();
    }
    
    /**
     * Create a plane mesh.
     */
    public static Mesh createPlane(String name, float width, float height) {
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;
        
        return new Builder()
            .name(name)
            .addVertex(-halfWidth, 0.0f, -halfHeight, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            .addVertex( halfWidth, 0.0f, -halfHeight, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)
            .addVertex( halfWidth, 0.0f,  halfHeight, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f)
            .addVertex(-halfWidth, 0.0f,  halfHeight, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
            .addQuad(0, 1, 2, 3)
            .addPositionAttribute(0, 0)
            .addNormalAttribute(1, 3 * Float.BYTES)
            .addTexCoordAttribute(2, 6 * Float.BYTES)
            .build();
    }
    
    /**
     * Create a sphere mesh.
     */
    public static Mesh createSphere(String name, float radius, int segments) {
        Builder builder = new Builder().name(name);
        
        // Generate vertices
        for (int i = 0; i <= segments; i++) {
            float phi = (float) Math.PI * i / segments;
            for (int j = 0; j <= segments; j++) {
                float theta = 2.0f * (float) Math.PI * j / segments;
                
                float x = radius * (float) (Math.sin(phi) * Math.cos(theta));
                float y = radius * (float) Math.cos(phi);
                float z = radius * (float) (Math.sin(phi) * Math.sin(theta));
                
                float nx = x / radius;
                float ny = y / radius;
                float nz = z / radius;
                
                float u = (float) j / segments;
                float v = (float) i / segments;
                
                builder.addVertex(x, y, z, nx, ny, nz, u, v);
            }
        }
        
        // Generate indices
        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < segments; j++) {
                int first = i * (segments + 1) + j;
                int second = first + segments + 1;
                
                builder.addTriangle(first, second, first + 1);
                builder.addTriangle(second, second + 1, first + 1);
            }
        }
        
        return builder
            .addPositionAttribute(0, 0)
            .addNormalAttribute(1, 3 * Float.BYTES)
            .addTexCoordAttribute(2, 6 * Float.BYTES)
            .build();
    }
    
    /**
     * Create a screen quad for post-processing.
     */
    public static Mesh createScreenQuad(String name) {
        return new Builder()
            .name(name)
            .addVertex(-1.0f, -1.0f, 0.0f, 1.0f)
            .addVertex( 1.0f, -1.0f, 1.0f, 1.0f)
            .addVertex( 1.0f,  1.0f, 1.0f, 0.0f)
            .addVertex(-1.0f,  1.0f, 0.0f, 0.0f)
            .addQuad(0, 1, 2, 3)
            .addAttribute(new VertexAttribute(0, 2, GL_FLOAT, false, 0))
            .addAttribute(new VertexAttribute(1, 2, GL_FLOAT, false, 2 * Float.BYTES))
            .build();
    }
    
    // Getters
    
    public int getVAO() {
        return vao;
    }
    
    public int getVBO() {
        return vbo;
    }
    
    public int getEBO() {
        return ebo;
    }
    
    public String getName() {
        return name;
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public int getIndexCount() {
        return indexCount;
    }
    
    public boolean usesIndices() {
        return useIndices;
    }
    
    public int getDrawMode() {
        return drawMode;
    }
    
    public void setDrawMode(int drawMode) {
        this.drawMode = drawMode;
    }
    
    public int getStride() {
        return stride;
    }
    
    public List<VertexAttribute> getAttributes() {
        return new ArrayList<>(attributes);
    }
    
    /**
     * Check if mesh is valid.
     */
    public boolean isValid() {
        return vao != 0 && vbo != 0 && glIsVertexArray(vao) && glIsBuffer(vbo);
    }
    
    /**
     * Get memory usage estimate in bytes.
     */
    public long getMemoryUsage() {
        long vertexMemory = (long) vertices.length * Float.BYTES;
        long indexMemory = useIndices ? (long) indices.length * Integer.BYTES : 0;
        return vertexMemory + indexMemory;
    }
    
    /**
     * Cleanup mesh resources.
     */
    public void cleanup() {
        if (vao != 0) {
            glDeleteVertexArrays(vao);
            vao = 0;
        }
        
        if (vbo != 0) {
            glDeleteBuffers(vbo);
            vbo = 0;
        }
        
        if (ebo != 0) {
            glDeleteBuffers(ebo);
            ebo = 0;
        }
        
        logger.debug("Cleaned up mesh: {}", name);
    }
    
    @Override
    public String toString() {
        return String.format("Mesh{name='%s', vertices=%d, indices=%d, vao=%d}", 
                           name, vertexCount, indexCount, vao);
    }
}