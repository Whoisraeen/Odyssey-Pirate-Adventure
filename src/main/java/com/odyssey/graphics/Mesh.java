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
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        
        // Delete buffers
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboId);
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glDeleteBuffers(eboId);
        
        // Delete VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
        
        // Free memory
        if (verticesBuffer != null) {
            MemoryUtil.memFree(verticesBuffer);
        }
        if (indicesBuffer != null) {
            MemoryUtil.memFree(indicesBuffer);
        }
        
        // Return mesh data to pool if using memory manager
        if (memoryManager != null && meshData != null) {
            memoryManager.releaseMeshData(meshData);
            meshData = null;
        }
        
        logger.debug("Cleaned up mesh resources");
    }
    
    public int getVertexCount() {
        return vertexCount;
    }
    
    public int getIndexCount() {
        return indexCount;
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
}