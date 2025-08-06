package com.odyssey.core.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A lock-free hash map implementation using atomic references and compare-and-swap operations.
 * This implementation provides high-performance concurrent access for read-heavy workloads
 * with occasional writes, making it ideal for caching and lookup tables.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Lock-free implementation using CAS operations</li>
 *   <li>Optimistic concurrency control</li>
 *   <li>Automatic resizing when load factor exceeds threshold</li>
 *   <li>Memory-efficient bucket design</li>
 * </ul>
 * 
 * @param <K> the type of keys
 * @param <V> the type of values
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class LockFreeHashMap<K, V> {
    
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final double DEFAULT_LOAD_FACTOR = 0.75;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    
    private volatile AtomicReference<Node<K, V>>[] table;
    private final AtomicInteger size = new AtomicInteger(0);
    private final double loadFactor;
    private volatile int threshold;
    
    /**
     * Creates a new lock-free hash map with default initial capacity and load factor.
     */
    public LockFreeHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a new lock-free hash map with the specified initial capacity.
     * 
     * @param initialCapacity the initial capacity
     */
    public LockFreeHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Creates a new lock-free hash map with the specified initial capacity and load factor.
     * 
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     */
    @SuppressWarnings("unchecked")
    public LockFreeHashMap(int initialCapacity, double loadFactor) {
        if (initialCapacity < 2) {
            throw new IllegalArgumentException("Initial capacity must be at least 2");
        }
        if (loadFactor <= 0 || Double.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive");
        }
        
        // Validate that initial capacity is a power of 2
        if ((initialCapacity & (initialCapacity - 1)) != 0) {
            throw new IllegalArgumentException("Initial capacity must be a power of 2");
        }
        
        this.loadFactor = loadFactor;
        
        int capacity = initialCapacity;
        
        this.table = new AtomicReference[capacity];
        for (int i = 0; i < capacity; i++) {
            this.table[i] = new AtomicReference<>();
        }
        
        this.threshold = (int) (capacity * loadFactor);
    }
    
    /**
     * Associates the specified value with the specified key.
     * 
     * @param key the key
     * @param value the value
     * @return the previous value associated with the key, or null if none
     */
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        return putInternal(key, value, false);
    }
    
    /**
     * Associates the specified value with the specified key only if the key is not already present.
     * 
     * @param key the key
     * @param value the value
     * @return the previous value associated with the key, or null if none
     */
    public V putIfAbsent(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        return putInternal(key, value, true);
    }
    
    private V putInternal(K key, V value, boolean onlyIfAbsent) {
        int hash = hash(key);
        
        while (true) {
            AtomicReference<Node<K, V>>[] currentTable = table;
            int index = hash & (currentTable.length - 1);
            AtomicReference<Node<K, V>> bucket = currentTable[index];
            
            Node<K, V> head = bucket.get();
            
            // Search for existing key in the chain
            Node<K, V> current = head;
            Node<K, V> prev = null;
            while (current != null) {
                if (current.hash == hash && key.equals(current.key)) {
                    // Key found, update value if allowed
                    if (onlyIfAbsent) {
                        return current.value;
                    }
                    
                    // Create new node with updated value to ensure atomicity
                    V oldValue = current.value;
                    Node<K, V> newNode = new Node<>(hash, key, value, current.next);
                    
                    if (prev == null) {
                        // Updating head node
                        if (bucket.compareAndSet(head, newNode)) {
                            return oldValue;
                        } else {
                            // CAS failed, retry from beginning
                            break;
                        }
                    } else {
                        // This is more complex for middle nodes, so let's rebuild the chain
                        Node<K, V> newHead = rebuildChainWithUpdate(head, hash, key, value);
                        if (bucket.compareAndSet(head, newHead)) {
                            return oldValue;
                        } else {
                            // CAS failed, retry from beginning
                            break;
                        }
                    }
                }
                prev = current;
                current = current.next;
            }
            
            // Key not found, add new node
            Node<K, V> newNode = new Node<>(hash, key, value, head);
            
            if (bucket.compareAndSet(head, newNode)) {
                int currentSize = size.incrementAndGet();
                
                // Check if resize is needed
                if (currentSize > threshold && currentTable.length < MAXIMUM_CAPACITY) {
                    resize();
                }
                
                return null;
            }
            
            // CAS failed, retry
        }
    }
    
    /**
     * Returns the value associated with the specified key.
     * 
     * @param key the key
     * @return the value, or null if not found
     */
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        int hash = hash(key);
        AtomicReference<Node<K, V>>[] currentTable = table;
        int index = hash & (currentTable.length - 1);
        
        Node<K, V> current = currentTable[index].get();
        while (current != null) {
            if (current.hash == hash && key.equals(current.key)) {
                return current.value;
            }
            current = current.next;
        }
        
        return null;
    }
    
    /**
     * Removes the mapping for the specified key.
     * 
     * @param key the key
     * @return the previous value, or null if not found
     */
    public V remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        int hash = hash(key);
        
        while (true) {
            AtomicReference<Node<K, V>>[] currentTable = table;
            int index = hash & (currentTable.length - 1);
            AtomicReference<Node<K, V>> bucket = currentTable[index];
            
            Node<K, V> head = bucket.get();
            Node<K, V> newHead = removeFromChain(head, hash, key);
            
            if (newHead == head) {
                // Key not found
                return null;
            }
            
            if (bucket.compareAndSet(head, newHead)) {
                size.decrementAndGet();
                
                // Find the removed value
                Node<K, V> current = head;
                while (current != null) {
                    if (current.hash == hash && key.equals(current.key)) {
                        return current.value;
                    }
                    current = current.next;
                }
                
                return null; // Should not reach here
            }
            
            // CAS failed, retry
        }
    }
    
    private Node<K, V> removeFromChain(Node<K, V> head, int hash, K key) {
        if (head == null) {
            return null;
        }
        
        if (head.hash == hash && key.equals(head.key)) {
            return head.next;
        }
        
        Node<K, V> newNext = removeFromChain(head.next, hash, key);
        if (newNext == head.next) {
            return head; // No change
        }
        
        return new Node<>(head.hash, head.key, head.value, newNext);
    }
    
    private Node<K, V> rebuildChainWithUpdate(Node<K, V> head, int hash, K key, V newValue) {
        if (head == null) {
            return null;
        }
        
        if (head.hash == hash && key.equals(head.key)) {
            return new Node<>(hash, key, newValue, head.next);
        }
        
        Node<K, V> newNext = rebuildChainWithUpdate(head.next, hash, key, newValue);
        return new Node<>(head.hash, head.key, head.value, newNext);
    }
    
    /**
     * Returns the number of key-value mappings.
     * 
     * @return the size
     */
    public int size() {
        return size.get();
    }
    
    /**
     * Returns whether the map is empty.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }
    
    /**
     * Returns whether the map contains the specified key.
     * 
     * @param key the key
     * @return true if the key is present
     */
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return get(key) != null;
    }
    
    /**
     * Computes a value for the specified key using the given function if the key is not present.
     * 
     * @param key the key
     * @param mappingFunction the function to compute the value
     * @return the current or computed value
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V value = get(key);
        if (value != null) {
            return value;
        }
        
        V newValue = mappingFunction.apply(key);
        if (newValue != null) {
            V existing = putIfAbsent(key, newValue);
            return existing != null ? existing : newValue;
        }
        
        return null;
    }
    
    /**
     * Removes all mappings from this map.
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        AtomicReference<Node<K, V>>[] currentTable = table;
        
        // Clear all buckets
        for (int i = 0; i < currentTable.length; i++) {
            currentTable[i].set(null);
        }
        
        // Reset size
        size.set(0);
    }
    
    @SuppressWarnings("unchecked")
    private synchronized void resize() {
        AtomicReference<Node<K, V>>[] oldTable = table;
        int oldCapacity = oldTable.length;
        
        // Double-check if resize is still needed after acquiring lock
        if (size.get() <= threshold || oldCapacity >= MAXIMUM_CAPACITY) {
            return;
        }
        
        int newCapacity = oldCapacity << 1;
        AtomicReference<Node<K, V>>[] newTable = new AtomicReference[newCapacity];
        
        for (int i = 0; i < newCapacity; i++) {
            newTable[i] = new AtomicReference<>();
        }
        
        // Rehash all nodes
        for (int i = 0; i < oldCapacity; i++) {
            Node<K, V> head = oldTable[i].get();
            while (head != null) {
                Node<K, V> next = head.next;
                int newIndex = head.hash & (newCapacity - 1);
                
                Node<K, V> newHead = newTable[newIndex].get();
                Node<K, V> newNode = new Node<>(head.hash, head.key, head.value, newHead);
                
                while (!newTable[newIndex].compareAndSet(newHead, newNode)) {
                    newHead = newTable[newIndex].get();
                    newNode = new Node<>(head.hash, head.key, head.value, newHead);
                }
                
                head = next;
            }
        }
        
        table = newTable;
        threshold = (int) (newCapacity * loadFactor);
    }
    
    private int hash(K key) {
        int h = key.hashCode();
        // Apply additional hashing to reduce collisions
        h ^= (h >>> 16);
        return h;
    }
    
    /**
     * Node class for the hash chain.
     */
    private static class Node<K, V> {
        final int hash;
        final K key;
        volatile V value;
        final Node<K, V> next;
        
        Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
}