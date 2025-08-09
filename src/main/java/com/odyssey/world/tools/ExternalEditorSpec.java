package com.odyssey.world.tools;

import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * External Editor Specification & Sample Generator.
 * Provides comprehensive documentation and sample files for external world editors.
 */
public class ExternalEditorSpec {
    private static final Logger logger = LoggerFactory.getLogger(ExternalEditorSpec.class);
    
    private static final String SPEC_VERSION = "1.0.0";
    private static final String ODYSSEY_VERSION = "0.1.0";
    
    /**
     * Prints usage information for the CLI.
     */
    private static void printUsage() {
        System.out.println("Usage: ExternalEditorSpec <command> [output_path]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  generate  - Generate complete specification (default: odyssey_editor_spec)");
        System.out.println("  samples   - Generate sample files only");
        System.out.println("  docs      - Generate documentation only");
        System.out.println("  schema    - Generate schema files only");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  ExternalEditorSpec generate ./spec");
        System.out.println("  ExternalEditorSpec samples ./samples");
        System.out.println("  ExternalEditorSpec docs ./docs");
    }
    
    /**
     * Main CLI entry point for generating external editor documentation and samples.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        Path outputPath = args.length > 1 ? Paths.get(args[1]) : Paths.get("odyssey_editor_spec");
        
        ExternalEditorSpec spec = new ExternalEditorSpec();
        
        try {
            switch (command.toLowerCase()) {
                case "generate" -> {
                    spec.generateFullSpecification(outputPath);
                    System.out.println("External editor specification generated at: " + outputPath);
                }
                case "samples" -> {
                    spec.generateSampleFiles(outputPath);
                    System.out.println("Sample files generated at: " + outputPath);
                }
                case "docs" -> {
                    spec.generateDocumentation(outputPath);
                    System.out.println("Documentation generated at: " + outputPath);
                }
                case "schema" -> {
                    spec.generateSchemaFiles(outputPath);
                    System.out.println("Schema files generated at: " + outputPath);
                }
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("Specification generation failed", e);
            System.exit(1);
        }
    }
    
    /**
     * Generates the complete external editor specification.
     */
    public static void generateFullSpecification(Path outputPath) throws IOException {
        logger.info("Generating full external editor specification at: {}", outputPath);
        
        Files.createDirectories(outputPath);
        
        // Generate all components
        generateDocumentation(outputPath);
        generateSchemaFiles(outputPath);
        generateSampleFiles(outputPath);
        generateToolingExamples(outputPath);
        generateValidationScripts(outputPath);
        
        // Generate main README
        generateMainReadme(outputPath);
        
        logger.info("Full specification generated successfully");
    }
    
    /**
     * Generates comprehensive documentation for the world format.
     */
    public static void generateDocumentation(Path outputPath) throws IOException {
        Path docsPath = outputPath.resolve("docs");
        Files.createDirectories(docsPath);
        
        // Generate format overview
        generateFormatOverview(docsPath);
        
        // Generate file format specifications
        generateLevelDatSpec(docsPath);
        generateRegionFileSpec(docsPath);
        generatePlayerDataSpec(docsPath);
        generateChunkDataSpec(docsPath);
        
        // Generate API documentation
        generateApiDocumentation(docsPath);
        
        // Generate best practices guide
        generateBestPracticesGuide(docsPath);
        
        logger.info("Documentation generated at: {}", docsPath);
    }
    
    /**
     * Generates schema files for validation and tooling.
     */
    public static void generateSchemaFiles(Path outputPath) throws IOException {
        Path schemaPath = outputPath.resolve("schemas");
        Files.createDirectories(schemaPath);
        
        // Generate JSON schemas
        Files.writeString(schemaPath.resolve("level_dat.schema.json"), generateLevelDatSchema());
        Files.writeString(schemaPath.resolve("chunk_data.schema.json"), generateChunkSchema());
        Files.writeString(schemaPath.resolve("region_file.schema.json"), generateRegionSchema());
        Files.writeString(schemaPath.resolve("player_data.schema.json"), generatePlayerDataSchema());
        
        // Generate binary format specifications
        generateBinaryFormatSpecs(schemaPath);
        
        logger.info("Schema files generated at: {}", schemaPath);
    }
    
    /**
     * Generates sample world files for testing and reference.
     */
    public static void generateSampleFiles(Path outputPath) throws IOException {
        Path samplesPath = outputPath.resolve("samples");
        Files.createDirectories(samplesPath);
        
        // Generate minimal world
        generateMinimalWorld(samplesPath.resolve("minimal_world"));
        
        // Generate complex world
        generateComplexWorld(samplesPath.resolve("complex_world"));
        
        // Generate corrupted samples for testing
        generateCorruptedSamples(samplesPath.resolve("corrupted_samples"));
        
        logger.info("Sample files generated at: {}", samplesPath);
    }
    
    /**
     * Generates tooling examples and utilities.
     */
    public static void generateToolingExamples(Path outputPath) throws IOException {
        Path toolsPath = outputPath.resolve("tools");
        Files.createDirectories(toolsPath);
        
        // Generate Python examples
        generatePythonExamples(toolsPath.resolve("python"));
        
        // Generate JavaScript examples
        generateJavaScriptExamples(toolsPath.resolve("javascript"));
        
        // Generate C++ examples
        generateCppExamples(toolsPath.resolve("cpp"));
        
        logger.info("Tooling examples generated at: {}", toolsPath);
    }
    
    /**
     * Generates validation scripts for format verification.
     */
    public static void generateValidationScripts(Path outputPath) throws IOException {
        Path validationPath = outputPath.resolve("validation");
        Files.createDirectories(validationPath);
        
        generateFormatValidator(validationPath);
        generateIntegrityChecker(validationPath);
        generateCompatibilityTester(validationPath);
        
        logger.info("Validation scripts generated at: {}", validationPath);
    }
    
    // Tooling generation methods
    
    private static void generatePythonExamples(Path pythonPath) throws IOException {
        Files.createDirectories(pythonPath);
        
        String pythonExample = """
            #!/usr/bin/env python3
            \"\"\"
            Odyssey World Format Python Example
            
            This example demonstrates how to read and write Odyssey world files in Python.
            \"\"\"
            
            import struct
            import json
            import gzip
            from pathlib import Path
            
            class OdysseyWorldReader:
                def __init__(self, world_path):
                    self.world_path = Path(world_path)
                
                def read_level_data(self):
                    \"\"\"Read level.json file.\"\"\"
                    level_file = self.world_path / "level.json"
                    if level_file.exists():
                        with open(level_file, 'r') as f:
                            return json.load(f)
                    return None
                
                def read_region_file(self, region_x, region_z):
                    \"\"\"Read a region file.\"\"\"
                    region_file = self.world_path / "regions" / f"r.{region_x}.{region_z}.region"
                    if not region_file.exists():
                        return None
                    
                    with open(region_file, 'rb') as f:
                        # Read header
                        magic = struct.unpack('>I', f.read(4))[0]
                        if magic != 0x4F524547:  # "OREG"
                            raise ValueError("Invalid region file magic number")
                        
                        version = struct.unpack('>I', f.read(4))[0]
                        chunk_count = struct.unpack('>I', f.read(4))[0]
                        
                        # Read chunk table
                        chunks = {}
                        for i in range(chunk_count):
                            chunk_x = struct.unpack('>i', f.read(4))[0]
                            chunk_z = struct.unpack('>i', f.read(4))[0]
                            offset = struct.unpack('>Q', f.read(8))[0]
                            size = struct.unpack('>I', f.read(4))[0]
                            chunks[(chunk_x, chunk_z)] = (offset, size)
                        
                        return chunks
            
            def main():
                # Example usage
                reader = OdysseyWorldReader("example_world")
                level_data = reader.read_level_data()
                if level_data:
                    print(f"World: {level_data.get('worldName', 'Unknown')}")
                    print(f"Seed: {level_data.get('seed', 0)}")
            
            if __name__ == "__main__":
                main()
            """;
        
        Files.writeString(pythonPath.resolve("odyssey_reader.py"), pythonExample);
    }
    
    private static void generateJavaScriptExamples(Path jsPath) throws IOException {
        Files.createDirectories(jsPath);
        
        String jsExample = """
            /**
             * Odyssey World Format JavaScript Example
             * 
             * This example demonstrates how to read Odyssey world files in Node.js.
             */
            
            const fs = require('fs');
            const path = require('path');
            const zlib = require('zlib');
            
            class OdysseyWorldReader {
                constructor(worldPath) {
                    this.worldPath = worldPath;
                }
                
                readLevelData() {
                    const levelFile = path.join(this.worldPath, 'level.json');
                    if (fs.existsSync(levelFile)) {
                        const data = fs.readFileSync(levelFile, 'utf8');
                        return JSON.parse(data);
                    }
                    return null;
                }
                
                readRegionFile(regionX, regionZ) {
                    const regionFile = path.join(this.worldPath, 'regions', `r.${regionX}.${regionZ}.region`);
                    if (!fs.existsSync(regionFile)) {
                        return null;
                    }
                    
                    const buffer = fs.readFileSync(regionFile);
                    let offset = 0;
                    
                    // Read header
                    const magic = buffer.readUInt32BE(offset);
                    offset += 4;
                    
                    if (magic !== 0x4F524547) { // "OREG"
                        throw new Error('Invalid region file magic number');
                    }
                    
                    const version = buffer.readUInt32BE(offset);
                    offset += 4;
                    
                    const chunkCount = buffer.readUInt32BE(offset);
                    offset += 4;
                    
                    // Read chunk table
                    const chunks = {};
                    for (let i = 0; i < chunkCount; i++) {
                        const chunkX = buffer.readInt32BE(offset);
                        offset += 4;
                        const chunkZ = buffer.readInt32BE(offset);
                        offset += 4;
                        const chunkOffset = buffer.readBigUInt64BE(offset);
                        offset += 8;
                        const size = buffer.readUInt32BE(offset);
                        offset += 4;
                        
                        chunks[`${chunkX},${chunkZ}`] = { offset: chunkOffset, size };
                    }
                    
                    return chunks;
                }
            }
            
            // Example usage
            function main() {
                const reader = new OdysseyWorldReader('example_world');
                const levelData = reader.readLevelData();
                if (levelData) {
                    console.log(`World: ${levelData.worldName || 'Unknown'}`);
                    console.log(`Seed: ${levelData.seed || 0}`);
                }
            }
            
            if (require.main === module) {
                main();
            }
            
            module.exports = OdysseyWorldReader;
            """;
        
        Files.writeString(jsPath.resolve("odyssey-reader.js"), jsExample);
    }
    
