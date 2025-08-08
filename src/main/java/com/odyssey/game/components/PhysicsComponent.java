package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;
import org.joml.Vector3f;

/**
 * Component that handles physics properties like velocity, acceleration, and mass in 3D space.
 * Upgraded to support 3D physics for marine ecosystem and world simulation.
 */
public class PhysicsComponent implements Component {
    public Vector3f velocity;
    public Vector3f acceleration;
    public float mass;
    public float drag; // Air/water resistance
    public float restitution; // Bounciness (0 = no bounce, 1 = perfect bounce)
    public boolean isStatic; // If true, object doesn't move due to physics
    public boolean affectedByGravity;
    public boolean isUnderwater; // Special flag for marine creatures
    public float buoyancy; // Buoyancy force for underwater physics
    
    public PhysicsComponent() {
        this(1.0f);
    }
    
    public PhysicsComponent(float mass) {
        this.velocity = new Vector3f();
        this.acceleration = new Vector3f();
        this.mass = mass;
        this.drag = 0.01f;
        this.restitution = 0.3f;
        this.isStatic = false;
        this.affectedByGravity = true;
        this.isUnderwater = false;
        this.buoyancy = 1.0f;
    }
    
    /**
     * Apply a force to this physics body.
     * Force = mass * acceleration, so acceleration = force / mass
     */
    public void applyForce(Vector3f force) {
        if (!isStatic && mass > 0) {
            Vector3f forcePerMass = new Vector3f(force).div(mass);
            acceleration.add(forcePerMass);
        }
    }
    
    /**
     * Apply a force to this physics body.
     */
    public void applyForce(float forceX, float forceY, float forceZ) {
        if (!isStatic && mass > 0) {
            acceleration.add(forceX / mass, forceY / mass, forceZ / mass);
        }
    }
    
    /**
     * Apply an impulse (instant velocity change) to this physics body.
     */
    public void applyImpulse(Vector3f impulse) {
        if (!isStatic && mass > 0) {
            Vector3f impulsePerMass = new Vector3f(impulse).div(mass);
            velocity.add(impulsePerMass);
        }
    }
    
    /**
     * Apply an impulse (instant velocity change) to this physics body.
     */
    public void applyImpulse(float impulseX, float impulseY, float impulseZ) {
        if (!isStatic && mass > 0) {
            velocity.add(impulseX / mass, impulseY / mass, impulseZ / mass);
        }
    }
    
    /**
     * Apply buoyancy force for underwater entities.
     */
    public void applyBuoyancy(float waterDensity, float submergedVolume) {
        if (isUnderwater && !isStatic) {
            // Archimedes' principle: buoyant force = weight of displaced fluid
            float buoyantForce = waterDensity * submergedVolume * 9.81f * buoyancy;
            applyForce(0, buoyantForce, 0);
        }
    }
    
    /**
     * Get the kinetic energy of this physics body.
     */
    public float getKineticEnergy() {
        return 0.5f * mass * velocity.lengthSquared();
    }
    
    /**
     * Get the momentum of this physics body.
     */
    public Vector3f getMomentum() {
        return new Vector3f(velocity).mul(mass);
    }
    
    /**
     * Get the speed (magnitude of velocity).
     */
    public float getSpeed() {
        return velocity.length();
    }
    
    /**
     * Stop all movement.
     */
    public void stop() {
        velocity.zero();
        acceleration.zero();
    }
    
    /**
     * Limit velocity to a maximum speed.
     */
    public void limitSpeed(float maxSpeed) {
        if (velocity.length() > maxSpeed) {
            velocity.normalize().mul(maxSpeed);
        }
    }
    
    @Override
    public Component copy() {
        PhysicsComponent copy = new PhysicsComponent(mass);
        copy.velocity.set(velocity);
        copy.acceleration.set(acceleration);
        copy.drag = drag;
        copy.restitution = restitution;
        copy.isStatic = isStatic;
        copy.affectedByGravity = affectedByGravity;
        copy.isUnderwater = isUnderwater;
        copy.buoyancy = buoyancy;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Physics{vel=%.2f,%.2f,%.2f, mass=%.2f, static=%s, underwater=%s}", 
                           velocity.x, velocity.y, velocity.z, mass, isStatic, isUnderwater);
    }
}