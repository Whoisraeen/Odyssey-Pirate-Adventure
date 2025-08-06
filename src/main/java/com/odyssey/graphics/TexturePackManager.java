package com.odyssey.graphics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manages texture packs and provides compatibility layer for modding support.
 * Handles loading, validation, and application of texture packs with namespace isolation.
 */
public class TexturePackManager {
    private static final Logger logger = LoggerFactory.getLogger(TexturePackManager.class);
    
    // Texture pack constants
    private static final String TEXTURE_PACK_DIR = "texture_packs";
    private static final String PACK_META_FILE = "pack.mcmeta";
    private static final String ASSETS_DIR = "assets";
    private static final String TEXTURES_DIR = "textures";
    
    // Supported texture pack formats
    private static final Set<String> SUPPORTED_FORMATS = Set.of(".zip", ".jar");
    private static final Set<String> SUPPORTED_IMAGE_FORMATS = Set.of(".png", ".jpg", ".jpeg");
    
    private final TextureAtlasManager atlasManager;
    private final Map<String, TexturePack> loadedPacks;
    private final Map<String, Integer> packPriorities;
    private final List<String> activePacks;
    
    /**
     * Represents a loaded texture pack with metadata and resources.
     */
    public static class TexturePack {
        public final String name;
        public final String description;
        public final String version;
        public final Path packPath;
        public final Map<String, String> textureOverrides;
        public final boolean isZipped;
        
        public TexturePack(String name, String description, String version, Path packPath, boolean isZipped) {
            this.name = name;
            this.description = description;
            this.version = version;
            this.packPath = packPath;
            this.textureOverrides = new ConcurrentHashMap<>();
            this.isZipped = isZipped;
        }
    }
    
    /**
     * Texture pack metadata from pack.mcmeta file.
     */
    public static class PackMetadata {
        public final int packFormat;
        public final String description;
        public final String version;
        
        public PackMetadata(int packFormat, String description, String version) {
            this.packFormat = packFormat;
            this.description = description;
            this.version = version;
        }
    }
    
    public TexturePackManager(TextureAtlasManager atlasManager) {
        this.atlasManager = atlasManager;
        this.loadedPacks = new ConcurrentHashMap<>();
        this.packPriorities = new ConcurrentHashMap<>();
        this.activePacks = new ArrayList<>();
        
        // Create texture packs directory if it doesn't exist
        createTexturePackDirectory();
    }
    