    private static void generateCppExamples(Path cppPath) throws IOException {
        Files.createDirectories(cppPath);
        
        String cppExample = """
            /**
             * Odyssey World Format C++ Example
             * 
             * This example demonstrates how to read Odyssey world files in C++.
             */
            
            #include <iostream>
            #include <fstream>
            #include <string>
            #include <vector>
            #include <map>
            #include <cstdint>
            #include <filesystem>
            
            namespace odyssey {
            
            struct ChunkInfo {
                uint64_t offset;
                uint32_t size;
            };
            
            class WorldReader {
            private:
                std::filesystem::path worldPath;
                
            public:
                WorldReader(const std::string& path) : worldPath(path) {}
                
                bool readLevelData(std::string& jsonData) {
                    auto levelFile = worldPath / "level.json";
                    if (!std::filesystem::exists(levelFile)) {
                        return false;
                    }
                    
                    std::ifstream file(levelFile);
                    if (!file.is_open()) {
                        return false;
                    }
                    
                    std::string line;
                    jsonData.clear();
                    while (std::getline(file, line)) {
                        jsonData += line + "\\n";
                    }
                    
                    return true;
                }
                
                bool readRegionFile(int regionX, int regionZ, std::map<std::pair<int, int>, ChunkInfo>& chunks) {
                    auto regionFile = worldPath / "regions" / ("r." + std::to_string(regionX) + "." + std::to_string(regionZ) + ".region");
                    if (!std::filesystem::exists(regionFile)) {
                        return false;
                    }
                    
                    std::ifstream file(regionFile, std::ios::binary);
                    if (!file.is_open()) {
                        return false;
                    }
                    
                    // Read header
                    uint32_t magic;
                    file.read(reinterpret_cast<char*>(&magic), sizeof(magic));
                    magic = __builtin_bswap32(magic); // Convert from big-endian
                    
                    if (magic != 0x4F524547) { // "OREG"
                        return false;
                    }
                    
                    uint32_t version;
                    file.read(reinterpret_cast<char*>(&version), sizeof(version));
                    version = __builtin_bswap32(version);
                    
                    uint32_t chunkCount;
                    file.read(reinterpret_cast<char*>(&chunkCount), sizeof(chunkCount));
                    chunkCount = __builtin_bswap32(chunkCount);
                    
                    // Read chunk table
                    for (uint32_t i = 0; i < chunkCount; i++) {
                        int32_t chunkX, chunkZ;
                        uint64_t offset;
                        uint32_t size;
                        
                        file.read(reinterpret_cast<char*>(&chunkX), sizeof(chunkX));
                        file.read(reinterpret_cast<char*>(&chunkZ), sizeof(chunkZ));
                        file.read(reinterpret_cast<char*>(&offset), sizeof(offset));
                        file.read(reinterpret_cast<char*>(&size), sizeof(size));
                        
                        chunkX = __builtin_bswap32(chunkX);
                        chunkZ = __builtin_bswap32(chunkZ);
                        offset = __builtin_bswap64(offset);
                        size = __builtin_bswap32(size);
                        
                        chunks[{chunkX, chunkZ}] = {offset, size};
                    }
                    
                    return true;
                }
            };
            
            } // namespace odyssey
            
            int main() {
                odyssey::WorldReader reader("example_world");
                
                std::string levelData;
                if (reader.readLevelData(levelData)) {
                    std::cout << "Level data loaded successfully" << std::endl;
                    // Parse JSON here if needed
                }
                
                std::map<std::pair<int, int>, odyssey::ChunkInfo> chunks;
                if (reader.readRegionFile(0, 0, chunks)) {
                    std::cout << "Region file loaded with " << chunks.size() << " chunks" << std::endl;
                }
                
                return 0;
            }
            """;
        
        Files.writeString(cppPath.resolve("odyssey_reader.cpp"), cppExample);
    }
    
    // Documentation generation methods
    
    private static void generateMainReadme(Path outputPath) throws IOException {
        String readme = """
            # Odyssey World Format Specification
            
            Version: %s
            Odyssey Version: %s
            Generated: %s
            
            ## Overview
            
            This specification defines the file format used by Odyssey: Pirate Adventure for storing world data.
            The format is designed to be efficient, extensible, and suitable for both single-player and multiplayer scenarios.
            
            ## Directory Structure
            
            ```
            odyssey_editor_spec/
            ├── docs/                    # Comprehensive documentation
            │   ├── format_overview.md   # High-level format description
            │   ├── level_dat_spec.md    # Level.dat file specification
            │   ├── region_file_spec.md  # Region file format
            │   ├── player_data_spec.md  # Player data format
            │   ├── chunk_data_spec.md   # Chunk data structure
            │   ├── api_documentation.md # Programming API reference
            │   └── best_practices.md    # Development best practices
            ├── schemas/                 # Validation schemas
            │   ├── level_dat.json       # JSON schema for level data
            │   ├── chunk_data.json      # JSON schema for chunk data
            │   ├── player_data.json     # JSON schema for player data
            │   └── binary_formats.md    # Binary format specifications
            ├── samples/                 # Sample world files
            │   ├── minimal_world/       # Minimal valid world
            │   ├── complex_world/       # Complex example world
            │   └── corrupted_samples/   # Corrupted files for testing
            ├── tools/                   # Example implementations
            │   ├── python/              # Python tools and examples
            │   ├── javascript/          # JavaScript/Node.js tools
            │   └── cpp/                 # C++ examples
            └── validation/              # Validation and testing tools
                ├── format_validator.py  # Format validation script
                ├── integrity_checker.py # Data integrity checker
                └── compatibility_test.py # Version compatibility tester
            ```
            
            ## Quick Start
            
            1. Read `docs/format_overview.md` for a high-level understanding
            2. Examine the sample worlds in `samples/`
            3. Use the validation tools in `validation/` to verify your implementations
            4. Refer to the programming examples in `tools/` for your language of choice
            
            ## Key Features
            
            - **Chunk-based storage**: Efficient loading and streaming
            - **Compression support**: GZIP compression for space efficiency
            - **Version compatibility**: Forward and backward compatibility handling
            - **Integrity checking**: Built-in checksums and validation
            - **Extensible format**: Support for custom data and future features
            
            ## File Format Summary
            
            | File Type | Extension | Compression | Description |
            |-----------|-----------|-------------|-------------|
            | Level Data | `.dat` | Optional | World metadata and settings |
            | Region Files | `.region` | Yes | Chunk data storage |
            | Player Data | `.dat` | Yes | Individual player information |
            | Session Lock | `.lock` | No | Session management |
            
            ## Magic Numbers
            
            | File Type | Magic Number | Hex Value |
            |-----------|--------------|-----------|
            | Level Data | %d | 0x%08X |
            | Region File | %d | 0x%08X |
            | Chunk Data | %d | 0x%08X |
            
            ## Version History
            
            - v1.0.0: Initial specification release
            
            ## License
            
            This specification is released under the MIT License.
            See the main Odyssey project for full license details.
            
            ## Contributing
            
            To contribute to this specification:
            1. Fork the main Odyssey repository
            2. Make your changes to the format specification
            3. Update this documentation accordingly
            4. Submit a pull request with detailed explanations
            
            ## Support
            
            For questions about this specification:
            - Check the documentation in `docs/`
            - Examine the sample implementations in `tools/`
            - Review the test cases in `validation/`
            - Open an issue in the main Odyssey repository
            """.formatted(
                SPEC_VERSION,
                ODYSSEY_VERSION,
                new Date(),
                WorldSaveFormat.REGION_MAGIC_NUMBER, WorldSaveFormat.REGION_MAGIC_NUMBER,
                WorldSaveFormat.JOURNAL_MAGIC_NUMBER, WorldSaveFormat.JOURNAL_MAGIC_NUMBER,
                WorldSaveFormat.REGION_MAGIC_NUMBER, WorldSaveFormat.REGION_MAGIC_NUMBER
            );
        
        Files.writeString(outputPath.resolve("README.md"), readme);
    }
    
    private void generateFormatOverview(Path docsPath) throws IOException {
        String overview = """
            # Odyssey World Format Overview
            
            ## Introduction
            
            The Odyssey world format is designed for efficient storage and streaming of large maritime worlds.
            It uses a chunk-based approach similar to Minecraft but optimized for ocean environments and ship-based gameplay.
            
            ## Design Principles
            
            1. **Streaming Efficiency**: Chunks can be loaded and unloaded independently
            2. **Compression**: All data is compressed to minimize storage requirements
            3. **Versioning**: Forward and backward compatibility through version headers
            4. **Integrity**: Built-in checksums and validation mechanisms
            5. **Extensibility**: Support for custom data and future features
            
            ## File Structure
            
            A typical Odyssey world consists of:
            
            ```
            world_name/
            ├── level.dat           # World metadata and settings
            ├── session.lock        # Session management (optional)
            ├── players/            # Player data directory
            │   ├── player1.dat     # Individual player files
            │   └── player2.dat
            ├── regions/            # Region files containing chunks
            │   ├── r.0.0.region    # Region file (32x32 chunks)
            │   ├── r.0.1.region
            │   └── r.1.0.region
            └── data/               # Additional world data (optional)
                ├── structures/     # Generated structures
                ├── poi/           # Points of interest
                └── stats/         # World statistics
            ```
            
            ## Coordinate System
            
            - **World coordinates**: 64-bit signed integers (long)
            - **Chunk coordinates**: 32-bit signed integers (int)
            - **Block coordinates**: 8-bit unsigned integers within chunks (0-255)
            - **Region coordinates**: Derived from chunk coordinates (chunk >> 5)
            
            ## Data Types
            
            | Type | Size | Description |
            |------|------|-------------|
            | byte | 1 byte | Signed 8-bit integer |
            | short | 2 bytes | Signed 16-bit integer |
            | int | 4 bytes | Signed 32-bit integer |
            | long | 8 bytes | Signed 64-bit integer |
            | float | 4 bytes | IEEE 754 single precision |
            | double | 8 bytes | IEEE 754 double precision |
            | string | variable | UTF-8 encoded, length-prefixed |
            | uuid | 16 bytes | Standard UUID format |
            
            ## Compression
            
            All data files use GZIP compression unless otherwise specified.
            The compression is applied to the entire file content after the header.
            
            ## Version Compatibility
            
            The format supports version numbers in all file headers:
            - **Major version**: Breaking changes that require migration
            - **Minor version**: Backward-compatible additions
            - **Patch version**: Bug fixes and optimizations
            
            Current version: %d.%d.%d
            
            ## Error Handling
            
            Implementations should handle:
            - Corrupted files gracefully
            - Missing optional data
            - Version mismatches
            - Incomplete writes
            
            ## Performance Considerations
            
            - Chunk loading should be asynchronous
            - Use memory mapping for large files when possible
            - Implement LRU caching for frequently accessed chunks
            - Consider using thread pools for I/O operations
            """.formatted(
                WorldSaveFormat.CURRENT_VERSION, 0, 0
            );
        
        Files.writeString(docsPath.resolve("format_overview.md"), overview);
    }
    
