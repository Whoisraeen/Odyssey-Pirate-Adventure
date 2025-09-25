package com.odyssey.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages background music playback with crossfading and playlist support.
 * Handles music streaming, volume control, and smooth transitions between tracks.
 */
public class MusicManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicManager.class);
    
    private static final float CROSSFADE_DURATION = 2.0f; // Crossfade duration in seconds
    
    private final Map<String, AudioBuffer> musicBuffers = new ConcurrentHashMap<>();
    private final List<String> playlist = new ArrayList<>();
    private final Random random = new Random();
    
    private AudioSource primarySource;
    private AudioSource secondarySource;
    
    private String currentTrack = null;
    private int currentPlaylistIndex = 0;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isShuffleEnabled = false;
    private boolean isRepeatEnabled = false;
    private boolean isCrossfading = false;
    
    private float masterVolume = 1.0f;
    private float musicVolume = 1.0f;
    private float crossfadeTimer = 0.0f;
    
    /**
     * Create a new music manager.
     */
    public MusicManager() {
        try {
            primarySource = new AudioSource();
            secondarySource = new AudioSource();
            
            // Configure sources for music playback
            primarySource.setLooping(false);
            secondarySource.setLooping(false);
            
            // Position sources at listener location (2D audio)
            primarySource.setPosition(0, 0, 0);
            secondarySource.setPosition(0, 0, 0);
            primarySource.setReferenceDistance(Float.MAX_VALUE);
            secondarySource.setReferenceDistance(Float.MAX_VALUE);
            
            LOGGER.info("MusicManager initialized");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MusicManager: {}", e.getMessage());
        }
    }
    
    /**
     * Load a music track from file.
     * 
     * @param name The name to associate with the track
     * @param filename The path to the audio file
     */
    public void loadMusic(String name, String filename) {
        if (musicBuffers.containsKey(name)) {
            LOGGER.warn("Music track '{}' is already loaded", name);
            return;
        }
        
        try {
            AudioBuffer buffer = new AudioBuffer(filename);
            musicBuffers.put(name, buffer);
            LOGGER.debug("Loaded music track '{}' from '{}'", name, filename);
        } catch (Exception e) {
            LOGGER.error("Failed to load music track '{}' from '{}': {}", name, filename, e.getMessage());
        }
    }
    
    /**
     * Play a specific music track.
     * 
     * @param trackName The name of the track to play
     */
    public void playMusic(String trackName) {
        playMusic(trackName, false);
    }
    
    /**
     * Play a specific music track with optional crossfade.
     * 
     * @param trackName The name of the track to play
     * @param crossfade Whether to crossfade from the current track
     */
    public void playMusic(String trackName, boolean crossfade) {
        AudioBuffer buffer = musicBuffers.get(trackName);
        if (buffer == null) {
            LOGGER.warn("Music track '{}' not found", trackName);
            return;
        }
        
        if (trackName.equals(currentTrack) && isPlaying && !isPaused) {
            LOGGER.debug("Track '{}' is already playing", trackName);
            return;
        }
        
        if (crossfade && isPlaying && primarySource != null) {
            startCrossfade(buffer, trackName);
        } else {
            playTrackDirectly(buffer, trackName);
        }
    }
    
    /**
     * Start crossfading to a new track.
     */
    private void startCrossfade(AudioBuffer newBuffer, String newTrackName) {
        if (secondarySource == null) {
            playTrackDirectly(newBuffer, newTrackName);
            return;
        }
        
        // Set up the secondary source with the new track
        secondarySource.setBuffer(newBuffer.getBufferId());
        secondarySource.setGain(0.0f); // Start at zero volume
        secondarySource.play();
        
        isCrossfading = true;
        crossfadeTimer = 0.0f;
        
        LOGGER.debug("Starting crossfade from '{}' to '{}'", currentTrack, newTrackName);
        
        // The actual track switch will happen when crossfade completes
        currentTrack = newTrackName;
    }
    
    /**
     * Play a track directly without crossfading.
     */
    private void playTrackDirectly(AudioBuffer buffer, String trackName) {
        if (primarySource == null) return;
        
        // Stop current playback
        if (isPlaying) {
            primarySource.stop();
        }
        
        // Set up and play the new track
        primarySource.setBuffer(buffer.getBufferId());
        primarySource.setGain(masterVolume * musicVolume);
        primarySource.play();
        
        currentTrack = trackName;
        isPlaying = true;
        isPaused = false;
        isCrossfading = false;
        
        LOGGER.debug("Playing music track: {}", trackName);
    }
    
    /**
     * Pause the current music.
     */
    public void pauseMusic() {
        if (isPlaying && !isPaused && primarySource != null) {
            primarySource.pause();
            if (isCrossfading && secondarySource != null) {
                secondarySource.pause();
            }
            isPaused = true;
            LOGGER.debug("Music paused");
        }
    }
    
    /**
     * Resume the paused music.
     */
    public void resumeMusic() {
        if (isPlaying && isPaused && primarySource != null) {
            primarySource.resume();
            if (isCrossfading && secondarySource != null) {
                secondarySource.resume();
            }
            isPaused = false;
            LOGGER.debug("Music resumed");
        }
    }
    
    /**
     * Stop the current music.
     */
    public void stopMusic() {
        if (primarySource != null) {
            primarySource.stop();
        }
        if (secondarySource != null) {
            secondarySource.stop();
        }
        
        isPlaying = false;
        isPaused = false;
        isCrossfading = false;
        currentTrack = null;
        
        LOGGER.debug("Music stopped");
    }
    
    /**
     * Add a track to the playlist.
     */
    public void addToPlaylist(String trackName) {
        if (!playlist.contains(trackName)) {
            playlist.add(trackName);
            LOGGER.debug("Added '{}' to playlist", trackName);
        }
    }
    
    /**
     * Remove a track from the playlist.
     */
    public void removeFromPlaylist(String trackName) {
        if (playlist.remove(trackName)) {
            LOGGER.debug("Removed '{}' from playlist", trackName);
            
            // Adjust current index if necessary
            if (currentPlaylistIndex >= playlist.size()) {
                currentPlaylistIndex = Math.max(0, playlist.size() - 1);
            }
        }
    }
    
    /**
     * Clear the playlist.
     */
    public void clearPlaylist() {
        playlist.clear();
        currentPlaylistIndex = 0;
        LOGGER.debug("Playlist cleared");
    }
    
    /**
     * Play the next track in the playlist.
     */
    public void playNext() {
        if (playlist.isEmpty()) {
            LOGGER.warn("Playlist is empty");
            return;
        }
        
        if (isShuffleEnabled) {
            currentPlaylistIndex = random.nextInt(playlist.size());
        } else {
            currentPlaylistIndex = (currentPlaylistIndex + 1) % playlist.size();
        }
        
        String nextTrack = playlist.get(currentPlaylistIndex);
        playMusic(nextTrack, true);
        
        LOGGER.debug("Playing next track: {} (index: {})", nextTrack, currentPlaylistIndex);
    }
    
    /**
     * Play the previous track in the playlist.
     */
    public void playPrevious() {
        if (playlist.isEmpty()) {
            LOGGER.warn("Playlist is empty");
            return;
        }
        
        if (isShuffleEnabled) {
            currentPlaylistIndex = random.nextInt(playlist.size());
        } else {
            currentPlaylistIndex = (currentPlaylistIndex - 1 + playlist.size()) % playlist.size();
        }
        
        String previousTrack = playlist.get(currentPlaylistIndex);
        playMusic(previousTrack, true);
        
        LOGGER.debug("Playing previous track: {} (index: {})", previousTrack, currentPlaylistIndex);
    }
    
    /**
     * Start playing the playlist from the beginning.
     */
    public void playPlaylist() {
        if (playlist.isEmpty()) {
            LOGGER.warn("Cannot play empty playlist");
            return;
        }
        
        currentPlaylistIndex = 0;
        String firstTrack = playlist.get(currentPlaylistIndex);
        playMusic(firstTrack);
        
        LOGGER.debug("Started playing playlist with track: {}", firstTrack);
    }
    
    /**
     * Update the music manager (should be called each frame).
     */
    public void update(float deltaTime) {
        if (!isPlaying || isPaused) return;
        
        // Handle crossfading
        if (isCrossfading) {
            updateCrossfade(deltaTime);
        }
        
        // Check if current track finished and handle playlist progression
        if (primarySource != null && primarySource.isStopped() && !isCrossfading) {
            handleTrackFinished();
        }
    }
    
    /**
     * Update crossfade between tracks.
     */
    private void updateCrossfade(float deltaTime) {
        crossfadeTimer += deltaTime;
        float progress = Math.min(crossfadeTimer / CROSSFADE_DURATION, 1.0f);
        
        if (primarySource != null && secondarySource != null) {
            // Fade out primary source, fade in secondary source
            float primaryVolume = (1.0f - progress) * masterVolume * musicVolume;
            float secondaryVolume = progress * masterVolume * musicVolume;
            
            primarySource.setGain(primaryVolume);
            secondarySource.setGain(secondaryVolume);
        }
        
        // Complete crossfade
        if (progress >= 1.0f) {
            completeCrossfade();
        }
    }
    
    /**
     * Complete the crossfade and swap sources.
     */
    private void completeCrossfade() {
        if (primarySource != null) {
            primarySource.stop();
        }
        
        // Swap sources
        AudioSource temp = primarySource;
        primarySource = secondarySource;
        secondarySource = temp;
        
        isCrossfading = false;
        crossfadeTimer = 0.0f;
        
        LOGGER.debug("Crossfade completed to track: {}", currentTrack);
    }
    
    /**
     * Handle when a track finishes playing.
     */
    private void handleTrackFinished() {
        if (!playlist.isEmpty()) {
            if (isRepeatEnabled && playlist.size() == 1) {
                // Repeat single track
                String track = playlist.get(currentPlaylistIndex);
                playMusic(track);
            } else {
                // Play next track in playlist
                playNext();
            }
        } else {
            // No playlist, just stop
            isPlaying = false;
            currentTrack = null;
            LOGGER.debug("Track finished, no playlist to continue");
        }
    }
    
    /**
     * Set the master volume.
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        updateSourceVolumes();
        LOGGER.debug("Master volume set to: {}", masterVolume);
    }
    
    /**
     * Set the music volume.
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        updateSourceVolumes();
        LOGGER.debug("Music volume set to: {}", musicVolume);
    }
    
    /**
     * Update the volume of all audio sources.
     */
    private void updateSourceVolumes() {
        float finalVolume = masterVolume * musicVolume;
        
        if (primarySource != null && !isCrossfading) {
            primarySource.setGain(finalVolume);
        }
        
        // During crossfade, volumes are handled by updateCrossfade()
    }
    
    /**
     * Enable or disable shuffle mode.
     */
    public void setShuffleEnabled(boolean enabled) {
        this.isShuffleEnabled = enabled;
        LOGGER.debug("Shuffle mode: {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Enable or disable repeat mode.
     */
    public void setRepeatEnabled(boolean enabled) {
        this.isRepeatEnabled = enabled;
        LOGGER.debug("Repeat mode: {}", enabled ? "enabled" : "disabled");
    }
    
    // Getters
    public boolean isPlaying() { return isPlaying && !isPaused; }
    public boolean isPaused() { return isPaused; }
    public boolean isShuffleEnabled() { return isShuffleEnabled; }
    public boolean isRepeatEnabled() { return isRepeatEnabled; }
    public String getCurrentTrack() { return currentTrack; }
    public List<String> getPlaylist() { return new ArrayList<>(playlist); }
    public int getCurrentPlaylistIndex() { return currentPlaylistIndex; }
    public float getMasterVolume() { return masterVolume; }
    public float getMusicVolume() { return musicVolume; }
    
    /**
     * Check if a music track is loaded.
     */
    public boolean isMusicLoaded(String trackName) {
        return musicBuffers.containsKey(trackName);
    }
    
    /**
     * Unload a music track from memory.
     */
    public void unloadMusic(String trackName) {
        AudioBuffer buffer = musicBuffers.remove(trackName);
        if (buffer != null) {
            // Stop playback if this track is currently playing
            if (trackName.equals(currentTrack)) {
                stopMusic();
            }
            
            // Remove from playlist
            removeFromPlaylist(trackName);
            
            buffer.cleanup();
            LOGGER.debug("Unloaded music track: {}", trackName);
        }
    }
    
    /**
     * Clean up all resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up MusicManager...");
        
        // Stop playback
        stopMusic();
        
        // Clean up sources
        if (primarySource != null) {
            primarySource.cleanup();
            primarySource = null;
        }
        if (secondarySource != null) {
            secondarySource.cleanup();
            secondarySource = null;
        }
        
        // Clean up all buffers
        for (AudioBuffer buffer : musicBuffers.values()) {
            buffer.cleanup();
        }
        musicBuffers.clear();
        
        // Clear playlist
        playlist.clear();
        
        LOGGER.info("MusicManager cleanup complete");
    }
}