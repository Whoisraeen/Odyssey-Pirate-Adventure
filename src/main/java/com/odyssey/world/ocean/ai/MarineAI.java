package com.odyssey.world.ocean.ai;

import com.odyssey.world.ocean.MarineEcosystem;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced AI system for marine creatures with sophisticated behaviors.
 * Implements schooling, hunting, territorial, and social behaviors.
 */
public class MarineAI {
    private static final Logger logger = LoggerFactory.getLogger(MarineAI.class);
    
    private final Map<UUID, AIState> creatureStates = new HashMap<>();
    private final Map<UUID, Territory> territories = new HashMap<>();
    private final List<HuntingPack> huntingPacks = new ArrayList<>();
    private final Map<UUID, SocialGroup> socialGroups = new HashMap<>();
    
    // AI parameters
    private static final float PERCEPTION_RADIUS = 50.0f;
    private static final float FLEE_DISTANCE = 30.0f;
    private static final float HUNT_DISTANCE = 25.0f;
    private static final float TERRITORIAL_RADIUS = 40.0f;
    
    /**
     * Represents the AI state of a marine creature.
     */
    public static class AIState {
        public UUID creatureId;
        public BehaviorType currentBehavior;
        public BehaviorType previousBehavior;
        public Vector3f targetPosition;
        public UUID targetCreature;
        public float behaviorTimer;
        public float aggressionLevel;
        public float fearLevel;
        public float socialNeed;
        public Map<String, Object> behaviorData;
        
        public AIState(UUID creatureId) {
            this.creatureId = creatureId;
            this.currentBehavior = BehaviorType.WANDERING;
            this.previousBehavior = BehaviorType.WANDERING;
            this.targetPosition = new Vector3f();
            this.behaviorTimer = 0.0f;
            this.aggressionLevel = 0.5f;
            this.fearLevel = 0.3f;
            this.socialNeed = 0.5f;
            this.behaviorData = new HashMap<>();
        }
    }
    
    /**
     * Enhanced behavior types for marine creatures.
     */
    public enum BehaviorType {
        WANDERING,
        SCHOOLING,
        HUNTING,
        FLEEING,
        FEEDING,
        TERRITORIAL_PATROL,
        MATING,
        MIGRATING,
        RESTING,
        EXPLORING,
        FOLLOWING,
        CIRCLING,
        AMBUSH_HUNTING,
        PACK_HUNTING,
        DEFENSIVE,
        CURIOUS,
        PLAYFUL
    }
    
    /**
     * Represents a territory claimed by a creature.
     */
    public static class Territory {
        public UUID ownerId;
        public Vector3f center;
        public float radius;
        public TerritoryType type;
        public Set<UUID> intruders;
        public float aggressionLevel;
        
        public Territory(UUID ownerId, Vector3f center, float radius, TerritoryType type) {
            this.ownerId = ownerId;
            this.center = new Vector3f(center);
            this.radius = radius;
            this.type = type;
            this.intruders = new HashSet<>();
            this.aggressionLevel = 0.5f;
        }
    }
    
    public enum TerritoryType {
        FEEDING_GROUND, BREEDING_AREA, RESTING_SPOT, HUNTING_GROUND
    }
    
    /**
     * Represents a hunting pack of predators.
     */
    public static class HuntingPack {
        public UUID leaderId;
        public Set<UUID> members;
        public Vector3f targetArea;
        public UUID currentPrey;
        public HuntingStrategy strategy;
        public float coordination;
        
        public HuntingPack(UUID leaderId) {
            this.leaderId = leaderId;
            this.members = new HashSet<>();
            this.members.add(leaderId);
            this.targetArea = new Vector3f();
            this.strategy = HuntingStrategy.SURROUND;
            this.coordination = 0.7f;
        }
    }
    
    public enum HuntingStrategy {
        SURROUND, CHASE, AMBUSH, HERD, PINCER
    }
    
    /**
     * Represents a social group for non-predatory behaviors.
     */
    public static class SocialGroup {
        public UUID leaderId;
        public Set<UUID> members;
        public SocialBehavior currentActivity;
        public Vector3f groupCenter;
        public float cohesion;
        
        public SocialGroup(UUID leaderId) {
            this.leaderId = leaderId;
            this.members = new HashSet<>();
            this.members.add(leaderId);
            this.currentActivity = SocialBehavior.FORAGING;
            this.groupCenter = new Vector3f();
            this.cohesion = 0.8f;
        }
    }
    
