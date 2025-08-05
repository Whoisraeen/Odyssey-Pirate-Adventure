package com.odyssey.world.biome;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced biome management system for The Odyssey.
 * Implements diverse maritime biomes with realistic ecosystems and climate zones.
 */
public class BiomeManager {
    private static final Logger logger = LoggerFactory.getLogger(BiomeManager.class);
    
    private final long seed;
    private final Random random;
    
    // Biome data and mapping
    private final Map<BiomeType, BiomeData> biomeDefinitions = new EnumMap<>(BiomeType.class);
    private final Map<Vector2f, BiomeRegion> biomeRegions = new HashMap<>();
    private final List<BiomeTransition> activeTransitions = new ArrayList<>();
    
    // Climate system
    private final Map<Vector2f, ClimateData> climateMap = new HashMap<>();
    private double seasonalTimer = 0.0;
    private Season currentSeason = Season.SPRING;
    
    public enum BiomeType {
        // Ocean biomes
        OPEN_OCEAN, DEEP_OCEAN, SHALLOW_WATERS, CORAL_REEFS,
        
        // Island biomes
        TROPICAL_ATOLLS, VOLCANIC_SPIRES, DENSE_JUNGLES, MANGROVE_SWAMPS,
        WHISPERING_ISLES, ARCTIC_ARCHIPELAGOS, DESERT_OASIS, FLOATING_GARDENS,
        BONE_ISLANDS, STORM_TOUCHED_ISLES, MAGNETIC_ANOMALIES,
        
        // Special biomes
        BIOLUMINESCENT_CAVERNS, TEMPORAL_ANOMALIES, MIRAGE_ISLANDS,
        ABYSSAL_DEPTHS, KELP_FORESTS, UNDERWATER_RUINS
    }
    
    public enum Season {
        SPRING(0.0f, 0.25f),
        SUMMER(0.25f, 0.5f),
        AUTUMN(0.5f, 0.75f),
        WINTER(0.75f, 1.0f);
        
        public final float start;
        public final float end;
        
        Season(float start, float end) {
            this.start = start;
            this.end = end;
        }
    }
    
    public static class BiomeData {
        public final BiomeType type;
        public final String name;
        public final String description;
        
        // Environmental parameters
        public final float baseTemperature;
        public final float temperatureVariation;
        public final float humidity;
        public final float rainfall;
        public final float windiness;
        
        // Gameplay parameters
        public final float navigationDifficulty;
        public final float resourceAbundance;
        public final float dangerLevel;
        
        // Visual parameters
        public final int[] dominantColors;
        public final float fogDensity;
        public final float visibility;
        
        // Flora and fauna
        public final List<String> flora;
        public final List<String> fauna;
        public final List<String> resources;
        public final List<String> specialFeatures;
        
        // Seasonal variations
        public final Map<Season, SeasonalModifier> seasonalModifiers;
        
        public BiomeData(BiomeType type, String name, String description,
                        float baseTemp, float tempVar, float humidity, float rainfall, float windiness,
                        float navDifficulty, float resourceAbundance, float dangerLevel,
                        int[] dominantColors, float fogDensity, float visibility) {
            this.type = type;
            this.name = name;
            this.description = description;
            this.baseTemperature = baseTemp;
            this.temperatureVariation = tempVar;
            this.humidity = humidity;
            this.rainfall = rainfall;
            this.windiness = windiness;
            this.navigationDifficulty = navDifficulty;
            this.resourceAbundance = resourceAbundance;
            this.dangerLevel = dangerLevel;
            this.dominantColors = dominantColors;
            this.fogDensity = fogDensity;
            this.visibility = visibility;
            this.flora = new ArrayList<>();
            this.fauna = new ArrayList<>();
            this.resources = new ArrayList<>();
            this.specialFeatures = new ArrayList<>();
            this.seasonalModifiers = new EnumMap<>(Season.class);
        }
    }
    
    public static class SeasonalModifier {
        public final float temperatureChange;
        public final float humidityChange;
        public final float rainfallChange;
        public final float windinessChange;
        public final List<String> seasonalEvents;
        
