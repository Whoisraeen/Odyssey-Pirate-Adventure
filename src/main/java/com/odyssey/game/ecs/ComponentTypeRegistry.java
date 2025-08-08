package com.odyssey.game.ecs;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry for component types, providing unique IDs for each component class.
 * This enables efficient component storage and lookup in the ECS.
 */
public class ComponentTypeRegistry {
    private static final ConcurrentHashMap<Class<? extends Component>, Integer> typeIds = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger(0);
    
    /**
     * Get the unique type ID for a component class.
     * If the class hasn't been registered yet, it will be assigned a new ID.
     */
    public static int getTypeId(Class<? extends Component> componentClass) {
        return typeIds.computeIfAbsent(componentClass, k -> nextId.getAndIncrement());
    }
    
    /**
     * Get the total number of registered component types.
     */
    public static int getRegisteredTypeCount() {
        return nextId.get();
    }
    
    /**
     * Clear all registered types (primarily for testing).
     */
    public static void clear() {
        typeIds.clear();
        nextId.set(0);
    }
}