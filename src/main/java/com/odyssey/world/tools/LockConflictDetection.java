package com.odyssey.world.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Lock & Conflict Detection System.
 * Manages session.lock heartbeats and detects conflicts between multiple world access attempts.
 */
public class LockConflictDetection {
    private static final Logger logger = LoggerFactory.getLogger(LockConflictDetection.class);
    
    private static final String LOCK_FILE_NAME = "session.lock";
    private static final long HEARTBEAT_INTERVAL_MS = 5000; // 5 seconds
    private static final long LOCK_TIMEOUT_MS = 30000; // 30 seconds
    private static final long CONFLICT_CHECK_INTERVAL_MS = 1000; // 1 second
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Path, LockSession> activeLocks = new ConcurrentHashMap<>();
    private final Map<Path, ConflictMonitor> conflictMonitors = new ConcurrentHashMap<>();
    
    /**
     * Attempts to acquire a lock on the specified world directory.
     */
    public LockResult acquireLock(Path worldPath, String processId, LockOptions options) {
        logger.info("Attempting to acquire lock for world: {}", worldPath);
        
        try {
            Path lockFile = worldPath.resolve(LOCK_FILE_NAME);
            
            // Check if lock already exists
            if (Files.exists(lockFile)) {
                LockInfo existingLock = readLockInfo(lockFile);
                
                if (isLockValid(existingLock)) {
                    if (options.forceAcquire()) {
                        logger.warn("Force acquiring lock, breaking existing lock");
                        breakLock(lockFile);
                    } else {
                        return new LockResult(false, LockStatus.ALREADY_LOCKED, 
                            "World is already locked by process: " + existingLock.processId());
                    }
                } else {
                    logger.info("Found stale lock, removing");
                    Files.deleteIfExists(lockFile);
                }
            }
            
            // Create new lock
            LockInfo lockInfo = new LockInfo(
                processId,
                Instant.now(),
                Instant.now(),
                options.readOnly(),
                System.getProperty("user.name"),
                getHostname()
            );
            
            writeLockInfo(lockFile, lockInfo);
            
            // Start heartbeat
            LockSession session = new LockSession(lockFile, lockInfo, options);
            activeLocks.put(worldPath, session);
            
            ScheduledFuture<?> heartbeatTask = scheduler.scheduleAtFixedRate(
                () -> updateHeartbeat(session),
                HEARTBEAT_INTERVAL_MS,
                HEARTBEAT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
            session.setHeartbeatTask(heartbeatTask);
            
            // Start conflict monitoring if requested
            if (options.monitorConflicts()) {
                startConflictMonitoring(worldPath, session);
            }
            
            logger.info("Successfully acquired lock for world: {}", worldPath);
            return new LockResult(true, LockStatus.ACQUIRED, "Lock acquired successfully");
            
        } catch (Exception e) {
            logger.error("Failed to acquire lock for world: " + worldPath, e);
            return new LockResult(false, LockStatus.ERROR, "Failed to acquire lock: " + e.getMessage());
        }
    }
    
    /**
     * Releases the lock on the specified world directory.
     */
    public void releaseLock(Path worldPath) {
        logger.info("Releasing lock for world: {}", worldPath);
        
        LockSession session = activeLocks.remove(worldPath);
        if (session != null) {
            // Cancel heartbeat
            if (session.getHeartbeatTask() != null) {
                session.getHeartbeatTask().cancel(false);
            }
            
            // Remove lock file
            try {
                Files.deleteIfExists(session.getLockFile());
                logger.info("Lock released for world: {}", worldPath);
            } catch (IOException e) {
                logger.error("Failed to remove lock file: " + session.getLockFile(), e);
            }
        }
        
        // Stop conflict monitoring
        ConflictMonitor monitor = conflictMonitors.remove(worldPath);
        if (monitor != null) {
            monitor.stop();
        }
    }
    
    /**
     * Checks if a world is currently locked.
     */
    public LockStatus checkLockStatus(Path worldPath) {
        Path lockFile = worldPath.resolve(LOCK_FILE_NAME);
        
        if (!Files.exists(lockFile)) {
            return LockStatus.UNLOCKED;
        }
        
        try {
            LockInfo lockInfo = readLockInfo(lockFile);
            return isLockValid(lockInfo) ? LockStatus.LOCKED : LockStatus.STALE;
        } catch (Exception e) {
            logger.error("Failed to read lock info: " + lockFile, e);
            return LockStatus.ERROR;
        }
    }
    
    /**
     * Forces the removal of a lock (use with caution).
     */
    public boolean breakLock(Path worldPath) {
        Path lockFile = worldPath.resolve(LOCK_FILE_NAME);
        return breakLockFile(lockFile);
    }
    
    private boolean breakLockFile(Path lockFile) {
        try {
            Files.deleteIfExists(lockFile);
            logger.warn("Forcibly broke lock: {}", lockFile);
            return true;
        } catch (IOException e) {
            logger.error("Failed to break lock: " + lockFile, e);
            return false;
        }
    }
    
    /**
     * Lists all currently locked worlds in the specified directory.
     */
    public List<LockedWorld> listLockedWorlds(Path worldsDirectory) throws IOException {
        List<LockedWorld> lockedWorlds = new ArrayList<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldsDirectory)) {
            for (Path worldPath : stream) {
                if (Files.isDirectory(worldPath)) {
                    Path lockFile = worldPath.resolve(LOCK_FILE_NAME);
                    if (Files.exists(lockFile)) {
                        try {
                            LockInfo lockInfo = readLockInfo(lockFile);
                            LockStatus status = isLockValid(lockInfo) ? LockStatus.LOCKED : LockStatus.STALE;
                            lockedWorlds.add(new LockedWorld(worldPath, lockInfo, status));
                        } catch (Exception e) {
                            logger.error("Failed to read lock info for: " + worldPath, e);
                        }
                    }
                }
            }
        }
        
