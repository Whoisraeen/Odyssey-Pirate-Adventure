package com.odyssey.graphics;

import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Physically Based Rendering (PBR) material definition.
 * Supports the metallic-roughness workflow with additional maps for enhanced realism.
 */
public class PBRMaterial {
    
    /**
     * PBR texture types for material definition.
     */
    public enum TextureType {
        ALBEDO("albedo", "Base color/diffuse texture"),
        NORMAL("normal", "Normal map for surface detail"),
        METALLIC("metallic", "Metallic map (grayscale)"),
        ROUGHNESS("roughness", "Roughness map (grayscale)"),
        AMBIENT_OCCLUSION("ao", "Ambient occlusion map"),
        EMISSION("emission", "Emissive texture"),
        HEIGHT("height", "Height/displacement map"),
        SUBSURFACE("subsurface", "Subsurface scattering map");
        
        private final String suffix;
        private final String description;
        
        TextureType(String suffix, String description) {
            this.suffix = suffix;
            this.description = description;
        }
        
        public String getSuffix() { return suffix; }
        public String getDescription() { return description; }
    }
    
    /**
     * PBR workflow types.
     */
    public enum Workflow {
        METALLIC_ROUGHNESS,  // Standard PBR workflow
        SPECULAR_GLOSSINESS  // Alternative workflow (less common)
    }
    
    // Material identification
    private final String name;
    private final String namespace;
    
    // PBR properties
    private final Workflow workflow;
    private Vector4f albedoFactor;      // Base color multiplier (RGBA)
    private float metallicFactor;       // Metallic multiplier (0.0 = dielectric, 1.0 = metallic)
    private float roughnessFactor;      // Roughness multiplier (0.0 = mirror, 1.0 = completely rough)
    private Vector3f emissiveFactor;    // Emissive color multiplier (RGB)
    private float normalScale;          // Normal map intensity
    private float occlusionStrength;    // AO map strength
    private float heightScale;          // Height map displacement scale
    private float alphaCutoff;          // Alpha testing cutoff value
    
    // Texture references
    private TextureAtlasManager.AtlasTextureReference albedoTexture;
    private TextureAtlasManager.AtlasTextureReference normalTexture;
    private TextureAtlasManager.AtlasTextureReference metallicTexture;
    private TextureAtlasManager.AtlasTextureReference roughnessTexture;
    private TextureAtlasManager.AtlasTextureReference aoTexture;
    private TextureAtlasManager.AtlasTextureReference emissionTexture;
    private TextureAtlasManager.AtlasTextureReference heightTexture;
    private TextureAtlasManager.AtlasTextureReference subsurfaceTexture;
    
    // Rendering flags
    private boolean doubleSided;
    private boolean alphaBlend;
    private boolean alphaTesting;
    private boolean castShadows;
    private boolean receiveShadows;
    
    // Animation properties
    private boolean animated;
    private float animationSpeed;
    private int frameCount;
    
    /**
     * Creates a new PBR material with default values.
     * 
     * @param name The material name
     * @param namespace The material namespace (for modding support)
     */
    public PBRMaterial(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
        this.workflow = Workflow.METALLIC_ROUGHNESS;
        
        // Default PBR values
        this.albedoFactor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
        this.metallicFactor = 0.0f;
        this.roughnessFactor = 1.0f;
        this.emissiveFactor = new Vector3f(0.0f, 0.0f, 0.0f);
        this.normalScale = 1.0f;
        this.occlusionStrength = 1.0f;
        this.heightScale = 0.05f;
        this.alphaCutoff = 0.5f;
        
        // Default rendering flags
        this.doubleSided = false;
        this.alphaBlend = false;
        this.alphaTesting = false;
        this.castShadows = true;
        this.receiveShadows = true;
        
        // Default animation properties
        this.animated = false;
        this.animationSpeed = 1.0f;
        this.frameCount = 1;
    }
    
    /**
     * Creates a PBR material in the default namespace.
     * 
     * @param name The material name
     */
    public PBRMaterial(String name) {
        this(name, "odyssey");
    }
    
    // Getters
    public String getName() { return name; }
    public String getNamespace() { return namespace; }
    public String getFullName() { return namespace + ":" + name; }
    public Workflow getWorkflow() { return workflow; }
    
