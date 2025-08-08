package com.odyssey.world.save.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Write-ahead journal for crash recovery and transaction logging.
 * Records all world modifications before they are applied to ensure
 * data consistency and enable recovery from crashes.
 * 
 * <p>Journal format:
 * <pre>
 * Header (32 bytes):
 *   - Magic number (8 bytes): "ODYSSEYJ"
 *   - Version (4 bytes): journal format version
 *   - Sequence number (8 bytes): current sequence number
 *   - Checksum (8 bytes): header checksum
 *   - Reserved (4 bytes): for future use
 * 
 * Entry format:
 *   - Sequence number (8 bytes): unique entry identifier
 *   - Timestamp (8 bytes): entry creation time
 *   - Operation type (4 bytes): type of operation
 *   - Data length (4 bytes): length of operation data
 *   - Data: operation-specific data
 *   - Checksum (8 bytes): entry checksum
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WriteAheadJournal {
    private static final Logger logger = LoggerFactory.getLogger(WriteAheadJournal.class);
    
    /** Magic number for journal files. */
    private static final long MAGIC_NUMBER = 0x4F44595353455946L; // "ODYSSEYJ"
    
    /** Current journal format version. */
    private static final int FORMAT_VERSION = 1;
    
    /** Size of the journal header. */
    private static final int HEADER_SIZE = 32;
    
    /** Maximum journal file size before rotation (100MB). */
    private static final long MAX_JOURNAL_SIZE = 100 * 1024 * 1024;
    
    /** Maximum number of journal files to keep. */
    private static final int MAX_JOURNAL_FILES = 10;
    
    private final Path journalDir;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Current journal file
    private Path currentJournalFile;
    private RandomAccessFile journalFile;
    private long sequenceNumber = 0;
    private long currentFileSize = 0;
    
    // Recovery state
    private boolean recoveryMode = false;
    private List<JournalEntry> pendingEntries = new ArrayList<>();
    
    /**
     * Creates a new write-ahead journal.
     * 
     * @param worldSaveDir the world save directory
     * @throws IOException if the journal cannot be initialized
     */
    public WriteAheadJournal(Path worldSaveDir) throws IOException {
        this.journalDir = worldSaveDir.resolve("journal");
        
        // Create journal directory
        Files.createDirectories(journalDir);
        
        // Initialize or recover journal
        initializeJournal();
        
        logger.info("Initialized write-ahead journal at: {}", journalDir);
    }
    
    /**
     * Initializes the journal system.
     * 
     * @throws IOException if initialization fails
     */
    private void initializeJournal() throws IOException {
        lock.writeLock().lock();
        try {
            // Find the latest journal file
            currentJournalFile = findLatestJournalFile();
            
            if (currentJournalFile == null) {
                // Create new journal file
                createNewJournalFile();
            } else {
                // Open existing journal file
                openJournalFile();
                
                // Check for incomplete transactions
                checkForIncompleteTransactions();
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Finds the latest journal file in the directory.
     * 
     * @return the latest journal file, or null if none exist
     * @throws IOException if directory listing fails
     */
    private Path findLatestJournalFile() throws IOException {
        Path latestFile = null;
        long latestSequence = -1;
        
        try (var stream = Files.list(journalDir)) {
            for (Path file : stream.toList()) {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith("journal-") && fileName.endsWith(".wal")) {
                    try {
                        String sequenceStr = fileName.substring(8, fileName.length() - 4);
                        long sequence = Long.parseLong(sequenceStr);
                        if (sequence > latestSequence) {
                            latestSequence = sequence;
                            latestFile = file;
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid journal file name: {}", fileName);
                    }
                }
            }
        }
        
        return latestFile;
    }
    
    /**
     * Creates a new journal file.
     * 
     * @throws IOException if file creation fails
     */
    private void createNewJournalFile() throws IOException {
        long fileSequence = System.currentTimeMillis();
        currentJournalFile = journalDir.resolve("journal-" + fileSequence + ".wal");
        
        journalFile = new RandomAccessFile(currentJournalFile.toFile(), "rw");
        currentFileSize = 0;
        sequenceNumber = 0;
        
        // Write header
        writeHeader();
        
        logger.debug("Created new journal file: {}", currentJournalFile);
    }
    
    /**
     * Opens an existing journal file.
     * 
     * @throws IOException if file opening fails
     */
    private void openJournalFile() throws IOException {
        journalFile = new RandomAccessFile(currentJournalFile.toFile(), "rw");
        currentFileSize = journalFile.length();
        
        // Read and validate header
        readHeader();
        
        // Position at end of file for appending
        journalFile.seek(currentFileSize);
        
        logger.debug("Opened existing journal file: {}", currentJournalFile);
    }
    
    /**
     * Writes the journal header.
     * 
     * @throws IOException if writing fails
     */
    private void writeHeader() throws IOException {
        journalFile.seek(0);
        
        // Write magic number and version
        journalFile.writeLong(MAGIC_NUMBER);
        journalFile.writeInt(FORMAT_VERSION);
        
        // Write sequence number
        journalFile.writeLong(sequenceNumber);
        
        // Write checksum (simplified - just use sequence number)
        journalFile.writeLong(sequenceNumber);
        
        // Write reserved bytes
        journalFile.writeInt(0);
        
        currentFileSize = HEADER_SIZE;
    }
    
    /**
     * Reads and validates the journal header.
     * 
     * @throws IOException if reading fails or header is invalid
     */
    private void readHeader() throws IOException {
        if (currentFileSize < HEADER_SIZE) {
            throw new IOException("Journal file too small: " + currentFileSize);
        }
        
        journalFile.seek(0);
        
        // Read and validate magic number
        long magic = journalFile.readLong();
        if (magic != MAGIC_NUMBER) {
            throw new IOException("Invalid journal magic number: " + Long.toHexString(magic));
        }
        
        // Read and validate version
        int version = journalFile.readInt();
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported journal version: " + version);
        }
        
        // Read sequence number
        sequenceNumber = journalFile.readLong();
        
        // Read and validate checksum
        long checksum = journalFile.readLong();
        if (checksum != sequenceNumber) {
            logger.warn("Journal header checksum mismatch");
        }
        
        // Skip reserved bytes
        journalFile.readInt();
    }
    
    /**
     * Checks for incomplete transactions and prepares recovery.
     * 
     * @throws IOException if checking fails
     */
    private void checkForIncompleteTransactions() throws IOException {
        pendingEntries.clear();
        
        // Scan journal entries from the end
        long position = HEADER_SIZE;
        journalFile.seek(position);
        
        while (position < currentFileSize) {
            try {
                JournalEntry entry = readEntry();
                if (entry != null) {
                    pendingEntries.add(entry);
                    position = journalFile.getFilePointer();
                    
                    // Update sequence number
                    if (entry.sequenceNumber > sequenceNumber) {
                        sequenceNumber = entry.sequenceNumber;
                    }
                } else {
                    break; // End of valid entries
                }
            } catch (IOException e) {
                logger.warn("Corrupted journal entry at position {}", position, e);
                break;
            }
        }
        
        if (!pendingEntries.isEmpty()) {
            logger.info("Found {} pending journal entries for recovery", pendingEntries.size());
            recoveryMode = true;
        }
    }
    
    /**
     * Logs a journal entry for a world operation.
     * 
     * @param operation the operation type
     * @param data the operation data
     * @throws IOException if logging fails
     */
    public void logOperation(JournalOperation operation, byte[] data) throws IOException {
        lock.writeLock().lock();
        try {
            // Check if we need to rotate the journal file
            if (currentFileSize > MAX_JOURNAL_SIZE) {
                rotateJournalFile();
            }
            
            // Create journal entry
            JournalEntry entry = new JournalEntry(
                ++sequenceNumber,
                System.currentTimeMillis(),
                operation,
                data
            );
            
            // Write entry to journal
            writeEntry(entry);
            
            // Force to disk for durability
            journalFile.getFD().sync();
            
            logger.debug("Logged journal entry: {} (seq={})", operation, sequenceNumber);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Writes a journal entry to the file.
     * 
     * @param entry the entry to write
     * @throws IOException if writing fails
     */
    private void writeEntry(JournalEntry entry) throws IOException {
        long startPosition = journalFile.getFilePointer();
        
        // Write entry header
        journalFile.writeLong(entry.sequenceNumber);
        journalFile.writeLong(entry.timestamp);
        journalFile.writeInt(entry.operation.ordinal());
        journalFile.writeInt(entry.data.length);
        
        // Write entry data
        journalFile.write(entry.data);
        
        // Write checksum (simplified - just use sequence number)
        journalFile.writeLong(entry.sequenceNumber);
        
        // Update file size
        currentFileSize = journalFile.getFilePointer();
        
        // Update header with new sequence number
        long currentPosition = journalFile.getFilePointer();
        journalFile.seek(12); // Position of sequence number in header
        journalFile.writeLong(sequenceNumber);
        journalFile.writeLong(sequenceNumber); // Update checksum too
        journalFile.seek(currentPosition);
    }
    
    /**
     * Reads a journal entry from the current file position.
     * 
     * @return the journal entry, or null if end of file
     * @throws IOException if reading fails
     */
    private JournalEntry readEntry() throws IOException {
        if (journalFile.getFilePointer() >= currentFileSize) {
            return null;
        }
        
        try {
            // Read entry header
            long sequenceNum = journalFile.readLong();
            long timestamp = journalFile.readLong();
            int operationOrdinal = journalFile.readInt();
            int dataLength = journalFile.readInt();
            
            // Validate data length
            if (dataLength < 0 || dataLength > 10 * 1024 * 1024) { // Max 10MB per entry
                throw new IOException("Invalid data length: " + dataLength);
            }
            
            // Read entry data
            byte[] data = new byte[dataLength];
            journalFile.readFully(data);
            
            // Read and validate checksum
            long checksum = journalFile.readLong();
            if (checksum != sequenceNum) {
                throw new IOException("Entry checksum mismatch");
            }
            
            // Create entry
            JournalOperation operation = JournalOperation.values()[operationOrdinal];
            return new JournalEntry(sequenceNum, timestamp, operation, data);
            
        } catch (EOFException e) {
            return null; // End of file
        }
    }
    
    /**
     * Rotates to a new journal file.
     * 
     * @throws IOException if rotation fails
     */
    private void rotateJournalFile() throws IOException {
        // Close current file
        if (journalFile != null) {
            journalFile.close();
        }
        
        // Create new journal file
        createNewJournalFile();
        
        // Clean up old journal files
        cleanupOldJournalFiles();
        
        logger.info("Rotated to new journal file: {}", currentJournalFile);
    }
    
    /**
     * Cleans up old journal files, keeping only the most recent ones.
     * 
     * @throws IOException if cleanup fails
     */
    private void cleanupOldJournalFiles() throws IOException {
        List<Path> journalFiles = new ArrayList<>();
        
        try (var stream = Files.list(journalDir)) {
            for (Path file : stream.toList()) {
                String fileName = file.getFileName().toString();
                if (fileName.startsWith("journal-") && fileName.endsWith(".wal")) {
                    journalFiles.add(file);
                }
            }
        }
        
        // Sort by modification time (newest first)
        journalFiles.sort((a, b) -> {
            try {
                return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
            } catch (IOException e) {
                return 0;
            }
        });
        
        // Delete old files beyond the limit
        for (int i = MAX_JOURNAL_FILES; i < journalFiles.size(); i++) {
            Path oldFile = journalFiles.get(i);
            try {
                Files.delete(oldFile);
                logger.debug("Deleted old journal file: {}", oldFile);
            } catch (IOException e) {
                logger.warn("Failed to delete old journal file: {}", oldFile, e);
            }
        }
    }
    
    /**
     * Gets pending journal entries for recovery.
     * 
     * @return the list of pending entries
     */
    public List<JournalEntry> getPendingEntries() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(pendingEntries);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Marks recovery as complete and clears pending entries.
     */
    public void markRecoveryComplete() {
        lock.writeLock().lock();
        try {
            pendingEntries.clear();
            recoveryMode = false;
            logger.info("Journal recovery completed");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if the journal is in recovery mode.
     * 
     * @return true if in recovery mode
     */
    public boolean isInRecoveryMode() {
        return recoveryMode;
    }
    
    /**
     * Flushes any pending writes to disk.
     * 
     * @throws IOException if flushing fails
     */
    public void flush() throws IOException {
        lock.readLock().lock();
        try {
            if (journalFile != null) {
                journalFile.getFD().sync();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Closes the journal and releases resources.
     * 
     * @throws IOException if closing fails
     */
    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            if (journalFile != null) {
                journalFile.close();
                journalFile = null;
            }
            
            logger.info("Closed write-ahead journal");
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current sequence number.
     * 
     * @return the sequence number
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * Gets the current journal file size.
     * 
     * @return the file size in bytes
     */
    public long getCurrentFileSize() {
        return currentFileSize;
    }
    
    /**
     * Represents a journal entry.
     */
    public static class JournalEntry {
        public final long sequenceNumber;
        public final long timestamp;
        public final JournalOperation operation;
        public final byte[] data;
        
        public JournalEntry(long sequenceNumber, long timestamp, JournalOperation operation, byte[] data) {
            this.sequenceNumber = sequenceNumber;
            this.timestamp = timestamp;
            this.operation = operation;
            this.data = data;
        }
        
        @Override
        public String toString() {
            return String.format("JournalEntry{seq=%d, op=%s, size=%d}", 
                sequenceNumber, operation, data.length);
        }
    }
    
    /**
     * Types of journal operations.
     */
    public enum JournalOperation {
        CHUNK_SAVE,
        CHUNK_UNLOAD,
        PLAYER_SAVE,
        METADATA_SAVE,
        REGION_CREATE,
        REGION_CLOSE,
        BACKUP_START,
        BACKUP_COMPLETE,
        WORLD_SAVE_START,
        WORLD_SAVE_COMPLETE
    }
}