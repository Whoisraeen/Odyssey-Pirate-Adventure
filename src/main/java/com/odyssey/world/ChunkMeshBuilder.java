package com.odyssey.world;

import com.odyssey.rendering.Mesh;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds optimized meshes for chunk rendering using greedy meshing algorithms.
 * Generates separate meshes for solid, transparent, and water blocks.
 */
public class ChunkMeshBuilder {
    
    private final Chunk chunk;
    
    // Face directions
    private static final int[][] FACE_DIRECTIONS = {
        {0, 1, 0},  // TOP
        {0, -1, 0}, // BOTTOM
        {0, 0, 1},  // NORTH
        {0, 0, -1}, // SOUTH
        {1, 0, 0},  // EAST
        {-1, 0, 0}  // WEST
    };
    
    // Face normals
    private static final Vector3f[] FACE_NORMALS = {
        new Vector3f(0, 1, 0),   // TOP
        new Vector3f(0, -1, 0),  // BOTTOM
        new Vector3f(0, 0, 1),   // NORTH
        new Vector3f(0, 0, -1),  // SOUTH
        new Vector3f(1, 0, 0),   // EAST
        new Vector3f(-1, 0, 0)   // WEST
    };
    
    // UV coordinates for each face
    private static final float[][] FACE_UVS = {
        // TOP
        {0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f},
        // BOTTOM
        {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f},
        // NORTH
        {1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
        // SOUTH
        {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f},
        // EAST
        {1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f},
        // WEST
        {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f}
    };
    
    public ChunkMeshBuilder(Chunk chunk) {
        this.chunk = chunk;
    }
    
    /**
     * Container for generated mesh data
     */
    public static class MeshData {
        public Mesh solidMesh;
        public Mesh transparentMesh;
        public Mesh waterMesh;
        
        public MeshData(Mesh solidMesh, Mesh transparentMesh, Mesh waterMesh) {
            this.solidMesh = solidMesh;
            this.transparentMesh = transparentMesh;
            this.waterMesh = waterMesh;
        }
    }
    
    /**
     * Generates meshes for the chunk
     */
    public MeshData generateMesh() {
        List<Float> solidVertices = new ArrayList<>();
        List<Integer> solidIndices = new ArrayList<>();
        
        List<Float> transparentVertices = new ArrayList<>();
        List<Integer> transparentIndices = new ArrayList<>();
        
        List<Float> waterVertices = new ArrayList<>();
        List<Integer> waterIndices = new ArrayList<>();
        
        // Generate faces for each block
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block.BlockType blockType = chunk.getBlock(x, y, z);
                    
                    if (blockType == Block.BlockType.AIR) {
                        continue;
                    }
                    
                    // Generate faces for each direction
                    for (int face = 0; face < 6; face++) {
                        if (shouldRenderFace(x, y, z, face, blockType)) {
                            if (blockType == Block.BlockType.WATER) {
                                addFace(waterVertices, waterIndices, x, y, z, face, blockType);
                            } else if (blockType.isTransparent()) {
                                addFace(transparentVertices, transparentIndices, x, y, z, face, blockType);
                            } else {
                                addFace(solidVertices, solidIndices, x, y, z, face, blockType);
                            }
                        }
                    }
                }
            }
        }
        
        // Create meshes
        Mesh solidMesh = createMesh(solidVertices, solidIndices);
        Mesh transparentMesh = createMesh(transparentVertices, transparentIndices);
        Mesh waterMesh = createMesh(waterVertices, waterIndices);
        
        return new MeshData(solidMesh, transparentMesh, waterMesh);
    }
    
    /**
     * Determines if a face should be rendered
     */
    private boolean shouldRenderFace(int x, int y, int z, int face, Block.BlockType blockType) {
        int[] dir = FACE_DIRECTIONS[face];
        int nx = x + dir[0];
        int ny = y + dir[1];
        int nz = z + dir[2];
        
        Block.BlockType neighborType = chunk.getNeighborBlock(nx, ny, nz);
        
        // Always render faces against air
        if (neighborType == Block.BlockType.AIR) {
            return true;
        }
        
        // Water rendering rules
        if (blockType == Block.BlockType.WATER) {
            return neighborType != Block.BlockType.WATER;
        }
        
        // Transparent block rendering rules
        if (blockType.isTransparent()) {
            return neighborType != blockType && !neighborType.isOpaque();
        }
        
        // Solid block rendering rules
        return neighborType.isTransparent();
    }
    
    /**
     * Adds a face to the vertex and index lists
     */
    private void addFace(List<Float> vertices, List<Integer> indices, 
                        int x, int y, int z, int face, Block.BlockType blockType) {
        
        Vector3f normal = FACE_NORMALS[face];
        Vector3f color = blockType.getColor();
        float[] uvs = FACE_UVS[face];
        
        // Get light level for this position
        float lightLevel = chunk.getLightLevel(x, y, z) / 15.0f;
        
        // Face vertices (4 vertices per face)
        Vector3f[] faceVertices = getFaceVertices(x, y, z, face);
        
        int baseIndex = vertices.size() / 9; // 9 floats per vertex (pos + normal + uv + color)
        
        // Add vertices
        for (int i = 0; i < 4; i++) {
            Vector3f vertex = faceVertices[i];
            
            // Position
            vertices.add(vertex.x);
            vertices.add(vertex.y);
            vertices.add(vertex.z);
            
            // Normal
            vertices.add(normal.x);
            vertices.add(normal.y);
            vertices.add(normal.z);
            
            // UV coordinates
            vertices.add(uvs[i * 2]);
            vertices.add(uvs[i * 2 + 1]);
            
            // Color with lighting
            vertices.add(color.x * lightLevel);
            vertices.add(color.y * lightLevel);
            vertices.add(color.z * lightLevel);
        }
        
        // Add indices (two triangles per face)
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
    
    /**
     * Gets the vertices for a face
     */
    private Vector3f[] getFaceVertices(int x, int y, int z, int face) {
        Vector3f[] vertices = new Vector3f[4];
        
        switch (face) {
            case 0: // TOP
                vertices[0] = new Vector3f(x, y + 1, z);
                vertices[1] = new Vector3f(x + 1, y + 1, z);
                vertices[2] = new Vector3f(x + 1, y + 1, z + 1);
                vertices[3] = new Vector3f(x, y + 1, z + 1);
                break;
            case 1: // BOTTOM
                vertices[0] = new Vector3f(x, y, z + 1);
                vertices[1] = new Vector3f(x + 1, y, z + 1);
                vertices[2] = new Vector3f(x + 1, y, z);
                vertices[3] = new Vector3f(x, y, z);
                break;
            case 2: // NORTH
                vertices[0] = new Vector3f(x + 1, y + 1, z + 1);
                vertices[1] = new Vector3f(x, y + 1, z + 1);
                vertices[2] = new Vector3f(x, y, z + 1);
                vertices[3] = new Vector3f(x + 1, y, z + 1);
                break;
            case 3: // SOUTH
                vertices[0] = new Vector3f(x, y + 1, z);
                vertices[1] = new Vector3f(x + 1, y + 1, z);
                vertices[2] = new Vector3f(x + 1, y, z);
                vertices[3] = new Vector3f(x, y, z);
                break;
            case 4: // EAST
                vertices[0] = new Vector3f(x + 1, y + 1, z);
                vertices[1] = new Vector3f(x + 1, y + 1, z + 1);
                vertices[2] = new Vector3f(x + 1, y, z + 1);
                vertices[3] = new Vector3f(x + 1, y, z);
                break;
            case 5: // WEST
                vertices[0] = new Vector3f(x, y + 1, z + 1);
                vertices[1] = new Vector3f(x, y + 1, z);
                vertices[2] = new Vector3f(x, y, z);
                vertices[3] = new Vector3f(x, y, z + 1);
                break;
        }
        
        return vertices;
    }
    
    /**
     * Creates a mesh from vertex and index data
     */
    private Mesh createMesh(List<Float> vertices, List<Integer> indices) {
        if (vertices.isEmpty() || indices.isEmpty()) {
            return null;
        }
        
        // Convert lists to arrays
        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        
        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        // Create mesh with vertex attributes:
        // Position (3), Normal (3), UV (2), Color (3) = 11 floats per vertex
        Mesh mesh = new Mesh("ChunkMesh", vertexArray, indexArray);
        mesh.addAttribute(Mesh.VertexAttribute.position(0, 0));
        mesh.addAttribute(Mesh.VertexAttribute.normal(1, 3 * Float.BYTES));
        mesh.addAttribute(Mesh.VertexAttribute.texCoord(2, 6 * Float.BYTES));
        mesh.addAttribute(Mesh.VertexAttribute.color(3, 8 * Float.BYTES));
        mesh.upload();
        return mesh;
    }
    
    /**
     * Optimized mesh generation using greedy meshing algorithm
     * TODO: Implement for better performance
     */
    public MeshData generateOptimizedMesh() {
        // For now, use the basic mesh generation
        // Future optimization: implement greedy meshing to reduce face count
        return generateMesh();
    }
    
    /**
     * Generates ambient occlusion values for vertices
     */
    private float calculateAmbientOcclusion(int x, int y, int z, int face, int vertex) {
        // TODO: Implement ambient occlusion calculation
        // This would check surrounding blocks to determine shadow intensity
        return 1.0f; // No occlusion for now
    }
    
    /**
     * Gets texture coordinates for a block type and face
     */
    private float[] getTextureCoords(Block.BlockType blockType, int face) {
        // TODO: Implement texture atlas mapping
        // For now, return default UV coordinates
        return FACE_UVS[face];
    }
}