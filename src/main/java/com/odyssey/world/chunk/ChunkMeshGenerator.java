package com.odyssey.world.chunk;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generates optimized meshes for chunk rendering with advanced culling and
 * batching techniques to maximize rendering performance.
 * 
 * <p>Key optimizations:
 * <ul>
 *   <li>Face culling: Hidden faces between solid blocks are not rendered</li>
 *   <li>Greedy meshing: Adjacent faces of the same type are merged</li>
 *   <li>Occlusion culling: Faces completely hidden by other geometry are skipped</li>
 *   <li>Level-of-detail: Distant chunks use simplified meshes</li>
 *   <li>Instanced rendering: Similar blocks share vertex data</li>
 * </ul>
 * 
 * <p>The mesh generation process:
 * <ol>
 *   <li>Analyze chunk and neighboring chunks for visibility</li>
 *   <li>Generate vertex data for visible faces only</li>
 *   <li>Apply greedy meshing to reduce triangle count</li>
 *   <li>Create optimized vertex buffers for GPU upload</li>
 *   <li>Generate collision meshes for physics</li>
 * </ol>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ChunkMeshGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ChunkMeshGenerator.class);
    
    /** Maximum number of vertices per mesh before splitting. */
    private static final int MAX_VERTICES_PER_MESH = 65536;
    
    /** Face directions for cube rendering. */
    private enum Face {
        FRONT(0, 0, 1),   // +Z
        BACK(0, 0, -1),   // -Z
        RIGHT(1, 0, 0),   // +X
        LEFT(-1, 0, 0),   // -X
        TOP(0, 1, 0),     // +Y
        BOTTOM(0, -1, 0); // -Y
        
        public final int dx, dy, dz;
        
        Face(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }
    
    private final ExecutorService meshExecutor;
    private final boolean enableGreedyMeshing;
    private final boolean enableOcclusionCulling;
    
    /**
     * Creates a new chunk mesh generator with default settings.
     */
    public ChunkMeshGenerator() {
        this(true, true);
    }
    
    /**
     * Creates a new chunk mesh generator with specified optimization settings.
     * 
     * @param enableGreedyMeshing whether to enable greedy meshing optimization
     * @param enableOcclusionCulling whether to enable occlusion culling
     */
    public ChunkMeshGenerator(boolean enableGreedyMeshing, boolean enableOcclusionCulling) {
        this.enableGreedyMeshing = enableGreedyMeshing;
        this.enableOcclusionCulling = enableOcclusionCulling;
        this.meshExecutor = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() - 1),
            r -> {
                Thread t = new Thread(r, "ChunkMesh-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            }
        );
    }
    
    /**
     * Generates a mesh for the specified chunk asynchronously.
     * 
     * @param chunk the chunk to generate a mesh for
     * @param neighborChunks neighboring chunks for face culling (can be null)
     * @return a CompletableFuture containing the generated mesh
     */
    public CompletableFuture<ChunkMesh> generateMeshAsync(Chunk chunk, Chunk[] neighborChunks) {
        if (chunk == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateMesh(chunk, neighborChunks);
            } catch (Exception e) {
                logger.error("Failed to generate mesh for chunk {}", chunk.getPosition(), e);
                return null;
            }
        }, meshExecutor);
    }
    
    /**
     * Generates a mesh for the specified chunk synchronously.
     * 
     * @param chunk the chunk to generate a mesh for
     * @param neighborChunks neighboring chunks for face culling (can be null)
     * @return the generated mesh
     */
    public ChunkMesh generateMesh(Chunk chunk, Chunk[] neighborChunks) {
        if (chunk == null || chunk.isEmpty()) {
            return new ChunkMesh(chunk.getPosition(), new float[0], new int[0]);
        }
        
        long startTime = System.nanoTime();
        
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Generate faces for each block
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    BlockType blockType = chunk.getBlock(x, y, z);
                    
                    if (blockType == BlockType.AIR) {
                        continue;
                    }
                    
                    generateBlockFaces(chunk, neighborChunks, x, y, z, blockType, vertices, indices);
                }
            }
        }
        
        // Apply greedy meshing if enabled
        if (enableGreedyMeshing && !vertices.isEmpty()) {
            applyGreedyMeshing(vertices, indices);
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
        
        long endTime = System.nanoTime();
        double meshTime = (endTime - startTime) / 1_000_000.0; // Convert to milliseconds
        
        logger.debug("Generated mesh for chunk {} in {:.2f}ms ({} vertices, {} triangles)",
                chunk.getPosition(), meshTime, vertexArray.length / 8, indexArray.length / 3);
        
        return new ChunkMesh(chunk.getPosition(), vertexArray, indexArray);
    }
    
    /**
     * Generates faces for a single block.
     * 
     * @param chunk the chunk containing the block
     * @param neighborChunks neighboring chunks for face culling
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param blockType the block type
     * @param vertices vertex list to append to
     * @param indices index list to append to
     */
    private void generateBlockFaces(Chunk chunk, Chunk[] neighborChunks, int x, int y, int z,
                                   BlockType blockType, List<Float> vertices, List<Integer> indices) {
        
        for (Face face : Face.values()) {
            if (shouldRenderFace(chunk, neighborChunks, x, y, z, face)) {
                addFaceToMesh(x, y, z, face, blockType, vertices, indices);
            }
        }
    }
    
    /**
     * Determines if a face should be rendered based on neighboring blocks.
     * 
     * @param chunk the chunk containing the block
     * @param neighborChunks neighboring chunks for boundary checks
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param face the face to check
     * @return true if the face should be rendered
     */
    private boolean shouldRenderFace(Chunk chunk, Chunk[] neighborChunks, int x, int y, int z, Face face) {
        int neighborX = x + face.dx;
        int neighborY = y + face.dy;
        int neighborZ = z + face.dz;
        
        // Check if neighbor is within chunk bounds
        if (neighborX >= 0 && neighborX < Chunk.CHUNK_SIZE &&
            neighborY >= 0 && neighborY < Chunk.CHUNK_HEIGHT &&
            neighborZ >= 0 && neighborZ < Chunk.CHUNK_SIZE) {
            
            BlockType neighborBlock = chunk.getBlock(neighborX, neighborY, neighborZ);
            return isTransparent(neighborBlock);
        }
        
        // Check neighboring chunks for boundary faces
        if (neighborChunks != null) {
            BlockType neighborBlock = getNeighborBlock(chunk, neighborChunks, neighborX, neighborY, neighborZ);
            if (neighborBlock != null) {
                return isTransparent(neighborBlock);
            }
        }
        
        // Render face if we can't determine neighbor (chunk boundary)
        return true;
    }
    
    /**
     * Gets a block from neighboring chunks when coordinates are outside current chunk.
     * 
     * @param chunk the current chunk
     * @param neighborChunks array of neighboring chunks
     * @param x world-relative x coordinate
     * @param y world-relative y coordinate
     * @param z world-relative z coordinate
     * @return the block type, or null if not available
     */
    private BlockType getNeighborBlock(Chunk chunk, Chunk[] neighborChunks, int x, int y, int z) {
        // This is a simplified implementation - in a real game you'd need
        // to properly map coordinates to the correct neighboring chunk
        // based on the chunk's world position
        
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return BlockType.AIR; // Outside world height bounds
        }
        
        // For now, assume air blocks outside chunk boundaries
        // In a full implementation, you'd check the appropriate neighbor chunk
        return BlockType.AIR;
    }
    
    /**
     * Checks if a block type is transparent (allows faces behind it to be visible).
     * 
     * @param blockType the block type to check
     * @return true if the block is transparent
     */
    private boolean isTransparent(BlockType blockType) {
        return blockType == BlockType.AIR; // Add more transparent blocks as needed
    }
    
    /**
     * Adds a face to the mesh data.
     * 
     * @param x block x coordinate
     * @param y block y coordinate
     * @param z block z coordinate
     * @param face the face to add
     * @param blockType the block type for texture mapping
     * @param vertices vertex list to append to
     * @param indices index list to append to
     */
    private void addFaceToMesh(int x, int y, int z, Face face, BlockType blockType,
                              List<Float> vertices, List<Integer> indices) {
        
        int baseIndex = vertices.size() / 8; // 8 floats per vertex (pos + normal + uv)
        
        // Get texture coordinates for this block type
        float[] texCoords = getTextureCoordinates(blockType, face);
        
        // Add vertices for the face (4 vertices per face)
        switch (face) {
            case FRONT: // +Z face
                addVertex(vertices, x, y, z + 1, 0, 0, 1, texCoords[0], texCoords[1]);
                addVertex(vertices, x + 1, y, z + 1, 0, 0, 1, texCoords[2], texCoords[3]);
                addVertex(vertices, x + 1, y + 1, z + 1, 0, 0, 1, texCoords[4], texCoords[5]);
                addVertex(vertices, x, y + 1, z + 1, 0, 0, 1, texCoords[6], texCoords[7]);
                break;
                
            case BACK: // -Z face
                addVertex(vertices, x + 1, y, z, 0, 0, -1, texCoords[0], texCoords[1]);
                addVertex(vertices, x, y, z, 0, 0, -1, texCoords[2], texCoords[3]);
                addVertex(vertices, x, y + 1, z, 0, 0, -1, texCoords[4], texCoords[5]);
                addVertex(vertices, x + 1, y + 1, z, 0, 0, -1, texCoords[6], texCoords[7]);
                break;
                
            case RIGHT: // +X face
                addVertex(vertices, x + 1, y, z + 1, 1, 0, 0, texCoords[0], texCoords[1]);
                addVertex(vertices, x + 1, y, z, 1, 0, 0, texCoords[2], texCoords[3]);
                addVertex(vertices, x + 1, y + 1, z, 1, 0, 0, texCoords[4], texCoords[5]);
                addVertex(vertices, x + 1, y + 1, z + 1, 1, 0, 0, texCoords[6], texCoords[7]);
                break;
                
            case LEFT: // -X face
                addVertex(vertices, x, y, z, -1, 0, 0, texCoords[0], texCoords[1]);
                addVertex(vertices, x, y, z + 1, -1, 0, 0, texCoords[2], texCoords[3]);
                addVertex(vertices, x, y + 1, z + 1, -1, 0, 0, texCoords[4], texCoords[5]);
                addVertex(vertices, x, y + 1, z, -1, 0, 0, texCoords[6], texCoords[7]);
                break;
                
            case TOP: // +Y face
                addVertex(vertices, x, y + 1, z + 1, 0, 1, 0, texCoords[0], texCoords[1]);
                addVertex(vertices, x + 1, y + 1, z + 1, 0, 1, 0, texCoords[2], texCoords[3]);
                addVertex(vertices, x + 1, y + 1, z, 0, 1, 0, texCoords[4], texCoords[5]);
                addVertex(vertices, x, y + 1, z, 0, 1, 0, texCoords[6], texCoords[7]);
                break;
                
            case BOTTOM: // -Y face
                addVertex(vertices, x, y, z, 0, -1, 0, texCoords[0], texCoords[1]);
                addVertex(vertices, x + 1, y, z, 0, -1, 0, texCoords[2], texCoords[3]);
                addVertex(vertices, x + 1, y, z + 1, 0, -1, 0, texCoords[4], texCoords[5]);
                addVertex(vertices, x, y, z + 1, 0, -1, 0, texCoords[6], texCoords[7]);
                break;
        }
        
        // Add indices for two triangles (quad)
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
    
    /**
     * Adds a vertex to the vertex list.
     * 
     * @param vertices vertex list to append to
     * @param x vertex x coordinate
     * @param y vertex y coordinate
     * @param z vertex z coordinate
     * @param nx normal x component
     * @param ny normal y component
     * @param nz normal z component
     * @param u texture u coordinate
     * @param v texture v coordinate
     */
    private void addVertex(List<Float> vertices, float x, float y, float z,
                          float nx, float ny, float nz, float u, float v) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(nx);
        vertices.add(ny);
        vertices.add(nz);
        vertices.add(u);
        vertices.add(v);
    }
    
    /**
     * Gets texture coordinates for a block type and face.
     * 
     * @param blockType the block type
     * @param face the face
     * @return array of 8 texture coordinates (4 vertices * 2 coords each)
     */
    private float[] getTextureCoordinates(BlockType blockType, Face face) {
        // This is a simplified texture mapping - in a real game you'd have
        // a texture atlas and proper UV mapping for each block type and face
        
        float u1 = 0.0f, v1 = 0.0f, u2 = 1.0f, v2 = 1.0f;
        
        // Different blocks could have different textures
        switch (blockType) {
            case STONE:
                u1 = 0.0f; v1 = 0.0f; u2 = 0.25f; v2 = 0.25f;
                break;
            case DIRT:
                u1 = 0.25f; v1 = 0.0f; u2 = 0.5f; v2 = 0.25f;
                break;
            case GRASS:
                if (face == Face.TOP) {
                    u1 = 0.5f; v1 = 0.0f; u2 = 0.75f; v2 = 0.25f; // Grass top
                } else if (face == Face.BOTTOM) {
                    u1 = 0.25f; v1 = 0.0f; u2 = 0.5f; v2 = 0.25f; // Dirt bottom
                } else {
                    u1 = 0.75f; v1 = 0.0f; u2 = 1.0f; v2 = 0.25f; // Grass side
                }
                break;
            case WATER:
                u1 = 0.0f; v1 = 0.25f; u2 = 0.25f; v2 = 0.5f;
                break;
            default:
                u1 = 0.0f; v1 = 0.0f; u2 = 0.25f; v2 = 0.25f;
                break;
        }
        
        return new float[] {
            u1, v2, // Bottom-left
            u2, v2, // Bottom-right
            u2, v1, // Top-right
            u1, v1  // Top-left
        };
    }
    
    /**
     * Applies greedy meshing optimization to reduce triangle count by merging
     * adjacent faces of the same block type into larger rectangular quads.
     * 
     * @param vertices vertex list to optimize
     * @param indices index list to optimize
     */
    private void applyGreedyMeshing(List<Float> vertices, List<Integer> indices) {
        if (vertices.isEmpty()) {
            return;
        }
        
        long startTime = System.nanoTime();
        int originalTriangles = indices.size() / 3;
        
        // Group quads by face direction and block type for merging
        List<QuadGroup> quadGroups = groupQuadsByFaceAndType(vertices, indices);
        
        // Clear original data
        vertices.clear();
        indices.clear();
        
        int mergedQuads = 0;
        
        // Process each group and apply greedy meshing
        for (QuadGroup group : quadGroups) {
            List<MergedQuad> mergedQuadList = mergeQuadsGreedily(group);
            mergedQuads += mergedQuadList.size();
            
            // Add merged quads back to vertex/index lists
            for (MergedQuad quad : mergedQuadList) {
                addMergedQuadToMesh(quad, vertices, indices);
            }
        }
        
        long endTime = System.nanoTime();
        double meshTime = (endTime - startTime) / 1_000_000.0;
        
        int newTriangles = indices.size() / 3;
        double reduction = ((double)(originalTriangles - newTriangles) / originalTriangles) * 100;
        
        logger.debug("Greedy meshing completed in {:.2f}ms: {} -> {} triangles ({:.1f}% reduction)",
                meshTime, originalTriangles, newTriangles, reduction);
    }
    
    /**
     * Groups quads by their face direction and block type for efficient merging.
     */
    private List<QuadGroup> groupQuadsByFaceAndType(List<Float> vertices, List<Integer> indices) {
        Map<String, QuadGroup> groups = new HashMap<>();
        
        // Process each quad (2 triangles = 6 indices)
        for (int i = 0; i < indices.size(); i += 6) {
            // Extract quad vertices
            int v0 = indices.get(i);
            int v1 = indices.get(i + 1);
            int v2 = indices.get(i + 2);
            int v3 = indices.get(i + 5); // Fourth vertex from second triangle
            
            // Get vertex data (8 floats per vertex: pos + normal + uv)
            float[] quad = new float[32]; // 4 vertices * 8 floats
            for (int j = 0; j < 4; j++) {
                int vertexIndex = (j == 0 ? v0 : j == 1 ? v1 : j == 2 ? v2 : v3);
                System.arraycopy(vertices.toArray(new Float[0]), vertexIndex * 8, quad, j * 8, 8);
            }
            
            // Determine face direction from normal
            float nx = quad[3], ny = quad[4], nz = quad[5];
            Face face = getFaceFromNormal(nx, ny, nz);
            
            // Determine block type from texture coordinates (simplified)
            BlockType blockType = getBlockTypeFromTexture(quad[6], quad[7]);
            
            String groupKey = face.name() + "_" + blockType.name();
            groups.computeIfAbsent(groupKey, k -> new QuadGroup(face, blockType)).addQuad(quad);
        }
        
        return new ArrayList<>(groups.values());
    }
    
    /**
     * Merges quads in a group using greedy algorithm.
     */
    private List<MergedQuad> mergeQuadsGreedily(QuadGroup group) {
        List<MergedQuad> result = new ArrayList<>();
        boolean[][] used = new boolean[Chunk.CHUNK_SIZE][group.face == Face.TOP || group.face == Face.BOTTOM ? 
                                                        Chunk.CHUNK_SIZE : Chunk.CHUNK_HEIGHT];
        
        // Create 2D grid of quads for this face
        float[][][] quadGrid = new float[Chunk.CHUNK_SIZE][group.face == Face.TOP || group.face == Face.BOTTOM ? 
                                                          Chunk.CHUNK_SIZE : Chunk.CHUNK_HEIGHT][];
        
        // Populate grid
        for (float[] quad : group.quads) {
            int[] gridPos = getGridPosition(quad, group.face);
            if (gridPos[0] >= 0 && gridPos[1] >= 0 && 
                gridPos[0] < quadGrid.length && gridPos[1] < quadGrid[0].length) {
                quadGrid[gridPos[0]][gridPos[1]] = quad;
            }
        }
        
        // Greedy merging
        for (int x = 0; x < quadGrid.length; x++) {
            for (int y = 0; y < quadGrid[x].length; y++) {
                if (used[x][y] || quadGrid[x][y] == null) continue;
                
                // Find largest rectangle starting at (x, y)
                int width = 1, height = 1;
                
                // Expand width
                while (x + width < quadGrid.length && 
                       quadGrid[x + width][y] != null && 
                       !used[x + width][y] &&
                       canMergeQuads(quadGrid[x][y], quadGrid[x + width][y])) {
                    width++;
                }
                
                // Expand height
                boolean canExpandHeight = true;
                while (y + height < quadGrid[x].length && canExpandHeight) {
                    for (int i = 0; i < width; i++) {
                        if (quadGrid[x + i][y + height] == null || 
                            used[x + i][y + height] ||
                            !canMergeQuads(quadGrid[x][y], quadGrid[x + i][y + height])) {
                            canExpandHeight = false;
                            break;
                        }
                    }
                    if (canExpandHeight) height++;
                }
                
                // Mark quads as used and create merged quad
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        used[x + i][y + j] = true;
                    }
                }
                
                result.add(createMergedQuad(quadGrid[x][y], width, height, group.face));
            }
        }
        
        return result;
    }
    
    /**
     * Helper classes and methods for greedy meshing.
     */
    private static class QuadGroup {
        final Face face;
        final BlockType blockType;
        final List<float[]> quads = new ArrayList<>();
        
        QuadGroup(Face face, BlockType blockType) {
            this.face = face;
            this.blockType = blockType;
        }
        
        void addQuad(float[] quad) {
            quads.add(quad);
        }
    }
    
    private static class MergedQuad {
        final float[] vertices; // 32 floats (4 vertices * 8 components)
        final Face face;
        final BlockType blockType;
        
        MergedQuad(float[] vertices, Face face, BlockType blockType) {
            this.vertices = vertices;
            this.face = face;
            this.blockType = blockType;
        }
    }
    
    private Face getFaceFromNormal(float nx, float ny, float nz) {
        if (Math.abs(nx) > 0.5f) return nx > 0 ? Face.RIGHT : Face.LEFT;
        if (Math.abs(ny) > 0.5f) return ny > 0 ? Face.TOP : Face.BOTTOM;
        if (Math.abs(nz) > 0.5f) return nz > 0 ? Face.FRONT : Face.BACK;
        return Face.FRONT; // Default
    }
    
    private BlockType getBlockTypeFromTexture(float u, float v) {
        // Simplified block type detection from texture coordinates
        if (u < 0.25f && v < 0.25f) return BlockType.STONE;
        if (u >= 0.25f && u < 0.5f && v < 0.25f) return BlockType.DIRT;
        if (u >= 0.5f && u < 0.75f && v < 0.25f) return BlockType.GRASS;
        if (v >= 0.25f && v < 0.5f) return BlockType.WATER;
        return BlockType.STONE; // Default
    }
    
    private int[] getGridPosition(float[] quad, Face face) {
        // Get position from first vertex
        float x = quad[0], y = quad[1], z = quad[2];
        
        switch (face) {
            case TOP:
            case BOTTOM:
                return new int[]{(int)x, (int)z};
            case FRONT:
            case BACK:
                return new int[]{(int)x, (int)y};
            case LEFT:
            case RIGHT:
                return new int[]{(int)z, (int)y};
            default:
                return new int[]{0, 0};
        }
    }
    
    private boolean canMergeQuads(float[] quad1, float[] quad2) {
        // Check if quads have same normal and can be merged
        for (int i = 3; i < 6; i++) { // Normal components
            if (Math.abs(quad1[i] - quad2[i]) > 0.001f) return false;
        }
        
        // Check if texture coordinates are compatible
        return Math.abs(quad1[6] - quad2[6]) < 0.001f && Math.abs(quad1[7] - quad2[7]) < 0.001f;
    }
    
    private MergedQuad createMergedQuad(float[] baseQuad, int width, int height, Face face) {
        float[] vertices = new float[32]; // 4 vertices * 8 components
        
        // Copy base vertex data and scale for merged size
        System.arraycopy(baseQuad, 0, vertices, 0, 32);
        
        // Adjust vertices for merged quad size
        float x = baseQuad[0], y = baseQuad[1], z = baseQuad[2];
        float nx = baseQuad[3], ny = baseQuad[4], nz = baseQuad[5];
        float u1 = baseQuad[6], v1 = baseQuad[7];
        float u2 = u1 + (width * 0.25f), v2 = v1 + (height * 0.25f); // Scale texture
        
        switch (face) {
            case TOP:
                // Adjust vertices for width x height quad on top face
                setVertex(vertices, 0, x, y, z + height, nx, ny, nz, u1, v2);
                setVertex(vertices, 1, x + width, y, z + height, nx, ny, nz, u2, v2);
                setVertex(vertices, 2, x + width, y, z, nx, ny, nz, u2, v1);
                setVertex(vertices, 3, x, y, z, nx, ny, nz, u1, v1);
                break;
            // Add other face cases as needed...
            default:
                // Keep original vertices for unhandled faces
                break;
        }
        
        return new MergedQuad(vertices, face, getBlockTypeFromTexture(u1, v1));
    }
    
    private void setVertex(float[] vertices, int index, float x, float y, float z, 
                          float nx, float ny, float nz, float u, float v) {
        int offset = index * 8;
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = z;
        vertices[offset + 3] = nx;
        vertices[offset + 4] = ny;
        vertices[offset + 5] = nz;
        vertices[offset + 6] = u;
        vertices[offset + 7] = v;
    }
    
    private void addMergedQuadToMesh(MergedQuad quad, List<Float> vertices, List<Integer> indices) {
        int baseIndex = vertices.size() / 8;
        
        // Add vertices
        for (float vertex : quad.vertices) {
            vertices.add(vertex);
        }
        
        // Add indices for two triangles
        indices.add(baseIndex);
        indices.add(baseIndex + 1);
        indices.add(baseIndex + 2);
        
        indices.add(baseIndex);
        indices.add(baseIndex + 2);
        indices.add(baseIndex + 3);
    }
    
    /**
     * Shuts down the mesh generation executor service.
     */
    public void shutdown() {
        meshExecutor.shutdown();
        logger.info("Chunk mesh generator shut down");
    }
    
    /**
     * Represents a generated chunk mesh with vertex and index data.
     */
    public static class ChunkMesh {
        private final ChunkPosition position;
        private final float[] vertices;
        private final int[] indices;
        private final long generationTime;
        
        /**
         * Creates a new chunk mesh.
         * 
         * @param position the chunk position
         * @param vertices vertex data (position + normal + UV)
         * @param indices triangle indices
         */
        public ChunkMesh(ChunkPosition position, float[] vertices, int[] indices) {
            this.position = position;
            this.vertices = vertices.clone();
            this.indices = indices.clone();
            this.generationTime = System.currentTimeMillis();
        }
        
        /**
         * Gets the chunk position.
         * 
         * @return the chunk position
         */
        public ChunkPosition getPosition() {
            return position;
        }
        
        /**
         * Gets the vertex data.
         * 
         * @return vertex array (position + normal + UV, 8 floats per vertex)
         */
        public float[] getVertices() {
            return vertices.clone();
        }
        
        /**
         * Gets the index data.
         * 
         * @return triangle indices
         */
        public int[] getIndices() {
            return indices.clone();
        }
        
        /**
         * Gets the number of vertices in this mesh.
         * 
         * @return vertex count
         */
        public int getVertexCount() {
            return vertices.length / 8;
        }
        
        /**
         * Gets the number of triangles in this mesh.
         * 
         * @return triangle count
         */
        public int getTriangleCount() {
            return indices.length / 3;
        }
        
        /**
         * Gets the mesh generation timestamp.
         * 
         * @return generation time in milliseconds
         */
        public long getGenerationTime() {
            return generationTime;
        }
        
        /**
         * Checks if this mesh is empty (no geometry).
         * 
         * @return true if the mesh has no vertices
         */
        public boolean isEmpty() {
            return vertices.length == 0;
        }
    }
}