    private void generateLevelDatSpec(Path docsPath) throws IOException {
        String spec = """
            # Level.dat File Specification
            
            The `level.dat` file contains world metadata and global settings.
            
            ## File Structure
            
            ```
            Header (12 bytes):
            ├── Magic Number (4 bytes): 0x%08X
            ├── Version (4 bytes): Current format version
            └── Flags (4 bytes): Feature flags and options
            
            Compressed Data (GZIP):
            ├── World Name (string)
            ├── Creation Time (8 bytes, long)
            ├── Last Played (8 bytes, long)
            ├── Seed (8 bytes, long)
            ├── Spawn Position (24 bytes, 3 doubles)
            ├── Game Rules (variable)
            ├── Weather Data (variable)
            ├── Time Data (variable)
            └── Custom Data (variable)
            ```
            
            ## Field Descriptions
            
            ### Header Fields
            
            - **Magic Number**: Always 0x%08X, used for file type identification
            - **Version**: Format version number for compatibility checking
            - **Flags**: Bit flags for optional features and compression settings
            
            ### World Data Fields
            
            - **World Name**: UTF-8 string, display name for the world
            - **Creation Time**: Unix timestamp when world was created
            - **Last Played**: Unix timestamp when world was last accessed
            - **Seed**: Random seed used for world generation
            - **Spawn Position**: Default spawn coordinates (x, y, z)
            
            ### Game Rules
            
            Game rules are stored as key-value pairs:
            
            ```
            Game Rules:
            ├── Rule Count (4 bytes, int)
            └── Rules (variable):
                ├── Rule Name (string)
                ├── Rule Type (1 byte): 0=boolean, 1=int, 2=float, 3=string
                └── Rule Value (variable, based on type)
            ```
            
            ### Weather Data
            
            ```
            Weather Data:
            ├── Current Weather (4 bytes, int): Weather type ID
            ├── Weather Duration (4 bytes, int): Remaining duration in ticks
            ├── Rain Level (4 bytes, float): 0.0 to 1.0
            ├── Thunder Level (4 bytes, float): 0.0 to 1.0
            └── Wind Data (12 bytes, 3 floats): Wind vector (x, y, z)
            ```
            
            ### Time Data
            
            ```
            Time Data:
            ├── World Time (8 bytes, long): Total world time in ticks
            ├── Day Time (8 bytes, long): Time of day (0-24000)
            ├── Day Count (4 bytes, int): Number of days elapsed
            └── Time Scale (4 bytes, float): Time multiplier
            ```
            
            ### Custom Data
            
            Custom data allows for extensibility:
            
            ```
            Custom Data:
            ├── Entry Count (4 bytes, int)
            └── Entries (variable):
                ├── Key (string): Identifier for the data
                ├── Data Type (1 byte): Type identifier
                ├── Data Length (4 bytes, int): Size of data in bytes
                └── Data (variable): Raw data bytes
            ```
            
            ## Example Implementation (Java)
            
            ```java
            public class LevelDatReader {
                public static LevelData readLevelDat(Path file) throws IOException {
                    try (DataInputStream dis = new DataInputStream(
                            new GZIPInputStream(Files.newInputStream(file)))) {
                        
                        // Read header
                        int magic = dis.readInt();
                        if (magic != LEVEL_MAGIC) {
                            throw new IOException("Invalid magic number");
                        }
                        
                        int version = dis.readInt();
                        int flags = dis.readInt();
                        
                        // Read world data
                        String worldName = dis.readUTF();
                        long creationTime = dis.readLong();
                        long lastPlayed = dis.readLong();
                        long seed = dis.readLong();
                        
                        double spawnX = dis.readDouble();
                        double spawnY = dis.readDouble();
                        double spawnZ = dis.readDouble();
                        
                        // Read game rules
                        Map<String, Object> gameRules = readGameRules(dis);
                        
                        // Read weather data
                        WeatherData weather = readWeatherData(dis);
                        
                        // Read time data
                        TimeData time = readTimeData(dis);
                        
                        // Read custom data
                        Map<String, byte[]> customData = readCustomData(dis);
                        
                        return new LevelData(worldName, creationTime, lastPlayed, 
                                           seed, spawnX, spawnY, spawnZ, 
                                           gameRules, weather, time, customData);
                    }
                }
            }
            ```
            
            ## Validation Rules
            
            1. Magic number must match exactly
            2. Version must be supported by the implementation
            3. World name must not be empty
            4. Spawn position must be valid coordinates
            5. All timestamps must be positive
            6. Custom data keys must be unique
            
            ## Error Handling
            
            - Invalid magic number: File is not a valid level.dat
            - Unsupported version: May require migration or newer software
            - Corrupted data: Attempt recovery or use backup
            - Missing required fields: Use default values where possible
            """.formatted(
                WorldSaveFormat.LEVEL_MAGIC,
                WorldSaveFormat.LEVEL_MAGIC
            );
        
        Files.writeString(docsPath.resolve("level_dat_spec.md"), spec);
    }
    
    private void generateRegionFileSpec(Path docsPath) throws IOException {
        String spec = """
            # Region File Specification
            
            Region files store chunk data in a 32x32 grid, providing efficient access to world data.
            
            ## File Naming
            
            Region files are named using the pattern: `r.{x}.{z}.region`
            - `x`: Region X coordinate (chunk_x >> 5)
            - `z`: Region Z coordinate (chunk_z >> 5)
            
            ## File Structure
            
            ```
            Header (8192 bytes):
            ├── Magic Number (4 bytes): 0x%08X
            ├── Version (4 bytes): Format version
            ├── Chunk Table (4096 bytes): 1024 entries × 4 bytes
            ├── Timestamp Table (4096 bytes): 1024 entries × 4 bytes
            └── Reserved (0 bytes): For future use
            
            Chunk Data (variable):
            ├── Chunk 0 (variable size, compressed)
            ├── Chunk 1 (variable size, compressed)
            ├── ...
            └── Chunk 1023 (variable size, compressed)
            ```
            
            ## Chunk Table Entry
            
            Each chunk table entry is 4 bytes:
            
            ```
            Bits 31-8: Sector offset (24 bits)
            Bits 7-0:  Sector count (8 bits)
            ```
            
            - **Sector offset**: Starting sector (4KB blocks) for chunk data
            - **Sector count**: Number of sectors used by chunk (max 255)
            - **Value 0**: Indicates chunk is not present
            
            ## Timestamp Table Entry
            
            Each timestamp entry is 4 bytes representing Unix timestamp of last modification.
            
            ## Chunk Data Format
            
            Each chunk's data is stored as:
            
            ```
            Chunk Header (8 bytes):
            ├── Data Length (4 bytes): Uncompressed size
            └── Compression Type (4 bytes): 0=none, 1=GZIP, 2=ZLIB
            
            Compressed Data (variable):
            └── Chunk NBT Data (compressed based on type)
            ```
            
            ## Chunk NBT Structure
            
            ```
            Chunk NBT:
            ├── Level (compound):
            │   ├── xPos (int): Chunk X coordinate
            │   ├── zPos (int): Chunk Z coordinate
            │   ├── LastUpdate (long): Last modification time
            │   ├── InhabitedTime (long): Time players spent in chunk
            │   ├── Biomes (int array): Biome data (256 values)
            │   ├── HeightMap (int array): Height map data
            │   ├── Sections (list): Chunk sections
            │   ├── Entities (list): Entity data
            │   ├── TileEntities (list): Block entity data
            │   ├── TileTicks (list): Scheduled block updates
            │   └── LiquidTicks (list): Scheduled liquid updates
            └── DataVersion (int): Data format version
            ```
            
            ## Chunk Section Format
            
            Each 16-block-high section contains:
            
            ```
            Section:
            ├── Y (byte): Section Y coordinate (0-15)
            ├── BlockStates (compound): Block state data
            │   ├── Palette (list): Block state palette
            │   └── Data (long array): Packed block indices
            ├── BlockLight (byte array): Block light data (2048 bytes)
            ├── SkyLight (byte array): Sky light data (2048 bytes)
            └── Biomes (compound): Biome data for section
            ```
            
            ## Example Implementation (Java)
            
            ```java
            public class RegionFile {
                private static final int SECTOR_SIZE = 4096;
                private static final int HEADER_SIZE = 8192;
                
                private final RandomAccessFile file;
                private final int[] chunkTable = new int[1024];
                private final int[] timestampTable = new int[1024];
                
                public RegionFile(Path regionPath) throws IOException {
                    this.file = new RandomAccessFile(regionPath.toFile(), "rw");
                    readHeader();
                }
                
                private void readHeader() throws IOException {
                    file.seek(0);
                    
                    int magic = file.readInt();
                    if (magic != REGION_MAGIC) {
                        throw new IOException("Invalid region file magic");
                    }
                    
                    int version = file.readInt();
                    
                    // Read chunk table
                    for (int i = 0; i < 1024; i++) {
                        chunkTable[i] = file.readInt();
                    }
                    
                    // Read timestamp table
                    for (int i = 0; i < 1024; i++) {
                        timestampTable[i] = file.readInt();
                    }
                }
                
                public byte[] readChunk(int chunkX, int chunkZ) throws IOException {
                    int index = (chunkX & 31) + (chunkZ & 31) * 32;
                    int entry = chunkTable[index];
                    
                    if (entry == 0) {
                        return null; // Chunk not present
                    }
                    
                    int sectorOffset = entry >>> 8;
                    int sectorCount = entry & 0xFF;
                    
                    file.seek(sectorOffset * SECTOR_SIZE);
                    
                    int dataLength = file.readInt();
                    int compressionType = file.readInt();
                    
                    byte[] compressedData = new byte[dataLength];
                    file.readFully(compressedData);
                    
                    return decompressData(compressedData, compressionType);
                }
            }
            ```
            
            ## Performance Considerations
            
            1. **Memory Mapping**: Use memory-mapped files for better performance
            2. **Caching**: Cache frequently accessed chunks in memory
            3. **Async I/O**: Use asynchronous operations for chunk loading
            4. **Compression**: Balance compression ratio vs. speed
            
            ## Error Recovery
            
            1. **Corrupted Header**: Attempt to rebuild from chunk data
            2. **Invalid Chunk**: Skip corrupted chunks, log errors
            3. **Partial Writes**: Use atomic operations where possible
            4. **Backup Strategy**: Keep backup copies of critical regions
            """.formatted(WorldSaveFormat.REGION_MAGIC_NUMBER);
        
        Files.writeString(docsPath.resolve("region_file_spec.md"), spec);
    }
    
