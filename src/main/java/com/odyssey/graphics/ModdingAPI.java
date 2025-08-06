package com.odyssey.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Modding API for texture registration and management.
 * Provides a safe interface for mods to register custom textures with namespace isolation.
 */
public class ModdingAPI {
    private static final Logger logger = LoggerFactory.getLogger(ModdingAPI.class);
    
    // Namespace validation pattern (alphanumeric + underscore, 3-32 chars)
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9_]{3,32}$");
    
    // Texture name validation pattern (alphanumeric + underscore + hyphen, 1-64 chars)
    private static final Pattern TEXTURE_NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");
    
    /**
     * Represents a mod's texture registration context.
     */
    public static class ModContext {
        private final String namespace;
        private final String modName;
        private final String version;
        private final Map<String, String> registeredTextures;
        private final Map<String, String> textureOverrides;
        
        public ModContext(String namespace, String modName, String version) {
            this.namespace = namespace;
            this.modName = modName;
            this.version = version;
            this.registeredTextures = new ConcurrentHashMap<>();
            this.textureOverrides = new ConcurrentHashMap<>();
        }
        
        public String getNamespace() { return namespace; }
        public String getModName() { return modName; }
        public String getVersion() { return version; }
        public Set<String> getRegisteredTextures() { return registeredTextures.keySet(); }
        public Set<String> getTextureOverrides() { return textureOverrides.keySet(); }
        
        /**
         * Gets the full namespaced texture ID.
         * @param textureName The local texture name
         * @return The full namespaced ID (namespace:textureName)
         */
        public String getNamespacedId(String textureName) {
            return namespace + ":" + textureName;
        }
    }
    
    /**
     * Texture registration result.
     */
    public enum RegistrationResult {
        SUCCESS,
        INVALID_NAMESPACE,
        INVALID_TEXTURE_NAME,
        NAMESPACE_NOT_REGISTERED,
        TEXTURE_ALREADY_EXISTS,
        TEXTURE_DATA_INVALID,
        ATLAS_FULL,
        UNKNOWN_ERROR
    }
    
    private final TextureAtlasManager atlasManager;
    private final Map<String, ModContext> registeredNamespaces;
    private final Map<String, String> textureOverrides; // original -> override mapping
    private final Map<String, String> textureOwnership; // texture ID -> namespace
    
    /**
     * Creates a new modding API instance.
     * 
     * @param atlasManager The texture atlas manager to use for texture storage
     */
    public ModdingAPI(TextureAtlasManager atlasManager) {
        this.atlasManager = atlasManager;
        this.registeredNamespaces = new ConcurrentHashMap<>();
        this.textureOverrides = new ConcurrentHashMap<>();
        this.textureOwnership = new ConcurrentHashMap<>();
        
        logger.info("Modding API initialized");
    }
    
    /**
     * Registers a mod namespace for texture registration.
     * 
     * @param namespace The namespace to register (must be unique)
     * @param modName The human-readable mod name
     * @param version The mod version
     * @return True if registration was successful, false if namespace already exists
     */
    public boolean registerNamespace(String namespace, String modName, String version) {
        if (!isValidNamespace(namespace)) {
            logger.warn("Invalid namespace format: {}", namespace);
            return false;
        }
        
        if (registeredNamespaces.containsKey(namespace)) {
            logger.warn("Namespace already registered: {}", namespace);
            return false;
        }
        
        // Reserve core namespaces
        if (namespace.equals("odyssey") || namespace.equals("minecraft") || namespace.equals("core")) {
            logger.warn("Cannot register reserved namespace: {}", namespace);
            return false;
        }
        
        ModContext context = new ModContext(namespace, modName, version);
        registeredNamespaces.put(namespace, context);
        
        logger.info("Registered mod namespace: {} ({})", namespace, modName);
        return true;
    }
    
    /**
     * Registers a custom texture for a mod.
     * 
     * @param namespace The mod's namespace
     * @param textureName The local texture name (within the namespace)
     * @param textureData The texture data as ByteBuffer (RGBA format)
     * @param width The texture width in pixels
     * @param height The texture height in pixels
     * @param category The atlas category to place the texture in
     * @return The registration result
     */
    public RegistrationResult registerTexture(String namespace, String textureName, 
                                            ByteBuffer textureData, int width, int height,
                                            TextureAtlasManager.AtlasCategory category) {
        // Validate namespace
        if (!registeredNamespaces.containsKey(namespace)) {
            return RegistrationResult.NAMESPACE_NOT_REGISTERED;
        }
        
        // Validate texture name
        if (!isValidTextureName(textureName)) {
            return RegistrationResult.INVALID_TEXTURE_NAME;
        }
        
        // Validate texture data
        if (textureData == null || width <= 0 || height <= 0) {
            return RegistrationResult.TEXTURE_DATA_INVALID;
        }
        
        String namespacedId = namespace + ":" + textureName;
        
        // Check if texture already exists
        if (atlasManager.hasTexture(namespacedId)) {
            return RegistrationResult.TEXTURE_ALREADY_EXISTS;
        }
        
        try {
            // Add texture to atlas
            TextureAtlasManager.AtlasTextureReference reference = 
                atlasManager.addTexture(namespacedId, textureData, width, height, category);
            
            if (reference == null) {
                return RegistrationResult.ATLAS_FULL;
            }
            
            // Track ownership and registration
            ModContext context = registeredNamespaces.get(namespace);
            context.registeredTextures.put(textureName, namespacedId);
            textureOwnership.put(namespacedId, namespace);
            
            logger.debug("Registered texture: {} for mod: {}", namespacedId, context.getModName());
            return RegistrationResult.SUCCESS;
            
        } catch (Exception e) {
            logger.error("Failed to register texture: {} - {}", namespacedId, e.getMessage());
            return RegistrationResult.UNKNOWN_ERROR;
        }
    }
    
    /**
     * Registers a texture override to replace an existing texture.
     * 
     * @param namespace The mod's namespace
     * @param originalTextureId The original texture ID to override
     * @param overrideTextureName The name of the override texture (must be registered first)
     * @return True if override was successful, false otherwise
     */
    public boolean registerTextureOverride(String namespace, String originalTextureId, String overrideTextureName) {
        if (!registeredNamespaces.containsKey(namespace)) {
            logger.warn("Namespace not registered: {}", namespace);
            return false;
        }
        
        ModContext context = registeredNamespaces.get(namespace);
        String overrideId = context.getNamespacedId(overrideTextureName);
        
        // Check if override texture exists
        if (!atlasManager.hasTexture(overrideId)) {
            logger.warn("Override texture not found: {}", overrideId);
            return false;
        }
        
        // Register the override
        textureOverrides.put(originalTextureId, overrideId);
        context.textureOverrides.put(originalTextureId, overrideId);
        
        logger.info("Registered texture override: {} -> {} (mod: {})", 
                   originalTextureId, overrideId, context.getModName());
        return true;
    }
    
    /**
     * Gets the effective texture ID, considering overrides.
     * 
     * @param textureId The original texture ID
     * @return The effective texture ID (override if exists, original otherwise)
     */
    public String getEffectiveTextureId(String textureId) {
        return textureOverrides.getOrDefault(textureId, textureId);
    }
    
    /**
     * Gets a texture reference, considering overrides.
     * 
     * @param textureId The original texture ID
     * @return The texture reference, or null if not found
     */
    public TextureAtlasManager.AtlasTextureReference getTexture(String textureId) {
        String effectiveId = getEffectiveTextureId(textureId);
        return atlasManager.getTexture(effectiveId);
    }
    
    /**
     * Loads a texture pack that may contain texture overrides.
     * 
     * @param namespace The namespace for the texture pack
     * @param texturePackPath Path to the texture pack directory
     * @return CompletableFuture that completes when loading is done
     */
    public CompletableFuture<Boolean> loadTexturePack(String namespace, String texturePackPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // This is a placeholder implementation
                // In a real implementation, you would scan the texture pack directory
                // and load textures according to a specific format
                
                logger.info("Loading texture pack from: {} (namespace: {})", texturePackPath, namespace);
                
                // TODO: Implement actual texture pack loading
                // - Scan directory for texture files
                // - Parse pack.mcmeta or similar metadata file
                // - Load textures and register overrides
                
                return true;
            } catch (Exception e) {
                logger.error("Failed to load texture pack: {} - {}", texturePackPath, e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Unregisters a mod namespace and all its textures.
     * 
     * @param namespace The namespace to unregister
     * @return True if unregistration was successful, false if namespace not found
     */
    public boolean unregisterNamespace(String namespace) {
        ModContext context = registeredNamespaces.remove(namespace);
        if (context == null) {
            return false;
        }
        
        // Remove all textures owned by this namespace
        for (String textureId : context.getRegisteredTextures()) {
            String namespacedId = context.getNamespacedId(textureId);
            textureOwnership.remove(namespacedId);
            // Note: We don't remove from atlas manager as other systems might still reference it
        }
        
        // Remove overrides registered by this namespace
        for (String originalId : context.getTextureOverrides()) {
            textureOverrides.remove(originalId);
        }
        
        logger.info("Unregistered mod namespace: {} ({})", namespace, context.getModName());
        return true;
    }
    
    /**
     * Gets information about a registered namespace.
     * 
     * @param namespace The namespace to query
     * @return The mod context, or null if not found
     */
    public ModContext getNamespaceInfo(String namespace) {
        return registeredNamespaces.get(namespace);
    }
    
    /**
     * Gets all registered namespaces.
     * 
     * @return Set of all registered namespace names
     */
    public Set<String> getRegisteredNamespaces() {
        return registeredNamespaces.keySet();
    }
    
    /**
     * Gets modding statistics for monitoring.
     * 
     * @return Formatted statistics string
     */
    public String getModdingStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Modding API Statistics:\n");
        stats.append("Registered namespaces: ").append(registeredNamespaces.size()).append("\n");
        stats.append("Total texture overrides: ").append(textureOverrides.size()).append("\n\n");
        
        for (ModContext context : registeredNamespaces.values()) {
            stats.append("Namespace: ").append(context.getNamespace()).append("\n");
            stats.append("  Mod: ").append(context.getModName()).append(" v").append(context.getVersion()).append("\n");
            stats.append("  Textures: ").append(context.getRegisteredTextures().size()).append("\n");
            stats.append("  Overrides: ").append(context.getTextureOverrides().size()).append("\n\n");
        }
        
        return stats.toString();
    }
    
    /**
     * Validates a namespace format.
     * 
     * @param namespace The namespace to validate
     * @return True if valid, false otherwise
     */
    private boolean isValidNamespace(String namespace) {
        return namespace != null && NAMESPACE_PATTERN.matcher(namespace).matches();
    }
    
    /**
     * Validates a texture name format.
     * 
     * @param textureName The texture name to validate
     * @return True if valid, false otherwise
     */
    private boolean isValidTextureName(String textureName) {
        return textureName != null && TEXTURE_NAME_PATTERN.matcher(textureName).matches();
    }
    
    /**
     * Cleans up modding API resources.
     */
    public void cleanup() {
        logger.info("Cleaning up modding API");
        
        registeredNamespaces.clear();
        textureOverrides.clear();
        textureOwnership.clear();
        
        logger.info("Modding API cleanup complete");
    }
}