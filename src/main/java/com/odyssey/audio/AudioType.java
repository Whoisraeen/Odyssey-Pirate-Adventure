package com.odyssey.audio;

/**
 * Enumeration of different audio types in the game.
 * Used for categorizing sounds and applying appropriate volume settings.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public enum AudioType {
    /**
     * Sound effects - short audio clips for actions and events
     */
    SOUND_EFFECT,
    
    /**
     * Background music - longer audio tracks for ambiance
     */
    MUSIC,
    
    /**
     * Voice audio - character dialogue and narration
     */
    VOICE,
    
    /**
     * Ambient sounds - environmental audio loops
     */
    AMBIENT,
    
    /**
     * UI sounds - interface feedback sounds
     */
    UI
}