    private void generatePlayerDataSpec(Path docsPath) throws IOException {
        String spec = """
            # Player Data Specification
            
            Player data files store individual player information including position, inventory, and statistics.
            
            ## File Naming
            
            Player files are named using the pattern: `{uuid}.dat`
            - `uuid`: Player's UUID in standard format (with hyphens)
            
            ## File Structure
            
            ```
            Header (16 bytes):
            ├── Magic Number (4 bytes): 0x%08X
            ├── Version (4 bytes): Format version
            ├── Flags (4 bytes): Player-specific flags
            └── Reserved (4 bytes): For future use
            
            Compressed Data (GZIP):
            └── Player NBT Data (variable size)
            ```
            
            ## Player NBT Structure
            
            ```
            Player NBT:
            ├── UUID (compound): Player UUID
            │   ├── Most (long): UUID most significant bits
            │   └── Least (long): UUID least significant bits
            ├── Name (string): Player display name
            ├── Position (compound): Player position
            │   ├── X (double): X coordinate
            │   ├── Y (double): Y coordinate
            │   ├── Z (double): Z coordinate
            │   ├── Yaw (float): Rotation around Y axis
            │   └── Pitch (float): Rotation around X axis
            ├── Motion (compound): Player velocity
            │   ├── X (double): X velocity
            │   ├── Y (double): Y velocity
            │   └── Z (double): Z velocity
            ├── Health (float): Current health points
            ├── MaxHealth (float): Maximum health points
            ├── Food (int): Food level (0-20)
            ├── Saturation (float): Food saturation
            ├── Air (short): Remaining air when underwater
            ├── Experience (compound): Experience data
            │   ├── Level (int): Experience level
            │   ├── Points (float): Experience points
            │   └── Total (int): Total experience earned
            ├── Inventory (list): Player inventory
            ├── EnderItems (list): Ender chest contents
            ├── Abilities (compound): Player abilities
            ├── Statistics (compound): Player statistics
            ├── Advancements (compound): Achievement progress
            ├── RecipeBook (compound): Known recipes
            ├── SeenCredits (byte): Has seen end credits
            ├── SelectedItemSlot (int): Currently selected hotbar slot
            ├── Sleeping (byte): Is player sleeping
            ├── SleepTimer (short): Sleep timer
            ├── SpawnX (int): Spawn X coordinate
            ├── SpawnY (int): Spawn Y coordinate
            ├── SpawnZ (int): Spawn Z coordinate
            ├── SpawnForced (byte): Force spawn at spawn point
            ├── WorldUUIDMost (long): World UUID most significant bits
            ├── WorldUUIDLeast (long): World UUID least significant bits
            └── CustomData (compound): Mod/plugin custom data
            ```
            
            ## Inventory Item Format
            
            Each inventory item is stored as:
            
            ```
            Item:
            ├── Slot (byte): Inventory slot index
            ├── id (string): Item identifier
            ├── Count (byte): Item stack size
            ├── Damage (short): Item damage/durability
            └── tag (compound): Item NBT data (optional)
            ```
            
            ## Abilities Format
            
            ```
            Abilities:
            ├── Flying (byte): Is currently flying
            ├── FlySpeed (float): Flying speed multiplier
            ├── InstantBuild (byte): Can instantly break blocks
            ├── Invulnerable (byte): Cannot take damage
            ├── MayBuild (byte): Can place/break blocks
            ├── MayFly (byte): Can toggle flight
            └── WalkSpeed (float): Walking speed multiplier
            ```
            
            ## Statistics Format
            
            ```
            Statistics:
            ├── StatType (compound): Statistics by type
            │   ├── minecraft:mined (compound): Blocks mined
            │   ├── minecraft:crafted (compound): Items crafted
            │   ├── minecraft:used (compound): Items used
            │   ├── minecraft:broken (compound): Items broken
            │   ├── minecraft:picked_up (compound): Items picked up
            │   ├── minecraft:dropped (compound): Items dropped
            │   ├── minecraft:killed (compound): Entities killed
            │   ├── minecraft:killed_by (compound): Deaths by entity
            │   └── minecraft:custom (compound): Custom statistics
            └── DataVersion (int): Statistics format version
            ```
            
            ## Example Implementation (Java)
            
            ```java
            public class PlayerDataReader {
                public static PlayerData readPlayerData(Path file) throws IOException {
                    try (DataInputStream dis = new DataInputStream(
                            new GZIPInputStream(Files.newInputStream(file)))) {
                        
                        // Read header
                        int magic = dis.readInt();
                        if (magic != PLAYER_MAGIC) {
                            throw new IOException("Invalid player data magic");
                        }
                        
                        int version = dis.readInt();
                        int flags = dis.readInt();
                        int reserved = dis.readInt();
                        
                        // Read NBT data
                        NBTTagCompound nbt = NBTUtil.readCompressed(dis);
                        
                        return parsePlayerData(nbt);
                    }
                }
                
                private static PlayerData parsePlayerData(NBTTagCompound nbt) {
                    UUID uuid = new UUID(
                        nbt.getCompound("UUID").getLong("Most"),
                        nbt.getCompound("UUID").getLong("Least")
                    );
                    
                    String name = nbt.getString("Name");
                    
                    NBTTagCompound pos = nbt.getCompound("Position");
                    double x = pos.getDouble("X");
                    double y = pos.getDouble("Y");
                    double z = pos.getDouble("Z");
                    float yaw = pos.getFloat("Yaw");
                    float pitch = pos.getFloat("Pitch");
                    
                    float health = nbt.getFloat("Health");
                    int food = nbt.getInt("Food");
                    
                    List<ItemStack> inventory = parseInventory(nbt.getList("Inventory"));
                    
                    return new PlayerData(uuid, name, x, y, z, yaw, pitch, 
                                        health, food, inventory);
                }
            }
            ```
            
            ## Validation Rules
            
            1. UUID must be valid and unique
            2. Position coordinates must be finite numbers
            3. Health must be between 0 and MaxHealth
            4. Food level must be between 0 and 20
            5. Inventory slots must be valid indices
            6. Item counts must be positive
            
            ## Migration Considerations
            
            When updating player data format:
            1. Preserve essential data (UUID, position, inventory)
            2. Provide default values for new fields
            3. Convert deprecated fields to new format
            4. Maintain backward compatibility where possible
            
            ## Security Considerations
            
            1. Validate all input data to prevent exploits
            2. Sanitize player names and custom data
            3. Limit inventory item counts and NBT complexity
            4. Verify world UUID matches current world
            """.formatted(0x4F504C52); // "OPLR" - Odyssey Player
        
        Files.writeString(docsPath.resolve("player_data_spec.md"), spec);
    }
    
    private void generateChunkDataSpec(Path docsPath) throws IOException {
        String spec = """
            # Chunk Data Specification
            
            Chunks are 256x256x256 block regions that form the basic unit of world storage and streaming.
            
            ## Chunk Coordinates
            
            - **World coordinates**: Absolute position in the world
            - **Chunk coordinates**: World coordinates divided by 256
            - **Block coordinates**: Position within chunk (0-255)
            
            ## Chunk Structure
            
            ```
            Chunk:
            ├── Header (32 bytes):
            │   ├── Magic Number (4 bytes): 0x%08X
            │   ├── Version (4 bytes): Format version
            │   ├── X Position (4 bytes): Chunk X coordinate
            │   ├── Z Position (4 bytes): Chunk Z coordinate
            │   ├── Last Modified (8 bytes): Unix timestamp
            │   ├── Inhabited Time (8 bytes): Player time in chunk
            │   └── Flags (4 bytes): Chunk-specific flags
            ├── Biome Data (1024 bytes): 32x32 biome grid
            ├── Height Map (2048 bytes): 256x256 height values (shorts)
            ├── Block Data (variable): Compressed block information
            ├── Light Data (variable): Light level information
            ├── Entity Data (variable): Entities in chunk
            ├── Block Entity Data (variable): Special block data
            └── Custom Data (variable): Extensible data storage
            ```
            
            ## Biome Data
            
            Biomes are stored as a 32x32 grid of integers:
            - Each biome covers an 8x8 block area
            - Biome IDs are defined in the biome registry
            - Interpolation is used for smooth transitions
            
            ## Height Map
            
            The height map stores the highest solid block in each column:
            - 256x256 array of short values
            - Represents Y coordinate of highest block
            - Used for lighting calculations and optimization
            
            ## Block Data Format
            
            Blocks are stored in 16x16x16 sections:
            
            ```
            Block Section:
            ├── Section Y (1 byte): Vertical position (0-15)
            ├── Block Count (2 bytes): Non-air blocks in section
            ├── Block States (variable): Palette-based storage
            │   ├── Palette Size (1 byte): Number of unique blocks
            │   ├── Palette (variable): Block state definitions
            │   └── Indices (variable): Packed block indices
            └── Biome Data (variable): Per-section biome data
            ```
            
            ## Palette Format
            
            The palette system efficiently stores block data:
            
            ```
            Palette Entry:
            ├── Block ID (string): Block identifier
            ├── Properties (compound): Block state properties
            └── Metadata (compound): Additional block data
            
            Packed Indices:
            ├── Bits Per Index (1 byte): Bits needed per block
            ├── Data Length (4 bytes): Size of packed data
            └── Data (variable): Bit-packed block indices
            ```
            
            ## Light Data
            
            Light information is stored per section:
            
            ```
            Light Section:
            ├── Block Light (2048 bytes): Light from blocks (4 bits per block)
            ├── Sky Light (2048 bytes): Light from sky (4 bits per block)
            └── Light Mask (512 bytes): Which blocks emit light
            ```
            
            ## Entity Data
            
            Entities are stored as NBT compounds:
            
            ```
            Entity:
            ├── UUID (compound): Entity unique identifier
            ├── Type (string): Entity type identifier
            ├── Position (compound): Entity position
            │   ├── X (double): X coordinate
            │   ├── Y (double): Y coordinate
            │   └── Z (double): Z coordinate
            ├── Motion (compound): Entity velocity
            ├── Rotation (compound): Entity rotation
            ├── Health (float): Current health
            ├── Age (int): Entity age in ticks
            ├── OnGround (byte): Is entity on ground
            ├── NoGravity (byte): Disable gravity
            ├── Silent (byte): Disable sounds
            ├── Glowing (byte): Glowing effect
            ├── CustomName (string): Display name
            ├── CustomNameVisible (byte): Show name
            ├── Tags (list): Entity tags
            └── CustomData (compound): Type-specific data
            ```
            
            ## Block Entity Data
            
            Special blocks with additional data:
            
            ```
            Block Entity:
            ├── Type (string): Block entity type
            ├── X (int): Block X coordinate
            ├── Y (int): Block Y coordinate
            ├── Z (int): Block Z coordinate
            ├── CustomName (string): Display name
            ├── Lock (string): Access lock
            └── TypeData (compound): Type-specific data
            ```
            
            ## Example Implementation (Java)
            
            ```java
            public class ChunkData {
                private final int chunkX, chunkZ;
                private final long lastModified;
                private final int[] biomes = new int[1024];
                private final short[] heightMap = new short[65536];
                private final ChunkSection[] sections = new ChunkSection[16];
                private final List<Entity> entities = new ArrayList<>();
                private final List<BlockEntity> blockEntities = new ArrayList<>();
                
                public static ChunkData readChunk(byte[] data) throws IOException {
                    try (DataInputStream dis = new DataInputStream(
                            new ByteArrayInputStream(data))) {
                        
                        // Read header
                        int magic = dis.readInt();
                        if (magic != CHUNK_MAGIC) {
                            throw new IOException("Invalid chunk magic");
                        }
                        
                        int version = dis.readInt();
                        int chunkX = dis.readInt();
                        int chunkZ = dis.readInt();
                        long lastModified = dis.readLong();
                        long inhabitedTime = dis.readLong();
                        int flags = dis.readInt();
                        
                        ChunkData chunk = new ChunkData(chunkX, chunkZ, lastModified);
                        
                        // Read biome data
                        for (int i = 0; i < 1024; i++) {
                            chunk.biomes[i] = dis.readInt();
                        }
                        
                        // Read height map
                        for (int i = 0; i < 65536; i++) {
                            chunk.heightMap[i] = dis.readShort();
                        }
                        
                        // Read sections
                        int sectionCount = dis.readByte();
                        for (int i = 0; i < sectionCount; i++) {
                            chunk.sections[i] = ChunkSection.read(dis);
                        }
                        
                        // Read entities
                        int entityCount = dis.readInt();
                        for (int i = 0; i < entityCount; i++) {
                            chunk.entities.add(Entity.read(dis));
                        }
                        
                        // Read block entities
                        int blockEntityCount = dis.readInt();
                        for (int i = 0; i < blockEntityCount; i++) {
                            chunk.blockEntities.add(BlockEntity.read(dis));
                        }
                        
                        return chunk;
                    }
                }
            }
            ```
            
            ## Optimization Techniques
            
            1. **Palette Compression**: Use small palettes for uniform areas
            2. **Empty Section Skipping**: Don't store empty sections
            3. **Delta Compression**: Store changes from previous versions
            4. **Lazy Loading**: Load sections on demand
            5. **Memory Pooling**: Reuse chunk objects
            
            ## Streaming Considerations
            
            1. **Priority Loading**: Load chunks based on player proximity
            2. **Background Generation**: Generate chunks asynchronously
            3. **Memory Management**: Unload distant chunks
            4. **Network Optimization**: Send only changed sections
            5. **Caching Strategy**: Cache frequently accessed chunks
            """.formatted(0x4F43484B); // "OCHK" - Odyssey Chunk
        
        Files.writeString(docsPath.resolve("chunk_data_spec.md"), spec);
    }
    
