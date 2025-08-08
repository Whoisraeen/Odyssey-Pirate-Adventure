package com.odyssey.game.systems;

import com.odyssey.game.components.MarineAIComponent;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.System;
import com.odyssey.world.ocean.MarineEcosystem;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System that processes AI behavior for marine creatures.
 * This system updates all entities that have MarineAIComponent, TransformComponent,
 * and PhysicsComponent, providing sophisticated AI behaviors for ocean life.
 */
public class MarineAISystem extends System {
    private static final Logger logger = LoggerFactory.getLogger(MarineAISystem.class);
    
    // AI behavior constants
    private static final float PERCEPTION_UPDATE_INTERVAL = 0.1f; // Update perception 10 times per second
    private static final float BEHAVIOR_DECISION_INTERVAL = 0.5f; // Make behavior decisions twice per second
    private static final float GROUP_UPDATE_INTERVAL = 1.0f; // Update group behaviors once per second
    
    // Behavior parameters
    private static final float MIN_BEHAVIOR_DURATION = 2.0f;
    private static final float MAX_WANDER_DISTANCE = 20.0f;
    private static final float SCHOOLING_COHESION_STRENGTH = 0.5f;
    private static final float SCHOOLING_SEPARATION_STRENGTH = 1.0f;
    private static final float SCHOOLING_ALIGNMENT_STRENGTH = 0.3f;
    
    // System state
    private float perceptionTimer = 0.0f;
    private float behaviorDecisionTimer = 0.0f;
    private float groupUpdateTimer = 0.0f;
    
    // Group management
    private final Map<UUID, SchoolGroup> schools = new ConcurrentHashMap<>();
    private final Map<UUID, HuntingPack> huntingPacks = new ConcurrentHashMap<>();
    private final Map<UUID, Territory> territories = new ConcurrentHashMap<>();
    
    // Performance optimization
    private final Map<Entity, List<Entity>> nearbyEntitiesCache = new ConcurrentHashMap<>();
    private float cacheUpdateTimer = 0.0f;
    private static final float CACHE_UPDATE_INTERVAL = 0.2f;
    
    /**
     * Represents a school of fish or similar creatures.
     */
    public static class SchoolGroup {
        public UUID id;
        public Set<Entity> members;
        public Vector3f centerPosition;
        public Vector3f averageVelocity;
        public MarineEcosystem.SpeciesType species;
        public float cohesionRadius;
        public long lastUpdate;
        
        public SchoolGroup(UUID id, MarineEcosystem.SpeciesType species) {
            this.id = id;
            this.species = species;
            this.members = ConcurrentHashMap.newKeySet();
            this.centerPosition = new Vector3f();
            this.averageVelocity = new Vector3f();
            this.cohesionRadius = 5.0f;
            this.lastUpdate = java.lang.System.currentTimeMillis();
        }
    }
    
    /**
     * Represents a hunting pack of predators.
     */
    public static class HuntingPack {
        public UUID id;
        public Set<Entity> members;
        public Vector3f huntingArea;
        public Entity currentTarget;
        public MarineEcosystem.SpeciesType species;
        public long lastUpdate;
        
        public HuntingPack(UUID id, MarineEcosystem.SpeciesType species) {
            this.id = id;
            this.species = species;
            this.members = ConcurrentHashMap.newKeySet();
            this.huntingArea = new Vector3f();
            this.lastUpdate = java.lang.System.currentTimeMillis();
        }
    }
    
    /**
     * Represents a territorial area claimed by a creature.
     */
    public static class Territory {
        public UUID id;
        public Entity owner;
        public Vector3f center;
        public float radius;
        public MarineEcosystem.SpeciesType species;
        public long establishedTime;
        
        public Territory(UUID id, Entity owner, Vector3f center, float radius) {
            this.id = id;
            this.owner = owner;
            this.center = new Vector3f(center);
            this.radius = radius;
            this.establishedTime = java.lang.System.currentTimeMillis();
        }
    }
    
