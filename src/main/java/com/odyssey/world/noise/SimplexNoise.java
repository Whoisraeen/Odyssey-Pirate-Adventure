package com.odyssey.world.noise;

/**
 * High-performance Simplex noise implementation for terrain generation.
 * Based on Ken Perlin's improved noise algorithm, optimized for continuous terrain generation.
 * 
 * Simplex noise provides several advantages over Perlin noise:
 * - Better visual quality with fewer directional artifacts
 * - Lower computational complexity (O(n²) vs O(2^n))
 * - More uniform gradient distribution
 * - Better scaling to higher dimensions
 */
public class SimplexNoise {
    
    // Simplex noise constants
    private static final int[] GRAD3 = {
        1,1,0,-1,1,0,1,-1,0,-1,-1,0,
        1,0,1,-1,0,1,1,0,-1,-1,0,-1,
        0,1,1,0,-1,1,0,1,-1,0,-1,-1
    };
    
    private final int[] P = new int[512];
    private final int[] PERM_MOD12 = new int[512];
    
    // Skewing and unskewing factors for 2D
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    
    // Skewing and unskewing factors for 3D
    private static final double F3 = 1.0 / 3.0;
    private static final double G3 = 1.0 / 6.0;
    
    /**
     * Constructor that initializes the noise generator with a specific seed
     * @param seed The seed for the random number generator
     */
    public SimplexNoise(long seed) {
        initializePermutationTable(seed);
    }
    
    /**
     * Default constructor using a default seed
     */
    public SimplexNoise() {
        this(0L);
    }
    
    /**
     * Initialize the permutation table with the given seed
     */
    private void initializePermutationTable(long seed) {
        java.util.Random random = new java.util.Random(seed);
        
        // Create base permutation array
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        
        // Shuffle the permutation array using Fisher-Yates algorithm
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        // Duplicate the permutation table
        for (int i = 0; i < 256; i++) {
            P[i] = P[i + 256] = permutation[i];
            PERM_MOD12[i] = PERM_MOD12[i + 256] = P[i] % 12;
        }
    }
    
    /**
     * Generate 2D Simplex noise (static version)
     * @param xin X coordinate
     * @param yin Y coordinate
     * @return Noise value between -1 and 1
     */
    public static double noise(double xin, double yin) {
        // Use a default instance for static calls
        return new SimplexNoise().noiseInstance(xin, yin);
    }
    
