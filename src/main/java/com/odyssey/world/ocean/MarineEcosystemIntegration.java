package com.odyssey.world.ocean;

import com.odyssey.game.components.MarineAIComponent;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.World;
import com.odyssey.game.systems.MarineAISystem;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration class that bridges the existing MarineEcosystem simulation
 * with the new ECS-based AI system. This class manages the conversion
 * between the old creature system and the new entity-based system.
 */
public class MarineEcosystemIntegration {
    private static final Logger logger = LoggerFactory.getLogger(MarineEcosystemIntegration.class);
    
    private final World world;
    private final MarineEcosystem ecosystem;
    private final MarineCreatureFactory creatureFactory;
    private final MarineAISystem aiSystem;
    
    // Mapping between old creatures and new entities
    private final Map<UUID, Entity> creatureToEntityMap = new ConcurrentHashMap<>();
    private final Map<Entity, UUID> entityToCreatureMap = new ConcurrentHashMap<>();
    
    // Migration state
    private boolean isInitialized = false;
    private float migrationProgress = 0.0f;
    
    public MarineEcosystemIntegration(World world, MarineEcosystem ecosystem) {
        this.world = world;
        this.ecosystem = ecosystem;
        this.creatureFactory = new MarineCreatureFactory(world);
        this.aiSystem = new MarineAISystem();
        
        // Add the AI system to the world
        world.addSystem(aiSystem);
    }
    
    /**
     * Initialize the integration by migrating existing creatures to ECS entities.
     */
    public void initialize() {
        if (isInitialized) {
            logger.warn("MarineEcosystemIntegration already initialized");
            return;
        }
        
        logger.info("Initializing marine ecosystem integration");
        
        // Migrate existing creatures to ECS entities
        migrateCreaturesToEntities();
        
        // Set up periodic synchronization
        setupSynchronization();
        
        isInitialized = true;
        logger.info("Marine ecosystem integration initialized successfully");
    }
    
    /**
     * Update the integration system.
     */
    public void update(double deltaTime) {
        if (!isInitialized) {
            return;
        }
        
        // Synchronize creature data between old and new systems
        synchronizeCreatureData();
        
        // Handle creature lifecycle events
        handleCreatureLifecycle();
        
        // Update migration progress
        migrationProgress += (float) deltaTime * 0.1f; // Slow migration
        if (migrationProgress >= 1.0f) {
            migrationProgress = 0.0f;
        }
    }
    
    /**
     * Migrate existing MarineCreature objects to ECS entities.
     */
    private void migrateCreaturesToEntities() {
        logger.info("Migrating creatures to ECS entities");
        
        int migratedCount = 0;
        
        // Iterate through all species populations
        for (Map.Entry<MarineEcosystem.SpeciesType, List<MarineEcosystem.MarineCreature>> entry : 
             ecosystem.getSpeciesPopulations().entrySet()) {
            
            MarineEcosystem.SpeciesType species = entry.getKey();
            List<MarineEcosystem.MarineCreature> creatures = entry.getValue();
            
            for (MarineEcosystem.MarineCreature creature : creatures) {
                if (creature.isAlive) {
                    Entity entity = migrateCreatureToEntity(creature, species);
                    if (entity != null) {
                        migratedCount++;
                    }
                }
            }
        }
        
        logger.info("Migrated {} creatures to ECS entities", migratedCount);
    }
    
    /**
     * Migrate a single creature to an ECS entity.
     */
    private Entity migrateCreatureToEntity(MarineEcosystem.MarineCreature creature, 
                                         MarineEcosystem.SpeciesType species) {
        try {
            // Create entity with all necessary components
            Entity entity = creatureFactory.createMarineCreature(species, creature.position);
            
            // Synchronize creature data with entity components
            syncCreatureToEntity(creature, entity);
            
            // Store mappings
            creatureToEntityMap.put(creature.id, entity);
            entityToCreatureMap.put(entity, creature.id);
            
            return entity;
        } catch (Exception e) {
            logger.error("Failed to migrate creature {} to entity", creature.id, e);
            return null;
        }
    }
    
