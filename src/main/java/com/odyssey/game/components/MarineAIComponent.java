package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;
import com.odyssey.world.ocean.MarineEcosystem;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Component that provides AI behavior for marine creatures.
 * This component stores the AI state and behavior parameters for entities
 * that represent marine life in the ocean ecosystem.
 */
public class MarineAIComponent implements Component {
    
    // Core AI state
    public BehaviorType currentBehavior = BehaviorType.WANDERING;
    public BehaviorType previousBehavior = BehaviorType.WANDERING;
    public float behaviorTimer = 0.0f;
    public float behaviorCooldown = 0.0f;
    
    // Target information
    public Vector3f targetPosition = new Vector3f();
    public UUID targetCreature = null;
    public float targetDistance = 0.0f;
    
    // Emotional states
    public float aggressionLevel = 0.5f;
    public float fearLevel = 0.0f;
    public float hungerLevel = 0.5f;
    public float socialNeed = 0.5f;
    public float territorialInstinct = 0.3f;
    
    // Behavior parameters
    public float perceptionRadius = 10.0f;
    public float fleeDistance = 5.0f;
    public float huntDistance = 8.0f;
    public float schoolingRadius = 3.0f;
    public float maxSpeed = 2.0f;
    public float agility = 1.0f;
    
    // Group associations
    public UUID schoolId = null;
    public UUID huntingPackId = null;
    public UUID socialGroupId = null;
    public UUID territoryId = null;
    
    // Species-specific traits
    public MarineEcosystem.SpeciesType species = MarineEcosystem.SpeciesType.FISH;
    public boolean isPredator = false;
    public boolean isSchooling = true;
    public boolean isTerritorial = false;
    public boolean isMigratory = false;
    
    // Memory and learning
    public Vector3f lastKnownThreatPosition = new Vector3f();
    public float threatMemoryTimer = 0.0f;
    public Vector3f lastFoodLocation = new Vector3f();
    public float foodMemoryTimer = 0.0f;
    
