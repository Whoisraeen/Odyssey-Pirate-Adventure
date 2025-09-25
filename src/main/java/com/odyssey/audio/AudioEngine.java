package com.odyssey.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main audio engine for the game.
 * Handles OpenAL initialization, 3D audio management, and coordinates sound and music systems.
 */
public class AudioEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioEngine.class);
    
    private boolean initialized = false;
    private long device;
    private long context;
    
    private AudioListener listener;
    private SoundManager soundManager;
    private MusicManager musicManager;
    
    // Audio configuration
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;
    
    /**
     * Initialize the audio engine with OpenAL.
     */
    public void initialize() {
        LOGGER.info("Initializing AudioEngine...");
        
        try {
            // Initialize OpenAL
            initializeOpenAL();
            
            // Create audio systems
            listener = new AudioListener();
            soundManager = new SoundManager();
            musicManager = new MusicManager();
            
            // Apply initial volume settings
            soundManager.setMasterVolume(masterVolume);
            soundManager.setSfxVolume(sfxVolume);
            musicManager.setMasterVolume(masterVolume);
            musicManager.setMusicVolume(musicVolume);
            
            initialized = true;
            LOGGER.info("AudioEngine initialized successfully");
            
            // Log OpenAL information
            logOpenALInfo();
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize AudioEngine: {}", e.getMessage());
            cleanup();
        }
    }
    
    /**
     * Initialize OpenAL context and device.
     */
    private void initializeOpenAL() {
        // Open the default audio device
        device = alcOpenDevice((CharSequence) null);
        if (device == NULL) {
            throw new RuntimeException("Failed to open OpenAL device");
        }
        
        // Create OpenAL capabilities for the device
        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        
        // Create audio context
        context = alcCreateContext(device, (int[]) null);
        if (context == NULL) {
            throw new RuntimeException("Failed to create OpenAL context");
        }
        
        // Make the context current
        if (!alcMakeContextCurrent(context)) {
            throw new RuntimeException("Failed to make OpenAL context current");
        }
        
        // Create OpenAL capabilities for the context
        ALCapabilities alCaps = AL.createCapabilities(deviceCaps);
        if (!alCaps.OpenAL10) {
            throw new RuntimeException("OpenAL 1.0 not supported");
        }
        
        // Set distance model for 3D audio
        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);
        
        // Check for errors
        int error = alGetError();
        if (error != AL_NO_ERROR) {
            throw new RuntimeException("OpenAL initialization error: " + error);
        }
        
        LOGGER.debug("OpenAL initialized successfully");
    }
    
    /**
     * Log OpenAL information for debugging.
     */
    private void logOpenALInfo() {
        String vendor = alGetString(AL_VENDOR);
        String version = alGetString(AL_VERSION);
        String renderer = alGetString(AL_RENDERER);
        String extensions = alGetString(AL_EXTENSIONS);
        
        LOGGER.info("OpenAL Vendor: {}", vendor);
        LOGGER.info("OpenAL Version: {}", version);
        LOGGER.info("OpenAL Renderer: {}", renderer);
        LOGGER.debug("OpenAL Extensions: {}", extensions);
    }
    
    /**
     * Play a sound effect at the listener's position (2D sound).
     * 
     * @param soundName Name of the loaded sound
     * @return AudioSource playing the sound, or null if failed
     */
    public AudioSource playSoundEffect(String soundName) {
        if (!initialized || soundManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return null;
        }
        
        return soundManager.playSound(soundName);
    }
    
    /**
     * Play a sound effect at a specific 3D position.
     * 
     * @param soundName Name of the loaded sound
     * @param position 3D position of the sound
     * @return AudioSource playing the sound, or null if failed
     */
    public AudioSource playSoundEffect(String soundName, Vector3f position) {
        if (!initialized || soundManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return null;
        }
        
        return soundManager.playSound(soundName, position);
    }
    
    /**
     * Play a sound effect at a specific 3D position.
     * 
     * @param soundName Name of the loaded sound
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return AudioSource playing the sound, or null if failed
     */
    public AudioSource playSoundEffect(String soundName, float x, float y, float z) {
        if (!initialized || soundManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return null;
        }
        
        return soundManager.playSound(soundName, x, y, z);
    }
    
    /**
     * Play a looping sound effect at a specific 3D position.
     * 
     * @param soundName Name of the loaded sound
     * @param position 3D position of the sound
     * @return AudioSource playing the sound, or null if failed
     */
    public AudioSource playLoopingSoundEffect(String soundName, Vector3f position) {
        if (!initialized || soundManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return null;
        }
        
        return soundManager.playLoopingSound(soundName, position);
    }
    
    /**
     * Stop a specific sound effect.
     * 
     * @param source The AudioSource to stop
     */
    public void stopSoundEffect(AudioSource source) {
        if (soundManager != null) {
            soundManager.stopSound(source);
        }
    }
    
    /**
     * Load a sound effect from file.
     * 
     * @param name Name to associate with the sound
     * @param filename Path to the audio file
     */
    public void loadSoundEffect(String name, String filename) {
        if (soundManager != null) {
            soundManager.loadSound(name, filename);
        }
    }
    
    /**
     * Play background music.
     * 
     * @param trackName Name of the loaded music track
     */
    public void playMusic(String trackName) {
        if (!initialized || musicManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return;
        }
        
        musicManager.playMusic(trackName);
    }
    
    /**
     * Play background music with crossfade.
     * 
     * @param trackName Name of the loaded music track
     * @param crossfade Whether to crossfade from current track
     */
    public void playMusic(String trackName, boolean crossfade) {
        if (!initialized || musicManager == null) {
            LOGGER.warn("AudioEngine not initialized");
            return;
        }
        
        musicManager.playMusic(trackName, crossfade);
    }
    
    /**
     * Pause the current music.
     */
    public void pauseMusic() {
        if (musicManager != null) {
            musicManager.pauseMusic();
        }
    }
    
    /**
     * Resume the paused music.
     */
    public void resumeMusic() {
        if (musicManager != null) {
            musicManager.resumeMusic();
        }
    }
    
    /**
     * Stop the current music.
     */
    public void stopMusic() {
        if (musicManager != null) {
            musicManager.stopMusic();
        }
    }
    
    /**
     * Load a music track from file.
     * 
     * @param name Name to associate with the track
     * @param filename Path to the audio file
     */
    public void loadMusic(String name, String filename) {
        if (musicManager != null) {
            musicManager.loadMusic(name, filename);
        }
    }
    
    /**
     * Add a track to the music playlist.
     * 
     * @param trackName Name of the track to add
     */
    public void addToPlaylist(String trackName) {
        if (musicManager != null) {
            musicManager.addToPlaylist(trackName);
        }
    }
    
    /**
     * Play the next track in the playlist.
     */
    public void playNextTrack() {
        if (musicManager != null) {
            musicManager.playNext();
        }
    }
    
    /**
     * Play the previous track in the playlist.
     */
    public void playPreviousTrack() {
        if (musicManager != null) {
            musicManager.playPrevious();
        }
    }
    
    /**
     * Start playing the playlist.
     */
    public void playPlaylist() {
        if (musicManager != null) {
            musicManager.playPlaylist();
        }
    }
    
    /**
     * Update the audio listener position and orientation.
     * 
     * @param position Listener position
     * @param velocity Listener velocity
     * @param forward Forward direction vector
     * @param up Up direction vector
     */
    public void updateListener(Vector3f position, Vector3f velocity, Vector3f forward, Vector3f up) {
        if (listener != null) {
            listener.setPosition(position);
            listener.setVelocity(velocity);
            listener.setOrientation(forward, up);
        }
    }
    
    /**
     * Update the audio listener from camera data.
     * 
     * @param cameraPosition Camera position
     * @param cameraForward Camera forward vector
     * @param cameraUp Camera up vector
     */
    public void updateListenerFromCamera(Vector3f cameraPosition, Vector3f cameraForward, Vector3f cameraUp) {
        if (listener != null) {
            listener.updateFromCamera(cameraPosition, cameraForward, cameraUp);
        }
    }
    
    /**
     * Set the master volume for all audio.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        if (soundManager != null) {
            soundManager.setMasterVolume(masterVolume);
        }
        if (musicManager != null) {
            musicManager.setMasterVolume(masterVolume);
        }
        if (listener != null) {
            listener.setGain(masterVolume);
        }
        
        LOGGER.debug("Master volume set to: {}", masterVolume);
    }
    
    /**
     * Set the music volume.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        if (musicManager != null) {
            musicManager.setMusicVolume(musicVolume);
        }
        
        LOGGER.debug("Music volume set to: {}", musicVolume);
    }
    
    /**
     * Set the sound effects volume.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        if (soundManager != null) {
            soundManager.setSfxVolume(sfxVolume);
        }
        
        LOGGER.debug("SFX volume set to: {}", sfxVolume);
    }
    
    /**
     * Stop all audio playback.
     */
    public void stopAll() {
        if (!initialized) {
            return;
        }
        
        if (soundManager != null) {
            soundManager.stopAllSounds();
        }
        if (musicManager != null) {
            musicManager.stopMusic();
        }
        
        LOGGER.debug("Stopped all audio playback");
    }
    
    /**
     * Update the audio engine (called each frame).
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (!initialized) {
            return;
        }
        
        // Update sound manager (cleans up finished sounds)
        if (soundManager != null) {
            soundManager.update();
        }
        
        // Update music manager (handles crossfading and playlist progression)
        if (musicManager != null) {
            musicManager.update(deltaTime);
        }
    }
    
    /**
     * Get audio system statistics.
     */
    public String getAudioStats() {
        if (!initialized || soundManager == null) {
            return "Audio not initialized";
        }
        
        int activeSounds = soundManager.getActiveSoundCount();
        int availableSources = soundManager.getAvailableSourceCount();
        String currentMusic = musicManager != null ? musicManager.getCurrentTrack() : "None";
        boolean musicPlaying = musicManager != null && musicManager.isPlaying();
        
        return String.format("Active Sounds: %d, Available Sources: %d, Music: %s (%s)",
                activeSounds, availableSources, currentMusic, musicPlaying ? "Playing" : "Stopped");
    }
    
    // Getters
    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    public float getSfxVolume() { return sfxVolume; }
    public boolean isInitialized() { return initialized; }
    public SoundManager getSoundManager() { return soundManager; }
    public MusicManager getMusicManager() { return musicManager; }
    public AudioListener getListener() { return listener; }
    
    /**
     * Clean up audio resources.
     */
    public void cleanup() {
        if (!initialized) {
            return;
        }
        
        LOGGER.info("Cleaning up AudioEngine...");
        
        // Clean up managers
        if (soundManager != null) {
            soundManager.cleanup();
            soundManager = null;
        }
        if (musicManager != null) {
            musicManager.cleanup();
            musicManager = null;
        }
        if (listener != null) {
            listener.cleanup();
            listener = null;
        }
        
        // Clean up OpenAL
        if (context != NULL) {
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            context = NULL;
        }
        
        if (device != NULL) {
            alcCloseDevice(device);
            device = NULL;
        }
        
        initialized = false;
        LOGGER.info("AudioEngine cleanup complete");
    }
}