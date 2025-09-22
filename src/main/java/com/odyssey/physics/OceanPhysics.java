package com.odyssey.physics;

import com.odyssey.util.Logger;
import com.odyssey.util.Timer;
import com.odyssey.util.MathUtils;
import com.odyssey.world.World;
import com.odyssey.world.Chunk;
import com.odyssey.world.Block;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles ocean physics simulation including waves, currents, and water dynamics.
 * Provides realistic water behavior for ship movement and environmental effects.
 */
public class OceanPhysics {
    
    private final World world;
    private final Timer timer;
    private final Logger logger;
    
    // Wave system
    private final List<WaveComponent> waveComponents;
    private final ConcurrentMap<ChunkCoordinate, WaterChunk> waterChunks;
    
    // Physics parameters
    private static final float GRAVITY = 9.81f;
    private static final float WATER_DENSITY = 1000.0f;
    private static final float AIR_DENSITY = 1.225f;
    private static final float VISCOSITY = 0.001f;
    private static final float FOAM_THRESHOLD = 2.0f;
    private static final float WAVE_BREAK_THRESHOLD = 1.5f;
    
    // Wave parameters
    private static final int MAX_WAVE_COMPONENTS = 8;
    private static final float BASE_WAVE_AMPLITUDE = 0.5f;
    private static final float BASE_WAVE_FREQUENCY = 0.1f;
    private static final float WAVE_SPEED_MULTIPLIER = 1.5f;
    
    // Current system
    private Vector2f globalCurrentDirection;
    private float globalCurrentStrength;
    private float windStrength;
    private Vector2f windDirection;
    
    // Time tracking
    private float simulationTime;
    private float lastUpdateTime;
    
    public OceanPhysics() {
        this.world = null;
        this.timer = null;
        this.logger = Logger.getLogger(OceanPhysics.class);
        this.waveComponents = new ArrayList<>();
        this.waterChunks = new ConcurrentHashMap<>();
        
        // Initialize default conditions
        this.globalCurrentDirection = new Vector2f(1.0f, 0.0f);
        this.globalCurrentStrength = 0.5f;
        this.windDirection = new Vector2f(0.7f, 0.7f).normalize();
        this.windStrength = 2.0f;
        
        this.simulationTime = 0.0f;
        this.lastUpdateTime = 0.0f;
    }
    
    /**
     * Initialize the ocean physics system
     */
    public void initialize() {
        initializeWaveSystem();
        logger.debug("Initialized ocean physics system");
    }
    
    /**
     * Clean up ocean physics resources
     */
    public void cleanup() {
        waveComponents.clear();
        waterChunks.clear();
        logger.debug("Cleaned up ocean physics system");
    }
    
    /**
     * Updates the ocean physics simulation
     */
    public void update(double deltaTime) {
        float dt = (float) deltaTime;
        simulationTime += dt;
        
        // Update wave system
        updateWaves(dt);
        
        // Update water chunks
        updateWaterChunks(dt);
        
        // Update currents based on wind
        updateCurrents(dt);
    }
    
    /**
     * Updates the ocean physics simulation (legacy method)
     */
    public void update() {
        if (timer != null) {
            float currentTime = timer.getTime();
            float deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            update((double) deltaTime);
        }
    }
    
    /**
     * Gets the water height at a specific world position
     */
    public float getWaterHeight(float worldX, float worldZ) {
        float baseHeight = getSeaLevel();
        float waveHeight = calculateWaveHeight(worldX, worldZ, simulationTime);
        
        return baseHeight + waveHeight;
    }
    
    /**
     * Gets the water velocity at a specific position
     */
    public Vector3f getWaterVelocity(float worldX, float worldY, float worldZ) {
        Vector3f velocity = new Vector3f();
        
        // Add wave-induced velocity
        Vector3f waveVelocity = calculateWaveVelocity(worldX, worldZ, simulationTime);
        velocity.add(waveVelocity);
        
        // Add current velocity
        Vector2f currentVel = getCurrentVelocity(worldX, worldZ);
        velocity.x += currentVel.x;
        velocity.z += currentVel.y;
        
        // Add depth-based velocity reduction
        float depth = getWaterDepth(worldX, worldY, worldZ);
        if (depth > 0) {
            float depthFactor = Math.min(1.0f, depth / 10.0f); // Reduce velocity near surface
            velocity.mul(depthFactor);
        }
        
        return velocity;
    }
    
    /**
     * Calculates buoyancy force for an object in water
     */
    public Vector3f calculateBuoyancy(Vector3f position, float volume, float objectDensity) {
        float depth = getWaterDepth(position.x, position.y, position.z);
        
        if (depth <= 0) {
            return new Vector3f(0, 0, 0); // Not in water
        }
        
        // Calculate submerged volume (simplified)
        float submergedVolume = Math.min(volume, depth * volume);
        
        // Buoyancy force = displaced water weight
        float buoyancyMagnitude = WATER_DENSITY * GRAVITY * submergedVolume;
        
        return new Vector3f(0, buoyancyMagnitude, 0);
    }
    