    @Override
    public void update(double deltaTime) {
        float dt = (float) deltaTime;
        
        // Update timers
        perceptionTimer += dt;
        behaviorDecisionTimer += dt;
        groupUpdateTimer += dt;
        cacheUpdateTimer += dt;
        
        // Get all marine AI entities
        List<Entity> marineEntities = world.getEntitiesWith(
            MarineAIComponent.class, 
            TransformComponent.class, 
            PhysicsComponent.class
        );
        
        if (marineEntities.isEmpty()) {
            return;
        }
        
        // Update nearby entities cache periodically
        if (cacheUpdateTimer >= CACHE_UPDATE_INTERVAL) {
            updateNearbyEntitiesCache(marineEntities);
            cacheUpdateTimer = 0.0f;
        }
        
        // Update individual creature AI
        for (Entity entity : marineEntities) {
            updateCreatureAI(entity, dt);
        }
        
        // Update group behaviors periodically
        if (groupUpdateTimer >= GROUP_UPDATE_INTERVAL) {
            updateGroupBehaviors(dt);
            groupUpdateTimer = 0.0f;
        }
        
        // Clean up empty groups
        cleanupEmptyGroups();
    }
    
    /**
     * Update AI for a single creature.
     */
    private void updateCreatureAI(Entity entity, float deltaTime) {
        MarineAIComponent aiComp = entity.get(MarineAIComponent.class);
        TransformComponent transform = entity.get(TransformComponent.class);
        PhysicsComponent physics = entity.get(PhysicsComponent.class);
        
        if (aiComp == null || transform == null || physics == null) {
            return;
        }
        
        // Update emotional state
        aiComp.updateEmotionalState(deltaTime);
        
        // Update perception periodically
        List<Entity> nearbyEntities = Collections.emptyList();
        if (perceptionTimer >= PERCEPTION_UPDATE_INTERVAL) {
            nearbyEntities = getNearbyEntities(entity);
        }
        
        // Make behavior decisions periodically
        if (behaviorDecisionTimer >= BEHAVIOR_DECISION_INTERVAL) {
            decideBehavior(entity, aiComp, nearbyEntities);
        }
        
        // Execute current behavior
        executeBehavior(entity, aiComp, transform, physics, nearbyEntities, deltaTime);
        
        // Reset timers if needed
        if (perceptionTimer >= PERCEPTION_UPDATE_INTERVAL) {
            perceptionTimer = 0.0f;
        }
        if (behaviorDecisionTimer >= BEHAVIOR_DECISION_INTERVAL) {
            behaviorDecisionTimer = 0.0f;
        }
    }
    
    /**
     * Decide the next behavior for a creature based on its state and environment.
     */
    private void decideBehavior(Entity entity, MarineAIComponent aiComp, List<Entity> nearbyEntities) {
        if (!aiComp.canChangeBehavior() || aiComp.behaviorTimer < MIN_BEHAVIOR_DURATION) {
            return;
        }
        
        MarineAIComponent.BehaviorType newBehavior = aiComp.currentBehavior;
        
        // Check for immediate threats (high priority)
        if (detectThreat(entity, aiComp, nearbyEntities)) {
            newBehavior = MarineAIComponent.BehaviorType.FLEEING;
        }
        // Check for hunting opportunities (predators only)
        else if (aiComp.isPredator && aiComp.hungerLevel > 0.6f && detectPrey(entity, aiComp, nearbyEntities)) {
            newBehavior = MarineAIComponent.BehaviorType.HUNTING;
        }
        // Check for schooling opportunities (schooling species)
        else if (aiComp.isSchooling && aiComp.socialNeed > 0.5f && detectSchoolmates(entity, aiComp, nearbyEntities)) {
            newBehavior = MarineAIComponent.BehaviorType.SCHOOLING;
        }
        // Check for territorial behavior
        else if (aiComp.isTerritorial && aiComp.territorialInstinct > 0.6f) {
            newBehavior = MarineAIComponent.BehaviorType.TERRITORIAL_PATROL;
        }
        // Default to wandering
        else if (aiComp.currentBehavior != MarineAIComponent.BehaviorType.WANDERING) {
            newBehavior = MarineAIComponent.BehaviorType.WANDERING;
        }
        
        // Apply behavior change
        if (newBehavior != aiComp.currentBehavior) {
            aiComp.setBehavior(newBehavior);
            aiComp.setBehaviorCooldown(1.0f); // Prevent rapid behavior switching
            initializeBehavior(entity, aiComp, newBehavior);
        }
    }
    