        public SeasonalModifier(float tempChange, float humidChange, float rainChange, float windChange) {
            this.temperatureChange = tempChange;
            this.humidityChange = humidChange;
            this.rainfallChange = rainChange;
            this.windinessChange = windChange;
            this.seasonalEvents = new ArrayList<>();
        }
    }
    
    public static class BiomeRegion {
        public final Vector2f center;
        public final float radius;
        public final BiomeType biomeType;
        public final float influence;
        public final Map<String, Object> properties;
        
        // Dynamic properties
        public float currentTemperature;
        public float currentHumidity;
        public float currentWindiness;
        public List<String> activeEvents;
        
        public BiomeRegion(Vector2f center, float radius, BiomeType biomeType, float influence) {
            this.center = new Vector2f(center);
            this.radius = radius;
            this.biomeType = biomeType;
            this.influence = influence;
            this.properties = new HashMap<>();
            this.activeEvents = new ArrayList<>();
        }
    }
    
    public static class BiomeTransition {
        public final Vector2f position;
        public final BiomeType fromBiome;
        public final BiomeType toBiome;
        public final float transitionRadius;
        public final double startTime;
        public final double duration;
        public float progress; // 0.0 to 1.0
        
        public BiomeTransition(Vector2f position, BiomeType from, BiomeType to, 
                             float radius, double duration) {
            this.position = new Vector2f(position);
            this.fromBiome = from;
            this.toBiome = to;
            this.transitionRadius = radius;
            this.startTime = System.currentTimeMillis() / 1000.0;
            this.duration = duration;
            this.progress = 0.0f;
        }
    }
    
    public static class ClimateData {
        public float temperature;
        public float humidity;
        public float pressure;
        public Vector2f windVector;
        public float rainfall;
        public List<String> weatherPatterns;
        
        public ClimateData() {
            this.windVector = new Vector2f();
            this.weatherPatterns = new ArrayList<>();
        }
    }
    
    public BiomeManager(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }
    
    public void initialize() {
        logger.info("Initializing advanced biome manager with seed: {}", seed);
        
        // Initialize biome definitions
        initializeBiomeDefinitions();
        
        // Generate biome regions
        generateBiomeRegions(50);
        
        // Initialize climate system
        initializeClimateSystem();
        
        logger.info("Biome manager initialized with {} biome types and {} regions", 
                   biomeDefinitions.size(), biomeRegions.size());
    }
    