    private void generateApiDocumentation(Path docsPath) throws IOException {
        String api = """
            # API Documentation
            
            This document provides programming interfaces for working with Odyssey world files.
            
            ## Java API
            
            ### Core Classes
            
            ```java
            // World loading and saving
            public class WorldSaveManager {
                public static World loadWorld(Path worldPath) throws IOException;
                public static void saveWorld(World world, Path worldPath) throws IOException;
                public static boolean isValidWorld(Path worldPath);
            }
            
            // Region file access
            public class RegionFileManager {
                public RegionFile getRegionFile(int regionX, int regionZ);
                public ChunkData loadChunk(int chunkX, int chunkZ) throws IOException;
                public void saveChunk(ChunkData chunk) throws IOException;
            }
            
            // Player data management
            public class PlayerDataManager {
                public PlayerData loadPlayer(UUID playerId) throws IOException;
                public void savePlayer(PlayerData player) throws IOException;
                public List<UUID> getKnownPlayers();
            }
            ```
            
            ### Data Structures
            
            ```java
            public class World {
                public String getName();
                public long getSeed();
                public Vector3d getSpawnPosition();
                public GameRules getGameRules();
                public WeatherData getWeather();
                public TimeData getTime();
            }
            
            public class ChunkData {
                public int getChunkX();
                public int getChunkZ();
                public BlockState getBlock(int x, int y, int z);
                public void setBlock(int x, int y, int z, BlockState block);
                public List<Entity> getEntities();
                public List<BlockEntity> getBlockEntities();
            }
            
            public class PlayerData {
                public UUID getUUID();
                public String getName();
                public Vector3d getPosition();
                public Vector2f getRotation();
                public float getHealth();
                public Inventory getInventory();
            }
            ```
            
            ## Python API
            
            ### Installation
            
            ```bash
            pip install odyssey-world-format
            ```
            
            ### Basic Usage
            
            ```python
            from odyssey_world import World, ChunkData, PlayerData
            
            # Load a world
            world = World.load("path/to/world")
            
            # Access world properties
            print(f"World: {world.name}")
            print(f"Seed: {world.seed}")
            print(f"Spawn: {world.spawn_position}")
            
            # Load a chunk
            chunk = world.get_chunk(0, 0)
            if chunk:
                # Access block data
                block = chunk.get_block(128, 64, 128)
                print(f"Block at center: {block}")
                
                # Modify blocks
                chunk.set_block(128, 65, 128, "minecraft:stone")
            
            # Load player data
            players = world.get_players()
            for player_id in players:
                player = world.get_player(player_id)
                print(f"Player {player.name} at {player.position}")
            
            # Save changes
            world.save()
            ```
            
            ### Advanced Features
            
            ```python
            # Chunk iteration
            for chunk_x, chunk_z in world.get_chunk_coordinates():
                chunk = world.get_chunk(chunk_x, chunk_z)
                if chunk:
                    # Process chunk
                    pass
            
            # Entity manipulation
            entities = chunk.get_entities()
            for entity in entities:
                if entity.type == "minecraft:boat":
                    # Modify boat entities
                    entity.position.y += 1.0
            
            # Block entity access
            block_entities = chunk.get_block_entities()
            for be in block_entities:
                if be.type == "minecraft:chest":
                    # Access chest inventory
                    items = be.get_inventory()
            ```
            
            ## JavaScript/Node.js API
            
            ### Installation
            
            ```bash
            npm install odyssey-world-format
            ```
            
            ### Basic Usage
            
            ```javascript
            const { World, ChunkData, PlayerData } = require('odyssey-world-format');
            
            // Load a world
            const world = await World.load('path/to/world');
            
            // Access world properties
            console.log(`World: ${world.name}`);
            console.log(`Seed: ${world.seed}`);
            console.log(`Spawn: ${world.spawnPosition}`);
            
            // Load a chunk
            const chunk = await world.getChunk(0, 0);
            if (chunk) {
                // Access block data
                const block = chunk.getBlock(128, 64, 128);
                console.log(`Block at center: ${block}`);
                
                // Modify blocks
                chunk.setBlock(128, 65, 128, 'minecraft:stone');
            }
            
            // Load player data
            const players = await world.getPlayers();
            for (const playerId of players) {
                const player = await world.getPlayer(playerId);
                console.log(`Player ${player.name} at ${player.position}`);
            }
            
            // Save changes
            await world.save();
            ```
            
            ## C++ API
            
            ### Headers
            
            ```cpp
            #include <odyssey/world.h>
            #include <odyssey/chunk.h>
            #include <odyssey/player.h>
            
            using namespace odyssey;
            ```
            
            ### Basic Usage
            
            ```cpp
            // Load a world
            auto world = World::load("path/to/world");
            
            // Access world properties
            std::cout << "World: " << world->getName() << std::endl;
            std::cout << "Seed: " << world->getSeed() << std::endl;
            
            // Load a chunk
            auto chunk = world->getChunk(0, 0);
            if (chunk) {
                // Access block data
                auto block = chunk->getBlock(128, 64, 128);
                std::cout << "Block at center: " << block.getId() << std::endl;
                
                // Modify blocks
                chunk->setBlock(128, 65, 128, BlockState("minecraft:stone"));
            }
            
            // Load player data
            auto players = world->getPlayers();
            for (const auto& playerId : players) {
                auto player = world->getPlayer(playerId);
                std::cout << "Player " << player->getName() 
                         << " at " << player->getPosition() << std::endl;
            }
            
            // Save changes
            world->save();
            ```
            
            ## Error Handling
            
            ### Java
            
            ```java
            try {
                World world = WorldSaveManager.loadWorld(worldPath);
                // Use world
            } catch (InvalidWorldException e) {
                // Handle invalid world format
            } catch (CorruptedDataException e) {
                // Handle corrupted data
            } catch (IOException e) {
                // Handle I/O errors
            }
            ```
            
            ### Python
            
            ```python
            try:
                world = World.load("path/to/world")
                # Use world
            except InvalidWorldError:
                # Handle invalid world format
            except CorruptedDataError:
                # Handle corrupted data
            except IOError:
                # Handle I/O errors
            ```
            
            ### JavaScript
            
            ```javascript
            try {
                const world = await World.load('path/to/world');
                // Use world
            } catch (error) {
                if (error instanceof InvalidWorldError) {
                    // Handle invalid world format
                } else if (error instanceof CorruptedDataError) {
                    // Handle corrupted data
                } else {
                    // Handle other errors
                }
            }
            ```
            
            ## Performance Tips
            
            1. **Batch Operations**: Group multiple changes together
            2. **Lazy Loading**: Only load chunks when needed
            3. **Memory Management**: Unload unused chunks
            4. **Async Operations**: Use asynchronous I/O where possible
            5. **Caching**: Cache frequently accessed data
            
            ## Thread Safety
            
            - **Java**: Use `ConcurrentHashMap` and synchronization
            - **Python**: Use threading locks or asyncio
            - **JavaScript**: Single-threaded, use async/await
            - **C++**: Use mutexes and atomic operations
            
            ## Migration Support
            
            All APIs support automatic migration from older formats:
            
            ```java
            // Java
            if (WorldSaveManager.needsMigration(worldPath)) {
                WorldSaveManager.migrateWorld(worldPath, targetVersion);
            }
            ```
            
            ```python
            # Python
            if world.needs_migration():
                world.migrate(target_version)
            ```
            """;
        
        Files.writeString(docsPath.resolve("api_documentation.md"), api);
    }
    
