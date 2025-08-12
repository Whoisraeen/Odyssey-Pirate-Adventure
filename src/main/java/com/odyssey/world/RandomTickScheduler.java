package com.odyssey.world;

import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random tick scheduler for handling block updates like crop growth, leaf decay, fire spread, etc.
 * Implements a system similar to Minecraft's random tick system.
 */
public class RandomTickScheduler {
    private static final Logger logger = LoggerFactory.getLogger(RandomTickScheduler.class);
    
    private final World world;
    private final Map<ChunkPosition, Set<BlockPosition>> randomTickBlocks;
    private final Set<String> randomTickableBlocks;
    private int randomTickSpeed = 3; // Default random tick speed
    
    public RandomTickScheduler(World world) {
        this.world = world;
        this.randomTickBlocks = new ConcurrentHashMap<>();
        this.randomTickableBlocks = new HashSet<>();
        
        // Initialize default random tickable blocks
        initializeRandomTickableBlocks();
    }
    
    private void initializeRandomTickableBlocks() {
        // Add blocks that should receive random ticks
        randomTickableBlocks.add("grass");
        randomTickableBlocks.add("dirt");
        randomTickableBlocks.add("farmland");
        randomTickableBlocks.add("wheat");
        randomTickableBlocks.add("carrots");
        randomTickableBlocks.add("potatoes");
        randomTickableBlocks.add("beetroots");
        randomTickableBlocks.add("sugar_cane");
        randomTickableBlocks.add("cactus");
        randomTickableBlocks.add("kelp");
        randomTickableBlocks.add("bamboo");
        randomTickableBlocks.add("leaves");
        randomTickableBlocks.add("oak_leaves");
        randomTickableBlocks.add("birch_leaves");
        randomTickableBlocks.add("spruce_leaves");
        randomTickableBlocks.add("jungle_leaves");
        randomTickableBlocks.add("acacia_leaves");
        randomTickableBlocks.add("dark_oak_leaves");
        randomTickableBlocks.add("fire");
        randomTickableBlocks.add("lava");
        randomTickableBlocks.add("water");
        randomTickableBlocks.add("coral");
        randomTickableBlocks.add("coral_block");
        randomTickableBlocks.add("sea_grass");
        randomTickableBlocks.add("kelp_plant");
        randomTickableBlocks.add("ice");
        randomTickableBlocks.add("snow");
        randomTickableBlocks.add("mycelium");
        randomTickableBlocks.add("podzol");
        
        logger.info("Initialized {} random tickable block types", randomTickableBlocks.size());
    }
    
    /**
     * Process random ticks for all loaded chunks
     */
    public void tick() {
        if (randomTickSpeed <= 0) {
            return; // Random ticks disabled
        }
        
        // Get all loaded chunks from the world
        Collection<Chunk> loadedChunks = world.getChunkManager().getLoadedChunks();
        
        for (Chunk chunk : loadedChunks) {
            processChunkRandomTicks(chunk);
        }
    }
    
    /**
     * Process random ticks for a specific chunk
     */
    private void processChunkRandomTicks(Chunk chunk) {
        ChunkPosition chunkPos = chunk.getPosition();
        
        // Each chunk section gets randomTickSpeed attempts per tick
        for (int section = 0; section < (Chunk.CHUNK_HEIGHT / 16); section++) {
            for (int i = 0; i < randomTickSpeed; i++) {
                // Pick a random block in this chunk section
                int x = ThreadLocalRandom.current().nextInt(16);
                int y = section * 16 + ThreadLocalRandom.current().nextInt(16);
                int z = ThreadLocalRandom.current().nextInt(16);
                
                // Convert to world coordinates
                int worldX = chunkPos.x * 16 + x;
                int worldZ = chunkPos.z * 16 + z;
                
                // Get the block at this position
                com.odyssey.world.generation.WorldGenerator.BlockType blockType = chunk.getBlock(x, y, z);
                if (blockType != null && isRandomTickable(blockType)) {
                    processRandomTick(worldX, y, worldZ, blockType);
                }
            }
        }
    }
    
