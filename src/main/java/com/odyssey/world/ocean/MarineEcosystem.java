package com.odyssey.world.ocean;

import com.odyssey.graphics.Renderer;
import com.odyssey.game.ecs.World;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced marine ecosystem simulation for The Odyssey.
 * Implements realistic predator-prey relationships, seasonal migrations,
 * and complex food chain dynamics.
 */
public class MarineEcosystem {
    private static final Logger logger = LoggerFactory.getLogger(MarineEcosystem.class);
    
    // Species management
    private final Map<SpeciesType, List<MarineCreature>> speciesPopulations = new EnumMap<>(SpeciesType.class);
    private final List<School> fishSchools = new ArrayList<>();
    private final List<PredatorPack> predatorPacks = new ArrayList<>();
    private final Map<Vector2f, Ecosystem> localEcosystems = new HashMap<>();
    
    // Environmental factors
    private float waterTemperature = 20.0f;
    private float oxygenLevel = 8.0f; // mg/L
    private float planktonDensity = 1.0f;
    private SeasonType currentSeason = SeasonType.SPRING;
    
    // Ecosystem statistics
    private int totalCreatures = 0;
    private float biodiversityIndex = 1.0f;
    private float ecosystemHealth = 1.0f;
    
    // Migration and spawning
    private final List<MigrationRoute> migrationRoutes = new ArrayList<>();
    private final Map<SpeciesType, SpawningGround> spawningGrounds = new EnumMap<>(SpeciesType.class);
    
    // ECS Integration
    private MarineEcosystemIntegration ecsIntegration;
    private boolean useECSSystem = false;
    
    public enum SpeciesType {
        PLANKTON, FISH, SMALL_FISH, MEDIUM_FISH, LARGE_FISH, 
        SHARK, WHALE, DOLPHIN, OCTOPUS, SQUID,
        CRAB, LOBSTER, SEA_TURTLE, JELLYFISH,
        CORAL, SEAWEED, SEA_ANEMONE
    }
    
    public enum SeasonType {
        SPRING, SUMMER, AUTUMN, WINTER
    }
    
    public static class MarineCreature {
        public final UUID id;
        public final SpeciesType species;
        public Vector3f position;
        public Vector3f velocity;
        public float age;
        public float size;
        public float health;
        public float hunger;
        public boolean isAlive;
        public CreatureBehavior behavior;
        
        public MarineCreature(SpeciesType species, Vector3f position) {
            this.id = UUID.randomUUID();
            this.species = species;
            this.position = new Vector3f(position);
            this.velocity = new Vector3f();
            this.age = 0.0f;
            this.size = getSpeciesBaseSize(species) * (0.8f + (float)Math.random() * 0.4f);
            this.health = 1.0f;
            this.hunger = 0.5f;
            this.isAlive = true;
            this.behavior = CreatureBehavior.WANDERING;
        }
        
        private static float getSpeciesBaseSize(SpeciesType species) {
            return switch (species) {
                case PLANKTON -> 0.001f;
                case FISH -> 0.5f;
                case SMALL_FISH -> 0.2f;
                case MEDIUM_FISH -> 0.8f;
                case LARGE_FISH -> 2.0f;
                case SHARK -> 4.0f;
                case WHALE -> 15.0f;
                case DOLPHIN -> 3.0f;
                case OCTOPUS -> 1.5f;
                case SQUID -> 1.0f;
                case CRAB -> 0.3f;
                case LOBSTER -> 0.5f;
                case SEA_TURTLE -> 1.2f;
                case JELLYFISH -> 0.6f;
                case CORAL -> 0.1f;
                case SEAWEED -> 2.0f;
                case SEA_ANEMONE -> 0.4f;
            };
        }
    }
    
    public enum CreatureBehavior {
        WANDERING, FEEDING, FLEEING, HUNTING, SCHOOLING, MATING, MIGRATING, RESTING
    }
    
    public static class School {
        public final List<UUID> members;
        public Vector3f centerPosition;
        public Vector3f direction;
        public SpeciesType species;
        public float cohesionRadius;
        