    public enum SocialBehavior {
        FORAGING, PLAYING, RESTING, EXPLORING, MIGRATING
    }
    
    /**
     * Updates AI for all marine creatures.
     */
    public void updateAI(MarineEcosystem ecosystem, double deltaTime) {
        // Update individual creature AI states
        updateCreatureStates(ecosystem, deltaTime);
        
        // Update group behaviors
        updateHuntingPacks(ecosystem, deltaTime);
        updateSocialGroups(ecosystem, deltaTime);
        updateTerritories(ecosystem, deltaTime);
        
        // Process behavior decisions
        processAIDecisions(ecosystem, deltaTime);
    }
    
    /**
     * Updates individual creature AI states.
     */
    private void updateCreatureStates(MarineEcosystem ecosystem, double deltaTime) {
        for (MarineEcosystem.SpeciesType species : MarineEcosystem.SpeciesType.values()) {
            List<MarineEcosystem.MarineCreature> population = ecosystem.getSpeciesPopulations().get(species);
            if (population == null) continue;
            
            for (MarineEcosystem.MarineCreature creature : population) {
                if (!creature.isAlive) continue;
                
                AIState state = creatureStates.computeIfAbsent(creature.id, AIState::new);
                updateCreatureAI(creature, state, ecosystem, deltaTime);
            }
        }
    }
    
    /**
     * Updates AI for a specific creature.
     */
    private void updateCreatureAI(MarineEcosystem.MarineCreature creature, AIState state, 
                                 MarineEcosystem ecosystem, double deltaTime) {
        
        // Update timers
        state.behaviorTimer += (float)deltaTime;
        
        // Perceive environment
        List<MarineEcosystem.MarineCreature> nearbyCreatures = ecosystem.getCreaturesNear(
            creature.position, PERCEPTION_RADIUS);
        
        // Update emotional states
        updateEmotionalState(creature, state, nearbyCreatures, deltaTime);
        
        // Make behavior decisions
        BehaviorType newBehavior = decideBehavior(creature, state, nearbyCreatures, ecosystem);
        
        if (newBehavior != state.currentBehavior) {
            state.previousBehavior = state.currentBehavior;
            state.currentBehavior = newBehavior;
            state.behaviorTimer = 0.0f;
            
            // Initialize new behavior
            initializeBehavior(creature, state, newBehavior, ecosystem);
        }
        
        // Execute current behavior
        executeBehavior(creature, state, nearbyCreatures, ecosystem, deltaTime);
    }
    
    /**
     * Updates emotional states based on environment.
     */
    private void updateEmotionalState(MarineEcosystem.MarineCreature creature, AIState state,
                                    List<MarineEcosystem.MarineCreature> nearbyCreatures, double deltaTime) {
        
        // Fear response to predators
        boolean predatorNearby = false;
        for (MarineEcosystem.MarineCreature nearby : nearbyCreatures) {
            if (isPredator(nearby.species, creature.species)) {
                float distance = creature.position.distance(nearby.position);
                if (distance < FLEE_DISTANCE) {
                    predatorNearby = true;
                    state.fearLevel = Math.min(1.0f, state.fearLevel + (float)deltaTime * 2.0f);
                    break;
                }
            }
        }
        
        if (!predatorNearby) {
            state.fearLevel = Math.max(0.0f, state.fearLevel - (float)deltaTime * 0.5f);
        }
        
        // Social need based on species
        if (isSocialSpecies(creature.species)) {
            long sameSpeciesNearby = nearbyCreatures.stream()
                .filter(c -> c.species == creature.species)
                .count();
            
            if (sameSpeciesNearby < 3) {
                state.socialNeed = Math.min(1.0f, state.socialNeed + (float)deltaTime * 0.3f);
            } else {
                state.socialNeed = Math.max(0.0f, state.socialNeed - (float)deltaTime * 0.2f);
            }
        }
        
        // Aggression based on territory and hunger
        if (isTerritorialSpecies(creature.species)) {
            Territory territory = territories.get(creature.id);
            if (territory != null) {
                boolean intruderPresent = nearbyCreatures.stream()
                    .anyMatch(c -> c.id != creature.id && 
                             territory.center.distance(c.position) < territory.radius);
                
                if (intruderPresent) {
                    state.aggressionLevel = Math.min(1.0f, state.aggressionLevel + (float)deltaTime * 1.0f);
                } else {
                    state.aggressionLevel = Math.max(0.0f, state.aggressionLevel - (float)deltaTime * 0.3f);
                }
            }
        }
    }
    
