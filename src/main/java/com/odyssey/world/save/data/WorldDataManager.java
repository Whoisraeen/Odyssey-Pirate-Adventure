package com.odyssey.world.save.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.odyssey.world.save.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages persistent world data storage in the data/ folder.
 * Handles global world state, configuration, and custom data.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WorldDataManager {
    private static final Logger logger = LoggerFactory.getLogger(WorldDataManager.class);
    
    private final Path dataDirectory;
    private final Gson gson;
    private final ConcurrentHashMap<String, Object> dataCache;
    private final ReadWriteLock cacheLock;
    
    public WorldDataManager(Path worldDirectory) {
        this.dataDirectory = worldDirectory.resolve(WorldSaveFormat.DATA_DIR);
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.dataCache = new ConcurrentHashMap<>();
        this.cacheLock = new ReentrantReadWriteLock();
        
        initializeDataDirectory();
    }
    
    /**
     * Initializes the data directory structure.
     */
    private void initializeDataDirectory() {
        try {
            Files.createDirectories(dataDirectory);
            logger.info("Initialized world data directory: {}", dataDirectory);
        } catch (IOException e) {
            logger.error("Failed to create data directory: {}", dataDirectory, e);
        }
    }
    
    /**
     * Stores data with the specified key.
     */
    public <T> void setData(String key, T data) {
        cacheLock.writeLock().lock();
        try {
            dataCache.put(key, data);
            saveDataToFile(key, data);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Retrieves data by key.
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        cacheLock.readLock().lock();
        try {
            Object cached = dataCache.get(key);
            if (cached != null) {
                return (T) cached;
            }
            
            T loaded = loadDataFromFile(key, type);
            if (loaded != null) {
                dataCache.put(key, loaded);
            }
            return loaded;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Checks if data exists for the specified key.
     */
    public boolean hasData(String key) {
        cacheLock.readLock().lock();
        try {
            if (dataCache.containsKey(key)) {
                return true;
            }
            
            Path filePath = getDataFilePath(key);
            return Files.exists(filePath);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Removes data by key.
     */
    public void removeData(String key) {
        cacheLock.writeLock().lock();
        try {
            dataCache.remove(key);
            
            Path filePath = getDataFilePath(key);
            try {
                Files.deleteIfExists(filePath);
                logger.debug("Removed data file: {}", filePath);
            } catch (IOException e) {
                logger.error("Failed to delete data file: {}", filePath, e);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Saves all cached data to disk.
     */
    public void saveAll() {
        cacheLock.readLock().lock();
        try {
            for (var entry : dataCache.entrySet()) {
                saveDataToFile(entry.getKey(), entry.getValue());
            }
            logger.info("Saved all world data to disk");
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Clears the data cache.
     */
    public void clearCache() {
        cacheLock.writeLock().lock();
        try {
            dataCache.clear();
            logger.debug("Cleared world data cache");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the number of cached data entries.
     */
    public int getCacheSize() {
        cacheLock.readLock().lock();
        try {
            return dataCache.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Saves data to file.
     */
    private <T> void saveDataToFile(String key, T data) {
        Path filePath = getDataFilePath(key);
        
        try {
            Files.createDirectories(filePath.getParent());
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                gson.toJson(data, writer);
            }
            
            logger.debug("Saved data to file: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to save data to file: {}", filePath, e);
        }
    }
    
    /**
     * Loads data from file.
     */
    private <T> T loadDataFromFile(String key, Class<T> type) {
        Path filePath = getDataFilePath(key);
        
        if (!Files.exists(filePath)) {
            return null;
        }
        
        try (FileReader reader = new FileReader(filePath.toFile())) {
            T data = gson.fromJson(reader, type);
            logger.debug("Loaded data from file: {}", filePath);
            return data;
        } catch (IOException | JsonSyntaxException e) {
            logger.error("Failed to load data from file: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * Gets the file path for a data key.
     */
    private Path getDataFilePath(String key) {
        String fileName = key.replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
        return dataDirectory.resolve(fileName);
    }
    
    /**
     * Gets the data directory path.
     */
    public Path getDataDirectory() {
        return dataDirectory;
    }
}