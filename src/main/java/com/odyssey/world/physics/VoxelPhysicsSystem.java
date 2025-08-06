package com.odyssey.world.physics;

import com.odyssey.core.GameConfig;
import com.odyssey.core.jobs.JobSystem;
import com.odyssey.world.chunk.ChunkManager;
import com.odyssey.world.generation.WorldGenerator.BlockType;
// All physics systems are in the same package
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Advanced Voxel Physics System for The Odyssey.
 * 
 * <p>Coordinates and manages all voxel-based physics simulations including:
 * <ul>
 *   <li>Fluid dynamics for water and lava flow</li>
 *   <li>Structural integrity and collapse mechanics</li>
 *   <li>Cloth and rope physics for sails and rigging</li>
 *   <li>Fire propagation and smoke simulation</li>
 *   <li>Flooding and compartment systems</li>
 * </ul>
 * 
 * <p>The system is designed for high performance with:
 * <ul>
 *   <li>Tick-based updates with configurable intervals</li>
 *   <li>Spatial partitioning for efficient collision detection</li>
 *   <li>Asynchronous processing for heavy calculations</li>
 *   <li>Memory-efficient data structures</li>
 *   <li>Integration with the existing chunk system</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class VoxelPhysicsSystem {
    private static final Logger logger = LoggerFactory.getLogger(VoxelPhysicsSystem.class);
    
    /** Default physics tick rate (20 TPS). */
    private static final int DEFAULT_TICK_RATE = 20;
    
    /** Physics update interval in milliseconds. */
    private static final long PHYSICS_UPDATE_INTERVAL = 1000 / DEFAULT_TICK_RATE;
    
    /** Maximum physics simulation distance from player. */
    private static final int MAX_SIMULATION_DISTANCE = 128;
    
    private final GameConfig config;
    private final ChunkManager chunkManager;
    private final JobSystem jobSystem;
    
    // Physics subsystems
    private final FluidDynamicsSystem fluidSystem;
    private final StructuralIntegritySystem structuralSystem;
    private final ClothPhysicsSystem clothSystem;
    private final FirePropagationSystem fireSystem;
    private final FloodingSystem floodingSystem;
    
    // Threading and performance
    private final ExecutorService physicsExecutor;
    private final AtomicLong lastPhysicsUpdate = new AtomicLong(0);
    private volatile boolean running = false;
    
    // Player tracking for simulation bounds
    private volatile Vector3f playerPosition = new Vector3f();
    private volatile int simulationDistance = MAX_SIMULATION_DISTANCE;
    
    // Active physics regions
    private final Set<PhysicsRegion> activeRegions = ConcurrentHashMap.newKeySet();
    private final Map<Vector3f, PhysicsBlock> physicsBlocks = new ConcurrentHashMap<>();
    
    // Performance statistics
    private final AtomicLong physicsUpdates = new AtomicLong(0);
    private final AtomicLong fluidUpdates = new AtomicLong(0);
    private final AtomicLong structuralChecks = new AtomicLong(0);
    private final AtomicLong fireUpdates = new AtomicLong(0);
    
    /**
     * Creates a new voxel physics system.
     * 
     * @param config the game configuration
     * @param chunkManager the chunk manager for world access
     * @param jobSystem the job system for async operations
     */
    public VoxelPhysicsSystem(GameConfig config, ChunkManager chunkManager, JobSystem jobSystem) {
        this.config = config;
        this.chunkManager = chunkManager;
        this.jobSystem = jobSystem;
        
        // Initialize physics subsystems
        this.fluidSystem = new FluidDynamicsSystem(config);
        this.structuralSystem = new StructuralIntegritySystem(config);
        this.clothSystem = new ClothPhysicsSystem(config);
        this.fireSystem = new FirePropagationSystem(config);
        this.floodingSystem = new FloodingSystem(config);
        
        // Initialize thread pool for physics calculations
        int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "VoxelPhysics-" + System.currentTimeMillis());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1); // Lower priority than rendering
            return t;
        });
        
        logger.info("Initialized voxel physics system with {} threads", threadCount);
    }
    
    /**
     * Initializes the physics system and starts background processing.
     */
    public void initialize() {
        logger.info("Initializing voxel physics system...");
        
        // Initialize all subsystems
        fluidSystem.initialize();
        structuralSystem.initialize();
        clothSystem.initialize();
        fireSystem.initialize();
        floodingSystem.initialize();
        
        running = true;
        
        // Start background physics processing
        startPhysicsLoop();
        
        logger.info("Voxel physics system initialized successfully");
    }
    
    /**
     * Updates the physics system with player position and delta time.
     * 
     * @param deltaTime the time elapsed since last update in seconds
     * @param playerPosition the current player position
     */
    public void update(double deltaTime, Vector3f playerPosition) {
        if (!running || playerPosition == null) {
            return;
        }
        
        this.playerPosition.set(playerPosition);
        
        // Update active physics regions based on player position
        updateActiveRegions();
        
        // Throttle physics updates to maintain consistent tick rate
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPhysicsUpdate.get() >= PHYSICS_UPDATE_INTERVAL) {
            lastPhysicsUpdate.set(currentTime);
            
            // Schedule async physics update
            CompletableFuture.runAsync(() -> {
                performPhysicsUpdate(deltaTime);
            }, physicsExecutor);
        }
    }
    
    /**
     * Performs a single physics update tick.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void performPhysicsUpdate(double deltaTime) {
        try {
            physicsUpdates.incrementAndGet();
            
            // Update all physics subsystems
            fluidSystem.update(deltaTime);
            structuralSystem.update(deltaTime);
            clothSystem.update(deltaTime);
            fireSystem.update(deltaTime);
            floodingSystem.update(deltaTime);
            
            // Clean up inactive physics blocks
            cleanupInactiveBlocks();
            
        } catch (Exception e) {
            logger.error("Error during physics update", e);
        }
    }
    
    /**
     * Starts the background physics processing loop.
     */
    private void startPhysicsLoop() {
        CompletableFuture.runAsync(() -> {
            while (running) {
                try {
                    Thread.sleep(PHYSICS_UPDATE_INTERVAL);
                    
                    // Perform maintenance tasks
                    maintainPhysicsRegions();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in physics loop", e);
                }
            }
        }, physicsExecutor);
    }
    
    /**
     * Updates active physics regions based on player position.
     */
    private void updateActiveRegions() {
        // Clear old regions that are too far from player
        activeRegions.removeIf(region -> {
            float distance = region.getCenter().distance(playerPosition);
            return distance > simulationDistance;
        });
        
        // Add new regions around player
        int regionSize = 32; // 32x32 block regions
        int playerRegionX = (int) Math.floor(playerPosition.x / regionSize);
        int playerRegionZ = (int) Math.floor(playerPosition.z / regionSize);
        
        int regionRadius = simulationDistance / regionSize;
        
        for (int x = playerRegionX - regionRadius; x <= playerRegionX + regionRadius; x++) {
            for (int z = playerRegionZ - regionRadius; z <= playerRegionZ + regionRadius; z++) {
                Vector3f regionCenter = new Vector3f(x * regionSize + regionSize / 2f, 0, z * regionSize + regionSize / 2f);
                PhysicsRegion region = new PhysicsRegion(x, z, regionSize, regionCenter);
                activeRegions.add(region);
            }
        }
    }
    
    /**
     * Maintains physics regions by updating their state.
     */
    private void maintainPhysicsRegions() {
        for (PhysicsRegion region : activeRegions) {
            region.update();
        }
    }
    
    /**
     * Cleans up inactive physics blocks to free memory.
     */
    private void cleanupInactiveBlocks() {
        physicsBlocks.entrySet().removeIf(entry -> {
            PhysicsBlock block = entry.getValue();
            return !block.isActive() || block.getPosition().distance(playerPosition) > simulationDistance;
        });
    }
    
    /**
     * Registers a block for physics simulation.
     * 
     * @param position the block position
     * @param blockType the block type
     * @param properties additional physics properties
     */
    public void registerPhysicsBlock(Vector3f position, BlockType blockType, Map<String, Object> properties) {
        PhysicsBlock physicsBlock = new PhysicsBlock(position, blockType, properties);
        physicsBlocks.put(new Vector3f(position), physicsBlock);
    }
    
    /**
     * Unregisters a block from physics simulation.
     * 
     * @param position the block position
     */
    public void unregisterPhysicsBlock(Vector3f position) {
        physicsBlocks.remove(position);
    }
    
    /**
     * Gets a physics block at the specified position.
     * 
     * @param position the block position
     * @return the physics block, or null if not found
     */
    public PhysicsBlock getPhysicsBlock(Vector3f position) {
        return physicsBlocks.get(position);
    }
    
    /**
     * Checks if a position is within the active simulation area.
     * 
     * @param position the position to check
     * @return true if within simulation area
     */
    public boolean isWithinSimulationArea(Vector3f position) {
        return position.distance(playerPosition) <= simulationDistance;
    }
    
    // Getters for subsystems
    public FluidDynamicsSystem getFluidSystem() { return fluidSystem; }
    public StructuralIntegritySystem getStructuralSystem() { return structuralSystem; }
    public ClothPhysicsSystem getClothSystem() { return clothSystem; }
    public FirePropagationSystem getFireSystem() { return fireSystem; }
    public FloodingSystem getFloodingSystem() { return floodingSystem; }
    
    // Getters for statistics
    public long getPhysicsUpdates() { return physicsUpdates.get(); }
    public long getFluidUpdates() { return fluidUpdates.get(); }
    public long getStructuralChecks() { return structuralChecks.get(); }
    public long getFireUpdates() { return fireUpdates.get(); }
    public int getActiveRegionCount() { return activeRegions.size(); }
    public int getPhysicsBlockCount() { return physicsBlocks.size(); }
    
    /**
     * Shuts down the physics system and releases resources.
     */
    public void shutdown() {
        logger.info("Shutting down voxel physics system...");
        
        running = false;
        
        // Shutdown subsystems
        fluidSystem.shutdown();
        structuralSystem.shutdown();
        clothSystem.shutdown();
        fireSystem.shutdown();
        floodingSystem.shutdown();
        
        // Shutdown thread pool
        physicsExecutor.shutdown();
        
        logger.info("Voxel physics system shut down successfully");
    }
}