    /**
     * Calculates drag force for an object moving through water
     */
    public Vector3f calculateDrag(Vector3f velocity, float dragCoefficient, float crossSectionalArea) {
        float speed = velocity.length();
        if (speed < 0.001f) {
            return new Vector3f(0, 0, 0);
        }
        
        // Drag force = 0.5 * density * velocity^2 * drag_coefficient * area
        float dragMagnitude = 0.5f * WATER_DENSITY * speed * speed * dragCoefficient * crossSectionalArea;
        
        // Drag opposes motion
        Vector3f dragDirection = new Vector3f(velocity).normalize().negate();
        return dragDirection.mul(dragMagnitude);
    }
    
    /**
     * Gets the current velocity at a position
     */
    public Vector2f getCurrentVelocity(float worldX, float worldZ) {
        // Base current
        Vector2f current = new Vector2f(globalCurrentDirection).mul(globalCurrentStrength);
        
        // Add local variations based on position
        float noiseX = MathUtils.noise(worldX * 0.01f, worldZ * 0.01f, simulationTime * 0.1f);
        float noiseZ = MathUtils.noise(worldX * 0.01f + 100, worldZ * 0.01f + 100, simulationTime * 0.1f);
        
        current.x += noiseX * 0.2f;
        current.y += noiseZ * 0.2f;
        
        return current;
    }
    
    /**
     * Gets the depth of water at a position
     */
    public float getWaterDepth(float worldX, float worldY, float worldZ) {
        float waterSurface = getWaterHeight(worldX, worldZ);
        return Math.max(0, waterSurface - worldY);
    }
    
    /**
     * Checks if a position is underwater
     */
    public boolean isUnderwater(float worldX, float worldY, float worldZ) {
        return worldY < getWaterHeight(worldX, worldZ);
    }
    
    /**
     * Gets the sea level (base water height)
     */
    public float getSeaLevel() {
        return 64.0f; // Same as world generation sea level
    }
    
    /**
     * Sets wind conditions
     */
    public void setWind(Vector2f direction, float strength) {
        this.windDirection.set(direction).normalize();
        this.windStrength = Math.max(0, strength);
        
        // Wind affects wave generation
        updateWaveSystemFromWind();
    }
    
    /**
     * Gets current wind direction
     */
    public Vector2f getWindDirection() {
        return new Vector2f(windDirection);
    }
    
    /**
     * Gets current wind strength
     */
    public float getWindStrength() {
        return windStrength;
    }
    
    /**
     * Get wind velocity at a specific position
     */
    public Vector3f getWindVelocity(Vector3f position) {
        // Convert 2D wind direction to 3D velocity
        Vector3f windVel = new Vector3f(windDirection.x, 0.0f, windDirection.y);
        windVel.mul(windStrength);
        return windVel;
    }
    
    /**
     * Calculates forces acting on a ship at a given position
     */
    public Vector3f calculateForces(Vector3f position, Vector3f velocity, float mass, float dragCoefficient) {
        Vector3f totalForce = new Vector3f();
        
        // Calculate buoyancy force
        Vector3f buoyancyForce = calculateBuoyancy(position, mass / WATER_DENSITY, mass / WATER_DENSITY);
        totalForce.add(buoyancyForce);
        
        // Calculate drag force
        Vector3f dragForce = calculateDrag(velocity, dragCoefficient, 10.0f); // Approximate cross-sectional area
        totalForce.add(dragForce);
        
        // Calculate current forces
        Vector2f currentVel = getCurrentVelocity(position.x, position.z);
        Vector3f currentForce = new Vector3f(currentVel.x, 0.0f, currentVel.y);
        currentForce.mul(mass * 0.1f); // Current influence factor
        totalForce.add(currentForce);
        
        return totalForce;
    }
    
    /**
     * Initializes the wave system with multiple wave components
     */
    private void initializeWaveSystem() {
        waveComponents.clear();
        
        // Create multiple wave components for realistic ocean
        for (int i = 0; i < MAX_WAVE_COMPONENTS; i++) {
            float amplitude = BASE_WAVE_AMPLITUDE * (1.0f - i * 0.1f);
            float frequency = BASE_WAVE_FREQUENCY * (1.0f + i * 0.3f);
            float phase = (float) (Math.random() * Math.PI * 2);
            
            Vector2f direction = new Vector2f(
                (float) Math.cos(Math.random() * Math.PI * 2),
                (float) Math.sin(Math.random() * Math.PI * 2)
            ).normalize();
            
            waveComponents.add(new WaveComponent(amplitude, frequency, phase, direction));
        }
        
        logger.warn("Initialized {} wave components", waveComponents.size());
    }
    
