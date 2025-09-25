package com.odyssey.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an individual audio source in 3D space.
 * Manages OpenAL source properties including position, velocity, and playback state.
 */
public class AudioSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioSource.class);
    
    private final int sourceId;
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private boolean isLooping = false;
    private boolean isPlaying = false;
    private float gain = 1.0f;
    private float pitch = 1.0f;
    private float maxDistance = 100.0f;
    private float referenceDistance = 1.0f;
    private float rolloffFactor = 1.0f;
    
    /**
     * Create a new audio source.
     */
    public AudioSource() {
        sourceId = AL10.alGenSources();
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            throw new RuntimeException("Failed to create OpenAL source");
        }
        
        // Set default properties
        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain);
        AL10.alSourcef(sourceId, AL10.AL_PITCH, pitch);
        AL10.alSource3f(sourceId, AL10.AL_POSITION, 0, 0, 0);
        AL10.alSource3f(sourceId, AL10.AL_VELOCITY, 0, 0, 0);
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
        
        // Set 3D audio properties
        AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, maxDistance);
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, referenceDistance);
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, rolloffFactor);
        
        LOGGER.debug("Created audio source with ID: {}", sourceId);
    }
    
    /**
     * Play the audio source.
     */
    public void play() {
        AL10.alSourcePlay(sourceId);
        isPlaying = true;
        LOGGER.debug("Playing audio source: {}", sourceId);
    }
    
    /**
     * Pause playback.
     */
    public void pause() {
        AL10.alSourcePause(sourceId);
        isPlaying = false;
        LOGGER.debug("Paused audio source: {}", sourceId);
    }
    
    /**
     * Resume playback if paused.
     */
    public void resume() {
        AL10.alSourcePlay(sourceId);
        isPlaying = true;
        LOGGER.debug("Resumed audio source: {}", sourceId);
    }
    
    /**
     * Stop the audio source.
     */
    public void stop() {
        AL10.alSourceStop(sourceId);
        isPlaying = false;
        LOGGER.debug("Stopped audio source: {}", sourceId);
    }
    
    /**
     * Set the audio buffer for this source.
     */
    public void setBuffer(int bufferId) {
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
    }
    
    /**
     * Set the 3D position of the audio source.
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        AL10.alSource3f(sourceId, AL10.AL_POSITION, x, y, z);
    }
    
    /**
     * Set the 3D position of the audio source.
     */
    public void setPosition(Vector3f position) {
        setPosition(position.x, position.y, position.z);
    }
    
    /**
     * Set the velocity of the audio source for Doppler effect.
     */
    public void setVelocity(float x, float y, float z) {
        velocity.set(x, y, z);
        AL10.alSource3f(sourceId, AL10.AL_VELOCITY, x, y, z);
    }
    
    /**
     * Set the velocity of the audio source for Doppler effect.
     */
    public void setVelocity(Vector3f velocity) {
        setVelocity(velocity.x, velocity.y, velocity.z);
    }
    
    /**
     * Set the gain (volume) of the audio source.
     */
    public void setGain(float gain) {
        this.gain = Math.max(0.0f, gain);
        AL10.alSourcef(sourceId, AL10.AL_GAIN, this.gain);
    }
    
    /**
     * Set the pitch of the audio source.
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(0.1f, pitch);
        AL10.alSourcef(sourceId, AL10.AL_PITCH, this.pitch);
    }
    
    /**
     * Set whether the audio source should loop.
     */
    public void setLooping(boolean looping) {
        this.isLooping = looping;
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }
    
    /**
     * Set the maximum distance for 3D audio attenuation.
     */
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, maxDistance);
    }
    
    /**
     * Set the reference distance for 3D audio attenuation.
     */
    public void setReferenceDistance(float referenceDistance) {
        this.referenceDistance = referenceDistance;
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, referenceDistance);
    }
    
    /**
     * Set the rolloff factor for 3D audio attenuation.
     */
    public void setRolloffFactor(float rolloffFactor) {
        this.rolloffFactor = rolloffFactor;
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, rolloffFactor);
    }
    
    /**
     * Check if the audio source is currently playing.
     */
    public boolean isPlaying() {
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        return state == AL10.AL_PLAYING;
    }
    
    /**
     * Check if the audio source has finished playing.
     */
    public boolean isStopped() {
        int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
        return state == AL10.AL_STOPPED;
    }
    
    /**
     * Get the OpenAL source ID.
     */
    public int getSourceId() {
        return sourceId;
    }
    
    /**
     * Get the current position.
     */
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    /**
     * Get the current velocity.
     */
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }
    
    /**
     * Get the current gain.
     */
    public float getGain() {
        return gain;
    }
    
    /**
     * Get the current pitch.
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Check if the source is set to loop.
     */
    public boolean isLooping() {
        return isLooping;
    }
    
    /**
     * Clean up the audio source.
     */
    public void cleanup() {
        stop();
        AL10.alDeleteSources(sourceId);
        LOGGER.debug("Cleaned up audio source: {}", sourceId);
    }
}