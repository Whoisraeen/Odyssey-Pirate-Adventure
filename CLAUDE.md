# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the Project
- **Build**: `mvn compile` - Compiles the Java source code
- **Package**: `mvn package` - Creates executable JAR with dependencies using maven-shade-plugin
- **Clean**: `mvn clean` - Removes target directory and compiled artifacts
- **Test**: `mvn test` - Runs unit tests (currently has basic GameConfigTest)

### Running the Game
- **Run from Maven**: `mvn exec:exec` - Runs the game with default args (--windowed --debug)
- **Run JAR directly**: `java -jar target/odyssey-pirate-adventure-1.0.0-SNAPSHOT.jar [options]`

### Available Runtime Options
- `--windowed` / `--fullscreen` - Display mode
- `--width <pixels>` / `--height <pixels>` - Window dimensions  
- `--debug` - Enable debug logging and rendering info
- `--seed <number>` - Set world generation seed
- `--help` - Show usage information

### Development Profile
The project uses Maven profiles for cross-platform LWJGL natives (Windows, Linux, macOS including ARM).

## Architecture Overview

### Core Engine Structure
The game follows a modular engine architecture with clear separation of concerns:

**Main Entry Point**: `OdysseyGame.java` - Handles command-line parsing, crash reporting, and engine lifecycle

**Engine Core**: `Engine.java` - Central coordinator managing all systems:
- Game loop with fixed timing (targeting consistent frame rate)
- System initialization and cleanup
- Orchestrates rendering, input, audio, and world systems
- Crash-safe error handling and recovery

### Key System Architecture

**Rendering Pipeline** (`graphics/`):
- OpenGL-based renderer with modern shader pipeline
- Camera system with FPS-style controls (WASD + mouse look)
- Separate shaders for basic geometry and ocean water effects
- Built-in wireframe toggle and debug rendering

**Input System** (`input/`):
- Abstracted input layer supporting keyboard, mouse, and gamepad
- Configurable keybinding system with conflict detection
- Input buffering for frame-perfect responses
- Gesture recognition for potential touch support

**World Systems** (`world/`):
- Voxel-based world with chunk management
- Ocean system with animated water and tidal mechanics
- Weather system with dynamic environmental effects
- Biome-based procedural generation

**Memory Management** (`core/memory/`):
- Custom object pooling for frequently allocated objects
- JVM garbage collection tuning for low-latency gameplay
- Memory pressure monitoring

### Technology Stack
- **Java 21** with modern language features
- **LWJGL 3.3.3** for OpenGL, GLFW, OpenAL, and native bindings
- **JOML** for 3D mathematics and transformations
- **Gson** for JSON serialization of game data
- **SLF4J + Logback** for structured logging
- **JUnit 5** for testing

### Game Configuration
`GameConfig.java` serves as the central configuration hub with settings for:
- Display options (resolution, fullscreen, debug mode)
- World generation parameters
- System toggles (weather, tidal systems)
- Performance tuning options

## Development Guidelines

### Code Style and Conventions
- Upgrade existing classes rather than creating "Advanced" or "Enhanced" versions
- Only create new Java classes when absolutely necessary
- Follow the existing package structure and naming conventions
- Use SLF4J logging consistently across all classes

### Testing Requirements
- Compile and test before marking any TODO item as complete
- Confirm with user before checking items off the comprehensive TODO.md
- Write full production code, not stubs or mock functionality
- All new features should include basic error handling and logging

### Project Rules
Based on `.trae/rules/project_rules.md`:
- Do not create files with "Advanced" or "Enhanced" prefixes
- Always upgrade existing codebase when possible
- Confirm with user before checking TODO items as complete
- Work from TODO.md creating complete production code
- Test compilation and execution before ending tasks

### Crash Reporting
The game includes a comprehensive crash reporting system (`CrashReporter.java`) that:
- Captures stack traces and system information
- Provides fallback reporting even during early initialization failures
- Integrates with the main error handling flow

### Performance Considerations
- The engine is designed for real-time performance with careful memory management
- Uses object pooling to minimize GC pressure
- Implements level-of-detail systems for distant rendering
- Supports configurable simulation distance separate from render distance

## Game-Specific Implementation Notes

### Ocean and Maritime Focus
This is a voxel-based maritime adventure game where water simulation and ship mechanics are central:
- Ocean rendering uses animated vertex shaders for realistic wave effects
- Tidal system affects water levels and navigation
- Ship physics and modular construction are core gameplay elements

### World Generation
- Procedural ocean world with scattered islands
- Biome-based generation with climate zones
- Chunk-based loading system for seamless exploration

### Multi-System Coordination
The Engine class demonstrates how systems should interact:
- Systems initialize in dependency order
- Update calls are coordinated with proper delta time
- Cleanup follows reverse initialization order
- All systems are accessible through the Engine singleton

This architecture supports the game's vision of a comprehensive maritime sandbox with advanced physics, dynamic weather, and procedural content generation.