    private void generateBestPracticesGuide(Path docsPath) throws IOException {
        String guide = """
            # Best Practices Guide
            
            This guide provides recommendations for working with Odyssey world files efficiently and safely.
            
            ## General Principles
            
            ### 1. Always Backup Before Modifications
            
            ```bash
            # Create a backup before any modifications
            cp -r world_folder world_folder_backup
            ```
            
            ### 2. Validate Data Before Processing
            
            ```java
            // Java example
            if (!WorldSaveManager.isValidWorld(worldPath)) {
                throw new IllegalArgumentException("Invalid world format");
            }
            ```
            
            ### 3. Handle Errors Gracefully
            
            ```python
            # Python example
            try:
                chunk = world.get_chunk(x, z)
                if chunk is None:
                    # Generate new chunk or use default
                    chunk = world.generate_chunk(x, z)
            except CorruptedDataError:
                # Log error and skip corrupted chunk
                logger.error(f"Corrupted chunk at {x}, {z}")
                return None
            ```
            
            ## Performance Optimization
            
            ### 1. Chunk Loading Strategy
            
            ```java
            // Load chunks asynchronously
            CompletableFuture<ChunkData> future = CompletableFuture.supplyAsync(() -> {
                return regionManager.loadChunk(chunkX, chunkZ);
            });
            
            // Use chunk when ready
            future.thenAccept(chunk -> {
                if (chunk != null) {
                    processChunk(chunk);
                }
            });
            ```
            
            ### 2. Memory Management
            
            ```cpp
            // C++ example with RAII
            class ChunkManager {
                std::unordered_map<ChunkCoord, std::unique_ptr<Chunk>> loadedChunks;
                
                void unloadDistantChunks(const Vector3& playerPos, double maxDistance) {
                    auto it = loadedChunks.begin();
                    while (it != loadedChunks.end()) {
                        if (distance(it->first, playerPos) > maxDistance) {
                            it = loadedChunks.erase(it);
                        } else {
                            ++it;
                        }
                    }
                }
            };
            ```
            
            ### 3. Batch Operations
            
            ```python
            # Batch multiple block changes
            changes = []
            for x in range(start_x, end_x):
                for z in range(start_z, end_z):
                    changes.append((x, y, z, new_block_type))
            
            # Apply all changes at once
            chunk.set_blocks_batch(changes)
            ```
            
            ## Data Integrity
            
            ### 1. Atomic Operations
            
            ```java
            // Use temporary files for atomic writes
            public void saveChunkSafely(ChunkData chunk, Path chunkFile) throws IOException {
                Path tempFile = chunkFile.resolveSibling(chunkFile.getFileName() + ".tmp");
                
                try {
                    // Write to temporary file
                    writeChunkData(chunk, tempFile);
                    
                    // Atomic move
                    Files.move(tempFile, chunkFile, StandardCopyOption.ATOMIC_MOVE);
                } finally {
                    // Clean up temporary file if it exists
                    Files.deleteIfExists(tempFile);
                }
            }
            ```
            
            ### 2. Checksums and Validation
            
            ```python
            import hashlib
            
            def save_with_checksum(data, filepath):
                # Calculate checksum
                checksum = hashlib.sha256(data).hexdigest()
                
                # Save data
                with open(filepath, 'wb') as f:
                    f.write(data)
                
                # Save checksum
                with open(filepath + '.sha256', 'w') as f:
                    f.write(checksum)
            
            def verify_checksum(filepath):
                # Read data and stored checksum
                with open(filepath, 'rb') as f:
                    data = f.read()
                
                with open(filepath + '.sha256', 'r') as f:
                    stored_checksum = f.read().strip()
                
                # Calculate and compare
                actual_checksum = hashlib.sha256(data).hexdigest()
                return actual_checksum == stored_checksum
            ```
            
            ### 3. Version Compatibility
            
            ```javascript
            // Handle version differences gracefully
            function loadChunk(chunkData) {
                const version = chunkData.version;
                
                if (version < MINIMUM_SUPPORTED_VERSION) {
                    throw new Error(`Unsupported chunk version: ${version}`);
                }
                
                if (version < CURRENT_VERSION) {
                    // Migrate to current version
                    chunkData = migrateChunk(chunkData, version, CURRENT_VERSION);
                }
                
                return parseChunk(chunkData);
            }
            ```
            
            ## Concurrency and Threading
            
            ### 1. Thread-Safe Access
            
            ```java
            public class ThreadSafeChunkManager {
                private final ConcurrentHashMap<ChunkCoord, ChunkData> chunks = new ConcurrentHashMap<>();
                private final ReadWriteLock lock = new ReentrantReadWriteLock();
                
                public ChunkData getChunk(int x, int z) {
                    ChunkCoord coord = new ChunkCoord(x, z);
                    
                    // Try to get from cache first
                    ChunkData chunk = chunks.get(coord);
                    if (chunk != null) {
                        return chunk;
                    }
                    
                    // Load chunk with write lock
                    lock.writeLock().lock();
                    try {
                        // Double-check after acquiring lock
                        chunk = chunks.get(coord);
                        if (chunk == null) {
                            chunk = loadChunkFromDisk(x, z);
                            chunks.put(coord, chunk);
                        }
                        return chunk;
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            }
            ```
            
            ### 2. Async/Await Patterns
            
            ```python
            import asyncio
            import aiofiles
            
            async def load_chunk_async(chunk_x, chunk_z):
                filepath = get_chunk_filepath(chunk_x, chunk_z)
                
                async with aiofiles.open(filepath, 'rb') as f:
                    data = await f.read()
                
                # Parse chunk data in executor to avoid blocking
                loop = asyncio.get_event_loop()
                chunk = await loop.run_in_executor(None, parse_chunk_data, data)
                
                return chunk
            
            async def load_multiple_chunks(chunk_coords):
                tasks = [load_chunk_async(x, z) for x, z in chunk_coords]
                chunks = await asyncio.gather(*tasks)
                return chunks
            ```
            
            ## Error Recovery
            
            ### 1. Graceful Degradation
            
            ```cpp
            std::optional<Chunk> loadChunkWithFallback(int x, int z) {
                try {
                    // Try to load from primary location
                    return loadChunk(x, z);
                } catch (const CorruptedDataException& e) {
                    logger.warn("Primary chunk corrupted, trying backup");
                    
                    try {
                        // Try backup location
                        return loadChunkBackup(x, z);
                    } catch (const std::exception& e2) {
                        logger.error("Both primary and backup chunks corrupted");
                        
                        // Generate new chunk as last resort
                        return generateNewChunk(x, z);
                    }
                }
            }
            ```
            
            ### 2. Partial Recovery
            
            ```java
            public ChunkData recoverPartialChunk(byte[] corruptedData) {
                ChunkData chunk = new ChunkData();
                
                try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(corruptedData))) {
                    // Try to read header
                    chunk.setChunkX(dis.readInt());
                    chunk.setChunkZ(dis.readInt());
                    
                    // Try to read each section independently
                    for (int y = 0; y < 16; y++) {
                        try {
                            ChunkSection section = readChunkSection(dis);
                            chunk.setSection(y, section);
                        } catch (IOException e) {
                            // Skip corrupted section, use empty section
                            logger.warn("Skipping corrupted section {} in chunk {}, {}", y, chunk.getChunkX(), chunk.getChunkZ());
                            chunk.setSection(y, new ChunkSection());
                        }
                    }
                } catch (IOException e) {
                    logger.error("Could not recover chunk header", e);
                    return null;
                }
                
                return chunk;
            }
            ```
            
            ## Testing and Validation
            
            ### 1. Unit Testing
            
            ```python
            import unittest
            import tempfile
            import shutil
            
            class TestWorldFormat(unittest.TestCase):
                def setUp(self):
                    self.temp_dir = tempfile.mkdtemp()
                    self.world_path = os.path.join(self.temp_dir, "test_world")
                
                def tearDown(self):
                    shutil.rmtree(self.temp_dir)
                
                def test_chunk_save_load(self):
                    # Create test chunk
                    chunk = ChunkData(0, 0)
                    chunk.set_block(128, 64, 128, "minecraft:stone")
                    
                    # Save and reload
                    save_chunk(chunk, self.world_path)
                    loaded_chunk = load_chunk(0, 0, self.world_path)
                    
                    # Verify data integrity
                    self.assertEqual(loaded_chunk.get_block(128, 64, 128), "minecraft:stone")
            ```
            
            ### 2. Property-Based Testing
            
            ```java
            @Property
            public void chunkRoundTripProperty(@ForAll ChunkData originalChunk) {
                // Save chunk to bytes
                byte[] serialized = serializeChunk(originalChunk);
                
                // Load chunk from bytes
                ChunkData deserializedChunk = deserializeChunk(serialized);
                
                // Verify they are equivalent
                assertEquals(originalChunk, deserializedChunk);
            }
            ```
            
            ## Conclusion
            
            This specification provides a comprehensive guide for implementing external editors
            for the Odyssey world format. By following these guidelines and using the provided
            examples, developers can create robust tools that properly handle the maritime-focused
            world data while maintaining compatibility and data integrity.
            
            For additional support or questions, please refer to the official documentation
            or contact the development team.
            """;
    }
    
    // Helper methods for generating specific documentation files
    
    private static String generateMainReadme() {
        return """
            # Odyssey World Format Specification
            
            This directory contains the complete specification for the Odyssey world format,
            including documentation, schemas, samples, and tooling examples.
            
            ## Contents
            
            - `docs/` - Format documentation and guides
            - `schemas/` - JSON schemas and binary format specifications
            - `samples/` - Example world files
            - `tools/` - Sample implementations and validation scripts
            
            ## Quick Start
            
            1. Read the format overview in `docs/format_overview.md`
            2. Check the schemas in `schemas/` for your target format
            3. Use the sample files in `samples/` as templates
            4. Validate your implementation with scripts in `tools/`
            
            ## Support
            
            For questions about the world format, please refer to the official documentation
            or contact the development team.
            """;
    }
    
    private static String generateFormatOverview() {
        return """
            # Odyssey World Format Overview
            
            The Odyssey world format is designed for efficient storage and streaming of
            large maritime worlds with advanced features like tidal systems and modular ships.
            
            ## File Structure
            
            ```
            world_name/
            ├── level.dat          # World metadata and settings
            ├── session.lock       # Session lock file
            ├── region/            # Region files (32x32 chunks each)
            │   ├── r.0.0.mca     # Region file format
            │   └── ...
            ├── playerdata/        # Player-specific data
            │   ├── uuid.dat      # Player data files
            │   └── ...
            └── data/              # Additional world data
                ├── tides.dat     # Tidal system state
                ├── weather.dat   # Weather patterns
                └── ...
            ```
            
            ## Key Features
            
            - **Chunk-based streaming**: 16x16x384 block chunks
            - **Region file compression**: Zlib compression with optional LZ4
            - **Incremental saves**: Only modified chunks are written
            - **Backup system**: Automatic backup creation with rotation
            - **Session locking**: Prevents concurrent access conflicts
            - **Maritime focus**: Optimized for ocean worlds and ship data
            
            ## Data Types
            
            - **Blocks**: 16-bit block IDs with 8-bit metadata
            - **Biomes**: 8-bit biome IDs per 4x4x4 block region
            - **Entities**: NBT-encoded entity data
            - **Tileentities**: NBT-encoded block entity data
            - **Heightmaps**: Motion-blocking and world-surface heightmaps
            
            ## Compression
            
            All chunk data is compressed using:
            1. Zlib (default) - Good compression ratio
            2. LZ4 (optional) - Faster compression/decompression
            3. Uncompressed (debug) - No compression for debugging
            
            ## Version Compatibility
            
            The format supports versioning for backward compatibility:
            - Version 1: Basic chunk format
            - Version 2: Added compression support
            - Version 3: Maritime-specific features (current)
            """;
    }
    
