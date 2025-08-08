package com.odyssey.world.generation;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Advanced procedural world generation system for The Odyssey.
 * Implements sophisticated island generation, terrain features, and geological systems.
 */
public class WorldGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WorldGenerator.class);
    
    private final long seed;
    private final Random random;
    
    // Noise generators for different terrain features
    private final NoiseGenerator continentNoise;
    private final NoiseGenerator islandNoise;
    private final NoiseGenerator mountainNoise;
    private final NoiseGenerator caveNoise;
    private final NoiseGenerator temperatureNoise;
    private final NoiseGenerator humidityNoise;
    private final AdvancedCaveGenerator caveGenerator;
    
    // Generation parameters
    private static final int SEA_LEVEL = 64;
    private static final float CONTINENT_SCALE = 0.0005f;
    private static final float ISLAND_SCALE = 0.003f;
    private static final float MOUNTAIN_SCALE = 0.01f;
    
    // Geological features
    private final List<Island> generatedIslands = new ArrayList<>();
    private final List<VolcanicIsland> volcanicIslands = new ArrayList<>();
    private final List<UndergroundCave> caveSystems = new ArrayList<>();
    private final Map<Vector2f, GeologicalFeature> geologicalFeatures = new HashMap<>();
    
    // Ore deposits and resources
    private final Map<Vector3f, OreDeposit> oreDeposits = new HashMap<>();
    private final List<TreasureLocation> treasureLocations = new ArrayList<>();
    
    public enum BlockType {
        AIR(0, 0, 0), 
        WATER(1, 2, 0), 
        SAND(2, 15, 0), 
        STONE(3, 15, 0), 
        DIRT(4, 15, 0), 
        GRASS(5, 15, 0),
        WOOD(6, 15, 0), 
        LEAVES(7, 5, 0), 
        COAL_ORE(8, 15, 0), 
        IRON_ORE(9, 15, 0), 
        GOLD_ORE(10, 15, 0),
        OBSIDIAN(11, 15, 0), 
        LAVA(12, 5, 15), 
        CORAL(13, 10, 2), 
        SEAWEED(14, 2, 0), 
        PALM_WOOD(15, 15, 0),
        VOLCANIC_ROCK(16, 15, 0), 
        LIMESTONE(17, 15, 0), 
        MARBLE(18, 15, 0), 
        CRYSTAL(19, 5, 10);
        
        public final int id;
        public final int opacity; // 0 = transparent, 15 = opaque
        public final int emission; // 0 = no light, 15 = max light
        
        BlockType(int id, int opacity, int emission) {
            this.id = id;
            this.opacity = opacity;
            this.emission = emission;
        }
        
        /**
         * Returns true if this block type is transparent to light.
         */
        public boolean isTransparent() {
            return opacity == 0;
        }
        
        /**
         * Returns true if this block type emits light.
         */
        public boolean isEmissive() {
            return emission > 0;
        }
    }
    
    public static class Island {
        public Vector2f center;
        public float radius;
        public float height;
        public IslandType type;
        public List<Vector2f> beaches;
        public List<Vector2f> harbors;
        public Map<String, Integer> resources;
        
        public Island(Vector2f center, float radius, float height, IslandType type) {
            this.center = new Vector2f(center);
            this.radius = radius;
            this.height = height;
            this.type = type;
            this.beaches = new ArrayList<>();
            this.harbors = new ArrayList<>();
            this.resources = new HashMap<>();
        }
    }
    
    public enum IslandType {
        TROPICAL_ATOLL, VOLCANIC_SPIRE, DENSE_JUNGLE, MANGROVE_SWAMP,
        CURSED_ISLE, ARCTIC_ARCHIPELAGO, DESERT_OASIS, CORAL_REEF,
        FLOATING_GARDEN, BONE_ISLAND, STORM_TOUCHED, MAGNETIC_ANOMALY
    }
    
    public static class VolcanicIsland extends Island {
        public Vector2f craterCenter;
        public float craterRadius;
        public boolean isActive;
        public double lastEruption;
        public float lavaLevel;
        
        public VolcanicIsland(Vector2f center, float radius, float height) {
            super(center, radius, height, IslandType.VOLCANIC_SPIRE);
            this.craterCenter = new Vector2f(center).add(
                (float)(Math.random() - 0.5) * radius * 0.3f,
                (float)(Math.random() - 0.5) * radius * 0.3f
            );
            this.craterRadius = radius * 0.15f;
            this.isActive = Math.random() < 0.3; // 30% chance of active volcano
            this.lastEruption = 0.0;
            this.lavaLevel = isActive ? height * 0.8f : 0;
        }
    }
    
    public static class UndergroundCave {
        public Vector3f entrance;
        public List<Vector3f> chambers;
        public Map<Vector3f, String> treasures;
        public float depth;
        public boolean isFlooded;
        
        public UndergroundCave(Vector3f entrance) {
            this.entrance = new Vector3f(entrance);
            this.chambers = new ArrayList<>();
            this.treasures = new HashMap<>();
            this.depth = 20 + (float)Math.random() * 50; // 20-70 blocks deep
            this.isFlooded = entrance.y < SEA_LEVEL + 10;
            
            generateChambers();
        }
        
        private void generateChambers() {
            int chamberCount = 3 + (int)(Math.random() * 5); // 3-8 chambers
            Vector3f currentPos = new Vector3f(entrance);
            
            for (int i = 0; i < chamberCount; i++) {
                chambers.add(new Vector3f(currentPos));
                
                // Move to next chamber position
                currentPos.add(
                    (float)(Math.random() - 0.5) * 30,
                    -(float)Math.random() * 15 - 5, // Go deeper
                    (float)(Math.random() - 0.5) * 30
                );
                
                // Occasionally place treasure
                if (Math.random() < 0.4) {
                    String[] treasureTypes = {"ancient_artifact", "gold_coins", "rare_gems", "magical_crystal"};
                    String treasure = treasureTypes[(int)(Math.random() * treasureTypes.length)];
                    treasures.put(new Vector3f(currentPos), treasure);
                }
            }
        }
    }
    
    public static class GeologicalFeature {
        public Vector2f position;
        public FeatureType type;
        public float intensity;
        public Map<String, Object> properties;
        
        public GeologicalFeature(Vector2f position, FeatureType type, float intensity) {
            this.position = new Vector2f(position);
            this.type = type;
            this.intensity = intensity;
            this.properties = new HashMap<>();
        }
    }
    
    public enum FeatureType {
        HOT_SPRING, UNDERWATER_VENT, TECTONIC_FAULT, CORAL_FORMATION,
        MINERAL_VEIN, ANCIENT_RUINS, SHIPWRECK, WHIRLPOOL
    }
    
    public static class OreDeposit {
        public Vector3f position;
        public BlockType oreType;
        public int quantity;
        public float richness;
        public boolean isExposed;
        
        public OreDeposit(Vector3f position, BlockType oreType, int quantity, float richness) {
            this.position = new Vector3f(position);
            this.oreType = oreType;
            this.quantity = quantity;
            this.richness = richness;
            this.isExposed = Math.random() < 0.2; // 20% chance of surface exposure
        }
    }
    
    public static class TreasureLocation {
        public Vector3f position;
        public TreasureType type;
        public boolean isHidden;
        public List<String> treasureItems;
        public String mapFragment;
        
        public TreasureLocation(Vector3f position, TreasureType type) {
            this.position = new Vector3f(position);
            this.type = type;
            this.isHidden = type != TreasureType.VISIBLE_WRECK;
            this.treasureItems = new ArrayList<>();
            this.mapFragment = generateMapFragment();
            
            generateTreasureContents();
        }
        
        private void generateTreasureContents() {
            String[] possibleTreasures = {
                "gold_coins", "silver_coins", "precious_gems", "ancient_artifact",
                "magical_scroll", "rare_weapon", "navigation_instrument", "exotic_spices"
            };
            
            int itemCount = 1 + (int)(Math.random() * 4); // 1-5 items
            for (int i = 0; i < itemCount; i++) {
                treasureItems.add(possibleTreasures[(int)(Math.random() * possibleTreasures.length)]);
            }
        }
        
        private String generateMapFragment() {
            return "Fragment_" + (int)(Math.random() * 1000);
        }
    }
    
    public enum TreasureType {
        BURIED_CHEST, SUNKEN_SHIP, CAVE_HOARD, VISIBLE_WRECK, ANCIENT_VAULT
    }
    
    // Advanced noise generator with Simplex and Perlin noise support, 3D capabilities, and multiple noise types
    public static class NoiseGenerator {
        private final long seed;
        private final Random random;
        
        // Permutation table for noise generation
        private final int[] permutation = new int[512];
        
        // Gradient vectors for 3D noise
        private static final Vector3f[] gradients3D = {
            new Vector3f(1, 1, 0), new Vector3f(-1, 1, 0), new Vector3f(1, -1, 0), new Vector3f(-1, -1, 0),
            new Vector3f(1, 0, 1), new Vector3f(-1, 0, 1), new Vector3f(1, 0, -1), new Vector3f(-1, 0, -1),
            new Vector3f(0, 1, 1), new Vector3f(0, -1, 1), new Vector3f(0, 1, -1), new Vector3f(0, -1, -1)
        };
        
        public NoiseGenerator(long seed) {
            this.seed = seed;
            this.random = new Random(seed);
            initializePermutationTable();
        }
        
        private void initializePermutationTable() {
            // Initialize permutation table with values 0-255
            for (int i = 0; i < 256; i++) {
                permutation[i] = i;
            }
            
            // Shuffle the permutation table using the seed
            Random shuffleRandom = new Random(seed);
            for (int i = 255; i > 0; i--) {
                int j = shuffleRandom.nextInt(i + 1);
                int temp = permutation[i];
                permutation[i] = permutation[j];
                permutation[j] = temp;
            }
            
            // Duplicate the permutation table to avoid overflow
            for (int i = 0; i < 256; i++) {
                permutation[256 + i] = permutation[i];
            }
        }
        
        public float noise(float x, float y) {
            // Find unit grid cell containing point
            int X = (int)Math.floor(x) & 255;
            int Y = (int)Math.floor(y) & 255;
            
            // Get relative xy coordinates of point within that cell
            x -= Math.floor(x);
            y -= Math.floor(y);
            
            // Compute fade curves for each of x, y
            float u = fade(x);
            float v = fade(y);
            
            // Hash coordinates of the 4 cube corners
            int A = permutation[X] + Y;
            int AA = permutation[A];
            int AB = permutation[A + 1];
            int B = permutation[X + 1] + Y;
            int BA = permutation[B];
            int BB = permutation[B + 1];
            
            // Add blended results from 4 corners of cube
            return lerp(v, lerp(u, grad2D(permutation[AA], x, y),
                                   grad2D(permutation[BA], x - 1, y)),
                           lerp(u, grad2D(permutation[AB], x, y - 1),
                                   grad2D(permutation[BB], x - 1, y - 1)));
        }
        
        public float noise3D(float x, float y, float z) {
            // Find unit grid cell containing point
            int X = (int)Math.floor(x) & 255;
            int Y = (int)Math.floor(y) & 255;
            int Z = (int)Math.floor(z) & 255;
            
            // Get relative xyz coordinates of point within that cell
            x -= Math.floor(x);
            y -= Math.floor(y);
            z -= Math.floor(z);
            
            // Compute fade curves for each of x, y, z
            float u = fade(x);
            float v = fade(y);
            float w = fade(z);
            
            // Hash coordinates of the 8 cube corners
            int A = permutation[X] + Y;
            int AA = permutation[A] + Z;
            int AB = permutation[A + 1] + Z;
            int B = permutation[X + 1] + Y;
            int BA = permutation[B] + Z;
            int BB = permutation[B + 1] + Z;
            
            // Add blended results from 8 corners of cube
            return lerp(w, lerp(v, lerp(u, grad3D(permutation[AA], x, y, z),
                                           grad3D(permutation[BA], x - 1, y, z)),
                                   lerp(u, grad3D(permutation[AB], x, y - 1, z),
                                           grad3D(permutation[BB], x - 1, y - 1, z))),
                           lerp(v, lerp(u, grad3D(permutation[AA + 1], x, y, z - 1),
                                           grad3D(permutation[BA + 1], x - 1, y, z - 1)),
                                   lerp(u, grad3D(permutation[AB + 1], x, y - 1, z - 1),
                                           grad3D(permutation[BB + 1], x - 1, y - 1, z - 1))));
        }
        
        private float fade(float t) {
            // Fade function as defined by Ken Perlin: 6t^5 - 15t^4 + 10t^3
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
        
        private float lerp(float t, float a, float b) {
            return a + t * (b - a);
        }
        
        private float grad2D(int hash, float x, float y) {
            // Convert low 4 bits of hash code into 12 gradient directions
            int h = hash & 15;
            float u = h < 8 ? x : y;
            float v = h < 4 ? y : h == 12 || h == 14 ? x : 0;
            return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
        }
        
        private float grad3D(int hash, float x, float y, float z) {
            // Use gradient vectors for 3D noise
            Vector3f grad = gradients3D[hash & 11];
            return grad.x * x + grad.y * y + grad.z * z;
        }
        
        public float octaveNoise(float x, float y, int octaves, float persistence) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                total += noise(x * frequency, y * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= persistence;
                frequency *= 2;
            }
            
            return total / maxValue;
        }
        
        public float octaveNoise3D(float x, float y, float z, int octaves, float persistence) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                total += noise3D(x * frequency, y * frequency, z * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= persistence;
                frequency *= 2;
            }
            
            return total / maxValue;
        }
        
        /**
         * Fractal Brownian Motion - creates natural-looking terrain
         */
        public float fbm(float x, float y, int octaves, float lacunarity, float gain) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                total += noise(x * frequency, y * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= gain;
                frequency *= lacunarity;
            }
            
            return total / maxValue;
        }
        
        /**
         * Ridged noise - creates mountain ridges and valleys
         */
        public float ridgedNoise(float x, float y, int octaves, float lacunarity, float gain) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                float n = Math.abs(noise(x * frequency, y * frequency));
                n = 1.0f - n; // Invert for ridges
                n = n * n; // Square for sharper ridges
                total += n * amplitude;
                maxValue += amplitude;
                amplitude *= gain;
                frequency *= lacunarity;
            }
            
            return total / maxValue;
        }
        
        /**
         * Turbulence - creates chaotic, swirling patterns
         */
        public float turbulence(float x, float y, int octaves) {
            float total = 0;
            float frequency = 1;
            
            for (int i = 0; i < octaves; i++) {
                total += Math.abs(noise(x * frequency, y * frequency)) / frequency;
                frequency *= 2;
            }
            
            return total;
        }
        
        // Simplex noise constants
        private static final float F2 = 0.5f * ((float)Math.sqrt(3.0) - 1.0f);
        private static final float G2 = (3.0f - (float)Math.sqrt(3.0)) / 6.0f;
        private static final float F3 = 1.0f / 3.0f;
        private static final float G3 = 1.0f / 6.0f;
        
        // Simplex noise gradient vectors
        private static final int[][] grad3 = {
            {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
            {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
            {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}
        };
        
        /**
         * 2D Simplex noise - improved performance and visual quality over Perlin
         */
        public float simplex2D(float xin, float yin) {
            float n0, n1, n2; // Noise contributions from the three corners
            
            // Skew the input space to determine which simplex cell we're in
            float s = (xin + yin) * F2; // Hairy factor for 2D
            int i = (int)Math.floor(xin + s);
            int j = (int)Math.floor(yin + s);
            float t = (i + j) * G2;
            float X0 = i - t; // Unskew the cell origin back to (x,y) space
            float Y0 = j - t;
            float x0 = xin - X0; // The x,y distances from the cell origin
            float y0 = yin - Y0;
            
            // For the 2D case, the simplex shape is an equilateral triangle.
            // Determine which simplex we are in.
            int i1, j1; // Offsets for second (middle) corner of simplex in (i,j) coords
            if (x0 > y0) {
                i1 = 1; j1 = 0; // lower triangle, XY order: (0,0)->(1,0)->(1,1)
            } else {
                i1 = 0; j1 = 1; // upper triangle, YX order: (0,0)->(0,1)->(1,1)
            }
            
            // A step of (1,0) in (i,j) means a step of (1-c,-c) in (x,y), and
            // a step of (0,1) in (i,j) means a step of (-c,1-c) in (x,y), where
            // c = (3-sqrt(3))/6
            float x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
            float y1 = y0 - j1 + G2;
            float x2 = x0 - 1.0f + 2.0f * G2; // Offsets for last corner in (x,y) unskewed coords
            float y2 = y0 - 1.0f + 2.0f * G2;
            
            // Work out the hashed gradient indices of the three simplex corners
            int ii = i & 255;
            int jj = j & 255;
            int gi0 = permutation[ii + permutation[jj]] % 12;
            int gi1 = permutation[ii + i1 + permutation[jj + j1]] % 12;
            int gi2 = permutation[ii + 1 + permutation[jj + 1]] % 12;
            
            // Calculate the contribution from the three corners
            float t0 = 0.5f - x0 * x0 - y0 * y0;
            if (t0 < 0) {
                n0 = 0.0f;
            } else {
                t0 *= t0;
                n0 = t0 * t0 * dot(grad3[gi0], x0, y0); // (x,y) of grad3 used for 2D gradient
            }
            
            float t1 = 0.5f - x1 * x1 - y1 * y1;
            if (t1 < 0) {
                n1 = 0.0f;
            } else {
                t1 *= t1;
                n1 = t1 * t1 * dot(grad3[gi1], x1, y1);
            }
            
            float t2 = 0.5f - x2 * x2 - y2 * y2;
            if (t2 < 0) {
                n2 = 0.0f;
            } else {
                t2 *= t2;
                n2 = t2 * t2 * dot(grad3[gi2], x2, y2);
            }
            
            // Add contributions from each corner to get the final noise value.
            // The result is scaled to return values in the interval [-1,1].
            return 70.0f * (n0 + n1 + n2);
        }
        
        /**
         * 3D Simplex noise
         */
        public float simplex3D(float xin, float yin, float zin) {
            float n0, n1, n2, n3; // Noise contributions from the four corners
            
            // Skew the input space to determine which simplex cell we're in
            float s = (xin + yin + zin) * F3; // Very nice and simple skew factor for 3D
            int i = (int)Math.floor(xin + s);
            int j = (int)Math.floor(yin + s);
            int k = (int)Math.floor(zin + s);
            float t = (i + j + k) * G3;
            float X0 = i - t; // Unskew the cell origin back to (x,y,z) space
            float Y0 = j - t;
            float Z0 = k - t;
            float x0 = xin - X0; // The x,y,z distances from the cell origin
            float y0 = yin - Y0;
            float z0 = zin - Z0;
            
            // For the 3D case, the simplex shape is a slightly irregular tetrahedron.
            // Determine which simplex we are in.
            int i1, j1, k1; // Offsets for second corner of simplex in (i,j,k) coords
            int i2, j2, k2; // Offsets for third corner of simplex in (i,j,k) coords
            
            if (x0 >= y0) {
                if (y0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; // X Y Z order
                } else if (x0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; // X Z Y order
                } else {
                    i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; // Z X Y order
                }
            } else { // x0 < y0
                if (y0 < z0) {
                    i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; // Z Y X order
                } else if (x0 < z0) {
                    i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; // Y Z X order
                } else {
                    i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; // Y X Z order
                }
            }
            
            // A step of (1,0,0) in (i,j,k) means a step of (1-c,-c,-c) in (x,y,z),
            // a step of (0,1,0) in (i,j,k) means a step of (-c,1-c,-c) in (x,y,z), and
            // a step of (0,0,1) in (i,j,k) means a step of (-c,-c,1-c) in (x,y,z), where
            // c = 1/6.
            float x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
            float y1 = y0 - j1 + G3;
            float z1 = z0 - k1 + G3;
            float x2 = x0 - i2 + 2.0f * G3; // Offsets for third corner in (x,y,z) coords
            float y2 = y0 - j2 + 2.0f * G3;
            float z2 = z0 - k2 + 2.0f * G3;
            float x3 = x0 - 1.0f + 3.0f * G3; // Offsets for last corner in (x,y,z) coords
            float y3 = y0 - 1.0f + 3.0f * G3;
            float z3 = z0 - 1.0f + 3.0f * G3;
            
            // Work out the hashed gradient indices of the four simplex corners
            int ii = i & 255;
            int jj = j & 255;
            int kk = k & 255;
            int gi0 = permutation[ii + permutation[jj + permutation[kk]]] % 12;
            int gi1 = permutation[ii + i1 + permutation[jj + j1 + permutation[kk + k1]]] % 12;
            int gi2 = permutation[ii + i2 + permutation[jj + j2 + permutation[kk + k2]]] % 12;
            int gi3 = permutation[ii + 1 + permutation[jj + 1 + permutation[kk + 1]]] % 12;
            
            // Calculate the contribution from the four corners
            float t0 = 0.6f - x0 * x0 - y0 * y0 - z0 * z0;
            if (t0 < 0) {
                n0 = 0.0f;
            } else {
                t0 *= t0;
                n0 = t0 * t0 * dot(grad3[gi0], x0, y0, z0);
            }
            
            float t1 = 0.6f - x1 * x1 - y1 * y1 - z1 * z1;
            if (t1 < 0) {
                n1 = 0.0f;
            } else {
                t1 *= t1;
                n1 = t1 * t1 * dot(grad3[gi1], x1, y1, z1);
            }
            
            float t2 = 0.6f - x2 * x2 - y2 * y2 - z2 * z2;
            if (t2 < 0) {
                n2 = 0.0f;
            } else {
                t2 *= t2;
                n2 = t2 * t2 * dot(grad3[gi2], x2, y2, z2);
            }
            
            float t3 = 0.6f - x3 * x3 - y3 * y3 - z3 * z3;
            if (t3 < 0) {
                n3 = 0.0f;
            } else {
                t3 *= t3;
                n3 = t3 * t3 * dot(grad3[gi3], x3, y3, z3);
            }
            
            // Add contributions from each corner to get the final noise value.
            // The result is scaled to return values in the interval [-1,1].
            return 32.0f * (n0 + n1 + n2 + n3);
        }
        
        /**
         * Simplex FBM - Fractal Brownian Motion using Simplex noise
         */
        public float simplexFbm(float x, float y, int octaves, float lacunarity, float gain) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                total += simplex2D(x * frequency, y * frequency) * amplitude;
                maxValue += amplitude;
                amplitude *= gain;
                frequency *= lacunarity;
            }
            
            return total / maxValue;
        }
        
        /**
         * Simplex ridged noise for mountain generation
         */
        public float simplexRidged(float x, float y, int octaves, float lacunarity, float gain) {
            float total = 0;
            float frequency = 1;
            float amplitude = 1;
            float maxValue = 0;
            
            for (int i = 0; i < octaves; i++) {
                float n = Math.abs(simplex2D(x * frequency, y * frequency));
                n = 1.0f - n; // Invert for ridges
                n = n * n; // Square for sharper ridges
                total += n * amplitude;
                maxValue += amplitude;
                amplitude *= gain;
                frequency *= lacunarity;
            }
            
            return total / maxValue;
        }
        
        // Helper method for dot product
        private float dot(int[] g, float x, float y) {
            return g[0] * x + g[1] * y;
        }
        
        private float dot(int[] g, float x, float y, float z) {
            return g[0] * x + g[1] * y + g[2] * z;
        }
        
        /**
         * Domain warping - creates more organic terrain shapes by distorting the input coordinates
         */
        public float domainWarp(float x, float y, float warpStrength) {
            // Generate offset values using noise
            float offsetX = simplex2D(x * 0.1f, y * 0.1f) * warpStrength;
            float offsetY = simplex2D(x * 0.1f + 100, y * 0.1f + 100) * warpStrength;
            
            // Apply the warped coordinates to generate the final noise
            return simplex2D(x + offsetX, y + offsetY);
        }
        
        /**
         * Advanced domain warping with multiple layers for complex organic shapes
         */
        public float advancedDomainWarp(float x, float y, float warpStrength, int layers) {
            float warpedX = x;
            float warpedY = y;
            
            // Apply multiple layers of warping
            for (int i = 0; i < layers; i++) {
                float scale = (float)Math.pow(2, i) * 0.1f;
                float strength = warpStrength / (float)Math.pow(2, i);
                
                float offsetX = simplex2D(warpedX * scale + i * 1000, warpedY * scale + i * 1000) * strength;
                float offsetY = simplex2D(warpedX * scale + i * 1000 + 500, warpedY * scale + i * 1000 + 500) * strength;
                
                warpedX += offsetX;
                warpedY += offsetY;
            }
            
            return simplex2D(warpedX, warpedY);
        }
        
        /**
         * Terrain twisting - creates swirling, twisted terrain patterns
         */
        public float terrainTwist(float x, float y, float twistStrength, float twistScale) {
            // Calculate distance from origin for radial effects
            float distance = (float)Math.sqrt(x * x + y * y);
            
            // Create a twisting angle based on distance and noise
            float twistAngle = distance * twistScale + simplex2D(x * 0.01f, y * 0.01f) * twistStrength;
            
            // Apply rotation transformation
            float cos = (float)Math.cos(twistAngle);
            float sin = (float)Math.sin(twistAngle);
            
            float twistedX = x * cos - y * sin;
            float twistedY = x * sin + y * cos;
            
            return simplex2D(twistedX, twistedY);
        }
        
        /**
         * Curl noise - generates vector field for realistic flow patterns
         */
        public Vector2f curlNoise(float x, float y, float epsilon) {
            // Calculate partial derivatives for curl
            float n1 = simplex2D(x, y + epsilon);
            float n2 = simplex2D(x, y - epsilon);
            float n3 = simplex2D(x + epsilon, y);
            float n4 = simplex2D(x - epsilon, y);
            
            // Compute curl (rotation) of the noise field
            float curlX = (n1 - n2) / (2.0f * epsilon);
            float curlY = (n4 - n3) / (2.0f * epsilon);
            
            return new Vector2f(curlX, curlY);
        }
        
        /**
         * Flow field warping using curl noise for natural erosion patterns
         */
        public float flowFieldWarp(float x, float y, float flowStrength, int iterations) {
            float currentX = x;
            float currentY = y;
            
            // Follow the flow field for multiple iterations
            for (int i = 0; i < iterations; i++) {
                Vector2f curl = curlNoise(currentX * 0.01f, currentY * 0.01f, 0.01f);
                currentX += curl.x * flowStrength;
                currentY += curl.y * flowStrength;
            }
            
            return simplex2D(currentX, currentY);
        }
    }
    
    // Advanced Cave Generation System
    public static class AdvancedCaveGenerator {
        private final NoiseGenerator noise;
        private final Random random;
        private final long seed;
        
        public AdvancedCaveGenerator(long seed) {
            this.seed = seed;
            this.noise = new NoiseGenerator(seed);
            this.random = new Random(seed);
        }
        
        /**
         * Perlin Worms algorithm for realistic cave tunnel networks
         */
        public boolean isPerlinWormCave(float x, float y, float z, float terrainHeight) {
            // Generate worm paths using 3D noise
            int numWorms = 5;
            
            for (int worm = 0; worm < numWorms; worm++) {
                // Each worm has its own seed offset
                float wormSeed = worm * 1000.0f;
                
                // Worm path using noise
                float wormX = noise.simplex2D((y + wormSeed) * 0.01f, (z + wormSeed) * 0.01f) * 200.0f;
                float wormZ = noise.simplex2D((y + wormSeed + 500) * 0.01f, (x + wormSeed + 500) * 0.01f) * 200.0f;
                
                // Distance to worm path
                float distanceToWorm = (float)Math.sqrt((x - wormX) * (x - wormX) + (z - wormZ) * (z - wormZ));
                
                // Worm radius varies with depth and noise
                float depthFactor = Math.max(0, (terrainHeight - y) / 100.0f);
                float radiusNoise = noise.simplex3D(x * 0.05f, y * 0.05f, z * 0.05f);
                float wormRadius = 3.0f + depthFactor * 2.0f + radiusNoise * 1.5f;
                
                if (distanceToWorm < wormRadius) {
                    return true;
                }
            }
            
            return false;
        }
        
        /**
         * 3D Cellular Automata for natural cavern chamber generation
         */
        public boolean isCellularAutomataCave(float x, float y, float z, int iterations) {
            // Initial noise-based seed
            float initialNoise = noise.simplex3D(x * 0.1f, y * 0.1f, z * 0.1f);
            boolean isCave = initialNoise > 0.45f;
            
            // Apply cellular automata rules
            for (int iter = 0; iter < iterations; iter++) {
                int neighbors = 0;
                
                // Check 3x3x3 neighborhood
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            
                            float neighborNoise = noise.simplex3D((x + dx) * 0.1f, (y + dy) * 0.1f, (z + dz) * 0.1f);
                            if (neighborNoise > 0.45f) neighbors++;
                        }
                    }
                }
                
                // Cellular automata rules: survive if 4+ neighbors, born if 5+ neighbors
                isCave = neighbors >= 4;
            }
            
            return isCave;
        }
        
        /**
         * Voronoi-based cave room generation with connecting passages
         */
        public boolean isVoronoiCave(float x, float y, float z, float terrainHeight) {
            // Generate Voronoi cell centers
            int numCells = 8;
            float minDistance = Float.MAX_VALUE;
            float secondMinDistance = Float.MAX_VALUE;
            
            for (int i = 0; i < numCells; i++) {
                // Deterministic cell centers based on position
                float cellX = (float)(Math.floor(x / 50.0f) * 50.0f + (i * 17) % 50);
                float cellY = (float)(Math.floor(y / 30.0f) * 30.0f + (i * 13) % 30);
                float cellZ = (float)(Math.floor(z / 50.0f) * 50.0f + (i * 19) % 50);
                
                float distance = (float)Math.sqrt(
                    (x - cellX) * (x - cellX) + 
                    (y - cellY) * (y - cellY) + 
                    (z - cellZ) * (z - cellZ)
                );
                
                if (distance < minDistance) {
                    secondMinDistance = minDistance;
                    minDistance = distance;
                } else if (distance < secondMinDistance) {
                    secondMinDistance = distance;
                }
            }
            
            // Create caves near cell centers and along cell boundaries
            float roomRadius = 8.0f + noise.simplex3D(x * 0.02f, y * 0.02f, z * 0.02f) * 3.0f;
            float passageWidth = 2.0f;
            
            return minDistance < roomRadius || (secondMinDistance - minDistance) < passageWidth;
        }
        
        /**
         * Underwater lava tube generation using flow simulation
         */
        public boolean isLavaTube(float x, float y, float z, float terrainHeight) {
            // Only generate below sea level
            if (y > SEA_LEVEL - 10) return false;
            
            // Simulate lava flow paths
            float flowDirection = noise.simplex2D(x * 0.005f, z * 0.005f) * (float)Math.PI * 2;
            float flowX = (float)Math.cos(flowDirection);
            float flowZ = (float)Math.sin(flowDirection);
            
            // Distance to flow path
            float pathX = x + flowX * (y - (SEA_LEVEL - 50)) * 0.5f;
            float pathZ = z + flowZ * (y - (SEA_LEVEL - 50)) * 0.5f;
            
            float distanceToPath = (float)Math.sqrt((x - pathX) * (x - pathX) + (z - pathZ) * (z - pathZ));
            
            // Tube radius varies with depth
            float tubeRadius = 4.0f + (SEA_LEVEL - y) * 0.1f;
            tubeRadius += noise.simplex3D(x * 0.03f, y * 0.03f, z * 0.03f) * 2.0f;
            
            return distanceToPath < tubeRadius;
        }
        
        /**
         * Karst cave system generation with realistic limestone dissolution patterns
         */
        public boolean isKarstCave(float x, float y, float z, float terrainHeight) {
            // Karst caves form in limestone through water dissolution
            float waterFlow = noise.curlNoise(x * 0.01f, z * 0.01f, 0.1f).length();
            float dissolutionRate = noise.simplex3D(x * 0.02f, y * 0.02f, z * 0.02f);
            
            // Higher dissolution near water flow paths
            float karstFactor = waterFlow * dissolutionRate;
            
            // Create cave if dissolution is high enough
            float karstThreshold = 0.6f - Math.max(0, (terrainHeight - y) / 200.0f) * 0.2f;
            
            return karstFactor > karstThreshold;
        }
        
        /**
         * Coral labyrinth generation using L-systems and growth algorithms
         */
        public boolean isCoralLabyrinth(float x, float y, float z, float terrainHeight) {
            // Only in underwater areas
            if (y > SEA_LEVEL - 5) return false;
            
            // Simulate coral growth patterns
            float growthNoise = noise.simplex3D(x * 0.08f, y * 0.08f, z * 0.08f);
            float branchingNoise = noise.simplex3D(x * 0.15f + 1000, y * 0.15f + 1000, z * 0.15f + 1000);
            
            // Create branching coral structures
            float coralDensity = Math.abs(growthNoise) + Math.abs(branchingNoise) * 0.5f;
            
            // Coral creates passages, not solid blocks
            return coralDensity > 0.7f && coralDensity < 1.2f;
        }
        
        /**
         * Sea cave generation with tidal erosion patterns
         */
        public boolean isSeaCave(float x, float y, float z, float terrainHeight) {
            // Only near sea level
            if (Math.abs(y - SEA_LEVEL) > 10) return false;
            
            // Simulate wave erosion
            float waveDirection = noise.simplex2D(x * 0.002f, z * 0.002f) * (float)Math.PI * 2;
            float waveX = (float)Math.cos(waveDirection);
            float waveZ = (float)Math.sin(waveDirection);
            
            // Erosion strength based on wave exposure
            float exposure = Math.abs(x * waveX + z * waveZ);
            float erosionNoise = noise.simplex3D(x * 0.05f, y * 0.05f, z * 0.05f);
            
            float erosionStrength = exposure * 0.01f + erosionNoise * 0.5f;
            
            return erosionStrength > 0.6f;
        }
        
        /**
         * Air pocket placement system for underwater cave breathing spaces
         */
        public boolean hasAirPocket(float x, float y, float z, float terrainHeight) {
            // Only in underwater caves
            if (y > SEA_LEVEL) return false;
            
            // Air pockets form at cave ceilings
            float ceilingNoise = noise.simplex3D(x * 0.1f, y * 0.1f, z * 0.1f);
            float pocketNoise = noise.simplex2D(x * 0.05f, z * 0.05f);
            
            // Check if we're near a cave ceiling
            boolean nearCeiling = false;
            for (int dy = 1; dy <= 3; dy++) {
                float aboveNoise = noise.simplex3D(x * 0.02f, (y + dy) * 0.02f, z * 0.02f);
                if (Math.abs(aboveNoise) < 0.4f) { // Solid block above
                    nearCeiling = true;
                    break;
                }
            }
            
            return nearCeiling && ceilingNoise > 0.7f && pocketNoise > 0.5f;
        }
        
        /**
         * Cave river and underground waterway generation
         */
        public boolean isUndergroundRiver(float x, float y, float z, float terrainHeight) {
            // Rivers flow downhill through caves
            float riverNoise = noise.simplex2D(x * 0.01f, z * 0.01f);
            float flowNoise = noise.simplex2D(x * 0.005f + 500, z * 0.005f + 500);
            
            // Create meandering river paths
            float riverPath = Math.abs(riverNoise + flowNoise * 0.3f);
            
            // Rivers are more likely at lower elevations
            float elevationFactor = Math.max(0, (terrainHeight - y) / 50.0f);
            
            return riverPath < 0.1f + elevationFactor * 0.05f;
        }
        
        /**
         * Master cave generation method that combines all algorithms
         */
        public boolean isCave(float x, float y, float z, float terrainHeight) {
            // Don't generate caves too close to surface
            if (y > terrainHeight - 3) return false;
            
            // Combine different cave generation algorithms
            boolean perlinWorm = isPerlinWormCave(x, y, z, terrainHeight);
            boolean cellular = isCellularAutomataCave(x, y, z, 2);
            boolean voronoi = isVoronoiCave(x, y, z, terrainHeight);
            boolean lavaTube = isLavaTube(x, y, z, terrainHeight);
            boolean karst = isKarstCave(x, y, z, terrainHeight);
            boolean coral = isCoralLabyrinth(x, y, z, terrainHeight);
            boolean seaCave = isSeaCave(x, y, z, terrainHeight);
            
            // Use different algorithms based on depth and location
            float depthFactor = (terrainHeight - y) / 100.0f;
            
            if (depthFactor < 0.2f) {
                // Shallow caves - sea caves and simple tunnels
                return seaCave || (perlinWorm && random.nextFloat() < 0.3f);
            } else if (depthFactor < 0.5f) {
                // Medium depth - Voronoi rooms and karst caves
                return voronoi || karst || (cellular && random.nextFloat() < 0.4f);
            } else {
                // Deep caves - complex systems
                return perlinWorm || cellular || lavaTube || (coral && y < SEA_LEVEL);
            }
        }
    }
    
    public WorldGenerator(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        
        // Initialize noise generators with different seeds
        this.continentNoise = new NoiseGenerator(seed);
        this.islandNoise = new NoiseGenerator(seed + 1);
        this.mountainNoise = new NoiseGenerator(seed + 2);
        this.caveNoise = new NoiseGenerator(seed + 3);
        this.temperatureNoise = new NoiseGenerator(seed + 4);
        this.humidityNoise = new NoiseGenerator(seed + 5);
        this.caveGenerator = new AdvancedCaveGenerator(seed + 6);
    }
    
    public void initialize() {
        logger.info("Initializing advanced world generator with seed: {}", seed);
        
        // Generate spawn island first to ensure player has land nearby
        generateSpawnIsland();
        
        // Pre-generate some major islands
        generateMajorIslands(20);
        
        // Generate volcanic islands
        generateVolcanicIslands(8);
        
        // Generate geological features
        generateGeologicalFeatures(50);
        
        // Generate ore deposits
        generateOreDeposits(200);
        
        // Generate treasure locations
        generateTreasureLocations(30);
        
        logger.info("World generator initialized with {} islands, {} volcanic islands, {} geological features", 
                   generatedIslands.size(), volcanicIslands.size(), geologicalFeatures.size());
    }
    
    /**
     * Generates a starter island near the spawn point to ensure players have land nearby.
     */
    private void generateSpawnIsland() {
        // Create a spawn island within 100-200 blocks of origin
        float angle = random.nextFloat() * 2 * (float)Math.PI;
        float distance = 100 + random.nextFloat() * 100; // 100-200 blocks from spawn
        
        Vector2f center = new Vector2f(
            (float)Math.cos(angle) * distance,
            (float)Math.sin(angle) * distance
        );
        
        // Make it a decent-sized tropical atoll for a good starting experience
        float radius = 80 + random.nextFloat() * 40; // 80-120 block radius
        float height = 25 + random.nextFloat() * 15; // 25-40 blocks above sea level
        
        Island spawnIsland = new Island(center, radius, height, IslandType.TROPICAL_ATOLL);
        
        // Generate island features
        generateIslandFeatures(spawnIsland);
        
        // Add extra resources for starting players
        spawnIsland.resources.put("coconuts", 100 + random.nextInt(50));
        spawnIsland.resources.put("palm_wood", 50 + random.nextInt(30));
        spawnIsland.resources.put("fresh_water", 1); // Ensure fresh water source
        spawnIsland.resources.put("basic_tools", 1); // Some basic starting tools
        
        generatedIslands.add(spawnIsland);
        
        logger.info("Generated spawn island at ({}, {}) with radius {} and height {}", 
                   center.x, center.y, radius, height);
    }

    private void generateMajorIslands(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (random.nextFloat() - 0.5f) * 4000, // Spread over 4km radius
                (random.nextFloat() - 0.5f) * 4000
            );
            
            float radius = 50 + random.nextFloat() * 200; // 50-250 block radius
            float height = 20 + random.nextFloat() * 60; // 20-80 blocks above sea level
            
            IslandType type = IslandType.values()[random.nextInt(IslandType.values().length)];
            Island island = new Island(center, radius, height, type);
            
            // Generate island features
            generateIslandFeatures(island);
            
            generatedIslands.add(island);
        }
    }
    
    private void generateIslandFeatures(Island island) {
        // Generate beaches around the perimeter
        int beachCount = 3 + random.nextInt(6); // 3-8 beaches
        for (int i = 0; i < beachCount; i++) {
            float angle = (float)i / beachCount * 2 * (float)Math.PI;
            float beachDistance = island.radius * (0.8f + random.nextFloat() * 0.2f);
            
            Vector2f beach = new Vector2f(
                island.center.x + (float)Math.cos(angle) * beachDistance,
                island.center.y + (float)Math.sin(angle) * beachDistance
            );
            island.beaches.add(beach);
            
            // Some beaches become natural harbors
            if (random.nextFloat() < 0.4) {
                island.harbors.add(beach);
            }
        }
        
        // Generate resources based on island type
        generateIslandResources(island);
    }
    
    private void generateIslandResources(Island island) {
        switch (island.type) {
            case TROPICAL_ATOLL -> {
                island.resources.put("coconuts", 50 + random.nextInt(100));
                island.resources.put("palm_wood", 20 + random.nextInt(50));
                island.resources.put("coral", 30 + random.nextInt(70));
                island.resources.put("fish", 40 + random.nextInt(80));
            }
            case VOLCANIC_SPIRE -> {
                island.resources.put("obsidian", 30 + random.nextInt(60));
                island.resources.put("sulfur", 20 + random.nextInt(40));
                island.resources.put("rare_gems", 5 + random.nextInt(15));
                island.resources.put("volcanic_glass", 10 + random.nextInt(25));
            }
            case DENSE_JUNGLE -> {
                island.resources.put("hardwood", 100 + random.nextInt(200));
                island.resources.put("exotic_fruits", 60 + random.nextInt(120));
                island.resources.put("medicinal_herbs", 30 + random.nextInt(60));
                island.resources.put("rare_animals", 10 + random.nextInt(20));
            }
            case ARCTIC_ARCHIPELAGO -> {
                island.resources.put("pine_wood", 80 + random.nextInt(160));
                island.resources.put("seal_blubber", 20 + random.nextInt(40));
                island.resources.put("ice_crystals", 15 + random.nextInt(30));
                island.resources.put("whale_bone", 5 + random.nextInt(15));
            }
        }
    }
    
    private void generateVolcanicIslands(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f center = new Vector2f(
                (random.nextFloat() - 0.5f) * 3000,
                (random.nextFloat() - 0.5f) * 3000
            );
            
            float radius = 80 + random.nextFloat() * 120; // Larger than normal islands
            float height = 60 + random.nextFloat() * 100; // Taller
            
            VolcanicIsland volcano = new VolcanicIsland(center, radius, height);
            volcanicIslands.add(volcano);
        }
    }
    
    private void generateGeologicalFeatures(int count) {
        for (int i = 0; i < count; i++) {
            Vector2f position = new Vector2f(
                (random.nextFloat() - 0.5f) * 5000,
                (random.nextFloat() - 0.5f) * 5000
            );
            
            FeatureType type = FeatureType.values()[random.nextInt(FeatureType.values().length)];
            float intensity = 0.3f + random.nextFloat() * 0.7f;
            
            GeologicalFeature feature = new GeologicalFeature(position, type, intensity);
            
            // Add type-specific properties
            switch (type) {
                case HOT_SPRING -> {
                    feature.properties.put("temperature", 60 + random.nextInt(40)); // 60-100°C
                    feature.properties.put("healing_properties", random.nextFloat() < 0.3);
                }
                case ANCIENT_RUINS -> {
                    feature.properties.put("age", 500 + random.nextInt(2000)); // 500-2500 years
                    feature.properties.put("civilization", "Ancient_" + random.nextInt(10));
                    feature.properties.put("treasure_chance", 0.6f + random.nextFloat() * 0.4f);
                }
                case MINERAL_VEIN -> {
                    BlockType[] ores = {BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.GOLD_ORE};
                    feature.properties.put("ore_type", ores[random.nextInt(ores.length)]);
                    feature.properties.put("richness", intensity);
                }
            }
            
            geologicalFeatures.put(position, feature);
        }
    }
    
    private void generateOreDeposits(int count) {
        BlockType[] oreTypes = {BlockType.COAL_ORE, BlockType.IRON_ORE, BlockType.GOLD_ORE};
        float[] rarities = {0.6f, 0.3f, 0.1f}; // Coal common, iron uncommon, gold rare
        
        for (int i = 0; i < count; i++) {
            Vector3f position = new Vector3f(
                (random.nextFloat() - 0.5f) * 4000,
                random.nextFloat() * -100 - 10, // Underground: -10 to -110
                (random.nextFloat() - 0.5f) * 4000
            );
            
            // Select ore type based on rarity
            BlockType oreType = BlockType.COAL_ORE;
            float roll = random.nextFloat();
            float cumulative = 0;
            
            for (int j = 0; j < oreTypes.length; j++) {
                cumulative += rarities[j];
                if (roll <= cumulative) {
                    oreType = oreTypes[j];
                    break;
                }
            }
            
            int quantity = switch (oreType) {
                case COAL_ORE -> 50 + random.nextInt(150);
                case IRON_ORE -> 20 + random.nextInt(80);
                case GOLD_ORE -> 5 + random.nextInt(20);
                default -> 10;
            };
            
            float richness = 0.3f + random.nextFloat() * 0.7f;
            
            oreDeposits.put(position, new OreDeposit(position, oreType, quantity, richness));
        }
    }
    
    private void generateTreasureLocations(int count) {
        TreasureType[] types = TreasureType.values();
        
        for (int i = 0; i < count; i++) {
            Vector3f position = new Vector3f(
                (random.nextFloat() - 0.5f) * 4000,
                random.nextFloat() * 20 + SEA_LEVEL - 30, // Can be above or below sea level
                (random.nextFloat() - 0.5f) * 4000
            );
            
            TreasureType type = types[random.nextInt(types.length)];
            TreasureLocation treasure = new TreasureLocation(position, type);
            
            treasureLocations.add(treasure);
        }
    }
    
    public void update(double deltaTime) {
        // Update volcanic activity
        updateVolcanicActivity(deltaTime);
        
        // Update geological processes (very slow)
        updateGeologicalProcesses(deltaTime);
    }
    
    private void updateVolcanicActivity(double deltaTime) {
        for (VolcanicIsland volcano : volcanicIslands) {
            if (volcano.isActive) {
                volcano.lastEruption += deltaTime;
                
                // Random chance of eruption (very rare)
                if (volcano.lastEruption > 3600 && random.nextFloat() < deltaTime * 0.0001) { // Once per ~3 hours minimum
                    triggerVolcanicEruption(volcano);
                    volcano.lastEruption = 0.0;
                }
            }
        }
    }
    
    private void triggerVolcanicEruption(VolcanicIsland volcano) {
        logger.info("Volcanic eruption at {} with intensity {}", volcano.center, volcano.lavaLevel);
        
        // Increase lava level
        volcano.lavaLevel = Math.min(volcano.height, volcano.lavaLevel + 5 + random.nextFloat() * 10);
        
        // Create new geological features around the volcano
        for (int i = 0; i < 3; i++) {
            Vector2f newFeaturePos = new Vector2f(volcano.center).add(
                (random.nextFloat() - 0.5f) * volcano.radius * 2,
                (random.nextFloat() - 0.5f) * volcano.radius * 2
            );
            
            GeologicalFeature lavaFeature = new GeologicalFeature(
                newFeaturePos, FeatureType.UNDERWATER_VENT, 0.8f + random.nextFloat() * 0.2f
            );
            
            geologicalFeatures.put(newFeaturePos, lavaFeature);
        }
    }
    
    private void updateGeologicalProcesses(double deltaTime) {
        // Very slow geological changes
        // This could include island erosion, new island formation, etc.
        
        // Occasionally spawn new small islands
        if (random.nextFloat() < deltaTime * 0.00001) { // Extremely rare
            Vector2f newIslandPos = new Vector2f(
                (random.nextFloat() - 0.5f) * 6000,
                (random.nextFloat() - 0.5f) * 6000
            );
            
            Island newIsland = new Island(newIslandPos, 20 + random.nextFloat() * 50, 
                                        10 + random.nextFloat() * 30, IslandType.TROPICAL_ATOLL);
            generatedIslands.add(newIsland);
            
            logger.info("New island formed at {} due to geological activity", newIslandPos);
        }
    }
    
    public float getHeightAt(float x, float z) {
        // Generate base ocean floor using enhanced Simplex noise
        float baseOceanFloor = SEA_LEVEL - 30; // Default deeper ocean floor
        
        // Continental shelf generation using FBM
        float continentalNoise = mountainNoise.fbm(x * 0.0005f, z * 0.0005f, 4, 2.0f, 0.5f);
        float continentalHeight = baseOceanFloor + continentalNoise * 40.0f; // Continental shelf variation
        
        // Underwater mountain ranges using ridged noise
        float underwaterMountains = mountainNoise.ridgedNoise(x * 0.002f, z * 0.002f, 6, 2.0f, 0.6f);
        if (underwaterMountains > 0.4f) {
            continentalHeight += (underwaterMountains - 0.4f) * 80.0f; // Underwater peaks
        }
        
        // Ocean trenches using inverted ridged noise
        float trenchNoise = mountainNoise.ridgedNoise(x * 0.001f + 1000, z * 0.001f + 1000, 4, 2.0f, 0.5f);
        if (trenchNoise > 0.7f) {
            continentalHeight -= (trenchNoise - 0.7f) * 120.0f; // Deep ocean trenches
        }
        
        // Local terrain variation using multiple noise layers
        float localTerrain = mountainNoise.octaveNoise(x * 0.01f, z * 0.01f, 8, 0.5f);
        float microTerrain = mountainNoise.octaveNoise(x * 0.05f, z * 0.05f, 4, 0.3f);
        
        float height = continentalHeight + localTerrain * 15.0f + microTerrain * 5.0f;
        
        // Island mask for archipelago generation
        float islandMask = generateIslandMask(x, z);
        
        // Apply island elevation where mask is strong
        if (islandMask > 0.3f) {
            float islandElevation = (islandMask - 0.3f) * 100.0f; // Raise land above sea level
            
            // Add mountainous terrain to islands using 3D Simplex noise
            float mountainTerrain = mountainNoise.simplex3D(x * 0.02f, islandElevation * 0.01f, z * 0.02f);
            islandElevation += mountainTerrain * 30.0f;
            
            // Create coastal cliffs using turbulence
            float coastalDistance = Math.abs(islandMask - 0.5f) * 2.0f; // Distance from coast
            if (coastalDistance < 0.3f) {
                float cliffNoise = mountainNoise.turbulence(x * 0.1f, z * 0.1f, 3);
                if (cliffNoise > 0.6f) {
                    islandElevation += (cliffNoise - 0.6f) * 25.0f; // Coastal cliffs
                }
            }
            
            height = Math.max(height, SEA_LEVEL + islandElevation);
        }
        
        // Check if position is on any pre-generated island (legacy support)
        for (Island island : generatedIslands) {
            float distance = new Vector2f(x, z).distance(island.center);
            if (distance <= island.radius) {
                // Use smooth falloff from center to edge with noise variation
                float falloff = 1.0f - (distance / island.radius);
                falloff = falloff * falloff; // Smooth curve
                
                // Add terrain detail to islands
                float islandDetail = mountainNoise.fbm(x * 0.02f, z * 0.02f, 6, 2.0f, 0.5f);
                float islandHeight = island.height * falloff + islandDetail * 20.0f;
                
                height = Math.max(height, SEA_LEVEL + islandHeight);
            }
        }
        
        // Check volcanic islands with enhanced terrain
        for (VolcanicIsland volcano : volcanicIslands) {
            float distance = new Vector2f(x, z).distance(volcano.center);
            if (distance <= volcano.radius) {
                float falloff = 1.0f - (distance / volcano.radius);
                falloff = falloff * falloff;
                
                // Add volcanic terrain detail using 3D Simplex noise
                float volcanoHeight = volcano.height * falloff;
                float volcanicDetail = mountainNoise.simplex3D(x * 0.03f, volcanoHeight * 0.02f, z * 0.03f);
                volcanoHeight += volcanicDetail * 25.0f;
                
                // Crater depression with realistic slopes
                float craterDistance = new Vector2f(x, z).distance(volcano.craterCenter);
                if (craterDistance <= volcano.craterRadius) {
                    float craterFalloff = 1.0f - (craterDistance / volcano.craterRadius);
                    craterFalloff = craterFalloff * craterFalloff * craterFalloff; // Steep crater walls
                    float craterDepth = craterFalloff * volcano.height * 0.4f;
                    
                    // Add crater wall detail
                    float craterWallNoise = mountainNoise.turbulence(x * 0.2f, z * 0.2f, 4);
                    craterDepth += craterWallNoise * 8.0f;
                    
                    volcanoHeight -= craterDepth;
                }
                
                height = Math.max(height, SEA_LEVEL + volcanoHeight);
            }
        }
        
        return height;
    }
    
    /**
     * Generate island mask for natural archipelago distribution
     */
    private float generateIslandMask(float x, float z) {
        // Large-scale island distribution
        float islandNoise1 = mountainNoise.fbm(x * 0.0008f, z * 0.0008f, 3, 2.0f, 0.6f);
        
        // Medium-scale island clusters
        float islandNoise2 = mountainNoise.octaveNoise(x * 0.003f, z * 0.003f, 4, 0.5f);
        
        // Small-scale island variation
        float islandNoise3 = mountainNoise.simplex2D(x * 0.01f, z * 0.01f);
        
        // Combine noise layers for natural island distribution
        float combinedNoise = islandNoise1 * 0.5f + islandNoise2 * 0.3f + islandNoise3 * 0.2f;
        
        // Apply threshold and smoothing
        return Math.max(0, combinedNoise);
    }
    
    public BlockType getBlockAt(int x, int y, int z) {
        float terrainHeight = getHeightAt(x, z);
        
        // Handle blocks above terrain
        if (y > terrainHeight) {
            // If we're above terrain but below or at sea level, it's water
            if (y <= SEA_LEVEL) {
                return BlockType.WATER;
            } else {
                return BlockType.AIR;
            }
        }
        
        // Handle blocks at or below terrain level
        
        // Advanced cave generation system
        if (y < terrainHeight - 3) {
            boolean isCave = caveGenerator.isCave(x, y, z, terrainHeight);
            
            if (isCave) {
                // Check for air pockets in underwater caves
                if (y < SEA_LEVEL && caveGenerator.hasAirPocket(x, y, z, terrainHeight)) {
                    return BlockType.AIR; // Air pocket for breathing
                }
                
                // Check for underground rivers
                if (caveGenerator.isUndergroundRiver(x, y, z, terrainHeight)) {
                    return BlockType.WATER; // Underground river
                }
                
                // Regular cave logic
                if (y <= SEA_LEVEL - 5) {
                    return BlockType.WATER; // Flooded cave
                } else {
                    return BlockType.AIR; // Cave space
                }
            }
        }
        
        // Enhanced ore distribution using noise-based veins
        Vector3f pos = new Vector3f(x, y, z);
        
        // Check for pre-placed ore deposits
        for (Map.Entry<Vector3f, OreDeposit> entry : oreDeposits.entrySet()) {
            if (pos.distance(entry.getKey()) <= 8.0f) { // Larger ore deposit radius
                OreDeposit deposit = entry.getValue();
                float distanceFactor = 1.0f - (pos.distance(entry.getKey()) / 8.0f);
                if (random.nextFloat() < deposit.richness * distanceFactor) {
                    return deposit.oreType;
                }
            }
        }
        
        // Procedural ore veins using noise
        if (y < terrainHeight - 10) {
            // Coal veins (common, shallow)
            if (y > terrainHeight - 40) {
                float coalNoise = mountainNoise.simplex3D(x * 0.05f, y * 0.05f, z * 0.05f);
                if (coalNoise > 0.7f) {
                    return BlockType.COAL_ORE;
                }
            }
            
            // Iron veins (uncommon, medium depth)
            if (y < terrainHeight - 20 && y > terrainHeight - 80) {
                float ironNoise = mountainNoise.simplex3D(x * 0.03f + 500, y * 0.03f + 500, z * 0.03f + 500);
                if (ironNoise > 0.8f) {
                    return BlockType.IRON_ORE;
                }
            }
            
            // Gold veins (rare, deep)
            if (y < terrainHeight - 50) {
                float goldNoise = mountainNoise.simplex3D(x * 0.02f + 1500, y * 0.02f + 1500, z * 0.02f + 1500);
                if (goldNoise > 0.85f) {
                    return BlockType.GOLD_ORE;
                }
            }
        }
        
        // Determine block type based on height, depth, and biome
        float depthFromSurface = terrainHeight - y;
        
        if (depthFromSurface <= 1) {
            // Surface layer - determine by biome and height
            if (terrainHeight > SEA_LEVEL + 20) {
                // High altitude - rocky terrain
                float rockNoise = mountainNoise.octaveNoise(x * 0.1f, z * 0.1f, 3, 0.5f);
                if (rockNoise > 0.3f) {
                    return BlockType.STONE; // Rocky outcrops
                } else {
                    return BlockType.GRASS; // Grass on mountains
                }
            } else if (terrainHeight > SEA_LEVEL + 5) {
                return BlockType.GRASS; // Normal land surface
            } else if (terrainHeight > SEA_LEVEL - 2) {
                return BlockType.SAND; // Beach/shallow water
            } else {
                // Ocean floor
                float oceanFloorNoise = mountainNoise.octaveNoise(x * 0.05f, z * 0.05f, 4, 0.6f);
                if (oceanFloorNoise > 0.4f) {
                    return BlockType.SAND; // Sandy ocean floor
                } else {
                    return BlockType.STONE; // Rocky ocean floor
                }
            }
        } else if (depthFromSurface <= 5) {
            // Subsurface layer
            if (terrainHeight > SEA_LEVEL) {
                return BlockType.DIRT; // Soil layer on land
            } else {
                return BlockType.SAND; // Sediment layer underwater
            }
        } else if (depthFromSurface <= 15) {
            // Transition to rock
            float transitionNoise = mountainNoise.octaveNoise(x * 0.08f, z * 0.08f, 2, 0.4f);
            if (transitionNoise > 0.2f) {
                return BlockType.STONE;
            } else {
                return terrainHeight > SEA_LEVEL ? BlockType.DIRT : BlockType.SAND;
            }
        } else {
            // Deep rock layer
            return BlockType.STONE;
        }
    }
    
    public float getTemperatureAt(float x, float z) {
        // Base temperature varies with latitude (distance from equator)
        float latitude = Math.abs(z) / 1000.0f; // Normalized latitude
        float baseTemp = 30.0f - latitude * 25.0f; // 30°C at equator, 5°C at poles
        
        // Add noise for local variation
        float tempNoise = temperatureNoise.octaveNoise(x * 0.001f, z * 0.001f, 3, 0.5f);
        
        return baseTemp + tempNoise * 10.0f;
    }
    
    public float getHumidityAt(float x, float z) {
        // Base humidity
        float baseHumidity = 60.0f;
        
        // Higher humidity near water
        float distanceToWater = 1000.0f; // Default large distance
        for (Island island : generatedIslands) {
            float distanceToIsland = new Vector2f(x, z).distance(island.center);
            if (distanceToIsland > island.radius) {
                distanceToWater = Math.min(distanceToWater, distanceToIsland - island.radius);
            }
        }
        
        float waterEffect = Math.max(0, 30.0f - distanceToWater * 0.1f);
        
        // Add noise
        float humidityNoiseValue = humidityNoise.octaveNoise(x * 0.002f, z * 0.002f, 3, 0.5f);
        
        return Math.max(10, Math.min(100, baseHumidity + waterEffect + humidityNoiseValue * 20));
    }
    
    public List<Island> getNearbyIslands(Vector2f position, float radius) {
        List<Island> nearby = new ArrayList<>();
        
        for (Island island : generatedIslands) {
            if (position.distance(island.center) <= radius + island.radius) {
                nearby.add(island);
            }
        }
        
        return nearby;
    }
    
    public List<TreasureLocation> getNearbyTreasures(Vector3f position, float radius) {
        List<TreasureLocation> nearby = new ArrayList<>();
        
        for (TreasureLocation treasure : treasureLocations) {
            if (position.distance(treasure.position) <= radius) {
                nearby.add(treasure);
            }
        }
        
        return nearby;
    }
    
    public void cleanup() {
        logger.info("Cleaning up world generator");
        generatedIslands.clear();
        volcanicIslands.clear();
        caveSystems.clear();
        geologicalFeatures.clear();
        oreDeposits.clear();
        treasureLocations.clear();
    }
    
    // Getters
    public long getSeed() { return seed; }
    public List<Island> getGeneratedIslands() { return new ArrayList<>(generatedIslands); }
    public List<VolcanicIsland> getVolcanicIslands() { return new ArrayList<>(volcanicIslands); }
    public Map<Vector2f, GeologicalFeature> getGeologicalFeatures() { return new HashMap<>(geologicalFeatures); }
    public List<TreasureLocation> getTreasureLocations() { return new ArrayList<>(treasureLocations); }
}