    /**
     * Synchronize creature data to entity components.
     */
    private void syncCreatureToEntity(MarineEcosystem.MarineCreature creature, Entity entity) {
        // Update transform with direct 3D position mapping
        TransformComponent transform = entity.get(TransformComponent.class);
        if (transform != null) {
            transform.position.set(creature.position);
        }
        
        // Update physics with direct 3D velocity mapping
        PhysicsComponent physics = entity.get(PhysicsComponent.class);
        if (physics != null) {
            physics.velocity.set(creature.velocity);
            physics.isUnderwater = true; // Marine creatures are underwater
        }
        
        // Update AI component
        MarineAIComponent aiComp = entity.get(MarineAIComponent.class);
        if (aiComp != null) {
            // Map old behavior to new behavior
            aiComp.setBehavior(mapBehavior(creature.behavior));
            aiComp.hungerLevel = creature.hunger;
            aiComp.energy = creature.health;
            
            // Set age-based parameters
            float ageRatio = creature.age / getSpeciesMaxAge(creature.species);
            aiComp.agility *= (1.0f - ageRatio * 0.3f); // Older creatures are less agile
            aiComp.maxSpeed *= (1.0f - ageRatio * 0.2f); // Older creatures are slower
        }
    }
    
    /**
     * Synchronize entity data back to creature objects.
     */
    private void syncEntityToCreature(Entity entity, MarineEcosystem.MarineCreature creature) {
        // Update position from transform with direct 3D mapping
        TransformComponent transform = entity.get(TransformComponent.class);
        if (transform != null) {
            creature.position.set(transform.position);
        }
        
        // Update velocity from physics with direct 3D mapping
        PhysicsComponent physics = entity.get(PhysicsComponent.class);
        if (physics != null) {
            creature.velocity.set(physics.velocity);
        }
        
        // Update creature state from AI component
        MarineAIComponent aiComp = entity.get(MarineAIComponent.class);
        if (aiComp != null) {
            creature.behavior = mapBehaviorBack(aiComp.currentBehavior);
            creature.hunger = aiComp.hungerLevel;
            creature.health = aiComp.energy;
        }
    }
    
    /**
     * Synchronize data between creatures and entities.
     */
    private void synchronizeCreatureData() {
        for (Map.Entry<Entity, UUID> entry : entityToCreatureMap.entrySet()) {
            Entity entity = entry.getKey();
            UUID creatureId = entry.getValue();
            
            // Find the creature
            MarineEcosystem.MarineCreature creature = findCreatureById(creatureId);
            if (creature != null && creature.isAlive) {
                syncEntityToCreature(entity, creature);
            } else {
                // Creature is dead, remove entity
                removeEntityMapping(entity);
            }
        }
    }
    
    /**
     * Handle creature lifecycle events (birth, death, etc.).
     */
    private void handleCreatureLifecycle() {
        // Check for new creatures that need entities
        for (Map.Entry<MarineEcosystem.SpeciesType, List<MarineEcosystem.MarineCreature>> entry : 
             ecosystem.getSpeciesPopulations().entrySet()) {
            
            MarineEcosystem.SpeciesType species = entry.getKey();
            List<MarineEcosystem.MarineCreature> creatures = entry.getValue();
            
            for (MarineEcosystem.MarineCreature creature : creatures) {
                if (creature.isAlive && !creatureToEntityMap.containsKey(creature.id)) {
                    // New creature, create entity
                    Entity entity = migrateCreatureToEntity(creature, species);
                    if (entity != null) {
                        logger.debug("Created entity for new creature {}", creature.id);
                    }
                }
            }
        }
        
        // Clean up entities for dead creatures
        List<Entity> entitiesToRemove = new ArrayList<>();
        for (Map.Entry<Entity, UUID> entry : entityToCreatureMap.entrySet()) {
            Entity entity = entry.getKey();
            UUID creatureId = entry.getValue();
            
            MarineEcosystem.MarineCreature creature = findCreatureById(creatureId);
            if (creature == null || !creature.isAlive) {
                entitiesToRemove.add(entity);
            }
        }
        
        for (Entity entity : entitiesToRemove) {
            removeEntityMapping(entity);
        }
    }
    