    /**
     * Decides the next behavior for a creature.
     */
    private BehaviorType decideBehavior(MarineEcosystem.MarineCreature creature, AIState state,
                                      List<MarineEcosystem.MarineCreature> nearbyCreatures,
                                      MarineEcosystem ecosystem) {
        
        // Emergency behaviors (highest priority)
        if (state.fearLevel > 0.7f) {
            return BehaviorType.FLEEING;
        }
        
        if (creature.health < 0.3f) {
            return BehaviorType.RESTING;
        }
        
        // Hunger-driven behaviors
        if (creature.hunger > 0.8f) {
            if (isPredatorSpecies(creature.species)) {
                return BehaviorType.HUNTING;
            } else {
                return BehaviorType.FEEDING;
            }
        }
        
        // Social behaviors
        if (state.socialNeed > 0.6f && isSocialSpecies(creature.species)) {
            return BehaviorType.SCHOOLING;
        }
        
        // Territorial behaviors
        if (state.aggressionLevel > 0.7f && isTerritorialSpecies(creature.species)) {
            return BehaviorType.TERRITORIAL_PATROL;
        }
        
        // Species-specific behaviors
        return getSpeciesDefaultBehavior(creature.species, state);
    }
    
    /**
     * Gets default behavior for a species.
     */
    private BehaviorType getSpeciesDefaultBehavior(MarineEcosystem.SpeciesType species, AIState state) {
        return switch (species) {
            case SHARK -> state.behaviorTimer > 30.0f ? BehaviorType.HUNTING : BehaviorType.WANDERING;
            case WHALE -> BehaviorType.MIGRATING;
            case DOLPHIN -> Math.random() < 0.3 ? BehaviorType.PLAYFUL : BehaviorType.EXPLORING;
            case SMALL_FISH, MEDIUM_FISH -> BehaviorType.SCHOOLING;
            case OCTOPUS -> BehaviorType.AMBUSH_HUNTING;
            case SEA_TURTLE -> BehaviorType.WANDERING;
            default -> BehaviorType.WANDERING;
        };
    }
    
    /**
     * Initializes a new behavior.
     */
    private void initializeBehavior(MarineEcosystem.MarineCreature creature, AIState state,
                                  BehaviorType behavior, MarineEcosystem ecosystem) {
        
        switch (behavior) {
            case HUNTING -> initializeHunting(creature, state, ecosystem);
            case SCHOOLING -> initializeSchooling(creature, state, ecosystem);
            case TERRITORIAL_PATROL -> initializeTerritorialPatrol(creature, state);
            case FLEEING -> initializeFleeing(creature, state, ecosystem);
            case AMBUSH_HUNTING -> initializeAmbushHunting(creature, state);
            // Add more behavior initializations...
        }
    }
    
    private void initializeHunting(MarineEcosystem.MarineCreature creature, AIState state,
                                 MarineEcosystem ecosystem) {
        // Find potential prey
        List<MarineEcosystem.MarineCreature> nearbyCreatures = ecosystem.getCreaturesNear(
            creature.position, HUNT_DISTANCE);
        
        for (MarineEcosystem.MarineCreature potential : nearbyCreatures) {
            if (isPrey(creature.species, potential.species)) {
                state.targetCreature = potential.id;
                state.targetPosition.set(potential.position);
                break;
            }
        }
        
        // Join or form hunting pack for pack hunters
        if (isPackHunter(creature.species)) {
            joinOrFormHuntingPack(creature, state);
        }
    }
    
    private void initializeSchooling(MarineEcosystem.MarineCreature creature, AIState state,
                                   MarineEcosystem ecosystem) {
        // Find nearby school or create new one
        SocialGroup group = findNearbyGroup(creature, ecosystem);
        if (group == null) {
            group = new SocialGroup(creature.id);
            socialGroups.put(creature.id, group);
        } else {
            group.members.add(creature.id);
        }
    }
    
