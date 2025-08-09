package com.odyssey.world.ocean;

import com.odyssey.game.components.*;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.World;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Factory class for creating marine creatures with appropriate components
 * and AI behaviors. This class integrates the ECS system with the marine
 * ecosystem simulation.
 */
public class MarineCreatureFactory {
    private static final Logger logger = LoggerFactory.getLogger(MarineCreatureFactory.class);
    
    private final World world;
    private final Random random;
    
    public MarineCreatureFactory(World world) {
        this.world = world;
        this.random = new Random();
    }
    
    /**
     * Create a marine creature entity with all necessary components.
     */
    public Entity createMarineCreature(MarineEcosystem.SpeciesType species, Vector3f position) {
        Entity creature = world.createEntity();
        
        // Add transform component with 3D positioning
        TransformComponent transform = new TransformComponent();
        transform.position.set(position);
        transform.rotation.set(0.0f, 0.0f, 0.0f); // Default rotation
        Vector3f scale3f = getSpeciesScale(species);
        transform.scale.set(scale3f);
        creature.add(transform);
        
        // Add physics component with 3D physics
        PhysicsComponent physics = new PhysicsComponent();
        physics.mass = getSpeciesMass(species);
        physics.drag = getSpeciesDrag(species);
        physics.velocity.set(0, 0, 0); // Vector3f initialization
        physics.acceleration.set(0, 0, 0); // Vector3f initialization
        physics.isUnderwater = true; // Marine creatures are underwater
        physics.buoyancy = getSpeciesBuoyancy(species);
        creature.add(physics);
        
        // Add health component
        HealthComponent health = new HealthComponent();
        health.maxHealth = getSpeciesMaxHealth(species);
        health.currentHealth = health.maxHealth;
        health.regenRate = getSpeciesRegenRate(species);
        creature.add(health);
        
        // Add marine AI component
        MarineAIComponent aiComponent = new MarineAIComponent(species);
        addSpeciesVariation(aiComponent, species);
        creature.add(aiComponent);
        
        // Add renderable component
        RenderableComponent renderable = new RenderableComponent();
        renderable.texturePath = getSpeciesTexturePath(species);
        renderable.visible = true;
        creature.add(renderable);
        
        // Add buoyancy component for underwater physics
        float speciesDensity = getSpeciesDensity(species);
        float speciesVolume = getSpeciesVolume(species);
        BuoyancyComponent buoyancy = new BuoyancyComponent(speciesVolume, speciesDensity);
        creature.add(buoyancy);
        
        // Add collider component for collision detection
        Vector3f bounds = getSpeciesBounds(species);
        ColliderComponent collider = ColliderComponent.createBox(bounds);
        collider.collisionLayer = "marine_creature";
        creature.add(collider);
        
        logger.debug("Created {} creature at position {}", species, position);
        return creature;
    }
    
    /**
     * Create a school of fish at the specified location.
     */
    public void createSchool(MarineEcosystem.SpeciesType species, Vector3f centerPosition, 
                           int count, float radius) {
        
        if (!isSchoolingSpecies(species)) {
            logger.warn("Attempted to create school of non-schooling species: {}", species);
            return;
        }
        
        for (int i = 0; i < count; i++) {
            // Generate random position within school radius
            float angle = random.nextFloat() * 2.0f * (float) Math.PI;
            float distance = random.nextFloat() * radius;
            float height = (random.nextFloat() - 0.5f) * 2.0f; // Small vertical spread
            
            Vector3f position = new Vector3f(
                centerPosition.x + (float) Math.cos(angle) * distance,
                centerPosition.y + height,
                centerPosition.z + (float) Math.sin(angle) * distance
            );
            
            Entity creature = createMarineCreature(species, position);
            
            // Set initial schooling behavior
            MarineAIComponent aiComp = creature.get(MarineAIComponent.class);
            if (aiComp != null) {
                aiComp.setBehavior(MarineAIComponent.BehaviorType.SCHOOLING);
                aiComp.socialNeed = 0.8f + random.nextFloat() * 0.2f; // High social need
            }
        }
        
        logger.info("Created school of {} {} creatures at {}", count, species, centerPosition);
    }
    
    /**
     * Create a predator with hunting behavior.
     */
    public Entity createPredator(MarineEcosystem.SpeciesType species, Vector3f position) {
        Entity predator = createMarineCreature(species, position);
        
        MarineAIComponent aiComp = predator.get(MarineAIComponent.class);
        if (aiComp != null && aiComp.isPredator) {
            aiComp.setBehavior(MarineAIComponent.BehaviorType.HUNTING);
            aiComp.aggressionLevel = 0.7f + random.nextFloat() * 0.3f;
            aiComp.hungerLevel = 0.6f + random.nextFloat() * 0.4f;
        }
        
        return predator;
    }
    
