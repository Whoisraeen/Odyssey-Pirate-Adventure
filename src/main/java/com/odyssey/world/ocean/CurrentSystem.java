package com.odyssey.world.ocean;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced ocean current simulation system for The Odyssey.
 * Implements multiple current types including surface currents, deep currents,
 * tidal currents, and thermal currents for realistic ocean dynamics.
 */
public class CurrentSystem {
    private static final Logger logger = LoggerFactory.getLogger(CurrentSystem.class);
    
    // Current types and layers
    private final Map<CurrentType, List<CurrentCell>> currentLayers = new EnumMap<>(CurrentType.class);
    private final List<CurrentVortex> vortices = new ArrayList<>();
    private final List<ThermalPlume> thermalPlumes = new ArrayList<>();
    
    // Grid system for current calculation
    private static final int GRID_SIZE = 64; // 64x64 grid
    private static final float CELL_SIZE = 16.0f; // Each cell is 16x16 blocks
    private final CurrentCell[][] surfaceCurrentGrid = new CurrentCell[GRID_SIZE][GRID_SIZE];
    private final CurrentCell[][] deepCurrentGrid = new CurrentCell[GRID_SIZE][GRID_SIZE];
    
    // Environmental factors
    private Vector3f windDirection = new Vector3f(1, 0, 0);
    private float windStrength = 5.0f;
    private float waterTemperature = 20.0f; // Celsius
    private float salinity = 35.0f; // Parts per thousand
    
    // Current statistics
    private float averageCurrentSpeed = 0.5f;
    private float maximumCurrentSpeed = 3.0f;
    
    public enum CurrentType {
        SURFACE,     // Wind-driven surface currents
        DEEP,        // Thermohaline circulation
        TIDAL,       // Tidal currents
        THERMAL,     // Temperature-driven convection
        COASTAL      // Coastal upwelling/downwelling
    }
    
    public static class CurrentCell {
        public Vector2f velocity;
        public float strength;
        public float temperature;
        public float salinity;
        public float depth;
        public CurrentType type;
        
        public CurrentCell(CurrentType type) {
            this.velocity = new Vector2f();
            this.strength = 0.0f;
            this.temperature = 20.0f;
            this.salinity = 35.0f;
            this.depth = 0.0f;
            this.type = type;
        }
    }
    
    public static class CurrentVortex {
        public Vector2f center;
        public float radius;
        public float strength;
        public float rotationSpeed;
        public boolean clockwise;
        public float lifetime;
        public float maxLifetime;
        
        public CurrentVortex(Vector2f center, float radius, float strength, boolean clockwise) {
            this.center = new Vector2f(center);
            this.radius = radius;
            this.strength = strength;
            this.clockwise = clockwise;
            this.rotationSpeed = strength * 0.1f;
            this.lifetime = 0.0f;
            this.maxLifetime = 300.0f + (float)Math.random() * 600.0f; // 5-15 minutes
        }
    }
    
    public static class ThermalPlume {
        public Vector2f position;
        public float intensity;
        public float radius;
        public float temperature;
        public boolean isHot; // True for upwelling hot water, false for cold downwelling
        
        public ThermalPlume(Vector2f position, float intensity, boolean isHot) {
            this.position = new Vector2f(position);
            this.intensity = intensity;
            this.radius = 20.0f + (float)Math.random() * 30.0f;
            this.isHot = isHot;
            this.temperature = isHot ? 25.0f + intensity * 10.0f : 15.0f - intensity * 5.0f;
        }
    }
    
    public void initialize() {
        logger.info("Initializing advanced current system with {}x{} grid", GRID_SIZE, GRID_SIZE);
        
        // Initialize current layers
        for (CurrentType type : CurrentType.values()) {
            currentLayers.put(type, new ArrayList<>());
        }
        
        // Initialize grid cells
        initializeGrids();
        
        // Generate initial current patterns
        generateSurfaceCurrents();
        generateDeepCurrents();
        generateVortices(5); // Start with 5 vortices
        generateThermalPlumes(10); // Start with 10 thermal plumes
        
        logger.info("Current system initialized with {} vortices and {} thermal plumes", 
                   vortices.size(), thermalPlumes.size());
    }
    
