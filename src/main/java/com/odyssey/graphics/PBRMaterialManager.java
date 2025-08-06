package com.odyssey.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages PBR materials, including loading, caching, and texture assignment.
 * Provides automatic texture discovery and loading for PBR material workflows.
 */
public class PBRMaterialManager {
    private static final Logger logger = LoggerFactory.getLogger(PBRMaterialManager.class);
    
    private final TextureAtlasManager atlasManager;
    private final Map<String, PBRMaterial> materials;
    private final Map<String, CompletableFuture<PBRMaterial>> loadingMaterials;
    
    // Default materials for fallback
    private PBRMaterial defaultMaterial;
    private PBRMaterial errorMaterial;
    
    /**
     * Creates a new PBR material manager.
     * 
     * @param atlasManager The texture atlas manager for loading textures
     */
    public PBRMaterialManager(TextureAtlasManager atlasManager) {
        this.atlasManager = atlasManager;
        this.materials = new ConcurrentHashMap<>();
        this.loadingMaterials = new ConcurrentHashMap<>();
        
        initializeDefaultMaterials();
        logger.info("PBR Material Manager initialized");
    }
    
    /**
     * Initializes default materials for fallback scenarios.
     */
    private void initializeDefaultMaterials() {
        // Create default material (neutral gray, slightly rough)
        defaultMaterial = new PBRMaterial("default", "odyssey")
            .setAlbedoFactor(0.7f, 0.7f, 0.7f, 1.0f)
            .setMetallicFactor(0.0f)
            .setRoughnessFactor(0.8f);
        
        // Create error material (bright magenta for debugging)
        errorMaterial = new PBRMaterial("error", "odyssey")
            .setAlbedoFactor(1.0f, 0.0f, 1.0f, 1.0f)
            .setMetallicFactor(0.0f)
            .setRoughnessFactor(1.0f);
        
        materials.put(defaultMaterial.getFullName(), defaultMaterial);
        materials.put(errorMaterial.getFullName(), errorMaterial);
        
        logger.debug("Default materials initialized");
    }
    
    /**
     * Registers a material manually.
     * 
     * @param material The material to register
     * @return True if registration was successful, false if material already exists
     */
    public boolean registerMaterial(PBRMaterial material) {
        String fullName = material.getFullName();
        
        if (materials.containsKey(fullName)) {
            logger.warn("Material already exists: {}", fullName);
            return false;
        }
        
        materials.put(fullName, material);
        logger.debug("Registered material: {}", fullName);
        return true;
    }
    
    /**
     * Loads a PBR material by automatically discovering and loading its textures.
     * 
     * @param materialName The material name (without namespace)
     * @param namespace The material namespace
     * @param basePath The base path for texture files
     * @return CompletableFuture that completes when the material is loaded
     */
    public CompletableFuture<PBRMaterial> loadMaterialAsync(String materialName, String namespace, String basePath) {
        String fullName = namespace + ":" + materialName;
        
        // Check if already loaded
        PBRMaterial existing = materials.get(fullName);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }
        
        // Check if already loading
        CompletableFuture<PBRMaterial> loading = loadingMaterials.get(fullName);
        if (loading != null) {
            return loading;
        }
        
        // Start loading
        CompletableFuture<PBRMaterial> future = CompletableFuture.supplyAsync(() -> {
            try {
                return loadMaterialSync(materialName, namespace, basePath);
            } catch (Exception e) {
                logger.error("Failed to load material: {} - {}", fullName, e.getMessage());
                return errorMaterial;
            }
        });
        
        loadingMaterials.put(fullName, future);
        
        // Clean up loading map when done
        future.whenComplete((material, throwable) -> {
            loadingMaterials.remove(fullName);
            if (material != null && material != errorMaterial) {
                materials.put(fullName, material);
            }
        });
        