        public School(SpeciesType species, Vector3f center) {
            this.members = new ArrayList<>();
            this.species = species;
            this.centerPosition = new Vector3f(center);
            this.direction = new Vector3f((float)Math.random() - 0.5f, 0, (float)Math.random() - 0.5f).normalize();
            this.cohesionRadius = 10.0f;
        }
    }
    
    public static class PredatorPack {
        public final List<UUID> members;
        public Vector3f huntingArea;
        public SpeciesType targetSpecies;
        public boolean isHunting;
        
        public PredatorPack(List<UUID> members, Vector3f huntingArea) {
            this.members = new ArrayList<>(members);
            this.huntingArea = new Vector3f(huntingArea);
            this.isHunting = false;
        }
    }
    
    public static class Ecosystem {
        public Vector2f location;
        public Map<SpeciesType, Integer> populations;
        public float biodiversity;
        public float productivity;
        public EcosystemType type;
        
        public Ecosystem(Vector2f location, EcosystemType type) {
            this.location = new Vector2f(location);
            this.type = type;
            this.populations = new EnumMap<>(SpeciesType.class);
            this.biodiversity = 1.0f;
            this.productivity = 1.0f;
            
            // Initialize with zero populations
            for (SpeciesType species : SpeciesType.values()) {
                populations.put(species, 0);
            }
        }
    }
    
    public enum EcosystemType {
        OPEN_OCEAN, CORAL_REEF, KELP_FOREST, DEEP_SEA, COASTAL, ABYSSAL
    }
    
    public static class MigrationRoute {
        public final List<Vector2f> waypoints;
        public final SpeciesType species;
        public final SeasonType season;
        public float progress; // 0.0 to 1.0
        
        public MigrationRoute(SpeciesType species, SeasonType season, List<Vector2f> waypoints) {
            this.species = species;
            this.season = season;
            this.waypoints = new ArrayList<>(waypoints);
            this.progress = 0.0f;
        }
    }
    
    public static class SpawningGround {
        public Vector2f location;
        public float radius;
        public SpeciesType species;
        public SeasonType spawningPeriod;
        public int maxCapacity;
        public int currentOccupancy;
        
        public SpawningGround(Vector2f location, SpeciesType species, SeasonType period) {
            this.location = new Vector2f(location);
            this.species = species;
            this.spawningPeriod = period;
            this.radius = 50.0f;
            this.maxCapacity = 100;
            this.currentOccupancy = 0;
        }
    }
    
    public void initialize() {
        logger.info("Initializing advanced marine ecosystem");
        
        // Initialize species populations
        initializeSpeciesPopulations();
        
        // Create local ecosystems
        generateLocalEcosystems(20); // 20 different ecosystem areas
        
        // Generate migration routes
        generateMigrationRoutes();
        
        // Create spawning grounds
        generateSpawningGrounds();
        
        // Form initial schools and packs
        formInitialGroups();
        
        logger.info("Marine ecosystem initialized with {} total creatures across {} species", 
                   totalCreatures, SpeciesType.values().length);
    }
    
    /**
     * Initialize the ecosystem with ECS integration.
     */
    public void initializeWithECS(World world) {
        // Initialize the basic ecosystem first
        initialize();
        
        // Set up ECS integration
        ecsIntegration = new MarineEcosystemIntegration(world, this);
        ecsIntegration.initialize();
        useECSSystem = true;
        
        logger.info("Marine ecosystem initialized with ECS integration");
    }
    
    private void initializeSpeciesPopulations() {
        Random random = new Random();
        
        for (SpeciesType species : SpeciesType.values()) {
            List<MarineCreature> population = new ArrayList<>();
            
            // Set population sizes based on ecological pyramid
            int populationSize = getInitialPopulationSize(species);
            
            for (int i = 0; i < populationSize; i++) {
                Vector3f position = new Vector3f(
                    (random.nextFloat() - 0.5f) * 2000, // -1000 to 1000
                    -random.nextFloat() * 100, // 0 to -100 (underwater)
                    (random.nextFloat() - 0.5f) * 2000
                );
                
                population.add(new MarineCreature(species, position));
            }
            
            speciesPopulations.put(species, population);
            totalCreatures += populationSize;
        }
    }
    