    private void initializeBiomeDefinitions() {
        // Ocean Biomes
        BiomeData openOcean = new BiomeData(
            BiomeType.OPEN_OCEAN, "Open Ocean", "Vast expanses of deep blue water with gentle swells",
            20.0f, 5.0f, 70.0f, 0.3f, 0.6f,
            0.2f, 0.3f, 0.2f,
            new int[]{0x1E40AF, 0x3B82F6, 0x60A5FA}, 0.1f, 15000.0f
        );
        openOcean.fauna.addAll(Arrays.asList("dolphins", "whales", "flying_fish", "sea_birds"));
        openOcean.resources.addAll(Arrays.asList("fish", "seaweed", "driftwood"));
        openOcean.specialFeatures.addAll(Arrays.asList("deep_currents", "whale_migrations"));
        biomeDefinitions.put(BiomeType.OPEN_OCEAN, openOcean);
        
        BiomeData coralReefs = new BiomeData(
            BiomeType.CORAL_REEFS, "Coral Reefs", "Vibrant underwater gardens teeming with life",
            25.0f, 3.0f, 80.0f, 0.1f, 0.3f,
            0.4f, 0.9f, 0.3f,
            new int[]{0xFF6B9D, 0xFF8CC8, 0xFAD5E4}, 0.05f, 8000.0f
        );
        coralReefs.flora.addAll(Arrays.asList("brain_coral", "staghorn_coral", "sea_anemones", "kelp"));
        coralReefs.fauna.addAll(Arrays.asList("tropical_fish", "sea_turtles", "octopi", "sea_urchins"));
        coralReefs.resources.addAll(Arrays.asList("coral", "pearls", "exotic_fish", "sea_shells"));
        coralReefs.specialFeatures.addAll(Arrays.asList("hidden_caves", "underwater_gardens", "natural_harbors"));
        biomeDefinitions.put(BiomeType.CORAL_REEFS, coralReefs);
        
        // Island Biomes
        BiomeData tropicalAtolls = new BiomeData(
            BiomeType.TROPICAL_ATOLLS, "Tropical Atolls", "Paradise islands with pristine beaches and palm trees",
            28.0f, 4.0f, 85.0f, 0.7f, 0.4f,
            0.1f, 0.8f, 0.1f,
            new int[]{0x10B981, 0x34D399, 0x6EE7B7}, 0.0f, 12000.0f
        );
        tropicalAtolls.flora.addAll(Arrays.asList("palm_trees", "coconut_palms", "tropical_flowers", "beach_grass"));
        tropicalAtolls.fauna.addAll(Arrays.asList("parrots", "hermit_crabs", "iguanas", "sea_birds"));
        tropicalAtolls.resources.addAll(Arrays.asList("coconuts", "palm_wood", "tropical_fruits", "fresh_water"));
        tropicalAtolls.specialFeatures.addAll(Arrays.asList("natural_harbors", "coral_lagoons", "hidden_springs"));
        biomeDefinitions.put(BiomeType.TROPICAL_ATOLLS, tropicalAtolls);
        
        BiomeData volcanicSpires = new BiomeData(
            BiomeType.VOLCANIC_SPIRES, "Volcanic Spires", "Dramatic peaks rising from the sea, rich in rare materials",
            30.0f, 8.0f, 60.0f, 0.2f, 0.7f,
            0.6f, 0.7f, 0.8f,
            new int[]{0x7C2D12, 0xDC2626, 0xFCA5A5}, 0.3f, 6000.0f
        );
        volcanicSpires.flora.addAll(Arrays.asList("volcanic_ferns", "hardy_shrubs", "sulfur_moss"));
        volcanicSpires.fauna.addAll(Arrays.asList("lava_lizards", "thermal_crabs", "sulfur_birds"));
        volcanicSpires.resources.addAll(Arrays.asList("obsidian", "sulfur", "rare_gems", "volcanic_glass", "hot_springs"));
        volcanicSpires.specialFeatures.addAll(Arrays.asList("active_vents", "lava_tubes", "mineral_deposits", "geysers"));
        biomeDefinitions.put(BiomeType.VOLCANIC_SPIRES, volcanicSpires);
        
        BiomeData denseJungles = new BiomeData(
            BiomeType.DENSE_JUNGLES, "Dense Jungles", "Massive islands with towering trees and hidden temples",
            26.0f, 6.0f, 95.0f, 0.9f, 0.2f,
            0.7f, 0.9f, 0.6f,
            new int[]{0x065F46, 0x10B981, 0x6EE7B7}, 0.4f, 3000.0f
        );
        denseJungles.flora.addAll(Arrays.asList("giant_trees", "tropical_vines", "exotic_flowers", "medicinal_herbs"));
        denseJungles.fauna.addAll(Arrays.asList("jungle_cats", "monkeys", "exotic_birds", "giant_snakes"));
        denseJungles.resources.addAll(Arrays.asList("hardwood", "exotic_fruits", "medicinal_plants", "rare_spices"));
        denseJungles.specialFeatures.addAll(Arrays.asList("ancient_temples", "hidden_rivers", "canopy_bridges", "sacred_groves"));
        biomeDefinitions.put(BiomeType.DENSE_JUNGLES, denseJungles);
        
        BiomeData arcticArchipelagos = new BiomeData(
            BiomeType.ARCTIC_ARCHIPELAGOS, "Arctic Archipelagos", "Frozen northern territories with unique challenges",
            -5.0f, 10.0f, 75.0f, 0.3f, 0.8f,
            0.8f, 0.6f, 0.7f,
            new int[]{0xE0F2FE, 0xBAE6FD, 0x7DD3FC}, 0.6f, 4000.0f
        );
        arcticArchipelagos.flora.addAll(Arrays.asList("hardy_pines", "arctic_moss", "ice_flowers"));
        arcticArchipelagos.fauna.addAll(Arrays.asList("polar_bears", "seals", "arctic_foxes", "whales"));
        arcticArchipelagos.resources.addAll(Arrays.asList("pine_wood", "seal_blubber", "ice_crystals", "whale_bone"));
        arcticArchipelagos.specialFeatures.addAll(Arrays.asList("icebergs", "frozen_caves", "aurora_phenomena", "thermal_vents"));
        biomeDefinitions.put(BiomeType.ARCTIC_ARCHIPELAGOS, arcticArchipelagos);
        
        // Special Biomes
        BiomeData whisperingIsles = new BiomeData(
            BiomeType.WHISPERING_ISLES, "Whispering Isles", "Cursed lands shrouded in eternal fog and mystery",
            15.0f, 3.0f, 90.0f, 0.1f, 0.3f,
            0.9f, 0.5f, 0.9f,
            new int[]{0x374151, 0x6B7280, 0x9CA3AF}, 0.9f, 500.0f
        );
        whisperingIsles.flora.addAll(Arrays.asList("dead_trees", "ghost_moss", "spectral_flowers"));
        whisperingIsles.fauna.addAll(Arrays.asList("ghost_crabs", "phantom_fish", "will_o_wisps"));
        whisperingIsles.resources.addAll(Arrays.asList("cursed_wood", "ectoplasm", "spirit_gems", "ancient_bones"));
        whisperingIsles.specialFeatures.addAll(Arrays.asList("ghost_ships", "spectral_ruins", "haunted_caves", "cursed_treasures"));
        biomeDefinitions.put(BiomeType.WHISPERING_ISLES, whisperingIsles);
        
        BiomeData temporalAnomalies = new BiomeData(
            BiomeType.TEMPORAL_ANOMALIES, "Temporal Anomalies", "Islands where time flows differently",
            20.0f, 15.0f, 50.0f, 0.2f, 0.5f,
            0.95f, 0.8f, 0.95f,
            new int[]{0x8B5CF6, 0xA78BFA, 0xC4B5FD}, 0.2f, 8000.0f
        );
        temporalAnomalies.flora.addAll(Arrays.asList("time_flowers", "eternal_trees", "chronos_crystals"));
        temporalAnomalies.fauna.addAll(Arrays.asList("time_fish", "temporal_birds", "chrono_butterflies"));
        temporalAnomalies.resources.addAll(Arrays.asList("time_crystals", "temporal_wood", "chronos_essence"));
        temporalAnomalies.specialFeatures.addAll(Arrays.asList("time_rifts", "temporal_storms", "aging_pools", "time_loops"));
        biomeDefinitions.put(BiomeType.TEMPORAL_ANOMALIES, temporalAnomalies);
        
        // Additional Ocean Biomes
        BiomeData shallowWaters = new BiomeData(
            BiomeType.SHALLOW_WATERS, "Shallow Waters", "Clear, shallow coastal waters perfect for navigation",
            22.0f, 4.0f, 75.0f, 0.4f, 0.4f,
            0.1f, 0.5f, 0.1f,
            new int[]{0x3B82F6, 0x60A5FA, 0x93C5FD}, 0.05f, 20000.0f
        );
        shallowWaters.flora.addAll(Arrays.asList("sea_grass", "coral_patches", "algae"));
        shallowWaters.fauna.addAll(Arrays.asList("small_fish", "crabs", "sea_birds", "rays"));
        shallowWaters.resources.addAll(Arrays.asList("fish", "shells", "sea_salt", "pearls"));
        shallowWaters.specialFeatures.addAll(Arrays.asList("sandbanks", "tidal_pools", "coral_gardens"));
        biomeDefinitions.put(BiomeType.SHALLOW_WATERS, shallowWaters);
        
        // Special Island Biomes
        BiomeData floatingGardens = new BiomeData(
            BiomeType.FLOATING_GARDENS, "Floating Gardens", "Mystical islands that hover above the water",
            24.0f, 5.0f, 80.0f, 0.6f, 0.3f,
            0.8f, 0.9f, 0.4f,
            new int[]{0x10B981, 0x6EE7B7, 0xA7F3D0}, 0.2f, 10000.0f
        );
        floatingGardens.flora.addAll(Arrays.asList("floating_vines", "sky_flowers", "cloud_moss", "aerial_roots"));
        floatingGardens.fauna.addAll(Arrays.asList("sky_fish", "wind_sprites", "floating_jellyfish", "cloud_birds"));
        floatingGardens.resources.addAll(Arrays.asList("sky_crystals", "wind_essence", "floating_wood", "cloud_water"));
        floatingGardens.specialFeatures.addAll(Arrays.asList("gravity_wells", "wind_currents", "sky_bridges", "floating_ruins"));
        biomeDefinitions.put(BiomeType.FLOATING_GARDENS, floatingGardens);
        
        BiomeData magneticAnomalies = new BiomeData(
            BiomeType.MAGNETIC_ANOMALIES, "Magnetic Anomalies", "Islands with strange magnetic properties that affect navigation",
            18.0f, 8.0f, 65.0f, 0.3f, 0.6f,
            0.9f, 0.6f, 0.7f,
            new int[]{0x7C3AED, 0x8B5CF6, 0xA78BFA}, 0.4f, 7000.0f
        );
        magneticAnomalies.flora.addAll(Arrays.asList("iron_trees", "magnetic_moss", "compass_flowers"));
        magneticAnomalies.fauna.addAll(Arrays.asList("magnetic_fish", "iron_crabs", "compass_birds"));
        magneticAnomalies.resources.addAll(Arrays.asList("magnetic_ore", "iron_crystals", "compass_stones", "lodestone"));
        magneticAnomalies.specialFeatures.addAll(Arrays.asList("magnetic_storms", "compass_disruption", "metal_deposits", "aurora_fields"));
        biomeDefinitions.put(BiomeType.MAGNETIC_ANOMALIES, magneticAnomalies);
        
        // Add seasonal modifiers to all biomes
        for (BiomeData biome : biomeDefinitions.values()) {
            addSeasonalModifiers(biome);
        }
    }
    
