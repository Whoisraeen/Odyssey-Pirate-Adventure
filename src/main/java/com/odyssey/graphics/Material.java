package com.odyssey.graphics;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Physically Based Rendering (PBR) material.
 * Contains all the properties needed for realistic material rendering including
 * albedo, normal, metallic, roughness, and ambient occlusion.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class Material {
    private static final Logger logger = LoggerFactory.getLogger(Material.class);
    
    // Material identification
    private final String name;
    private final int id;
    
    // PBR properties
    private Vector3f albedo;           // Base color (RGB)
    private float metallic;            // Metallic factor (0.0 = dielectric, 1.0 = metallic)
    private float roughness;           // Surface roughness (0.0 = mirror, 1.0 = completely rough)
    private float ambientOcclusion;    // Ambient occlusion factor (0.0 = fully occluded, 1.0 = no occlusion)
    private Vector3f emission;         // Emissive color for glowing materials
    private float alpha;               // Transparency (0.0 = transparent, 1.0 = opaque)
    
    // Texture references
    private Texture albedoTexture;
    private Texture normalTexture;
    private Texture metallicTexture;
    private Texture roughnessTexture;
    private Texture aoTexture;
    private Texture emissionTexture;
    
    // Material flags
    private boolean transparent;
    private boolean doubleSided;
    private boolean emissive;
    private boolean animated;
    
    // Animation properties
    private float animationSpeed;
    private int frameCount;
    private float currentFrame;
    
    /**
     * Creates a new material with the specified name and ID.
     * 
     * @param name The material name
     * @param id The unique material ID
     */
    public Material(String name, int id) {
        this.name = name;
        this.id = id;
        
        // Set default PBR values
        this.albedo = new Vector3f(0.8f, 0.8f, 0.8f);  // Light gray
        this.metallic = 0.0f;                           // Non-metallic by default
        this.roughness = 0.5f;                          // Medium roughness
        this.ambientOcclusion = 1.0f;                   // No occlusion
        this.emission = new Vector3f(0.0f, 0.0f, 0.0f); // No emission
        this.alpha = 1.0f;                              // Fully opaque
        
        // Default flags
        this.transparent = false;
        this.doubleSided = false;
        this.emissive = false;
        this.animated = false;
        
        // Animation defaults
        this.animationSpeed = 1.0f;
        this.frameCount = 1;
        this.currentFrame = 0.0f;
        
        logger.debug("Created material: {} (ID: {})", name, id);
    }
    
    /**
     * Updates animated materials.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(double deltaTime) {
        if (animated && frameCount > 1) {
            currentFrame += (float) (deltaTime * animationSpeed);
            if (currentFrame >= frameCount) {
                currentFrame = 0.0f;
            }
        }
    }
    
    /**
     * Binds this material's textures to the specified shader.
     * 
     * @param shader The shader to bind textures to
     */
    public void bind(Shader shader) {
        if (shader == null) {
            logger.warn("Cannot bind material {} to null shader", name);
            return;
        }
        
        // Bind PBR properties
        shader.setUniform("material.albedo", albedo);
        shader.setUniform("material.metallic", metallic);
        shader.setUniform("material.roughness", roughness);
        shader.setUniform("material.ao", ambientOcclusion);
        shader.setUniform("material.emission", emission);
        shader.setUniform("material.alpha", alpha);
        
        // Bind material flags
        shader.setUniform("material.transparent", transparent);
        shader.setUniform("material.doubleSided", doubleSided);
        shader.setUniform("material.emissive", emissive);
        shader.setUniform("material.animated", animated);
        
        // Bind animation properties
        if (animated) {
            shader.setUniform("material.animationFrame", getCurrentFrameIndex());
            shader.setUniform("material.frameCount", frameCount);
        }
        
        // Bind textures
        bindTextures(shader);
    }
    
    /**
     * Binds all material textures to the shader.
     * 
     * @param shader The shader to bind textures to
     */
    private void bindTextures(Shader shader) {
        int textureUnit = 0;
        
        // Bind albedo texture
        if (albedoTexture != null) {
            albedoTexture.bind(textureUnit);
            shader.setUniform("material.albedoTexture", textureUnit);
            shader.setUniform("material.hasAlbedoTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasAlbedoTexture", false);
        }
        
        // Bind normal texture
        if (normalTexture != null) {
            normalTexture.bind(textureUnit);
            shader.setUniform("material.normalTexture", textureUnit);
            shader.setUniform("material.hasNormalTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasNormalTexture", false);
        }
        
        // Bind metallic texture
        if (metallicTexture != null) {
            metallicTexture.bind(textureUnit);
            shader.setUniform("material.metallicTexture", textureUnit);
            shader.setUniform("material.hasMetallicTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasMetallicTexture", false);
        }
        
        // Bind roughness texture
        if (roughnessTexture != null) {
            roughnessTexture.bind(textureUnit);
            shader.setUniform("material.roughnessTexture", textureUnit);
            shader.setUniform("material.hasRoughnessTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasRoughnessTexture", false);
        }
        
        // Bind AO texture
        if (aoTexture != null) {
            aoTexture.bind(textureUnit);
            shader.setUniform("material.aoTexture", textureUnit);
            shader.setUniform("material.hasAoTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasAoTexture", false);
        }
        
        // Bind emission texture
        if (emissionTexture != null) {
            emissionTexture.bind(textureUnit);
            shader.setUniform("material.emissionTexture", textureUnit);
            shader.setUniform("material.hasEmissionTexture", true);
            textureUnit++;
        } else {
            shader.setUniform("material.hasEmissionTexture", false);
        }
    }
    
    /**
     * Gets the current animation frame index.
     * 
     * @return The current frame index
     */
    public int getCurrentFrameIndex() {
        return (int) Math.floor(currentFrame);
    }
    
    /**
     * Creates a default material with basic properties.
     * 
     * @param name The material name
     * @param id The material ID
     * @return A new default material
     */
    public static Material createDefault(String name, int id) {
        return new Material(name, id);
    }
    
    /**
     * Creates a metallic material (like iron or gold).
     * 
     * @param name The material name
     * @param id The material ID
     * @param color The metallic color
     * @return A new metallic material
     */
    public static Material createMetallic(String name, int id, Vector3f color) {
        Material material = new Material(name, id);
        material.setAlbedo(color);
        material.setMetallic(1.0f);
        material.setRoughness(0.1f);
        return material;
    }
    
    /**
     * Creates a dielectric material (like wood or stone).
     * 
     * @param name The material name
     * @param id The material ID
     * @param color The base color
     * @param roughness The surface roughness
     * @return A new dielectric material
     */
    public static Material createDielectric(String name, int id, Vector3f color, float roughness) {
        Material material = new Material(name, id);
        material.setAlbedo(color);
        material.setMetallic(0.0f);
        material.setRoughness(roughness);
        return material;
    }
    
    /**
     * Creates an emissive material (like lava or glowstone).
     * 
     * @param name The material name
     * @param id The material ID
     * @param color The base color
     * @param emission The emission color
     * @return A new emissive material
     */
    public static Material createEmissive(String name, int id, Vector3f color, Vector3f emission) {
        Material material = new Material(name, id);
        material.setAlbedo(color);
        material.setEmission(emission);
        material.setEmissive(true);
        return material;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public int getId() { return id; }
    
    public Vector3f getAlbedo() { return new Vector3f(albedo); }
    public void setAlbedo(Vector3f albedo) { this.albedo.set(albedo); }
    public void setAlbedo(float r, float g, float b) { this.albedo.set(r, g, b); }
    
    public float getMetallic() { return metallic; }
    public void setMetallic(float metallic) { this.metallic = Math.max(0.0f, Math.min(1.0f, metallic)); }
    
    public float getRoughness() { return roughness; }
    public void setRoughness(float roughness) { this.roughness = Math.max(0.0f, Math.min(1.0f, roughness)); }
    
    public float getAmbientOcclusion() { return ambientOcclusion; }
    public void setAmbientOcclusion(float ao) { this.ambientOcclusion = Math.max(0.0f, Math.min(1.0f, ao)); }
    
    public Vector3f getEmission() { return new Vector3f(emission); }
    public void setEmission(Vector3f emission) { this.emission.set(emission); }
    public void setEmission(float r, float g, float b) { this.emission.set(r, g, b); }
    
    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { 
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.transparent = this.alpha < 1.0f;
    }
    
    public boolean isTransparent() { return transparent; }
    public void setTransparent(boolean transparent) { this.transparent = transparent; }
    
    public boolean isDoubleSided() { return doubleSided; }
    public void setDoubleSided(boolean doubleSided) { this.doubleSided = doubleSided; }
    
    public boolean isEmissive() { return emissive; }
    public void setEmissive(boolean emissive) { this.emissive = emissive; }
    
    public boolean isAnimated() { return animated; }
    public void setAnimated(boolean animated) { this.animated = animated; }
    
    public float getAnimationSpeed() { return animationSpeed; }
    public void setAnimationSpeed(float speed) { this.animationSpeed = Math.max(0.0f, speed); }
    
    public int getFrameCount() { return frameCount; }
    public void setFrameCount(int frameCount) { this.frameCount = Math.max(1, frameCount); }
    
    // Texture setters
    public void setAlbedoTexture(Texture texture) { this.albedoTexture = texture; }
    public void setNormalTexture(Texture texture) { this.normalTexture = texture; }
    public void setMetallicTexture(Texture texture) { this.metallicTexture = texture; }
    public void setRoughnessTexture(Texture texture) { this.roughnessTexture = texture; }
    public void setAoTexture(Texture texture) { this.aoTexture = texture; }
    public void setEmissionTexture(Texture texture) { this.emissionTexture = texture; }
    
    // Texture getters
    public Texture getAlbedoTexture() { return albedoTexture; }
    public Texture getNormalTexture() { return normalTexture; }
    public Texture getMetallicTexture() { return metallicTexture; }
    public Texture getRoughnessTexture() { return roughnessTexture; }
    public Texture getAoTexture() { return aoTexture; }
    public Texture getEmissionTexture() { return emissionTexture; }
    
    @Override
    public String toString() {
        return String.format("Material{name='%s', id=%d, albedo=%s, metallic=%.2f, roughness=%.2f}", 
                           name, id, albedo, metallic, roughness);
    }
}