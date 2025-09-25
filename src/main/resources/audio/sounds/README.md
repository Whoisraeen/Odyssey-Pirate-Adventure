# Audio Resources

This directory contains all audio resources for The Odyssey pirate adventure game.

## Directory Structure

```
audio/
├── sounds/          # Sound effects
│   ├── cannon/      # Cannon firing, reloading sounds
│   ├── ocean/       # Wave sounds, water splashing
│   ├── ship/        # Creaking wood, sail flapping, anchor
│   ├── combat/      # Sword clashing, boarding sounds
│   ├── ambient/     # Environmental sounds
│   └── ui/          # Interface sounds
├── music/           # Background music tracks
│   ├── exploration/ # Calm sailing music
│   ├── combat/      # Battle music
│   ├── port/        # Town and harbor music
│   └── ambient/     # Environmental music
└── voice/           # Character dialogue and narration
    ├── crew/        # Crew member voices
    ├── npcs/        # Non-player character dialogue
    └── narrator/    # Story narration
```

## Supported Formats

- **Primary**: OGG Vorbis (.ogg) - Recommended for all audio
- **Secondary**: WAV (.wav) - For short sound effects only

## Audio Guidelines

### Sound Effects
- Keep files under 5 seconds for most effects
- Use 44.1kHz sample rate
- Mono for most effects, stereo for ambient sounds
- Normalize to -6dB to prevent clipping

### Music
- Loop-friendly tracks (seamless beginning/end)
- 44.1kHz or 48kHz sample rate
- Stereo format
- Compress to reasonable file sizes (under 10MB per track)

### Voice
- Clear dialogue recording
- 44.1kHz sample rate
- Mono format preferred
- Consistent volume levels

## Implementation Notes

- All audio files are loaded through the AudioBuffer class
- 3D positional audio is supported for sound effects
- Music supports crossfading between tracks
- Volume levels are controlled per audio type (music, effects, voice, etc.)

## Placeholder Files

Currently, this directory contains placeholder files. Replace with actual audio assets during development.