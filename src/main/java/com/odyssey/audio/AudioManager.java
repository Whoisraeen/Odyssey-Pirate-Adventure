package com.odyssey.audio;

import com.odyssey.core.GameConfig;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced 3D audio management system for The Odyssey.
 * Implements spatial audio, dynamic music, and environmental sound effects.
 */
public class AudioManager {
    private static final Logger logger = LoggerFactory.getLogger(AudioManager.class);
    
    private final GameConfig config;
    
    // Audio channels and mixing
    private final Map<AudioChannel, Float> channelVolumes = new EnumMap<>(AudioChannel.class);
    private final Map<String, AudioClip> loadedSounds = new ConcurrentHashMap<>();
    private final List<ActiveSound> activeSounds = Collections.synchronizedList(new ArrayList<>());
    
    // 3D Audio system
    private Vector3f listenerPosition = new Vector3f();
    private Vector3f listenerVelocity = new Vector3f();
    private Vector3f listenerForward = new Vector3f(0, 0, -1);
    private Vector3f listenerUp = new Vector3f(0, 1, 0);
    
    // Dynamic music system
    private final MusicManager musicManager;
    private final List<AudioZone> audioZones = new ArrayList<>();
    
    // Environmental audio
    private final Map<String, EnvironmentalAudio> environmentalSounds = new HashMap<>();
    private final List<AudioEmitter> audioEmitters = new ArrayList<>();
    
    // Audio effects and processing
    private final Map<String, AudioEffect> audioEffects = new HashMap<>();
    private boolean underwaterMode = false;
    private float weatherIntensity = 0.0f;
    
    public enum AudioChannel {
        MASTER, MUSIC, SFX, AMBIENT, VOICE, UI
    }
    
    public enum AudioCategory {
        MUSIC_AMBIENT, MUSIC_COMBAT, MUSIC_EXPLORATION, MUSIC_MENU,
        SFX_ENVIRONMENT, SFX_WEATHER, SFX_OCEAN, SFX_SHIP, SFX_COMBAT,
        SFX_UI, SFX_INTERACTION, VOICE_NARRATION, VOICE_CHARACTER
    }
    
    public static class AudioClip {
        public final String name;
        public final AudioCategory category;
        public final boolean is3D;
        public final boolean looping;
        public final float duration;
        public final float baseVolume;
        public final float basePitch;
        public final String filePath;
        
        // Audio properties
        public final float minDistance;
        public final float maxDistance;
        public final float rolloffFactor;
        
        public AudioClip(String name, AudioCategory category, String filePath,
                        boolean is3D, boolean looping, float duration,
                        float baseVolume, float basePitch) {
            this.name = name;
            this.category = category;
            this.filePath = filePath;
            this.is3D = is3D;
            this.looping = looping;
            this.duration = duration;
            this.baseVolume = baseVolume;
            this.basePitch = basePitch;
            
            // Default 3D audio properties
            this.minDistance = 10.0f;
            this.maxDistance = 1000.0f;
            this.rolloffFactor = 1.0f;
        }
    }
    
    public static class ActiveSound {
        public final UUID id;
        public final AudioClip clip;
        public Vector3f position;
        public Vector3f velocity;
        public float volume;
        public float pitch;
        public boolean isPlaying;
        public double startTime;
        public double pauseTime;
        public boolean isPaused;
        
        public ActiveSound(AudioClip clip, Vector3f position) {
            this.id = UUID.randomUUID();
            this.clip = clip;
            this.position = position != null ? new Vector3f(position) : null;
            this.velocity = new Vector3f();
            this.volume = clip.baseVolume;
            this.pitch = clip.basePitch;
            this.isPlaying = false;
            this.startTime = System.nanoTime() / 1_000_000_000.0;
            this.pauseTime = 0.0;
            this.isPaused = false;
        }
    }
    
    public static class MusicManager {
        private final List<AudioClip> ambientTracks = new ArrayList<>();
        private final List<AudioClip> combatTracks = new ArrayList<>();
        private final List<AudioClip> explorationTracks = new ArrayList<>();
        
        private ActiveSound currentTrack;
        private MusicState currentState = MusicState.AMBIENT;
        private float crossfadeTime = 3.0f;
        private boolean isTransitioning = false;
        
        public enum MusicState {
            AMBIENT, EXPLORATION, COMBAT, MENU, CUTSCENE
        }
        
