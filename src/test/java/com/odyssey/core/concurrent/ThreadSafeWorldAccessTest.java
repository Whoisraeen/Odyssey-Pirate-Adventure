package com.odyssey.core.concurrent;

import com.odyssey.world.World;
import com.odyssey.world.chunk.ChunkManager;
import com.odyssey.world.chunk.ChunkPosition;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.biome.BiomeManager;
import com.odyssey.world.generation.WorldGenerator.BlockType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for ThreadSafeWorldAccess.
 * Tests thread-safe world operations and concurrent access patterns.
 */
class ThreadSafeWorldAccessTest {
    
    @Mock
    private World world;
    
    @Mock
    private ChunkManager chunkManager;
    
    @Mock
    private WorldGenerator worldGenerator;
    
    @Mock
    private BiomeManager biomeManager;
    
    private ThreadSafeWorldAccess worldAccess;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock behavior
        when(world.getChunkManager()).thenReturn(chunkManager);
        when(world.getWorldGenerator()).thenReturn(worldGenerator);
        when(world.getBiomeManager()).thenReturn(biomeManager);
        
        worldAccess = new ThreadSafeWorldAccess(world);
    }
    
    @Test
    void testConstructorValidation() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadSafeWorldAccess(null));
    }
    
    @Test
    void testInitialState() {
        assertNotNull(worldAccess.getPlayerPosition());
        assertEquals(0.0f, worldAccess.getPlayerPosition().x, 0.001f);
        assertEquals(0.0f, worldAccess.getPlayerPosition().y, 0.001f);
        assertEquals(0.0f, worldAccess.getPlayerPosition().z, 0.001f);
    }
    
    @Test
    void testPlayerPositionUpdate() {
        ThreadSafeWorldAccess.Vector3f newPosition = new ThreadSafeWorldAccess.Vector3f(10.5f, 20.0f, 30.5f);

        worldAccess.updatePlayerPosition(newPosition);

        ThreadSafeWorldAccess.Vector3f retrieved = worldAccess.getPlayerPosition();
        assertEquals(10.5f, retrieved.x, 0.001f);
        assertEquals(20.0f, retrieved.y, 0.001f);
        assertEquals(30.5f, retrieved.z, 0.001f);
    }

    @Test
    void testGetPlayerPositionConsistency() throws Exception {
        // Access the internal atomic reference to compare against returned values
        Field field = ThreadSafeWorldAccess.class.getDeclaredField("playerPosition");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<ThreadSafeWorldAccess.Vector3f> internal =
            (AtomicReference<ThreadSafeWorldAccess.Vector3f>) field.get(worldAccess);

        Thread updater = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                worldAccess.updatePlayerPosition(new ThreadSafeWorldAccess.Vector3f(i, i + 1, i + 2));
            }
        });
        updater.start();

        boolean mismatchFound = false;
        for (int i = 0; i < 10000 && !mismatchFound; i++) {
            ThreadSafeWorldAccess.Vector3f observed = worldAccess.getPlayerPosition();
            ThreadSafeWorldAccess.Vector3f actual = internal.get();
            if (!observed.equals(actual)) {
                mismatchFound = true;
            }
        }
        updater.join();
        assertFalse(mismatchFound, "Observed inconsistent player position");
    }
    
    @Test
    void testPlayerPositionNullRejection() {
        assertThrows(IllegalArgumentException.class, () -> worldAccess.updatePlayerPosition(null));
    }
    
    @Test
    void testGetBlockWithCache() {
        int x = 10, y = 20, z = 30;
        BlockType expectedBlock = BlockType.STONE;
        
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt()))
            .thenReturn(expectedBlock);
        
        // First call should hit the chunk manager
        BlockType result1 = worldAccess.getBlock(x, y, z);
        assertEquals(expectedBlock, result1);
        verify(chunkManager, times(1)).getBlock(anyInt(), anyInt(), anyInt());
        
        // Second call should hit the cache
        BlockType result2 = worldAccess.getBlock(x, y, z);
        assertEquals(expectedBlock, result2);
        verify(chunkManager, times(1)).getBlock(anyInt(), anyInt(), anyInt());
    }
    
    @Test
    void testSetBlockWithOptimisticConcurrency() {
        int x = 10, y = 20, z = 30;
        BlockType oldBlock = BlockType.DIRT;
        BlockType newBlock = BlockType.STONE;
        
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt()))
            .thenReturn(oldBlock);
        when(chunkManager.setBlock(anyInt(), anyInt(), anyInt(), eq(newBlock)))
            .thenReturn(true);
        
        boolean result = worldAccess.setBlock(x, y, z, newBlock);
        assertTrue(result);
        
        verify(chunkManager).setBlock(anyInt(), anyInt(), anyInt(), eq(newBlock));
    }
    
    @Test
    void testSetBlockNullRejection() {
        assertThrows(IllegalArgumentException.class, () -> worldAccess.setBlock(0, 0, 0, null));
    }
    
    @Test
    void testIsChunkLoaded() {
        ChunkPosition chunkPos = new ChunkPosition(1, 2);
        
        when(chunkManager.isChunkLoaded(1, 2)).thenReturn(true);
        
        assertTrue(worldAccess.isChunkLoaded(chunkPos));
        verify(chunkManager).isChunkLoaded(1, 2);
    }
    
    @Test
    void testIsChunkLoadedNullRejection() {
        assertThrows(IllegalArgumentException.class, () -> worldAccess.isChunkLoaded(null));
    }
    
    @Test
    void testTriggerChunkLoad() {
        ChunkPosition chunkPos = new ChunkPosition(1, 2);
        
        when(chunkManager.isChunkLoaded(1, 2)).thenReturn(false);
        when(chunkManager.loadChunk(1, 2)).thenReturn(mock(com.odyssey.world.chunk.Chunk.class));
        
        assertTrue(worldAccess.triggerChunkLoad(chunkPos));
        verify(chunkManager).loadChunk(1, 2);
    }
    
    @Test
    void testTriggerChunkLoadNullRejection() {
        assertThrows(IllegalArgumentException.class, () -> worldAccess.triggerChunkLoad(null));
    }
    
    @Test
    void testProcessPendingOperations() {
        // Add some pending operations
        worldAccess.setBlock(10, 20, 30, BlockType.STONE);
        worldAccess.setBlock(15, 25, 35, BlockType.DIRT);
        
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt()))
            .thenReturn(BlockType.AIR);
        when(chunkManager.setBlock(anyInt(), anyInt(), anyInt(), any(BlockType.class)))
            .thenReturn(true);
        
        int processed = worldAccess.processPendingOperations(10);
        assertTrue(processed >= 0);
    }
    
    @Test
    void testClearCache() {
        int x = 10, y = 20, z = 30;
        BlockType block = BlockType.STONE;
        
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt()))
            .thenReturn(block);
        
        // Populate cache
        worldAccess.getBlock(x, y, z);
        verify(chunkManager, times(1)).getBlock(anyInt(), anyInt(), anyInt());
        
        // Clear cache
        worldAccess.clearCache();
        
        // Next call should hit chunk manager again
        worldAccess.getBlock(x, y, z);
        verify(chunkManager, times(2)).getBlock(anyInt(), anyInt(), anyInt());
    }
    
    @Test
    @Timeout(10)
    void testConcurrentPlayerPositionUpdates() throws InterruptedException {
        final int threadCount = 8;
        final int updatesPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < updatesPerThread; i++) {
                        ThreadSafeWorldAccess.Vector3f position = new ThreadSafeWorldAccess.Vector3f(
                            threadId * 100 + i,
                            threadId * 200 + i,
                            threadId * 300 + i
                        );
                        worldAccess.updatePlayerPosition(position);
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        // Position should be valid
        ThreadSafeWorldAccess.Vector3f finalPosition = worldAccess.getPlayerPosition();
        assertNotNull(finalPosition);
    }
    
    @Test
    @Timeout(10)
    void testConcurrentBlockAccess() throws InterruptedException {
        final int threadCount = 4;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        final AtomicInteger getCount = new AtomicInteger(0);
        final AtomicInteger setCount = new AtomicInteger(0);
        
        when(chunkManager.getBlock(anyInt(), anyInt(), anyInt()))
            .thenReturn(BlockType.DIRT);
        when(chunkManager.setBlock(anyInt(), anyInt(), anyInt(), any(BlockType.class)))
            .thenReturn(true);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        int x = threadId * 10 + i;
                        int y = 50;
                        int z = threadId * 10 + i;
                        
                        if (i % 2 == 0) {
                            worldAccess.getBlock(x, y, z);
                            getCount.incrementAndGet();
                        } else {
                            worldAccess.setBlock(x, y, z, BlockType.STONE);
                            setCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
        
        assertTrue(getCount.get() > 0);
        assertTrue(setCount.get() > 0);
    }
    
    @Test
    @Timeout(10)
    void testConcurrentChunkOperations() throws InterruptedException {
        final int threadCount = 4;
        final int operationsPerThread = 50;
        final CountDownLatch startLatch = new CountDownLatch(threadCount);
        final CountDownLatch endLatch = new CountDownLatch(threadCount);
        final AtomicReference<Exception> error = new AtomicReference<>();
        
        when(chunkManager.isChunkLoaded(anyInt(), anyInt())).thenReturn(false, true);
        when(chunkManager.loadChunk(anyInt(), anyInt())).thenReturn(mock(com.odyssey.world.chunk.Chunk.class));
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    
                    for (int i = 0; i < operationsPerThread; i++) {
                        ChunkPosition chunkPos = new ChunkPosition(threadId + i, threadId - i);
                        
                        worldAccess.isChunkLoaded(chunkPos);
                        worldAccess.triggerChunkLoad(chunkPos);
                    }
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (error.get() != null) {
            throw new RuntimeException(error.get());
        }
    }
    
    @Test
    void testVector3fEquality() {
        ThreadSafeWorldAccess.Vector3f v1 = new ThreadSafeWorldAccess.Vector3f(1.0f, 2.0f, 3.0f);
        ThreadSafeWorldAccess.Vector3f v2 = new ThreadSafeWorldAccess.Vector3f(1.0f, 2.0f, 3.0f);
        ThreadSafeWorldAccess.Vector3f v3 = new ThreadSafeWorldAccess.Vector3f(1.0f, 2.0f, 4.0f);
        
        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertEquals(v1.hashCode(), v2.hashCode());
        assertNotEquals(v1.hashCode(), v3.hashCode());
    }
    
    @Test
    void testVector3fToString() {
        ThreadSafeWorldAccess.Vector3f vector = new ThreadSafeWorldAccess.Vector3f(1.5f, 2.5f, 3.5f);
        String str = vector.toString();
        assertTrue(str.contains("1.5"));
        assertTrue(str.contains("2.5"));
        assertTrue(str.contains("3.5"));
    }
    
    @Test
    void testBlockOperationEquality() {
        ThreadSafeWorldAccess.BlockOperation op1 = new ThreadSafeWorldAccess.BlockOperation(
            new ThreadSafeWorldAccess.Vector3i(1, 2, 3), BlockType.STONE
        );
        ThreadSafeWorldAccess.BlockOperation op2 = new ThreadSafeWorldAccess.BlockOperation(
            new ThreadSafeWorldAccess.Vector3i(1, 2, 3), BlockType.STONE
        );
        ThreadSafeWorldAccess.BlockOperation op3 = new ThreadSafeWorldAccess.BlockOperation(
            new ThreadSafeWorldAccess.Vector3i(1, 2, 4), BlockType.STONE
        );
        
        assertEquals(op1, op2);
        assertNotEquals(op1, op3);
    }
}