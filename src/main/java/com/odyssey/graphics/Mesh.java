package com.odyssey.graphics;

import com.odyssey.core.memory.MemoryManager;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh class for rendering geometry in The Odyssey.
 * Handles vertex data, indices, and OpenGL buffer objects.
 */
public class Mesh {
    private static final Logger logger = LoggerFactory.getLogger(Mesh.class);
    
    private int vaoId;
    private int vboId;
    private int eboId;
    private int vertexCount;
    private int indexCount;
    
    private FloatBuffer verticesBuffer;
    private IntBuffer indicesBuffer;
    private MemoryManager memoryManager;
    private MemoryManager.MeshData meshData;
    
    public Mesh(float[] vertices, int[] indices) {
        this(vertices, indices, null);
    }
    
    public Mesh(float[] vertices, int[] indices, MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        this.vertexCount = vertices.length / 8; // 3 pos + 3 normal + 2 texCoord
        this.indexCount = indices.length;
        
        // Use memory pools if available, otherwise fallback to direct allocation
        if (memoryManager != null) {
            meshData = memoryManager.acquireMeshData();
            
            // Ensure arrays are large enough
            if (meshData.vertices.length < vertices.length) {
                meshData.vertices = new float[vertices.length];
            }
            if (meshData.indices.length < indices.length) {
                meshData.indices = new int[indices.length];
            }
            
            // Copy data
            System.arraycopy(vertices, 0, meshData.vertices, 0, vertices.length);
            System.arraycopy(indices, 0, meshData.indices, 0, indices.length);
            meshData.vertexCount = vertices.length;
            meshData.indexCount = indices.length;
            
            // Create OpenGL buffers from pooled data
            verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
            verticesBuffer.put(vertices).flip();
            
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
        } else {
            // Fallback to direct allocation
            verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
            verticesBuffer.put(vertices).flip();
            
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
        }
        
        // Generate VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        // Generate VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Normal attribute (location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Texture coordinate attribute (location = 2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);
        
        // Generate EBO
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        
        // Unbind
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        
        logger.debug("Created mesh with {} vertices and {} indices", vertexCount, indexCount);
    }
    
    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        // Bind VAO first to ensure proper cleanup context
        glBindVertexArray(vaoId);
        
        // Disable vertex attributes
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        
        // Unbind and delete buffers in proper order
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        if (vboId != 0) {
            glDeleteBuffers(vboId);
            vboId = 0;
        }
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        if (eboId != 0) {
            glDeleteBuffers(eboId);
            eboId = 0;
        }
        
        // Unbind and delete VAO
        glBindVertexArray(0);
        if (vaoId != 0) {
            glDeleteVertexArrays(vaoId);
            vaoId = 0;
        }
        
        // Free memory buffers
        if (verticesBuffer != null) {
            MemoryUtil.memFree(verticesBuffer);
            verticesBuffer = null;
        }
        if (indicesBuffer != null) {
            MemoryUtil.memFree(indicesBuffer);
            indicesBuffer = null;
        }
        
        // Return mesh data to pool if using memory manager
        if (memoryManager != null && meshData != null) {
            memoryManager.releaseMeshData(meshData);
            meshData = null;
        }
        