    /**
     * Remove entity mapping and destroy entity.
     */
    private void removeEntityMapping(Entity entity) {
        UUID creatureId = entityToCreatureMap.remove(entity);
        if (creatureId != null) {
            creatureToEntityMap.remove(creatureId);
        }
        world.destroyEntity(entity);
    }
    
    /**
     * Find a creature by ID across all species populations.
     */
    private MarineEcosystem.MarineCreature findCreatureById(UUID creatureId) {
        for (List<MarineEcosystem.MarineCreature> creatures : ecosystem.getSpeciesPopulations().values()) {
            for (MarineEcosystem.MarineCreature creature : creatures) {
                if (creature.id.equals(creatureId)) {
                    return creature;
                }
            }
        }
        return null;
    }
    
    /**
     * Map old behavior enum to new behavior enum.
     */
    private MarineAIComponent.BehaviorType mapBehavior(MarineEcosystem.CreatureBehavior oldBehavior) {
        return switch (oldBehavior) {
            case WANDERING -> MarineAIComponent.BehaviorType.WANDERING;
            case FEEDING -> MarineAIComponent.BehaviorType.FEEDING;
            case FLEEING -> MarineAIComponent.BehaviorType.FLEEING;
            case HUNTING -> MarineAIComponent.BehaviorType.HUNTING;
            case SCHOOLING -> MarineAIComponent.BehaviorType.SCHOOLING;
            case MATING -> MarineAIComponent.BehaviorType.MATING;
            case MIGRATING -> MarineAIComponent.BehaviorType.MIGRATING;
            case RESTING -> MarineAIComponent.BehaviorType.RESTING;
        };
    }
    
    /**
     * Map new behavior enum back to old behavior enum.
     */
    private MarineEcosystem.CreatureBehavior mapBehaviorBack(MarineAIComponent.BehaviorType newBehavior) {
        return switch (newBehavior) {
            case WANDERING -> MarineEcosystem.CreatureBehavior.WANDERING;
            case FEEDING -> MarineEcosystem.CreatureBehavior.FEEDING;
            case FLEEING -> MarineEcosystem.CreatureBehavior.FLEEING;
            case HUNTING -> MarineEcosystem.CreatureBehavior.HUNTING;
            case SCHOOLING -> MarineEcosystem.CreatureBehavior.SCHOOLING;
            case MATING -> MarineEcosystem.CreatureBehavior.MATING;
            case MIGRATING -> MarineEcosystem.CreatureBehavior.MIGRATING;
            case RESTING -> MarineEcosystem.CreatureBehavior.RESTING;
            case TERRITORIAL_PATROL -> MarineEcosystem.CreatureBehavior.WANDERING; // Map to closest equivalent
            case EXPLORING -> MarineEcosystem.CreatureBehavior.WANDERING;
            case FOLLOWING -> MarineEcosystem.CreatureBehavior.SCHOOLING;
            case CIRCLING -> MarineEcosystem.CreatureBehavior.WANDERING;
            case AMBUSH_HUNTING -> MarineEcosystem.CreatureBehavior.HUNTING;
            case PACK_HUNTING -> MarineEcosystem.CreatureBehavior.HUNTING;
            case SOCIAL_INTERACTION -> MarineEcosystem.CreatureBehavior.SCHOOLING;
            case DEFENSIVE_POSTURING -> MarineEcosystem.CreatureBehavior.FLEEING;
            case CURIOSITY_INVESTIGATION -> MarineEcosystem.CreatureBehavior.WANDERING;
        };
    }
    