    /**
     * Execute the current behavior for a creature.
     */
    private void executeBehavior(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                               PhysicsComponent physics, List<Entity> nearbyEntities, float deltaTime) {
        
        switch (aiComp.currentBehavior) {
            case WANDERING -> executeWandering(entity, aiComp, transform, physics, deltaTime);
            case SCHOOLING -> executeSchooling(entity, aiComp, transform, physics, nearbyEntities, deltaTime);
            case HUNTING -> executeHunting(entity, aiComp, transform, physics, nearbyEntities, deltaTime);
            case FLEEING -> executeFleeing(entity, aiComp, transform, physics, deltaTime);
            case TERRITORIAL_PATROL -> executeTerritorialPatrol(entity, aiComp, transform, physics, deltaTime);
            case FEEDING -> executeFeeding(entity, aiComp, transform, physics, deltaTime);
            default -> executeWandering(entity, aiComp, transform, physics, deltaTime);
        }
    }
    
    /**
     * Execute wandering behavior - random movement within bounds.
     */
    private void executeWandering(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                                PhysicsComponent physics, float deltaTime) {
        
        // Check if we need a new target position
        if (aiComp.targetPosition.lengthSquared() == 0 || 
            transform.position.distance(aiComp.targetPosition) < 2.0f) {
            
            // Generate new random target within wander distance
            Random random = new Random();
            float angle = random.nextFloat() * 2.0f * (float) Math.PI;
            float distance = random.nextFloat() * MAX_WANDER_DISTANCE;
            
            aiComp.targetPosition.set(
                transform.position.x + (float) Math.cos(angle) * distance,
                transform.position.y + (random.nextFloat() - 0.5f) * 5.0f, // Some vertical movement
                transform.position.z + (float) Math.sin(angle) * distance
            );
        }
        
        // Move towards target
        moveTowardsTarget(aiComp, transform, physics, aiComp.targetPosition, deltaTime);
    }
    
    /**
     * Execute schooling behavior - stay with nearby schoolmates.
     */
    private void executeSchooling(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                                PhysicsComponent physics, List<Entity> nearbyEntities, float deltaTime) {
        
        Vector3f cohesion = new Vector3f();
        Vector3f separation = new Vector3f();
        Vector3f alignment = new Vector3f();
        int schoolmateCount = 0;
        
        // Calculate schooling forces
        for (Entity other : nearbyEntities) {
            MarineAIComponent otherAI = other.get(MarineAIComponent.class);
            TransformComponent otherTransform = other.get(TransformComponent.class);
            
            if (otherAI != null && otherTransform != null && 
                otherAI.species == aiComp.species && other != entity) {
                
                float distance = transform.position.distance(otherTransform.position);
                
                if (distance < aiComp.schoolingRadius) {
                    schoolmateCount++;
                    
                    // Cohesion: move towards center of nearby schoolmates
                    cohesion.add(otherTransform.position);
                    
                    // Separation: avoid crowding
                    if (distance < aiComp.schoolingRadius * 0.5f) {
                        Vector3f separationForce = new Vector3f(transform.position).sub(otherTransform.position);
                        separationForce.normalize().mul(1.0f / distance);
                        separation.add(separationForce);
                    }
                    
                    // Alignment: match velocity of nearby schoolmates
                    PhysicsComponent otherPhysics = other.get(PhysicsComponent.class);
                    if (otherPhysics != null) {
                        alignment.add(otherPhysics.velocity);
                    }
                }
            }
        }
        
        // Apply schooling forces
        if (schoolmateCount > 0) {
            // Cohesion
            cohesion.div(schoolmateCount).sub(transform.position).normalize().mul(SCHOOLING_COHESION_STRENGTH);
            
            // Alignment
            alignment.div(schoolmateCount).normalize().mul(SCHOOLING_ALIGNMENT_STRENGTH);
            
            // Separation
            separation.normalize().mul(SCHOOLING_SEPARATION_STRENGTH);
            
            // Combine forces
            Vector3f schoolingForce = new Vector3f(cohesion).add(alignment).add(separation);
            Vector3f targetPosition = new Vector3f(transform.position).add(schoolingForce.mul(deltaTime * aiComp.maxSpeed));
            
            moveTowardsTarget(aiComp, transform, physics, targetPosition, deltaTime);
        } else {
            // No schoolmates nearby, switch to wandering
            aiComp.setBehavior(MarineAIComponent.BehaviorType.WANDERING);
        }
    }
    
