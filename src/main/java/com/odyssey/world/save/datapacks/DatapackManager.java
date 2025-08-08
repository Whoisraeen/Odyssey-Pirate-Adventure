package com.odyssey.world.save.datapacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.odyssey.world.save.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Manages datapack loading and validation for world customization.
 * Supports both folder and ZIP-based datapacks.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class DatapackManager {
    private static final Logger logger = LoggerFactory.getLogger(DatapackManager.class);
    
    private final Path datapacksDirectory;
    private final Gson gson;
    private final Map<String, Datapack> loadedDatapacks;
    private final Set<String> enabledDatapacks;
    
    public DatapackManager(Path worldDirectory) {
        this.datapacksDirectory = worldDirectory.resolve(WorldSaveFormat.DATAPACKS_DIR);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.loadedDatapacks = new ConcurrentHashMap<>();
        this.enabledDatapacks = ConcurrentHashMap.newKeySet();
        
        initializeDatapacksDirectory();
    }
    
    /**
     * Initializes the datapacks directory structure.
     */
    private void initializeDatapacksDirectory() {
        try {
            Files.createDirectories(datapacksDirectory);
            logger.info("Initialized datapacks directory: {}", datapacksDirectory);
        } catch (IOException e) {
            logger.error("Failed to create datapacks directory: {}", datapacksDirectory, e);
        }
    }
    
    /**
     * Scans and loads all datapacks in the directory.
     */
    public void loadAllDatapacks() {
        loadedDatapacks.clear();
        
        try {
            if (!Files.exists(datapacksDirectory)) {
                return;
            }
            
            Files.list(datapacksDirectory)
                    .forEach(this::loadDatapack);
            
            logger.info("Loaded {} datapacks", loadedDatapacks.size());
        } catch (IOException e) {
            logger.error("Failed to scan datapacks directory", e);
        }
    }
    
    /**
     * Loads a single datapack from a path.
     */
    private void loadDatapack(Path datapackPath) {
        try {
            Datapack datapack;
            
            if (Files.isDirectory(datapackPath)) {
                datapack = loadFolderDatapack(datapackPath);
            } else if (datapackPath.toString().endsWith(".zip")) {
                datapack = loadZipDatapack(datapackPath);
            } else {
                logger.debug("Skipping non-datapack file: {}", datapackPath);
                return;
            }
            
            if (datapack != null && validateDatapack(datapack)) {
                loadedDatapacks.put(datapack.getId(), datapack);
                logger.info("Loaded datapack: {} v{}", datapack.getName(), datapack.getVersion());
            }
        } catch (Exception e) {
            logger.error("Failed to load datapack: {}", datapackPath, e);
        }
    }
    
    /**
     * Loads a folder-based datapack.
     */
    private Datapack loadFolderDatapack(Path folderPath) throws IOException {
        Path packMcmetaPath = folderPath.resolve(WorldSaveFormat.PACK_MCMETA_FILE);
        
        if (!Files.exists(packMcmetaPath)) {
            logger.warn("Datapack missing pack.mcmeta: {}", folderPath);
            return null;
        }
        
        try (FileReader reader = new FileReader(packMcmetaPath.toFile())) {
            PackMcmeta mcmeta = gson.fromJson(reader, PackMcmeta.class);
            return new Datapack(
                    folderPath.getFileName().toString(),
                    mcmeta.getPack().getDescription(),
                    mcmeta.getPack().getPackFormat(),
                    "1.0.0",
                    folderPath,
                    DatapackType.FOLDER
            );
        } catch (JsonSyntaxException e) {
            logger.error("Invalid pack.mcmeta format: {}", packMcmetaPath, e);
            return null;
        }
    }
    
    /**
     * Loads a ZIP-based datapack.
     */
    private Datapack loadZipDatapack(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry packMcmetaEntry = zipFile.getEntry(WorldSaveFormat.PACK_MCMETA_FILE);
            
            if (packMcmetaEntry == null) {
                logger.warn("ZIP datapack missing pack.mcmeta: {}", zipPath);
                return null;
            }
            
            try (InputStream inputStream = zipFile.getInputStream(packMcmetaEntry);
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                
                PackMcmeta mcmeta = gson.fromJson(reader, PackMcmeta.class);
                String id = zipPath.getFileName().toString().replaceAll("\\.zip$", "");
                
                return new Datapack(
                        id,
                        mcmeta.getPack().getDescription(),
                        mcmeta.getPack().getPackFormat(),
                        "1.0.0",
                        zipPath,
                        DatapackType.ZIP
                );
            }
        } catch (JsonSyntaxException e) {
            logger.error("Invalid pack.mcmeta format in ZIP: {}", zipPath, e);
            return null;
        }
    }
    
    /**
     * Validates a datapack structure and compatibility.
     */
    private boolean validateDatapack(Datapack datapack) {
        // Check pack format compatibility
        if (datapack.getPackFormat() < 1 || datapack.getPackFormat() > 10) {
            logger.warn("Unsupported pack format {} for datapack: {}", 
                    datapack.getPackFormat(), datapack.getName());
            return false;
        }
        
        // Additional validation can be added here
        return true;
    }
    
    /**
     * Enables a datapack by ID.
     */
    public boolean enableDatapack(String datapackId) {
        if (loadedDatapacks.containsKey(datapackId)) {
            enabledDatapacks.add(datapackId);
            logger.info("Enabled datapack: {}", datapackId);
            return true;
        }
        return false;
    }
    
    /**
     * Disables a datapack by ID.
     */
    public boolean disableDatapack(String datapackId) {
        boolean removed = enabledDatapacks.remove(datapackId);
        if (removed) {
            logger.info("Disabled datapack: {}", datapackId);
        }
        return removed;
    }
    
    /**
     * Gets all loaded datapacks.
     */
    public Collection<Datapack> getAllDatapacks() {
        return Collections.unmodifiableCollection(loadedDatapacks.values());
    }
    
    /**
     * Gets all enabled datapacks.
     */
    public Collection<Datapack> getEnabledDatapacks() {
        return enabledDatapacks.stream()
                .map(loadedDatapacks::get)
                .filter(Objects::nonNull)
                .toList();
    }
    
    /**
     * Gets a datapack by ID.
     */
    public Datapack getDatapack(String id) {
        return loadedDatapacks.get(id);
    }
    
    /**
     * Checks if a datapack is enabled.
     */
    public boolean isDatapackEnabled(String datapackId) {
        return enabledDatapacks.contains(datapackId);
    }
    
    /**
     * Gets the number of loaded datapacks.
     */
    public int getLoadedDatapackCount() {
        return loadedDatapacks.size();
    }
    
    /**
     * Gets the number of enabled datapacks.
     */
    public int getEnabledDatapackCount() {
        return enabledDatapacks.size();
    }
    
    /**
     * Reloads all datapacks.
     */
    public void reloadDatapacks() {
        Set<String> previouslyEnabled = new HashSet<>(enabledDatapacks);
        loadAllDatapacks();
        
        // Re-enable previously enabled datapacks
        for (String datapackId : previouslyEnabled) {
            enableDatapack(datapackId);
        }
        
        logger.info("Reloaded datapacks");
    }
}