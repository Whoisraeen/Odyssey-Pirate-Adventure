package com.odyssey.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Block event bus and notifier system for handling block-related events.
 * Provides a centralized system for block change notifications, neighbor updates,
 * and block-specific event handling.
 */
public class BlockEventBus {
    private static final Logger logger = LoggerFactory.getLogger(BlockEventBus.class);
    
    private final World world;
    private final Map<Class<? extends BlockEvent>, List<BlockEventHandler<?>>> handlers;
    private final Map<String, List<BlockEventHandler<BlockChangeEvent>>> blockChangeHandlers;
    private final Map<String, List<BlockEventHandler<BlockNeighborUpdateEvent>>> neighborUpdateHandlers;
    private final Set<BlockEventListener> globalListeners;
    private final Queue<BlockEvent> eventQueue;
    private boolean processingEvents = false;
    private int maxEventsPerTick = 1000;
    
    public BlockEventBus(World world) {
        this.world = world;
        this.handlers = new ConcurrentHashMap<>();
        this.blockChangeHandlers = new ConcurrentHashMap<>();
        this.neighborUpdateHandlers = new ConcurrentHashMap<>();
        this.globalListeners = ConcurrentHashMap.newKeySet();
        this.eventQueue = new ArrayDeque<>();
        
        logger.info("Block event bus initialized");
    }
    
    /**
     * Posts a block event to the bus
     */
    public void post(BlockEvent event) {
        if (event == null) {
            logger.warn("Attempted to post null block event");
            return;
        }
        
        synchronized (eventQueue) {
            eventQueue.offer(event);
        }
        
        logger.debug("Posted block event: {}", event.getClass().getSimpleName());
    }
    
    /**
     * Posts a block change event
     */
    public void postBlockChange(int x, int y, int z, String oldBlock, String newBlock, Object cause) {
        BlockChangeEvent event = new BlockChangeEvent(x, y, z, oldBlock, newBlock, cause);
        post(event);
    }
    
    /**
     * Posts a block break event
     */
    public void postBlockBreak(int x, int y, int z, String blockType, Object breaker) {
        BlockBreakEvent event = new BlockBreakEvent(x, y, z, blockType, breaker);
        post(event);
    }
    
    /**
     * Posts a block place event
     */
    public void postBlockPlace(int x, int y, int z, String blockType, Object placer) {
        BlockPlaceEvent event = new BlockPlaceEvent(x, y, z, blockType, placer);
        post(event);
    }
    
    /**
     * Posts a neighbor update event
     */
    public void postNeighborUpdate(int x, int y, int z, String blockType, int neighborX, int neighborY, int neighborZ, String neighborType) {
        BlockNeighborUpdateEvent event = new BlockNeighborUpdateEvent(x, y, z, blockType, neighborX, neighborY, neighborZ, neighborType);
        post(event);
    }
    
    /**
     * Posts a block interaction event
     */
    public void postBlockInteraction(int x, int y, int z, String blockType, Object interactor, String interactionType) {
        BlockInteractionEvent event = new BlockInteractionEvent(x, y, z, blockType, interactor, interactionType);
        post(event);
    }
    
    /**
     * Posts a block update event
     */
    public void postBlockUpdate(int x, int y, int z, String blockType, String updateType) {
        BlockUpdateEvent event = new BlockUpdateEvent(x, y, z, blockType, updateType);
        post(event);
    }
    
    /**
     * Registers an event handler for a specific event type
     */
    @SuppressWarnings("unchecked")
    public <T extends BlockEvent> void register(Class<T> eventType, BlockEventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add((BlockEventHandler<?>) handler);
        logger.debug("Registered handler for event type: {}", eventType.getSimpleName());
    }
    
    /**
     * Registers a block change handler for a specific block type
     */
    public void registerBlockChange(String blockType, BlockEventHandler<BlockChangeEvent> handler) {
        blockChangeHandlers.computeIfAbsent(blockType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("Registered block change handler for: {}", blockType);
    }
    
    /**
     * Registers a neighbor update handler for a specific block type
     */
    public void registerNeighborUpdate(String blockType, BlockEventHandler<BlockNeighborUpdateEvent> handler) {
        neighborUpdateHandlers.computeIfAbsent(blockType, k -> new CopyOnWriteArrayList<>()).add(handler);
        logger.debug("Registered neighbor update handler for: {}", blockType);
    }
    
    /**
     * Registers a global event listener
     */
    public void registerGlobalListener(BlockEventListener listener) {
        globalListeners.add(listener);
        logger.debug("Registered global block event listener");
    }
    
    /**
     * Unregisters an event handler
     */
    public <T extends BlockEvent> void unregister(Class<T> eventType, BlockEventHandler<T> handler) {
        List<BlockEventHandler<?>> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            if (eventHandlers.isEmpty()) {
                handlers.remove(eventType);
            }
        }
        logger.debug("Unregistered handler for event type: {}", eventType.getSimpleName());
    }
    