    // Energy and health
    public float energy = 1.0f;
    public float stress = 0.0f;
    public float reproductiveUrge = 0.0f;
    
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
        SOCIAL_INTERACTION,
        DEFENSIVE_POSTURING,
        CURIOSITY_INVESTIGATION
    }
    
    /**
     * Default constructor with basic wandering behavior.
     */
    public MarineAIComponent() {
        this.currentBehavior = BehaviorType.WANDERING;
        this.targetPosition.set(0, 0, 0);
    }
    
    /**
     * Constructor with species-specific initialization.
     */
    public MarineAIComponent(MarineEcosystem.SpeciesType species) {
        this.species = species;
        initializeSpeciesTraits();
    }
    
    /**
     * Initialize AI parameters based on species type.
     */
    private void initializeSpeciesTraits() {
        switch (species) {
            case SHARK -> {
                isPredator = true;
                isSchooling = false;
                isTerritorial = true;
                aggressionLevel = 0.8f;
                fearLevel = 0.1f;
                perceptionRadius = 15.0f;
                huntDistance = 12.0f;
                maxSpeed = 4.0f;
                agility = 0.7f;
            }
            case FISH -> {
                isPredator = false;
                isSchooling = true;
                isTerritorial = false;
                aggressionLevel = 0.1f;
                fearLevel = 0.7f;
                perceptionRadius = 8.0f;
                fleeDistance = 6.0f;
                maxSpeed = 3.0f;
                agility = 1.2f;
            }
            case WHALE -> {
                isPredator = false;
                isSchooling = false;
                isTerritorial = false;
                isMigratory = true;
                aggressionLevel = 0.2f;
                fearLevel = 0.3f;
                perceptionRadius = 20.0f;
                maxSpeed = 1.5f;
                agility = 0.4f;
            }
            case DOLPHIN -> {
                isPredator = true;
                isSchooling = true;
                isTerritorial = false;
                aggressionLevel = 0.6f;
                fearLevel = 0.3f;
                socialNeed = 0.9f;
                perceptionRadius = 12.0f;
                huntDistance = 10.0f;
                maxSpeed = 5.0f;
                agility = 1.5f;
            }
            case JELLYFISH -> {
                isPredator = false;
                isSchooling = false;
                isTerritorial = false;
                aggressionLevel = 0.0f;
                fearLevel = 0.2f;
                perceptionRadius = 5.0f;
                maxSpeed = 0.5f;
                agility = 0.3f;
            }
            case OCTOPUS -> {
                isPredator = true;
                isSchooling = false;
                isTerritorial = true;
                aggressionLevel = 0.7f;
                fearLevel = 0.5f;
                perceptionRadius = 8.0f;
                huntDistance = 6.0f;
                maxSpeed = 2.5f;
                agility = 1.8f;
            }
            case TURTLE -> {
                isPredator = false;
                isSchooling = false;
                isTerritorial = false;
                isMigratory = true;
                aggressionLevel = 0.1f;
                fearLevel = 0.4f;
                perceptionRadius = 6.0f;
                maxSpeed = 0.8f;
                agility = 0.2f;
            }
            default -> {
                // Default fish-like behavior
                isPredator = false;
                isSchooling = true;
                aggressionLevel = 0.3f;
                fearLevel = 0.5f;
            }
        }
    }
    
    /**
     * Reset behavior to wandering state.
     */
    public void resetBehavior() {
        previousBehavior = currentBehavior;
        currentBehavior = BehaviorType.WANDERING;
        behaviorTimer = 0.0f;
        targetCreature = null;
        targetPosition.set(0, 0, 0);
    }
    
    /**
     * Set a new behavior with proper state transition.
     */
    public void setBehavior(BehaviorType newBehavior) {
        if (currentBehavior != newBehavior) {
            previousBehavior = currentBehavior;
            currentBehavior = newBehavior;
            behaviorTimer = 0.0f;
        }
    }
    
    /**
     * Update emotional states based on environmental factors.
     */
    public void updateEmotionalState(float deltaTime) {
        // Decay emotional states over time
        aggressionLevel = Math.max(0.0f, aggressionLevel - deltaTime * 0.1f);
        fearLevel = Math.max(0.0f, fearLevel - deltaTime * 0.2f);
        stress = Math.max(0.0f, stress - deltaTime * 0.15f);
        
        // Increase hunger over time
        hungerLevel = Math.min(1.0f, hungerLevel + deltaTime * 0.05f);
        
        // Update memory timers
        threatMemoryTimer = Math.max(0.0f, threatMemoryTimer - deltaTime);
        foodMemoryTimer = Math.max(0.0f, foodMemoryTimer - deltaTime);
        
        // Update behavior timer
        behaviorTimer += deltaTime;
        behaviorCooldown = Math.max(0.0f, behaviorCooldown - deltaTime);
    }
    
    /**
     * Check if the creature can change behavior (not in cooldown).
     */
    public boolean canChangeBehavior() {
        return behaviorCooldown <= 0.0f;
    }
    
    /**
     * Set behavior cooldown to prevent rapid behavior switching.
     */
    public void setBehaviorCooldown(float cooldown) {
        this.behaviorCooldown = cooldown;
    }
    
    /**
     * Check if the creature is in a group behavior.
     */
    public boolean isInGroup() {
        return schoolId != null || huntingPackId != null || socialGroupId != null;
    }
    
    /**
     * Leave all groups.
     */
    public void leaveAllGroups() {
        schoolId = null;
        huntingPackId = null;
        socialGroupId = null;
    }
    
    /**
     * Get the dominant emotional state.
     */
    public String getDominantEmotion() {
        float maxLevel = Math.max(Math.max(aggressionLevel, fearLevel), 
                                 Math.max(hungerLevel, socialNeed));
        
        if (maxLevel == aggressionLevel && aggressionLevel > 0.6f) {
            return "Aggressive";
        } else if (maxLevel == fearLevel && fearLevel > 0.6f) {
            return "Fearful";
        } else if (maxLevel == hungerLevel && hungerLevel > 0.7f) {
            return "Hungry";
        } else if (maxLevel == socialNeed && socialNeed > 0.7f) {
            return "Social";
        } else {
            return "Calm";
        }
    }
    
    @Override
    public void onAttach() {
        // Initialize any required state when component is attached
    }
    
    @Override
    public void onDetach() {
        // Clean up when component is removed
        leaveAllGroups();
    }
}