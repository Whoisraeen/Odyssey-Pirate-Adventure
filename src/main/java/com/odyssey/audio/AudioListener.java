package com.odyssey.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the 3D audio listener properties.
 * The listener represents the player's ears in the 3D audio space.
 */
public class AudioListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioListener.class);
    
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();
    private final Vector3f forward = new Vector3f(0, 0, -1);
    private final Vector3f up = new Vector3f(0, 1, 0);
    private float gain = 1.0f;
    
    /**
     * Create a new audio listener.
     */
    public AudioListener() {
        // Set default listener properties
        updateOpenALListener();
        LOGGER.debug("Created audio listener");
    }
    
    /**
     * Set the 3D position of the listener.
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        AL10.alListener3f(AL10.AL_POSITION, x, y, z);
        LOGGER.trace("Listener position set to: ({}, {}, {})", x, y, z);
    }
    
    /**
     * Set the 3D position of the listener.
     */
    public void setPosition(Vector3f position) {
        setPosition(position.x, position.y, position.z);
    }
    
    /**
     * Set the velocity of the listener for Doppler effect.
     */
    public void setVelocity(float x, float y, float z) {
        velocity.set(x, y, z);
        AL10.alListener3f(AL10.AL_VELOCITY, x, y, z);
        LOGGER.trace("Listener velocity set to: ({}, {}, {})", x, y, z);
    }
    
    /**
     * Set the velocity of the listener for Doppler effect.
     */
    public void setVelocity(Vector3f velocity) {
        setVelocity(velocity.x, velocity.y, velocity.z);
    }
    
    /**
     * Set the orientation of the listener.
     * 
     * @param forwardX Forward direction X component
     * @param forwardY Forward direction Y component
     * @param forwardZ Forward direction Z component
     * @param upX Up direction X component
     * @param upY Up direction Y component
     * @param upZ Up direction Z component
     */
    public void setOrientation(float forwardX, float forwardY, float forwardZ,
                              float upX, float upY, float upZ) {
        forward.set(forwardX, forwardY, forwardZ);
        up.set(upX, upY, upZ);
        
        float[] orientation = {
            forwardX, forwardY, forwardZ,
            upX, upY, upZ
        };
        
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
        LOGGER.trace("Listener orientation set - Forward: ({}, {}, {}), Up: ({}, {}, {})",
                    forwardX, forwardY, forwardZ, upX, upY, upZ);
    }
    
    /**
     * Set the orientation of the listener using vectors.
     */
    public void setOrientation(Vector3f forward, Vector3f up) {
        setOrientation(forward.x, forward.y, forward.z, up.x, up.y, up.z);
    }
    
    /**
     * Set the master gain (volume) for the listener.
     */
    public void setGain(float gain) {
        this.gain = Math.max(0.0f, gain);
        AL10.alListenerf(AL10.AL_GAIN, this.gain);
        LOGGER.debug("Listener gain set to: {}", this.gain);
    }
    
    /**
     * Update listener from camera data.
     * 
     * @param position Camera position
     * @param forward Camera forward vector
     * @param up Camera up vector
     */
    public void updateFromCamera(Vector3f position, Vector3f forward, Vector3f up) {
        setPosition(position);
        setOrientation(forward, up);
        setVelocity(0.0f, 0.0f, 0.0f); // Default to no velocity
    }
    
    /**
     * Update listener from camera data with velocity.
     * 
     * @param position Camera position
     * @param forward Camera forward vector
     * @param up Camera up vector
     * @param velocity Camera velocity
     */
    public void updateFromCamera(Vector3f position, Vector3f forward, Vector3f up, Vector3f velocity) {
        setPosition(position);
        setOrientation(forward, up);
        setVelocity(velocity);
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
     * Get the current forward direction.
     */
    public Vector3f getForward() {
        return new Vector3f(forward);
    }
    
    /**
     * Get the current up direction.
     */
    public Vector3f getUp() {
        return new Vector3f(up);
    }
    
    /**
     * Get the current gain.
     */
    public float getGain() {
        return gain;
    }
    
    /**
     * Update OpenAL listener with current properties.
     */
    private void updateOpenALListener() {
        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z);
        AL10.alListener3f(AL10.AL_VELOCITY, velocity.x, velocity.y, velocity.z);
        AL10.alListenerf(AL10.AL_GAIN, gain);
        
        float[] orientation = {
            forward.x, forward.y, forward.z,
            up.x, up.y, up.z
        };
        AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
    }
    
    /**
     * Clean up OpenAL resources.
     */
    public void cleanup() {
        // AudioListener doesn't create any OpenAL resources that need cleanup
        // The listener state is managed by OpenAL context
        LOGGER.debug("AudioListener cleanup complete");
    }
}