    /**
     * Unregisters a block change handler
     */
    public void unregisterBlockChange(String blockType, BlockEventHandler<BlockChangeEvent> handler) {
        List<BlockEventHandler<BlockChangeEvent>> blockHandlers = blockChangeHandlers.get(blockType);
        if (blockHandlers != null) {
            blockHandlers.remove(handler);
            if (blockHandlers.isEmpty()) {
                blockChangeHandlers.remove(blockType);
            }
        }
        logger.debug("Unregistered block change handler for: {}", blockType);
    }
    
    /**
     * Unregisters a neighbor update handler
     */
    public void unregisterNeighborUpdate(String blockType, BlockEventHandler<BlockNeighborUpdateEvent> handler) {
        List<BlockEventHandler<BlockNeighborUpdateEvent>> neighborHandlers = neighborUpdateHandlers.get(blockType);
        if (neighborHandlers != null) {
            neighborHandlers.remove(handler);
            if (neighborHandlers.isEmpty()) {
                neighborUpdateHandlers.remove(blockType);
            }
        }
        logger.debug("Unregistered neighbor update handler for: {}", blockType);
    }
    
    /**
     * Unregisters a global listener
     */
    public void unregisterGlobalListener(BlockEventListener listener) {
        globalListeners.remove(listener);
        logger.debug("Unregistered global block event listener");
    }
    
    /**
     * Processes queued events
     */
    public void processEvents() {
        if (processingEvents) {
            return; // Prevent recursive processing
        }
        
        processingEvents = true;
        int processed = 0;
        
        try {
            while (processed < maxEventsPerTick) {
                BlockEvent event;
                synchronized (eventQueue) {
                    event = eventQueue.poll();
                }
                
                if (event == null) {
                    break;
                }
                
                try {
                    processEvent(event);
                    processed++;
                } catch (Exception e) {
                    logger.error("Error processing block event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
            
            if (processed > 0) {
                logger.debug("Processed {} block events", processed);
            }
        } finally {
            processingEvents = false;
        }
    }
    
    /**
     * Processes a single event
     */
    @SuppressWarnings("unchecked")
    private void processEvent(BlockEvent event) {
        // Notify global listeners first
        for (BlockEventListener listener : globalListeners) {
            try {
                listener.onBlockEvent(event);
            } catch (Exception e) {
                logger.error("Error in global block event listener: {}", e.getMessage(), e);
            }
        }
        
        // Handle specific event types
        List<BlockEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers != null) {
            for (BlockEventHandler<?> handler : eventHandlers) {
                try {
                    ((BlockEventHandler<BlockEvent>) handler).handle(event);
                } catch (Exception e) {
                    logger.error("Error in block event handler: {}", e.getMessage(), e);
                }
            }
        }
        
        // Handle block-specific events
        if (event instanceof BlockChangeEvent) {
            handleBlockChangeEvent((BlockChangeEvent) event);
        } else if (event instanceof BlockNeighborUpdateEvent) {
            handleNeighborUpdateEvent((BlockNeighborUpdateEvent) event);
        }
        
        logger.debug("Processed event: {} at ({}, {}, {})", 
            event.getClass().getSimpleName(), event.getX(), event.getY(), event.getZ());
    }
    
    /**
     * Handles block change events
     */
    private void handleBlockChangeEvent(BlockChangeEvent event) {
        // Handle old block type handlers
        List<BlockEventHandler<BlockChangeEvent>> oldHandlers = blockChangeHandlers.get(event.getOldBlockType());
        if (oldHandlers != null) {
            for (BlockEventHandler<BlockChangeEvent> handler : oldHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("Error in block change handler for {}: {}", event.getOldBlockType(), e.getMessage(), e);
                }
            }
        }
        
        // Handle new block type handlers
        List<BlockEventHandler<BlockChangeEvent>> newHandlers = blockChangeHandlers.get(event.getNewBlockType());
        if (newHandlers != null) {
            for (BlockEventHandler<BlockChangeEvent> handler : newHandlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("Error in block change handler for {}: {}", event.getNewBlockType(), e.getMessage(), e);
                }
            }
        }
        
        // Trigger neighbor updates
        triggerNeighborUpdates(event.getX(), event.getY(), event.getZ(), event.getNewBlockType());
    }
    