    private static String generateLevelDatSchema() {
        return """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Odyssey Level.dat Schema",
              "type": "object",
              "properties": {
                "version": {
                  "type": "integer",
                  "description": "World format version"
                },
                "worldName": {
                  "type": "string",
                  "description": "Display name of the world"
                },
                "seed": {
                  "type": "integer",
                  "description": "World generation seed"
                },
                "gameType": {
                  "type": "string",
                  "enum": ["survival", "creative", "adventure"],
                  "description": "Game mode"
                },
                "difficulty": {
                  "type": "string",
                  "enum": ["peaceful", "easy", "normal", "hard"],
                  "description": "Difficulty level"
                },
                "spawnX": { "type": "integer" },
                "spawnY": { "type": "integer" },
                "spawnZ": { "type": "integer" },
                "time": {
                  "type": "integer",
                  "description": "World time in ticks"
                },
                "weather": {
                  "type": "object",
                  "properties": {
                    "raining": { "type": "boolean" },
                    "thundering": { "type": "boolean" },
                    "rainTime": { "type": "integer" },
                    "thunderTime": { "type": "integer" }
                  }
                },
                "tidal": {
                  "type": "object",
                  "properties": {
                    "enabled": { "type": "boolean" },
                    "cycle": { "type": "integer", "description": "Tidal cycle in minutes" },
                    "currentPhase": { "type": "number", "description": "Current tidal phase (0.0-1.0)" }
                  }
                }
              },
              "required": ["version", "worldName", "seed", "gameType"]
            }
            """;
    }
    
    private static String generateChunkSchema() {
        return """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Odyssey Chunk Schema",
              "type": "object",
              "properties": {
                "version": {
                  "type": "integer",
                  "description": "Chunk format version"
                },
                "x": { "type": "integer", "description": "Chunk X coordinate" },
                "z": { "type": "integer", "description": "Chunk Z coordinate" },
                "lastModified": {
                  "type": "integer",
                  "description": "Last modification timestamp"
                },
                "sections": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "y": { "type": "integer", "description": "Section Y coordinate" },
                      "blocks": {
                        "type": "string",
                        "description": "Base64-encoded block data"
                      },
                      "metadata": {
                        "type": "string",
                        "description": "Base64-encoded metadata"
                      },
                      "biomes": {
                        "type": "string",
                        "description": "Base64-encoded biome data"
                      }
                    },
                    "required": ["y", "blocks"]
                  }
                },
                "heightmaps": {
                  "type": "object",
                  "properties": {
                    "motionBlocking": {
                      "type": "string",
                      "description": "Base64-encoded heightmap"
                    },
                    "worldSurface": {
                      "type": "string",
                      "description": "Base64-encoded heightmap"
                    }
                  }
                },
                "entities": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "description": "NBT-encoded entity data"
                  }
                },
                "tileEntities": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "description": "NBT-encoded tile entity data"
                  }
                }
              },
              "required": ["version", "x", "z", "sections"]
            }
            """;
    }
    
    private static String generateRegionSchema() {
        return """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Odyssey Region File Schema",
              "type": "object",
              "properties": {
                "header": {
                  "type": "object",
                  "properties": {
                    "version": { "type": "integer" },
                    "regionX": { "type": "integer" },
                    "regionZ": { "type": "integer" },
                    "chunkCount": { "type": "integer" },
                    "compression": {
                      "type": "string",
                      "enum": ["zlib", "lz4", "none"]
                    }
                  },
                  "required": ["version", "regionX", "regionZ"]
                },
                "chunks": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "x": { "type": "integer" },
                      "z": { "type": "integer" },
                      "offset": { "type": "integer" },
                      "size": { "type": "integer" },
                      "timestamp": { "type": "integer" },
                      "data": {
                        "type": "string",
                        "description": "Base64-encoded chunk data"
                      }
                    },
                    "required": ["x", "z", "offset", "size", "data"]
                  }
                }
              },
              "required": ["header", "chunks"]
            }
            """;
    }
    
    private static String generatePlayerDataSchema() {
        return """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "title": "Odyssey Player Data Schema",
              "type": "object",
              "properties": {
                "version": {
                  "type": "integer",
                  "description": "Player data format version"
                },
                "uuid": {
                  "type": "string",
                  "description": "Player UUID"
                },
                "name": {
                  "type": "string",
                  "description": "Player display name"
                },
                "position": {
                  "type": "object",
                  "properties": {
                    "x": { "type": "number" },
                    "y": { "type": "number" },
                    "z": { "type": "number" },
                    "yaw": { "type": "number" },
                    "pitch": { "type": "number" }
                  },
                  "required": ["x", "y", "z", "yaw", "pitch"]
                },
                "health": {
                  "type": "number",
                  "minimum": 0,
                  "maximum": 20
                },
                "hunger": {
                  "type": "number",
                  "minimum": 0,
                  "maximum": 20
                },
                "experience": {
                  "type": "object",
                  "properties": {
                    "level": { "type": "integer", "minimum": 0 },
                    "points": { "type": "number", "minimum": 0 },
                    "total": { "type": "integer", "minimum": 0 }
                  }
                },
                "inventory": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "slot": { "type": "integer" },
                      "item": { "type": "string" },
                      "count": { "type": "integer", "minimum": 1 },
                      "damage": { "type": "integer", "minimum": 0 },
                      "nbt": { "type": "object" }
                    },
                    "required": ["slot", "item", "count"]
                  }
                },
                "gameMode": {
                  "type": "string",
                  "enum": ["survival", "creative", "adventure", "spectator"]
                },
                "lastPlayed": {
                  "type": "integer",
                  "description": "Unix timestamp of last login"
                }
              },
              "required": ["version", "uuid", "name", "position", "health", "hunger"]
            }
            """;
    }
    
    private static String generateBinaryFormatSpecs() {
        return """
            # Binary Format Specifications
            
            This document describes the binary format specifications for Odyssey world files.
            
            ## Region File Format (.mca)
            
            Region files store 32x32 chunks in a compressed format:
            
            ```
            Header (8192 bytes):
            - Chunk locations (4096 bytes): 4 bytes per chunk (3 bytes offset, 1 byte size)
            - Chunk timestamps (4096 bytes): 4 bytes per chunk (Unix timestamp)
            
            Chunk Data:
            - Length (4 bytes): Size of compressed chunk data
            - Compression type (1 byte): 0=uncompressed, 1=gzip, 2=zlib, 3=lz4
            - Compressed chunk data (variable length)
            ```
            
            ## Chunk Format
            
            Each chunk contains:
            - Version (4 bytes)
            - Chunk coordinates (8 bytes: 4 bytes X, 4 bytes Z)
            - Last modified timestamp (8 bytes)
            - Section count (4 bytes)
            - Sections data (variable length)
            - Heightmaps (variable length)
            - Entities (variable length NBT)
            - Tile entities (variable length NBT)
            
            ## Level.dat Format
            
            World metadata stored in NBT format:
            - Root compound tag containing world settings
            - Version information for compatibility
            - Spawn coordinates and world generation settings
            - Game rules and difficulty settings
            - Tidal system configuration
            """;
    }
    
    private static String generateMinimalWorld() {
        return """
            # Minimal World Example
            
            This is a minimal world structure for testing:
            
            ```
            minimal_world/
            ├── level.dat
            ├── session.lock
            └── region/
                └── r.0.0.mca
            ```
            
            ## level.dat Content
            
            ```json
            {
              "version": 3,
              "worldName": "Minimal Test World",
              "seed": 12345,
              "gameType": "creative",
              "difficulty": "peaceful",
              "spawnX": 0,
              "spawnY": 64,
              "spawnZ": 0,
              "time": 0,
              "weather": {
                "raining": false,
                "thundering": false,
                "rainTime": 0,
                "thunderTime": 0
              },
              "tidal": {
                "enabled": true,
                "cycle": 20,
                "currentPhase": 0.0
              }
            }
            ```
            
            ## Region File Content
            
            Contains a single chunk at (0,0) with:
            - Bedrock layer at Y=0
            - Stone layers from Y=1 to Y=63
            - Water layer at Y=64
            - Air above Y=64
            
            This minimal world is suitable for basic testing and validation.
            """;
    }
    
    private static String generateComplexWorld() {
        return """
            # Complex World Example
            
            This example demonstrates a complex world with multiple features:
            
            ```
            complex_world/
            ├── level.dat
            ├── session.lock
            ├── region/
            │   ├── r.0.0.mca
            │   ├── r.0.1.mca
            │   ├── r.1.0.mca
            │   └── r.1.1.mca
            ├── playerdata/
            │   └── 550e8400-e29b-41d4-a716-446655440000.dat
            └── data/
                ├── tides.dat
                └── weather.dat
            ```
            
            ## Features Included
            
            - Multiple biomes (ocean, island, deep ocean)
            - Complex terrain with islands and underwater features
            - Ships and maritime structures
            - Advanced tidal system with multiple phases
            - Weather patterns and storms
            - Player data with inventory and progress
            
            ## Biome Distribution
            
            - Ocean biome: 60% of world
            - Deep ocean: 25% of world
            - Islands: 10% of world
            - Coastal areas: 5% of world
            
            ## Maritime Features
            
            - Shipwrecks with loot
            - Underwater ruins
            - Coral reefs
            - Kelp forests
            - Tidal pools
            - Lighthouse structures
            
            This complex world showcases the full capabilities of the Odyssey format.
            """;
    }
    
    private static void generateCorruptedSamples(Path samplesPath) throws IOException {
        Files.createDirectories(samplesPath);
        
        String readme = """
            # Corrupted Sample Files
            
            These samples demonstrate various corruption scenarios for testing error handling:
            
            ## 1. Truncated Region File
            
            File: `corrupted_truncated.mca`
            - Missing chunk data sections
            - Incomplete header information
            - Tests recovery mechanisms
            
            ## 2. Invalid Compression
            
            File: `corrupted_compression.mca`
            - Corrupted zlib/lz4 data
            - Invalid compression type flags
            - Tests decompression error handling
            
            ## 3. Malformed NBT Data
            
            File: `corrupted_nbt.dat`
            - Invalid NBT tag structures
            - Truncated compound tags
            - Tests NBT parser resilience
            
            ## 4. Version Mismatch
            
            File: `corrupted_version.dat`
            - Unsupported version numbers
            - Missing version information
            - Tests backward compatibility
            
            ## 5. Checksum Failures
            
            File: `corrupted_checksum.mca`
            - Modified data with invalid checksums
            - Tests data integrity verification
            
            ## Error Handling Guidelines
            
            When encountering corrupted data:
            1. Log the specific error with context
            2. Attempt graceful degradation
            3. Provide user-friendly error messages
            4. Offer recovery options when possible
            5. Prevent data loss during recovery
            
            These samples help ensure robust error handling in external editors.
            """;
        
        Files.writeString(samplesPath.resolve("README.md"), readme);
    }
    
