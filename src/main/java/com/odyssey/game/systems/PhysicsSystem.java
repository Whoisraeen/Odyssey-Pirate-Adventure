package com.odyssey.game.systems;

import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.System;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import org.joml.Vector3f;

import java.util.List;

/**
 * System that handles physics simulation including movement, gravity, and drag.
 */
public class PhysicsSystem extends System {
    private Vector3f gravity = new Vector3f(0, -9.81f, 0); // Default gravity pointing down
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
                physics.applyForce(new Vector3f(gravity).mul(physics.mass));
            }
            
            // Apply drag (air/water resistance)
            if (physics.drag > 0 && physics.velocity.lengthSquared() > 0) {
                Vector3f dragForce = new Vector3f(physics.velocity)
                    .mul(-1) // Opposite to velocity direction
                    .normalize()
                    .mul(physics.drag * physics.velocity.lengthSquared()); // Quadratic drag
                
                physics.applyForce(dragForce);
            }
            
            // Integrate velocity (velocity += acceleration * deltaTime)
            physics.velocity.add(
                physics.acceleration.x * scaledDeltaTime,
                physics.acceleration.y * scaledDeltaTime,
                physics.acceleration.z * scaledDeltaTime
            );
            
            // Integrate position (position += velocity * deltaTime)
            transform.position.add(
                physics.velocity.x * scaledDeltaTime,
                physics.velocity.y * scaledDeltaTime,
                physics.velocity.z * scaledDeltaTime
            );
            
            // Clear acceleration for next frame
            physics.acceleration.zero();
        }
    }
    
    /**
     * Set the gravity vector.
     */
    public void setGravity(Vector3f gravity) {
        this.gravity.set(gravity);
    }
    
    /**
     * Set the gravity vector.
     */
    public void setGravity(float x, float y, float z) {
        this.gravity.set(x, y, z);
    }
    
    /**
     * Get the current gravity vector.
     */
    public Vector3f getGravity() {
        return new Vector3f(gravity);
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
    public void applyExplosion(Vector3f center, float force, float radius) {
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
                Vector3f direction = new Vector3f(transform.position).sub(center).normalize();
                
                // Calculate force magnitude (inverse square law with minimum distance)
                float effectiveDistance = Math.max(distance, 1.0f);
                float forceMagnitude = force / (effectiveDistance * effectiveDistance);
                
                // Apply the explosion force
                Vector3f explosionForce = new Vector3f(direction).mul(forceMagnitude);
                physics.applyForce(explosionForce);
            }
        }
    }
    
    /**
     * Apply a constant force field to all physics entities in an area.
     */
    public void applyForceField(Vector3f center, float radius, Vector3f force) {
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