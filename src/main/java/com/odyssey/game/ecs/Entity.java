package com.odyssey.game.ecs;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an entity in the Entity Component System.
 * An entity is simply a unique identifier that components can be attached to.
 */
public class Entity {
    private static final AtomicLong nextId = new AtomicLong(1);
    
    private final long id;
    private final World world;
    private boolean alive = true;
    
    public Entity(World world) {
        this.id = nextId.getAndIncrement();
        this.world = world;
    }
    
    /**
     * Get the unique ID of this entity.
     */
    public long getId() {
        return id;
    }
    
    /**
     * Get the world this entity belongs to.
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * Check if this entity is still alive (not destroyed).
     */
    public boolean isAlive() {
        return alive;
    }
    
    /**
     * Mark this entity as destroyed.
     * This is called internally by the world when the entity is destroyed.
     */
    void setAlive(boolean alive) {
        this.alive = alive;
    }
    
    /**
     * Add a component to this entity.
     */
    public <T extends Component> Entity add(T component) {
        world.addComponent(this, component);
        return this;
    }
    
    /**
     * Remove a component from this entity.
     */
    public <T extends Component> Entity remove(Class<T> componentClass) {
        world.removeComponent(this, componentClass);
        return this;
    }
    
    /**
     * Get a component from this entity.
     */
    public <T extends Component> T get(Class<T> componentClass) {
        return world.getComponent(this, componentClass);
    }
    
    /**
     * Check if this entity has a specific component.
     */
    public <T extends Component> boolean has(Class<T> componentClass) {
        return world.hasComponent(this, componentClass);
    }
    
    /**
     * Destroy this entity, removing it from the world.
     */
    public void destroy() {
        world.destroyEntity(this);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Entity entity = (Entity) obj;
        return id == entity.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return "Entity{id=" + id + ", alive=" + alive + "}";
    }
}