        return future;
    }
    
    /**
     * Loads a material synchronously.
     * 
     * @param materialName The material name
     * @param namespace The namespace
     * @param basePath The base path for textures
     * @return The loaded material
     */
    private PBRMaterial loadMaterialSync(String materialName, String namespace, String basePath) {
        PBRMaterial material = new PBRMaterial(materialName, namespace);
        
        // Try to load each texture type
        for (PBRMaterial.TextureType textureType : PBRMaterial.TextureType.values()) {
            String textureName = material.getExpectedTextureName(textureType);
            String texturePath = basePath + "/" + textureName + ".png";
            
            try {
                // Request texture loading with appropriate priority
                TextureAtlasManager.StreamingPriority priority = 
                    (textureType == PBRMaterial.TextureType.ALBEDO) ? 
                    TextureAtlasManager.StreamingPriority.HIGH : 
                    TextureAtlasManager.StreamingPriority.MEDIUM;
                
                CompletableFuture<TextureAtlasManager.AtlasTextureReference> textureRef = 
                    atlasManager.requestTextureAsync(textureName, texturePath, 
                                                   TextureAtlasManager.AtlasCategory.BLOCKS, 
                                                   priority, null);
                
                // Wait for texture to load (with timeout)
                TextureAtlasManager.AtlasTextureReference texture = textureRef.get();
                
                if (texture != null) {
                    assignTextureToMaterial(material, textureType, texture);
                    logger.debug("Loaded texture: {} for material: {}", textureName, material.getFullName());
                }
                
            } catch (Exception e) {
                // Texture loading failed, continue without this texture
                logger.debug("Could not load texture: {} for material: {} - {}", 
                           textureName, material.getFullName(), e.getMessage());
            }
        }
        
        logger.info("Loaded PBR material: {} (textures: {})", material.getFullName(), material.hasTextures());
        return material;
    }
    
    /**
     * Assigns a texture to the appropriate slot in a material.
     * 
     * @param material The material to modify
     * @param textureType The type of texture
     * @param texture The texture reference
     */
    private void assignTextureToMaterial(PBRMaterial material, PBRMaterial.TextureType textureType, 
                                       TextureAtlasManager.AtlasTextureReference texture) {
        switch (textureType) {
            case ALBEDO:
                material.setAlbedoTexture(texture);
                break;
            case NORMAL:
                material.setNormalTexture(texture);
                break;
            case METALLIC:
                material.setMetallicTexture(texture);
                break;
            case ROUGHNESS:
                material.setRoughnessTexture(texture);
                break;
            case AMBIENT_OCCLUSION:
                material.setAoTexture(texture);
                break;
            case EMISSION:
                material.setEmissionTexture(texture);
                break;
            case HEIGHT:
                material.setHeightTexture(texture);
                break;
            case SUBSURFACE:
                material.setSubsurfaceTexture(texture);
                break;
        }
    }
    
    /**
     * Gets a material by its full name.
     * 
     * @param fullName The full material name (namespace:name)
     * @return The material, or default material if not found
     */
    public PBRMaterial getMaterial(String fullName) {
        PBRMaterial material = materials.get(fullName);
        return material != null ? material : defaultMaterial;
    }
    
    /**
     * Gets a material by namespace and name.
     * 
     * @param namespace The namespace
     * @param name The material name
     * @return The material, or default material if not found
     */
    public PBRMaterial getMaterial(String namespace, String name) {
        return getMaterial(namespace + ":" + name);
    }
    
    /**
     * Checks if a material exists.
     * 
     * @param fullName The full material name
     * @return True if the material exists
     */
    public boolean hasMaterial(String fullName) {
        return materials.containsKey(fullName);
    }
    
    /**
     * Gets all registered material names.
     * 
     * @return Set of all material names
     */
    public Set<String> getAllMaterialNames() {
        return materials.keySet();
    }
    
    /**
     * Creates a material variant with modified properties.
     * 
     * @param baseMaterialName The base material to copy from
     * @param variantName The new variant name
     * @param namespace The namespace for the variant
     * @return The created variant material
     */
    public PBRMaterial createMaterialVariant(String baseMaterialName, String variantName, String namespace) {
        PBRMaterial baseMaterial = getMaterial(baseMaterialName);
        if (baseMaterial == defaultMaterial) {
            logger.warn("Base material not found: {}, using default", baseMaterialName);
        }
        
        PBRMaterial variant = baseMaterial.copy(variantName);
        String fullName = namespace + ":" + variantName;
        materials.put(fullName, variant);
        
        logger.debug("Created material variant: {} from base: {}", fullName, baseMaterialName);
        return variant;
    }
    
    /**
     * Preloads a set of materials for a specific namespace.
     * 
     * @param namespace The namespace to preload
     * @param materialNames The material names to preload
     * @param basePath The base path for textures
     * @return CompletableFuture that completes when all materials are loaded
     */
    public CompletableFuture<Void> preloadMaterials(String namespace, String[] materialNames, String basePath) {
        CompletableFuture<?>[] futures = new CompletableFuture[materialNames.length];
        
        for (int i = 0; i < materialNames.length; i++) {
            futures[i] = loadMaterialAsync(materialNames[i], namespace, basePath);
        }
        
        return CompletableFuture.allOf(futures);
    }
    
    /**
     * Gets the default material.
     * 
     * @return The default material
     */
    public PBRMaterial getDefaultMaterial() {
        return defaultMaterial;
    }
    
    /**
     * Gets the error material (used for debugging).
     * 
     * @return The error material
     */
    public PBRMaterial getErrorMaterial() {
        return errorMaterial;
    }
    
    /**
     * Gets material loading statistics.
     * 
     * @return Formatted statistics string
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("PBR Material Manager Statistics:\n");
        stats.append("Total materials: ").append(materials.size()).append("\n");
        stats.append("Currently loading: ").append(loadingMaterials.size()).append("\n");
        
        // Count materials by namespace
        Map<String, Integer> namespaceCount = new HashMap<>();
        for (String fullName : materials.keySet()) {
            String namespace = fullName.split(":")[0];
            namespaceCount.put(namespace, namespaceCount.getOrDefault(namespace, 0) + 1);
        }
        
        stats.append("Materials by namespace:\n");
        for (Map.Entry<String, Integer> entry : namespaceCount.entrySet()) {
            stats.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Cleans up material manager resources.
     */
    public void cleanup() {
        logger.info("Cleaning up PBR material manager");
        
        materials.clear();
        loadingMaterials.clear();
        
        logger.info("PBR material manager cleanup complete");
    }
}