        public void addTrack(AudioClip track) {
            switch (track.category) {
                case MUSIC_AMBIENT -> ambientTracks.add(track);
                case MUSIC_COMBAT -> combatTracks.add(track);
                case MUSIC_EXPLORATION -> explorationTracks.add(track);
            }
        }
        
        public AudioClip selectTrackForState(MusicState state) {
            List<AudioClip> tracks = switch (state) {
                case AMBIENT -> ambientTracks;
                case COMBAT -> combatTracks;
                case EXPLORATION -> explorationTracks;
                default -> ambientTracks;
            };
            
            if (!tracks.isEmpty()) {
                return tracks.get((int)(Math.random() * tracks.size()));
            }
            return null;
        }
    }
    
    public static class AudioZone {
        public final String name;
        public final Vector3f center;
        public final float radius;
        public final List<String> allowedSounds;
        public final Map<String, Float> volumeModifiers;
        public final String ambientSound;
        
        public AudioZone(String name, Vector3f center, float radius) {
            this.name = name;
            this.center = new Vector3f(center);
            this.radius = radius;
            this.allowedSounds = new ArrayList<>();
            this.volumeModifiers = new HashMap<>();
            this.ambientSound = null;
        }
    }
    
    public static class EnvironmentalAudio {
        public final String name;
        public final AudioClip clip;
        public final Vector3f position;
        public final boolean followsWeather;
        public final boolean followsTide;
        public final float baseIntensity;
        public float currentIntensity;
        
        public EnvironmentalAudio(String name, AudioClip clip, Vector3f position,
                                 boolean followsWeather, boolean followsTide) {
            this.name = name;
            this.clip = clip;
            this.position = new Vector3f(position);
            this.followsWeather = followsWeather;
            this.followsTide = followsTide;
            this.baseIntensity = 1.0f;
            this.currentIntensity = 1.0f;
        }
    }
    
    public static class AudioEmitter {
        public final UUID id;
        public Vector3f position;
        public final String soundName;
        public final float range;
        public final boolean autoPlay;
        public boolean isActive;
        public ActiveSound activeSound;
        
        public AudioEmitter(Vector3f position, String soundName, float range, boolean autoPlay) {
            this.id = UUID.randomUUID();
            this.position = new Vector3f(position);
            this.soundName = soundName;
            this.range = range;
            this.autoPlay = autoPlay;
            this.isActive = true;
        }
    }
    
    public static class AudioEffect {
        public final String name;
        public final EffectType type;
        public final Map<String, Float> parameters;
        public boolean isActive;
        
        public AudioEffect(String name, EffectType type) {
            this.name = name;
            this.type = type;
            this.parameters = new HashMap<>();
            this.isActive = false;
        }
        
        public enum EffectType {
            REVERB, ECHO, LOWPASS, HIGHPASS, DISTORTION, CHORUS, UNDERWATER
        }
    }
    
    public AudioManager(GameConfig config) {
        this.config = config;
        this.musicManager = new MusicManager();
        
        // Initialize channel volumes
        channelVolumes.put(AudioChannel.MASTER, config.getMasterVolume());
        channelVolumes.put(AudioChannel.MUSIC, 0.7f);
        channelVolumes.put(AudioChannel.SFX, 0.8f);
        channelVolumes.put(AudioChannel.AMBIENT, 0.6f);
        channelVolumes.put(AudioChannel.VOICE, 0.9f);
        channelVolumes.put(AudioChannel.UI, 0.5f);
    }
    
    public void initialize() {
        logger.info("Initializing advanced audio manager");
        logger.info("Master volume: {}", config.getMasterVolume());
        
        // Initialize audio system (would use OpenAL, FMOD, or similar)
        initializeAudioDevice();
        
        // Load core sound effects
        loadCoreSounds();
        
        // Initialize environmental audio
        initializeEnvironmentalAudio();
        
        // Setup audio effects
        initializeAudioEffects();
        
        // Create audio zones
        createDefaultAudioZones();
        
        logger.info("Audio manager initialized with {} sounds loaded", loadedSounds.size());
    }
    
    private void initializeAudioDevice() {
        // This would initialize the actual audio device
        // For now, just log the initialization
        logger.info("Audio device initialized (placeholder)");
    }
    