    public Vector4f getAlbedoFactor() { return new Vector4f(albedoFactor); }
    public float getMetallicFactor() { return metallicFactor; }
    public float getRoughnessFactor() { return roughnessFactor; }
    public Vector3f getEmissiveFactor() { return new Vector3f(emissiveFactor); }
    public float getNormalScale() { return normalScale; }
    public float getOcclusionStrength() { return occlusionStrength; }
    public float getHeightScale() { return heightScale; }
    public float getAlphaCutoff() { return alphaCutoff; }
    
    public boolean isDoubleSided() { return doubleSided; }
    public boolean isAlphaBlend() { return alphaBlend; }
    public boolean isAlphaTesting() { return alphaTesting; }
    public boolean isCastShadows() { return castShadows; }
    public boolean isReceiveShadows() { return receiveShadows; }
    
    public boolean isAnimated() { return animated; }
    public float getAnimationSpeed() { return animationSpeed; }
    public int getFrameCount() { return frameCount; }
    
    // Texture getters
    public TextureAtlasManager.AtlasTextureReference getAlbedoTexture() { return albedoTexture; }
    public TextureAtlasManager.AtlasTextureReference getNormalTexture() { return normalTexture; }
    public TextureAtlasManager.AtlasTextureReference getMetallicTexture() { return metallicTexture; }
    public TextureAtlasManager.AtlasTextureReference getRoughnessTexture() { return roughnessTexture; }
    public TextureAtlasManager.AtlasTextureReference getAoTexture() { return aoTexture; }
    public TextureAtlasManager.AtlasTextureReference getEmissionTexture() { return emissionTexture; }
    public TextureAtlasManager.AtlasTextureReference getHeightTexture() { return heightTexture; }
    public TextureAtlasManager.AtlasTextureReference getSubsurfaceTexture() { return subsurfaceTexture; }
    
    // Setters for PBR factors
    public PBRMaterial setAlbedoFactor(Vector4f albedoFactor) {
        this.albedoFactor.set(albedoFactor);
        return this;
    }
    
    public PBRMaterial setAlbedoFactor(float r, float g, float b, float a) {
        this.albedoFactor.set(r, g, b, a);
        return this;
    }
    
    public PBRMaterial setMetallicFactor(float metallicFactor) {
        this.metallicFactor = Math.max(0.0f, Math.min(1.0f, metallicFactor));
        return this;
    }
    
    public PBRMaterial setRoughnessFactor(float roughnessFactor) {
        this.roughnessFactor = Math.max(0.0f, Math.min(1.0f, roughnessFactor));
        return this;
    }
    
    public PBRMaterial setEmissiveFactor(Vector3f emissiveFactor) {
        this.emissiveFactor.set(emissiveFactor);
        return this;
    }
    
    public PBRMaterial setEmissiveFactor(float r, float g, float b) {
        this.emissiveFactor.set(r, g, b);
        return this;
    }
    
    public PBRMaterial setNormalScale(float normalScale) {
        this.normalScale = Math.max(0.0f, normalScale);
        return this;
    }
    
    public PBRMaterial setOcclusionStrength(float occlusionStrength) {
        this.occlusionStrength = Math.max(0.0f, Math.min(1.0f, occlusionStrength));
        return this;
    }
    
    public PBRMaterial setHeightScale(float heightScale) {
        this.heightScale = heightScale;
        return this;
    }
    
    public PBRMaterial setAlphaCutoff(float alphaCutoff) {
        this.alphaCutoff = Math.max(0.0f, Math.min(1.0f, alphaCutoff));
        return this;
    }
    
    // Setters for rendering flags
    public PBRMaterial setDoubleSided(boolean doubleSided) {
        this.doubleSided = doubleSided;
        return this;
    }
    
    public PBRMaterial setAlphaBlend(boolean alphaBlend) {
        this.alphaBlend = alphaBlend;
        return this;
    }
    
    public PBRMaterial setAlphaTesting(boolean alphaTesting) {
        this.alphaTesting = alphaTesting;
        return this;
    }
    
    public PBRMaterial setCastShadows(boolean castShadows) {
        this.castShadows = castShadows;
        return this;
    }
    