    private void initializeGrids() {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                surfaceCurrentGrid[x][z] = new CurrentCell(CurrentType.SURFACE);
                deepCurrentGrid[x][z] = new CurrentCell(CurrentType.DEEP);
                
                // Set depths
                surfaceCurrentGrid[x][z].depth = 5.0f; // 5 blocks deep
                deepCurrentGrid[x][z].depth = 50.0f; // 50 blocks deep
            }
        }
    }
    
    private void generateSurfaceCurrents() {
        // Generate large-scale surface current patterns (similar to Gulf Stream)
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                CurrentCell cell = surfaceCurrentGrid[x][z];
                
                // Create circular current pattern
                float centerX = GRID_SIZE * 0.5f;
                float centerZ = GRID_SIZE * 0.5f;
                float dx = x - centerX;
                float dz = z - centerZ;
                float distance = (float)Math.sqrt(dx * dx + dz * dz);
                
                if (distance > 5.0f && distance < GRID_SIZE * 0.4f) {
                    // Circular flow
                    float angle = (float)Math.atan2(dz, dx) + (float)Math.PI * 0.5f;
                    float strength = (float)Math.exp(-distance * 0.1f) * 2.0f;
                    
                    cell.velocity.set(
                        (float)Math.cos(angle) * strength,
                        (float)Math.sin(angle) * strength
                    );
                    cell.strength = strength;
                }
            }
        }
    }
    
    private void generateDeepCurrents() {
        // Generate thermohaline circulation patterns
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                CurrentCell cell = deepCurrentGrid[x][z];
                
                // Create deep water mass movement
                float temperature = 15.0f + (float)Math.sin(x * 0.1f) * 3.0f;
                float salinity = 34.0f + (float)Math.cos(z * 0.1f) * 2.0f;
                
                cell.temperature = temperature;
                cell.salinity = salinity;
                
                // Current strength based on density differences
                float density = calculateWaterDensity(temperature, salinity);
                float densityGradient = (density - 1025.0f) / 10.0f; // Normalized
                
                cell.velocity.set(densityGradient * 0.5f, 0);
                cell.strength = Math.abs(densityGradient) * 0.3f;
            }
        }
    }
    
    private float calculateWaterDensity(float temperature, float salinity) {
        // Simplified seawater density calculation
        return 1000.0f + salinity * 0.7f - (temperature - 20.0f) * 0.2f;
    }
    
    private void generateVortices(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (float)Math.random() * GRID_SIZE * CELL_SIZE,
                (float)Math.random() * GRID_SIZE * CELL_SIZE
            );
            
            float radius = 50.0f + (float)Math.random() * 100.0f;
            float strength = 0.5f + (float)Math.random() * 2.0f;
            boolean clockwise = Math.random() < 0.5;
            
            vortices.add(new CurrentVortex(center, radius, strength, clockwise));
        }
    }
    
    private void generateThermalPlumes(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f position = new Vector2f(
                (float)Math.random() * GRID_SIZE * CELL_SIZE,
                (float)Math.random() * GRID_SIZE * CELL_SIZE
            );
            
            float intensity = 0.5f + (float)Math.random() * 1.5f;
            boolean isHot = Math.random() < 0.6; // 60% chance of hot plume
            
            thermalPlumes.add(new ThermalPlume(position, intensity, isHot));
        }
    }
    
    public void update(double deltaTime, Vector3f windDirection, float windStrength) {
        this.windDirection.set(windDirection);
        this.windStrength = windStrength;
        
        // Update surface currents based on wind
        updateSurfaceCurrents(deltaTime);
        
        // Update deep currents
        updateDeepCurrents(deltaTime);
        
        // Update vortices
        updateVortices(deltaTime);
        
        // Update thermal plumes
        updateThermalPlumes(deltaTime);
        
        // Apply tidal effects
        applyTidalCurrents(deltaTime);
        
        // Calculate statistics
        updateCurrentStatistics();
    }
    
    private void updateSurfaceCurrents(double deltaTime) {
        // Apply wind stress to surface currents
        Vector2f windForce = new Vector2f(windDirection.x, windDirection.z).normalize().mul(windStrength * 0.1f);
        
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                CurrentCell cell = surfaceCurrentGrid[x][z];
                
                // Apply wind stress
                cell.velocity.add(windForce.x * (float)deltaTime * 0.1f, windForce.y * (float)deltaTime * 0.1f);
                
                // Apply drag
                cell.velocity.mul(1.0f - (float)deltaTime * 0.05f);
                
                // Update strength
                cell.strength = cell.velocity.length();
            }
        }
    }
    
    private void updateDeepCurrents(double deltaTime) {
        // Deep currents are more stable, less affected by surface conditions
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                CurrentCell cell = deepCurrentGrid[x][z];
                
                // Slowly adjust based on temperature and salinity gradients
                cell.velocity.mul(1.0f - (float)deltaTime * 0.001f); // Very slow decay
            }
        }
    }
    
    private void updateVortices(double deltaTime) {
        Iterator<CurrentVortex> iterator = vortices.iterator();
        while (iterator.hasNext()) {
            CurrentVortex vortex = iterator.next();
            
            vortex.lifetime += (float)deltaTime;
            
            // Vortex strength decays over time
            vortex.strength *= 1.0f - (float)deltaTime * 0.001f;
            
            // Remove expired or weak vortices
            if (vortex.lifetime > vortex.maxLifetime || vortex.strength < 0.1f) {
                iterator.remove();
            }
        }
        
        // Occasionally spawn new vortices
        if (Math.random() < deltaTime * 0.001) { // Very rare
            Vector2f center = new Vector2f(
                (float)Math.random() * GRID_SIZE * CELL_SIZE,
                (float)Math.random() * GRID_SIZE * CELL_SIZE
            );
            vortices.add(new CurrentVortex(center, 75.0f, 1.5f, Math.random() < 0.5));
        }
    }
    
    private void updateThermalPlumes(double deltaTime) {
        for (ThermalPlume plume : thermalPlumes) {
            // Thermal plumes slowly drift with the current
            Vector3f currentAtPlume = getCurrentAt(plume.position.x, plume.position.y);
            plume.position.add(currentAtPlume.x * (float)deltaTime * 0.1f, currentAtPlume.z * (float)deltaTime * 0.1f);
            
            // Intensity slowly changes
            plume.intensity += ((float)Math.random() - 0.5f) * (float)deltaTime * 0.1f;
            plume.intensity = Math.max(0.1f, Math.min(2.0f, plume.intensity));
        }
    }
    
    private void applyTidalCurrents(double deltaTime) {
        // This would integrate with the TidalSystem
        float tidalStrength = (float)Math.sin(System.currentTimeMillis() * 0.001) * 0.5f;
        Vector2f tidalDirection = new Vector2f(1, 0); // East-west tidal flow
        
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                surfaceCurrentGrid[x][z].velocity.add(
                    tidalDirection.x * tidalStrength * (float)deltaTime * 0.1f,
                    tidalDirection.y * tidalStrength * (float)deltaTime * 0.1f
                );
            }
        }
    }
    
    private void updateCurrentStatistics() {
        float totalSpeed = 0.0f;
        float maxSpeed = 0.0f;
        int cellCount = 0;
        
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int z = 0; z < GRID_SIZE; z++) {
                float surfaceSpeed = surfaceCurrentGrid[x][z].strength;
                float deepSpeed = deepCurrentGrid[x][z].strength;
                
                totalSpeed += surfaceSpeed + deepSpeed;
                maxSpeed = Math.max(maxSpeed, Math.max(surfaceSpeed, deepSpeed));
                cellCount += 2;
            }
        }
        
        averageCurrentSpeed = totalSpeed / cellCount;
        maximumCurrentSpeed = maxSpeed;
    }
    
    public Vector3f getCurrentAt(float x, float z) {
        // Convert world coordinates to grid coordinates
        int gridX = Math.max(0, Math.min(GRID_SIZE - 1, (int)(x / CELL_SIZE + GRID_SIZE * 0.5f)));
        int gridZ = Math.max(0, Math.min(GRID_SIZE - 1, (int)(z / CELL_SIZE + GRID_SIZE * 0.5f)));
        
        Vector3f totalCurrent = new Vector3f();
        
        // Add surface current
        CurrentCell surfaceCell = surfaceCurrentGrid[gridX][gridZ];
        totalCurrent.add(surfaceCell.velocity.x, 0, surfaceCell.velocity.y);
        
        // Add deep current (reduced influence at surface)
        CurrentCell deepCell = deepCurrentGrid[gridX][gridZ];
        totalCurrent.add(deepCell.velocity.x * 0.3f, 0, deepCell.velocity.y * 0.3f);
        
        // Add vortex influences
        for (CurrentVortex vortex : vortices) {
            float distance = new Vector2f(x, z).distance(vortex.center);
            if (distance < vortex.radius) {
                float influence = 1.0f - (distance / vortex.radius);
                float angle = (float)Math.atan2(z - vortex.center.y, x - vortex.center.x);
                angle += vortex.clockwise ? -Math.PI * 0.5f : Math.PI * 0.5f;
                
                float vortexStrength = vortex.strength * influence;
                totalCurrent.add(
                    (float)Math.cos(angle) * vortexStrength,
                    0,
                    (float)Math.sin(angle) * vortexStrength
                );
            }
        }
        
        // Add thermal plume influences
        for (ThermalPlume plume : thermalPlumes) {
            float distance = new Vector2f(x, z).distance(plume.position);
            if (distance < plume.radius) {
                float influence = 1.0f - (distance / plume.radius);
                float verticalCurrent = plume.isHot ? plume.intensity * influence : -plume.intensity * influence;
                
                // Hot plumes create upward flow, cold plumes create downward flow
                totalCurrent.y += verticalCurrent * 0.5f;
                
                // Radial flow at the surface
                Vector2f radialDir = new Vector2f(x - plume.position.x, z - plume.position.y).normalize();
                float radialStrength = plume.intensity * influence * (plume.isHot ? 1.0f : -0.5f);
                totalCurrent.add(radialDir.x * radialStrength, 0, radialDir.y * radialStrength);
            }
        }
        
        return totalCurrent;
    }
    
    public void cleanup() {
        logger.info("Cleaning up advanced current system");
        currentLayers.clear();
        vortices.clear();
        thermalPlumes.clear();
    }
}