    /**
     * Updates wave system based on wind conditions
     */
    private void updateWaveSystemFromWind() {
        for (WaveComponent wave : waveComponents) {
            // Align wave directions with wind
            float windAlignment = wave.direction.dot(windDirection);
            if (windAlignment > 0) {
                // Waves aligned with wind get stronger
                wave.amplitude = Math.min(wave.baseAmplitude * (1.0f + windStrength * 0.2f), 
                                        wave.baseAmplitude * 2.0f);
            } else {
                // Waves against wind get weaker
                wave.amplitude = wave.baseAmplitude * (1.0f - windStrength * 0.1f);
            }
        }
    }
    
    /**
     * Updates the wave system
     */
    private void updateWaves(float deltaTime) {
        // Waves are updated through time progression
        // Individual wave components don't need per-frame updates
        // as they're calculated on-demand in calculateWaveHeight
    }
    
    /**
     * Updates water chunks for local water simulation
     */
    private void updateWaterChunks(float deltaTime) {
        // Update existing water chunks
        for (WaterChunk waterChunk : waterChunks.values()) {
            waterChunk.update(deltaTime);
        }
        
        // Add/remove water chunks based on player position
        updateWaterChunkLoading();
    }
    
    /**
     * Manages loading and unloading of water chunks based on player position
     */
    private void updateWaterChunkLoading() {
        // Get player position from world (assuming there's a player entity)
        Vector3f playerPos = getPlayerPosition();
        if (playerPos == null) return;
        
        int playerChunkX = (int) Math.floor(playerPos.x / Chunk.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPos.z / Chunk.CHUNK_SIZE);
        
        // Define render distance for water chunks
        int waterChunkRenderDistance = 8;
        
        // Load water chunks around player
        for (int x = playerChunkX - waterChunkRenderDistance; x <= playerChunkX + waterChunkRenderDistance; x++) {
            for (int z = playerChunkZ - waterChunkRenderDistance; z <= playerChunkZ + waterChunkRenderDistance; z++) {
                ChunkCoordinate coord = new ChunkCoordinate(x, z);
                
                // Only create water chunks for ocean areas (not land)
                if (!waterChunks.containsKey(coord) && isOceanChunk(x, z)) {
                    WaterChunk waterChunk = new WaterChunk(x, z);
                    waterChunks.put(coord, waterChunk);
                    initializeWaterChunk(waterChunk);
                }
            }
        }
        
        // Unload distant water chunks
        waterChunks.entrySet().removeIf(entry -> {
            ChunkCoordinate coord = entry.getKey();
            int distance = Math.max(Math.abs(coord.x - playerChunkX), Math.abs(coord.z - playerChunkZ));
            return distance > waterChunkRenderDistance + 2; // Add buffer to prevent constant loading/unloading
        });
    }
    
    /**
     * Gets the current player position from the world
     */
    private Vector3f getPlayerPosition() {
        // This would typically get the player entity from the world
        // For now, return a default position or implement based on your player system
        return new Vector3f(0, 64, 0); // Default sea level position
    }
    
    /**
     * Checks if a chunk coordinate represents an ocean area
     */
    private boolean isOceanChunk(int chunkX, int chunkZ) {
        // Check if this chunk is primarily ocean
        // This could be determined by checking the world's terrain height
        // For now, assume all chunks can have water
        return true;
    }
    