    private void loadCoreSounds() {
        // Ocean and water sounds
        loadedSounds.put("ocean_waves", new AudioClip(
            "ocean_waves", AudioCategory.SFX_OCEAN, "sounds/ocean/waves.ogg",
            true, true, -1, 0.6f, 1.0f
        ));
        
        loadedSounds.put("underwater_ambient", new AudioClip(
            "underwater_ambient", AudioCategory.SFX_OCEAN, "sounds/ocean/underwater.ogg",
            false, true, -1, 0.4f, 0.8f
        ));
        
        // Weather sounds
        loadedSounds.put("rain_light", new AudioClip(
            "rain_light", AudioCategory.SFX_WEATHER, "sounds/weather/rain_light.ogg",
            false, true, -1, 0.3f, 1.0f
        ));
        
        loadedSounds.put("thunder", new AudioClip(
            "thunder", AudioCategory.SFX_WEATHER, "sounds/weather/thunder.ogg",
            true, false, 4.5f, 0.8f, 1.0f
        ));
        
        loadedSounds.put("wind_strong", new AudioClip(
            "wind_strong", AudioCategory.SFX_WEATHER, "sounds/weather/wind_strong.ogg",
            false, true, -1, 0.5f, 1.0f
        ));
        
        // Ship sounds
        loadedSounds.put("ship_sailing", new AudioClip(
            "ship_sailing", AudioCategory.SFX_SHIP, "sounds/ship/sailing.ogg",
            true, true, -1, 0.4f, 1.0f
        ));
        
        loadedSounds.put("cannon_fire", new AudioClip(
            "cannon_fire", AudioCategory.SFX_COMBAT, "sounds/combat/cannon.ogg",
            true, false, 2.1f, 0.9f, 1.0f
        ));
        
        // Ambient music
        loadedSounds.put("ocean_exploration", new AudioClip(
            "ocean_exploration", AudioCategory.MUSIC_EXPLORATION, "music/ocean_exploration.ogg",
            false, true, 180.0f, 0.5f, 1.0f
        ));
        
        loadedSounds.put("peaceful_voyage", new AudioClip(
            "peaceful_voyage", AudioCategory.MUSIC_AMBIENT, "music/peaceful_voyage.ogg",
            false, true, 240.0f, 0.4f, 1.0f
        ));
        
        // Environmental sounds
        loadedSounds.put("seagulls", new AudioClip(
            "seagulls", AudioCategory.SFX_ENVIRONMENT, "sounds/environment/seagulls.ogg",
            true, true, -1, 0.3f, 1.0f
        ));
        
        loadedSounds.put("tropical_birds", new AudioClip(
            "tropical_birds", AudioCategory.SFX_ENVIRONMENT, "sounds/environment/tropical_birds.ogg",
            true, true, -1, 0.4f, 1.0f
        ));
        
        // Add tracks to music manager
        for (AudioClip clip : loadedSounds.values()) {
            if (clip.category.name().startsWith("MUSIC_")) {
                musicManager.addTrack(clip);
            }
        }
    }
    
    private void initializeEnvironmentalAudio() {
        // Ocean ambient sound
        AudioClip oceanWaves = loadedSounds.get("ocean_waves");
        environmentalSounds.put("ocean_ambient", new EnvironmentalAudio(
            "ocean_ambient", oceanWaves, new Vector3f(0, 0, 0), true, true
        ));
        
        // Seagull sounds near coasts
        AudioClip seagulls = loadedSounds.get("seagulls");
        environmentalSounds.put("coastal_birds", new EnvironmentalAudio(
            "coastal_birds", seagulls, new Vector3f(0, 20, 0), false, false
        ));
    }
    
    private void initializeAudioEffects() {
        // Underwater effect
        AudioEffect underwater = new AudioEffect("underwater", AudioEffect.EffectType.UNDERWATER);
        underwater.parameters.put("lowpass_cutoff", 800.0f);
        underwater.parameters.put("reverb_mix", 0.3f);
        underwater.parameters.put("volume_reduction", 0.6f);
        audioEffects.put("underwater", underwater);
        
        // Cave reverb
        AudioEffect caveReverb = new AudioEffect("cave_reverb", AudioEffect.EffectType.REVERB);
        caveReverb.parameters.put("reverb_time", 2.5f);
        caveReverb.parameters.put("wet_mix", 0.4f);
        caveReverb.parameters.put("dry_mix", 0.6f);
        audioEffects.put("cave_reverb", caveReverb);
        
        // Storm distortion
        AudioEffect stormEffect = new AudioEffect("storm_distortion", AudioEffect.EffectType.DISTORTION);
        stormEffect.parameters.put("distortion_amount", 0.2f);
        stormEffect.parameters.put("frequency_shift", 0.9f);
        audioEffects.put("storm_distortion", stormEffect);
    }
    
