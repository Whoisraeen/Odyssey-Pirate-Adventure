package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Manages mesh loading, caching, and cleanup for the rendering system.
 * Provides efficient mesh resource management with automatic cleanup.
 */
public class MeshManager {
    
    private static final Logger logger = Logger.getLogger(MeshManager.class);
    
    // Mesh cache
    private final Map<String, Mesh> meshCache;
    private final Map<String, Integer> referenceCount;
    
    // Primitive meshes
    private Mesh quadMesh;
    private Mesh cubeMesh;
    private Mesh sphereMesh;
    private Mesh planeMesh;
    
    public MeshManager() {
        this.meshCache = new HashMap<>();
        this.referenceCount = new HashMap<>();
        
        createPrimitiveMeshes();
        logger.info("MeshManager initialized");
    }
    
    /**
     * Initialize the mesh manager (called by Renderer).
     * This method is separate from the constructor to allow for explicit initialization.
     */
    public void initialize() {
        // Additional initialization if needed
        logger.debug("MeshManager initialize() called");
    }
    
    /**
     * Creates a mesh from vertex data
     */
    public Mesh createMesh(String name, float[] vertices, int[] indices) {
        if (meshCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return meshCache.get(name);
        }
        
        Mesh mesh = new Mesh(name, vertices, indices);
        mesh.addAttribute(Mesh.VertexAttribute.position(0, 0));
        mesh.addAttribute(Mesh.VertexAttribute.normal(1, 3 * Float.BYTES));
        mesh.addAttribute(Mesh.VertexAttribute.texCoord(2, 6 * Float.BYTES));
        mesh.upload();
            
        meshCache.put(name, mesh);
        referenceCount.put(name, 1);
        
        logger.debug("Created mesh: {} ({} vertices, {} indices)", 
                    name, vertices.length / 8, indices.length); // Assuming 8 floats per vertex (pos+norm+uv)
        return mesh;
    }
    
    /**
     * Creates a mesh with full vertex attributes
     */
    public Mesh createMesh(String name, List<Vector3f> positions, List<Vector3f> normals, 
                          List<Vector2f> texCoords, List<Integer> indices) {
        if (meshCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return meshCache.get(name);
        }
        
        // Convert to arrays
        float[] vertices = new float[positions.size() * 8]; // pos(3) + norm(3) + uv(2)
        int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
        
        for (int i = 0; i < positions.size(); i++) {
            Vector3f pos = positions.get(i);
            Vector3f norm = i < normals.size() ? normals.get(i) : new Vector3f(0, 1, 0);
            Vector2f uv = i < texCoords.size() ? texCoords.get(i) : new Vector2f(0, 0);
            
            int offset = i * 8;
            vertices[offset] = pos.x;
            vertices[offset + 1] = pos.y;
            vertices[offset + 2] = pos.z;
            vertices[offset + 3] = norm.x;
            vertices[offset + 4] = norm.y;
            vertices[offset + 5] = norm.z;
            vertices[offset + 6] = uv.x;
            vertices[offset + 7] = uv.y;
        }
        
        Mesh mesh = new Mesh(name, vertices, indexArray);
        mesh.addAttribute(Mesh.VertexAttribute.position(0, 0));
        mesh.addAttribute(Mesh.VertexAttribute.normal(1, 3 * Float.BYTES));
        mesh.addAttribute(Mesh.VertexAttribute.texCoord(2, 6 * Float.BYTES));
        mesh.upload();
            
        meshCache.put(name, mesh);
        referenceCount.put(name, 1);
        
        logger.debug("Created mesh: {} ({} vertices, {} indices)", 
                    name, positions.size(), indices.size());
        return mesh;
    }
    
    /**
     * Gets a cached mesh by name
     */
    public Mesh getMesh(String name) {
        Mesh mesh = meshCache.get(name);
        if (mesh != null) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return mesh;
        }
        
