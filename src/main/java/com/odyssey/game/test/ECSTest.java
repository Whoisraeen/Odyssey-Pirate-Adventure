package com.odyssey.game.test;

import com.odyssey.game.ecs.World;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.components.HealthComponent;
import com.odyssey.game.components.RenderableComponent;
import com.odyssey.game.systems.PhysicsSystem;
import org.joml.Vector3f;

/**
 * Simple test class to verify the ECS system is working correctly.
 */
public class ECSTest {
    
    public static void main(String[] args) {
        System.out.println("Testing ECS System...");
        
        // Create a world
        World world = new World();
        
        // Add physics system
        PhysicsSystem physicsSystem = new PhysicsSystem();
        world.addSystem(physicsSystem);
        
        // Create a test entity
        Entity testEntity = world.createEntity()
            .add(new TransformComponent(10, 20, 0))
            .add(new PhysicsComponent(5.0f))
            .add(new HealthComponent(100.0f))
            .add(new RenderableComponent("test_texture.png"));
        
        System.out.println("Created entity: " + testEntity);
        
        // Test component access
        TransformComponent transform = testEntity.get(TransformComponent.class);
        PhysicsComponent physics = testEntity.get(PhysicsComponent.class);
        HealthComponent health = testEntity.get(HealthComponent.class);
        RenderableComponent renderable = testEntity.get(RenderableComponent.class);
        
        System.out.println("Initial state:");
        System.out.println("  " + transform);
        System.out.println("  " + physics);
        System.out.println("  " + health);
        System.out.println("  " + renderable);
        
        // Apply some forces
        physics.applyForce(new Vector3f(100, 0, 0)); // Push right
        physics.applyForce(new Vector3f(0, 50, 0));  // Push up
        
        System.out.println("\nAfter applying forces:");
        System.out.println("  " + physics);
        
        // Simulate a few physics steps
        System.out.println("\nSimulating physics for 3 steps (0.016s each):");
        for (int i = 0; i < 3; i++) {
            world.update(0.016f); // ~60 FPS
            System.out.println("  Step " + (i + 1) + ": " + transform);
        }
        
        // Test health system
        System.out.println("\nTesting health system:");
        System.out.println("  Before damage: " + health);
        health.takeDamage(25, 0);
        System.out.println("  After 25 damage: " + health);
        health.heal(10);
        System.out.println("  After 10 healing: " + health);
        
        // Test entity queries
        System.out.println("\nTesting entity queries:");
        var physicsEntities = world.getEntitiesWith(TransformComponent.class, PhysicsComponent.class);
        System.out.println("  Entities with Transform + Physics: " + physicsEntities.size());
        
        var renderableEntities = world.getEntitiesWith(RenderableComponent.class);
        System.out.println("  Entities with Renderable: " + renderableEntities.size());
        
        // Test entity destruction
        System.out.println("\nTesting entity destruction:");
        System.out.println("  Entities before destruction: " + world.getEntityCount());
        testEntity.destroy();
        world.update(0.016f); // Process destruction
        System.out.println("  Entities after destruction: " + world.getEntityCount());
        
        // Clean up
        world.clear();
        
        System.out.println("\nECS Test completed successfully!");
    }
}