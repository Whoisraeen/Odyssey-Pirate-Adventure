package com.odyssey.world.save.datapacks;

import java.nio.file.Path;

/**
 * Represents a datapack with its metadata and properties.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class Datapack {
    private final String id;
    private final String name;
    private final String description;
    private final int packFormat;
    private final String version;
    private final Path path;
    private final DatapackType type;
    
    public Datapack(String id, String description, int packFormat, String version, Path path, DatapackType type) {
        this.id = id;
        this.name = id; // Use ID as name by default
        this.description = description;
        this.packFormat = packFormat;
        this.version = version;
        this.path = path;
        this.type = type;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getPackFormat() { return packFormat; }
    public String getVersion() { return version; }
    public Path getPath() { return path; }
    public DatapackType getType() { return type; }
    
    @Override
    public String toString() {
        return String.format("Datapack{id='%s', name='%s', version='%s', type=%s}", 
                id, name, version, type);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Datapack datapack = (Datapack) obj;
        return id.equals(datapack.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

enum DatapackType {
    FOLDER, ZIP
}