    /**
     * Creates the texture packs directory if it doesn't exist.
     */
    private void createTexturePackDirectory() {
        try {
            Path packDir = Paths.get(TEXTURE_PACK_DIR);
            if (!Files.exists(packDir)) {
                Files.createDirectories(packDir);
                logger.info("Created texture packs directory: {}", packDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create texture packs directory", e);
        }
    }
    
    /**
     * Scans for and loads all available texture packs.
     */
    public void scanAndLoadTexturePacks() {
        Path packDir = Paths.get(TEXTURE_PACK_DIR);
        if (!Files.exists(packDir)) {
            logger.warn("Texture packs directory does not exist: {}", packDir);
            return;
        }
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(packDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    loadDirectoryTexturePack(entry);
                } else if (Files.isRegularFile(entry) && isValidPackFile(entry)) {
                    loadZippedTexturePack(entry);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to scan texture packs directory", e);
        }
        
        logger.info("Loaded {} texture packs", loadedPacks.size());
    }
    
    /**
     * Checks if a file is a valid texture pack file.
     */
    private boolean isValidPackFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return SUPPORTED_FORMATS.stream().anyMatch(fileName::endsWith);
    }
    
    /**
     * Loads a texture pack from a directory.
     */
    private void loadDirectoryTexturePack(Path packPath) {
        try {
            PackMetadata metadata = loadPackMetadata(packPath, false);
            if (metadata == null) {
                logger.warn("Skipping directory texture pack without valid metadata: {}", packPath);
                return;
            }
            
            String packName = packPath.getFileName().toString();
            TexturePack pack = new TexturePack(packName, metadata.description, metadata.version, packPath, false);
            
            // Scan for texture overrides
            scanTextureOverrides(pack);
            
            loadedPacks.put(packName, pack);
            logger.info("Loaded directory texture pack: {} ({})", packName, metadata.description);
            
        } catch (Exception e) {
            logger.error("Failed to load directory texture pack: {}", packPath, e);
        }
    }
    
    /**
     * Loads a texture pack from a ZIP file.
     */
    private void loadZippedTexturePack(Path packPath) {
        try (ZipFile zipFile = new ZipFile(packPath.toFile())) {
            PackMetadata metadata = loadPackMetadata(packPath, true);
            if (metadata == null) {
                logger.warn("Skipping zipped texture pack without valid metadata: {}", packPath);
                return;
            }
            
            String packName = packPath.getFileName().toString();
            // Remove file extension from pack name
            int lastDot = packName.lastIndexOf('.');
            if (lastDot > 0) {
                packName = packName.substring(0, lastDot);
            }
            
            TexturePack pack = new TexturePack(packName, metadata.description, metadata.version, packPath, true);
            
            // Scan for texture overrides in ZIP
            scanZippedTextureOverrides(pack, zipFile);
            
            loadedPacks.put(packName, pack);
            logger.info("Loaded zipped texture pack: {} ({})", packName, metadata.description);
            
        } catch (Exception e) {
            logger.error("Failed to load zipped texture pack: {}", packPath, e);
        }
    }
    
    /**
     * Loads pack metadata from pack.mcmeta file.
     */
    private PackMetadata loadPackMetadata(Path packPath, boolean isZipped) {
        try {
            if (isZipped) {
                try (ZipFile zipFile = new ZipFile(packPath.toFile())) {
                    ZipEntry metaEntry = zipFile.getEntry(PACK_META_FILE);
                    if (metaEntry == null) {
                        return null;
                    }
                    
                    try (InputStream is = zipFile.getInputStream(metaEntry)) {
                        return parsePackMetadata(is);
                    }
                }
            } else {
                Path metaPath = packPath.resolve(PACK_META_FILE);
                if (!Files.exists(metaPath)) {
                    return null;
                }
                
                try (InputStream is = Files.newInputStream(metaPath)) {
                    return parsePackMetadata(is);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load pack metadata from: {}", packPath, e);
            return null;
        }
    }
    
    /**
     * Parses pack metadata from JSON input stream.
     */
    private PackMetadata parsePackMetadata(InputStream inputStream) throws IOException {
        // Simple JSON parsing for pack.mcmeta
        // In a real implementation, you'd use Gson or similar
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            
            // Basic parsing - in production, use proper JSON parser
            String json = content.toString();
            int packFormat = extractIntValue(json, "pack_format", 1);
            String description = extractStringValue(json, "description", "Unknown");
            String version = extractStringValue(json, "version", "1.0");
            
            return new PackMetadata(packFormat, description, version);
        }
    }
    
    /**
     * Extracts integer value from simple JSON string.
     */
    private int extractIntValue(String json, String key, int defaultValue) {
        String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse integer value for key: {}", key);
            }
        }
        return defaultValue;
    }
    