    private void addSeasonalModifiers(BiomeData biome) {
        // Spring: mild temperatures, increased rainfall
        SeasonalModifier spring = new SeasonalModifier(0.0f, 10.0f, 0.3f, -0.1f);
        spring.seasonalEvents.addAll(Arrays.asList("migration_season", "spawning_season", "bloom_season"));
        biome.seasonalModifiers.put(Season.SPRING, spring);
        
        // Summer: higher temperatures, less rain
        SeasonalModifier summer = new SeasonalModifier(8.0f, -10.0f, -0.2f, 0.2f);
        summer.seasonalEvents.addAll(Arrays.asList("storm_season", "drought_risk", "high_activity"));
        biome.seasonalModifiers.put(Season.SUMMER, summer);
        
        // Autumn: cooling temperatures, variable weather
        SeasonalModifier autumn = new SeasonalModifier(-3.0f, 5.0f, 0.1f, 0.3f);
        autumn.seasonalEvents.addAll(Arrays.asList("harvest_season", "return_migration", "storm_activity"));
        biome.seasonalModifiers.put(Season.AUTUMN, autumn);
        
        // Winter: cold temperatures, different patterns by biome type
        float winterTemp = switch (biome.type) {
            case TROPICAL_ATOLLS, CORAL_REEFS -> -2.0f; // Mild winter
            case ARCTIC_ARCHIPELAGOS -> -15.0f; // Harsh winter
            default -> -8.0f; // Moderate winter
        };
        
        SeasonalModifier winter = new SeasonalModifier(winterTemp, -5.0f, -0.1f, 0.4f);
        winter.seasonalEvents.addAll(Arrays.asList("dormant_season", "ice_formation", "survival_challenge"));
        biome.seasonalModifiers.put(Season.WINTER, winter);
    }
    
