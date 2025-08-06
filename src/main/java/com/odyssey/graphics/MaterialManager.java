package com.odyssey.graphics;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all materials in the game, providing registration, caching, and retrieval
 * of PBR materials. Supports both built-in materials and runtime material creation.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class MaterialManager {
    private static final Logger logger = LoggerFactory.getLogger(MaterialManager.class);
    
    // Material storage
    private final Map<String, Material> materialsByName;
    private final Map<Integer, Material> materialsById;
    private int nextMaterialId;
    
    // Default materials
    private Material defaultMaterial;
    private Material errorMaterial;
    
    /**
     * Creates a new MaterialManager.
     */
    public MaterialManager() {
        this.materialsByName = new ConcurrentHashMap<>();
        this.materialsById = new ConcurrentHashMap<>();
        this.nextMaterialId = 1;
        
        logger.info("MaterialManager initialized");
    }
    
    /**
     * Initializes the material system with built-in materials.
     */
    public void initialize() {
        logger.info("Loading built-in materials...");
        
        // Create default materials
        createDefaultMaterials();
        
        // Create block materials
        createBlockMaterials();
        
        // Create entity materials
        createEntityMaterials();
        
        logger.info("Loaded {} materials", materialsByName.size());
    }
    
    /**
     * Creates essential default materials.
     */
    private void createDefaultMaterials() {
        // Default material (fallback)
        defaultMaterial = Material.createDefault("default", getNextId());
        defaultMaterial.setAlbedo(new Vector3f(0.8f, 0.8f, 0.8f));
        registerMaterial(defaultMaterial);
        
        // Error material (magenta for missing materials)
        errorMaterial = Material.createDefault("error", getNextId());
        errorMaterial.setAlbedo(new Vector3f(1.0f, 0.0f, 1.0f)); // Bright magenta
        registerMaterial(errorMaterial);
        
        logger.debug("Created default materials");
    }
    
    /**
     * Creates materials for various block types.
     */
    private void createBlockMaterials() {
        // Stone materials
        registerMaterial(Material.createDielectric("stone", getNextId(), 
                        new Vector3f(0.6f, 0.6f, 0.6f), 0.8f));
        registerMaterial(Material.createDielectric("cobblestone", getNextId(), 
                        new Vector3f(0.5f, 0.5f, 0.5f), 0.9f));
        registerMaterial(Material.createDielectric("limestone", getNextId(), 
                        new Vector3f(0.9f, 0.9f, 0.8f), 0.7f));
        registerMaterial(Material.createDielectric("marble", getNextId(), 
                        new Vector3f(0.95f, 0.95f, 0.95f), 0.2f));
        registerMaterial(Material.createDielectric("volcanic_rock", getNextId(), 
                        new Vector3f(0.3f, 0.2f, 0.2f), 0.9f));
        
        // Wood materials
        registerMaterial(Material.createDielectric("wood", getNextId(), 
                        new Vector3f(0.6f, 0.4f, 0.2f), 0.7f));
        registerMaterial(Material.createDielectric("palm_wood", getNextId(), 
                        new Vector3f(0.7f, 0.5f, 0.3f), 0.6f));
        
        // Dirt and organic materials
        registerMaterial(Material.createDielectric("dirt", getNextId(), 
                        new Vector3f(0.4f, 0.3f, 0.2f), 0.9f));
        registerMaterial(Material.createDielectric("grass", getNextId(), 
                        new Vector3f(0.3f, 0.6f, 0.2f), 0.8f));
        registerMaterial(Material.createDielectric("sand", getNextId(), 
                        new Vector3f(0.9f, 0.8f, 0.6f), 0.6f));
        
        // Foliage materials
        registerMaterial(Material.createDielectric("leaves", getNextId(), 
                        new Vector3f(0.2f, 0.6f, 0.2f), 0.7f));
        
        // Ore materials (metallic)
        registerMaterial(Material.createMetallic("coal_ore", getNextId(), 
                        new Vector3f(0.2f, 0.2f, 0.2f)));
        registerMaterial(Material.createMetallic("iron_ore", getNextId(), 
                        new Vector3f(0.7f, 0.5f, 0.3f)));
        registerMaterial(Material.createMetallic("gold_ore", getNextId(), 
                        new Vector3f(1.0f, 0.8f, 0.2f)));
        
        // Special materials
        registerMaterial(Material.createDielectric("obsidian", getNextId(), 
                        new Vector3f(0.1f, 0.1f, 0.1f), 0.1f)); // Very smooth and dark
        
        // Emissive materials
        Material lava = Material.createEmissive("lava", getNextId(), 
                       new Vector3f(1.0f, 0.3f, 0.1f), new Vector3f(2.0f, 0.5f, 0.1f));
        lava.setAnimated(true);
        lava.setFrameCount(8);
        lava.setAnimationSpeed(2.0f);
        registerMaterial(lava);
        
        // Ocean materials
        registerMaterial(Material.createDielectric("coral", getNextId(), 
                        new Vector3f(1.0f, 0.4f, 0.4f), 0.3f));
        registerMaterial(Material.createDielectric("seaweed", getNextId(), 
                        new Vector3f(0.1f, 0.4f, 0.2f), 0.8f));
        
        // Crystal materials
        Material crystal = Material.createDielectric("crystal", getNextId(), 
                          new Vector3f(0.9f, 0.9f, 1.0f), 0.0f);
        crystal.setAlpha(0.8f); // Semi-transparent
        registerMaterial(crystal);
        
        logger.debug("Created {} block materials", materialsByName.size() - 2); // Exclude default and error
    }
    
    /**
     * Creates materials for entities and special objects.
     */
    private void createEntityMaterials() {
        // Water material (transparent)
        Material water = Material.createDielectric("water", getNextId(), 
                        new Vector3f(0.1f, 0.3f, 0.6f), 0.0f);
        water.setAlpha(0.7f);
        water.setAnimated(true);
        water.setFrameCount(16);
        water.setAnimationSpeed(1.0f);
        registerMaterial(water);
        
        // Ship materials
        registerMaterial(Material.createDielectric("ship_hull", getNextId(), 
                        new Vector3f(0.5f, 0.3f, 0.2f), 0.6f));
        registerMaterial(Material.createDielectric("sail", getNextId(), 
                        new Vector3f(0.9f, 0.9f, 0.8f), 0.4f));
        
        // Metal materials for tools and equipment
        registerMaterial(Material.createMetallic("iron", getNextId(), 
                        new Vector3f(0.6f, 0.6f, 0.6f)));
        registerMaterial(Material.createMetallic("gold", getNextId(), 
                        new Vector3f(1.0f, 0.8f, 0.2f)));
        registerMaterial(Material.createMetallic("copper", getNextId(), 
                        new Vector3f(0.9f, 0.5f, 0.3f)));
        
        logger.debug("Created entity materials");
    }
    
    /**
     * Registers a material in the manager.
     * 
     * @param material The material to register
     * @return true if registration was successful, false if a material with the same name already exists
     */
    public boolean registerMaterial(Material material) {
        if (material == null) {
            logger.warn("Cannot register null material");
            return false;
        }
        
        String name = material.getName();
        int id = material.getId();
        
        // Check for name conflicts
        if (materialsByName.containsKey(name)) {
            logger.warn("Material with name '{}' already exists", name);
            return false;
        }
        
        // Check for ID conflicts
        if (materialsById.containsKey(id)) {
            logger.warn("Material with ID {} already exists", id);
            return false;
        }
        
        // Register the material
        materialsByName.put(name, material);
        materialsById.put(id, material);
        
        logger.debug("Registered material: {} (ID: {})", name, id);
        return true;
    }
    
    /**
     * Gets a material by name.
     * 
     * @param name The material name
     * @return The material, or the error material if not found
     */
    public Material getMaterial(String name) {
        if (name == null || name.isEmpty()) {
            return defaultMaterial;
        }
        
        Material material = materialsByName.get(name);
        if (material == null) {
            logger.warn("Material '{}' not found, using error material", name);
            return errorMaterial;
        }
        
        return material;
    }
    
    /**
     * Gets a material by ID.
     * 
     * @param id The material ID
     * @return The material, or the error material if not found
     */
    public Material getMaterial(int id) {
        Material material = materialsById.get(id);
        if (material == null) {
            logger.warn("Material with ID {} not found, using error material", id);
            return errorMaterial;
        }
        
        return material;
    }
    
    /**
     * Gets the default material.
     * 
     * @return The default material
     */
    public Material getDefaultMaterial() {
        return defaultMaterial;
    }
    
    /**
     * Gets the error material (used for missing materials).
     * 
     * @return The error material
     */
    public Material getErrorMaterial() {
        return errorMaterial;
    }
    
    /**
     * Checks if a material with the given name exists.
     * 
     * @param name The material name
     * @return true if the material exists, false otherwise
     */
    public boolean hasMaterial(String name) {
        return materialsByName.containsKey(name);
    }
    
    /**
     * Checks if a material with the given ID exists.
     * 
     * @param id The material ID
     * @return true if the material exists, false otherwise
     */
    public boolean hasMaterial(int id) {
        return materialsById.containsKey(id);
    }
    
    /**
     * Updates all animated materials.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        for (Material material : materialsByName.values()) {
            if (material.isAnimated()) {
                material.update(deltaTime);
            }
        }
    }
    
    /**
     * Gets the next available material ID.
     * 
     * @return The next material ID
     */
    private int getNextId() {
        return nextMaterialId++;
    }
    
    /**
     * Gets the total number of registered materials.
     * 
     * @return The material count
     */
    public int getMaterialCount() {
        return materialsByName.size();
    }
    
    /**
     * Gets all registered material names.
     * 
     * @return A map of material names to materials
     */
    public Map<String, Material> getAllMaterials() {
        return new HashMap<>(materialsByName);
    }
    
    /**
     * Cleans up all materials and releases resources.
     */
    public void cleanup() {
        logger.info("Cleaning up MaterialManager...");
        
        materialsByName.clear();
        materialsById.clear();
        
        logger.info("MaterialManager cleaned up");
    }
    
    @Override
    public String toString() {
        return String.format("MaterialManager{materials=%d, nextId=%d}", 
                           materialsByName.size(), nextMaterialId);
    }
}