        // Check for OpenGL errors after cleanup
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            logger.warn("OpenGL error during mesh cleanup: {}", error);
        }
        
        logger.debug("Cleaned up mesh resources (VAO: {}, VBO: {}, EBO: {})", vaoId, vboId, eboId);
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public int getIndexCount() {
        return indexCount;
    }
    
    public int getVaoId() {
        return vaoId;
    }
    
    // Static factory methods for common shapes
    public static Mesh createCube() {
        float[] vertices = {
            // Front face
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  0.0f, 0.0f, // Bottom-left
             0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  1.0f, 0.0f, // Bottom-right
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  1.0f, 1.0f, // Top-right
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,  0.0f, 1.0f, // Top-left
            
            // Back face
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  1.0f, 0.0f, // Bottom-left
             0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  0.0f, 0.0f, // Bottom-right
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  0.0f, 1.0f, // Top-right
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,  1.0f, 1.0f, // Top-left
            
            // Left face
            -0.5f, -0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  0.0f, 0.0f, // Bottom-left
            -0.5f, -0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  1.0f, 0.0f, // Bottom-right
            -0.5f,  0.5f,  0.5f, -1.0f,  0.0f,  0.0f,  1.0f, 1.0f, // Top-right
            -0.5f,  0.5f, -0.5f, -1.0f,  0.0f,  0.0f,  0.0f, 1.0f, // Top-left
            
            // Right face
             0.5f, -0.5f, -0.5f,  1.0f,  0.0f,  0.0f,  1.0f, 0.0f, // Bottom-left
             0.5f, -0.5f,  0.5f,  1.0f,  0.0f,  0.0f,  0.0f, 0.0f, // Bottom-right
             0.5f,  0.5f,  0.5f,  1.0f,  0.0f,  0.0f,  0.0f, 1.0f, // Top-right
             0.5f,  0.5f, -0.5f,  1.0f,  0.0f,  0.0f,  1.0f, 1.0f, // Top-left
            
            // Top face
            -0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,  0.0f, 1.0f, // Bottom-left
             0.5f,  0.5f, -0.5f,  0.0f,  1.0f,  0.0f,  1.0f, 1.0f, // Bottom-right
             0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  1.0f, 0.0f, // Top-right
            -0.5f,  0.5f,  0.5f,  0.0f,  1.0f,  0.0f,  0.0f, 0.0f, // Top-left
            
            // Bottom face
            -0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,  1.0f, 1.0f, // Bottom-left
             0.5f, -0.5f, -0.5f,  0.0f, -1.0f,  0.0f,  0.0f, 1.0f, // Bottom-right
             0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  0.0f, 0.0f, // Top-right
            -0.5f, -0.5f,  0.5f,  0.0f, -1.0f,  0.0f,  1.0f, 0.0f  // Top-left
        };
        
        int[] indices = {
            // Front face
            0, 1, 2, 2, 3, 0,
            // Back face
            4, 5, 6, 6, 7, 4,
            // Left face
            8, 9, 10, 10, 11, 8,
            // Right face
            12, 13, 14, 14, 15, 12,
            // Top face
            16, 17, 18, 18, 19, 16,
            // Bottom face
            20, 21, 22, 22, 23, 20
        };
        
        return new Mesh(vertices, indices);
    }
    
    public static Mesh createPlane(float size) {
        float[] vertices = {
            -size, 0.0f, -size,  0.0f, 1.0f, 0.0f,  0.0f, 0.0f, // Bottom-left
             size, 0.0f, -size,  0.0f, 1.0f, 0.0f,  1.0f, 0.0f, // Bottom-right
             size, 0.0f,  size,  0.0f, 1.0f, 0.0f,  1.0f, 1.0f, // Top-right
            -size, 0.0f,  size,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f  // Top-left
        };
        
        int[] indices = {
            0, 1, 2, 2, 3, 0
        };
        
        return new Mesh(vertices, indices);
    }
    
    /**
     * Creates an ocean grid mesh for Gerstner wave displacement.
     * @param width Width in world units
     * @param depth Depth in world units  
     * @param subdivisions Number of subdivisions per side (higher = more detailed waves)
     * @param centerX World X coordinate of grid center
     * @param centerZ World Z coordinate of grid center
     * @param seaLevel Y coordinate for sea level
     * @return Ocean grid mesh
     */
    public static Mesh createOceanGrid(float width, float depth, int subdivisions, 
                                       float centerX, float centerZ, float seaLevel) {
        int verticesPerSide = subdivisions + 1;
        int totalVertices = verticesPerSide * verticesPerSide;
        
        // 8 floats per vertex: 3 pos + 3 normal + 2 UV
        float[] vertices = new float[totalVertices * 8];
        
        float halfWidth = width * 0.5f;
        float halfDepth = depth * 0.5f;
        float stepX = width / subdivisions;
        float stepZ = depth / subdivisions;
        
        int vertexIndex = 0;
        for (int z = 0; z <= subdivisions; z++) {
            for (int x = 0; x <= subdivisions; x++) {
                // World-space position (critical for continuous waves across chunks)
                float worldX = centerX - halfWidth + x * stepX;
                float worldZ = centerZ - halfDepth + z * stepZ;
                
                // Position (world-space XZ, sea level Y)
                vertices[vertexIndex++] = worldX;
                vertices[vertexIndex++] = seaLevel;  // Will be displaced in vertex shader
                vertices[vertexIndex++] = worldZ;
                
                // Normal (up, will be recalculated in shader)
                vertices[vertexIndex++] = 0.0f;
                vertices[vertexIndex++] = 1.0f;
                vertices[vertexIndex++] = 0.0f;
                
                // UV coordinates
                vertices[vertexIndex++] = (float)x / subdivisions;
                vertices[vertexIndex++] = (float)z / subdivisions;
            }
        }
        
        // Generate indices for triangles
        int totalTriangles = subdivisions * subdivisions * 2;
        int[] indices = new int[totalTriangles * 3];
        
        int indexPos = 0;
        for (int z = 0; z < subdivisions; z++) {
            for (int x = 0; x < subdivisions; x++) {
                int topLeft = z * verticesPerSide + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * verticesPerSide + x;
                int bottomRight = bottomLeft + 1;
                
                // First triangle (top-left, bottom-left, top-right)
                indices[indexPos++] = topLeft;
                indices[indexPos++] = bottomLeft;
                indices[indexPos++] = topRight;
                
                // Second triangle (top-right, bottom-left, bottom-right)
                indices[indexPos++] = topRight;
                indices[indexPos++] = bottomLeft;
                indices[indexPos++] = bottomRight;
            }
        }
        
        logger.debug("Created ocean grid: {}x{} subdivisions, {} vertices, {} triangles", 
                    subdivisions, subdivisions, totalVertices, totalTriangles);
        
        return new Mesh(vertices, indices);
    }
}