    /**
     * Process a random tick for a specific block
     */
    private void processRandomTick(int x, int y, int z, String blockType) {
        try {
            switch (blockType) {
                case "grass":
                    processGrassRandomTick(x, y, z);
                    break;
                case "dirt":
                    processDirtRandomTick(x, y, z);
                    break;
                case "farmland":
                    processFarmlandRandomTick(x, y, z);
                    break;
                case "wheat":
                case "carrots":
                case "potatoes":
                case "beetroots":
                    processCropRandomTick(x, y, z, blockType);
                    break;
                case "sugar_cane":
                    processSugarCaneRandomTick(x, y, z);
                    break;
                case "cactus":
                    processCactusRandomTick(x, y, z);
                    break;
                case "leaves":
                case "oak_leaves":
                case "birch_leaves":
                case "spruce_leaves":
                case "jungle_leaves":
                case "acacia_leaves":
                case "dark_oak_leaves":
                    processLeavesRandomTick(x, y, z);
                    break;
                case "fire":
                    processFireRandomTick(x, y, z);
                    break;
                case "coral":
                case "coral_block":
                    processCoralRandomTick(x, y, z);
                    break;
                case "ice":
                    processIceRandomTick(x, y, z);
                    break;
                case "snow":
                    processSnowRandomTick(x, y, z);
                    break;
                default:
                    // Generic random tick processing
                    logger.debug("Random tick for {} at ({}, {}, {})", blockType, x, y, z);
                    break;
            }
        } catch (Exception e) {
            logger.warn("Error processing random tick for {} at ({}, {}, {}): {}", 
                blockType, x, y, z, e.getMessage());
        }
    }
    