    /**
     * Execute hunting behavior - pursue and attack prey.
     */
    private void executeHunting(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                              PhysicsComponent physics, List<Entity> nearbyEntities, float deltaTime) {
        
        Entity target = findNearestPrey(entity, aiComp, nearbyEntities);
        
        if (target != null) {
            TransformComponent targetTransform = target.get(TransformComponent.class);
            if (targetTransform != null) {
                // Move towards prey
                moveTowardsTarget(aiComp, transform, physics, targetTransform.position, deltaTime);
                
                // Check if close enough to attack
                float distance = transform.position.distance(targetTransform.position);
                if (distance < 2.0f) {
                    // Attack! (This would trigger damage/consumption logic)
                    aiComp.hungerLevel = Math.max(0.0f, aiComp.hungerLevel - 0.3f);
                    aiComp.setBehavior(MarineAIComponent.BehaviorType.WANDERING);
                    logger.debug("Creature {} attacked prey at distance {}", entity.getId(), distance);
                }
            }
        } else {
            // No prey found, return to wandering
            aiComp.setBehavior(MarineAIComponent.BehaviorType.WANDERING);
        }
    }
    
    /**
     * Execute fleeing behavior - escape from threats.
     */
    private void executeFleeing(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                              PhysicsComponent physics, float deltaTime) {
        
        // Move away from last known threat position
        if (aiComp.threatMemoryTimer > 0) {
            // Convert Vector2f transform position to Vector3f for calculation
            Vector3f currentPos3f = new Vector3f(transform.position.x, 0.0f, transform.position.y);
            Vector3f fleeDirection = new Vector3f(currentPos3f).sub(aiComp.lastKnownThreatPosition);
            fleeDirection.normalize();
            
            Vector3f fleeTarget = new Vector3f(currentPos3f).add(fleeDirection.mul(aiComp.fleeDistance));
            moveTowardsTarget(aiComp, transform, physics, fleeTarget, deltaTime);
        }
        
        // Stop fleeing after some time
        if (aiComp.behaviorTimer > 5.0f) {
            aiComp.setBehavior(MarineAIComponent.BehaviorType.WANDERING);
        }
    }
    
    /**
     * Execute territorial patrol behavior.
     */
    private void executeTerritorialPatrol(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                                        PhysicsComponent physics, float deltaTime) {
        
        Territory territory = territories.get(aiComp.territoryId);
        if (territory == null) {
            // Create new territory - convert Vector2f to Vector3f
            UUID territoryId = UUID.randomUUID();
            Vector3f territoryCenter = new Vector3f(transform.position.x, 0.0f, transform.position.y);
            territory = new Territory(territoryId, entity, territoryCenter, 10.0f);
            territories.put(territoryId, territory);
            aiComp.territoryId = territoryId;
        }
        
        // Patrol around territory center
        float angle = aiComp.behaviorTimer * 0.5f; // Slow circular patrol
        float patrolRadius = territory.radius * 0.7f;
        
        Vector3f patrolTarget = new Vector3f(
            territory.center.x + (float) Math.cos(angle) * patrolRadius,
            territory.center.y,
            territory.center.z + (float) Math.sin(angle) * patrolRadius
        );
        
        moveTowardsTarget(aiComp, transform, physics, patrolTarget, deltaTime);
    }
    
    /**
     * Execute feeding behavior.
     */
    private void executeFeeding(Entity entity, MarineAIComponent aiComp, TransformComponent transform, 
                              PhysicsComponent physics, float deltaTime) {
        
        // Simulate feeding by reducing hunger over time
        aiComp.hungerLevel = Math.max(0.0f, aiComp.hungerLevel - deltaTime * 0.2f);
        aiComp.energy = Math.min(1.0f, aiComp.energy + deltaTime * 0.1f);
        
        // Stop feeding when satisfied
        if (aiComp.hungerLevel < 0.3f || aiComp.behaviorTimer > 10.0f) {
            aiComp.setBehavior(MarineAIComponent.BehaviorType.WANDERING);
        }
    }
    