    private int getInitialPopulationSize(SpeciesType species) {
        return switch (species) {
            case PLANKTON -> 10000;
            case FISH -> 1000;
            case SMALL_FISH -> 2000;
            case MEDIUM_FISH -> 500;
            case LARGE_FISH -> 100;
            case SHARK -> 20;
            case WHALE -> 5;
            case DOLPHIN -> 15;
            case OCTOPUS -> 50;
            case SQUID -> 150;
            case CRAB -> 300;
            case LOBSTER -> 100;
            case SEA_TURTLE -> 25;
            case JELLYFISH -> 200;
            case CORAL -> 500;
            case SEAWEED -> 1000;
            case SEA_ANEMONE -> 150;
        };
    }
    
    private void generateLocalEcosystems(int count) {
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            Vector2f location = new Vector2f(
                (random.nextFloat() - 0.5f) * 2000,
                (random.nextFloat() - 0.5f) * 2000
            );
            
            EcosystemType type = EcosystemType.values()[random.nextInt(EcosystemType.values().length)];
            localEcosystems.put(location, new Ecosystem(location, type));
        }
    }
    
    private void generateMigrationRoutes() {
        // Create migration routes for species that migrate
        SpeciesType[] migratingSpecies = {SpeciesType.WHALE, SpeciesType.SEA_TURTLE, SpeciesType.LARGE_FISH};
        
        for (SpeciesType species : migratingSpecies) {
            for (SeasonType season : SeasonType.values()) {
                List<Vector2f> waypoints = new ArrayList<>();
                
                // Generate 3-5 waypoints for migration route
                int waypointCount = 3 + (int)(Math.random() * 3);
                for (int i = 0; i < waypointCount; i++) {
                    waypoints.add(new Vector2f(
                        (float)(Math.random() - 0.5) * 1500,
                        (float)(Math.random() - 0.5) * 1500
                    ));
                }
                
                migrationRoutes.add(new MigrationRoute(species, season, waypoints));
            }
        }
    }
    
    private void generateSpawningGrounds() {
        for (SpeciesType species : SpeciesType.values()) {
            if (isSpawningSpecies(species)) {
                Vector2f location = new Vector2f(
                    (float)(Math.random() - 0.5) * 1000,
                    (float)(Math.random() - 0.5) * 1000
                );
                
                SeasonType spawningPeriod = SeasonType.values()[(int)(Math.random() * SeasonType.values().length)];
                spawningGrounds.put(species, new SpawningGround(location, species, spawningPeriod));
            }
        }
    }
    
    private boolean isSpawningSpecies(SpeciesType species) {
        return species != SpeciesType.CORAL && species != SpeciesType.SEAWEED && species != SpeciesType.SEA_ANEMONE;
    }
    
    private void formInitialGroups() {
        // Form fish schools
        SpeciesType[] schoolingSpecies = {SpeciesType.SMALL_FISH, SpeciesType.MEDIUM_FISH};
        
        for (SpeciesType species : schoolingSpecies) {
            List<MarineCreature> population = speciesPopulations.get(species);
            
            // Group fish into schools of 20-50 individuals
            for (int i = 0; i < population.size(); i += 35) {
                Vector3f schoolCenter = population.get(i).position;
                School school = new School(species, schoolCenter);
                
                for (int j = i; j < Math.min(i + 35, population.size()); j++) {
                    school.members.add(population.get(j).id);
                    population.get(j).behavior = CreatureBehavior.SCHOOLING;
                }
                
                fishSchools.add(school);
            }
        }
        
        // Form predator packs
        List<MarineCreature> sharks = speciesPopulations.get(SpeciesType.SHARK);
        for (int i = 0; i < sharks.size(); i += 3) {
            List<UUID> packMembers = new ArrayList<>();
            Vector3f huntingArea = sharks.get(i).position;
            
            for (int j = i; j < Math.min(i + 3, sharks.size()); j++) {
                packMembers.add(sharks.get(j).id);
                sharks.get(j).behavior = CreatureBehavior.HUNTING;
            }
            
            predatorPacks.add(new PredatorPack(packMembers, huntingArea));
        }
    }
    
    public void update(double deltaTime) {
        // Update ECS integration if enabled
        if (useECSSystem && ecsIntegration != null) {
            ecsIntegration.update(deltaTime);
        }
        
        // Update environmental factors
        updateEnvironmentalFactors(deltaTime);
        
        // Update creature behaviors (only if not using ECS)
        if (!useECSSystem) {
            updateCreatureBehaviors(deltaTime);
        }
        
        // Process predator-prey interactions
        processPredatorPreyInteractions(deltaTime);
        
        // Update migrations
        updateMigrations(deltaTime);
        
        // Handle spawning
        handleSpawning(deltaTime);
        
        // Update ecosystem health
        updateEcosystemHealth();
        
        // Population dynamics
        updatePopulationDynamics(deltaTime);
    }
    
    private void updateEnvironmentalFactors(double deltaTime) {
        // Simulate seasonal changes
        // This would be more complex in a full implementation
        
        // Update plankton density based on season and water conditions
        float targetPlanktonDensity = switch (currentSeason) {
            case SPRING -> 1.2f;
            case SUMMER -> 1.5f;
            case AUTUMN -> 1.0f;
            case WINTER -> 0.7f;
        };
        
        planktonDensity += (targetPlanktonDensity - planktonDensity) * (float)deltaTime * 0.01f;
    }
    
    private void updateCreatureBehaviors(double deltaTime) {
        for (List<MarineCreature> population : speciesPopulations.values()) {
            for (MarineCreature creature : population) {
                if (!creature.isAlive) continue;
                
                // Age the creature
                creature.age += (float)deltaTime;
                
                // Update hunger
                creature.hunger += (float)deltaTime * 0.1f;
                creature.hunger = Math.min(1.0f, creature.hunger);
                
                // Update behavior based on needs and environment
                updateCreatureBehavior(creature, deltaTime);
                
                // Move creature based on behavior
                moveCreature(creature, deltaTime);
            }
        }
    }
    
    private void updateCreatureBehavior(MarineCreature creature, double deltaTime) {
        // Simplified behavior state machine
        switch (creature.behavior) {
            case WANDERING -> {
                if (creature.hunger > 0.7f) {
                    creature.behavior = CreatureBehavior.FEEDING;
                } else if (shouldJoinSchool(creature)) {
                    creature.behavior = CreatureBehavior.SCHOOLING;
                }
            }
            case FEEDING -> {
                if (creature.hunger < 0.3f) {
                    creature.behavior = CreatureBehavior.WANDERING;
                }
            }
            case SCHOOLING -> {
                if (creature.hunger > 0.8f) {
                    creature.behavior = CreatureBehavior.FEEDING;
                }
            }
            // Add more behavior transitions...
        }
    }
    
    private boolean shouldJoinSchool(MarineCreature creature) {
        return creature.species == SpeciesType.SMALL_FISH || creature.species == SpeciesType.MEDIUM_FISH;
    }
    
    private void moveCreature(MarineCreature creature, double deltaTime) {
        Vector3f movement = new Vector3f();
        
        switch (creature.behavior) {
            case WANDERING -> {
                // Random walk
                movement.set(
                    (float)(Math.random() - 0.5) * 2.0f,
                    (float)(Math.random() - 0.5) * 0.5f,
                    (float)(Math.random() - 0.5) * 2.0f
                );
            }
            case SCHOOLING -> {
                // Move towards school center
                School school = findSchoolForCreature(creature.id);
                if (school != null) {
                    movement.set(school.centerPosition).sub(creature.position).normalize().mul(3.0f);
                }
            }
            case FEEDING -> {
                // Move towards food sources (simplified)
                movement.set(
                    (float)(Math.random() - 0.5) * 1.0f,
                    -0.5f, // Slightly downward to search for food
                    (float)(Math.random() - 0.5) * 1.0f
                );
            }
        }
        
        // Apply movement
        creature.velocity.lerp(movement, (float)deltaTime * 0.5f);
        creature.position.add(creature.velocity.x * (float)deltaTime, 
                             creature.velocity.y * (float)deltaTime, 
                             creature.velocity.z * (float)deltaTime);
        
        // Keep creatures in reasonable bounds
        creature.position.x = Math.max(-1000, Math.min(1000, creature.position.x));
        creature.position.z = Math.max(-1000, Math.min(1000, creature.position.z));
        creature.position.y = Math.max(-200, Math.min(-1, creature.position.y)); // Stay underwater
    }
    
    private School findSchoolForCreature(UUID creatureId) {
        for (School school : fishSchools) {
            if (school.members.contains(creatureId)) {
                return school;
            }
        }
        return null;
    }
    
    private void processPredatorPreyInteractions(double deltaTime) {
        // Simplified predator-prey interactions
        List<MarineCreature> predators = new ArrayList<>();
        predators.addAll(speciesPopulations.get(SpeciesType.SHARK));
        predators.addAll(speciesPopulations.get(SpeciesType.LARGE_FISH));
        
        for (MarineCreature predator : predators) {
            if (!predator.isAlive || predator.behavior != CreatureBehavior.HUNTING) continue;
            
            // Find nearby prey
            List<MarineCreature> nearbyPrey = findNearbyPrey(predator, 20.0f);
            
            if (!nearbyPrey.isEmpty() && Math.random() < deltaTime * 0.1) { // 10% chance per second
                MarineCreature prey = nearbyPrey.get((int)(Math.random() * nearbyPrey.size()));
                
                // Predation success depends on size difference
                if (predator.size > prey.size * 1.2f) {
                    prey.isAlive = false;
                    predator.hunger *= 0.5f; // Reduce hunger
                    predator.health = Math.min(1.0f, predator.health + 0.1f);
                }
            }
        }
    }
    
    private List<MarineCreature> findNearbyPrey(MarineCreature predator, float searchRadius) {
        List<MarineCreature> nearbyPrey = new ArrayList<>();
        
        for (List<MarineCreature> population : speciesPopulations.values()) {
            for (MarineCreature creature : population) {
                if (!creature.isAlive || creature.species == predator.species) continue;
                
                float distance = predator.position.distance(creature.position);
                if (distance <= searchRadius && isPrey(predator.species, creature.species)) {
                    nearbyPrey.add(creature);
                }
            }
        }
        
        return nearbyPrey;
    }
    
    private boolean isPrey(SpeciesType predator, SpeciesType prey) {
        return switch (predator) {
            case SHARK -> prey == SpeciesType.SMALL_FISH || prey == SpeciesType.MEDIUM_FISH || prey == SpeciesType.SEA_TURTLE;
            case LARGE_FISH -> prey == SpeciesType.SMALL_FISH;
            case MEDIUM_FISH -> prey == SpeciesType.PLANKTON;
            case SMALL_FISH -> prey == SpeciesType.PLANKTON;
            default -> false;
        };
    }
    
    private void updateMigrations(double deltaTime) {
        for (MigrationRoute route : migrationRoutes) {
            if (route.season == currentSeason) {
                route.progress += (float)deltaTime * 0.001f; // Very slow migration
                if (route.progress > 1.0f) route.progress = 0.0f; // Loop migration
                
                // Move creatures along migration route
                List<MarineCreature> migrants = speciesPopulations.get(route.species);
                for (MarineCreature creature : migrants) {
                    if (creature.behavior == CreatureBehavior.MIGRATING) {
                        // Calculate position along route
                        updateMigrantPosition(creature, route);
                    }
                }
            }
        }
    }
    
    private void updateMigrantPosition(MarineCreature creature, MigrationRoute route) {
        if (route.waypoints.isEmpty()) return;
        
        int currentWaypoint = (int)(route.progress * (route.waypoints.size() - 1));
        int nextWaypoint = Math.min(currentWaypoint + 1, route.waypoints.size() - 1);
        
        Vector2f current = route.waypoints.get(currentWaypoint);
        Vector2f next = route.waypoints.get(nextWaypoint);
        
        float localProgress = (route.progress * (route.waypoints.size() - 1)) - currentWaypoint;
        
        Vector2f targetPosition = new Vector2f(current).lerp(next, localProgress);
        creature.position.x = targetPosition.x;
        creature.position.z = targetPosition.y;
    }
    
    private void handleSpawning(double deltaTime) {
        for (SpawningGround ground : spawningGrounds.values()) {
            if (ground.spawningPeriod == currentSeason && ground.currentOccupancy < ground.maxCapacity) {
                // Spawn chance increases during spawning season
                if (Math.random() < deltaTime * 0.01) { // 1% chance per second
                    spawnCreature(ground);
                }
            }
        }
    }
    
    private void spawnCreature(SpawningGround ground) {
        Vector3f spawnPosition = new Vector3f(
            ground.location.x + (float)(Math.random() - 0.5) * ground.radius,
            -5.0f - (float)Math.random() * 10.0f, // Spawn underwater
            ground.location.y + (float)(Math.random() - 0.5) * ground.radius
        );
        
        MarineCreature newCreature = new MarineCreature(ground.species, spawnPosition);
        newCreature.age = 0.0f; // Newborn
        newCreature.size *= 0.3f; // Start small
        
        speciesPopulations.get(ground.species).add(newCreature);
        ground.currentOccupancy++;
        totalCreatures++;
    }
    
    private void updateEcosystemHealth() {
        // Calculate biodiversity index
        int livingSpecies = 0;
        for (List<MarineCreature> population : speciesPopulations.values()) {
            long livingCount = population.stream().mapToLong(c -> c.isAlive ? 1 : 0).sum();
            if (livingCount > 0) livingSpecies++;
        }
        
        biodiversityIndex = (float)livingSpecies / SpeciesType.values().length;
        
        // Calculate overall ecosystem health
        ecosystemHealth = (biodiversityIndex + planktonDensity + oxygenLevel / 10.0f) / 3.0f;
    }
    
    private void updatePopulationDynamics(double deltaTime) {
        // Remove dead creatures and update population counts
        int removedCreatures = 0;
        
        for (List<MarineCreature> population : speciesPopulations.values()) {
            Iterator<MarineCreature> iterator = population.iterator();
            while (iterator.hasNext()) {
                MarineCreature creature = iterator.next();
                
                // Remove dead creatures
                if (!creature.isAlive) {
                    iterator.remove();
                    removedCreatures++;
                    continue;
                }
                
                // Natural death from old age
                float maxAge = getSpeciesMaxAge(creature.species);
                if (creature.age > maxAge) {
                    creature.isAlive = false;
                }
                
                // Death from starvation
                if (creature.hunger >= 1.0f) {
                    creature.health -= (float)deltaTime * 0.1f;
                    if (creature.health <= 0.0f) {
                        creature.isAlive = false;
                    }
                }
            }
        }
        
        totalCreatures -= removedCreatures;
    }
    
    private float getSpeciesMaxAge(SpeciesType species) {
        return switch (species) {
            case PLANKTON -> 30.0f; // 30 seconds (very short-lived)
            case FISH -> 450.0f; // 7.5 minutes
            case SMALL_FISH -> 300.0f; // 5 minutes
            case MEDIUM_FISH -> 600.0f; // 10 minutes
            case LARGE_FISH -> 1200.0f; // 20 minutes
            case SHARK -> 3600.0f; // 1 hour
            case WHALE -> 7200.0f; // 2 hours
            case DOLPHIN -> 3600.0f; // 1 hour
            case SEA_TURTLE -> 5400.0f; // 1.5 hours
            default -> 1800.0f; // 30 minutes default
        };
    }
    
    public void render(Renderer renderer) {
        // Render marine creatures
        for (List<MarineCreature> population : speciesPopulations.values()) {
            for (MarineCreature creature : population) {
                if (creature.isAlive) {
                    renderCreature(renderer, creature);
                }
            }
        }
        
        // Render schools (visual groupings)
        for (School school : fishSchools) {
            renderSchool(renderer, school);
        }
    }
    
    private void renderCreature(Renderer renderer, MarineCreature creature) {
        // This would render the creature model at its position
        // For now, just render a simple representation
        renderer.renderCube(creature.position.x, creature.position.y, creature.position.z, creature.size);
    }
    
    private void renderSchool(Renderer renderer, School school) {
        // Render school boundaries or effects
        // This could show the cohesion area or movement trails
    }
    
    public void setSeason(SeasonType season) {
        if (this.currentSeason != season) {
            logger.info("Season changed from {} to {}", this.currentSeason, season);
            this.currentSeason = season;
            
            // Trigger seasonal behaviors
            triggerSeasonalMigrations();
        }
    }
    
    private void triggerSeasonalMigrations() {
        // Set creatures to migrate based on the new season
        for (MigrationRoute route : migrationRoutes) {
            if (route.season == currentSeason) {
                List<MarineCreature> migrants = speciesPopulations.get(route.species);
                for (MarineCreature creature : migrants) {
                    if (Math.random() < 0.1) { // 10% of population migrates
                        creature.behavior = CreatureBehavior.MIGRATING;
                    }
                }
            }
        }
    }
    
    public void cleanup() {
        logger.info("Cleaning up marine ecosystem");
        
        // Cleanup ECS integration if enabled
        if (useECSSystem && ecsIntegration != null) {
            ecsIntegration.cleanup();
            ecsIntegration = null;
            useECSSystem = false;
        }
        
        speciesPopulations.clear();
        fishSchools.clear();
        predatorPacks.clear();
        localEcosystems.clear();
        migrationRoutes.clear();
        spawningGrounds.clear();
    }
    
    // Getters for ecosystem monitoring
    public int getTotalCreatures() { return totalCreatures; }
    public float getBiodiversityIndex() { return biodiversityIndex; }
    public float getEcosystemHealth() { return ecosystemHealth; }
    public float getPlanktonDensity() { return planktonDensity; }
    public SeasonType getCurrentSeason() { return currentSeason; }
    
    public Map<SpeciesType, Integer> getPopulationCounts() {
        Map<SpeciesType, Integer> counts = new EnumMap<>(SpeciesType.class);
        for (Map.Entry<SpeciesType, List<MarineCreature>> entry : speciesPopulations.entrySet()) {
            long livingCount = entry.getValue().stream().mapToLong(c -> c.isAlive ? 1 : 0).sum();
            counts.put(entry.getKey(), (int)livingCount);
        }
        return counts;
    }
    
    public List<MarineCreature> getCreaturesNear(Vector3f position, float radius) {
        List<MarineCreature> nearby = new ArrayList<>();
        for (List<MarineCreature> population : speciesPopulations.values()) {
            for (MarineCreature creature : population) {
                if (creature.isAlive && creature.position.distance(position) <= radius) {
                    nearby.add(creature);
                }
            }
        }
        return nearby;
    }
    
    // ECS Integration Methods
    
    /**
     * Check if ECS system is enabled.
     */
    public boolean isUsingECSSystem() {
        return useECSSystem;
    }
    
    /**
     * Get the ECS integration instance.
     */
    public MarineEcosystemIntegration getECSIntegration() {
        return ecsIntegration;
    }
    
    /**
     * Enable or disable ECS system.
     */
    public void setUseECSSystem(boolean useECS) {
        this.useECSSystem = useECS;
        if (useECS && ecsIntegration != null) {
            logger.info("ECS system enabled for marine ecosystem");
        } else if (!useECS) {
            logger.info("ECS system disabled for marine ecosystem");
        }
    }
    
    /**
     * Get species populations for ECS integration.
     */
    public Map<SpeciesType, List<MarineCreature>> getSpeciesPopulations() {
        return speciesPopulations;
    }
    
    /**
     * Get fish schools for ECS integration.
     */
    public List<School> getFishSchools() {
        return fishSchools;
    }
    
    /**
     * Get predator packs for ECS integration.
     */
    public List<PredatorPack> getPredatorPacks() {
        return predatorPacks;
    }
}