    private void createDefaultAudioZones() {
        // Open ocean zone
        AudioZone oceanZone = new AudioZone("open_ocean", new Vector3f(0, 0, 0), 10000.0f);
        oceanZone.allowedSounds.addAll(Arrays.asList("ocean_waves", "seagulls", "wind_strong"));
        oceanZone.volumeModifiers.put("ocean_waves", 1.0f);
        oceanZone.volumeModifiers.put("seagulls", 0.3f);
        audioZones.add(oceanZone);
        
        // Tropical island zone
        AudioZone tropicalZone = new AudioZone("tropical_island", new Vector3f(500, 10, 500), 200.0f);
        tropicalZone.allowedSounds.addAll(Arrays.asList("tropical_birds", "ocean_waves"));
        tropicalZone.volumeModifiers.put("tropical_birds", 1.0f);
        tropicalZone.volumeModifiers.put("ocean_waves", 0.6f);
        audioZones.add(tropicalZone);
    }
    
    public void update(double deltaTime) {
        // Update listener position and orientation
        updateListener(deltaTime);
        
        // Update active sounds
        updateActiveSounds(deltaTime);
        
        // Update music system
        updateMusicSystem(deltaTime);
        
        // Update environmental audio
        updateEnvironmentalAudio(deltaTime);
        
        // Update audio emitters
        updateAudioEmitters(deltaTime);
        
        // Update audio effects based on environment
        updateAudioEffects(deltaTime);
        
        // Clean up finished sounds
        cleanupFinishedSounds();
    }
    
    private void updateListener(double deltaTime) {
        // The listener position would be updated from the camera/player position
        // This is a placeholder - in a real implementation, this would come from the game engine
    }
    
    private void updateActiveSounds(double deltaTime) {
        synchronized (activeSounds) {
            for (ActiveSound sound : activeSounds) {
                if (!sound.isPlaying || sound.isPaused) continue;
                
                // Update 3D audio positioning
                if (sound.clip.is3D && sound.position != null) {
                    update3DAudio(sound);
                }
                
                // Check if non-looping sound has finished
                if (!sound.clip.looping && sound.clip.duration > 0) {
                    double elapsed = (System.nanoTime() / 1_000_000_000.0) - sound.startTime;
                    if (elapsed >= sound.clip.duration) {
                        sound.isPlaying = false;
                    }
                }
            }
        }
    }
    
    private void update3DAudio(ActiveSound sound) {
        float distance = listenerPosition.distance(sound.position);
        
        // Distance attenuation
        float attenuation = 1.0f;
        if (distance > sound.clip.minDistance) {
            if (distance < sound.clip.maxDistance) {
                attenuation = sound.clip.minDistance / 
                    (sound.clip.minDistance + sound.clip.rolloffFactor * (distance - sound.clip.minDistance));
            } else {
                attenuation = 0.0f; // Beyond max distance
            }
        }
        
        // Apply distance attenuation
        sound.volume = sound.clip.baseVolume * attenuation;
        
        // Doppler effect (simplified)
        Vector3f relativeVelocity = new Vector3f(sound.velocity).sub(listenerVelocity);
        Vector3f soundDirection = new Vector3f(sound.position).sub(listenerPosition).normalize();
        float dopplerFactor = 1.0f + relativeVelocity.dot(soundDirection) * 0.001f; // Speed of sound approximation
        sound.pitch = sound.clip.basePitch * dopplerFactor;
        
        // Pan calculation for stereo positioning
        // This would be implemented with the actual audio library
    }
    
    private void updateMusicSystem(double deltaTime) {
        // Dynamic music state changes based on game state
        // This would integrate with the game state manager
        
        if (musicManager.currentTrack == null || !musicManager.currentTrack.isPlaying) {
            // Start new track based on current state
            AudioClip newTrack = musicManager.selectTrackForState(musicManager.currentState);
            if (newTrack != null) {
                musicManager.currentTrack = playSound(newTrack.name, null);
            }
        }
    }
    