    /**
     * Generate 2D Simplex noise (instance method)
     * @param xin X coordinate
     * @param yin Y coordinate
     * @return Noise value between -1 and 1
     */
    public double noiseInstance(double xin, double yin) {
        double n0, n1, n2; // Noise contributions from the three corners
        
        // Skew the input space to determine which simplex cell we're in
        double s = (xin + yin) * F2; // Hairy factor for 2D
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        double t = (i + j) * G2;
        double X0 = i - t; // Unskew the cell origin back to (x,y) space
        double Y0 = j - t;
        double x0 = xin - X0; // The x,y distances from the cell origin
        double y0 = yin - Y0;
        
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
        double x1 = x0 - i1 + G2; // Offsets for middle corner in (x,y) unskewed coords
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2; // Offsets for last corner in (x,y) unskewed coords
        double y2 = y0 - 1.0 + 2.0 * G2;
        
        // Work out the hashed gradient indices of the three simplex corners
        int ii = i & 255;
        int jj = j & 255;
        int gi0 = PERM_MOD12[ii + P[jj]];
        int gi1 = PERM_MOD12[ii + i1 + P[jj + j1]];
        int gi2 = PERM_MOD12[ii + 1 + P[jj + 1]];
        
        // Calculate the contribution from the three corners
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) {
            n0 = 0.0;
        } else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3, gi0, x0, y0);
        }
        
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) {
            n1 = 0.0;
        } else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3, gi1, x1, y1);
        }
        
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) {
            n2 = 0.0;
        } else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3, gi2, x2, y2);
        }
        
        // Add contributions from each corner to get the final noise value.
        // The result is scaled to return values in the interval [-1,1].
        return 70.0 * (n0 + n1 + n2);
    }
    
    /**
     * Generate 3D Simplex noise
     * @param xin X coordinate
     * @param yin Y coordinate
     * @param zin Z coordinate
     * @return Noise value between -1 and 1
     */
    public double noise(double xin, double yin, double zin) {
        double n0, n1, n2, n3; // Noise contributions from the four corners
        
        // Skew the input space to determine which simplex cell we're in
        double s = (xin + yin + zin) * F3; // Very nice and simple skew factor for 3D
        int i = fastFloor(xin + s);
        int j = fastFloor(yin + s);
        int k = fastFloor(zin + s);
        double t = (i + j + k) * G3;
        double X0 = i - t; // Unskew the cell origin back to (x,y,z) space
        double Y0 = j - t;
        double Z0 = k - t;
        double x0 = xin - X0; // The x,y,z distances from the cell origin
        double y0 = yin - Y0;
        double z0 = zin - Z0;
        
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
        double x1 = x0 - i1 + G3; // Offsets for second corner in (x,y,z) coords
        double y1 = y0 - j1 + G3;
        double z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0 * G3; // Offsets for third corner in (x,y,z) coords
        double y2 = y0 - j2 + 2.0 * G3;
        double z2 = z0 - k2 + 2.0 * G3;
        double x3 = x0 - 1.0 + 3.0 * G3; // Offsets for last corner in (x,y,z) coords
        double y3 = y0 - 1.0 + 3.0 * G3;
        double z3 = z0 - 1.0 + 3.0 * G3;
        
        // Work out the hashed gradient indices of the four simplex corners
        int ii = i & 255;
        int jj = j & 255;
        int kk = k & 255;
        int gi0 = PERM_MOD12[ii + P[jj + P[kk]]];
        int gi1 = PERM_MOD12[ii + i1 + P[jj + j1 + P[kk + k1]]];
        int gi2 = PERM_MOD12[ii + i2 + P[jj + j2 + P[kk + k2]]];
        int gi3 = PERM_MOD12[ii + 1 + P[jj + 1 + P[kk + 1]]];
        
        // Calculate the contribution from the four corners
        double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
        if (t0 < 0) {
            n0 = 0.0;
        } else {
            t0 *= t0;
            n0 = t0 * t0 * dot(GRAD3, gi0, x0, y0, z0);
        }
        
        double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
        if (t1 < 0) {
            n1 = 0.0;
        } else {
            t1 *= t1;
            n1 = t1 * t1 * dot(GRAD3, gi1, x1, y1, z1);
        }
        
        double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
        if (t2 < 0) {
            n2 = 0.0;
        } else {
            t2 *= t2;
            n2 = t2 * t2 * dot(GRAD3, gi2, x2, y2, z2);
        }
        
        double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
        if (t3 < 0) {
            n3 = 0.0;
        } else {
            t3 *= t3;
            n3 = t3 * t3 * dot(GRAD3, gi3, x3, y3, z3);
        }
        
        // Add contributions from each corner to get the final noise value.
        // The result is scaled to return values in the interval [-1,1].
        return 32.0 * (n0 + n1 + n2 + n3);
    }
    
    /**
     * Fast floor function
     */
    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
    
    /**
     * 2D dot product
     */
    private static double dot(int[] g, int gi, double x, double y) {
        return g[gi * 3] * x + g[gi * 3 + 1] * y;
    }
    
    /**
     * 3D dot product
     */
    private static double dot(int[] g, int gi, double x, double y, double z) {
        return g[gi * 3] * x + g[gi * 3 + 1] * y + g[gi * 3 + 2] * z;
    }
    
    /**
     * Generate fractal Simplex noise with multiple octaves (instance method)
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Fractal noise value
     */
    public float fractalNoise(float x, float y, int octaves, float persistence, float lacunarity) {
        return (float) fractal(x, y, octaves, persistence, lacunarity);
    }
    
    /**
     * Generate fractal Simplex noise with multiple octaves
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Fractal noise value
     */
    public static double fractal(double x, double y, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Generate ridged Simplex noise (instance method)
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Ridged noise value
     */
    public float ridgedNoise(float x, float y, int octaves, float persistence, float lacunarity) {
        return (float) ridged(x, y, octaves, persistence, lacunarity);
    }
    
    /**
     * Generate ridged Simplex noise (inverted absolute value)
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Ridged noise value
     */
    public static double ridged(double x, double y, int octaves, double persistence, double lacunarity) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;
        double maxValue = 0.0;
        
        for (int i = 0; i < octaves; i++) {
            double n = Math.abs(noise(x * frequency, y * frequency));
            n = 1.0 - n;
            n = n * n;
            total += n * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        return total / maxValue;
    }
    
    /**
     * Instance method for 3D noise generation
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Noise value between -1 and 1
     */
    public float noise3D(float x, float y, float z) {
        return (float) noise(x, y, z);
    }

    /**
     * Generate turbulence (absolute value of fractal noise)
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Turbulence noise value
     */
    public static double turbulence(double x, double y, int octaves, double persistence, double lacunarity) {
        return Math.abs(fractal(x, y, octaves, persistence, lacunarity));
    }
    
    /**
     * Generate turbulence noise (instance method)
     * @param x X coordinate
     * @param y Y coordinate
     * @param octaves Number of noise octaves
     * @param persistence Amplitude multiplier for each octave
     * @param lacunarity Frequency multiplier for each octave
     * @return Turbulence noise value
     */
    public float turbulenceNoise(float x, float y, int octaves, float persistence, float lacunarity) {
        return (float) turbulence(x, y, octaves, persistence, lacunarity);
    }
}