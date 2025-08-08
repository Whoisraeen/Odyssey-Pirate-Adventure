package com.odyssey.game.systems;

import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.System;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.math.Vector2f;

import java.util.List;

/**
 * System that handles physics simulation including movement, gravity, and drag.
 */
public class PhysicsSystem extends System {
    private Vector2f gravity = new Vector2f(0, -9.81f); // Default gravity pointing down
    private float timeScale = 1.0f;
    
    public PhysicsSystem() {
        setPriority(100); // High priority - physics should run early
    }
    
    @Override
    public void update(float deltaTime) {
        float scaledDeltaTime = deltaTime * timeScale;
        
        List<Entity> physicsEntities = world.getEntitiesWith(TransformComponent.class, PhysicsComponent.class);
        
        for (Entity entity : physicsEntities) {
            TransformComponent transform = entity.get(TransformComponent.class);
            PhysicsComponent physics = entity.get(PhysicsComponent.class);
            
            if (physics.isStatic) {
                continue; // Skip static objects
            }
            
            // Apply gravity
            if (physics.affectedByGravity) {
                physics.applyForce(new Vector2f(gravity).multiply(physics.mass));
            }
            
            // Apply drag (air/water resistance)
            if (physics.drag > 0 && !physics.velocity.isZero()) {
                Vector2f dragForce = new Vector2f(physics.velocity)
                    .multiply(-1) // Opposite to velocity direction
                    .normalize()
                    .multiply(physics.drag * physics.velocity.lengthSquared()); // Quadratic drag
                
                physics.applyForce(dragForce);
            }
            
            // Integrate velocity (velocity += acceleration * deltaTime)
            physics.velocity.add(
                physics.acceleration.x * scaledDeltaTime,
                physics.acceleration.y * scaledDeltaTime
            );
            
            // Integrate position (position += velocity * deltaTime)
            transform.position.add(
                physics.velocity.x * scaledDeltaTime,
                physics.velocity.y * scaledDeltaTime
            );
            
            // Clear acceleration for next frame
            physics.acceleration.zero();
        }
    }
    
    /**
     * Set the gravity vector.
     */
    public void setGravity(Vector2f gravity) {
        this.gravity.set(gravity);
    }
    
    /**
     * Set the gravity vector.
     */
    public void setGravity(float x, float y) {
        this.gravity.set(x, y);
    }
    
    /**
     * Get the current gravity vector.
     */
    public Vector2f getGravity() {
        return new Vector2f(gravity);
    }
    
    /**
     * Set the time scale for physics simulation.
     * 1.0 = normal speed, 0.5 = half speed, 2.0 = double speed
     */
    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(0, timeScale);
    }
    
    /**
     * Get the current time scale.
     */
    public float getTimeScale() {
        return timeScale;
    }
    
    /**
     * Apply an explosion force to all physics entities within a radius.
     */
    public void applyExplosion(Vector2f center, float force, float radius) {
        List<Entity> physicsEntities = world.getEntitiesWith(TransformComponent.class, PhysicsComponent.class);
        
        for (Entity entity : physicsEntities) {
            TransformComponent transform = entity.get(TransformComponent.class);
            PhysicsComponent physics = entity.get(PhysicsComponent.class);
            
            if (physics.isStatic) {
                continue;
            }
            
            float distance = transform.position.distance(center);
            if (distance <= radius && distance > 0) {
                // Calculate force direction (away from explosion center)
                Vector2f direction = new Vector2f(transform.position).subtract(center).normalize();
                
                // Calculate force magnitude (inverse square law with minimum distance)
                float effectiveDistance = Math.max(distance, 1.0f);
                float forceMagnitude = force / (effectiveDistance * effectiveDistance);
                
                // Apply the explosion force
                Vector2f explosionForce = new Vector2f(direction).multiply(forceMagnitude);
                physics.applyForce(explosionForce);
            }
        }
    }
    
    /**
     * Apply a constant force field to all physics entities in an area.
     */
    public void applyForceField(Vector2f center, float radius, Vector2f force) {
        List<Entity> physicsEntities = world.getEntitiesWith(TransformComponent.class, PhysicsComponent.class);
        
        for (Entity entity : physicsEntities) {
            TransformComponent transform = entity.get(TransformComponent.class);
            PhysicsComponent physics = entity.get(PhysicsComponent.class);
            
            if (physics.isStatic) {
                continue;
            }
            
            float distance = transform.position.distance(center);
            if (distance <= radius) {
                physics.applyForce(force);
            }
        }
    }
}