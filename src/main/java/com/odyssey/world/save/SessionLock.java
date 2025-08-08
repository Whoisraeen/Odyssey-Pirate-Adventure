package com.odyssey.world.save;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages session locking for world saves to prevent concurrent access.
 * Implements PID tracking and heartbeat system for crash detection and recovery.
 * 
 * <p>Lock file format:
 * <pre>
 * Line 1: Process ID (PID)
 * Line 2: Host name
 * Line 3: Start timestamp (milliseconds)
 * Line 4: Last heartbeat timestamp (milliseconds)
 * Line 5: Session UUID
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class SessionLock {
    private static final Logger logger = LoggerFactory.getLogger(SessionLock.class);
    
    private static final String LOCK_FILE_NAME = "session.lock";
    private static final long HEARTBEAT_INTERVAL_MS = 5000; // 5 seconds
    private static final long STALE_LOCK_TIMEOUT_MS = 30000; // 30 seconds
    
    private final Path lockFile;
    private final String processId;
    private final String hostName;
    private final String sessionUuid;
    private final long startTime;
    
    private ScheduledExecutorService heartbeatExecutor;
    private volatile boolean lockAcquired = false;
    
    /**
     * Creates a new session lock for the specified world directory.
     * 
     * @param worldDirectory the world directory
     */
    public SessionLock(Path worldDirectory) {
        this.lockFile = worldDirectory.resolve(LOCK_FILE_NAME);
        this.processId = getProcessId();
        this.hostName = getHostName();
        this.sessionUuid = java.util.UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        
        logger.debug("Created session lock for: {} (PID: {}, Host: {})", 
                    worldDirectory, processId, hostName);
    }
    
    /**
     * Attempts to acquire the session lock.
     * 
     * @throws IOException if the lock cannot be acquired or an I/O error occurs
     */
    public synchronized void acquire() throws IOException {
        if (lockAcquired) {
            return;
        }
        
        // Check for existing lock
        if (Files.exists(lockFile)) {
            if (isLockStale()) {
                logger.warn("Removing stale lock file: {}", lockFile);
                Files.deleteIfExists(lockFile);
            } else {
                LockInfo existingLock = readLockFile();
                throw new IOException(String.format(
                    "World is already in use by another process (PID: %s, Host: %s, Session: %s)",
                    existingLock.processId, existingLock.hostName, existingLock.sessionUuid));
            }
        }
        
        // Create lock file
        writeLockFile();
        
        // Start heartbeat
        startHeartbeat();
        
        lockAcquired = true;
        logger.info("Acquired session lock: {}", lockFile);
    }
    
    /**
     * Releases the session lock.
     */
    public synchronized void release() {
        if (!lockAcquired) {
            return;
        }
        
        // Stop heartbeat
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Remove lock file
        try {
            Files.deleteIfExists(lockFile);
            logger.info("Released session lock: {}", lockFile);
        } catch (IOException e) {
            logger.error("Failed to remove lock file: {}", lockFile, e);
        }
        
        lockAcquired = false;
    }
    
    /**
     * Checks if the current lock file is stale (process no longer running).
     * 
     * @return true if the lock is stale
     */
    private boolean isLockStale() {
        try {
            LockInfo lockInfo = readLockFile();
            
            // Check if heartbeat is too old
            long timeSinceHeartbeat = System.currentTimeMillis() - lockInfo.lastHeartbeat;
            if (timeSinceHeartbeat > STALE_LOCK_TIMEOUT_MS) {
                logger.debug("Lock is stale - heartbeat timeout: {} ms", timeSinceHeartbeat);
                return true;
            }
            
            // Check if process is still running (platform-specific)
            if (!isProcessRunning(lockInfo.processId)) {
                logger.debug("Lock is stale - process not running: {}", lockInfo.processId);
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            logger.warn("Failed to read lock file, assuming stale: {}", lockFile, e);
            return true;
        }
    }
    
    /**
     * Checks if a process with the given PID is still running.
     * 
     * @param pid the process ID
     * @return true if the process is running
     */
    private boolean isProcessRunning(String pid) {
        try {
            // Try to parse PID
            long processId = Long.parseLong(pid);
            
            // Use ProcessHandle API (Java 9+) if available
            return ProcessHandle.of(processId).isPresent();
            
        } catch (NumberFormatException e) {
            logger.warn("Invalid PID format: {}", pid);
            return false;
        } catch (Exception e) {
            // Fallback - assume process is running if we can't determine
            logger.debug("Cannot determine if process is running: {}", pid, e);
            return true;
        }
    }
    
    /**
     * Writes the lock file with current session information.
     * 
     * @throws IOException if the lock file cannot be written
     */
    private void writeLockFile() throws IOException {
        long currentTime = System.currentTimeMillis();
        
        String lockContent = String.format("%s%n%s%n%d%n%d%n%s%n",
            processId, hostName, startTime, currentTime, sessionUuid);
        
        Files.write(lockFile, lockContent.getBytes(), 
                   StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    /**
     * Updates the heartbeat timestamp in the lock file.
     */
    private void updateHeartbeat() {
        if (!lockAcquired) {
            return;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            
            String lockContent = String.format("%s%n%s%n%d%n%d%n%s%n",
                processId, hostName, startTime, currentTime, sessionUuid);
            
            Files.write(lockFile, lockContent.getBytes(), 
                       StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            
        } catch (IOException e) {
            logger.error("Failed to update heartbeat in lock file: {}", lockFile, e);
        }
    }
    
    /**
     * Reads the lock file and returns the lock information.
     * 
     * @return the lock information
     * @throws IOException if the lock file cannot be read
     */
    private LockInfo readLockFile() throws IOException {
        if (!Files.exists(lockFile)) {
            throw new IOException("Lock file does not exist: " + lockFile);
        }
        
        try (BufferedReader reader = Files.newBufferedReader(lockFile)) {
            String processId = reader.readLine();
            String hostName = reader.readLine();
            String startTimeStr = reader.readLine();
            String lastHeartbeatStr = reader.readLine();
            String sessionUuid = reader.readLine();
            
            if (processId == null || hostName == null || startTimeStr == null || 
                lastHeartbeatStr == null || sessionUuid == null) {
                throw new IOException("Invalid lock file format: " + lockFile);
            }
            
            return new LockInfo(processId, hostName, 
                              Long.parseLong(startTimeStr), 
                              Long.parseLong(lastHeartbeatStr), 
                              sessionUuid);
        }
    }
    
    /**
     * Starts the heartbeat thread to keep the lock alive.
     */
    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionLock-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        
        heartbeatExecutor.scheduleAtFixedRate(this::updateHeartbeat, 
                                            HEARTBEAT_INTERVAL_MS, 
                                            HEARTBEAT_INTERVAL_MS, 
                                            TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gets the current process ID.
     * 
     * @return the process ID as a string
     */
    private String getProcessId() {
        try {
            // Try to get PID from RuntimeMXBean
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        } catch (Exception e) {
            // Fallback to thread ID
            return String.valueOf(Thread.currentThread().getId());
        }
    }
    
    /**
     * Gets the current host name.
     * 
     * @return the host name
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Returns true if the lock is currently acquired.
     * 
     * @return true if the lock is acquired
     */
    public boolean isLockAcquired() {
        return lockAcquired;
    }
    
    /**
     * Container for lock file information.
     */
    private static class LockInfo {
        final String processId;
        final String hostName;
        final long startTime;
        final long lastHeartbeat;
        final String sessionUuid;
        
        LockInfo(String processId, String hostName, long startTime, long lastHeartbeat, String sessionUuid) {
            this.processId = processId;
            this.hostName = hostName;
            this.startTime = startTime;
            this.lastHeartbeat = lastHeartbeat;
            this.sessionUuid = sessionUuid;
        }
    }
}