    private void updateEnvironmentalAudio(double deltaTime) {
        for (EnvironmentalAudio envAudio : environmentalSounds.values()) {
            float targetIntensity = envAudio.baseIntensity;
            
            // Adjust based on weather
            if (envAudio.followsWeather) {
                targetIntensity *= (0.5f + weatherIntensity * 0.5f);
            }
            
            // Smooth intensity changes
            envAudio.currentIntensity += (targetIntensity - envAudio.currentIntensity) * (float)deltaTime * 2.0f;
            
            // Find or create active sound for this environmental audio
            ActiveSound activeEnvSound = findActiveSoundByName(envAudio.clip.name);
            if (activeEnvSound == null && envAudio.currentIntensity > 0.1f) {
                activeEnvSound = playSound(envAudio.clip.name, envAudio.position);
            }
            
            if (activeEnvSound != null) {
                activeEnvSound.volume = envAudio.clip.baseVolume * envAudio.currentIntensity;
                if (activeEnvSound.volume < 0.05f) {
                    activeEnvSound.isPlaying = false;
                }
            }
        }
    }
    
    private void updateAudioEmitters(double deltaTime) {
        for (AudioEmitter emitter : audioEmitters) {
            if (!emitter.isActive) continue;
            
            float distanceToListener = listenerPosition.distance(emitter.position);
            
            if (distanceToListener <= emitter.range) {
                // Start sound if not already playing
                if (emitter.activeSound == null || !emitter.activeSound.isPlaying) {
                    if (emitter.autoPlay) {
                        emitter.activeSound = playSound(emitter.soundName, emitter.position);
                    }
                }
            } else {
                // Stop sound if too far away
                if (emitter.activeSound != null && emitter.activeSound.isPlaying) {
                    emitter.activeSound.isPlaying = false;
                }
            }
        }
    }
    
    private void updateAudioEffects(double deltaTime) {
        // Update underwater effect
        AudioEffect underwaterEffect = audioEffects.get("underwater");
        if (underwaterEffect != null) {
            underwaterEffect.isActive = underwaterMode;
        }
        
        // Update storm effect based on weather intensity
        AudioEffect stormEffect = audioEffects.get("storm_distortion");
        if (stormEffect != null) {
            stormEffect.isActive = weatherIntensity > 0.6f;
            stormEffect.parameters.put("distortion_amount", weatherIntensity * 0.3f);
        }
    }
    
    private void cleanupFinishedSounds() {
        synchronized (activeSounds) {
            activeSounds.removeIf(sound -> !sound.isPlaying);
        }
    }
    
    private ActiveSound findActiveSoundByName(String soundName) {
        synchronized (activeSounds) {
            return activeSounds.stream()
                .filter(sound -> sound.clip.name.equals(soundName) && sound.isPlaying)
                .findFirst()
                .orElse(null);
        }
    }
    
    public ActiveSound playSound(String soundName, Vector3f position) {
        AudioClip clip = loadedSounds.get(soundName);
        if (clip == null) {
            logger.warn("Attempted to play unknown sound: {}", soundName);
            return null;
        }
        
        ActiveSound activeSound = new ActiveSound(clip, position);
        activeSound.isPlaying = true;
        
        // Apply channel volume
        AudioChannel channel = getChannelForCategory(clip.category);
        float channelVolume = channelVolumes.get(channel);
        activeSound.volume *= channelVolume * channelVolumes.get(AudioChannel.MASTER);
        
        synchronized (activeSounds) {
            activeSounds.add(activeSound);
        }
        
        logger.debug("Playing sound: {} at position: {}", soundName, position);
        return activeSound;
    }
    
    public void stopSound(UUID soundId) {
        synchronized (activeSounds) {
            activeSounds.stream()
                .filter(sound -> sound.id.equals(soundId))
                .findFirst()
                .ifPresent(sound -> sound.isPlaying = false);
        }
    }
    
    public void pauseSound(UUID soundId) {
        synchronized (activeSounds) {
            activeSounds.stream()
                .filter(sound -> sound.id.equals(soundId))
                .findFirst()
                .ifPresent(sound -> {
                    sound.isPaused = true;
                    sound.pauseTime = System.nanoTime() / 1_000_000_000.0;
                });
        }
    }
    
    public void resumeSound(UUID soundId) {
        synchronized (activeSounds) {
            activeSounds.stream()
                .filter(sound -> sound.id.equals(soundId))
                .findFirst()
                .ifPresent(sound -> {
                    if (sound.isPaused) {
                        sound.isPaused = false;
                        double pauseDuration = System.nanoTime() / 1_000_000_000.0 - sound.pauseTime;
                        sound.startTime += pauseDuration; // Adjust start time
                    }
                });
        }
    }
    