    private void processGrassRandomTick(int x, int y, int z) {
        // Grass can spread to nearby dirt blocks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    int nx = x + dx, ny = y + dy, nz = z + dz;
                    String neighborBlock = world.getBlock(nx, ny, nz);
                    
                    if ("dirt".equals(neighborBlock) && hasLightAccess(nx, ny, nz)) {
                        if (ThreadLocalRandom.current().nextFloat() < 0.25f) {
                            world.setBlock(nx, ny, nz, "grass");
                        }
                    }
                }
            }
        }
    }
    
    private void processDirtRandomTick(int x, int y, int z) {
        // Dirt can turn to grass if it has light access
        if (hasLightAccess(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
                world.setBlock(x, y, z, "grass");
            }
        }
    }
    
    private void processFarmlandRandomTick(int x, int y, int z) {
        // Farmland can turn back to dirt if not hydrated
        if (!isHydrated(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
                world.setBlock(x, y, z, "dirt");
            }
        }
    }
    
    private void processCropRandomTick(int x, int y, int z, String cropType) {
        // Crops grow over time
        // This is a simplified implementation - in a real game you'd have growth stages
        if (isValidCropGrowthCondition(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                // Advance growth stage (simplified)
                logger.debug("Crop {} growing at ({}, {}, {})", cropType, x, y, z);
                // In a real implementation, you'd update the block state/metadata
            }
        }
    }
    
    private void processSugarCaneRandomTick(int x, int y, int z) {
        // Sugar cane can grow upward
        if (world.getBlock(x, y + 1, z) == null || "air".equals(world.getBlock(x, y + 1, z))) {
            if (hasWaterNearby(x, y, z) && ThreadLocalRandom.current().nextFloat() < 0.1f) {
                world.setBlock(x, y + 1, z, "sugar_cane");
            }
        }
    }
    
    private void processCactusRandomTick(int x, int y, int z) {
        // Cactus can grow upward
        if (world.getBlock(x, y + 1, z) == null || "air".equals(world.getBlock(x, y + 1, z))) {
            if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
                world.setBlock(x, y + 1, z, "cactus");
            }
        }
    }
    
    private void processLeavesRandomTick(int x, int y, int z) {
        // Leaves decay if not connected to a log
        if (!isConnectedToLog(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                world.setBlock(x, y, z, "air");
                // Drop items based on leaf type
                String leafType = world.getBlock(x, y, z);
                float random = ThreadLocalRandom.current().nextFloat();
                
                if (random < 0.05f) { // 5% chance to drop sapling
                    String saplingType = leafType.replace("leaves", "sapling");
                    world.dropItem(x, y, z, saplingType, 1);
                }
                
                if (leafType.equals("oak_leaves") && random < 0.005f) { // 0.5% chance for oak leaves to drop apple
                    world.dropItem(x, y, z, "apple", 1);
                }
            }
        }
    }
    
    private void processFireRandomTick(int x, int y, int z) {
        // Fire spreads and burns out
        if (ThreadLocalRandom.current().nextFloat() < 0.3f) {
            // Fire burns out
            world.setBlock(x, y, z, "air");
        } else {
            // Fire spreads to nearby flammable blocks
            spreadFire(x, y, z);
        }
    }
    
    private void processCoralRandomTick(int x, int y, int z) {
        // Coral dies if not in water
        if (!isInWater(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
                world.setBlock(x, y, z, "dead_coral");
            }
        }
    }
    
    private void processIceRandomTick(int x, int y, int z) {
        // Ice melts in warm conditions
        if (isWarmEnvironment(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
                world.setBlock(x, y, z, "water");
            }
        }
    }
    
    private void processSnowRandomTick(int x, int y, int z) {
        // Snow melts in warm conditions
        if (isWarmEnvironment(x, y, z)) {
            if (ThreadLocalRandom.current().nextFloat() < 0.1f) {
                world.setBlock(x, y, z, "air");
            }
        }
    }
    
    // Helper methods
    private boolean isRandomTickable(String blockType) {
        return randomTickableBlocks.contains(blockType);
    }
    
    private boolean hasLightAccess(int x, int y, int z) {
        // Check if the block above is transparent or if there's sufficient light
        String blockAbove = world.getBlock(x, y + 1, z);
        return blockAbove == null || "air".equals(blockAbove) || isTransparent(blockAbove);
    }
    
    private boolean isTransparent(String blockType) {
        return "air".equals(blockType) || "water".equals(blockType) || "glass".equals(blockType);
    }
    
    private boolean isHydrated(int x, int y, int z) {
        // Check for water within 4 blocks horizontally
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                String block = world.getBlock(x + dx, y, z + dz);
                if ("water".equals(block)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isValidCropGrowthCondition(int x, int y, int z) {
        // Check if crop has proper farmland below and light above
        String blockBelow = world.getBlock(x, y - 1, z);
        return "farmland".equals(blockBelow) && hasLightAccess(x, y, z);
    }
    
    private boolean hasWaterNearby(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                String block = world.getBlock(x + dx, y, z + dz);
                if ("water".equals(block)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isConnectedToLog(int x, int y, int z) {
        // Simplified check - in reality this would be a flood-fill algorithm
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    String block = world.getBlock(x + dx, y + dy, z + dz);
                    if (isLog(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isLog(String blockType) {
        return blockType != null && (blockType.contains("log") || blockType.contains("wood"));
    }
    
    private void spreadFire(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    
                    int nx = x + dx, ny = y + dy, nz = z + dz;
                    String block = world.getBlock(nx, ny, nz);
                    
                    if (isFlammable(block) && ThreadLocalRandom.current().nextFloat() < 0.1f) {
                        world.setBlock(nx, ny, nz, "fire");
                    }
                }
            }
        }
    }
    
    private boolean isFlammable(String blockType) {
        return blockType != null && (blockType.contains("wood") || blockType.contains("leaves") || 
                                   blockType.contains("wool") || blockType.contains("planks"));
    }
    
    private boolean isInWater(int x, int y, int z) {
        return "water".equals(world.getBlock(x, y, z)) || hasWaterNearby(x, y, z);
    }
    
    private boolean isWarmEnvironment(int x, int y, int z) {
        // Simplified - check for nearby heat sources or biome temperature
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    String block = world.getBlock(x + dx, y + dy, z + dz);
                    if (isHeatSource(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean isHeatSource(String blockType) {
        return "fire".equals(blockType) || "lava".equals(blockType) || 
               "torch".equals(blockType) || "furnace".equals(blockType);
    }
    
    // Configuration methods
    public void setRandomTickSpeed(int speed) {
        this.randomTickSpeed = Math.max(0, speed);
        logger.info("Random tick speed set to {}", this.randomTickSpeed);
    }
    
    public int getRandomTickSpeed() {
        return randomTickSpeed;
    }
    
    public void addRandomTickableBlock(String blockType) {
        randomTickableBlocks.add(blockType);
        logger.debug("Added {} to random tickable blocks", blockType);
    }
    
    public void removeRandomTickableBlock(String blockType) {
        randomTickableBlocks.remove(blockType);
        logger.debug("Removed {} from random tickable blocks", blockType);
    }
    
    public Set<String> getRandomTickableBlocks() {
        return new HashSet<>(randomTickableBlocks);
    }
    
    /**
     * Simple block position class for internal use
     */
    private static class BlockPosition {
        final int x, y, z;
        
        BlockPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockPosition)) return false;
            BlockPosition other = (BlockPosition) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }
}