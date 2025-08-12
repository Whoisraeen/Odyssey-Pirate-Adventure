package com.odyssey.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event bus system for handling game events and data-driven function scheduling.
 * Provides a flexible event system with priority handling and async execution.
 */
public class EventBus {
    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);
    private static volatile EventBus instance;
    
    private final Map<Class<?>, List<EventHandler>> handlers;
    private final Map<Object, List<EventHandler>> listenerHandlers;
    private final ExecutorService asyncExecutor;
    private final Set<Class<?>> registeredEventTypes;
    private boolean enabled = true;
    
    public EventBus() {
        this.handlers = new ConcurrentHashMap<>();
        this.listenerHandlers = new ConcurrentHashMap<>();
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventBus-Async");
            t.setDaemon(true);
            return t;
        });
        this.registeredEventTypes = ConcurrentHashMap.newKeySet();
        
        logger.info("Event bus initialized");
    }
    
    /**
     * Gets the singleton instance of EventBus
     */
    public static EventBus getInstance() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }
    
    /**
     * Posts an event to all registered handlers
     */
    public void post(Object event) {
        if (!enabled) {
            return;
        }
        
        Class<?> eventType = event.getClass();
        List<EventHandler> eventHandlers = handlers.get(eventType);
        
        if (eventHandlers != null && !eventHandlers.isEmpty()) {
            // Sort handlers by priority
            List<EventHandler> sortedHandlers = new ArrayList<>(eventHandlers);
            sortedHandlers.sort(Comparator.comparingInt(h -> h.priority.ordinal()));
            
            for (EventHandler handler : sortedHandlers) {
                try {
                    if (handler.async) {
                        asyncExecutor.submit(() -> invokeHandler(handler, event));
                    } else {
                        invokeHandler(handler, event);
                    }
                } catch (Exception e) {
                    logger.error("Error posting event {} to handler {}: {}", 
                        eventType.getSimpleName(), handler.method.getName(), e.getMessage(), e);
                }
            }
        }
        
        // Also check for handlers of parent classes/interfaces
        postToParentTypes(event, eventType);
    }
    
    /**
     * Posts event to handlers of parent types
     */
    private void postToParentTypes(Object event, Class<?> eventType) {
        // Check superclasses
        Class<?> superClass = eventType.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            List<EventHandler> superHandlers = handlers.get(superClass);
            if (superHandlers != null) {
                for (EventHandler handler : superHandlers) {
                    try {
                        if (handler.async) {
                            asyncExecutor.submit(() -> invokeHandler(handler, event));
                        } else {
                            invokeHandler(handler, event);
                        }
                    } catch (Exception e) {
                        logger.error("Error posting event to super handler: {}", e.getMessage(), e);
                    }
                }
            }
            postToParentTypes(event, superClass);
        }
        
        // Check interfaces
        for (Class<?> interfaceType : eventType.getInterfaces()) {
            List<EventHandler> interfaceHandlers = handlers.get(interfaceType);
            if (interfaceHandlers != null) {
                for (EventHandler handler : interfaceHandlers) {
                    try {
                        if (handler.async) {
                            asyncExecutor.submit(() -> invokeHandler(handler, event));
                        } else {
                            invokeHandler(handler, event);
                        }
                    } catch (Exception e) {
                        logger.error("Error posting event to interface handler: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Invokes an event handler
     */
    private void invokeHandler(EventHandler handler, Object event) {
        try {
            handler.method.invoke(handler.listener, event);
        } catch (Exception e) {
            logger.error("Error invoking event handler {}: {}", 
                handler.method.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Registers an event listener
     */
    public void register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        Class<?> listenerClass = listener.getClass();
        List<EventHandler> handlerList = new ArrayList<>();
        
        // Find all methods with @Subscribe annotation
        for (Method method : listenerClass.getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation != null) {
                // Validate method signature
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1) {
                    logger.warn("Event handler method {} must have exactly one parameter", method.getName());
                    continue;
                }
                
                Class<?> eventType = paramTypes[0];
                method.setAccessible(true);
                
                EventHandler handler = new EventHandler(
                    listener, method, eventType, annotation.priority(), annotation.async()
                );
                
                // Add to global handlers map
                handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
                
                // Add to listener-specific handlers
                handlerList.add(handler);
                
                registeredEventTypes.add(eventType);
                
                logger.debug("Registered event handler for {} in {}", 
                    eventType.getSimpleName(), listenerClass.getSimpleName());
            }
        }
        
        if (!handlerList.isEmpty()) {
            listenerHandlers.put(listener, handlerList);
            logger.info("Registered {} event handlers for {}", handlerList.size(), listenerClass.getSimpleName());
        }
    }
    
    /**
     * Unregisters an event listener
     */
    public void unregister(Object listener) {
        List<EventHandler> handlerList = listenerHandlers.remove(listener);
        if (handlerList != null) {
            for (EventHandler handler : handlerList) {
                List<EventHandler> typeHandlers = handlers.get(handler.eventType);
                if (typeHandlers != null) {
                    typeHandlers.remove(handler);
                    if (typeHandlers.isEmpty()) {
                        handlers.remove(handler.eventType);
                        registeredEventTypes.remove(handler.eventType);
                    }
                }
            }
            logger.info("Unregistered {} event handlers for {}", 
                handlerList.size(), listener.getClass().getSimpleName());
        }
    }
    
    /**
     * Registers a simple event handler function
     */
    public <T> void registerHandler(Class<T> eventType, EventListener<T> handler) {
        registerHandler(eventType, handler, EventPriority.NORMAL, false);
    }
    
    /**
     * Registers an event handler function with priority and async options
     */
    public <T> void registerHandler(Class<T> eventType, EventListener<T> handler, 
                                  EventPriority priority, boolean async) {
        Object listenerWrapper = new Object() {
            @Subscribe(priority = EventPriority.NORMAL, async = false)
            public void handle(Object event) {
                if (eventType.isInstance(event)) {
                    handler.handle(eventType.cast(event));
                }
            }
        };
        
        register(listenerWrapper);
    }
    
    /**
     * Checks if an event type has any handlers
     */
    public boolean hasHandlers(Class<?> eventType) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        return eventHandlers != null && !eventHandlers.isEmpty();
    }
    
    /**
     * Gets the number of handlers for an event type
     */
    public int getHandlerCount(Class<?> eventType) {
        List<EventHandler> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }
    
    /**
     * Gets all registered event types
     */
    public Set<Class<?>> getRegisteredEventTypes() {
        return new HashSet<>(registeredEventTypes);
    }
    
    /**
     * Enables or disables the event bus
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Event bus {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Shuts down the event bus
     */
    public void shutdown() {
        enabled = false;
        asyncExecutor.shutdown();
        handlers.clear();
        listenerHandlers.clear();
        registeredEventTypes.clear();
        logger.info("Event bus shut down");
    }
    
    /**
     * Event handler wrapper class
     */
    private static class EventHandler {
        final Object listener;
        final Method method;
        final Class<?> eventType;
        final EventPriority priority;
        final boolean async;
        
        EventHandler(Object listener, Method method, Class<?> eventType, 
                    EventPriority priority, boolean async) {
            this.listener = listener;
            this.method = method;
            this.eventType = eventType;
            this.priority = priority;
            this.async = async;
        }
    }
    
    /**
     * Event priority enumeration
     */
    public enum EventPriority {
        HIGHEST,
        HIGH,
        NORMAL,
        LOW,
        LOWEST
    }
    
    /**
     * Subscribe annotation for marking event handler methods
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Subscribe {
        EventPriority priority() default EventPriority.NORMAL;
        boolean async() default false;
    }
    
    /**
     * Functional interface for event listeners
     */
    @FunctionalInterface
    public interface EventListener<T> {
        void handle(T event);
    }
    
    // Common game events
    public static class GameEvent {
        private final long timestamp;
        
        public GameEvent() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
    
    public static class PlayerEvent extends GameEvent {
        private final String playerName;
        
        public PlayerEvent(String playerName) {
            this.playerName = playerName;
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
    
    public static class PlayerJoinEvent extends PlayerEvent {
        public PlayerJoinEvent(String playerName) {
            super(playerName);
        }
    }
    
    public static class PlayerLeaveEvent extends PlayerEvent {
        public PlayerLeaveEvent(String playerName) {
            super(playerName);
        }
    }
    
    public static class WorldEvent extends GameEvent {
        private final String worldName;
        
        public WorldEvent(String worldName) {
            this.worldName = worldName;
        }
        
        public String getWorldName() {
            return worldName;
        }
    }
    
    public static class WorldLoadEvent extends WorldEvent {
        public WorldLoadEvent(String worldName) {
            super(worldName);
        }
    }
    
    public static class WorldUnloadEvent extends WorldEvent {
        public WorldUnloadEvent(String worldName) {
            super(worldName);
        }
    }
    
    public static class BlockEvent extends GameEvent {
        private final int x, y, z;
        private final String blockType;
        
        public BlockEvent(int x, int y, int z, String blockType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockType = blockType;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
        public String getBlockType() { return blockType; }
    }
    
    public static class BlockPlaceEvent extends BlockEvent {
        private final String playerName;
        
        public BlockPlaceEvent(int x, int y, int z, String blockType, String playerName) {
            super(x, y, z, blockType);
            this.playerName = playerName;
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
    
    public static class BlockBreakEvent extends BlockEvent {
        private final String playerName;
        
        public BlockBreakEvent(int x, int y, int z, String blockType, String playerName) {
            super(x, y, z, blockType);
            this.playerName = playerName;
        }
        
        public String getPlayerName() {
            return playerName;
        }
    }
    
    public static class ChunkEvent extends GameEvent {
        private final int chunkX, chunkZ;
        
        public ChunkEvent(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
        
        public int getChunkX() { return chunkX; }
        public int getChunkZ() { return chunkZ; }
    }
    
    public static class ChunkLoadEvent extends ChunkEvent {
        public ChunkLoadEvent(int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
        }
    }
    
    public static class ChunkUnloadEvent extends ChunkEvent {
        public ChunkUnloadEvent(int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
        }
    }
    
    public static class WeatherChangeEvent extends GameEvent {
        private final String oldWeather;
        private final String newWeather;
        
        public WeatherChangeEvent(String oldWeather, String newWeather) {
            this.oldWeather = oldWeather;
            this.newWeather = newWeather;
        }
        
        public String getOldWeather() { return oldWeather; }
        public String getNewWeather() { return newWeather; }
    }
    
    public static class TimeChangeEvent extends GameEvent {
        private final long oldTime;
        private final long newTime;
        
        public TimeChangeEvent(long oldTime, long newTime) {
            this.oldTime = oldTime;
            this.newTime = newTime;
        }
        
        public long getOldTime() { return oldTime; }
        public long getNewTime() { return newTime; }
    }
    
    public static class EntityEvent extends GameEvent {
        private final String entityId;
        private final String entityType;
        
        public EntityEvent(String entityId, String entityType) {
            this.entityId = entityId;
            this.entityType = entityType;
        }
        
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
    }
    
    public static class EntitySpawnEvent extends EntityEvent {
        private final int x, y, z;
        
        public EntitySpawnEvent(String entityId, String entityType, int x, int y, int z) {
            super(entityId, entityType);
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getZ() { return z; }
    }
    
    public static class EntityDeathEvent extends EntityEvent {
        private final String cause;
        
        public EntityDeathEvent(String entityId, String entityType, String cause) {
            super(entityId, entityType);
            this.cause = cause;
        }
        
        public String getCause() { return cause; }
    }
}