    /**
     * Move a creature towards a target position.
     */
    private void moveTowardsTarget(MarineAIComponent aiComp, TransformComponent transform, 
                                 PhysicsComponent physics, Vector3f target, float deltaTime) {
        
        // Convert Vector2f transform position to Vector3f for calculation
        Vector3f currentPos3f = new Vector3f(transform.position.x, 0.0f, transform.position.y);
        Vector3f direction = new Vector3f(target).sub(currentPos3f);
        float distance = direction.length();
        
        if (distance > 0.1f) {
            direction.normalize();
            
            // Apply speed and agility - convert to 2D for physics
            Vector2f desiredVelocity2f = new Vector2f(direction.x, direction.z).mul(aiComp.maxSpeed);
            Vector2f steering2f = new Vector2f(desiredVelocity2f).sub(physics.velocity);
            steering2f.mul(aiComp.agility * deltaTime);
            
            // Apply steering force
            physics.velocity.add(steering2f);
            
            // Limit velocity to max speed
            if (physics.velocity.length() > aiComp.maxSpeed) {
                physics.velocity.normalize().mul(aiComp.maxSpeed);
            }
        }
    }
    
    /**
     * Initialize behavior-specific state.
     */
    private void initializeBehavior(Entity entity, MarineAIComponent aiComp, MarineAIComponent.BehaviorType behavior) {
        switch (behavior) {
            case FLEEING -> {
                aiComp.threatMemoryTimer = 5.0f; // Remember threat for 5 seconds
                aiComp.fearLevel = 1.0f;
            }
            case HUNTING -> {
                aiComp.aggressionLevel = Math.min(1.0f, aiComp.aggressionLevel + 0.3f);
            }
            case TERRITORIAL_PATROL -> {
                // Territory will be created in execute method if needed
            }
            case SCHOOLING -> {
                aiComp.socialNeed = Math.max(0.0f, aiComp.socialNeed - 0.2f);
            }
        }
    }
    
