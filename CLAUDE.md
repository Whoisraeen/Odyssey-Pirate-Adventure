# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**The Odyssey** is a revolutionary voxel-based maritime adventure game built with Java and LWJGL. It features a procedurally generated infinite ocean world with dynamic islands, advanced ship building mechanics, realistic ocean physics, and immersive survival gameplay. Players start as castaways and build their way up to legendary pirate captains.

## Build System & Commands

This is a **Maven-based Java project** using Java 17.

### Essential Commands

**Build the project:**
```bash
mvn clean compile
```

**Run tests:**
```bash
mvn test
```

**Create executable JAR:**
```bash
mvn clean package
```

**Run the game:**
```bash
# Using Maven exec plugin (development)
mvn exec:java

# Or run the JAR directly
java -jar target/odyssey-pirate-adventure-1.0.0-SNAPSHOT.jar

# With command line options
java -jar target/odyssey-pirate-adventure-1.0.0-SNAPSHOT.jar --windowed --debug
```

**Available command line options:**
- `--windowed` / `--fullscreen` - Display mode
- `--width <pixels>` / `--height <pixels>` - Window dimensions
- `--debug` - Enable debug mode
- `--seed <number>` - Set world generation seed

### Dependencies

The project uses LWJGL 3.3.3 for:
- OpenGL rendering
- GLFW window management
- OpenAL audio
- STB image loading
- Assimp 3D model loading

Additional libraries:
- JOML for mathematics
- Gson for JSON processing
- SLF4J + Logback for logging
- JUnit 5 for testing

## Architecture Overview

### Core Engine Structure

The game follows a **component-based engine architecture** centered around the `Engine` class:

**Main Entry Point:**
- `com.odyssey.OdysseyGame` - Main class that parses command line args and initializes the engine

**Core Systems:**
- `com.odyssey.core.Engine` - Main game loop, system coordination, and lifecycle management
- `com.odyssey.core.GameConfig` - Centralized configuration management
- `com.odyssey.graphics.Renderer` - OpenGL rendering system
- `com.odyssey.graphics.Window` - GLFW window management
- `com.odyssey.input.InputManager` - Input handling and event processing
- `com.odyssey.audio.AudioManager` - OpenAL audio system

### World System Architecture

**Voxel World Management:**
- `com.odyssey.world.World` - Main world coordinator (chunk loading, player position tracking)
- `com.odyssey.world.generation.WorldGenerator` - Procedural world generation algorithms
- `com.odyssey.world.chunk.ChunkManager` - Chunk loading/unloading and block data management
- `com.odyssey.world.biome.BiomeManager` - Biome generation and management

**Ocean Simulation:**
- `com.odyssey.world.ocean.OceanSystem` - Main ocean system coordinator
- `com.odyssey.world.ocean.TidalSystem` - Dynamic tidal mechanics
- `com.odyssey.world.ocean.WaveSystem` - Wave physics simulation
- `com.odyssey.world.ocean.CurrentSystem` - Ocean current simulation
- `com.odyssey.world.ocean.MarineEcosystem` - Marine life and ecosystem simulation

**Environmental Systems:**
- `com.odyssey.world.weather.WeatherSystem` - Dynamic weather patterns

### Key Constants and Configuration

**World Properties:**
- Chunk size: 16x16x16 blocks
- World height: 256 blocks
- Sea level: 64 blocks (Y coordinate)
- Default render distance: 16 chunks

**Performance Settings:**
- Target FPS: 60
- Max chunk updates per frame: 4
- Multithreading enabled by default
- Worker threads: CPU cores - 1

## Development Patterns

### System Lifecycle
All major systems follow this pattern:
1. Constructor (accepts config/dependencies)
2. `initialize()` - Setup resources
3. `update(deltaTime)` - Per-frame updates
4. `render(renderer)` - Rendering (if applicable)
5. `cleanup()` - Resource disposal

### Configuration Management
- All settings centralized in `GameConfig`
- Settings loaded from command line args
- Debug mode enables additional logging and debug rendering
- Ocean simulation and weather systems can be toggled via config

### Error Handling
- SLF4J logging used throughout
- Fatal errors logged and cause `System.exit(1)`
- Clean resource disposal in cleanup methods

### Testing
- JUnit 5 for unit tests
- Test classes follow `*Test.java` naming convention
- Tests focus on configuration and core functionality

## World Generation Details

The game uses a **seed-based procedural generation system** with these key features:

- **Infinite Ocean:** Procedurally generated as player explores
- **Dynamic Islands:** Generated with multiple biomes (tropical, volcanic, jungle, mangrove, arctic, cursed)
- **Chunk-Based Loading:** 16x16x16 voxel chunks loaded/unloaded based on player position
- **Biome System:** Climate zones influence island generation, resources, and weather
- **Height Maps:** Terrain generated using noise functions with sea level at Y=64

## Ocean Systems

The ocean is the game's central feature with multiple interconnected systems:

- **Tidal System:** Real-time tide simulation affecting water levels
- **Wave System:** Dynamic wave physics for realistic water movement  
- **Current System:** Ocean currents affecting ship navigation
- **Marine Ecosystem:** Living ecosystem with dynamic marine life

These systems can be individually enabled/disabled through `GameConfig` for performance tuning.

## Graphics and Rendering

- **OpenGL-based rendering** through LWJGL
- **Frustum culling** enabled by default
- **Occlusion culling** available but disabled by default (performance impact)
- **Debug rendering** available when debug mode is enabled
- **FPS display** in window title during debug mode

## Performance Considerations

- **Multithreading:** Worker threads handle chunk generation and world updates
- **Chunk Management:** Only chunks within render distance are kept loaded
- **Configurable Quality:** Ocean simulation quality can be adjusted (0.5x to 2.0x)
- **Frame Rate Limiting:** Target FPS configurable, VSync enabled by default