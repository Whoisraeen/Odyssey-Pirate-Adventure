package com.odyssey.audio;

/**
 * Configuration class for audio system settings.
 * Manages volume levels, audio quality, and system preferences.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public class AudioConfig {
    
    // Volume settings (0.0 to 1.0)
    private float masterVolume = 1.0f;
    private float musicVolume = 0.8f;
    private float effectsVolume = 0.9f;
    private float voiceVolume = 1.0f;
    private float ambientVolume = 0.7f;
    private float uiVolume = 0.8f;
    
    // Audio quality settings
    private int sampleRate = 44100;
    private int bufferSize = 4096;
    private boolean enableReverb = true;
    private boolean enable3DAudio = true;
    
    // Distance attenuation settings
    private float maxAudioDistance = 100.0f;
    private float referenceDistance = 1.0f;
    private float rolloffFactor = 1.0f;
    
    /**
     * Creates a new AudioConfig with default settings.
     */
    public AudioConfig() {
        // Default values are set in field declarations
    }
    
    /**
     * Gets the master volume level.
     * @return Master volume (0.0 to 1.0)
     */
    public float getMasterVolume() {
        return masterVolume;
    }
    
    /**
     * Sets the master volume level.
     * @param masterVolume Master volume (0.0 to 1.0)
     */
    public void setMasterVolume(float masterVolume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, masterVolume));
    }
    
    /**
     * Gets the music volume level.
     * @return Music volume (0.0 to 1.0)
     */
    public float getMusicVolume() {
        return musicVolume;
    }
    
    /**
     * Sets the music volume level.
     * @param musicVolume Music volume (0.0 to 1.0)
     */
    public void setMusicVolume(float musicVolume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, musicVolume));
    }
    
    /**
     * Gets the sound effects volume level.
     * @return Effects volume (0.0 to 1.0)
     */
    public float getEffectsVolume() {
        return effectsVolume;
    }
    
    /**
     * Sets the sound effects volume level.
     * @param effectsVolume Effects volume (0.0 to 1.0)
     */
    public void setEffectsVolume(float effectsVolume) {
        this.effectsVolume = Math.max(0.0f, Math.min(1.0f, effectsVolume));
    }
    
    /**
     * Gets the voice volume level.
     * @return Voice volume (0.0 to 1.0)
     */
    public float getVoiceVolume() {
        return voiceVolume;
    }
    
    /**
     * Sets the voice volume level.
     * @param voiceVolume Voice volume (0.0 to 1.0)
     */
    public void setVoiceVolume(float voiceVolume) {
        this.voiceVolume = Math.max(0.0f, Math.min(1.0f, voiceVolume));
    }
    
    /**
     * Gets the ambient volume level.
     * @return Ambient volume (0.0 to 1.0)
     */
    public float getAmbientVolume() {
        return ambientVolume;
    }
    
    /**
     * Sets the ambient volume level.
     * @param ambientVolume Ambient volume (0.0 to 1.0)
     */
    public void setAmbientVolume(float ambientVolume) {
        this.ambientVolume = Math.max(0.0f, Math.min(1.0f, ambientVolume));
    }
    
    /**
     * Gets the UI volume level.
     * @return UI volume (0.0 to 1.0)
     */
    public float getUiVolume() {
        return uiVolume;
    }
    
    /**
     * Sets the UI volume level.
     * @param uiVolume UI volume (0.0 to 1.0)
     */
    public void setUiVolume(float uiVolume) {
        this.uiVolume = Math.max(0.0f, Math.min(1.0f, uiVolume));
    }
    
    /**
     * Gets the volume for a specific audio type.
     * @param type The audio type
     * @return Volume level for the type (0.0 to 1.0)
     */
    public float getVolumeForType(AudioType type) {
        switch (type) {
            case MUSIC:
                return musicVolume * masterVolume;
            case SOUND_EFFECT:
                return effectsVolume * masterVolume;
            case VOICE:
                return voiceVolume * masterVolume;
            case AMBIENT:
                return ambientVolume * masterVolume;
            case UI:
                return uiVolume * masterVolume;
            default:
                return masterVolume;
        }
    }
    
    /**
     * Gets the audio sample rate.
     * @return Sample rate in Hz
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    /**
     * Sets the audio sample rate.
     * @param sampleRate Sample rate in Hz
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    
    /**
     * Gets the audio buffer size.
     * @return Buffer size in samples
     */
    public int getBufferSize() {
        return bufferSize;
    }
    
    /**
     * Sets the audio buffer size.
     * @param bufferSize Buffer size in samples
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    /**
     * Checks if reverb effects are enabled.
     * @return True if reverb is enabled
     */
    public boolean isReverbEnabled() {
        return enableReverb;
    }
    
    /**
     * Enables or disables reverb effects.
     * @param enableReverb True to enable reverb
     */
    public void setReverbEnabled(boolean enableReverb) {
        this.enableReverb = enableReverb;
    }
    
    /**
     * Checks if 3D audio is enabled.
     * @return True if 3D audio is enabled
     */
    public boolean is3DAudioEnabled() {
        return enable3DAudio;
    }
    
    /**
     * Enables or disables 3D audio.
     * @param enable3DAudio True to enable 3D audio
     */
    public void set3DAudioEnabled(boolean enable3DAudio) {
        this.enable3DAudio = enable3DAudio;
    }
    
    /**
     * Gets the maximum audio distance.
     * @return Maximum distance for audio attenuation
     */
    public float getMaxAudioDistance() {
        return maxAudioDistance;
    }
    
    /**
     * Sets the maximum audio distance.
     * @param maxAudioDistance Maximum distance for audio attenuation
     */
    public void setMaxAudioDistance(float maxAudioDistance) {
        this.maxAudioDistance = maxAudioDistance;
    }
    
    /**
     * Gets the reference distance for audio attenuation.
     * @return Reference distance
     */
    public float getReferenceDistance() {
        return referenceDistance;
    }
    
    /**
     * Sets the reference distance for audio attenuation.
     * @param referenceDistance Reference distance
     */
    public void setReferenceDistance(float referenceDistance) {
        this.referenceDistance = referenceDistance;
    }
    
    /**
     * Gets the rolloff factor for distance attenuation.
     * @return Rolloff factor
     */
    public float getRolloffFactor() {
        return rolloffFactor;
    }
    
    /**
     * Sets the rolloff factor for distance attenuation.
     * @param rolloffFactor Rolloff factor
     */
    public void setRolloffFactor(float rolloffFactor) {
        this.rolloffFactor = rolloffFactor;
    }
}