    /**
     * Get maximum age for a species.
     */
    private float getSpeciesMaxAge(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case PLANKTON -> 0.1f;
            case FISH -> 3.0f;
            case SMALL_FISH -> 2.0f;
            case MEDIUM_FISH -> 5.0f;
            case LARGE_FISH -> 10.0f;
            case SHARK -> 25.0f;
            case WHALE -> 80.0f;
            case DOLPHIN -> 45.0f;
            case OCTOPUS -> 3.0f;
            case SQUID -> 2.0f;
            case CRAB -> 8.0f;
            case LOBSTER -> 15.0f;
            case SEA_TURTLE -> 100.0f;
            case JELLYFISH -> 1.0f;
            case CORAL -> 1000.0f;
            case SEAWEED -> 5.0f;
            case SEA_ANEMONE -> 50.0f;
        };
    }
    
    /**
     * Set up periodic synchronization tasks.
     */
    private void setupSynchronization() {
        // This could be expanded to include scheduled tasks for:
        // - Periodic data validation
        // - Performance monitoring
        // - Cleanup of orphaned entities/creatures
        logger.debug("Synchronization setup complete");
    }
    
    /**
     * Create a new creature using the ECS system.
     */
    public Entity createCreature(MarineEcosystem.SpeciesType species, Vector3f position) {
        Entity entity = creatureFactory.createMarineCreature(species, position);
        
        // Also create corresponding MarineCreature object for backward compatibility
        MarineEcosystem.MarineCreature creature = new MarineEcosystem.MarineCreature(species, position);
        
        // Add to ecosystem
        ecosystem.getSpeciesPopulations().computeIfAbsent(species, k -> new ArrayList<>()).add(creature);
        
        // Store mappings
        creatureToEntityMap.put(creature.id, entity);
        entityToCreatureMap.put(entity, creature.id);
        
        return entity;
    }
    
    /**
     * Create a school of creatures using the ECS system.
     */
    public void createSchool(MarineEcosystem.SpeciesType species, Vector3f center, int count, float radius) {
        creatureFactory.createSchool(species, center, count, radius);
        
        // The factory will create entities, we need to create corresponding MarineCreature objects
        // This is handled automatically by the lifecycle management
    }
    
    /**
     * Populate an area with diverse marine life.
     */
    public void populateArea(Vector3f center, float radius, int totalCreatures) {
        creatureFactory.populateArea(center, radius, totalCreatures);
    }
    
    /**
     * Get statistics about the integration.
     */
    public Map<String, Object> getIntegrationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("mappedCreatures", creatureToEntityMap.size());
        stats.put("mappedEntities", entityToCreatureMap.size());
        stats.put("migrationProgress", migrationProgress);
        stats.put("isInitialized", isInitialized);
        
        // Add AI system statistics
        if (aiSystem != null) {
            stats.putAll(aiSystem.getAIStatistics());
        }
        
        return stats;
    }
    
    /**
     * Get the entity for a creature ID.
     */
    public Entity getEntityForCreature(UUID creatureId) {
        return creatureToEntityMap.get(creatureId);
    }
    
    /**
     * Get the creature ID for an entity.
     */
    public UUID getCreatureForEntity(Entity entity) {
        return entityToCreatureMap.get(entity);
    }
    
    /**
     * Check if the integration is properly initialized.
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Cleanup the integration system.
     */
    public void cleanup() {
        logger.info("Cleaning up marine ecosystem integration");
        
        // Remove all entity mappings
        for (Entity entity : entityToCreatureMap.keySet()) {
            world.destroyEntity(entity);
        }
        
        creatureToEntityMap.clear();
        entityToCreatureMap.clear();
        
        // Remove AI system from world
        world.removeSystem(aiSystem);
        
        isInitialized = false;
        logger.info("Marine ecosystem integration cleanup complete");
    }
}