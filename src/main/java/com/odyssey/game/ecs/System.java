package com.odyssey.game.ecs;

/**
 * Base class for all systems in the Entity Component System.
 * Systems contain the logic that operates on entities with specific components.
 */
public abstract class System {
    protected World world;
    private boolean enabled = true;
    private int priority = 0;
    
    /**
     * Initialize the system with a reference to the world.
     */
    public void init(World world) {
        this.world = world;
        onInit();
    }
    
    /**
     * Called when the system is initialized.
     * Override to perform setup logic.
     */
    protected void onInit() {}
    
    /**
     * Update the system. Called every frame.
     * @param deltaTime Time elapsed since last update in seconds
     */
    public abstract void update(float deltaTime);
    
    /**
     * Called when the system is being destroyed.
     * Override to perform cleanup logic.
     */
    public void destroy() {
        onDestroy();
    }
    
    /**
     * Called when the system is being destroyed.
     * Override to perform cleanup logic.
     */
    protected void onDestroy() {}
    
    /**
     * Check if this system is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enable or disable this system.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the priority of this system.
     * Systems with higher priority are updated first.
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Set the priority of this system.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
}