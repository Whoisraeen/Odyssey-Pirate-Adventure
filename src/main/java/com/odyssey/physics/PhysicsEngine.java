package com.odyssey.physics;

import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Physics engine for The Odyssey.
 * Handles ship physics, ocean simulation, collision detection, and fluid dynamics.
 */
public class PhysicsEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PhysicsEngine.class);
    
    private final GameConfig config;
    private OceanPhysics oceanPhysics;
    private FluidDynamics fluidDynamics;
    private WaveSystem waveSystem;
    private boolean initialized = false;
    
    public PhysicsEngine(GameConfig config) {
        this.config = config;
    }
    
    /**
     * Initialize the physics engine.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("PhysicsEngine is already initialized");
            return;
        }
        
        LOGGER.info("Initializing PhysicsEngine...");
        
        try {
            // Initialize ocean physics
            oceanPhysics = new OceanPhysics();
            oceanPhysics.initialize();
            
            // Initialize fluid dynamics
            fluidDynamics = new FluidDynamics();
            fluidDynamics.initialize();
            
            // Initialize wave system
            waveSystem = new WaveSystem();
            waveSystem.initialize();
            
            initialized = true;
            LOGGER.info("PhysicsEngine initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize PhysicsEngine", e);
            cleanup();
            throw new RuntimeException("PhysicsEngine initialization failed", e);
        }
    }
    
    /**
     * Update the physics simulation.
     */
    public void update(double deltaTime) {
        if (!initialized) return;
        
        // Update ocean physics
        if (oceanPhysics != null) {
            oceanPhysics.update(deltaTime);
        }
        
        // Update fluid dynamics
        if (fluidDynamics != null) {
            fluidDynamics.update(deltaTime);
        }
        
        // Update wave system
        if (waveSystem != null) {
            waveSystem.update(deltaTime);
        }
    }
    
    /**
     * Get the ocean physics system.
     */
    public OceanPhysics getOceanPhysics() {
        return oceanPhysics;
    }
    
    /**
     * Get the fluid dynamics system.
     */
    public FluidDynamics getFluidDynamics() {
        return fluidDynamics;
    }
    
    /**
     * Get the wave system.
     */
    public WaveSystem getWaveSystem() {
        return waveSystem;
    }
    
    /**
     * Apply force to an object in the physics world.
     */
    public void applyForce(Object object, float forceX, float forceY, float forceZ) {
        if (!initialized) return;
        
        LOGGER.debug("Applying force ({}, {}, {}) to object", forceX, forceY, forceZ);
        
        if (object instanceof com.odyssey.ship.Ship) {
            com.odyssey.ship.Ship ship = (com.odyssey.ship.Ship) object;
            org.joml.Vector3f force = new org.joml.Vector3f(forceX, forceY, forceZ);
            
            // Apply force through ship's physics system
            if (ship.getShipPhysics() != null) {
                ship.getShipPhysics().applyExternalForce(force);
            } else {
                // Fallback: apply force directly to velocity
                org.joml.Vector3f velocity = ship.getVelocity();
                float mass = ship.getMass();
                if (mass > 0) {
                    org.joml.Vector3f acceleration = new org.joml.Vector3f(force).div(mass);
                    velocity.add(acceleration.mul(0.016f)); // Assume 60 FPS
                    ship.setVelocity(velocity);
                }
            }
        } else if (object instanceof com.odyssey.world.entities.Entity) {
            // Handle other entity types
            com.odyssey.world.entities.Entity entity = (com.odyssey.world.entities.Entity) object;
            org.joml.Vector3f force = new org.joml.Vector3f(forceX, forceY, forceZ);
            
            // Apply force to entity velocity
            org.joml.Vector3f velocity = entity.getVelocity();
            float mass = entity.getMass();
            if (mass > 0) {
                org.joml.Vector3f acceleration = new org.joml.Vector3f(force).div(mass);
                velocity.add(acceleration.mul(0.016f)); // Assume 60 FPS
                entity.setVelocity(velocity);
            }
        }
    }
    
    /**
     * Perform collision detection between two objects.
     */
    public boolean checkCollision(Object objectA, Object objectB) {
        if (!initialized) return false;
        
        // Handle Ship-Ship collisions
        if (objectA instanceof com.odyssey.ship.Ship && objectB instanceof com.odyssey.ship.Ship) {
            return checkShipCollision((com.odyssey.ship.Ship) objectA, (com.odyssey.ship.Ship) objectB);
        }
        
        // Handle Ship-World collisions
        if (objectA instanceof com.odyssey.ship.Ship && objectB instanceof com.odyssey.world.Chunk) {
            return checkShipWorldCollision((com.odyssey.ship.Ship) objectA, (com.odyssey.world.Chunk) objectB);
        }
        
        // Handle Entity-Entity collisions
        if (objectA instanceof com.odyssey.world.entities.Entity && objectB instanceof com.odyssey.world.entities.Entity) {
            return checkEntityCollision((com.odyssey.world.entities.Entity) objectA, (com.odyssey.world.entities.Entity) objectB);
        }
        
        return false;
    }
    
    /**
     * Simulate buoyancy for floating objects.
     */
    public void simulateBuoyancy(Object object) {
        if (!initialized) return;
        
        if (object instanceof com.odyssey.ship.Ship) {
            com.odyssey.ship.Ship ship = (com.odyssey.ship.Ship) object;
            simulateShipBuoyancy(ship);
        } else if (object instanceof com.odyssey.world.entities.Entity) {
            com.odyssey.world.entities.Entity entity = (com.odyssey.world.entities.Entity) object;
            simulateEntityBuoyancy(entity);
        }
    }
    
    /**
     * Simulate buoyancy specifically for ships using ocean physics.
     */
    private void simulateShipBuoyancy(com.odyssey.ship.Ship ship) {
        org.joml.Vector3f position = ship.getPosition();
        float waterHeight = oceanPhysics.getWaterHeight(position.x, position.z);
        
        // Check if ship is in water
        if (position.y <= waterHeight) {
            float submersion = Math.max(0, waterHeight - position.y);
            float mass = ship.getMass();
            
            // Calculate buoyant force using Archimedes' principle
            float displacedVolume = submersion * ship.getShipType().getLength() * ship.getShipType().getWidth();
            float buoyantForce = displacedVolume * 1000.0f * 9.81f; // Water density * gravity
            
            // Apply buoyant force upward
            applyForce(ship, 0, buoyantForce - mass * 9.81f, 0);
            
            // Calculate ocean forces (currents, waves)
            org.joml.Vector3f oceanForces = oceanPhysics.calculateForces(position, ship.getVelocity(), mass, 0.5f);
            applyForce(ship, oceanForces.x, oceanForces.y, oceanForces.z);
            
            // Apply wave forces if wave system is available
            if (waveSystem != null) {
                org.joml.Vector3f waveForce = waveSystem.calculateWaveForce(position, ship.getShipType().getLength(), mass);
                applyForce(ship, waveForce.x, waveForce.y, waveForce.z);
            }
        }
    }
    
    /**
     * Simulate buoyancy for general entities.
     */
    private void simulateEntityBuoyancy(com.odyssey.world.entities.Entity entity) {
        org.joml.Vector3f position = entity.getPosition();
        float waterHeight = oceanPhysics.getWaterHeight(position.x, position.z);
        
        // Check if entity is in water
        if (position.y <= waterHeight) {
            float submersion = Math.max(0, waterHeight - position.y);
            float mass = entity.getMass();
            float volume = entity.getVolume(); // Assume entities have a volume method
            
            // Calculate buoyant force
            float displacedVolume = Math.min(volume, submersion * 1.0f); // Approximate
            float buoyantForce = displacedVolume * 1000.0f * 9.81f;
            
            // Apply buoyant force
            applyForce(entity, 0, buoyantForce - mass * 9.81f, 0);
            
            // Apply water resistance
            org.joml.Vector3f velocity = entity.getVelocity();
            float dragCoefficient = 0.8f;
            org.joml.Vector3f dragForce = new org.joml.Vector3f(velocity).negate().mul(dragCoefficient * velocity.lengthSquared());
            applyForce(entity, dragForce.x, dragForce.y, dragForce.z);
        }
    }
    
    /**
     * Check collision between two ships using bounding box intersection.
     */
    private boolean checkShipCollision(com.odyssey.ship.Ship shipA, com.odyssey.ship.Ship shipB) {
        org.joml.Vector3f posA = shipA.getPosition();
        org.joml.Vector3f posB = shipB.getPosition();
        
        float lengthA = shipA.getShipType().getLength();
        float widthA = shipA.getShipType().getWidth();
        float lengthB = shipB.getShipType().getLength();
        float widthB = shipB.getShipType().getWidth();
        
        // Simple AABB collision detection
        float dx = Math.abs(posA.x - posB.x);
        float dz = Math.abs(posA.z - posB.z);
        
        return dx < (lengthA + lengthB) / 2.0f && dz < (widthA + widthB) / 2.0f;
    }
    
    /**
     * Check collision between ship and world chunk.
     */
    private boolean checkShipWorldCollision(com.odyssey.ship.Ship ship, com.odyssey.world.Chunk chunk) {
        org.joml.Vector3f shipPos = ship.getPosition();
        
        // Check if ship is within chunk bounds
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        int chunkSize = 16; // Standard chunk size
        
        float shipLength = ship.getShipType().getLength();
        float shipWidth = ship.getShipType().getWidth();
        
        // Check if ship bounding box intersects with chunk
        float minX = chunkX * chunkSize;
        float maxX = (chunkX + 1) * chunkSize;
        float minZ = chunkZ * chunkSize;
        float maxZ = (chunkZ + 1) * chunkSize;
        
        return shipPos.x - shipLength/2 < maxX && shipPos.x + shipLength/2 > minX &&
               shipPos.z - shipWidth/2 < maxZ && shipPos.z + shipWidth/2 > minZ;
    }
    
    /**
     * Check collision between two entities.
     */
    private boolean checkEntityCollision(com.odyssey.world.entities.Entity entityA, com.odyssey.world.entities.Entity entityB) {
        org.joml.Vector3f posA = entityA.getPosition();
        org.joml.Vector3f posB = entityB.getPosition();
        
        float radiusA = entityA.getBoundingRadius();
        float radiusB = entityB.getBoundingRadius();
        
        float distance = posA.distance(posB);
        return distance < (radiusA + radiusB);
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up PhysicsEngine resources...");
        
        if (waveSystem != null) {
            waveSystem.cleanup();
        }
        
        if (fluidDynamics != null) {
            fluidDynamics.cleanup();
        }
        
        if (oceanPhysics != null) {
            oceanPhysics.cleanup();
        }
        
        initialized = false;
        LOGGER.info("PhysicsEngine cleanup complete");
    }
}