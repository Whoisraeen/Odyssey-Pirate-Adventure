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
        // Get the face direction and vertex offset
        int[] faceDir = FACE_DIRECTIONS[face];
        Vector3f[] faceVertices = getFaceVertices(x, y, z, face);
        Vector3f vertexPos = faceVertices[vertex];
        
        // Calculate the three neighboring positions for this vertex
        Vector3f[] neighbors = getVertexNeighbors(vertexPos, faceDir);
        
        // Check occlusion from the three neighboring blocks
        int occludedCount = 0;
        boolean[] occluded = new boolean[3];
        
        for (int i = 0; i < 3; i++) {
            Vector3f neighbor = neighbors[i];
            int nx = (int) Math.floor(neighbor.x);
            int ny = (int) Math.floor(neighbor.y);
            int nz = (int) Math.floor(neighbor.z);
            
            Block.BlockType neighborType = chunk.getNeighborBlock(nx, ny, nz);
            if (neighborType != Block.BlockType.AIR && neighborType.isOpaque()) {
                occluded[i] = true;
                occludedCount++;
            }
        }
        
        // Calculate ambient occlusion based on neighboring blocks
        float aoValue = 1.0f;
        
        if (occludedCount == 0) {
            // No occlusion
            aoValue = 1.0f;
        } else if (occludedCount == 1) {
            // Light occlusion
            aoValue = 0.8f;
        } else if (occludedCount == 2) {
            // Medium occlusion
            aoValue = 0.6f;
        } else {
            // Heavy occlusion (corner case)
            aoValue = 0.4f;
        }
        
        // Additional corner darkening for more realistic AO
        if (occluded[0] && occluded[1] && occluded[2]) {
            aoValue = 0.2f; // Very dark corners
        } else if ((occluded[0] && occluded[1]) || (occluded[1] && occluded[2]) || (occluded[0] && occluded[2])) {
            aoValue = Math.min(aoValue, 0.5f); // Darker edges
        }
        
        return aoValue;
    }
    
    /**
     * Gets the three neighboring block positions for a vertex based on face direction
     */
    private Vector3f[] getVertexNeighbors(Vector3f vertexPos, int[] faceDir) {
        Vector3f[] neighbors = new Vector3f[3];
        
        // The three neighbors are along the face normal and the two perpendicular directions
        Vector3f normal = new Vector3f(faceDir[0], faceDir[1], faceDir[2]);
        
        // Get two perpendicular directions to the face normal
        Vector3f perp1, perp2;
        if (Math.abs(normal.x) < 0.9f) {
            perp1 = new Vector3f(1, 0, 0).cross(normal).normalize();
        } else {
            perp1 = new Vector3f(0, 1, 0).cross(normal).normalize();
        }
        perp2 = new Vector3f(normal).cross(perp1).normalize();
        
        // Calculate neighbor positions
        neighbors[0] = new Vector3f(vertexPos).add(normal);
        neighbors[1] = new Vector3f(vertexPos).add(perp1);
        neighbors[2] = new Vector3f(vertexPos).add(perp2);
        
        return neighbors;
    }
    
    /**
     * Gets texture coordinates for a block type and face
     */
    private float[] getTextureCoords(Block.BlockType blockType, int face) {
        // Get the texture atlas from the advanced rendering manager
        // For now, we'll use a simple mapping system based on block type
        
        String textureName = getTextureNameForBlock(blockType, face);
        
        // Try to get UV coordinates from texture atlas
        // If no atlas is available, fall back to default coordinates
        try {
            // This would typically get the texture atlas from a renderer or resource manager
            // For now, we'll implement a basic texture coordinate mapping
            return getBlockTextureUVs(blockType, face);
        } catch (Exception e) {
            // Fall back to default UV coordinates if texture atlas fails
            return FACE_UVS[face];
        }
    }
    
    /**
     * Gets the texture name for a specific block type and face
     */
    private String getTextureNameForBlock(Block.BlockType blockType, int face) {
        switch (blockType) {
            case STONE:
                return "stone";
            case DIRT:
                return "dirt";
            case GRASS:
                // Grass has different textures for different faces
                if (face == 0) return "grass_top";      // Top face
                if (face == 1) return "dirt";           // Bottom face
                return "grass_side";                    // Side faces
            case SAND:
                return "sand";
            case WOOD:
                // Wood has different textures for different faces
                if (face == 0 || face == 1) return "wood_top";  // Top/bottom faces
                return "wood_side";                              // Side faces
            case LEAVES:
                return "leaves";
            case WATER:
                return "water";
            case IRON_ORE:
                return "iron_ore";
            case COAL_ORE:
                return "coal_ore";
            case GOLD_ORE:
                return "gold_ore";
            case PLANKS:
                return "planks";
            case COBBLESTONE:
                return "cobblestone";
            case GLOWSTONE:
                return "glowstone";
            default:
                return "stone"; // Default fallback
        }
    }
    
    /**
     * Gets UV coordinates for a block type and face using texture atlas mapping
     */
    private float[] getBlockTextureUVs(Block.BlockType blockType, int face) {
        // This is a simplified texture atlas mapping
        // In a real implementation, this would query the TextureAtlas
        
        // Calculate texture coordinates based on a 16x16 texture atlas grid
        int textureIndex = getTextureIndex(blockType, face);
        int atlasSize = 16; // 16x16 grid of textures
        float textureSize = 1.0f / atlasSize;
        
        int u = textureIndex % atlasSize;
        int v = textureIndex / atlasSize;
        
        float minU = u * textureSize;
        float minV = v * textureSize;
        float maxU = minU + textureSize;
        float maxV = minV + textureSize;
        
        // Return UV coordinates for the four corners of the face
        return new float[] {
            minU, minV,  // Bottom-left
            maxU, minV,  // Bottom-right
            maxU, maxV,  // Top-right
            minU, maxV   // Top-left
        };
    }
    
    /**
     * Gets the texture index in the atlas for a block type and face
     */
    private int getTextureIndex(Block.BlockType blockType, int face) {
        switch (blockType) {
            case STONE: return 0;
            case DIRT: return 1;
            case GRASS:
                if (face == 0) return 2;      // Grass top
                if (face == 1) return 1;      // Dirt bottom
                return 3;                     // Grass side
            case SAND: return 4;
            case WOOD:
                if (face == 0 || face == 1) return 5;  // Wood top/bottom
                return 6;                               // Wood side
            case LEAVES: return 7;
            case WATER: return 8;
            case IRON_ORE: return 9;
            case COAL_ORE: return 10;
            case GOLD_ORE: return 11;
            case PLANKS: return 12;
            case COBBLESTONE: return 13;
            case GLOWSTONE: return 14;
            default: return 0; // Default to stone
        }
    }
}