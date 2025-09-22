Java 25 is installed on the system at C:\Program Files\Java\jdk-25\bin
Think of this game development exactly how minecraft works as its based off minecraft but must not violate any copyright. 

## Project Overview
This is a Java-based voxel pirate adventure game called "The Odyssey" - an open-world survival, crafting, and adventure game set in a procedurally generated voxel ocean. Players begin as castaways and progress to legendary pirate captains through ship building, exploration, and naval combat.

## Core Game Concepts (MUST READ BEFORE CODING)

### Primary Game Mechanics
- **Voxel-based world** with procedural island generation
- **Modular ship building** - ships built block-by-block like Minecraft structures
- **Dynamic ocean physics** - wind, currents, tides, weather systems
- **Multi-tiered progression** - Age of Sail → Age of Steam → Age of Wonders
- **Fleet management** - command multiple ships with AI captains
- **Living ecosystem** - marine food chains, seasonal migrations, predator-prey relationships
- **Advanced combat** - naval warfare, boarding actions, crew management
- **Reputation system** - honor, infamy, reliability, mercy across multiple factions

### Technical Architecture
- **Java-based** with Maven build system
- **Modular component system** for ships (hull, sails, cannons, engines)
- **Voxel rendering engine** with advanced water physics
- **Procedural generation** for islands, biomes, weather, and ocean features
- **Multiplayer support** with persistent world design

## Coding Standards & Requirements

### 1. Package Structure Compliance
- **ALWAYS** follow the existing package structure: `com.odyssey.*`
- **Core systems**: `core/` - GameEngine, GameState, ResourceManager, StateManager
- **Rendering**: `rendering/` - RenderEngine, Shader, Texture, Material, Mesh
- **Physics**: `physics/` - PhysicsEngine, OceanPhysics, WaveSystem, FluidDynamics
- **Ship systems**: `ship/` - Ship, ShipComponent, ShipPhysics, ShipBuilder
- **World generation**: `world/` - World, WorldGenerator, IslandGenerator, BiomeType
- **Audio**: `audio/` - AudioEngine
- **Input**: `input/` - InputManager
- **Networking**: `networking/` - NetworkManager
- **Utilities**: `util/` - Logger, MathUtils, Timer

### 2. Import Management
- **ALWAYS** check existing imports before adding new ones
- **ALWAYS** use fully qualified package names for clarity
- **ALWAYS** organize imports: Java standard → third-party → project packages
- **NEVER** use wildcard imports (`import com.odyssey.*`)
- **ALWAYS** verify import paths match the actual file structure

### 3. Code Documentation Requirements
- **MANDATORY**: Add comprehensive JavaDoc comments to ALL public methods and classes
- **MANDATORY**: Include inline comments explaining complex game logic
- **MANDATORY**: Document all ship component interactions and physics calculations
- **MANDATORY**: Explain procedural generation algorithms and parameters
- **MANDATORY**: Document faction relationships and reputation calculations
- **MANDATORY**: Comment all rendering pipeline stages and shader operations

### 4. Game-Specific Coding Rules

#### Ship System Development
- **ALWAYS** implement ships as modular components (HullComponent, SailComponent, CannonComponent, EngineComponent)
- **ALWAYS** consider ship physics: weight distribution, buoyancy, stability
- **ALWAYS** implement crew management with roles (Cannoneers, Helmsmen, Lookouts, Deckhands)
- **ALWAYS** account for different ship types: Exploration Vessels, War Galleons, Merchant Traders, Stealth Runners

#### World Generation
- **ALWAYS** implement biome-specific features: Tropical Atolls, Volcanic Spires, Dense Jungles, Mangrove Swamps, Whispering Isles, Arctic Archipelagoes
- **ALWAYS** include dynamic weather systems: wind direction, ocean currents, storms, hurricanes
- **ALWAYS** implement tidal mechanics with 20-minute cycles
- **ALWAYS** create depth zones: Surface (0-10), Thermocline (10-50), Abyssal (50-200), Hadal (200+)

#### Combat Systems
- **ALWAYS** implement tactical naval combat with broadside positioning
- **ALWAYS** include different ammunition types: Cannonball, Chain Shot, Grapeshot
- **ALWAYS** implement boarding mechanics with grappling hooks
- **ALWAYS** consider crew morale and mutiny systems

