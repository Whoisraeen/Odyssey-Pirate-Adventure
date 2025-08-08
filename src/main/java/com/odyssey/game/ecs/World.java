package com.odyssey.game.ecs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The World manages all entities, components, and systems in the ECS.
 * It provides the main interface for creating entities and managing the game state.
 */
public class World {
    private final Map<Long, Entity> entities = new ConcurrentHashMap<>();
    private final Map<Long, Map<Class<? extends Component>, Component>> entityComponents = new ConcurrentHashMap<>();
    private final List<System> systems = new ArrayList<>();
    private final Set<Long> entitiesToDestroy = ConcurrentHashMap.newKeySet();
    
    /**
     * Create a new entity in this world.
     */
    public Entity createEntity() {
        Entity entity = new Entity(this);
        entities.put(entity.getId(), entity);
        entityComponents.put(entity.getId(), new ConcurrentHashMap<>());
        return entity;
    }
    
    /**
     * Destroy an entity, removing it and all its components from the world.
     */
    public void destroyEntity(Entity entity) {
        if (entity != null && entities.containsKey(entity.getId())) {
            entitiesToDestroy.add(entity.getId());
        }
    }
    
    /**
     * Add a component to an entity.
     */
    public <T extends Component> void addComponent(Entity entity, T component) {
        if (entity != null && entities.containsKey(entity.getId())) {
            Map<Class<? extends Component>, Component> components = entityComponents.get(entity.getId());
            if (components != null) {
                components.put(component.getClass(), component);
                component.onAttach();
            }
        }
    }
    
    /**
     * Remove a component from an entity.
     */
    public <T extends Component> void removeComponent(Entity entity, Class<T> componentClass) {
        if (entity != null && entities.containsKey(entity.getId())) {
            Map<Class<? extends Component>, Component> components = entityComponents.get(entity.getId());
            if (components != null) {
                Component component = components.remove(componentClass);
                if (component != null) {
                    component.onDetach();
                }
            }
        }
    }
    
    /**
     * Get a component from an entity.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
        if (entity != null && entities.containsKey(entity.getId())) {
            Map<Class<? extends Component>, Component> components = entityComponents.get(entity.getId());
            if (components != null) {
                return (T) components.get(componentClass);
            }
        }
        return null;
    }
    
    /**
     * Check if an entity has a specific component.
     */
    public <T extends Component> boolean hasComponent(Entity entity, Class<T> componentClass) {
        if (entity != null && entities.containsKey(entity.getId())) {
            Map<Class<? extends Component>, Component> components = entityComponents.get(entity.getId());
            return components != null && components.containsKey(componentClass);
        }
        return false;
    }
    
    /**
     * Get all entities that have all of the specified component types.
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWith(Class<? extends Component>... componentTypes) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities.values()) {
            if (entity.isAlive() && hasAllComponents(entity, componentTypes)) {
                result.add(entity);
            }
        }
        return result;
    }
    
    /**
     * Check if an entity has all of the specified component types.
     */
    @SafeVarargs
    private boolean hasAllComponents(Entity entity, Class<? extends Component>... componentTypes) {
        for (Class<? extends Component> componentType : componentTypes) {
            if (!hasComponent(entity, componentType)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Add a system to this world.
     */
    public void addSystem(System system) {
        system.init(this);
        systems.add(system);
        systems.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    /**
     * Remove a system from this world.
     */
    public void removeSystem(System system) {
        system.destroy();
        systems.remove(system);
    }
    
    /**
     * Update all systems in this world.
     */
    public void update(float deltaTime) {
        // Process entity destruction
        processEntityDestruction();
        
        // Update all enabled systems
        for (System system : systems) {
            if (system.isEnabled()) {
                system.update(deltaTime);
            }
        }
    }
    
    /**
     * Process entities marked for destruction.
     */
    private void processEntityDestruction() {
        for (Long entityId : entitiesToDestroy) {
            Entity entity = entities.get(entityId);
            if (entity != null) {
                // Remove all components
                Map<Class<? extends Component>, Component> components = entityComponents.get(entityId);
                if (components != null) {
                    for (Component component : components.values()) {
                        component.onDetach();
                    }
                    components.clear();
                }
                
                // Remove entity
                entity.setAlive(false);
                entities.remove(entityId);
                entityComponents.remove(entityId);
            }
        }
        entitiesToDestroy.clear();
    }
    
    /**
     * Get all entities in this world.
     */
    public Collection<Entity> getAllEntities() {
        return entities.values();
    }
    
    /**
     * Get the number of entities in this world.
     */
    public int getEntityCount() {
        return entities.size();
    }
    
    /**
     * Clear all entities and systems from this world.
     */
    public void clear() {
        // Destroy all systems
        for (System system : systems) {
            system.destroy();
        }
        systems.clear();
        
        // Clear all entities and components
        for (Map<Class<? extends Component>, Component> components : entityComponents.values()) {
            for (Component component : components.values()) {
                component.onDetach();
            }
        }
        
        entities.clear();
        entityComponents.clear();
        entitiesToDestroy.clear();
    }
}