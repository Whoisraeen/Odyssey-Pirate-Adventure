package com.odyssey.game.systems;

import com.odyssey.game.ecs.System;
import com.odyssey.game.ecs.World;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.components.BuoyancyComponent;
import com.odyssey.game.math.Vector2f;

import java.util.List;

/**
 * System that handles buoyancy physics for entities in water.
 * Applies forces based on Archimedes' principle and fluid dynamics.
 */
public class BuoyancySystem extends System {
    
    /** Default water level for testing */
    private float globalWaterLevel = 50.0f;
    
    /** Damping factor for water movement */
    private float waterDamping = 0.95f;
    
    /** Minimum velocity threshold for drag calculations */
    private static final float MIN_VELOCITY_THRESHOLD = 0.01f;
    
    @Override
    public void update(float deltaTime) {
        List<Entity> buoyantEntities = world.getEntitiesWith(
            TransformComponent.class, 
            PhysicsComponent.class, 
            BuoyancyComponent.class
        );
        
        for (Entity entity : buoyantEntities) {
            updateBuoyancy(entity, deltaTime);
        }
    }
    
    /**
     * Updates buoyancy physics for a single entity.
     * 
     * @param entity The entity to update
     * @param deltaTime Time step in seconds
     */
    private void updateBuoyancy(Entity entity, float deltaTime) {
        TransformComponent transform = entity.get(TransformComponent.class);
        PhysicsComponent physics = entity.get(PhysicsComponent.class);
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        
        if (transform == null || physics == null || buoyancy == null) {
            return;
        }
        
        // Estimate entity height from volume (assuming roughly cubic shape)
        float entityHeight = (float) Math.pow(buoyancy.getVolume(), 1.0f / 3.0f);
        
        // Update water interaction state
        buoyancy.updateWaterInteraction(globalWaterLevel, transform.position.y, entityHeight);
        
        if (!buoyancy.isInWater()) {
            return; // No buoyancy forces if not in water
        }
        
        // Calculate and apply buoyancy force
        float buoyancyForce = buoyancy.calculateBuoyancyForce();
        if (buoyancyForce > 0.0f) {
            // Convert force to acceleration (F = ma, so a = F/m)
            float acceleration = buoyancyForce / physics.mass;
            physics.applyForce(new Vector2f(0, acceleration * deltaTime));
        }
        
        // Apply drag forces
        applyDragForces(physics, buoyancy, deltaTime);
        
        // Apply water damping to simulate viscosity
        applyWaterDamping(physics, buoyancy, deltaTime);
    }
    
    /**
     * Applies drag forces to slow down movement in water.
     * 
     * @param physics Physics component
     * @param buoyancy Buoyancy component
     * @param deltaTime Time step
     */
    private void applyDragForces(PhysicsComponent physics, BuoyancyComponent buoyancy, float deltaTime) {
        Vector2f velocity = physics.velocity;
        float speed = velocity.length();
        
        if (speed < MIN_VELOCITY_THRESHOLD) {
            return; // Skip drag for very slow movement
        }
        
        // Calculate drag force magnitude
        float dragForce = buoyancy.calculateDragForce(speed);
        
        if (dragForce > 0.0f) {
            // Apply drag in opposite direction of movement
            Vector2f dragDirection = velocity.copy().normalize().multiply(-1.0f);
            float dragAcceleration = dragForce / physics.mass;
            
            Vector2f dragForceVector = dragDirection.multiply(dragAcceleration * deltaTime);
            physics.applyForce(dragForceVector);
        }
    }
    
    /**
     * Applies general water damping to simulate water viscosity.
     * 
     * @param physics Physics component
     * @param buoyancy Buoyancy component
     * @param deltaTime Time step
     */
    private void applyWaterDamping(PhysicsComponent physics, BuoyancyComponent buoyancy, float deltaTime) {
        if (!buoyancy.isInWater()) {
            return;
        }
        
        // Apply damping based on submersion level
        float dampingFactor = 1.0f - (waterDamping * buoyancy.getSubmersionLevel() * deltaTime);
        dampingFactor = Math.max(0.0f, Math.min(1.0f, dampingFactor));
        
        physics.velocity.multiply(dampingFactor);
    }
    
    /**
     * Sets the global water level for buoyancy calculations.
     * 
     * @param waterLevel New water level
     */
    public void setGlobalWaterLevel(float waterLevel) {
        this.globalWaterLevel = waterLevel;
    }
    
    /**
     * Gets the current global water level.
     * 
     * @return Current water level
     */
    public float getGlobalWaterLevel() {
        return globalWaterLevel;
    }
    
    /**
     * Sets the water damping factor.
     * 
     * @param damping Damping factor (0.0 - 1.0)
     */
    public void setWaterDamping(float damping) {
        this.waterDamping = Math.max(0.0f, Math.min(1.0f, damping));
    }
    
    /**
     * Gets the current water damping factor.
     * 
     * @return Water damping factor
     */
    public float getWaterDamping() {
        return waterDamping;
    }
    
    /**
     * Checks if an entity will float at the current water level.
     * 
     * @param entity Entity to check
     * @return true if the entity will float
     */
    public boolean willEntityFloat(Entity entity) {
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        return buoyancy != null && buoyancy.willFloat();
    }
    
    /**
     * Gets the stability factor for an entity in water.
     * 
     * @param entity Entity to check
     * @return Stability factor (0.0 - 1.0)
     */
    public float getEntityStability(Entity entity) {
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        return buoyancy != null ? buoyancy.getStabilityFactor() : 0.0f;
    }
}