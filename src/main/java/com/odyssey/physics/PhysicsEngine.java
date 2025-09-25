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
     * Handle collision response between two objects.
     * This method applies damage, momentum transfer, and other collision effects.
     */
    public void handleCollisionResponse(Object objectA, Object objectB) {
        if (!initialized) return;
        
        // Handle Ship-Ship collision response
        if (objectA instanceof com.odyssey.ship.Ship && objectB instanceof com.odyssey.ship.Ship) {
            handleShipShipCollision((com.odyssey.ship.Ship) objectA, (com.odyssey.ship.Ship) objectB);
        }
        
        // Handle Ship-World collision response
        else if (objectA instanceof com.odyssey.ship.Ship && objectB instanceof com.odyssey.world.Chunk) {
            handleShipWorldCollision((com.odyssey.ship.Ship) objectA, (com.odyssey.world.Chunk) objectB);
        }
        
        // Handle Entity-Entity collision response
        else if (objectA instanceof com.odyssey.world.entities.Entity && objectB instanceof com.odyssey.world.entities.Entity) {
            handleEntityEntityCollision((com.odyssey.world.entities.Entity) objectA, (com.odyssey.world.entities.Entity) objectB);
        }
    }
    
    /**
     * Handle collision response between two ships.
     * Applies ramming damage, momentum transfer, and collision effects.
     */
    private void handleShipShipCollision(com.odyssey.ship.Ship shipA, com.odyssey.ship.Ship shipB) {
        org.joml.Vector3f posA = shipA.getPosition();
        org.joml.Vector3f posB = shipB.getPosition();
        org.joml.Vector3f velA = shipA.getVelocity();
        org.joml.Vector3f velB = shipB.getVelocity();
        
        float massA = shipA.getMass();
        float massB = shipB.getMass();
        
        // Calculate collision normal (from A to B)
        org.joml.Vector3f collisionNormal = new org.joml.Vector3f(posB).sub(posA).normalize();
        
        // Calculate relative velocity
        org.joml.Vector3f relativeVelocity = new org.joml.Vector3f(velA).sub(velB);
        float relativeSpeed = relativeVelocity.dot(collisionNormal);
        
        // Don't resolve if objects are separating
        if (relativeSpeed > 0) return;
        
        // Calculate collision impulse
        float restitution = 0.3f; // Ships don't bounce much
        float impulse = -(1 + restitution) * relativeSpeed / (1/massA + 1/massB);
        
        // Apply impulse to velocities
        org.joml.Vector3f impulseVector = new org.joml.Vector3f(collisionNormal).mul(impulse);
        velA.add(new org.joml.Vector3f(impulseVector).div(massA));
        velB.sub(new org.joml.Vector3f(impulseVector).div(massB));
        
        shipA.setVelocity(velA);
        shipB.setVelocity(velB);
        
        // Calculate damage based on collision energy
        float collisionEnergy = 0.5f * (massA * massB) / (massA + massB) * relativeSpeed * relativeSpeed;
        float damageA = collisionEnergy * 0.001f; // Scale damage appropriately
        float damageB = collisionEnergy * 0.001f;
        
        // Apply ramming damage to both ships
        org.joml.Vector3f localPosA = new org.joml.Vector3f(collisionNormal).mul(-shipA.getShipType().getLength() * 0.5f);
        org.joml.Vector3f localPosB = new org.joml.Vector3f(collisionNormal).mul(shipB.getShipType().getLength() * 0.5f);
        
        shipA.takeDamage(localPosA, damageA, com.odyssey.ship.DamageType.RAMMING);
        shipB.takeDamage(localPosB, damageB, com.odyssey.ship.DamageType.RAMMING);
        
        LOGGER.info("Ship collision: {} and {} collided with energy {}", 
                   shipA.getName(), shipB.getName(), collisionEnergy);
    }
    
    /**
     * Handle collision response between ship and world chunk.
     * Applies collision damage and stops the ship.
     */
    private void handleShipWorldCollision(com.odyssey.ship.Ship ship, com.odyssey.world.Chunk chunk) {
        org.joml.Vector3f shipPos = ship.getPosition();
        org.joml.Vector3f shipVel = ship.getVelocity();
        
        // Find collision point on ship
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        int chunkSize = 16;
        
        // Calculate collision normal based on which side of chunk was hit
        org.joml.Vector3f chunkCenter = new org.joml.Vector3f(
            chunkX * chunkSize + chunkSize * 0.5f,
            0,
            chunkZ * chunkSize + chunkSize * 0.5f
        );
        
        org.joml.Vector3f collisionNormal = new org.joml.Vector3f(shipPos).sub(chunkCenter).normalize();
        
        // Calculate collision energy
        float speed = shipVel.length();
        float mass = ship.getMass();
        float collisionEnergy = 0.5f * mass * speed * speed;
        
        // Apply collision damage based on speed and mass
        float damage = collisionEnergy * 0.0005f; // Scale appropriately
        
        // Determine collision point on ship (bow, side, stern)
        org.joml.Vector3f forward = ship.getForwardDirection();
        float dotProduct = forward.dot(collisionNormal);
        
        org.joml.Vector3f localCollisionPoint;
        com.odyssey.ship.DamageType damageType;
        
        if (dotProduct < -0.5f) {
            // Head-on collision (bow)
            localCollisionPoint = new org.joml.Vector3f(0, 0, ship.getShipType().getLength() * 0.5f);
            damageType = com.odyssey.ship.DamageType.COLLISION;
            damage *= 1.5f; // Bow takes more damage
        } else if (Math.abs(dotProduct) < 0.5f) {
            // Side collision
            localCollisionPoint = new org.joml.Vector3f(
                collisionNormal.x * ship.getShipType().getWidth() * 0.5f,
                0,
                0
            );
            damageType = com.odyssey.ship.DamageType.COLLISION;
        } else {
            // Rear collision (stern)
            localCollisionPoint = new org.joml.Vector3f(0, 0, -ship.getShipType().getLength() * 0.5f);
            damageType = com.odyssey.ship.DamageType.COLLISION;
            damage *= 0.8f; // Stern takes less damage
        }
        
        // Check if collision is with reef/rocks for different damage type
        // This would require checking the chunk's block types
        if (isReefCollision(chunk, shipPos)) {
            damageType = com.odyssey.ship.DamageType.REEF;
            damage *= 1.3f; // Reefs cause more damage
        }
        
        // Apply damage to ship
        ship.takeDamage(localCollisionPoint, damage, damageType);
        
        // Stop the ship or reduce velocity significantly
        org.joml.Vector3f newVelocity = new org.joml.Vector3f(shipVel);
        
        // Reflect velocity off collision normal with energy loss
        float normalComponent = newVelocity.dot(collisionNormal);
        if (normalComponent < 0) {
            org.joml.Vector3f reflection = new org.joml.Vector3f(collisionNormal).mul(normalComponent * 2);
            newVelocity.sub(reflection);
            newVelocity.mul(0.3f); // Lose most energy in collision
        }
        
        ship.setVelocity(newVelocity);
        
        LOGGER.info("Ship {} collided with terrain, damage: {}, type: {}", 
                   ship.getName(), damage, damageType);
    }
    
    /**
     * Handle collision response between two entities.
     */
    private void handleEntityEntityCollision(com.odyssey.world.entities.Entity entityA, com.odyssey.world.entities.Entity entityB) {
        org.joml.Vector3f posA = entityA.getPosition();
        org.joml.Vector3f posB = entityB.getPosition();
        org.joml.Vector3f velA = entityA.getVelocity();
        org.joml.Vector3f velB = entityB.getVelocity();
        
        float massA = entityA.getMass();
        float massB = entityB.getMass();
        
        // Calculate collision normal
        org.joml.Vector3f collisionNormal = new org.joml.Vector3f(posB).sub(posA).normalize();
        
        // Calculate relative velocity
        org.joml.Vector3f relativeVelocity = new org.joml.Vector3f(velA).sub(velB);
        float relativeSpeed = relativeVelocity.dot(collisionNormal);
        
        // Don't resolve if objects are separating
        if (relativeSpeed > 0) return;
        
        // Calculate collision impulse
        float restitution = 0.5f; // Entities bounce more than ships
        float impulse = -(1 + restitution) * relativeSpeed / (1/massA + 1/massB);
        
        // Apply impulse to velocities
        org.joml.Vector3f impulseVector = new org.joml.Vector3f(collisionNormal).mul(impulse);
        velA.add(new org.joml.Vector3f(impulseVector).div(massA));
        velB.sub(new org.joml.Vector3f(impulseVector).div(massB));
        
        entityA.setVelocity(velA);
        entityB.setVelocity(velB);
        
        // Apply collision damage if entities support it
        float collisionEnergy = 0.5f * (massA * massB) / (massA + massB) * relativeSpeed * relativeSpeed;
        float damage = collisionEnergy * 0.01f;
        
        // Apply damage through force (entities handle this in applyForce method)
        entityA.applyForce(new org.joml.Vector3f(collisionNormal).mul(-damage));
        entityB.applyForce(new org.joml.Vector3f(collisionNormal).mul(damage));
    }
    
    /**
     * Check if collision with chunk involves reef/rocks.
     * This is a simplified check - in a full implementation, 
     * this would examine the chunk's block composition.
     */
    private boolean isReefCollision(com.odyssey.world.Chunk chunk, org.joml.Vector3f position) {
        // Simplified reef detection - in reality would check chunk blocks
        // For now, assume chunks near water surface with solid blocks are reefs
        return position.y < 5.0f; // Shallow water collision likely reef
    }
    
    /**
     * Perform broad-phase collision detection to find potential collision pairs.
     * This method should be called before detailed collision checking.
     */
    public java.util.List<CollisionPair> broadPhaseCollisionDetection(java.util.List<Object> objects) {
        java.util.List<CollisionPair> potentialCollisions = new java.util.ArrayList<>();
        
        // Simple O(n²) broad phase - could be optimized with spatial partitioning
        for (int i = 0; i < objects.size(); i++) {
            for (int j = i + 1; j < objects.size(); j++) {
                Object objA = objects.get(i);
                Object objB = objects.get(j);
                
                if (couldCollide(objA, objB)) {
                    potentialCollisions.add(new CollisionPair(objA, objB));
                }
            }
        }
        
        return potentialCollisions;
    }
    
    /**
     * Quick check if two objects could potentially collide based on distance.
     */
    private boolean couldCollide(Object objA, Object objB) {
        org.joml.Vector3f posA = getObjectPosition(objA);
        org.joml.Vector3f posB = getObjectPosition(objB);
        
        if (posA == null || posB == null) return false;
        
        float radiusA = getObjectRadius(objA);
        float radiusB = getObjectRadius(objB);
        
        float distance = posA.distance(posB);
        float combinedRadius = radiusA + radiusB;
        
        // Add some margin for fast-moving objects
        return distance < combinedRadius * 1.5f;
    }
    
    /**
     * Get position of any supported object type.
     */
    private org.joml.Vector3f getObjectPosition(Object obj) {
        if (obj instanceof com.odyssey.ship.Ship) {
            return ((com.odyssey.ship.Ship) obj).getPosition();
        } else if (obj instanceof com.odyssey.world.entities.Entity) {
            return ((com.odyssey.world.entities.Entity) obj).getPosition();
        }
        return null;
    }
    
    /**
     * Get bounding radius of any supported object type.
     */
    private float getObjectRadius(Object obj) {
        if (obj instanceof com.odyssey.ship.Ship) {
            com.odyssey.ship.Ship ship = (com.odyssey.ship.Ship) obj;
            return Math.max(ship.getShipType().getLength(), ship.getShipType().getWidth()) * 0.5f;
        } else if (obj instanceof com.odyssey.world.entities.Entity) {
            return ((com.odyssey.world.entities.Entity) obj).getBoundingRadius();
        }
        return 1.0f; // Default radius
    }
    
    /**
     * Represents a potential collision pair for broad-phase detection.
     */
    public static class CollisionPair {
        public final Object objectA;
        public final Object objectB;
        
        public CollisionPair(Object objectA, Object objectB) {
            this.objectA = objectA;
            this.objectB = objectB;
        }
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
    
    /**
     * Gets the number of active physics objects being simulated
     */
    public int getActiveObjectCount() {
        // For now, return a placeholder value
        // In a full implementation, this would track all physics objects
        return 0;
    }
    
    /**
     * Gets the total number of physics simulation steps performed
     */
    public long getStepCount() {
        // For now, return a placeholder value
        // In a full implementation, this would track simulation steps
        return 0;
    }
}