    /**
     * Create a territorial creature.
     */
    public Entity createTerritorialCreature(MarineEcosystem.SpeciesType species, Vector3f position) {
        Entity creature = createMarineCreature(species, position);
        
        MarineAIComponent aiComp = creature.get(MarineAIComponent.class);
        if (aiComp != null && aiComp.isTerritorial) {
            aiComp.setBehavior(MarineAIComponent.BehaviorType.TERRITORIAL_PATROL);
            aiComp.territorialInstinct = 0.8f + random.nextFloat() * 0.2f;
        }
        
        return creature;
    }
    
    /**
     * Add random variation to AI parameters for more natural behavior.
     */
    private void addSpeciesVariation(MarineAIComponent aiComp, MarineEcosystem.SpeciesType species) {
        // Add 10-20% variation to base parameters
        float variation = 0.1f + random.nextFloat() * 0.1f;
        
        aiComp.maxSpeed *= (1.0f + (random.nextFloat() - 0.5f) * variation);
        aiComp.agility *= (1.0f + (random.nextFloat() - 0.5f) * variation);
        aiComp.perceptionRadius *= (1.0f + (random.nextFloat() - 0.5f) * variation);
        
        // Add personality traits
        aiComp.aggressionLevel += (random.nextFloat() - 0.5f) * 0.2f;
        aiComp.fearLevel += (random.nextFloat() - 0.5f) * 0.2f;
        aiComp.socialNeed += (random.nextFloat() - 0.5f) * 0.2f;
        
        // Clamp values to valid ranges
        aiComp.aggressionLevel = Math.max(0.0f, Math.min(1.0f, aiComp.aggressionLevel));
        aiComp.fearLevel = Math.max(0.0f, Math.min(1.0f, aiComp.fearLevel));
        aiComp.socialNeed = Math.max(0.0f, Math.min(1.0f, aiComp.socialNeed));
    }
    