    private AudioChannel getChannelForCategory(AudioCategory category) {
        return switch (category) {
            case MUSIC_AMBIENT, MUSIC_COMBAT, MUSIC_EXPLORATION, MUSIC_MENU -> AudioChannel.MUSIC;
            case SFX_ENVIRONMENT, SFX_WEATHER, SFX_OCEAN, SFX_SHIP, SFX_COMBAT, SFX_INTERACTION -> AudioChannel.SFX;
            case VOICE_NARRATION, VOICE_CHARACTER -> AudioChannel.VOICE;
            case SFX_UI -> AudioChannel.UI;
        };
    }
    
    public void setListenerPosition(Vector3f position) {
        this.listenerPosition.set(position);
    }
    
    public void setListenerVelocity(Vector3f velocity) {
        this.listenerVelocity.set(velocity);
    }
    
    public void setListenerOrientation(Vector3f forward, Vector3f up) {
        this.listenerForward.set(forward);
        this.listenerUp.set(up);
    }
    
    public void setChannelVolume(AudioChannel channel, float volume) {
        channelVolumes.put(channel, Math.max(0.0f, Math.min(1.0f, volume)));
        logger.debug("Set {} channel volume to {}", channel, volume);
    }
    
    public float getChannelVolume(AudioChannel channel) {
        return channelVolumes.getOrDefault(channel, 1.0f);
    }
    
    public void setUnderwaterMode(boolean underwater) {
        if (this.underwaterMode != underwater) {
            this.underwaterMode = underwater;
            logger.debug("Underwater mode: {}", underwater);
        }
    }
    
    public void setWeatherIntensity(float intensity) {
        this.weatherIntensity = Math.max(0.0f, Math.min(1.0f, intensity));
    }
    
    public void changeMusicState(MusicManager.MusicState newState) {
        if (musicManager.currentState != newState) {
            logger.info("Music state changed from {} to {}", musicManager.currentState, newState);
            musicManager.currentState = newState;
            
            // Trigger crossfade to new music
            AudioClip newTrack = musicManager.selectTrackForState(newState);
            if (newTrack != null) {
                crossfadeToTrack(newTrack);
            }
        }
    }
    
    private void crossfadeToTrack(AudioClip newTrack) {
        // Implementation would gradually fade out current track and fade in new track
        if (musicManager.currentTrack != null) {
            musicManager.currentTrack.isPlaying = false;
        }
        
        musicManager.currentTrack = playSound(newTrack.name, null);
        musicManager.isTransitioning = true;
    }
    
    public AudioEmitter createAudioEmitter(Vector3f position, String soundName, float range, boolean autoPlay) {
        AudioEmitter emitter = new AudioEmitter(position, soundName, range, autoPlay);
        audioEmitters.add(emitter);
        logger.debug("Created audio emitter for sound '{}' at position {}", soundName, position);
        return emitter;
    }
    
    public void removeAudioEmitter(UUID emitterId) {
        audioEmitters.removeIf(emitter -> emitter.id.equals(emitterId));
    }
    
    public void cleanup() {
        logger.info("Cleaning up audio manager");
        
        // Stop all active sounds
        synchronized (activeSounds) {
            for (ActiveSound sound : activeSounds) {
                sound.isPlaying = false;
            }
            activeSounds.clear();
        }
        
        // Clean up resources
        loadedSounds.clear();
        environmentalSounds.clear();
        audioEmitters.clear();
        audioZones.clear();
        audioEffects.clear();
        
        logger.info("Audio cleanup complete");
    }
    
    // Getters and status
    public int getActiveSoundCount() {
        synchronized (activeSounds) {
            return (int)activeSounds.stream().mapToLong(s -> s.isPlaying ? 1 : 0).sum();
        }
    }
    
    public boolean isUnderwaterMode() { return underwaterMode; }
    public float getWeatherIntensity() { return weatherIntensity; }
    public MusicManager.MusicState getCurrentMusicState() { return musicManager.currentState; }
    public int getLoadedSoundCount() { return loadedSounds.size(); }
    public int getAudioEmitterCount() { return audioEmitters.size(); }
    
    public List<String> getLoadedSoundNames() {
        return new ArrayList<>(loadedSounds.keySet());
    }
    
    public List<ActiveSound> getActiveSounds() {
        synchronized (activeSounds) {
            return new ArrayList<>(activeSounds);
        }
    }
}