    private void initializeTerritorialPatrol(MarineEcosystem.MarineCreature creature, AIState state) {
        Territory territory = territories.get(creature.id);
        if (territory == null) {
            territory = new Territory(creature.id, creature.position, TERRITORIAL_RADIUS, 
                                    TerritoryType.FEEDING_GROUND);
            territories.put(creature.id, territory);
        }
        
        // Set patrol target
        float angle = (float)(Math.random() * Math.PI * 2);
        float distance = territory.radius * 0.7f;
        state.targetPosition.set(
            territory.center.x + (float)Math.cos(angle) * distance,
            territory.center.y,
            territory.center.z + (float)Math.sin(angle) * distance
        );
    }
    
    private void initializeFleeing(MarineEcosystem.MarineCreature creature, AIState state,
                                 MarineEcosystem ecosystem) {
        // Find direction away from threats
        Vector3f fleeDirection = new Vector3f();
        List<MarineEcosystem.MarineCreature> threats = ecosystem.getCreaturesNear(
            creature.position, FLEE_DISTANCE);
        
        for (MarineEcosystem.MarineCreature threat : threats) {
            if (isPredator(threat.species, creature.species)) {
                Vector3f awayFromThreat = new Vector3f(creature.position).sub(threat.position);
                fleeDirection.add(awayFromThreat);
            }
        }
        
        if (fleeDirection.lengthSquared() > 0) {
            fleeDirection.normalize().mul(100.0f); // Flee distance
            state.targetPosition.set(creature.position).add(fleeDirection);
        }
    }
    
    private void initializeAmbushHunting(MarineEcosystem.MarineCreature creature, AIState state) {
        // Find hiding spot near potential prey paths
        Vector3f hideSpot = new Vector3f(creature.position);
        hideSpot.y -= 5.0f; // Hide lower in water
        state.targetPosition.set(hideSpot);
        state.behaviorData.put("ambushTime", 0.0f);
    }
    
    /**
     * Executes the current behavior.
     */
    private void executeBehavior(MarineEcosystem.MarineCreature creature, AIState state,
                               List<MarineEcosystem.MarineCreature> nearbyCreatures,
                               MarineEcosystem ecosystem, double deltaTime) {
        
        switch (state.currentBehavior) {
            case HUNTING -> executeHunting(creature, state, nearbyCreatures, deltaTime);
            case SCHOOLING -> executeSchooling(creature, state, nearbyCreatures, deltaTime);
            case FLEEING -> executeFleeing(creature, state, deltaTime);
            case TERRITORIAL_PATROL -> executeTerritorialPatrol(creature, state, deltaTime);
            case AMBUSH_HUNTING -> executeAmbushHunting(creature, state, nearbyCreatures, deltaTime);
            case PLAYFUL -> executePlayful(creature, state, nearbyCreatures, deltaTime);
            default -> executeWandering(creature, state, deltaTime);
        }
    }
    
    private void executeHunting(MarineEcosystem.MarineCreature creature, AIState state,
                              List<MarineEcosystem.MarineCreature> nearbyCreatures, double deltaTime) {
        
        // Move towards target
        if (state.targetPosition.lengthSquared() > 0) {
            Vector3f direction = new Vector3f(state.targetPosition).sub(creature.position);
            if (direction.lengthSquared() > 1.0f) {
                direction.normalize();
                float speed = getSpeciesSpeed(creature.species) * 1.5f; // Hunting speed boost
                creature.velocity.lerp(direction.mul(speed), (float)deltaTime * 2.0f);
            }
        }
    }
    
    private void executeSchooling(MarineEcosystem.MarineCreature creature, AIState state,
                                List<MarineEcosystem.MarineCreature> nearbyCreatures, double deltaTime) {
        
        Vector3f cohesion = new Vector3f();
        Vector3f separation = new Vector3f();
        Vector3f alignment = new Vector3f();
        int neighborCount = 0;
        
        // Calculate boids forces
        for (MarineEcosystem.MarineCreature neighbor : nearbyCreatures) {
            if (neighbor.id.equals(creature.id) || neighbor.species != creature.species) continue;
            
            float distance = creature.position.distance(neighbor.position);
            if (distance < 20.0f) { // School radius
                neighborCount++;
                
                // Cohesion: move towards center of neighbors
                cohesion.add(neighbor.position);
                
                // Separation: avoid crowding
                if (distance < 5.0f) {
                    Vector3f away = new Vector3f(creature.position).sub(neighbor.position);
                    separation.add(away.normalize().div(distance));
                }
                
                // Alignment: match neighbor velocities
                alignment.add(neighbor.velocity);
            }
        }
        
        if (neighborCount > 0) {
            // Apply boids rules
            cohesion.div(neighborCount).sub(creature.position).normalize().mul(0.5f);
            separation.normalize().mul(1.0f);
            alignment.div(neighborCount).normalize().mul(0.3f);
            
            Vector3f steer = new Vector3f(cohesion).add(separation).add(alignment);
            creature.velocity.lerp(steer, (float)deltaTime);
        }
    }
    