    /**
     * Get the scale factor for a species.
     */
    private Vector3f getSpeciesScale(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> new Vector3f(8.0f, 8.0f, 8.0f);
            case SHARK -> new Vector3f(3.0f, 3.0f, 3.0f);
            case DOLPHIN -> new Vector3f(2.0f, 2.0f, 2.0f);
            case SEA_TURTLE -> new Vector3f(1.5f, 1.5f, 1.5f);
            case OCTOPUS -> new Vector3f(1.2f, 1.2f, 1.2f);
            case SMALL_FISH -> new Vector3f(0.3f, 0.3f, 0.3f);
            case MEDIUM_FISH -> new Vector3f(0.5f, 0.5f, 0.5f);
            case LARGE_FISH -> new Vector3f(0.8f, 0.8f, 0.8f);
            case JELLYFISH -> new Vector3f(0.8f, 0.8f, 0.8f);
            default -> new Vector3f(1.0f, 1.0f, 1.0f);
        };
    }
    
    /**
     * Get the mass for a species.
     */
    private float getSpeciesMass(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 50000.0f;
            case SHARK -> 500.0f;
            case DOLPHIN -> 200.0f;
            case SEA_TURTLE -> 100.0f;
            case OCTOPUS -> 10.0f;
            case SMALL_FISH -> 0.5f;
            case MEDIUM_FISH -> 2.0f;
            case LARGE_FISH -> 5.0f;
            case JELLYFISH -> 1.0f;
            default -> 5.0f;
        };
    }
    
    /**
     * Get the drag coefficient for a species.
     */
    private float getSpeciesDrag(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 0.8f;
            case SHARK -> 0.3f;
            case DOLPHIN -> 0.2f;
            case SEA_TURTLE -> 0.6f;
            case OCTOPUS -> 0.4f;
            case SMALL_FISH, MEDIUM_FISH, LARGE_FISH -> 0.3f;
            case JELLYFISH -> 0.9f;
            default -> 0.5f;
        };
    }
    
    /**
     * Get the buoyancy for a species.
     */
    private float getSpeciesBuoyancy(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 0.95f;
            case SHARK -> 1.02f;
            case DOLPHIN -> 0.98f;
            case SEA_TURTLE -> 1.05f;
            case OCTOPUS -> 1.0f;
            case SMALL_FISH, MEDIUM_FISH, LARGE_FISH -> 1.01f;
            case JELLYFISH -> 0.99f;
            default -> 1.0f;
        };
    }
    
    /**
     * Get the collider radius for a species.
     */
    private float getSpeciesColliderRadius(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 6.0f;
            case SHARK -> 2.0f;
            case DOLPHIN -> 1.5f;
            case SEA_TURTLE -> 1.0f;
            case OCTOPUS -> 0.8f;
            case SMALL_FISH -> 0.2f;
            case MEDIUM_FISH -> 0.3f;
            case LARGE_FISH -> 0.5f;
            case JELLYFISH -> 0.6f;
            default -> 0.5f;
        };
    }
    
    /**
     * Get the maximum health for a species.
     */
    private float getSpeciesMaxHealth(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 1000.0f;
            case SHARK -> 200.0f;
            case DOLPHIN -> 150.0f;
            case SEA_TURTLE -> 100.0f;
            case OCTOPUS -> 50.0f;
            case SMALL_FISH -> 10.0f;
            case MEDIUM_FISH -> 20.0f;
            case LARGE_FISH -> 40.0f;
            case JELLYFISH -> 10.0f;
            default -> 50.0f;
        };
    }
    
    /**
     * Get the health regeneration rate for a species.
     */
    private float getSpeciesRegenRate(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 2.0f;
            case SHARK -> 1.5f;
            case DOLPHIN -> 1.2f;
            case SEA_TURTLE -> 0.8f;
            case OCTOPUS -> 1.0f;
            case SMALL_FISH -> 0.3f;
            case MEDIUM_FISH -> 0.5f;
            case LARGE_FISH -> 0.8f;
            case JELLYFISH -> 0.3f;
            default -> 1.0f;
        };
    }
    
    /**
     * Get the density for buoyancy calculations.
     */
    private float getSpeciesDensity(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 0.9f; // Slightly less dense than water
            case SHARK -> 1.05f; // Slightly denser than water
            case DOLPHIN -> 0.95f;
            case SEA_TURTLE -> 1.1f;
            case OCTOPUS -> 1.0f;
            case SMALL_FISH, MEDIUM_FISH, LARGE_FISH -> 1.02f;
            case JELLYFISH -> 0.98f;
            default -> 1.0f;
        };
    }
    
    /**
     * Get the volume for buoyancy calculations.
     */
    private float getSpeciesVolume(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> 100.0f;
            case SHARK -> 8.0f;
            case DOLPHIN -> 5.0f;
            case SEA_TURTLE -> 3.0f;
            case OCTOPUS -> 2.0f;
            case SMALL_FISH -> 0.2f;
            case MEDIUM_FISH -> 0.5f;
            case LARGE_FISH -> 1.0f;
            case JELLYFISH -> 1.0f;
            default -> 1.0f;
        };
    }
    
    /**
     * Get the collision bounds for a species.
     */
    private Vector3f getSpeciesBounds(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> new Vector3f(12.0f, 4.0f, 3.0f);
            case SHARK -> new Vector3f(4.0f, 1.5f, 1.0f);
            case DOLPHIN -> new Vector3f(3.0f, 1.0f, 0.8f);
            case SEA_TURTLE -> new Vector3f(1.5f, 0.8f, 1.2f);
            case OCTOPUS -> new Vector3f(1.0f, 1.0f, 1.0f);
            case SMALL_FISH -> new Vector3f(0.2f, 0.1f, 0.05f);
            case MEDIUM_FISH -> new Vector3f(0.3f, 0.2f, 0.1f);
            case LARGE_FISH -> new Vector3f(0.5f, 0.3f, 0.2f);
            case JELLYFISH -> new Vector3f(0.8f, 1.2f, 0.8f);
            default -> new Vector3f(1.0f, 1.0f, 1.0f);
        };
    }
    
    /**
     * Get the model path for a species.
     */
    private String getSpeciesModelPath(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> "models/marine/whale.obj";
            case SHARK -> "models/marine/shark.obj";
            case DOLPHIN -> "models/marine/dolphin.obj";
            case SEA_TURTLE -> "models/marine/turtle.obj";
            case OCTOPUS -> "models/marine/octopus.obj";
            case SMALL_FISH -> "models/marine/small_fish.obj";
            case MEDIUM_FISH -> "models/marine/medium_fish.obj";
            case LARGE_FISH -> "models/marine/large_fish.obj";
            case JELLYFISH -> "models/marine/jellyfish.obj";
            default -> "models/marine/generic_fish.obj";
        };
    }
    
    /**
     * Get the texture path for a species.
     */
    private String getSpeciesTexturePath(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case WHALE -> "textures/marine/whale.png";
            case SHARK -> "textures/marine/shark.png";
            case DOLPHIN -> "textures/marine/dolphin.png";
            case SEA_TURTLE -> "textures/marine/turtle.png";
            case OCTOPUS -> "textures/marine/octopus.png";
            case SMALL_FISH -> "textures/marine/small_fish.png";
            case MEDIUM_FISH -> "textures/marine/medium_fish.png";
            case LARGE_FISH -> "textures/marine/large_fish.png";
            case JELLYFISH -> "textures/marine/jellyfish.png";
            default -> "textures/marine/generic_fish.png";
        };
    }
    
    /**
     * Check if a species forms schools.
     */
    private boolean isSchoolingSpecies(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case SMALL_FISH, MEDIUM_FISH, LARGE_FISH, DOLPHIN -> true;
            case WHALE, SHARK, SEA_TURTLE, OCTOPUS, JELLYFISH -> false;
            default -> false;
        };
    }
    
    /**
     * Create a diverse marine ecosystem in a given area.
     */
    public void populateArea(Vector3f center, float radius, int totalCreatures) {
        logger.info("Populating marine area at {} with radius {} and {} creatures", 
                   center, radius, totalCreatures);
        
        // Distribution percentages for different species
        int smallFishCount = (int) (totalCreatures * 0.4f); // 40% small fish
        int mediumFishCount = (int) (totalCreatures * 0.15f); // 15% medium fish
        int largeFishCount = (int) (totalCreatures * 0.05f); // 5% large fish
        int sharkCount = (int) (totalCreatures * 0.05f); // 5% sharks
        int dolphinCount = (int) (totalCreatures * 0.1f); // 10% dolphins
        int jellyfishCount = (int) (totalCreatures * 0.15f); // 15% jellyfish
        int turtleCount = (int) (totalCreatures * 0.03f); // 3% turtles
        int octopusCount = (int) (totalCreatures * 0.02f); // 2% octopus
        int whaleCount = (int) (totalCreatures * 0.01f); // 1% whales
        
        // Create schools of fish
        int smallSchoolCount = Math.max(1, smallFishCount / 20); // Schools of ~20 small fish
        for (int i = 0; i < smallSchoolCount; i++) {
            Vector3f schoolPos = getRandomPositionInRadius(center, radius);
            int schoolSize = Math.min(20, smallFishCount / smallSchoolCount);
            createSchool(MarineEcosystem.SpeciesType.SMALL_FISH, schoolPos, schoolSize, 5.0f);
        }
        
        int mediumSchoolCount = Math.max(1, mediumFishCount / 15); // Schools of ~15 medium fish
        for (int i = 0; i < mediumSchoolCount; i++) {
            Vector3f schoolPos = getRandomPositionInRadius(center, radius);
            int schoolSize = Math.min(15, mediumFishCount / mediumSchoolCount);
            createSchool(MarineEcosystem.SpeciesType.MEDIUM_FISH, schoolPos, schoolSize, 4.0f);
        }
        
        // Create individual creatures
        createRandomCreatures(MarineEcosystem.SpeciesType.LARGE_FISH, center, radius, largeFishCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.SHARK, center, radius, sharkCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.DOLPHIN, center, radius, dolphinCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.JELLYFISH, center, radius, jellyfishCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.SEA_TURTLE, center, radius, turtleCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.OCTOPUS, center, radius, octopusCount);
        createRandomCreatures(MarineEcosystem.SpeciesType.WHALE, center, radius, whaleCount);
        
        logger.info("Marine ecosystem population complete");
    }
    
    /**
     * Create random creatures of a specific species in an area.
     */
    private void createRandomCreatures(MarineEcosystem.SpeciesType species, Vector3f center, 
                                     float radius, int count) {
        for (int i = 0; i < count; i++) {
            Vector3f position = getRandomPositionInRadius(center, radius);
            createMarineCreature(species, position);
        }
    }
    
    /**
     * Get a random position within a radius of a center point.
     */
    private Vector3f getRandomPositionInRadius(Vector3f center, float radius) {
        float angle = random.nextFloat() * 2.0f * (float) Math.PI;
        float distance = random.nextFloat() * radius;
        float depth = random.nextFloat() * 20.0f - 10.0f; // Depth variation
        
        return new Vector3f(
            center.x + (float) Math.cos(angle) * distance,
            center.y + depth,
            center.z + (float) Math.sin(angle) * distance
        );
    }
}