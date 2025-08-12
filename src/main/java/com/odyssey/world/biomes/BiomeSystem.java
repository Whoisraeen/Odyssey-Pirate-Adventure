package com.odyssey.world.biomes;

import com.odyssey.core.Engine;
import com.odyssey.world.generation.WorldGenerator.NoiseGenerator;
import com.odyssey.world.biome.BiomeManager.ClimateData;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprehensive biome system for managing different biomes and their properties.
 * Handles biome generation, transitions, climate effects, and maritime-specific features.
 */
public class BiomeSystem {
    private static final Logger logger = LoggerFactory.getLogger(BiomeSystem.class);
    
    private final Map<String, BiomeType> registeredBiomes;
    private final Map<Integer, BiomeInstance> activeBiomes;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final NoiseGenerator elevationNoise;
    private final NoiseGenerator oceanCurrentNoise;
    private final BiomeTransitionManager transitionManager;
    
    // Biome generation parameters
    private double temperatureScale = 0.02;
    private double humidityScale = 0.025;
    private double elevationScale = 0.01;
    private double oceanCurrentScale = 0.008;
    private int biomeBlendRadius = 8;
    
    public BiomeSystem(long seed) {
        this.registeredBiomes = new ConcurrentHashMap<>();
        this.activeBiomes = new ConcurrentHashMap<>();
        this.temperatureNoise = new NoiseGenerator(seed);
        this.humidityNoise = new NoiseGenerator(seed + 1000);
        this.elevationNoise = new NoiseGenerator(seed + 2000);
        this.oceanCurrentNoise = new NoiseGenerator(seed + 3000);
        this.transitionManager = new BiomeTransitionManager();
        
        registerDefaultBiomes();
        logger.info("Biome system initialized with seed: {}", seed);
    }
    
    /**
     * Registers default biomes
     */
    private void registerDefaultBiomes() {
        // Ocean biomes
        registerBiome(createOceanBiome());
        registerBiome(createDeepOceanBiome());
        registerBiome(createWarmOceanBiome());
        registerBiome(createColdOceanBiome());
        registerBiome(createFrozenOceanBiome());
        
        // Coastal biomes
        registerBiome(createBeachBiome());
        registerBiome(createRockyCoastBiome());
        registerBiome(createMangroveSwampBiome());
        registerBiome(createCoralReefBiome());
        registerBiome(createIcebergBiome());
        
        // Island biomes
        registerBiome(createTropicalIslandBiome());
        registerBiome(createVolcanicIslandBiome());
        registerBiome(createDesertIslandBiome());
        registerBiome(createTundraIslandBiome());
        registerBiome(createJungleIslandBiome());
        
        // Special biomes
        registerBiome(createShipwreckFieldBiome());
        registerBiome(createKelpForestBiome());
        registerBiome(createAbyssalPlainBiome());
        registerBiome(createHydrothermalVentBiome());
        
        logger.info("Registered {} default biomes", registeredBiomes.size());
    }
    