    /**
     * Handles neighbor update events
     */
    private void handleNeighborUpdateEvent(BlockNeighborUpdateEvent event) {
        List<BlockEventHandler<BlockNeighborUpdateEvent>> handlers = neighborUpdateHandlers.get(event.getBlockType());
        if (handlers != null) {
            for (BlockEventHandler<BlockNeighborUpdateEvent> handler : handlers) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    logger.error("Error in neighbor update handler for {}: {}", event.getBlockType(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Triggers neighbor updates for surrounding blocks
     */
    private void triggerNeighborUpdates(int x, int y, int z, String changedBlockType) {
        // Update all 6 adjacent neighbors
        int[][] neighbors = {
            {x + 1, y, z}, {x - 1, y, z},
            {x, y + 1, z}, {x, y - 1, z},
            {x, y, z + 1}, {x, y, z - 1}
        };
        
        for (int[] neighbor : neighbors) {
            int nx = neighbor[0], ny = neighbor[1], nz = neighbor[2];
            String neighborType = world.getBlock(nx, ny, nz);
            
            if (neighborType != null && !neighborType.equals("air")) {
                postNeighborUpdate(nx, ny, nz, neighborType, x, y, z, changedBlockType);
            }
        }
    }
    
    /**
     * Clears all events and handlers
     */
    public void clear() {
        synchronized (eventQueue) {
            eventQueue.clear();
        }
        handlers.clear();
        blockChangeHandlers.clear();
        neighborUpdateHandlers.clear();
        globalListeners.clear();
        logger.info("Cleared all block events and handlers");
    }
    
    /**
     * Gets the number of queued events
     */
    public int getQueueSize() {
        synchronized (eventQueue) {
            return eventQueue.size();
        }
    }
    
    /**
     * Sets the maximum events to process per tick
     */
    public void setMaxEventsPerTick(int maxEvents) {
        this.maxEventsPerTick = Math.max(1, maxEvents);
        logger.info("Max events per tick set to {}", this.maxEventsPerTick);
    }
    
    public int getMaxEventsPerTick() {
        return maxEventsPerTick;
    }
    
    // Event classes
    public abstract static class BlockEvent {
        protected final int x, y, z;
        protected final long timestamp;
        
        public BlockEvent(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = System.currentTimeMillis();
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class BlockChangeEvent extends BlockEvent {
        private final String oldBlockType;
        private final String newBlockType;
        private final Object cause;
        
        public BlockChangeEvent(int x, int y, int z, String oldBlockType, String newBlockType, Object cause) {
            super(x, y, z);
            this.oldBlockType = oldBlockType;
            this.newBlockType = newBlockType;
            this.cause = cause;
        }
        
        public String getOldBlockType() { return oldBlockType; }
        public String getNewBlockType() { return newBlockType; }
        public Object getCause() { return cause; }
    }
    
    public static class BlockBreakEvent extends BlockEvent {
        private final String blockType;
        private final Object breaker;
        private boolean cancelled = false;
        
        public BlockBreakEvent(int x, int y, int z, String blockType, Object breaker) {
            super(x, y, z);
            this.blockType = blockType;
            this.breaker = breaker;
        }
        
        public String getBlockType() { return blockType; }
        public Object getBreaker() { return breaker; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    public static class BlockPlaceEvent extends BlockEvent {
        private final String blockType;
        private final Object placer;
        private boolean cancelled = false;
        
        public BlockPlaceEvent(int x, int y, int z, String blockType, Object placer) {
            super(x, y, z);
            this.blockType = blockType;
            this.placer = placer;
        }
        
        public String getBlockType() { return blockType; }
        public Object getPlacer() { return placer; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    public static class BlockNeighborUpdateEvent extends BlockEvent {
        private final String blockType;
        private final int neighborX, neighborY, neighborZ;
        private final String neighborType;
        
        public BlockNeighborUpdateEvent(int x, int y, int z, String blockType, int neighborX, int neighborY, int neighborZ, String neighborType) {
            super(x, y, z);
            this.blockType = blockType;
            this.neighborX = neighborX;
            this.neighborY = neighborY;
            this.neighborZ = neighborZ;
            this.neighborType = neighborType;
        }
        
        public String getBlockType() { return blockType; }
        public int getNeighborX() { return neighborX; }
        public int getNeighborY() { return neighborY; }
        public int getNeighborZ() { return neighborZ; }
        public String getNeighborType() { return neighborType; }
    }
    
    public static class BlockInteractionEvent extends BlockEvent {
        private final String blockType;
        private final Object interactor;
        private final String interactionType;
        private boolean cancelled = false;
        
        public BlockInteractionEvent(int x, int y, int z, String blockType, Object interactor, String interactionType) {
            super(x, y, z);
            this.blockType = blockType;
            this.interactor = interactor;
            this.interactionType = interactionType;
        }
        
        public String getBlockType() { return blockType; }
        public Object getInteractor() { return interactor; }
        public String getInteractionType() { return interactionType; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
    
    public static class BlockUpdateEvent extends BlockEvent {
        private final String blockType;
        private final String updateType;
        
        public BlockUpdateEvent(int x, int y, int z, String blockType, String updateType) {
            super(x, y, z);
            this.blockType = blockType;
            this.updateType = updateType;
        }
        
        public String getBlockType() { return blockType; }
        public String getUpdateType() { return updateType; }
    }
    
    // Handler interfaces
    @FunctionalInterface
    public interface BlockEventHandler<T extends BlockEvent> {
        void handle(T event);
    }
    
    @FunctionalInterface
    public interface BlockEventListener {
        void onBlockEvent(BlockEvent event);
    }
}