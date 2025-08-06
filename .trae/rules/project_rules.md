# The Odyssey: Project Development Rules

## **CRITICAL: Anti-Chaos Rules**

### **File Naming & Creation Prevention**
- **NEVER use these suffixes**: "Advanced", "Enhanced", "Improved", "Better", "New", "V2", "Extended", "Super", "Pro", "Plus", "Ex"
- **NEVER use these prefixes**: "Enhanced", "Advanced", "Improved", "Better", "New", "Super", "Mega", "Ultra"
- **BANNED class patterns**: `AdvancedWeatherSystem`, `ImprovedRenderer`, `BetterChunkManager`, `NewInputSystem`
- **MANDATORY check**: Before creating ANY new class, search for existing classes with similar functionality
- **Upgrade over create**: If `WeatherSystem.java` exists, upgrade it. NEVER create `WeatherSystemV2.java`

---

## **Core Development Standards**

### **Basic Project Rules**
1. **Always confirm with user** before checking something off the TODO list
2. **Work from the TODO list** creating full production code, no stubs or mock functionality
3. **Test compile and execute** before ending a task
4. **Use industry-standard package naming** (all-lowercase, reverse-DNS) and keep each class in a single public file
5. **Follow Google Java Style** for braces, spacing, imports, and 100-column line length
6. **Add Javadoc** to every public type, method, and field you touch or create

### **Code Quality Standards**
7. **Never delete existing methods** without first deprecating them for one full milestone release
8. **Inject dependencies through constructors** rather than using global singletons
9. **Prefer immutable data objects**; if mutability required, synchronize or use `java.util.concurrent` primitives
10. **Maintain ≥ 90% line-coverage** in core module; block compilation if coverage drops below threshold
11. **Log meaningful messages** at INFO and errors at ERROR; never print directly to `System.out` or `System.err`

### **Resource Management**
13. **Use try-with-resources** for every I/O stream and closeable object to avoid leaks
14. **Guard all public entry points** with null-checks and validate external data before use
15. **Avoid hard-coded constants**; place configurable values in `*.properties` files loaded via resource manager
16. **Run static-analysis** (SpotBugs, PMD, Checkstyle) and refuse to compile if any new high-severity issues appear

### **Version Control & Documentation**
17. **Group commits by logical task**; each commit message must reference exact TODO list item ID it addresses
18. **Never merge generated code** directly into main without opening pull request and waiting for human approval
19. **Record version bumps** and migration notes in `CHANGELOG.md` when upgrading third-party libraries
20. **Document every new configuration key** in `docs/configuration_reference.md` as soon as it is added

### **Performance Requirements**
21. **Profile rendering/physics code** with flight-recorder settings; ensure no single frame exceeds 16ms budget
22. **Default to non-blocking, asynchronous patterns** for file/network/chunk-generation tasks exceeding 2ms on main thread
23. **Raise confirmation prompt** before introducing breaking API changes or refactoring widely-used packages

---

## **The Odyssey Specific Rules**

### **Project Identity**
- **Main class**: `com.odyssey.OdysseyGame`
- **Run command**: `mvn exec:java "-Dexec.mainClass=com.odyssey.OdysseyGame"`
- **Platform support**: Windows, macOS, and Linux
- **Performance target**: Run on potato hardware with AAA graphics (graphics are the most important part)

### **System Architecture Preservation**
- **Never bypass Engine singleton**: All systems must be accessible through `Engine.getInstance()` as documented in CLAUDE.md
- **Respect dependency order**: Systems must initialize in documented order (input → rendering → audio → world)
- **Use existing system patterns**: If `WeatherSystem` exists, extend it rather than creating `OceanWeatherSystem`
- **Follow modular engine pattern**: Keep rendering, input, audio, and world systems separate as documented

### **Maritime Game Specific Rules**
- **Ocean-centric naming**: Use maritime terminology consistently (e.g., `helm` not `steering`, `bow/stern` not `front/back`)
- **Water physics priority**: Any changes affecting water must maintain advanced Gerstner wave system
- **Ship modularity**: Never create monolithic ship classes - all ships must be voxel-based modular constructions
- **Tidal system integration**: Any time-based systems must respect 20-minute tidal cycle from expanded concept

---

## **Technology Stack Compliance**

### **Required Technologies**
- **Java 21 features only**: Use modern Java features but stay within Java 21 compatibility
- **LWJGL 3.3.3 consistency**: Don't import different versions of LWJGL components
- **JOML for all math**: Never import alternative math libraries like Apache Commons Math
- **SLF4J + Logback only**: Don't use `java.util.logging` or other logging frameworks
- **Gson for JSON**: Don't switch to Jackson or other JSON libraries

