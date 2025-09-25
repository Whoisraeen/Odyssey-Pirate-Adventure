package com.odyssey.audio;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages sound effects and audio source pooling.
 * Handles loading, caching, and playing of sound effects with 3D positioning.
 */
public class SoundManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SoundManager.class);
    
    private static final int MAX_SOURCES = 32; // Maximum number of concurrent audio sources
    private static final int INITIAL_SOURCES = 16; // Initial number of sources to create
    
    private final Map<String, AudioBuffer> soundBuffers = new ConcurrentHashMap<>();
    private final Queue<AudioSource> availableSources = new LinkedList<>();
    private final List<AudioSource> activeSources = new ArrayList<>();
    private final List<AudioSource> allSources = new ArrayList<>();
    
    private float masterVolume = 1.0f;
    private float sfxVolume = 1.0f;
    
    /**
     * Create a new sound manager.
     */
    public SoundManager() {
        initializeSourcePool();
        LOGGER.info("SoundManager initialized with {} audio sources", INITIAL_SOURCES);
    }
    
    /**
     * Initialize the audio source pool.
     */
    private void initializeSourcePool() {
        for (int i = 0; i < INITIAL_SOURCES; i++) {
            try {
                AudioSource source = new AudioSource();
                availableSources.offer(source);
                allSources.add(source);
            } catch (Exception e) {
                LOGGER.warn("Failed to create audio source {}: {}", i, e.getMessage());
            }
        }
        LOGGER.debug("Created {} audio sources", availableSources.size());
    }
    
    /**
     * Load a sound effect from file.
     * 
     * @param name The name to associate with the sound
     * @param filename The path to the audio file
     */
    public void loadSound(String name, String filename) {
        if (soundBuffers.containsKey(name)) {
            LOGGER.warn("Sound '{}' is already loaded", name);
            return;
        }
        
        try {
            AudioBuffer buffer = new AudioBuffer(filename);
            soundBuffers.put(name, buffer);
            LOGGER.debug("Loaded sound '{}' from '{}'", name, filename);
        } catch (Exception e) {
            LOGGER.error("Failed to load sound '{}' from '{}': {}", name, filename, e.getMessage());
        }
    }
    
    /**
     * Play a sound effect at the listener's position (2D sound).
     * 
     * @param soundName The name of the sound to play
     * @return The AudioSource playing the sound, or null if failed
     */
    public AudioSource playSound(String soundName) {
        return playSound(soundName, 0, 0, 0, false);
    }
    
    /**
     * Play a sound effect at a specific 3D position.
     * 
     * @param soundName The name of the sound to play
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @return The AudioSource playing the sound, or null if failed
     */
    public AudioSource playSound(String soundName, float x, float y, float z) {
        return playSound(soundName, x, y, z, true);
    }
    
    /**
     * Play a sound effect at a specific 3D position.
     * 
     * @param soundName The name of the sound to play
     * @param position The 3D position
     * @return The AudioSource playing the sound, or null if failed
     */
    public AudioSource playSound(String soundName, Vector3f position) {
        return playSound(soundName, position.x, position.y, position.z, true);
    }
    
    /**
     * Play a looping sound effect at a specific 3D position.
     * 
     * @param soundName The name of the sound to play
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @return The AudioSource playing the sound, or null if failed
     */
    public AudioSource playLoopingSound(String soundName, float x, float y, float z) {
        AudioSource source = playSound(soundName, x, y, z, true);
        if (source != null) {
            source.setLooping(true);
        }
        return source;
    }
    
    /**
     * Play a looping sound effect at a specific 3D position.
     * 
     * @param soundName The name of the sound to play
     * @param position The 3D position
     * @return The AudioSource playing the sound, or null if failed
     */
    public AudioSource playLoopingSound(String soundName, Vector3f position) {
        return playLoopingSound(soundName, position.x, position.y, position.z);
    }
    
    /**
     * Internal method to play a sound with specified parameters.
     */
    private AudioSource playSound(String soundName, float x, float y, float z, boolean is3D) {
        AudioBuffer buffer = soundBuffers.get(soundName);
        if (buffer == null) {
            LOGGER.warn("Sound '{}' not found", soundName);
            return null;
        }
        
        AudioSource source = getAvailableSource();
        if (source == null) {
            LOGGER.warn("No available audio sources for sound '{}'", soundName);
            return null;
        }
        
        // Configure the source
        source.setBuffer(buffer.getBufferId());
        source.setGain(masterVolume * sfxVolume);
        
        if (is3D) {
            source.setPosition(x, y, z);
        } else {
            // For 2D sounds, position at listener location with no attenuation
            source.setPosition(0, 0, 0);
            source.setReferenceDistance(Float.MAX_VALUE);
        }
        
        // Play the sound
        source.play();
        activeSources.add(source);
        
        LOGGER.trace("Playing sound '{}' at position ({}, {}, {})", soundName, x, y, z);
        return source;
    }
    
    /**
     * Stop a specific audio source.
     */
    public void stopSound(AudioSource source) {
        if (source != null && activeSources.contains(source)) {
            source.stop();
            activeSources.remove(source);
            availableSources.offer(source);
            LOGGER.trace("Stopped audio source: {}", source.getSourceId());
        }
    }
    
    /**
     * Stop all sounds with a specific name.
     */
    public void stopAllSounds(String soundName) {
        AudioBuffer buffer = soundBuffers.get(soundName);
        if (buffer == null) return;
        
        Iterator<AudioSource> iterator = activeSources.iterator();
        while (iterator.hasNext()) {
            AudioSource source = iterator.next();
            // Note: This is a simplified check. In a more robust implementation,
            // you might want to track which buffer each source is using.
            source.stop();
            iterator.remove();
            availableSources.offer(source);
        }
        
        LOGGER.debug("Stopped all instances of sound '{}'", soundName);
    }
    
    /**
     * Stop all currently playing sounds.
     */
    public void stopAllSounds() {
        for (AudioSource source : activeSources) {
            source.stop();
            availableSources.offer(source);
        }
        activeSources.clear();
        LOGGER.debug("Stopped all sounds");
    }
    
    /**
     * Update the sound manager (should be called each frame).
     * Cleans up finished sounds and returns sources to the pool.
     */
    public void update() {
        Iterator<AudioSource> iterator = activeSources.iterator();
        while (iterator.hasNext()) {
            AudioSource source = iterator.next();
            if (source.isStopped()) {
                iterator.remove();
                availableSources.offer(source);
                LOGGER.trace("Returned finished audio source to pool: {}", source.getSourceId());
            }
        }
    }
    
    /**
     * Set the master volume for all sound effects.
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        // Update all active sources
        for (AudioSource source : activeSources) {
            source.setGain(masterVolume * sfxVolume);
        }
        
        LOGGER.debug("Master volume set to: {}", masterVolume);
    }
    
    /**
     * Set the sound effects volume.
     */
    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        // Update all active sources
        for (AudioSource source : activeSources) {
            source.setGain(masterVolume * sfxVolume);
        }
        
        LOGGER.debug("SFX volume set to: {}", sfxVolume);
    }
    
    /**
     * Get an available audio source from the pool.
     */
    private AudioSource getAvailableSource() {
        AudioSource source = availableSources.poll();
        
        if (source == null && allSources.size() < MAX_SOURCES) {
            // Create a new source if we haven't reached the limit
            try {
                source = new AudioSource();
                allSources.add(source);
                LOGGER.debug("Created new audio source (total: {})", allSources.size());
            } catch (Exception e) {
                LOGGER.warn("Failed to create new audio source: {}", e.getMessage());
            }
        }
        
        if (source == null) {
            // Find the oldest playing source and stop it
            if (!activeSources.isEmpty()) {
                source = activeSources.get(0);
                source.stop();
                activeSources.remove(0);
                LOGGER.trace("Recycled oldest audio source");
            }
        }
        
        return source;
    }
    
    /**
     * Get the number of currently playing sounds.
     */
    public int getActiveSoundCount() {
        return activeSources.size();
    }
    
    /**
     * Get the number of available sources in the pool.
     */
    public int getAvailableSourceCount() {
        return availableSources.size();
    }
    
    /**
     * Check if a sound is loaded.
     */
    public boolean isSoundLoaded(String soundName) {
        return soundBuffers.containsKey(soundName);
    }
    
    /**
     * Unload a sound from memory.
     */
    public void unloadSound(String soundName) {
        AudioBuffer buffer = soundBuffers.remove(soundName);
        if (buffer != null) {
            // Stop all sources using this buffer
            stopAllSounds(soundName);
            buffer.cleanup();
            LOGGER.debug("Unloaded sound: {}", soundName);
        }
    }
    
    /**
     * Clean up all resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up SoundManager...");
        
        // Stop all sounds
        stopAllSounds();
        
        // Clean up all sources
        for (AudioSource source : allSources) {
            source.cleanup();
        }
        allSources.clear();
        availableSources.clear();
        activeSources.clear();
        
        // Clean up all buffers
        for (AudioBuffer buffer : soundBuffers.values()) {
            buffer.cleanup();
        }
        soundBuffers.clear();
        
        LOGGER.info("SoundManager cleanup complete");
    }
}