package com.odyssey.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Scheduled tick queue system for handling delayed block updates and scheduled events.
 * Manages timed events like redstone delays, crop growth, water flow, etc.
 */
public class ScheduledTickQueue {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTickQueue.class);
    
    private final World world;
    private final PriorityBlockingQueue<ScheduledTick> tickQueue;
    private final Map<BlockPosition, Set<ScheduledTick>> positionTicks;
    private final Map<String, Integer> defaultDelays;
    private long currentTick = 0;
    private int maxTicksPerUpdate = 1000; // Prevent lag spikes
    
    public ScheduledTickQueue(World world) {
        this.world = world;
        this.tickQueue = new PriorityBlockingQueue<>();
        this.positionTicks = new ConcurrentHashMap<>();
        this.defaultDelays = new HashMap<>();
        
        initializeDefaultDelays();
        logger.info("Scheduled tick queue initialized");
    }
    
    /**
     * Initializes default delays for various block types
     */
    private void initializeDefaultDelays() {
        // Redstone components
        defaultDelays.put("redstone_wire", 1);
        defaultDelays.put("redstone_torch", 2);
        defaultDelays.put("redstone_repeater", 2);
        defaultDelays.put("redstone_comparator", 2);
        defaultDelays.put("piston", 2);
        defaultDelays.put("sticky_piston", 2);
        
        // Fluids
        defaultDelays.put("water", 5);
        defaultDelays.put("lava", 30);
        defaultDelays.put("flowing_water", 5);
        defaultDelays.put("flowing_lava", 30);
        
        // Plants and growth
        defaultDelays.put("wheat", 200);
        defaultDelays.put("carrots", 200);
        defaultDelays.put("potatoes", 200);
        defaultDelays.put("beetroots", 200);
        defaultDelays.put("sugar_cane", 300);
        defaultDelays.put("cactus", 300);
        defaultDelays.put("bamboo", 100);
        defaultDelays.put("kelp", 150);
        
        // Fire and burning
        defaultDelays.put("fire", 30);
        defaultDelays.put("soul_fire", 30);
        
        // Ice and snow
        defaultDelays.put("ice", 100);
        defaultDelays.put("snow", 50);
        defaultDelays.put("powder_snow", 50);
        
        // Leaves decay
        defaultDelays.put("leaves", 100);
        defaultDelays.put("oak_leaves", 100);
        defaultDelays.put("birch_leaves", 100);
        defaultDelays.put("spruce_leaves", 100);
        defaultDelays.put("jungle_leaves", 100);
        defaultDelays.put("acacia_leaves", 100);
        defaultDelays.put("dark_oak_leaves", 100);
        
        // Gravity blocks
        defaultDelays.put("sand", 2);
        defaultDelays.put("gravel", 2);
        defaultDelays.put("anvil", 2);
        defaultDelays.put("concrete_powder", 2);
        
        // Other blocks
        defaultDelays.put("farmland", 60);
        defaultDelays.put("grass_block", 100);
        defaultDelays.put("mycelium", 100);
        defaultDelays.put("podzol", 100);
        
        logger.debug("Initialized {} default block delays", defaultDelays.size());
    }
    
    /**
     * Schedules a tick for a block at the specified position
     */
    public void scheduleTick(int x, int y, int z, String blockType, int delay) {
        scheduleTick(x, y, z, blockType, delay, TickPriority.NORMAL);
    }
    
    /**
     * Schedules a tick with priority
     */
    public void scheduleTick(int x, int y, int z, String blockType, int delay, TickPriority priority) {
        if (delay < 0) {
            logger.warn("Attempted to schedule tick with negative delay: {}", delay);
            return;
        }
        
        long scheduledTime = currentTick + delay;
        BlockPosition pos = new BlockPosition(x, y, z);
        
        ScheduledTick tick = new ScheduledTick(pos, blockType, scheduledTime, priority);
        
        // Add to queue
        tickQueue.offer(tick);
        
        // Add to position mapping
        positionTicks.computeIfAbsent(pos, k -> ConcurrentHashMap.newKeySet()).add(tick);
        
        logger.debug("Scheduled tick for {} at ({}, {}, {}) in {} ticks", 
            blockType, x, y, z, delay);
    }
    
    /**
     * Schedules a tick with default delay for the block type
     */
    public void scheduleTickDefault(int x, int y, int z, String blockType) {
        int delay = defaultDelays.getOrDefault(blockType, 1);
        scheduleTick(x, y, z, blockType, delay);
    }
    
    /**
     * Schedules a tick with default delay and priority
     */
    public void scheduleTickDefault(int x, int y, int z, String blockType, TickPriority priority) {
        int delay = defaultDelays.getOrDefault(blockType, 1);
        scheduleTick(x, y, z, blockType, delay, priority);
    }
    
    /**
     * Cancels all scheduled ticks at a position
     */
    public void cancelTicks(int x, int y, int z) {
        BlockPosition pos = new BlockPosition(x, y, z);
        Set<ScheduledTick> ticks = positionTicks.remove(pos);
        
        if (ticks != null) {
            for (ScheduledTick tick : ticks) {
                tick.cancelled = true;
            }
            logger.debug("Cancelled {} ticks at ({}, {}, {})", ticks.size(), x, y, z);
        }
    }
    
    /**
     * Cancels scheduled ticks for a specific block type at a position
     */
    public void cancelTicks(int x, int y, int z, String blockType) {
        BlockPosition pos = new BlockPosition(x, y, z);
        Set<ScheduledTick> ticks = positionTicks.get(pos);
        
        if (ticks != null) {
            int cancelled = 0;
            Iterator<ScheduledTick> iterator = ticks.iterator();
            while (iterator.hasNext()) {
                ScheduledTick tick = iterator.next();
                if (tick.blockType.equals(blockType)) {
                    tick.cancelled = true;
                    iterator.remove();
                    cancelled++;
                }
            }
            
            if (ticks.isEmpty()) {
                positionTicks.remove(pos);
            }
            
            if (cancelled > 0) {
                logger.debug("Cancelled {} {} ticks at ({}, {}, {})", cancelled, blockType, x, y, z);
            }
        }
    }
    
    /**
     * Checks if there are scheduled ticks at a position
     */
    public boolean hasScheduledTicks(int x, int y, int z) {
        BlockPosition pos = new BlockPosition(x, y, z);
        Set<ScheduledTick> ticks = positionTicks.get(pos);
        return ticks != null && !ticks.isEmpty();
    }
    
    /**
     * Checks if there are scheduled ticks for a specific block type at a position
     */
    public boolean hasScheduledTicks(int x, int y, int z, String blockType) {
        BlockPosition pos = new BlockPosition(x, y, z);
        Set<ScheduledTick> ticks = positionTicks.get(pos);
        
        if (ticks != null) {
            for (ScheduledTick tick : ticks) {
                if (tick.blockType.equals(blockType) && !tick.cancelled) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets the next scheduled tick time for a position
     */
    public long getNextTickTime(int x, int y, int z) {
        BlockPosition pos = new BlockPosition(x, y, z);
        Set<ScheduledTick> ticks = positionTicks.get(pos);
        
        if (ticks != null) {
            long nextTime = Long.MAX_VALUE;
            for (ScheduledTick tick : ticks) {
                if (!tick.cancelled && tick.scheduledTime < nextTime) {
                    nextTime = tick.scheduledTime;
                }
            }
            return nextTime == Long.MAX_VALUE ? -1 : nextTime;
        }
        return -1;
    }
    
    /**
     * Processes scheduled ticks for the current game tick
     */
    public void tick() {
        currentTick++;
        
        int processed = 0;
        while (processed < maxTicksPerUpdate && !tickQueue.isEmpty()) {
            ScheduledTick tick = tickQueue.peek();
            
            if (tick.scheduledTime > currentTick) {
                // No more ticks to process this tick
                break;
            }
            
            // Remove from queue
            tickQueue.poll();
            processed++;
            
            // Skip cancelled ticks
            if (tick.cancelled) {
                continue;
            }
            
            // Remove from position mapping
            Set<ScheduledTick> posTicks = positionTicks.get(tick.position);
            if (posTicks != null) {
                posTicks.remove(tick);
                if (posTicks.isEmpty()) {
                    positionTicks.remove(tick.position);
                }
            }
            
            // Execute the tick
            try {
                executeTick(tick);
            } catch (Exception e) {
                logger.error("Error executing scheduled tick for {} at {}: {}", 
                    tick.blockType, tick.position, e.getMessage(), e);
            }
        }
        
        if (processed > 0) {
            logger.debug("Processed {} scheduled ticks", processed);
        }
    }
    
    /**
     * Executes a scheduled tick
     */
    private void executeTick(ScheduledTick tick) {
        BlockPosition pos = tick.position;
        String currentBlock = world.getBlock(pos.x, pos.y, pos.z);
        
        // Verify the block is still the same type
        if (!tick.blockType.equals(currentBlock)) {
            logger.debug("Block changed from {} to {} at {}, skipping tick", 
                tick.blockType, currentBlock, pos);
            return;
        }
        
        // Execute block-specific tick logic
        switch (tick.blockType) {
            case "redstone_wire":
                updateRedstoneWire(pos);
                break;
            case "redstone_torch":
                updateRedstoneTorch(pos);
                break;
            case "redstone_repeater":
                updateRedstoneRepeater(pos);
                break;
            case "redstone_comparator":
                updateRedstoneComparator(pos);
                break;
            case "piston":
            case "sticky_piston":
                updatePiston(pos);
                break;
            case "water":
            case "flowing_water":
                updateWaterFlow(pos);
                break;
            case "lava":
            case "flowing_lava":
                updateLavaFlow(pos);
                break;
            case "fire":
            case "soul_fire":
                updateFire(pos);
                break;
            case "sand":
            case "gravel":
            case "anvil":
            case "concrete_powder":
                updateGravityBlock(pos);
                break;
            case "farmland":
                updateFarmland(pos);
                break;
            case "grass_block":
            case "mycelium":
            case "podzol":
                updateSpreadableBlock(pos);
                break;
            case "ice":
                updateIce(pos);
                break;
            case "snow":
            case "powder_snow":
                updateSnow(pos);
                break;
            default:
                // Handle crop growth and other generic ticks
                if (isCrop(tick.blockType)) {
                    updateCrop(pos, tick.blockType);
                } else if (isLeaves(tick.blockType)) {
                    updateLeaves(pos);
                } else {
                    logger.debug("No specific handler for block type: {}", tick.blockType);
                }
                break;
        }
        
        logger.debug("Executed tick for {} at {}", tick.blockType, pos);
    }
    
    // Block update methods
    private void updateRedstoneWire(BlockPosition pos) {
        // Update redstone wire power level and propagate
        world.getRedstoneSystem().updateWire(pos.x, pos.y, pos.z);
    }
    
    private void updateRedstoneTorch(BlockPosition pos) {
        // Update redstone torch state
        world.getRedstoneSystem().updateTorch(pos.x, pos.y, pos.z);
    }
    
    private void updateRedstoneRepeater(BlockPosition pos) {
        // Update repeater state and schedule next tick if needed
        world.getRedstoneSystem().updateRepeater(pos.x, pos.y, pos.z);
    }
    
    private void updateRedstoneComparator(BlockPosition pos) {
        // Update comparator state
        world.getRedstoneSystem().updateComparator(pos.x, pos.y, pos.z);
    }
    
    private void updatePiston(BlockPosition pos) {
        // Update piston extension/retraction
        world.getRedstoneSystem().updatePiston(pos.x, pos.y, pos.z);
    }
    
    private void updateWaterFlow(BlockPosition pos) {
        // Update water flow to adjacent blocks
        world.getFluidSystem().updateWaterFlow(pos.x, pos.y, pos.z);
    }
    
    private void updateLavaFlow(BlockPosition pos) {
        // Update lava flow to adjacent blocks
        world.getFluidSystem().updateLavaFlow(pos.x, pos.y, pos.z);
    }
    
    private void updateFire(BlockPosition pos) {
        // Fire spread and burnout logic
        world.getFireSystem().updateFire(pos.x, pos.y, pos.z);
    }
    
    private void updateGravityBlock(BlockPosition pos) {
        // Check if gravity block should fall
        world.getPhysicsSystem().updateGravityBlock(pos.x, pos.y, pos.z);
    }
    
    private void updateFarmland(BlockPosition pos) {
        // Check farmland hydration and reversion to dirt
        world.getAgricultureSystem().updateFarmland(pos.x, pos.y, pos.z);
    }
    
    private void updateSpreadableBlock(BlockPosition pos) {
        // Grass/mycelium spreading logic
        world.getSpreadSystem().updateSpreadableBlock(pos.x, pos.y, pos.z);
    }
    
    private void updateCrop(BlockPosition pos, String cropType) {
        // Crop growth logic
        world.getAgricultureSystem().updateCrop(pos.x, pos.y, pos.z, cropType);
    }
    
    private void updateLeaves(BlockPosition pos) {
        // Leaf decay logic
        world.getDecaySystem().updateLeaves(pos.x, pos.y, pos.z);
    }
    
    private void updateIce(BlockPosition pos) {
        // Ice melting logic
        world.getTemperatureSystem().updateIce(pos.x, pos.y, pos.z);
    }
    
    private void updateSnow(BlockPosition pos) {
        // Snow melting logic
        world.getTemperatureSystem().updateSnow(pos.x, pos.y, pos.z);
    }
    
    // Helper methods
    private boolean isCrop(String blockType) {
        return blockType.equals("wheat") || blockType.equals("carrots") || 
               blockType.equals("potatoes") || blockType.equals("beetroots") ||
               blockType.equals("sugar_cane") || blockType.equals("cactus") ||
               blockType.equals("bamboo") || blockType.equals("kelp");
    }
    
    private boolean isLeaves(String blockType) {
        return blockType.contains("leaves");
    }
    
    // Configuration methods
    public void setMaxTicksPerUpdate(int maxTicks) {
        this.maxTicksPerUpdate = Math.max(1, maxTicks);
        logger.info("Max ticks per update set to {}", this.maxTicksPerUpdate);
    }
    
    public int getMaxTicksPerUpdate() {
        return maxTicksPerUpdate;
    }
    
    public void setDefaultDelay(String blockType, int delay) {
        defaultDelays.put(blockType, Math.max(1, delay));
        logger.debug("Set default delay for {} to {}", blockType, delay);
    }
    
    public int getDefaultDelay(String blockType) {
        return defaultDelays.getOrDefault(blockType, 1);
    }
    
    public long getCurrentTick() {
        return currentTick;
    }
    
    public int getQueueSize() {
        return tickQueue.size();
    }
    
    public int getPositionCount() {
        return positionTicks.size();
    }
    
    /**
     * Clears all scheduled ticks
     */
    public void clear() {
        tickQueue.clear();
        positionTicks.clear();
        logger.info("Cleared all scheduled ticks");
    }
    
    /**
     * Scheduled tick class
     */
    private static class ScheduledTick implements Comparable<ScheduledTick> {
        final BlockPosition position;
        final String blockType;
        final long scheduledTime;
        final TickPriority priority;
        volatile boolean cancelled = false;
        
        ScheduledTick(BlockPosition position, String blockType, long scheduledTime, TickPriority priority) {
            this.position = position;
            this.blockType = blockType;
            this.scheduledTime = scheduledTime;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(ScheduledTick other) {
            int timeCompare = Long.compare(this.scheduledTime, other.scheduledTime);
            if (timeCompare != 0) {
                return timeCompare;
            }
            
            // If times are equal, compare by priority
            return Integer.compare(this.priority.ordinal(), other.priority.ordinal());
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ScheduledTick)) return false;
            ScheduledTick other = (ScheduledTick) obj;
            return Objects.equals(position, other.position) &&
                   Objects.equals(blockType, other.blockType) &&
                   scheduledTime == other.scheduledTime;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(position, blockType, scheduledTime);
        }
    }
    
    /**
     * Block position class
     */
    public static class BlockPosition {
        public final int x, y, z;
        
        public BlockPosition(int x, int y, int z) {
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
        
        @Override
        public String toString() {
            return String.format("(%d, %d, %d)", x, y, z);
        }
    }
    
    /**
     * Tick priority enumeration
     */
    public enum TickPriority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }
}