    private void executeFleeing(MarineEcosystem.MarineCreature creature, AIState state, double deltaTime) {
        Vector3f direction = new Vector3f(state.targetPosition).sub(creature.position);
        if (direction.lengthSquared() > 1.0f) {
            direction.normalize();
            float speed = getSpeciesSpeed(creature.species) * 2.0f; // Panic speed
            creature.velocity.lerp(direction.mul(speed), (float)deltaTime * 3.0f);
        }
    }
    
    private void executeTerritorialPatrol(MarineEcosystem.MarineCreature creature, AIState state, double deltaTime) {
        Vector3f direction = new Vector3f(state.targetPosition).sub(creature.position);
        if (direction.lengthSquared() < 4.0f) {
            // Reached patrol point, pick new one
            initializeTerritorialPatrol(creature, state);
        } else {
            direction.normalize();
            float speed = getSpeciesSpeed(creature.species) * 0.8f;
            creature.velocity.lerp(direction.mul(speed), (float)deltaTime);
        }
    }
    
    private void executeAmbushHunting(MarineEcosystem.MarineCreature creature, AIState state,
                                    List<MarineEcosystem.MarineCreature> nearbyCreatures, double deltaTime) {
        
        Float ambushTime = (Float) state.behaviorData.get("ambushTime");
        if (ambushTime == null) ambushTime = 0.0f;
        
        ambushTime += (float)deltaTime;
        state.behaviorData.put("ambushTime", ambushTime);
        
        if (ambushTime < 10.0f) {
            // Stay hidden and wait
            creature.velocity.mul(0.1f); // Move very slowly
        } else {
            // Look for prey to ambush
            for (MarineEcosystem.MarineCreature potential : nearbyCreatures) {
                if (isPrey(creature.species, potential.species)) {
                    float distance = creature.position.distance(potential.position);
                    if (distance < 15.0f) {
                        // Strike!
                        state.targetCreature = potential.id;
                        state.currentBehavior = BehaviorType.HUNTING;
                        break;
                    }
                }
            }
        }
    }
    
    private void executePlayful(MarineEcosystem.MarineCreature creature, AIState state,
                              List<MarineEcosystem.MarineCreature> nearbyCreatures, double deltaTime) {
        
        // Dolphins and other playful species
        if (Math.random() < 0.1) { // 10% chance to change direction
            Vector3f randomDirection = new Vector3f(
                (float)(Math.random() - 0.5) * 2.0f,
                (float)(Math.random() - 0.5) * 1.0f,
                (float)(Math.random() - 0.5) * 2.0f
            ).normalize();
            
            float speed = getSpeciesSpeed(creature.species) * 1.2f;
            creature.velocity.lerp(randomDirection.mul(speed), (float)deltaTime);
        }
    }
    
    private void executeWandering(MarineEcosystem.MarineCreature creature, AIState state, double deltaTime) {
        // Simple random movement
        if (Math.random() < 0.05) { // 5% chance to change direction
            Vector3f randomDirection = new Vector3f(
                (float)(Math.random() - 0.5) * 2.0f,
                (float)(Math.random() - 0.5) * 0.5f,
                (float)(Math.random() - 0.5) * 2.0f
            ).normalize();
            
            float speed = getSpeciesSpeed(creature.species);
            creature.velocity.lerp(randomDirection.mul(speed), (float)deltaTime * 0.5f);
        }
    }
    
    // Helper methods for group behaviors
    private void updateHuntingPacks(MarineEcosystem ecosystem, double deltaTime) {
        for (HuntingPack pack : huntingPacks) {
            updatePackCoordination(pack, ecosystem, deltaTime);
        }
    }
    
    private void updateSocialGroups(MarineEcosystem ecosystem, double deltaTime) {
        for (SocialGroup group : socialGroups.values()) {
            updateGroupCohesion(group, ecosystem, deltaTime);
        }
    }
    