    /**
     * Creates ocean biome
     */
    private BiomeType createOceanBiome() {
        return new BiomeType.Builder("ocean")
            .temperature(0.5f)
            .humidity(1.0f)
            .elevation(-0.3f)
            .color(0x3F76E4)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.OCEAN)
            .addFeature("kelp", 0.3f)
            .addFeature("fish_schools", 0.6f)
            .addFeature("sea_grass", 0.4f)
            .addMobSpawn("cod", 10, 4, 8)
            .addMobSpawn("salmon", 5, 1, 5)
            .addMobSpawn("squid", 8, 1, 4)
            .addMobSpawn("dolphin", 2, 1, 2)
            .setDepthRange(10, 40)
            .setWaveIntensity(0.6f)
            .setCurrentStrength(0.4f)
            .build();
    }
    
    /**
     * Creates deep ocean biome
     */
    private BiomeType createDeepOceanBiome() {
        return new BiomeType.Builder("deep_ocean")
            .temperature(0.3f)
            .humidity(1.0f)
            .elevation(-0.8f)
            .color(0x1A237E)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.OCEAN)
            .addFeature("deep_kelp", 0.2f)
            .addFeature("abyssal_creatures", 0.1f)
            .addFeature("underwater_caves", 0.15f)
            .addMobSpawn("guardian", 3, 1, 2)
            .addMobSpawn("elder_guardian", 1, 1, 1)
            .addMobSpawn("giant_squid", 1, 1, 1)
            .setDepthRange(40, 100)
            .setWaveIntensity(0.8f)
            .setCurrentStrength(0.6f)
            .setPressure(2.0f)
            .build();
    }
    
    /**
     * Creates warm ocean biome
     */
    private BiomeType createWarmOceanBiome() {
        return new BiomeType.Builder("warm_ocean")
            .temperature(0.8f)
            .humidity(1.0f)
            .elevation(-0.2f)
            .color(0x43A5BE)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.OCEAN)
            .addFeature("coral_reefs", 0.7f)
            .addFeature("tropical_fish", 0.8f)
            .addFeature("sea_pickles", 0.5f)
            .addMobSpawn("tropical_fish", 15, 8, 16)
            .addMobSpawn("pufferfish", 3, 1, 3)
            .addMobSpawn("turtle", 5, 2, 5)
            .setDepthRange(5, 25)
            .setWaveIntensity(0.4f)
            .setCurrentStrength(0.3f)
            .setVisibility(1.5f)
            .build();
    }
    
    /**
     * Creates tropical island biome
     */
    private BiomeType createTropicalIslandBiome() {
        return new BiomeType.Builder("tropical_island")
            .temperature(0.9f)
            .humidity(0.8f)
            .elevation(0.3f)
            .color(0x7FFF00)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.ISLAND)
            .addFeature("palm_trees", 0.8f)
            .addFeature("coconuts", 0.6f)
            .addFeature("tropical_fruits", 0.7f)
            .addFeature("bamboo", 0.4f)
            .addMobSpawn("parrot", 8, 2, 4)
            .addMobSpawn("ocelot", 3, 1, 2)
            .addMobSpawn("turtle", 5, 1, 3)
            .setErosionRate(0.1f)
            .setFertility(0.9f)
            .build();
    }
    
    /**
     * Creates volcanic island biome
     */
    private BiomeType createVolcanicIslandBiome() {
        return new BiomeType.Builder("volcanic_island")
            .temperature(1.2f)
            .humidity(0.3f)
            .elevation(0.8f)
            .color(0x8B0000)
            .precipitation(BiomeType.Precipitation.NONE)
            .category(BiomeType.Category.ISLAND)
            .addFeature("lava_flows", 0.6f)
            .addFeature("obsidian_deposits", 0.8f)
            .addFeature("volcanic_ash", 0.9f)
            .addFeature("geysers", 0.3f)
            .addMobSpawn("magma_cube", 5, 2, 4)
            .addMobSpawn("strider", 3, 1, 2)
            .setErosionRate(0.3f)
            .setFertility(0.2f)
            .setGeothermalActivity(0.9f)
            .build();
    }
    
    /**
     * Creates mangrove swamp biome
     */
    private BiomeType createMangroveSwampBiome() {
        return new BiomeType.Builder("mangrove_swamp")
            .temperature(0.8f)
            .humidity(0.9f)
            .elevation(0.0f)
            .color(0x6A7039)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.COASTAL)
            .addFeature("mangrove_trees", 0.9f)
            .addFeature("muddy_water", 0.8f)
            .addFeature("fireflies", 0.6f)
            .addFeature("lily_pads", 0.7f)
            .addMobSpawn("frog", 10, 2, 5)
            .addMobSpawn("crocodile", 2, 1, 2)
            .addMobSpawn("firefly", 15, 5, 10)
            .setTidalRange(0.8f)
            .setFertility(0.8f)
            .build();
    }
    
    /**
     * Creates coral reef biome
     */
    private BiomeType createCoralReefBiome() {
        return new BiomeType.Builder("coral_reef")
            .temperature(0.8f)
            .humidity(1.0f)
            .elevation(-0.1f)
            .color(0xFF6B9D)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.COASTAL)
            .addFeature("coral_formations", 0.95f)
            .addFeature("sea_anemones", 0.7f)
            .addFeature("colorful_fish", 0.9f)
            .addFeature("sea_fans", 0.6f)
            .addMobSpawn("tropical_fish", 20, 10, 30)
            .addMobSpawn("angelfish", 8, 3, 6)
            .addMobSpawn("clownfish", 12, 4, 8)
            .addMobSpawn("sea_turtle", 3, 1, 2)
            .setDepthRange(2, 15)
            .setVisibility(2.0f)
            .setBiodiversity(0.95f)
            .build();
    }
    
    /**
     * Creates shipwreck field biome
     */
    private BiomeType createShipwreckFieldBiome() {
        return new BiomeType.Builder("shipwreck_field")
            .temperature(0.4f)
            .humidity(1.0f)
            .elevation(-0.5f)
            .color(0x2F4F4F)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.SPECIAL)
            .addFeature("shipwrecks", 0.8f)
            .addFeature("treasure_chests", 0.4f)
            .addFeature("ghost_ships", 0.1f)
            .addFeature("cursed_artifacts", 0.2f)
            .addMobSpawn("drowned", 8, 2, 4)
            .addMobSpawn("phantom", 3, 1, 2)
            .addMobSpawn("ghost_pirate", 2, 1, 1)
            .setDepthRange(20, 60)
            .setDangerLevel(0.8f)
            .setMysticalEnergy(0.7f)
            .build();
    }
    
    /**
     * Registers a biome type
     */
    public void registerBiome(BiomeType biome) {
        registeredBiomes.put(biome.getName(), biome);
        logger.debug("Registered biome: {}", biome.getName());
    }
    
    /**
     * Gets a biome at the specified coordinates
     */
    public BiomeInstance getBiomeAt(int x, int z) {
        int biomeId = getBiomeId(x, z);
        
        return activeBiomes.computeIfAbsent(biomeId, id -> {
            BiomeType type = determineBiomeType(x, z);
            return new BiomeInstance(type, x, z, generateBiomeVariant(type, x, z));
        });
    }
    
    /**
     * Determines the biome type at coordinates
     */
    private BiomeType determineBiomeType(int x, int z) {
        float temperature = temperatureNoise.noise((float)(x * temperatureScale), (float)(z * temperatureScale));
        float humidity = humidityNoise.noise((float)(x * humidityScale), (float)(z * humidityScale));
        float elevation = elevationNoise.noise((float)(x * elevationScale), (float)(z * elevationScale));
        float oceanCurrent = oceanCurrentNoise.noise((float)(x * oceanCurrentScale), (float)(z * oceanCurrentScale));
        
        // Normalize values to 0-1 range
        temperature = (temperature + 1.0f) * 0.5f;
        humidity = (humidity + 1.0f) * 0.5f;
        elevation = (elevation + 1.0f) * 0.5f;
        oceanCurrent = (oceanCurrent + 1.0f) * 0.5f;
        
        return selectBiomeByClimate(temperature, humidity, elevation, oceanCurrent);
    }
    
    /**
     * Selects biome based on climate parameters
     */
    private BiomeType selectBiomeByClimate(double temperature, double humidity, double elevation, double oceanCurrent) {
        // Ocean biomes (elevation < 0.4)
        if (elevation < 0.4) {
            if (elevation < 0.2) {
                // Deep ocean
                if (temperature < 0.3) {
                    return registeredBiomes.get("frozen_ocean");
                } else if (temperature > 0.7) {
                    return registeredBiomes.get("warm_ocean");
                } else {
                    return registeredBiomes.get("deep_ocean");
                }
            } else {
                // Shallow ocean
                if (temperature < 0.4) {
                    return registeredBiomes.get("cold_ocean");
                } else if (temperature > 0.7) {
                    return registeredBiomes.get("warm_ocean");
                } else {
                    return registeredBiomes.get("ocean");
                }
            }
        }
        
        // Coastal biomes (elevation 0.4-0.5)
        if (elevation < 0.5) {
            if (temperature > 0.7 && humidity > 0.6) {
                return registeredBiomes.get("mangrove_swamp");
            } else if (temperature > 0.6) {
                return registeredBiomes.get("coral_reef");
            } else if (temperature < 0.3) {
                return registeredBiomes.get("iceberg");
            } else {
                return Math.random() < 0.7 ? 
                    registeredBiomes.get("beach") : 
                    registeredBiomes.get("rocky_coast");
            }
        }
        
        // Island biomes (elevation > 0.5)
        if (temperature > 0.8 && humidity > 0.6) {
            return registeredBiomes.get("tropical_island");
        } else if (temperature > 0.9 && humidity < 0.4) {
            return registeredBiomes.get("volcanic_island");
        } else if (temperature > 0.7 && humidity > 0.7) {
            return registeredBiomes.get("jungle_island");
        } else if (temperature < 0.3) {
            return registeredBiomes.get("tundra_island");
        } else if (temperature > 0.6 && humidity < 0.3) {
            return registeredBiomes.get("desert_island");
        }
        
        // Default to tropical island
        return registeredBiomes.get("tropical_island");
    }
    
    /**
     * Generates biome variant data
     */
    private BiomeVariant generateBiomeVariant(BiomeType type, int x, int z) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        BiomeVariant variant = new BiomeVariant();
        variant.setTemperatureModifier((float) (random.nextGaussian() * 0.1));
        variant.setHumidityModifier((float) (random.nextGaussian() * 0.1));
        variant.setElevationModifier((float) (random.nextGaussian() * 0.05));
        variant.setFeatureIntensity(0.8f + random.nextFloat() * 0.4f);
        variant.setRarityBonus(random.nextFloat() < 0.1f ? 1.5f : 1.0f);
        
        // Special variants based on coordinates
        if ((x + z) % 1000 == 0) {
            variant.setSpecialVariant(true);
            variant.setSpecialType("legendary");
            variant.setRarityBonus(3.0f);
        } else if ((x * z) % 500 == 0) {
            variant.setSpecialVariant(true);
            variant.setSpecialType("rare");
            variant.setRarityBonus(2.0f);
        }
        
        return variant;
    }
    
    /**
     * Gets biome ID for caching
     */
    private int getBiomeId(int x, int z) {
        // Reduce precision for biome caching
        int biomeX = x >> 4; // Divide by 16
        int biomeZ = z >> 4;
        return (biomeX << 16) | (biomeZ & 0xFFFF);
    }
    
    /**
     * Gets blended biome data for smooth transitions
     */
    public BlendedBiomeData getBlendedBiomeData(int x, int z) {
        return transitionManager.getBlendedData(x, z, biomeBlendRadius, this);
    }
    
    /**
     * Updates biome system
     */
    public void update(float deltaTime) {
        // Climate updates are handled by individual biome instances
        transitionManager.update(deltaTime);
        
        // Update active biome instances
        for (BiomeInstance instance : activeBiomes.values()) {
            instance.update(deltaTime);
        }
        
        // Clean up distant biomes
        cleanupDistantBiomes();
    }
    
    /**
     * Cleans up biomes that are far from active areas
     */
    private void cleanupDistantBiomes() {
        if (activeBiomes.size() > 1000) {
            // Simple cleanup - remove random biomes
            Iterator<Map.Entry<Integer, BiomeInstance>> iterator = activeBiomes.entrySet().iterator();
            int removed = 0;
            while (iterator.hasNext() && removed < 200) {
                iterator.next();
                if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                    iterator.remove();
                    removed++;
                }
            }
            logger.debug("Cleaned up {} distant biomes", removed);
        }
    }
    
    /**
     * Gets all registered biomes
     */
    public Collection<BiomeType> getRegisteredBiomes() {
        return Collections.unmodifiableCollection(registeredBiomes.values());
    }
    
    /**
     * Gets biome by name
     */
    public BiomeType getBiomeType(String name) {
        return registeredBiomes.get(name);
    }
    
    /**
     * Gets climate data at coordinates
     */
    public ClimateData getClimateAt(int x, int z) {
        // Generate climate data based on biome at location
        BiomeInstance biomeInstance = getBiomeAt(x, z);
        BiomeType biome = biomeInstance.getType();
        ClimateData climate = new ClimateData();
        
        // Base values from biome
        climate.temperature = biome.getTemperature() * 30.0f; // Convert to Celsius-like scale
        climate.humidity = biome.getHumidity() * 100.0f; // Convert to percentage
        climate.pressure = 1013.25f; // Standard atmospheric pressure
        climate.rainfall = biome.getHumidity() * 0.8f; // Rainfall based on humidity
        
        // Add some noise variation
        float tempNoise = temperatureNoise.noise((float)(x * temperatureScale), (float)(z * temperatureScale));
        float humidityNoise = this.humidityNoise.noise((float)(x * humidityScale), (float)(z * humidityScale));
        
        climate.temperature += tempNoise * 5.0f;
        climate.humidity += humidityNoise * 20.0f;
        climate.humidity = Math.max(0, Math.min(100, climate.humidity));
        
        // Wind vector based on ocean currents
        float windX = oceanCurrentNoise.noise((float)(x * oceanCurrentScale), (float)(z * oceanCurrentScale));
        float windZ = oceanCurrentNoise.noise((float)((x + 1000) * oceanCurrentScale), (float)(z * oceanCurrentScale));
        climate.windVector = new Vector2f(windX * 10.0f, windZ * 10.0f);
        
        return climate;
    }
    
    // Configuration methods
    public void setTemperatureScale(double scale) {
        this.temperatureScale = scale;
        logger.debug("Temperature scale set to {}", scale);
    }
    
    public void setHumidityScale(double scale) {
        this.humidityScale = scale;
        logger.debug("Humidity scale set to {}", scale);
    }
    
    public void setElevationScale(double scale) {
        this.elevationScale = scale;
        logger.debug("Elevation scale set to {}", scale);
    }
    
    public void setBiomeBlendRadius(int radius) {
        this.biomeBlendRadius = Math.max(1, Math.min(32, radius));
        logger.debug("Biome blend radius set to {}", this.biomeBlendRadius);
    }
    
    // Helper methods for missing biomes
    private BiomeType createColdOceanBiome() {
        return new BiomeType.Builder("cold_ocean")
            .temperature(0.2f)
            .humidity(1.0f)
            .elevation(-0.3f)
            .color(0x2E5984)
            .precipitation(BiomeType.Precipitation.SNOW)
            .category(BiomeType.Category.OCEAN)
            .addFeature("ice_formations", 0.4f)
            .addFeature("cold_water_fish", 0.7f)
            .addMobSpawn("cod", 12, 4, 8)
            .addMobSpawn("polar_bear", 1, 1, 2)
            .setDepthRange(15, 45)
            .setWaveIntensity(0.7f)
            .setCurrentStrength(0.5f)
            .build();
    }
    
    private BiomeType createFrozenOceanBiome() {
        return new BiomeType.Builder("frozen_ocean")
            .temperature(-0.2f)
            .humidity(1.0f)
            .elevation(-0.4f)
            .color(0x7FB8DA)
            .precipitation(BiomeType.Precipitation.SNOW)
            .category(BiomeType.Category.OCEAN)
            .addFeature("ice_sheets", 0.8f)
            .addFeature("icebergs", 0.6f)
            .addMobSpawn("polar_bear", 3, 1, 2)
            .addMobSpawn("seal", 5, 2, 4)
            .setDepthRange(20, 50)
            .setWaveIntensity(0.3f)
            .setCurrentStrength(0.2f)
            .build();
    }
    
    private BiomeType createBeachBiome() {
        return new BiomeType.Builder("beach")
            .temperature(0.7f)
            .humidity(0.4f)
            .elevation(0.45f)
            .color(0xFADE55)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.COASTAL)
            .addFeature("sand_dunes", 0.6f)
            .addFeature("seashells", 0.8f)
            .addMobSpawn("crab", 8, 2, 5)
            .addMobSpawn("seagull", 6, 1, 3)
            .setTidalRange(1.0f)
            .build();
    }
    
    private BiomeType createRockyCoastBiome() {
        return new BiomeType.Builder("rocky_coast")
            .temperature(0.5f)
            .humidity(0.6f)
            .elevation(0.48f)
            .color(0x696969)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.COASTAL)
            .addFeature("rock_formations", 0.9f)
            .addFeature("tide_pools", 0.7f)
            .addMobSpawn("crab", 10, 3, 6)
            .addMobSpawn("seagull", 8, 2, 4)
            .setTidalRange(1.2f)
            .setErosionRate(0.2f)
            .build();
    }
    
    private BiomeType createIcebergBiome() {
        return new BiomeType.Builder("iceberg")
            .temperature(-0.1f)
            .humidity(0.8f)
            .elevation(0.46f)
            .color(0xB0E0E6)
            .precipitation(BiomeType.Precipitation.SNOW)
            .category(BiomeType.Category.COASTAL)
            .addFeature("ice_formations", 0.95f)
            .addFeature("frozen_caves", 0.3f)
            .addMobSpawn("polar_bear", 2, 1, 2)
            .addMobSpawn("seal", 4, 1, 3)
            .setTidalRange(0.5f)
            .build();
    }
    
    private BiomeType createDesertIslandBiome() {
        return new BiomeType.Builder("desert_island")
            .temperature(1.0f)
            .humidity(0.1f)
            .elevation(0.4f)
            .color(0xDEB887)
            .precipitation(BiomeType.Precipitation.NONE)
            .category(BiomeType.Category.ISLAND)
            .addFeature("cacti", 0.6f)
            .addFeature("sand_dunes", 0.8f)
            .addMobSpawn("lizard", 5, 1, 3)
            .addMobSpawn("scorpion", 2, 1, 2)
            .setFertility(0.1f)
            .build();
    }
    
    private BiomeType createTundraIslandBiome() {
        return new BiomeType.Builder("tundra_island")
            .temperature(0.1f)
            .humidity(0.3f)
            .elevation(0.5f)
            .color(0x8FBC8F)
            .precipitation(BiomeType.Precipitation.SNOW)
            .category(BiomeType.Category.ISLAND)
            .addFeature("permafrost", 0.9f)
            .addFeature("moss", 0.5f)
            .addMobSpawn("arctic_fox", 3, 1, 2)
            .addMobSpawn("caribou", 2, 1, 3)
            .setFertility(0.2f)
            .build();
    }
    
    private BiomeType createJungleIslandBiome() {
        return new BiomeType.Builder("jungle_island")
            .temperature(0.9f)
            .humidity(0.9f)
            .elevation(0.6f)
            .color(0x228B22)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.ISLAND)
            .addFeature("dense_vegetation", 0.95f)
            .addFeature("vines", 0.8f)
            .addMobSpawn("parrot", 12, 2, 6)
            .addMobSpawn("jaguar", 2, 1, 1)
            .setFertility(0.95f)
            .setBiodiversity(0.9f)
            .build();
    }
    
    private BiomeType createKelpForestBiome() {
        return new BiomeType.Builder("kelp_forest")
            .temperature(0.4f)
            .humidity(1.0f)
            .elevation(-0.25f)
            .color(0x2E8B57)
            .precipitation(BiomeType.Precipitation.RAIN)
            .category(BiomeType.Category.SPECIAL)
            .addFeature("giant_kelp", 0.9f)
            .addFeature("sea_otters", 0.4f)
            .addMobSpawn("sea_otter", 6, 2, 4)
            .addMobSpawn("kelp_fish", 15, 5, 10)
            .setDepthRange(8, 30)
            .setBiodiversity(0.8f)
            .build();
    }
    
    private BiomeType createAbyssalPlainBiome() {
        return new BiomeType.Builder("abyssal_plain")
            .temperature(0.1f)
            .humidity(1.0f)
            .elevation(-0.9f)
            .color(0x000080)
            .precipitation(BiomeType.Precipitation.NONE)
            .category(BiomeType.Category.SPECIAL)
            .addFeature("deep_sea_creatures", 0.3f)
            .addFeature("bioluminescence", 0.5f)
            .addMobSpawn("anglerfish", 3, 1, 2)
            .addMobSpawn("deep_sea_jellyfish", 5, 2, 4)
            .setDepthRange(80, 150)
            .setPressure(5.0f)
            .build();
    }
    
    private BiomeType createHydrothermalVentBiome() {
        return new BiomeType.Builder("hydrothermal_vent")
            .temperature(2.0f)
            .humidity(1.0f)
            .elevation(-0.7f)
            .color(0xFF4500)
            .precipitation(BiomeType.Precipitation.NONE)
            .category(BiomeType.Category.SPECIAL)
            .addFeature("thermal_vents", 0.8f)
            .addFeature("mineral_deposits", 0.9f)
            .addMobSpawn("tube_worm", 10, 5, 15)
            .addMobSpawn("vent_crab", 8, 3, 6)
            .setDepthRange(60, 120)
            .setGeothermalActivity(1.0f)
            .build();
    }
}