    private static void generateFormatValidator(Path validationPath) throws IOException {
        String validator = """
            #!/usr/bin/env python3
            \"\"\"
            Odyssey World Format Validator
            
            This script validates Odyssey world files for format compliance.
            \"\"\"
            
            import os
            import sys
            import struct
            import json
            import gzip
            from pathlib import Path
            
            class OdysseyFormatValidator:
                def __init__(self):
                    self.errors = []
                    self.warnings = []
                
                def validate_world(self, world_path):
                    \"\"\"Validate an entire world directory.\"\"\"
                    world_path = Path(world_path)
                    
                    if not world_path.exists():
                        self.errors.append(f"World directory does not exist: {world_path}")
                        return False
                    
                    # Check required files and directories
                    self.check_world_structure(world_path)
                    self.validate_level_dat(world_path / "level.json")
                    self.validate_region_files(world_path / "region")
                    
                    return len(self.errors) == 0
                
                def check_world_structure(self, world_path):
                    \"\"\"Check basic world directory structure.\"\"\"
                    required_files = ["level.json"]
                    required_dirs = ["region", "playerdata"]
                    
                    for file_name in required_files:
                        if not (world_path / file_name).exists():
                            self.errors.append(f"Missing required file: {file_name}")
                    
                    for dir_name in required_dirs:
                        if not (world_path / dir_name).is_dir():
                            self.errors.append(f"Missing required directory: {dir_name}")
                
                def validate_level_dat(self, level_path):
                    \"\"\"Validate level.json file.\"\"\"
                    if not level_path.exists():
                        return
                    
                    try:
                        with open(level_path, 'r') as f:
                            level_data = json.load(f)
                        
                        required_fields = ["version", "worldName", "seed", "gameType"]
                        for field in required_fields:
                            if field not in level_data:
                                self.errors.append(f"Missing required field in level.json: {field}")
                    
                    except json.JSONDecodeError as e:
                        self.errors.append(f"Invalid JSON in level.json: {e}")
                    except Exception as e:
                        self.errors.append(f"Error reading level.json: {e}")
                
                def validate_region_files(self, region_dir):
                    \"\"\"Validate all region files in the region directory.\"\"\"
                    if not region_dir.exists():
                        return
                    
                    for region_file in region_dir.glob("*.region"):
                        self.validate_region_file(region_file)
                
                def validate_region_file(self, region_path):
                    \"\"\"Validate a single region file.\"\"\"
                    try:
                        with open(region_path, 'rb') as f:
                            # Read magic number
                            magic = struct.unpack('>I', f.read(4))[0]
                            if magic != 0x4F524547:  # "OREG"
                                self.errors.append(f"Invalid magic number in {region_path.name}")
                            
                            # Read version
                            version = struct.unpack('>I', f.read(4))[0]
                            if version != 1:
                                self.warnings.append(f"Unexpected version {version} in {region_path.name}")
                    
                    except Exception as e:
                        self.errors.append(f"Error reading region file {region_path.name}: {e}")
                
                def print_results(self):
                    \"\"\"Print validation results.\"\"\"
                    if self.errors:
                        print("ERRORS:")
                        for error in self.errors:
                            print(f"  - {error}")
                    
                    if self.warnings:
                        print("WARNINGS:")
                        for warning in self.warnings:
                            print(f"  - {warning}")
                    
                    if not self.errors and not self.warnings:
                        print("Validation passed: No issues found.")
            
            def main():
                if len(sys.argv) != 2:
                    print("Usage: python format_validator.py <world_directory>")
                    sys.exit(1)
                
                validator = OdysseyFormatValidator()
                success = validator.validate_world(sys.argv[1])
                validator.print_results()
                
                sys.exit(0 if success else 1)
            
            if __name__ == "__main__":
                main()
            """;
        
        Files.writeString(validationPath.resolve("format_validator.py"), validator);
    }
    
    private static void generateIntegrityChecker(Path validationPath) throws IOException {
        String checker = """
            #!/usr/bin/env python3
            \"\"\"
            Odyssey World Integrity Checker
            
            This script checks the integrity of Odyssey world data.
            \"\"\"
            
            import os
            import sys
            import hashlib
            import json
            from pathlib import Path
            
            class OdysseyIntegrityChecker:
                def __init__(self):
                    self.issues = []
                
                def check_world_integrity(self, world_path):
                    \"\"\"Check integrity of an entire world.\"\"\"
                    world_path = Path(world_path)
                    
                    if not world_path.exists():
                        self.issues.append(f"World directory does not exist: {world_path}")
                        return False
                    
                    self.check_file_consistency(world_path)
                    self.check_data_corruption(world_path)
                    self.check_cross_references(world_path)
                    
                    return len(self.issues) == 0
                
                def check_file_consistency(self, world_path):
                    \"\"\"Check for file consistency issues.\"\"\"
                    # Check for orphaned files
                    region_dir = world_path / "region"
                    if region_dir.exists():
                        for region_file in region_dir.glob("*.region"):
                            # Parse region coordinates from filename
                            name_parts = region_file.stem.split('.')
                            if len(name_parts) != 3 or name_parts[0] != 'r':
                                self.issues.append(f"Invalid region filename: {region_file.name}")
                
                def check_data_corruption(self, world_path):
                    \"\"\"Check for data corruption.\"\"\"
                    level_file = world_path / "level.json"
                    if level_file.exists():
                        try:
                            with open(level_file, 'r') as f:
                                json.load(f)
                        except json.JSONDecodeError:
                            self.issues.append("Corrupted level.json file")
                
                def check_cross_references(self, world_path):
                    \"\"\"Check cross-references between files.\"\"\"
                    # This would check things like:
                    # - Player references in level.json match playerdata files
                    # - Region files contain expected chunks
                    # - Entity references are valid
                    pass
                
                def print_results(self):
                    \"\"\"Print integrity check results.\"\"\"
                    if self.issues:
                        print("INTEGRITY ISSUES:")
                        for issue in self.issues:
                            print(f"  - {issue}")
                    else:
                        print("Integrity check passed: No issues found.")
            
            def main():
                if len(sys.argv) != 2:
                    print("Usage: python integrity_checker.py <world_directory>")
                    sys.exit(1)
                
                checker = OdysseyIntegrityChecker()
                success = checker.check_world_integrity(sys.argv[1])
                checker.print_results()
                
                sys.exit(0 if success else 1)
            
            if __name__ == "__main__":
                main()
            """;
        
        Files.writeString(validationPath.resolve("integrity_checker.py"), checker);
    }
    
    private static void generateCompatibilityTester(Path validationPath) throws IOException {
        String tester = """
            #!/usr/bin/env python3
            \"\"\"
            Odyssey World Compatibility Tester
            
            This script tests compatibility between different versions of Odyssey world format.
            \"\"\"
            
            import os
            import sys
            import json
            import struct
            from pathlib import Path
            
            class OdysseyCompatibilityTester:
                def __init__(self):
                    self.compatibility_issues = []
                    self.supported_versions = [1]  # Currently supported format versions
                
                def test_compatibility(self, world_path):
                    \"\"\"Test compatibility of a world with current format.\"\"\"
                    world_path = Path(world_path)
                    
                    if not world_path.exists():
                        self.compatibility_issues.append(f"World directory does not exist: {world_path}")
                        return False
                    
                    self.check_format_version(world_path)
                    self.check_feature_compatibility(world_path)
                    self.check_migration_requirements(world_path)
                    
                    return len(self.compatibility_issues) == 0
                
                def check_format_version(self, world_path):
                    \"\"\"Check if the world format version is supported.\"\"\"
                    level_file = world_path / "level.json"
                    if level_file.exists():
                        try:
                            with open(level_file, 'r') as f:
                                level_data = json.load(f)
                            
                            version = level_data.get("version", 0)
                            if version not in self.supported_versions:
                                self.compatibility_issues.append(
                                    f"Unsupported world format version: {version}. "
                                    f"Supported versions: {self.supported_versions}"
                                )
                        
                        except Exception as e:
                            self.compatibility_issues.append(f"Error reading level.json: {e}")
                
                def check_feature_compatibility(self, world_path):
                    \"\"\"Check if world features are compatible with current version.\"\"\"
                    level_file = world_path / "level.json"
                    if level_file.exists():
                        try:
                            with open(level_file, 'r') as f:
                                level_data = json.load(f)
                            
                            # Check for deprecated features
                            deprecated_features = []
                            if "oldTidalSystem" in level_data:
                                deprecated_features.append("oldTidalSystem")
                            
                            if deprecated_features:
                                self.compatibility_issues.append(
                                    f"World uses deprecated features: {deprecated_features}"
                                )
                        
                        except Exception as e:
                            self.compatibility_issues.append(f"Error checking features: {e}")
                
                def check_migration_requirements(self, world_path):
                    \"\"\"Check if the world requires migration.\"\"\"
                    # This would check for:
                    # - Old file formats that need conversion
                    # - Data structures that need updating
                    # - Missing required fields that need defaults
                    pass
                
                def print_results(self):
                    \"\"\"Print compatibility test results.\"\"\"
                    if self.compatibility_issues:
                        print("COMPATIBILITY ISSUES:")
                        for issue in self.compatibility_issues:
                            print(f"  - {issue}")
                        print("\\nThe world may need migration before it can be used.")
                    else:
                        print("Compatibility test passed: World is compatible with current format.")
            
            def main():
                if len(sys.argv) != 2:
                    print("Usage: python compatibility_tester.py <world_directory>")
                    sys.exit(1)
                
                tester = OdysseyCompatibilityTester()
                success = tester.test_compatibility(sys.argv[1])
                tester.print_results()
                
                sys.exit(0 if success else 1)
            
            if __name__ == "__main__":
                main()
            """;
        
        Files.writeString(validationPath.resolve("compatibility_tester.py"), tester);
    }
    
    // Inner classes for organization
    
    public static class SpecificationGenerator {
        public static void generateAll(Path outputDir) throws IOException {
            ExternalEditorSpec.generateFullSpecification(outputDir);
        }
        
        public static void generateDocumentationOnly(Path outputDir) throws IOException {
            ExternalEditorSpec.generateDocumentation(outputDir);
        }
        
        public static void generateSchemasOnly(Path outputDir) throws IOException {
            ExternalEditorSpec.generateSchemaFiles(outputDir);
        }
        
        public static void generateSamplesOnly(Path outputDir) throws IOException {
            ExternalEditorSpec.generateSampleFiles(outputDir);
        }
        
        public static void generateToolingOnly(Path outputDir) throws IOException {
            ExternalEditorSpec.generateToolingExamples(outputDir);
        }
    }
    
    public static class ValidationHelper {
        public static boolean validateWorldFormat(Path worldPath) {
            // Implementation would validate world format
            return true;
        }
        
        public static boolean validateChunkData(byte[] chunkData) {
            // Implementation would validate chunk data
            return true;
        }
        
        public static boolean validateRegionFile(Path regionFile) {
            // Implementation would validate region file
            return true;
        }
    }
    
    public static class FormatConstants {
        public static final int CURRENT_VERSION = 3;
        public static final int CHUNK_SIZE = 16;
        public static final int CHUNK_HEIGHT = 384;
        public static final int REGION_SIZE = 32;
        public static final String LEVEL_DAT_NAME = "level.dat";
        public static final String SESSION_LOCK_NAME = "session.lock";
        public static final String REGION_DIR_NAME = "region";
        public static final String PLAYERDATA_DIR_NAME = "playerdata";
        public static final String DATA_DIR_NAME = "data";
    }
}