    private void updateTerritories(MarineEcosystem ecosystem, double deltaTime) {
        for (Territory territory : territories.values()) {
            updateTerritoryDefense(territory, ecosystem, deltaTime);
        }
    }
    
    private void processAIDecisions(MarineEcosystem ecosystem, double deltaTime) {
        // Process complex multi-creature decisions
        // This could include pack hunting coordination, territory disputes, etc.
    }
    
    // Utility methods
    private boolean isPredator(MarineEcosystem.SpeciesType predator, MarineEcosystem.SpeciesType prey) {
        return switch (predator) {
            case SHARK -> prey == MarineEcosystem.SpeciesType.SMALL_FISH || 
                         prey == MarineEcosystem.SpeciesType.MEDIUM_FISH || 
                         prey == MarineEcosystem.SpeciesType.SEA_TURTLE;
            case LARGE_FISH -> prey == MarineEcosystem.SpeciesType.SMALL_FISH;
            case MEDIUM_FISH -> prey == MarineEcosystem.SpeciesType.PLANKTON;
            case SMALL_FISH -> prey == MarineEcosystem.SpeciesType.PLANKTON;
            case OCTOPUS -> prey == MarineEcosystem.SpeciesType.SMALL_FISH || 
                           prey == MarineEcosystem.SpeciesType.CRAB;
            default -> false;
        };
    }
    
    private boolean isPrey(MarineEcosystem.SpeciesType predator, MarineEcosystem.SpeciesType prey) {
        return isPredator(predator, prey);
    }
    
    private boolean isSocialSpecies(MarineEcosystem.SpeciesType species) {
        return species == MarineEcosystem.SpeciesType.SMALL_FISH || 
               species == MarineEcosystem.SpeciesType.MEDIUM_FISH ||
               species == MarineEcosystem.SpeciesType.DOLPHIN;
    }
    
    private boolean isTerritorialSpecies(MarineEcosystem.SpeciesType species) {
        return species == MarineEcosystem.SpeciesType.SHARK || 
               species == MarineEcosystem.SpeciesType.OCTOPUS ||
               species == MarineEcosystem.SpeciesType.LARGE_FISH;
    }
    
    private boolean isPredatorSpecies(MarineEcosystem.SpeciesType species) {
        return species == MarineEcosystem.SpeciesType.SHARK || 
               species == MarineEcosystem.SpeciesType.LARGE_FISH ||
               species == MarineEcosystem.SpeciesType.OCTOPUS;
    }
    
    private boolean isPackHunter(MarineEcosystem.SpeciesType species) {
        return species == MarineEcosystem.SpeciesType.SHARK || 
               species == MarineEcosystem.SpeciesType.DOLPHIN;
    }
    
    private float getSpeciesSpeed(MarineEcosystem.SpeciesType species) {
        return switch (species) {
            case SHARK -> 8.0f;
            case DOLPHIN -> 10.0f;
            case LARGE_FISH -> 6.0f;
            case MEDIUM_FISH -> 4.0f;
            case SMALL_FISH -> 3.0f;
            case WHALE -> 5.0f;
            case SEA_TURTLE -> 2.0f;
            case OCTOPUS -> 3.0f;
            default -> 2.0f;
        };
    }
    
    // Placeholder methods for complex group behaviors
    private void joinOrFormHuntingPack(MarineEcosystem.MarineCreature creature, AIState state) {
        // Implementation for pack formation
    }
    
    private SocialGroup findNearbyGroup(MarineEcosystem.MarineCreature creature, MarineEcosystem ecosystem) {
        // Implementation for finding nearby social groups
        return null;
    }
    
    private void updatePackCoordination(HuntingPack pack, MarineEcosystem ecosystem, double deltaTime) {
        // Implementation for pack hunting coordination
    }
    
    private void updateGroupCohesion(SocialGroup group, MarineEcosystem ecosystem, double deltaTime) {
        // Implementation for social group cohesion
    }
    
    private void updateTerritoryDefense(Territory territory, MarineEcosystem ecosystem, double deltaTime) {
        // Implementation for territory defense behaviors
    }
    
    // Getters for monitoring
    public Map<UUID, AIState> getCreatureStates() { return creatureStates; }
    public Map<UUID, Territory> getTerritories() { return territories; }
    public List<HuntingPack> getHuntingPacks() { return huntingPacks; }
    public Map<UUID, SocialGroup> getSocialGroups() { return socialGroups; }
}