        logger.warn("Mesh not found: {}", name);
        return getQuadMesh();
    }
    
    /**
     * Releases a mesh reference
     */
    public void releaseMesh(String name) {
        if (!referenceCount.containsKey(name)) {
            return;
        }
        
        int count = referenceCount.get(name) - 1;
        if (count <= 0) {
            // Remove from cache and cleanup
            Mesh mesh = meshCache.remove(name);
            referenceCount.remove(name);
            
            if (mesh != null) {
                mesh.cleanup();
                logger.debug("Released mesh: {}", name);
            }
        } else {
            referenceCount.put(name, count);
        }
    }
    
    /**
     * Gets a quad mesh (2 triangles)
     */
    public Mesh getQuadMesh() {
        return quadMesh;
    }
    
    /**
     * Gets a cube mesh
     */
    public Mesh getCubeMesh() {
        return cubeMesh;
    }
    
    /**
     * Gets a sphere mesh
     */
    public Mesh getSphereMesh() {
        return sphereMesh;
    }
    
    /**
     * Gets a plane mesh
     */
    public Mesh getPlaneMesh() {
        return planeMesh;
    }
    
    /**
     * Creates primitive meshes for common use
     */
    private void createPrimitiveMeshes() {
        // Create quad mesh
        float[] quadVertices = {
            // Position      Normal        TexCoord
            -1.0f, -1.0f, 0.0f,  0.0f, 0.0f, 1.0f,  0.0f, 0.0f,
             1.0f, -1.0f, 0.0f,  0.0f, 0.0f, 1.0f,  1.0f, 0.0f,
             1.0f,  1.0f, 0.0f,  0.0f, 0.0f, 1.0f,  1.0f, 1.0f,
            -1.0f,  1.0f, 0.0f,  0.0f, 0.0f, 1.0f,  0.0f, 1.0f
        };
        int[] quadIndices = { 0, 1, 2, 2, 3, 0 };
        
        quadMesh = new Mesh("quad", quadVertices, quadIndices);
        quadMesh.addAttribute(Mesh.VertexAttribute.position(0, 0));
        quadMesh.addAttribute(Mesh.VertexAttribute.normal(1, 3 * Float.BYTES));
        quadMesh.addAttribute(Mesh.VertexAttribute.texCoord(2, 6 * Float.BYTES));
        quadMesh.upload();
            
        meshCache.put("quad", quadMesh);
        referenceCount.put("quad", 1);
        
        // Create cube mesh
        createCubeMesh();
        
        // Create sphere mesh
        createSphereMesh();
        
        // Create plane mesh
        createPlaneMesh();
    }
    
    /**
     * Creates a cube mesh
     */
    private void createCubeMesh() {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Define cube faces
        Vector3f[][] faces = {
            // Front face
            {new Vector3f(-1, -1,  1), new Vector3f( 1, -1,  1), new Vector3f( 1,  1,  1), new Vector3f(-1,  1,  1)},
            // Back face
            {new Vector3f( 1, -1, -1), new Vector3f(-1, -1, -1), new Vector3f(-1,  1, -1), new Vector3f( 1,  1, -1)},
            // Top face
            {new Vector3f(-1,  1,  1), new Vector3f( 1,  1,  1), new Vector3f( 1,  1, -1), new Vector3f(-1,  1, -1)},
            // Bottom face
            {new Vector3f(-1, -1, -1), new Vector3f( 1, -1, -1), new Vector3f( 1, -1,  1), new Vector3f(-1, -1,  1)},
            // Right face
            {new Vector3f( 1, -1,  1), new Vector3f( 1, -1, -1), new Vector3f( 1,  1, -1), new Vector3f( 1,  1,  1)},
            // Left face
            {new Vector3f(-1, -1, -1), new Vector3f(-1, -1,  1), new Vector3f(-1,  1,  1), new Vector3f(-1,  1, -1)}
        };
        
        Vector3f[] faceNormals = {
            new Vector3f( 0,  0,  1), // Front
            new Vector3f( 0,  0, -1), // Back
            new Vector3f( 0,  1,  0), // Top
            new Vector3f( 0, -1,  0), // Bottom
            new Vector3f( 1,  0,  0), // Right
            new Vector3f(-1,  0,  0)  // Left
        };
        
        Vector2f[] faceTexCoords = {
            new Vector2f(0, 0), new Vector2f(1, 0), new Vector2f(1, 1), new Vector2f(0, 1)
        };
        
        for (int face = 0; face < 6; face++) {
            int baseIndex = positions.size();
            
            for (int vertex = 0; vertex < 4; vertex++) {
                positions.add(faces[face][vertex]);
                normals.add(faceNormals[face]);
                texCoords.add(faceTexCoords[vertex]);
            }
            
            // Add indices for two triangles
            indices.add(baseIndex);
            indices.add(baseIndex + 1);
            indices.add(baseIndex + 2);
            indices.add(baseIndex + 2);
            indices.add(baseIndex + 3);
            indices.add(baseIndex);
        }
        
        cubeMesh = createMesh("cube", positions, normals, texCoords, indices);
    }
    
    /**
     * Creates a sphere mesh
     */
    private void createSphereMesh() {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int rings = 16;
        int sectors = 32;
        float radius = 1.0f;
        
        // Generate vertices
        for (int ring = 0; ring <= rings; ring++) {
            float phi = (float) Math.PI * ring / rings;
            float y = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);
            
            for (int sector = 0; sector <= sectors; sector++) {
                float theta = 2.0f * (float) Math.PI * sector / sectors;
                float x = ringRadius * (float) Math.cos(theta);
                float z = ringRadius * (float) Math.sin(theta);
                
                Vector3f position = new Vector3f(x * radius, y * radius, z * radius);
                Vector3f normal = new Vector3f(x, y, z).normalize();
                Vector2f texCoord = new Vector2f((float) sector / sectors, (float) ring / rings);
                
                positions.add(position);
                normals.add(normal);
                texCoords.add(texCoord);
            }
        }
        
        // Generate indices
        for (int ring = 0; ring < rings; ring++) {
            for (int sector = 0; sector < sectors; sector++) {
                int current = ring * (sectors + 1) + sector;
                int next = current + sectors + 1;
                
                indices.add(current);
                indices.add(next);
                indices.add(current + 1);
                
                indices.add(current + 1);
                indices.add(next);
                indices.add(next + 1);
            }
        }
        
        sphereMesh = createMesh("sphere", positions, normals, texCoords, indices);
    }
    
    /**
     * Creates a plane mesh
     */
    private void createPlaneMesh() {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int resolution = 10;
        float size = 10.0f;
        
        // Generate vertices
        for (int z = 0; z <= resolution; z++) {
            for (int x = 0; x <= resolution; x++) {
                float xPos = (x / (float) resolution - 0.5f) * size;
                float zPos = (z / (float) resolution - 0.5f) * size;
                
                positions.add(new Vector3f(xPos, 0, zPos));
                normals.add(new Vector3f(0, 1, 0));
                texCoords.add(new Vector2f(x / (float) resolution, z / (float) resolution));
            }
        }
        
        // Generate indices
        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                int topLeft = z * (resolution + 1) + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * (resolution + 1) + x;
                int bottomRight = bottomLeft + 1;
                
                indices.add(topLeft);
                indices.add(bottomLeft);
                indices.add(topRight);
                
                indices.add(topRight);
                indices.add(bottomLeft);
                indices.add(bottomRight);
            }
        }
        
        planeMesh = createMesh("plane", positions, normals, texCoords, indices);
    }
    
    /**
     * Gets mesh cache statistics
     */
    public void printCacheStats() {
        logger.info("Mesh cache: {} meshes loaded", meshCache.size());
        for (Map.Entry<String, Integer> entry : referenceCount.entrySet()) {
            logger.debug("  {} - {} references", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Cleanup all meshes
     */
    public void cleanup() {
        logger.info("Cleaning up MeshManager...");
        
        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        
        meshCache.clear();
        referenceCount.clear();
        
        logger.info("MeshManager cleanup complete");
    }
}