        return lockedWorlds;
    }
    
    private void startConflictMonitoring(Path worldPath, LockSession session) {
        ConflictMonitor monitor = new ConflictMonitor(worldPath, session);
        conflictMonitors.put(worldPath, monitor);
        
        ScheduledFuture<?> monitorTask = scheduler.scheduleAtFixedRate(
            monitor::checkForConflicts,
            CONFLICT_CHECK_INTERVAL_MS,
            CONFLICT_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        monitor.setMonitorTask(monitorTask);
    }
    
    private void updateHeartbeat(LockSession session) {
        try {
            LockInfo currentInfo = readLockInfo(session.getLockFile());
            LockInfo updatedInfo = new LockInfo(
                currentInfo.processId(),
                currentInfo.createdAt(),
                Instant.now(), // Update heartbeat
                currentInfo.readOnly(),
                currentInfo.username(),
                currentInfo.hostname()
            );
            
            writeLockInfo(session.getLockFile(), updatedInfo);
            session.setLockInfo(updatedInfo);
            
        } catch (Exception e) {
            logger.error("Failed to update heartbeat for: " + session.getLockFile(), e);
        }
    }
    
    private boolean isLockValid(LockInfo lockInfo) {
        Instant now = Instant.now();
        long timeSinceHeartbeat = ChronoUnit.MILLIS.between(lockInfo.lastHeartbeat(), now);
        return timeSinceHeartbeat < LOCK_TIMEOUT_MS;
    }
    
    private LockInfo readLockInfo(Path lockFile) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(lockFile)) {
            props.load(is);
        }
        
        return new LockInfo(
            props.getProperty("processId"),
            Instant.parse(props.getProperty("createdAt")),
            Instant.parse(props.getProperty("lastHeartbeat")),
            Boolean.parseBoolean(props.getProperty("readOnly", "false")),
            props.getProperty("username", "unknown"),
            props.getProperty("hostname", "unknown")
        );
    }
    
    private void writeLockInfo(Path lockFile, LockInfo lockInfo) throws IOException {
        Properties props = new Properties();
        props.setProperty("processId", lockInfo.processId());
        props.setProperty("createdAt", lockInfo.createdAt().toString());
        props.setProperty("lastHeartbeat", lockInfo.lastHeartbeat().toString());
        props.setProperty("readOnly", String.valueOf(lockInfo.readOnly()));
        props.setProperty("username", lockInfo.username());
        props.setProperty("hostname", lockInfo.hostname());
        
        try (OutputStream os = Files.newOutputStream(lockFile)) {
            props.store(os, "Odyssey World Lock File");
        }
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Shuts down the lock detection system.
     */
    public void shutdown() {
        logger.info("Shutting down lock conflict detection system");
        
        // Release all active locks
        for (Path worldPath : new ArrayList<>(activeLocks.keySet())) {
            releaseLock(worldPath);
        }
        
        // Stop all conflict monitors
        for (ConflictMonitor monitor : conflictMonitors.values()) {
            monitor.stop();
        }
        conflictMonitors.clear();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Inner classes and records
    
    public enum LockStatus {
        UNLOCKED, LOCKED, STALE, ALREADY_LOCKED, ACQUIRED, ERROR
    }
    
    public record LockOptions(
        boolean readOnly,
        boolean forceAcquire,
        boolean monitorConflicts
    ) {
        public static LockOptions defaultOptions() {
            return new LockOptions(false, false, true);
        }
        
        public static LockOptions readOnly() {
            return new LockOptions(true, false, true);
        }
        
        public static LockOptions forceAcquire() {
            return new LockOptions(false, true, true);
        }
    }
    
    public record LockInfo(
        String processId,
        Instant createdAt,
        Instant lastHeartbeat,
        boolean readOnly,
        String username,
        String hostname
    ) {}
    
    public record LockResult(
        boolean success,
        LockStatus status,
        String message
    ) {}
    
    public record LockedWorld(
        Path worldPath,
        LockInfo lockInfo,
        LockStatus status
    ) {}
    
    private static class LockSession {
        private final Path lockFile;
        private LockInfo lockInfo;
        private final LockOptions options;
        private ScheduledFuture<?> heartbeatTask;
        
        public LockSession(Path lockFile, LockInfo lockInfo, LockOptions options) {
            this.lockFile = lockFile;
            this.lockInfo = lockInfo;
            this.options = options;
        }
        
        // Getters and setters
        public Path getLockFile() { return lockFile; }
        public LockInfo getLockInfo() { return lockInfo; }
        public void setLockInfo(LockInfo lockInfo) { this.lockInfo = lockInfo; }
        public LockOptions getOptions() { return options; }
        public ScheduledFuture<?> getHeartbeatTask() { return heartbeatTask; }
        public void setHeartbeatTask(ScheduledFuture<?> heartbeatTask) { this.heartbeatTask = heartbeatTask; }
    }
    
    private static class ConflictMonitor {
        private final Path worldPath;
        private final LockSession session;
        private ScheduledFuture<?> monitorTask;
        
        public ConflictMonitor(Path worldPath, LockSession session) {
            this.worldPath = worldPath;
            this.session = session;
        }
        
        public void checkForConflicts() {
            // Check for file system changes that might indicate conflicts
            // This is a simplified implementation - in practice, you might want
            // to monitor file modification times, check for backup files, etc.
            
            Path lockFile = worldPath.resolve(LOCK_FILE_NAME);
            if (!Files.exists(lockFile)) {
                logger.warn("Lock file disappeared for world: {}", worldPath);
                return;
            }
            
            try {
                LockInfo currentLock = readLockInfo(lockFile);
                if (!currentLock.processId().equals(session.getLockInfo().processId())) {
                    logger.error("Lock conflict detected! Another process has acquired the lock: {}", worldPath);
                }
            } catch (Exception e) {
                logger.error("Failed to check for conflicts in world: " + worldPath, e);
            }
        }
        
        public void stop() {
            if (monitorTask != null) {
                monitorTask.cancel(false);
            }
        }
        
        public void setMonitorTask(ScheduledFuture<?> monitorTask) {
            this.monitorTask = monitorTask;
        }
        
        private LockInfo readLockInfo(Path lockFile) throws IOException {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(lockFile)) {
                props.load(is);
            }
            
            return new LockInfo(
                props.getProperty("processId"),
                Instant.parse(props.getProperty("createdAt")),
                Instant.parse(props.getProperty("lastHeartbeat")),
                Boolean.parseBoolean(props.getProperty("readOnly", "false")),
                props.getProperty("username", "unknown"),
                props.getProperty("hostname", "unknown")
            );
        }
    }
}