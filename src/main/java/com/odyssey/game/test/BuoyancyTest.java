package com.odyssey.game.test;

import com.odyssey.game.ecs.World;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.components.BuoyancyComponent;
import com.odyssey.game.systems.PhysicsSystem;
import com.odyssey.game.systems.BuoyancySystem;
import org.joml.Vector3f;

/**
 * Test class for the buoyancy physics system.
 */
public class BuoyancyTest {
    
    public static void main(String[] args) {
        System.out.println("Testing Buoyancy System...");
        
        // Create world and systems
        World world = new World();
        PhysicsSystem physicsSystem = new PhysicsSystem();
        BuoyancySystem buoyancySystem = new BuoyancySystem();
        
        world.addSystem(physicsSystem);
        world.addSystem(buoyancySystem);
        
        // Set water level
        buoyancySystem.setGlobalWaterLevel(50.0f);
        System.out.println("Water level set to: " + buoyancySystem.getGlobalWaterLevel());
        
        // Test 1: Floating wooden ship (density < water)
        System.out.println("\n=== Test 1: Floating Wooden Ship ===");
        Entity woodenShip = createShip(world, "Wooden Ship", 40.0f, 8.0f, 600.0f); // Wood density ~600 kg/m³
        testFloatingBehavior(world, woodenShip, "Wooden Ship");
        
        // Test 2: Heavy iron ship (density > water, but large volume)
        System.out.println("\n=== Test 2: Iron Ship with Air Chambers ===");
        Entity ironShip = createShip(world, "Iron Ship", 45.0f, 20.0f, 400.0f); // Effective density with air chambers
        testFloatingBehavior(world, ironShip, "Iron Ship");
        
        // Test 3: Dense metal block (will sink)
        System.out.println("\n=== Test 3: Dense Metal Block ===");
        Entity metalBlock = createShip(world, "Metal Block", 55.0f, 2.0f, 7800.0f); // Steel density
        testSinkingBehavior(world, metalBlock, "Metal Block");
        
        // Test 4: Partially submerged object
        System.out.println("\n=== Test 4: Partially Submerged Cork ===");
        Entity cork = createShip(world, "Cork", 49.0f, 1.0f, 240.0f); // Cork density
        testPartialSubmersion(world, cork, "Cork");
        
        // Test 5: Drag and damping effects
        System.out.println("\n=== Test 5: Drag and Damping ===");
        Entity fastShip = createShip(world, "Fast Ship", 45.0f, 5.0f, 500.0f);
        testDragEffects(world, fastShip, "Fast Ship");
        
        // Clean up
        world.clear();
        System.out.println("\nBuoyancy Test completed successfully!");
    }
    
    private static Entity createShip(World world, String name, float yPosition, float volume, float density) {
        Entity ship = world.createEntity()
            .add(new TransformComponent(0, yPosition, 0))
            .add(new PhysicsComponent(density * volume)) // mass = density * volume
            .add(new BuoyancyComponent(volume, density));
        
        System.out.println("Created " + name + ":");
        System.out.println("  Position: (0, " + yPosition + ")");
        System.out.println("  Volume: " + volume + " m³");
        System.out.println("  Density: " + density + " kg/m³");
        System.out.println("  Mass: " + (density * volume) + " kg");
        System.out.println("  Will float: " + ship.get(BuoyancyComponent.class).willFloat());
        
        return ship;
    }
    
    private static void testFloatingBehavior(World world, Entity entity, String name) {
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        TransformComponent transform = entity.get(TransformComponent.class);
        
        System.out.println("\nTesting floating behavior for " + name + ":");
        
        // Simulate for several steps
        for (int i = 0; i < 10; i++) {
            world.update(0.1f); // 100ms steps
            
            if (i % 3 == 0) { // Print every 3rd step
                System.out.printf("  Step %d: Y=%.2f, %s\n", 
                    i, transform.position.y, buoyancy.toString());
            }
        }
        
        // Check final state
        float finalY = transform.position.y;
        boolean isFloating = buoyancy.isInWater() && Math.abs(buoyancy.calculateNetVerticalForce()) < 10.0f;
        
        System.out.println("  Final Y position: " + finalY);
        System.out.println("  Is floating: " + isFloating);
        System.out.println("  Stability factor: " + buoyancy.getStabilityFactor());
    }
    
    private static void testSinkingBehavior(World world, Entity entity, String name) {
        TransformComponent transform = entity.get(TransformComponent.class);
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        
        System.out.println("\nTesting sinking behavior for " + name + ":");
        
        float initialY = transform.position.y;
        
        // Simulate for several steps
        for (int i = 0; i < 8; i++) {
            world.update(0.1f);
            
            if (i % 2 == 0) {
                System.out.printf("  Step %d: Y=%.2f, %s\n", 
                    i, transform.position.y, buoyancy.toString());
            }
        }
        
        float finalY = transform.position.y;
        boolean hasSunk = finalY < initialY - 1.0f;
        
        System.out.println("  Initial Y: " + initialY);
        System.out.println("  Final Y: " + finalY);
        System.out.println("  Has sunk: " + hasSunk);
        System.out.println("  Net vertical force: " + buoyancy.calculateNetVerticalForce() + "N");
    }
    
    private static void testPartialSubmersion(World world, Entity entity, String name) {
        TransformComponent transform = entity.get(TransformComponent.class);
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        
        System.out.println("\nTesting partial submersion for " + name + ":");
        
        // Simulate to equilibrium
        for (int i = 0; i < 15; i++) {
            world.update(0.05f); // Smaller time steps for stability
            
            if (i % 5 == 0) {
                System.out.printf("  Step %d: Y=%.2f, Submersion=%.1f%%, %s\n", 
                    i, transform.position.y, buoyancy.getSubmersionLevel() * 100, 
                    buoyancy.isInWater() ? "In Water" : "Above Water");
            }
        }
        
        System.out.println("  Final submersion level: " + (buoyancy.getSubmersionLevel() * 100) + "%");
        System.out.println("  Buoyancy force: " + buoyancy.calculateBuoyancyForce() + "N");
    }
    
    private static void testDragEffects(World world, Entity entity, String name) {
        PhysicsComponent physics = entity.get(PhysicsComponent.class);
        BuoyancyComponent buoyancy = entity.get(BuoyancyComponent.class);
        
        System.out.println("\nTesting drag effects for " + name + ":");
        
        // Give the ship initial velocity
        physics.velocity.set(10.0f, 0.0f, 0.0f); // 10 m/s horizontal
        System.out.println("  Initial velocity: " + physics.velocity);
        
        // Simulate and observe velocity decay
        for (int i = 0; i < 8; i++) {
            world.update(0.1f);
            
            Vector3f velocity = physics.velocity;
            float speed = velocity.length();
            float dragForce = buoyancy.calculateDragForce(speed);
            
            if (i % 2 == 0) {
                System.out.printf("  Step %d: Speed=%.2f m/s, Drag=%.1fN\n", 
                    i, speed, dragForce);
            }
        }
        
        Vector3f finalVelocity = physics.velocity;
        System.out.println("  Final velocity: " + finalVelocity);
        System.out.println("  Speed reduction: " + (10.0f - finalVelocity.length()) + " m/s");
    }
}