    /**
     * Detect if there are any threats nearby.
     */
    private boolean detectThreat(Entity entity, MarineAIComponent aiComp, List<Entity> nearbyEntities) {
        TransformComponent transform = entity.get(TransformComponent.class);
        
        for (Entity other : nearbyEntities) {
            MarineAIComponent otherAI = other.get(MarineAIComponent.class);
            TransformComponent otherTransform = other.get(TransformComponent.class);
            
            if (otherAI != null && otherTransform != null && otherAI.isPredator && 
                otherAI.species != aiComp.species && !aiComp.isPredator) {
                
                float distance = transform.position.distance(otherTransform.position);
                if (distance < aiComp.perceptionRadius) {
                    // Convert Vector2f to Vector3f (map X,Y to X,Z, keep Y at 0)
                    aiComp.lastKnownThreatPosition.set(otherTransform.position.x, 0.0f, otherTransform.position.y);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Detect if there is prey nearby.
     */
    private boolean detectPrey(Entity entity, MarineAIComponent aiComp, List<Entity> nearbyEntities) {
        return findNearestPrey(entity, aiComp, nearbyEntities) != null;
    }
    
    /**
     * Find the nearest prey entity.
     */
    private Entity findNearestPrey(Entity entity, MarineAIComponent aiComp, List<Entity> nearbyEntities) {
        TransformComponent transform = entity.get(TransformComponent.class);
        Entity nearestPrey = null;
        float nearestDistance = Float.MAX_VALUE;
        
        for (Entity other : nearbyEntities) {
            MarineAIComponent otherAI = other.get(MarineAIComponent.class);
            TransformComponent otherTransform = other.get(TransformComponent.class);
            
            if (otherAI != null && otherTransform != null && !otherAI.isPredator && 
                otherAI.species != aiComp.species && other != entity) {
                
                float distance = transform.position.distance(otherTransform.position);
                if (distance < aiComp.huntDistance && distance < nearestDistance) {
                    nearestPrey = other;
                    nearestDistance = distance;
                }
            }
        }
        
        return nearestPrey;
    }
    
    /**
     * Detect if there are schoolmates nearby.
     */
    private boolean detectSchoolmates(Entity entity, MarineAIComponent aiComp, List<Entity> nearbyEntities) {
        for (Entity other : nearbyEntities) {
            MarineAIComponent otherAI = other.get(MarineAIComponent.class);
            
            if (otherAI != null && otherAI.species == aiComp.species && 
                otherAI.isSchooling && other != entity) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get nearby entities for a given entity.
     */
    private List<Entity> getNearbyEntities(Entity entity) {
        return nearbyEntitiesCache.getOrDefault(entity, Collections.emptyList());
    }
    
    /**
     * Update the cache of nearby entities for performance optimization.
     */
    private void updateNearbyEntitiesCache(List<Entity> allEntities) {
        nearbyEntitiesCache.clear();
        
        for (Entity entity : allEntities) {
            TransformComponent transform = entity.get(TransformComponent.class);
            MarineAIComponent aiComp = entity.get(MarineAIComponent.class);
            
            if (transform != null && aiComp != null) {
                List<Entity> nearby = new ArrayList<>();
                
                for (Entity other : allEntities) {
                    if (other != entity) {
                        TransformComponent otherTransform = other.get(TransformComponent.class);
                        if (otherTransform != null) {
                            float distance = transform.position.distance(otherTransform.position);
                            if (distance <= aiComp.perceptionRadius) {
                                nearby.add(other);
                            }
                        }
                    }
                }
                
                nearbyEntitiesCache.put(entity, nearby);
            }
        }
    }
    
    /**
     * Update group behaviors.
     */
    private void updateGroupBehaviors(float deltaTime) {
        updateSchools(deltaTime);
        updateHuntingPacks(deltaTime);
        updateTerritories(deltaTime);
    }
    
    /**
     * Update school behaviors.
     */
    private void updateSchools(float deltaTime) {
        for (SchoolGroup school : schools.values()) {
            if (!school.members.isEmpty()) {
                // Calculate school center and average velocity
                school.centerPosition.set(0, 0, 0);
                school.averageVelocity.set(0, 0, 0);
                
                for (Entity member : school.members) {
                    TransformComponent transform = member.get(TransformComponent.class);
                    PhysicsComponent physics = member.get(PhysicsComponent.class);
                    
                    if (transform != null) {
                        // Convert Vector2f to Vector3f (map X,Y to X,Z, keep Y at 0)
                        school.centerPosition.add(transform.position.x, 0.0f, transform.position.y);
                    }
                    if (physics != null) {
                        // Convert Vector2f to Vector3f (map X,Y to X,Z, keep Y at 0)
                        school.averageVelocity.add(physics.velocity.x, 0.0f, physics.velocity.y);
                    }
                }
                
                school.centerPosition.div(school.members.size());
                school.averageVelocity.div(school.members.size());
                school.lastUpdate = java.lang.System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Update hunting pack behaviors.
     */
    private void updateHuntingPacks(float deltaTime) {
        for (HuntingPack pack : huntingPacks.values()) {
            if (!pack.members.isEmpty()) {
                // Update hunting area based on pack position
                pack.huntingArea.set(0, 0, 0);
                
                for (Entity member : pack.members) {
                    TransformComponent transform = member.get(TransformComponent.class);
                    if (transform != null) {
                        // Convert Vector2f to Vector3f (map X,Y to X,Z, keep Y at 0)
                        pack.huntingArea.add(transform.position.x, 0.0f, transform.position.y);
                    }
                }
                
                pack.huntingArea.div(pack.members.size());
                pack.lastUpdate = java.lang.System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Update territory behaviors.
     */
    private void updateTerritories(float deltaTime) {
        // Territories are mostly static, just update timestamps
        for (Territory territory : territories.values()) {
            // Could add territory expansion/contraction logic here
        }
    }
    
    /**
     * Clean up empty groups.
     */
    private void cleanupEmptyGroups() {
        schools.entrySet().removeIf(entry -> entry.getValue().members.isEmpty());
        huntingPacks.entrySet().removeIf(entry -> entry.getValue().members.isEmpty());
        territories.entrySet().removeIf(entry -> entry.getValue().owner == null);
    }
    
    /**
     * Get statistics about the AI system.
     */
    public Map<String, Object> getAIStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("schoolCount", schools.size());
        stats.put("huntingPackCount", huntingPacks.size());
        stats.put("territoryCount", territories.size());
        stats.put("cachedEntities", nearbyEntitiesCache.size());
        return stats;
    }
}