    private void generateBiomeRegions(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (random.nextFloat() - 0.5f) * 6000, // 6km spread
                (random.nextFloat() - 0.5f) * 6000
            );
            
            // Select biome type based on location and climate
            BiomeType biomeType = selectBiomeForLocation(center);
            float radius = 200 + random.nextFloat() * 800; // 200-1000 block radius
            float influence = 0.5f + random.nextFloat() * 0.5f; // 0.5-1.0 influence
            
            BiomeRegion region = new BiomeRegion(center, radius, biomeType, influence);
            
            // Set initial environmental values
            BiomeData biomeData = biomeDefinitions.get(biomeType);
            if (biomeData != null) {
                region.currentTemperature = biomeData.baseTemperature + 
                                          (random.nextFloat() - 0.5f) * biomeData.temperatureVariation;
                region.currentHumidity = biomeData.humidity + (random.nextFloat() - 0.5f) * 20.0f;
                region.currentWindiness = biomeData.windiness + (random.nextFloat() - 0.5f) * 0.4f;
            } else {
                // Fallback values if biome data is missing
                logger.warn("Missing biome data for type: {}, using default values", biomeType);
                region.currentTemperature = 20.0f + (random.nextFloat() - 0.5f) * 10.0f;
                region.currentHumidity = 50.0f + (random.nextFloat() - 0.5f) * 20.0f;
                region.currentWindiness = 0.5f + (random.nextFloat() - 0.5f) * 0.4f;
            }
            