    /**
     * Initializes a newly created water chunk with wave data
     */
    private void initializeWaterChunk(WaterChunk waterChunk) {
        // Initialize the water chunk's height field with current wave state
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                float worldX = waterChunk.chunkX * Chunk.CHUNK_SIZE + x;
                float worldZ = waterChunk.chunkZ * Chunk.CHUNK_SIZE + z;
                
                // Set initial wave height
                waterChunk.heightField[x][z] = calculateWaveHeight(worldX, worldZ, simulationTime);
                
                // Set initial velocity based on currents
                Vector2f current = getCurrentVelocity(worldX, worldZ);
                waterChunk.velocityField[x][z].set(current);
            }
        }
    }
    
    /**
     * Updates current system
     */
    private void updateCurrents(float deltaTime) {
        // Gradually align currents with wind direction
        Vector2f targetDirection = new Vector2f(windDirection);
        globalCurrentDirection.lerp(targetDirection, deltaTime * 0.1f);
        globalCurrentDirection.normalize();
        
        // Adjust current strength based on wind
        float targetStrength = windStrength * 0.3f;
        globalCurrentStrength = MathUtils.lerp(globalCurrentStrength, targetStrength, deltaTime * 0.2f);
    }
    
    /**
     * Calculates wave height at a position and time
     */
    private float calculateWaveHeight(float x, float z, float time) {
        float totalHeight = 0.0f;
        
        for (WaveComponent wave : waveComponents) {
            float dotProduct = x * wave.direction.x + z * wave.direction.y;
            float waveValue = (float) Math.sin(dotProduct * wave.frequency + time * wave.speed + wave.phase);
            totalHeight += wave.amplitude * waveValue;
        }
        
        return totalHeight;
    }
    
    /**
     * Calculates wave-induced velocity at a position
     */
    private Vector3f calculateWaveVelocity(float x, float z, float time) {
        Vector3f velocity = new Vector3f();
        
        for (WaveComponent wave : waveComponents) {
            float dotProduct = x * wave.direction.x + z * wave.direction.y;
            float wavePhase = dotProduct * wave.frequency + time * wave.speed + wave.phase;
            
            float cosWave = (float) Math.cos(wavePhase);
            float amplitude = wave.amplitude * wave.frequency * wave.speed;
            
            velocity.x += amplitude * wave.direction.x * cosWave;
            velocity.z += amplitude * wave.direction.y * cosWave;
            velocity.y += amplitude * (float) Math.sin(wavePhase);
        }
        
        return velocity;
    }
    
    /**
     * Represents a single wave component
     */
    private static class WaveComponent {
        float amplitude;
        final float baseAmplitude;
        final float frequency;
        final float phase;
        final float speed;
        final Vector2f direction;
        
        WaveComponent(float amplitude, float frequency, float phase, Vector2f direction) {
            this.amplitude = amplitude;
            this.baseAmplitude = amplitude;
            this.frequency = frequency;
            this.phase = phase;
            this.direction = new Vector2f(direction);
            this.speed = (float) Math.sqrt(GRAVITY / frequency) * WAVE_SPEED_MULTIPLIER;
        }
    }
    
    /**
     * Represents a chunk of water for local simulation
     */
    private class WaterChunk {
        final int chunkX, chunkZ;
        final float[][] heightField;
        final Vector2f[][] velocityField;
        
        WaterChunk(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.heightField = new float[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
            this.velocityField = new Vector2f[Chunk.CHUNK_SIZE][Chunk.CHUNK_SIZE];
            
            // Initialize velocity field
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    velocityField[x][z] = new Vector2f();
                }
            }
        }
        
        void update(float deltaTime) {
            // Update local water simulation with foam, splash effects, and wave propagation
            updateWaveHeights(deltaTime);
            updateVelocityField(deltaTime);
            updateFoamEffects(deltaTime);
        }
        
        /**
         * Updates wave heights in this water chunk
         */
        private void updateWaveHeights(float deltaTime) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    float worldX = chunkX * Chunk.CHUNK_SIZE + x;
                    float worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                    
                    // Update height based on global wave system
                    heightField[x][z] = calculateWaveHeight(worldX, worldZ, simulationTime);
                }
            }
        }
        
        /**
         * Updates velocity field for water flow simulation
         */
        private void updateVelocityField(float deltaTime) {
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    float worldX = chunkX * Chunk.CHUNK_SIZE + x;
                    float worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                    
                    // Get current at this position
                    Vector2f current = getCurrentVelocity(worldX, worldZ);
                    
                    // Apply wave-induced velocity
                    float waveInfluence = 0.1f;
                    Vector2f waveVelocity = calculateWaveVelocity(worldX, worldZ);
                    
                    // Blend current and wave velocity
                    velocityField[x][z].lerp(current, deltaTime * 2.0f);
                    velocityField[x][z].add(waveVelocity.x * waveInfluence, waveVelocity.y * waveInfluence);
                }
            }
        }
        
        /**
         * Updates foam effects based on wave activity
         */
        private void updateFoamEffects(float deltaTime) {
            // This could track foam intensity, whitecaps, etc.
            // For now, this is a placeholder for future foam rendering
        }
        
        /**
         * Calculates wave-induced velocity at a position
         */
        private Vector2f calculateWaveVelocity(float worldX, float worldZ) {
            Vector2f velocity = new Vector2f(0, 0);
            
            for (WaveComponent wave : waveComponents) {
                float dotProduct = worldX * wave.direction.x + worldZ * wave.direction.y;
                float wavePhase = dotProduct * wave.frequency + simulationTime * wave.speed + wave.phase;
                float waveDerivative = (float) Math.cos(wavePhase) * wave.frequency * wave.amplitude;
                
                velocity.add(wave.direction.x * waveDerivative, wave.direction.y * waveDerivative);
            }
            
            return velocity;
        }
    }
    
    /**
     * Coordinate pair for water chunks
     */
    private static class ChunkCoordinate {
        final int x, z;
        
        ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ChunkCoordinate)) return false;
            ChunkCoordinate other = (ChunkCoordinate) obj;
            return x == other.x && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }
}