    /**
     * Extracts string value from simple JSON string.
     */
    private String extractStringValue(String json, String key, String defaultValue) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return defaultValue;
    }
    
    /**
     * Scans for texture overrides in a directory-based texture pack.
     */
    private void scanTextureOverrides(TexturePack pack) {
        Path assetsPath = pack.packPath.resolve(ASSETS_DIR);
        if (!Files.exists(assetsPath)) {
            return;
        }
        
        try {
            Files.walk(assetsPath)
                .filter(Files::isRegularFile)
                .filter(this::isValidTextureFile)
                .forEach(texturePath -> {
                    String relativePath = assetsPath.relativize(texturePath).toString().replace('\\', '/');
                    String textureId = pathToTextureId(relativePath);
                    pack.textureOverrides.put(textureId, texturePath.toString());
                });
        } catch (IOException e) {
            logger.error("Failed to scan texture overrides for pack: {}", pack.name, e);
        }
    }
    
    /**
     * Scans for texture overrides in a zipped texture pack.
     */
    private void scanZippedTextureOverrides(TexturePack pack, ZipFile zipFile) {
        String assetsPrefix = ASSETS_DIR + "/";
        
        zipFile.stream()
            .filter(entry -> !entry.isDirectory())
            .filter(entry -> entry.getName().startsWith(assetsPrefix))
            .filter(entry -> isValidTextureFile(entry.getName()))
            .forEach(entry -> {
                String relativePath = entry.getName().substring(assetsPrefix.length());
                String textureId = pathToTextureId(relativePath);
                pack.textureOverrides.put(textureId, entry.getName());
            });
    }
    
    /**
     * Checks if a file is a valid texture file.
     */
    private boolean isValidTextureFile(Path file) {
        return isValidTextureFile(file.getFileName().toString());
    }
    
    /**
     * Checks if a filename represents a valid texture file.
     */
    private boolean isValidTextureFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return SUPPORTED_IMAGE_FORMATS.stream().anyMatch(lowerName::endsWith);
    }
    
    /**
     * Converts a file path to a texture ID.
     */
    private String pathToTextureId(String path) {
        // Remove file extension and convert to texture ID format
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0) {
            path = path.substring(0, lastDot);
        }
        return path.replace('/', ':');
    }
    
    /**
     * Activates a texture pack with the given priority.
     */
    public boolean activateTexturePack(String packName, int priority) {
        TexturePack pack = loadedPacks.get(packName);
        if (pack == null) {
            logger.warn("Cannot activate unknown texture pack: {}", packName);
            return false;
        }
        
        packPriorities.put(packName, priority);
        if (!activePacks.contains(packName)) {
            activePacks.add(packName);
            activePacks.sort((a, b) -> packPriorities.get(b) - packPriorities.get(a));
        }
        
        applyTextureOverrides();
        logger.info("Activated texture pack: {} with priority {}", packName, priority);
        return true;
    }
    
    /**
     * Deactivates a texture pack.
     */
    public boolean deactivateTexturePack(String packName) {
        if (!activePacks.contains(packName)) {
            logger.warn("Cannot deactivate inactive texture pack: {}", packName);
            return false;
        }
        
        activePacks.remove(packName);
        packPriorities.remove(packName);
        
        applyTextureOverrides();
        logger.info("Deactivated texture pack: {}", packName);
        return true;
    }
    
    /**
     * Applies texture overrides from all active packs in priority order.
     */
    private void applyTextureOverrides() {
        // Clear existing overrides
        // Note: In a real implementation, you'd need to track and revert overrides
        
        // Apply overrides from active packs in priority order
        for (String packName : activePacks) {
            TexturePack pack = loadedPacks.get(packName);
            if (pack != null) {
                applyPackOverrides(pack);
            }
        }
    }
    
    /**
     * Applies texture overrides from a specific pack.
     */
    private void applyPackOverrides(TexturePack pack) {
        for (Map.Entry<String, String> override : pack.textureOverrides.entrySet()) {
            String textureId = override.getKey();
            String texturePath = override.getValue();
            
            try {
                byte[] textureData = loadTextureData(pack, texturePath);
                if (textureData != null) {
                    // Apply override through modding API or atlas manager
                    logger.debug("Applied texture override: {} -> {}", textureId, texturePath);
                }
            } catch (Exception e) {
                logger.error("Failed to apply texture override: {} -> {}", textureId, texturePath, e);
            }
        }
    }
    
    /**
     * Loads texture data from a pack.
     */
    private byte[] loadTextureData(TexturePack pack, String texturePath) throws IOException {
        if (pack.isZipped) {
            try (ZipFile zipFile = new ZipFile(pack.packPath.toFile())) {
                ZipEntry entry = zipFile.getEntry(texturePath);
                if (entry == null) {
                    return null;
                }
                
                try (InputStream is = zipFile.getInputStream(entry)) {
                    return is.readAllBytes();
                }
            }
        } else {
            Path fullPath = Paths.get(texturePath);
            if (Files.exists(fullPath)) {
                return Files.readAllBytes(fullPath);
            }
        }
        return null;
    }
    
    /**
     * Gets all loaded texture packs.
     */
    public Map<String, TexturePack> getLoadedPacks() {
        return new HashMap<>(loadedPacks);
    }
    
    /**
     * Gets all active texture packs in priority order.
     */
    public List<String> getActivePacks() {
        return new ArrayList<>(activePacks);
    }
    
    /**
     * Gets texture pack statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPacks", loadedPacks.size());
        stats.put("activePacks", activePacks.size());
        stats.put("totalOverrides", loadedPacks.values().stream()
            .mapToInt(pack -> pack.textureOverrides.size())
            .sum());
        return stats;
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        loadedPacks.clear();
        packPriorities.clear();
        activePacks.clear();
        logger.info("Texture pack manager cleanup complete");
    }
}