### **Performance & Graphics Rules**
- **Maintain "potato to AAA" scaling**: Any graphics feature must have LOW/MEDIUM/HIGH/ULTRA quality levels
- **Respect frame budget**: No single system should exceed 16ms per frame as documented
- **Use existing object pools**: Check `core/memory/` before creating new object instances
- **Shader naming convention**: Follow `basic.vert/basic.frag` pattern, not `BasicVertexShader.java`

---

## **Consistency Enforcement Rules**

### **Method & Variable Naming Consistency**
- **Never rename existing public methods** without deprecating them first for one full milestone
- **Use existing naming patterns**: If existing methods use `get/set`, don't suddenly switch to `retrieve/update`
- **Check existing field names**: If class has `textureCache`, don't add `materialStorage` - use `materialCache` for consistency
- **Follow existing abbreviations**: If codebase uses `Mgr` for Manager, don't switch to `Manager` - stay consistent

### **Package Structure Preservation**
- **Never reorganize packages** without explicit user approval - it breaks imports everywhere
- **Follow existing package patterns**: If graphics classes are in `com.odyssey.graphics`, don't create `com.odyssey.render`
- **Check package before creating**: If `com.odyssey.world.ocean` exists, don't create `com.odyssey.ocean`

### **Dependency & Import Discipline**
- **Use existing dependencies**: If project already uses JOML for math, don't import different math library
- **Check existing utilities**: Before creating `StringHelper`, check if `StringUtils` already exists
- **Follow existing patterns**: If project uses SLF4J for logging, don't suddenly use `System.out.println`

### **Configuration & Constants Management**
- **Check existing config patterns**: If project uses `.properties` files, don't create `.yaml` configs
- **Use existing constant naming**: If project uses `CHUNK_SIZE`, don't create `chunkDimension`
- **Extend existing enums**: Instead of creating new enums, extend existing ones (like `BlockType`, `WeatherState`)

---

## **System Integration Rules**

### **Configuration System Rules**
- **Extend GameConfig.java**: Add new settings to existing config, don't create separate config classes
- **Use properties files**: Follow `.properties` pattern for configuration as documented
- **Respect existing toggles**: Don't duplicate functionality of existing config options like `isEnableTidalSystem()`

### **World System Integration**
- **Chunk-based everything**: All world features must work with existing chunk system
- **Biome compatibility**: New features must integrate with existing biome generation
- **Procedural generation respect**: Use existing seed system and noise generators
- **LOD system compatibility**: New rendering must work with existing LOD system

### **Multiplayer Consideration Rules**
- **Thread-safe by default**: Use `ConcurrentHashMap` and atomic operations as shown in existing code
- **Serializable state**: All game state must be serializable for persistent world design
- **Network-aware design**: Consider bandwidth impact for massive persistent world described
- **Event system integration**: Use existing publish/subscribe pattern for cross-system communication

### **Package Structure Enforcement**
- **Graphics in `com.odyssey.graphics`**: All rendering code belongs here
- **World in `com.odyssey.world`**: All world generation and management
- **Input in `com.odyssey.input`**: All input handling and keybinding
- **Core in `com.odyssey.core`**: Engine, config, memory management
- **Never create parallel packages**: Don't create `com.odyssey.render` when `com.odyssey.graphics` exists

### **Asset and Resource Rules**
- **Use existing AssetManager**: Don't create separate resource loading systems
- **Texture atlas integration**: All textures must go through TextureAtlasManager system
- **Streaming compatibility**: New assets must work with priority-based streaming system
- **Cross-platform paths**: Use forward slashes and relative paths for all resource references

### **Error Handling Patterns**
- **Use CrashReporter integration**: Fatal errors should integrate with existing crash reporting system
- **Graceful degradation**: Graphics features should fall back to simpler versions, not crash
- **Comprehensive logging**: Log meaningful state changes for complex maritime systems
- **Recovery patterns**: Systems should attempt recovery before failing, especially for multiplayer persistence

---

## **Pre-Creation Checklist**

Before creating ANY new file, method, or system:

1. ✅ **Search for existing similar functionality**
2. ✅ **Check if existing class can be extended/upgraded**
3. ✅ **Verify package structure follows existing patterns**
4. ✅ **Confirm dependencies match existing technology stack**
5. ✅ **Ensure naming follows existing conventions**
6. ✅ **Verify it integrates with existing systems (Engine, config, etc.)**
7. ✅ **Check if it maintains maritime game theme consistency**
8. ✅ **Review for performance implications**
9. ✅ **Check for memory leaks or resource exhaustion**
10. ✅ **Verify thread safety**
11. ✅ **Check for cross-platform compatibility**
12. ✅ **Review for security implications**
13. ✅ **Check for compliance with licensing requirements**
14. ✅ **Verify it aligns with existing design patterns**
15. ✅ **Check for potential integration points with future features**