#### Economy & Reputation
- **ALWAYS** implement multi-dimensional reputation: Honor, Infamy, Reliability, Mercy
- **ALWAYS** include faction relationships: Royal Navy, Crimson Corsairs, Free Traders Guild, Islander Villagers
- **ALWAYS** implement supply and demand economics with fluctuating prices

### 5. File Structure Validation
- **ALWAYS** verify file paths match the package structure
- **ALWAYS** check that new files are placed in the correct directory
- **ALWAYS** ensure class names match file names exactly
- **ALWAYS** validate that imports reference existing files

### 6. Performance Considerations
- **ALWAYS** optimize voxel rendering for large worlds
- **ALWAYS** implement efficient chunk loading/unloading
- **ALWAYS** consider memory usage for procedural generation
- **ALWAYS** optimize physics calculations for multiple ships
- **ALWAYS** implement LOD (Level of Detail) for distant objects

### 7. Multiplayer Considerations
- **ALWAYS** design systems with multiplayer synchronization in mind
- **ALWAYS** implement proper state management for network play
- **ALWAYS** consider latency compensation for naval combat
- **ALWAYS** implement proper conflict resolution for simultaneous actions

### 8. Testing Requirements
- **ALWAYS** test ship physics with different configurations
- **ALWAYS** verify procedural generation produces varied results
- **ALWAYS** test weather systems affect ship handling realistically
- **ALWAYS** validate faction reputation changes work correctly
- **ALWAYS** test combat systems with different ship types

### 9. Code Review Checklist
Before submitting any code changes:
- [ ] Package structure follows `com.odyssey.*` convention
- [ ] All imports are correct and necessary
- [ ] JavaDoc comments are comprehensive
- [ ] Inline comments explain complex logic
- [ ] Ship components follow modular design
- [ ] World generation includes biome variety
- [ ] Physics calculations are realistic
- [ ] Faction systems are properly implemented
- [ ] Performance implications are considered
- [ ] Multiplayer compatibility is maintained

### 10. Common Pitfalls to Avoid
- **NEVER** hardcode ship configurations - use the component system
- **NEVER** ignore wind and current effects in ship movement
- **NEVER** create static, unchanging worlds - implement dynamic systems
- **NEVER** forget crew needs and morale in ship management
- **NEVER** ignore faction consequences for player actions
- **NEVER** create unrealistic physics for ships and ocean
- **NEVER** forget to implement proper error handling for network operations

## Context Awareness Rules

### When Working on Ship Systems
- **ALWAYS** reference existing Ship.java, ShipComponent.java, ShipPhysics.java
- **ALWAYS** consider how new components interact with existing hull, sail, cannon, engine components
- **ALWAYS** maintain compatibility with ShipBuilder.java patterns

### When Working on World Generation
- **ALWAYS** reference IslandGenerator.java, WorldGenerator.java, BiomeType.java
- **ALWAYS** consider how new biomes affect existing Island.java, IslandType.java, IslandClimate.java
- **ALWAYS** maintain compatibility with Chunk.java, WorldChunk.java systems

### When Working on Rendering
- **ALWAYS** reference RenderEngine.java, Shader.java, Material.java, Mesh.java
- **ALWAYS** consider how new rendering features affect existing Camera.java, Framebuffer.java
- **ALWAYS** maintain compatibility with ShaderManager.java patterns

### When Working on Physics
- **ALWAYS** reference PhysicsEngine.java, OceanPhysics.java, WaveSystem.java
- **ALWAYS** consider how new physics affect existing FluidDynamics.java
- **ALWAYS** maintain compatibility with ship physics calculations

## Emergency Context Recovery
If you lose context about the project:
1. **READ** the expanded_odyssey_concept.md file for game design overview
2. **READ** the Game.md file for core mechanics and technical details
3. **EXAMINE** the existing Java files in the src/main/java/com/odyssey/ directory
4. **CHECK** the pom.xml for dependencies and build configuration
5. **REVIEW** the package structure to understand system organization

## Final Reminder
This is a complex, ambitious project that combines voxel world generation, advanced ship physics, dynamic weather systems, and multiplayer functionality. Every line of code should contribute to creating an immersive pirate adventure experience. When in doubt, prioritize gameplay fun and technical excellence over quick implementations.