            biomeRegions.put(center, region);
        }
    }
    
    private BiomeType selectBiomeForLocation(Vector2f location) {
        // Use location to determine appropriate biome types
        float distanceFromCenter = location.length();
        float latitude = Math.abs(location.y) / 3000.0f; // Normalized latitude (0-1)
        
        // Arctic regions in far north/south
        if (latitude > 0.7f) {
            return Math.random() < 0.8 ? BiomeType.ARCTIC_ARCHIPELAGOS : BiomeType.OPEN_OCEAN;
        }
        
        // Tropical regions near equator
        if (latitude < 0.3f) {
            BiomeType[] tropicalBiomes = {
                BiomeType.TROPICAL_ATOLLS, BiomeType.CORAL_REEFS, 
                BiomeType.DENSE_JUNGLES, BiomeType.VOLCANIC_SPIRES
            };
            return tropicalBiomes[random.nextInt(tropicalBiomes.length)];
        }
        
        // Special biomes (rare)
        if (random.nextFloat() < 0.1f) {
            BiomeType[] specialBiomes = {
                BiomeType.WHISPERING_ISLES, BiomeType.TEMPORAL_ANOMALIES,
                BiomeType.FLOATING_GARDENS, BiomeType.MAGNETIC_ANOMALIES
            };
            return specialBiomes[random.nextInt(specialBiomes.length)];
        }
        
        // Default to ocean biomes
        return distanceFromCenter < 1000 ? BiomeType.SHALLOW_WATERS : BiomeType.OPEN_OCEAN;
    }
    
    private void initializeClimateSystem() {
        // Generate climate data for a grid of points
        int gridSize = 32;
        float cellSize = 200.0f;
        
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                Vector2f position = new Vector2f(
                    (x - gridSize / 2.0f) * cellSize,
                    (z - gridSize / 2.0f) * cellSize
                );
                
                ClimateData climate = new ClimateData();
                
                // Base climate from nearest biome
                BiomeRegion nearestBiome = findNearestBiomeRegion(position);
                if (nearestBiome != null) {
                    BiomeData biomeData = biomeDefinitions.get(nearestBiome.biomeType);
                    if (biomeData != null) {
                        climate.temperature = biomeData.baseTemperature;
                        climate.humidity = biomeData.humidity;
                        climate.rainfall = biomeData.rainfall;
                        climate.pressure = 1013.25f; // Standard sea level pressure
                        climate.windVector.set(
                            (random.nextFloat() - 0.5f) * biomeData.windiness,
                            (random.nextFloat() - 0.5f) * biomeData.windiness
                        );
                    } else {
                        // Default climate values
                        climate.temperature = 20.0f;
                        climate.humidity = 50.0f;
                        climate.rainfall = 0.5f;
                        climate.pressure = 1013.25f;
                        climate.windVector.set(0.0f, 0.0f);
                    }
                }
                
                climateMap.put(position, climate);
            }
        }
    }
    
    public void update(double deltaTime) {
        seasonalTimer += deltaTime;
        
        // Update season (assuming 1 hour real time = 1 day game time)
        double yearProgress = (seasonalTimer / 3600.0) % 1.0; // 1 hour = 1 year
        Season newSeason = getCurrentSeasonFromProgress(yearProgress);
        
        if (newSeason != currentSeason) {
            logger.info("Season changed from {} to {}", currentSeason, newSeason);
            currentSeason = newSeason;
            applySeasonalChanges();
        }
        
        // Update biome transitions
        updateBiomeTransitions(deltaTime);
        
        // Update regional environmental factors
        updateRegionalEnvironments(deltaTime);
        
        // Process biome events
        processBiomeEvents(deltaTime);
    }
    
    private Season getCurrentSeasonFromProgress(double yearProgress) {
        float progress = (float)yearProgress;
        for (Season season : Season.values()) {
            if (progress >= season.start && progress < season.end) {
                return season;
            }
        }
        return Season.WINTER; // Wrap around
    }
    
    private void applySeasonalChanges() {
        for (BiomeRegion region : biomeRegions.values()) {
            BiomeData biomeData = biomeDefinitions.get(region.biomeType);
            if (biomeData != null) {
                SeasonalModifier modifier = biomeData.seasonalModifiers.get(currentSeason);
                
                if (modifier != null) {
                    region.currentTemperature = biomeData.baseTemperature + modifier.temperatureChange;
                    region.currentHumidity = Math.max(0, Math.min(100, 
                        biomeData.humidity + modifier.humidityChange));
                    region.currentWindiness = Math.max(0, 
                        biomeData.windiness + modifier.windinessChange);
                    
                    // Add seasonal events
                    region.activeEvents.clear();
                    region.activeEvents.addAll(modifier.seasonalEvents);
                }
            }
        }
    }
    
    private void updateBiomeTransitions(double deltaTime) {
        Iterator<BiomeTransition> iterator = activeTransitions.iterator();
        
        while (iterator.hasNext()) {
            BiomeTransition transition = iterator.next();
            
            double currentTime = System.currentTimeMillis() / 1000.0;
            double elapsed = currentTime - transition.startTime;
            transition.progress = Math.min(1.0f, (float)(elapsed / transition.duration));
            
            if (transition.progress >= 1.0f) {
                // Transition complete
                completeBiomeTransition(transition);
                iterator.remove();
            }
        }
    }
    
    private void completeBiomeTransition(BiomeTransition transition) {
        // Find and update the biome region
        BiomeRegion region = findNearestBiomeRegion(transition.position);
        if (region != null && region.center.distance(transition.position) <= transition.transitionRadius) {
            logger.info("Biome transition completed: {} -> {} at {}", 
                       transition.fromBiome, transition.toBiome, transition.position);
            
            // Update the region's biome type
            BiomeRegion newRegion = new BiomeRegion(
                region.center, region.radius, transition.toBiome, region.influence
            );
            biomeRegions.put(region.center, newRegion);
        }
    }
    
    private void updateRegionalEnvironments(double deltaTime) {
        for (BiomeRegion region : biomeRegions.values()) {
            // Gradual environmental changes
            float tempVariation = (random.nextFloat() - 0.5f) * (float)deltaTime * 0.1f;
            region.currentTemperature += tempVariation;
            
            float humidityVariation = (random.nextFloat() - 0.5f) * (float)deltaTime * 2.0f;
            region.currentHumidity = Math.max(0, Math.min(100, region.currentHumidity + humidityVariation));
        }
    }
    
    private void processBiomeEvents(double deltaTime) {
        for (BiomeRegion region : biomeRegions.values()) {
            // Random chance of special biome events
            if (random.nextFloat() < deltaTime * 0.001) { // 0.1% chance per second
                triggerRandomBiomeEvent(region);
            }
        }
    }
    
    private void triggerRandomBiomeEvent(BiomeRegion region) {
        BiomeData biomeData = biomeDefinitions.get(region.biomeType);
        if (biomeData != null) {
            String[] possibleEvents = biomeData.specialFeatures.toArray(new String[0]);
            
            if (possibleEvents.length > 0) {
                String event = possibleEvents[random.nextInt(possibleEvents.length)];
                region.activeEvents.add(event);
                
                logger.debug("Biome event '{}' triggered in {} at {}", 
                            event, biomeData.name, region.center);
            }
        }
    }
    
    public BiomeType getBiomeAt(Vector2f position) {
        BiomeRegion nearest = findNearestBiomeRegion(position);
        
        if (nearest != null) {
            float distance = position.distance(nearest.center);
            if (distance <= nearest.radius) {
                return nearest.biomeType;
            }
        }
        
        // Default to open ocean if no biome regions nearby
        return BiomeType.OPEN_OCEAN;
    }
    
    public BiomeData getBiomeData(BiomeType biomeType) {
        return biomeDefinitions.get(biomeType);
    }
    
    public ClimateData getClimateAt(Vector2f position) {
        // Find nearest climate data point
        ClimateData nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (Map.Entry<Vector2f, ClimateData> entry : climateMap.entrySet()) {
            float distance = position.distance(entry.getKey());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = entry.getValue();
            }
        }
        
        return nearest;
    }
    
    private BiomeRegion findNearestBiomeRegion(Vector2f position) {
        BiomeRegion nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (BiomeRegion region : biomeRegions.values()) {
            float distance = position.distance(region.center);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = region;
            }
        }
        
        return nearest;
    }
    
    public void initiateBiomeTransition(Vector2f position, BiomeType newBiomeType, 
                                       float radius, double duration) {
        BiomeType currentBiome = getBiomeAt(position);
        
        if (currentBiome != newBiomeType) {
            BiomeTransition transition = new BiomeTransition(
                position, currentBiome, newBiomeType, radius, duration
            );
            
            activeTransitions.add(transition);
            
            logger.info("Initiated biome transition from {} to {} at {} over {:.1f} minutes", 
                       currentBiome, newBiomeType, position, duration / 60.0);
        }
    }
    
    public List<BiomeRegion> getBiomeRegionsNear(Vector2f position, float radius) {
        List<BiomeRegion> nearby = new ArrayList<>();
        
        for (BiomeRegion region : biomeRegions.values()) {
            float distance = position.distance(region.center);
            if (distance <= radius + region.radius) {
                nearby.add(region);
            }
        }
        
        return nearby;
    }
    
    public float getBiodiversityIndex(Vector2f position, float radius) {
        Set<BiomeType> uniqueBiomes = new HashSet<>();
        
        for (BiomeRegion region : biomeRegions.values()) {
            if (position.distance(region.center) <= radius) {
                uniqueBiomes.add(region.biomeType);
            }
        }
        
        return (float)uniqueBiomes.size() / BiomeType.values().length;
    }
    
    public void cleanup() {
        logger.info("Cleaning up biome manager");
        biomeDefinitions.clear();
        biomeRegions.clear();
        activeTransitions.clear();
        climateMap.clear();
    }
    
    // Getters
    public Season getCurrentSeason() { return currentSeason; }
    public double getSeasonalTimer() { return seasonalTimer; }
    public int getBiomeRegionCount() { return biomeRegions.size(); }
    public int getActiveTransitionCount() { return activeTransitions.size(); }
    
    public Map<BiomeType, BiomeData> getBiomeDefinitions() {
        return new EnumMap<>(biomeDefinitions);
    }
    
    public Collection<BiomeRegion> getAllBiomeRegions() {
        return new ArrayList<>(biomeRegions.values());
    }
    
    public List<BiomeTransition> getActiveTransitions() {
        return new ArrayList<>(activeTransitions);
    }
}