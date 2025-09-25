package com.odyssey.world.entities;

import org.joml.Vector3f;

/**
 * Base class for all entities in the world.
 * Entities are objects that can move, have physics properties, and interact with the world.
 */
public abstract class Entity {
    protected Vector3f position;
    protected Vector3f velocity;
    protected Vector3f rotation;
    protected float mass;
    protected float volume;
    protected boolean isActive;
    protected int entityId;
    
    private static int nextEntityId = 1;
    
    /**
     * Creates a new entity with default properties.
     */
    public Entity() {
        this.entityId = nextEntityId++;
        this.position = new Vector3f(0, 0, 0);
        this.velocity = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.mass = 1.0f;
        this.volume = 1.0f;
        this.isActive = true;
    }
    
    /**
     * Creates a new entity at the specified position.
     */
    public Entity(Vector3f position) {
        this();
        this.position = new Vector3f(position);
    }
    
    /**
     * Updates the entity's state.
     * @param deltaTime Time elapsed since last update in seconds
     */
    public abstract void update(float deltaTime);
    
    /**
     * Renders the entity.
     */
    public abstract void render();
    
    /**
     * Called when the entity is destroyed.
     */
    public void destroy() {
        this.isActive = false;
    }
    
    // Getters
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }
    
    public Vector3f getRotation() {
        return new Vector3f(rotation);
    }
    
    public float getMass() {
        return mass;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getEntityId() {
        return entityId;
    }
    
    // Setters
    public void setPosition(Vector3f position) {
        this.position.set(position);
    }
    
    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }
    
    public void setRotation(Vector3f rotation) {
        this.rotation.set(rotation);
    }
    
    public void setMass(float mass) {
        this.mass = Math.max(0.1f, mass); // Prevent zero or negative mass
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0.1f, volume); // Prevent zero or negative volume
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    /**
     * Applies a force to this entity.
     */
    public void applyForce(Vector3f force) {
        if (mass > 0) {
            Vector3f acceleration = new Vector3f(force).div(mass);
            velocity.add(acceleration.mul(0.016f)); // Assume 60 FPS
        }
    }
    
    /**
     * Checks if this entity collides with another entity.
     */
    public boolean collidesWith(Entity other) {
        if (other == null || !other.isActive()) {
            return false;
        }
        
        // Simple distance-based collision detection
        float distance = position.distance(other.position);
        float combinedRadius = (float) (Math.cbrt(volume) + Math.cbrt(other.volume));
        
        return distance < combinedRadius;
    }
    
    /**
     * Gets the bounding radius of this entity based on its volume.
     */
    public float getBoundingRadius() {
        return (float) Math.cbrt(volume);
    }
    
    @Override
    public String toString() {
        return String.format("Entity[id=%d, pos=(%.2f,%.2f,%.2f), mass=%.2f]", 
                           entityId, position.x, position.y, position.z, mass);
    }
}