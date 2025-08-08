package com.odyssey.game.ecs;

/**
 * Base interface for all components in the Entity Component System.
 * Components are pure data containers that hold state for entities.
 */
public interface Component {
    /**
     * Get the unique type ID for this component type.
     * This is used for efficient component lookup and storage.
     */
    default int getTypeId() {
        return ComponentTypeRegistry.getTypeId(this.getClass());
    }
    
    /**
     * Called when this component is added to an entity.
     * Override to perform initialization logic.
     */
    default void onAttach() {}
    
    /**
     * Called when this component is removed from an entity.
     * Override to perform cleanup logic.
     */
    default void onDetach() {}
    
    /**
     * Create a copy of this component.
     * Used for entity cloning and serialization.
     */
    Component copy();
}