    public PBRMaterial setReceiveShadows(boolean receiveShadows) {
        this.receiveShadows = receiveShadows;
        return this;
    }
    
    // Setters for animation
    public PBRMaterial setAnimated(boolean animated) {
        this.animated = animated;
        return this;
    }
    
    public PBRMaterial setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = Math.max(0.0f, animationSpeed);
        return this;
    }
    
    public PBRMaterial setFrameCount(int frameCount) {
        this.frameCount = Math.max(1, frameCount);
        return this;
    }
    
    // Texture setters
    public PBRMaterial setAlbedoTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.albedoTexture = texture;
        return this;
    }
    
    public PBRMaterial setNormalTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.normalTexture = texture;
        return this;
    }
    
    public PBRMaterial setMetallicTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.metallicTexture = texture;
        return this;
    }
    
    public PBRMaterial setRoughnessTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.roughnessTexture = texture;
        return this;
    }
    
    public PBRMaterial setAoTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.aoTexture = texture;
        return this;
    }
    
    public PBRMaterial setEmissionTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.emissionTexture = texture;
        return this;
    }
    
    public PBRMaterial setHeightTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.heightTexture = texture;
        return this;
    }
    
    public PBRMaterial setSubsurfaceTexture(TextureAtlasManager.AtlasTextureReference texture) {
        this.subsurfaceTexture = texture;
        return this;
    }
    
    /**
     * Checks if the material has any textures assigned.
     * 
     * @return True if at least one texture is assigned
     */
    public boolean hasTextures() {
        return albedoTexture != null || normalTexture != null || 
               metallicTexture != null || roughnessTexture != null ||
               aoTexture != null || emissionTexture != null ||
               heightTexture != null || subsurfaceTexture != null;
    }
    
    /**
     * Checks if the material is transparent (uses alpha blending or testing).
     * 
     * @return True if the material is transparent
     */
    public boolean isTransparent() {
        return alphaBlend || alphaTesting || albedoFactor.w < 1.0f;
    }
    
    /**
     * Checks if the material is emissive.
     * 
     * @return True if the material emits light
     */
    public boolean isEmissive() {
        return emissiveFactor.lengthSquared() > 0.0f || emissionTexture != null;
    }
    
    /**
     * Gets the expected texture name for a given type.
     * 
     * @param type The texture type
     * @return The expected texture name
     */
    public String getExpectedTextureName(TextureType type) {
        return getFullName() + "_" + type.getSuffix();
    }
    
    /**
     * Creates a copy of this material with a new name.
     * 
     * @param newName The new material name
     * @return A copy of this material
     */
    public PBRMaterial copy(String newName) {
        PBRMaterial copy = new PBRMaterial(newName, namespace);
        
        // Copy all properties
        copy.albedoFactor.set(this.albedoFactor);
        copy.metallicFactor = this.metallicFactor;
        copy.roughnessFactor = this.roughnessFactor;
        copy.emissiveFactor.set(this.emissiveFactor);
        copy.normalScale = this.normalScale;
        copy.occlusionStrength = this.occlusionStrength;
        copy.heightScale = this.heightScale;
        copy.alphaCutoff = this.alphaCutoff;
        
        copy.doubleSided = this.doubleSided;
        copy.alphaBlend = this.alphaBlend;
        copy.alphaTesting = this.alphaTesting;
        copy.castShadows = this.castShadows;
        copy.receiveShadows = this.receiveShadows;
        
        copy.animated = this.animated;
        copy.animationSpeed = this.animationSpeed;
        copy.frameCount = this.frameCount;
        
        // Copy texture references
        copy.albedoTexture = this.albedoTexture;
        copy.normalTexture = this.normalTexture;
        copy.metallicTexture = this.metallicTexture;
        copy.roughnessTexture = this.roughnessTexture;
        copy.aoTexture = this.aoTexture;
        copy.emissionTexture = this.emissionTexture;
        copy.heightTexture = this.heightTexture;
        copy.subsurfaceTexture = this.subsurfaceTexture;
        
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("PBRMaterial{name='%s', namespace='%s', metallic=%.2f, roughness=%.2f, hasTextures=%b}", 
                           name, namespace, metallicFactor, roughnessFactor, hasTextures());
    }
}