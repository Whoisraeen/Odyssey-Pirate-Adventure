package com.odyssey.world.physics;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles structural integrity simulation including support calculations,
 * collapse mechanics, and stress distribution.
 * 
 * <p>This system provides:
 * <ul>
 *   <li>Support chain analysis and propagation</li>
 *   <li>Stress distribution across structures</li>
 *   <li>Realistic collapse mechanics</li>
 *   <li>Material fatigue and wear simulation</li>
 *   <li>Foundation and anchor point detection</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class StructuralIntegrity {
    private static final Logger logger = LoggerFactory.getLogger(StructuralIntegrity.class);
    
    /** Maximum support distance for structural analysis. */
    private static final int MAX_SUPPORT_DISTANCE = 16;
    
    /** Minimum integrity threshold before collapse. */
    private static final float COLLAPSE_THRESHOLD = 0.1f;
    
    /** Maximum stress a block can handle before damage. */
    private static final float MAX_STRESS = 10.0f;
    
    /** Stress propagation factor to adjacent blocks. */
    private static final float STRESS_PROPAGATION = 0.7f;
    
    /** Foundation block types that provide infinite support. */
    private static final Set<BlockType> FOUNDATION_BLOCKS = Set.of(
        BlockType.OBSIDIAN,
        BlockType.STONE,
        BlockType.VOLCANIC_ROCK
    );
    
    private final PhysicsRegion region;
    private final Map<Vector3i, StructuralNode> structuralNodes = new ConcurrentHashMap<>();
    private final Queue<Vector3i> pendingAnalysis = new ConcurrentLinkedQueue<>();
    private final Queue<CollapseEvent> pendingCollapses = new ConcurrentLinkedQueue<>();
    
    /**
     * Represents a structural node in the support network.
     */
    private static class StructuralNode {
        final Vector3i position;
        float supportStrength = 1.0f;
        float currentStress = 0.0f;
        float maxStress = MAX_STRESS;
        boolean isFoundation = false;
        boolean isSupported = false;
        final Set<Vector3i> supportedBy = ConcurrentHashMap.newKeySet();
        final Set<Vector3i> supporting = ConcurrentHashMap.newKeySet();
        long lastAnalysis = 0;
        
        StructuralNode(Vector3i position) {
            this.position = new Vector3i(position);
        }
        
        void reset() {
            currentStress = 0.0f;
            isSupported = false;
            supportedBy.clear();
            supporting.clear();
        }
    }
    
    /**
     * Represents a collapse event.
     */
    private static class CollapseEvent {
        final Vector3i position;
        final float intensity;
        final String cause;
        
        CollapseEvent(Vector3i position, float intensity, String cause) {
            this.position = new Vector3i(position);
            this.intensity = intensity;
            this.cause = cause;
        }
    }
    
    /**
     * Creates a new structural integrity system.
     * 
     * @param region the physics region to operate on
     */
    public StructuralIntegrity(PhysicsRegion region) {
        this.region = region;
    }
    
    /**
     * Updates structural integrity analysis for all blocks.
     * 
     * @param deltaTime the time elapsed since last update
     */
    public void update(double deltaTime) {
        // Process pending analysis requests
        processPendingAnalysis();
        
        // Update structural nodes
        updateStructuralNodes(deltaTime);
        
        // Analyze support chains
        analyzeSupportChains();
        
        // Calculate stress distribution
        calculateStressDistribution();
        
        // Check for collapses
        checkForCollapses();
        
        // Process collapse events
        processCollapses();
        
        // Clean up inactive nodes
        cleanupInactiveNodes();
    }
    
    /**
     * Processes pending structural analysis requests.
     */
    private void processPendingAnalysis() {
        Vector3i position;
        while ((position = pendingAnalysis.poll()) != null) {
            analyzeStructuralNode(position);
        }
    }
    
    /**
     * Updates structural nodes with current block data.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateStructuralNodes(double deltaTime) {
        for (StructuralNode node : structuralNodes.values()) {
            PhysicsBlock block = region.getBlock(node.position);
            if (block == null) {
                continue;
            }
            
            // Update support strength based on block integrity
            node.supportStrength = block.getStructuralIntegrity() * getSupportMultiplier(block.getBlockType());
            
            // Update max stress based on material properties
            node.maxStress = calculateMaxStress(block);
            
            // Check if block is foundation
            node.isFoundation = isFoundationBlock(block.getBlockType()) || isGroundLevel(node.position);
            
            // Apply stress-induced damage
            if (node.currentStress > node.maxStress) {
                float damage = (node.currentStress - node.maxStress) * 0.01f * (float) deltaTime;
                block.applyDamage(damage, "structural_stress");
            }
            
            // Update block supporting state
            block.setSupporting(!node.supporting.isEmpty());
        }
    }
    
    /**
     * Analyzes support chains to determine which blocks are properly supported.
     */
    private void analyzeSupportChains() {
        // Reset support states
        for (StructuralNode node : structuralNodes.values()) {
            node.reset();
        }
        
        // Find all foundation blocks
        Set<Vector3i> foundations = new HashSet<>();
        for (StructuralNode node : structuralNodes.values()) {
            if (node.isFoundation) {
                foundations.add(node.position);
                node.isSupported = true;
            }
        }
        
        // Propagate support from foundations
        propagateSupport(foundations);
        
        // Build support relationships
        buildSupportRelationships();
    }
    
    /**
     * Propagates support from foundation blocks to connected structures.
     * 
     * @param foundations the set of foundation positions
     */
    private void propagateSupport(Set<Vector3i> foundations) {
        Queue<Vector3i> supportQueue = new LinkedList<>(foundations);
        Set<Vector3i> visited = new HashSet<>(foundations);
        
        while (!supportQueue.isEmpty()) {
            Vector3i current = supportQueue.poll();
            StructuralNode currentNode = structuralNodes.get(current);
            
            if (currentNode == null) {
                continue;
            }
            
            // Check adjacent blocks for support propagation
            for (Vector3i direction : getSupportDirections()) {
                Vector3i adjacent = new Vector3i(current).add(direction);
                
                if (visited.contains(adjacent)) {
                    continue;
                }
                
                StructuralNode adjacentNode = structuralNodes.get(adjacent);
                if (adjacentNode == null) {
                    continue;
                }
                
                PhysicsBlock adjacentBlock = region.getBlock(adjacent);
                if (adjacentBlock == null || !canSupport(adjacentBlock)) {
                    continue;
                }
                
                // Calculate support strength
                float supportDistance = calculateSupportDistance(current, adjacent);
                float supportStrength = currentNode.supportStrength * getSupportTransfer(direction);
                
                if (supportDistance <= MAX_SUPPORT_DISTANCE && supportStrength > 0.1f) {
                    adjacentNode.isSupported = true;
                    adjacentNode.supportedBy.add(current);
                    currentNode.supporting.add(adjacent);
                    
                    visited.add(adjacent);
                    supportQueue.offer(adjacent);
                }
            }
        }
    }
    
    /**
     * Builds support relationships between blocks.
     */
    private void buildSupportRelationships() {
        for (StructuralNode node : structuralNodes.values()) {
            if (!node.isSupported) {
                continue;
            }
            
            // Find blocks this node is supporting
            for (Vector3i direction : getSupportDirections()) {
                Vector3i supported = new Vector3i(node.position).add(direction);
                StructuralNode supportedNode = structuralNodes.get(supported);
                
                if (supportedNode != null && supportedNode.supportedBy.contains(node.position)) {
                    node.supporting.add(supported);
                }
            }
        }
    }
    
    /**
     * Calculates stress distribution across the structure.
     */
    private void calculateStressDistribution() {
        // Calculate load for each block
        Map<Vector3i, Float> loads = calculateBlockLoads();
        
        // Distribute stress through support chains
        for (Map.Entry<Vector3i, Float> entry : loads.entrySet()) {
            Vector3i position = entry.getKey();
            float load = entry.getValue();
            
            distributeStress(position, load);
        }
    }
    
    /**
     * Calculates the load (weight) for each block.
     * 
     * @return map of positions to loads
     */
    private Map<Vector3i, Float> calculateBlockLoads() {
        Map<Vector3i, Float> loads = new HashMap<>();
        
        for (StructuralNode node : structuralNodes.values()) {
            PhysicsBlock block = region.getBlock(node.position);
            if (block == null) {
                continue;
            }
            
            // Base load from block weight
            float load = calculateBlockWeight(block);
            
            // Add load from blocks above
            load += calculateOverheadLoad(node.position);
            
            loads.put(node.position, load);
        }
        
        return loads;
    }
    
    /**
     * Distributes stress from a loaded block through its support chain.
     * 
     * @param position the position of the loaded block
     * @param load the load to distribute
     */
    private void distributeStress(Vector3i position, float load) {
        StructuralNode node = structuralNodes.get(position);
        if (node == null || node.supportedBy.isEmpty()) {
            return;
        }
        
        // Distribute load among supporting blocks
        float stressPerSupport = load / node.supportedBy.size();
        
        for (Vector3i supportPos : node.supportedBy) {
            StructuralNode supportNode = structuralNodes.get(supportPos);
            if (supportNode != null) {
                supportNode.currentStress += stressPerSupport;
                
                // Propagate stress further down the chain
                if (!supportNode.isFoundation) {
                    distributeStress(supportPos, stressPerSupport * STRESS_PROPAGATION);
                }
            }
        }
    }
    
    /**
     * Checks for potential collapses based on structural integrity.
     */
    private void checkForCollapses() {
        for (StructuralNode node : structuralNodes.values()) {
            PhysicsBlock block = region.getBlock(node.position);
            if (block == null) {
                continue;
            }
            
            // Check for integrity failure
            if (block.getStructuralIntegrity() < COLLAPSE_THRESHOLD) {
                pendingCollapses.offer(new CollapseEvent(node.position, 1.0f, "integrity_failure"));
                continue;
            }
            
            // Check for stress failure
            if (node.currentStress > node.maxStress * 2.0f) {
                float intensity = Math.min(2.0f, node.currentStress / node.maxStress);
                pendingCollapses.offer(new CollapseEvent(node.position, intensity, "stress_failure"));
                continue;
            }
            
            // Check for unsupported blocks
            if (!node.isSupported && block.needsSupport()) {
                pendingCollapses.offer(new CollapseEvent(node.position, 0.5f, "lack_of_support"));
            }
        }
    }
    
    /**
     * Processes collapse events.
     */
    private void processCollapses() {
        CollapseEvent event;
        while ((event = pendingCollapses.poll()) != null) {
            processCollapse(event);
        }
    }
    
    /**
     * Processes a single collapse event.
     * 
     * @param event the collapse event
     */
    private void processCollapse(CollapseEvent event) {
        PhysicsBlock block = region.getBlock(event.position);
        if (block == null) {
            return;
        }
        
        logger.info("Block collapse at {} due to {} (intensity: {})", 
                   event.position, event.cause, event.intensity);
        
        // Apply damage to the block
        block.applyDamage(event.intensity, event.cause);
        
        // Remove structural node
        StructuralNode node = structuralNodes.remove(event.position);
        if (node != null) {
            // Trigger analysis of dependent blocks
            for (Vector3i dependent : node.supporting) {
                requestAnalysis(dependent);
            }
        }
        
        // Propagate collapse to nearby blocks if intense enough
        if (event.intensity > 1.5f) {
            propagateCollapse(event.position, event.intensity * 0.5f);
        }
        
        // TODO: Convert block to debris or remove it
        // This would integrate with the chunk system to actually modify the world
    }
    
    /**
     * Propagates collapse effects to nearby blocks.
     * 
     * @param origin the collapse origin
     * @param intensity the collapse intensity
     */
    private void propagateCollapse(Vector3i origin, float intensity) {
        if (intensity < 0.3f) {
            return;
        }
        
        for (Vector3i direction : getSupportDirections()) {
            Vector3i adjacent = new Vector3i(origin).add(direction);
            PhysicsBlock adjacentBlock = region.getBlock(adjacent);
            
            if (adjacentBlock != null) {
                // Apply damage based on distance and intensity
                float damage = intensity * 0.3f;
                adjacentBlock.applyDamage(damage, "collapse_propagation");
                
                // Small chance of chain collapse
                if (ThreadLocalRandom.current().nextFloat() < intensity * 0.1f) {
                    pendingCollapses.offer(new CollapseEvent(adjacent, intensity * 0.7f, "chain_collapse"));
                }
            }
        }
    }
    
    /**
     * Analyzes a structural node and updates its properties.
     * 
     * @param position the position to analyze
     */
    private void analyzeStructuralNode(Vector3i position) {
        PhysicsBlock block = region.getBlock(position);
        if (block == null) {
            structuralNodes.remove(position);
            return;
        }
        
        StructuralNode node = structuralNodes.computeIfAbsent(position, StructuralNode::new);
        node.lastAnalysis = System.currentTimeMillis();
        
        // Update node properties
        node.supportStrength = block.getStructuralIntegrity() * getSupportMultiplier(block.getBlockType());
        node.maxStress = calculateMaxStress(block);
        node.isFoundation = isFoundationBlock(block.getBlockType()) || isGroundLevel(position);
    }
    
    /**
     * Gets support directions (6 cardinal directions with priority).
     * 
     * @return array of support directions
     */
    private Vector3i[] getSupportDirections() {
        return new Vector3i[] {
            new Vector3i(0, -1, 0),  // Down (primary support)
            new Vector3i(0, 1, 0),   // Up
            new Vector3i(1, 0, 0),   // East
            new Vector3i(-1, 0, 0),  // West
            new Vector3i(0, 0, 1),   // North
            new Vector3i(0, 0, -1)   // South
        };
    }
    
    /**
     * Gets the support multiplier for a block type.
     * 
     * @param blockType the block type
     * @return the support multiplier
     */
    private float getSupportMultiplier(BlockType blockType) {
        return switch (blockType) {
            case STONE, OBSIDIAN -> 2.0f;
            case IRON_ORE, COAL_ORE -> 1.8f;
            case WOOD -> 1.5f;
            case SAND, DIRT -> 0.8f;
            case LEAVES -> 0.3f;
            case WATER, LAVA -> 0.0f;
            default -> 1.0f;
        };
    }
    
    /**
     * Gets the support transfer factor for a direction.
     * 
     * @param direction the support direction
     * @return the transfer factor
     */
    private float getSupportTransfer(Vector3i direction) {
        if (direction.y == -1) return 1.0f;  // Full support from below
        if (direction.y == 1) return 0.3f;   // Limited support from above
        return 0.7f;  // Lateral support
    }
    
    /**
     * Calculates the maximum stress a block can handle.
     * 
     * @param block the physics block
     * @return the maximum stress
     */
    private float calculateMaxStress(PhysicsBlock block) {
        float baseStress = MAX_STRESS * block.getHardness();
        float integrityFactor = block.getStructuralIntegrity();
        return baseStress * integrityFactor;
    }
    
    /**
     * Calculates the weight of a block.
     * 
     * @param block the physics block
     * @return the block weight
     */
    private float calculateBlockWeight(PhysicsBlock block) {
        return block.getDensity() * 9.81f; // Weight = density * gravity
    }
    
    /**
     * Calculates the overhead load from blocks above.
     * 
     * @param position the position
     * @return the overhead load
     */
    private float calculateOverheadLoad(Vector3i position) {
        float load = 0.0f;
        
        for (int y = position.y + 1; y < position.y + 10; y++) {
            Vector3i above = new Vector3i(position.x, y, position.z);
            PhysicsBlock aboveBlock = region.getBlock(above);
            
            if (aboveBlock == null) {
                break;
            }
            
            load += calculateBlockWeight(aboveBlock);
        }
        
        return load;
    }
    
    /**
     * Calculates the support distance between two positions.
     * 
     * @param from the source position
     * @param to the target position
     * @return the support distance
     */
    private float calculateSupportDistance(Vector3i from, Vector3i to) {
        return (float) from.distance(to);
    }
    
    /**
     * Checks if a block can provide structural support.
     * 
     * @param block the physics block
     * @return true if the block can provide support
     */
    private boolean canSupport(PhysicsBlock block) {
        return block.getSupportStrength() > 0.1f && 
               block.getStructuralIntegrity() > 0.1f &&
               block.getBlockType() != BlockType.AIR;
    }
    
    /**
     * Checks if a block type is a foundation block.
     * 
     * @param blockType the block type
     * @return true if it's a foundation block
     */
    private boolean isFoundationBlock(BlockType blockType) {
        return FOUNDATION_BLOCKS.contains(blockType);
    }
    
    /**
     * Checks if a position is at ground level.
     * 
     * @param position the position
     * @return true if at ground level
     */
    private boolean isGroundLevel(Vector3i position) {
        // Simple ground level check - could be improved with world data
        return position.y <= 64; // Assuming sea level is around 64
    }
    
    /**
     * Cleans up inactive structural nodes.
     */
    private void cleanupInactiveNodes() {
        long currentTime = System.currentTimeMillis();
        structuralNodes.entrySet().removeIf(entry -> {
            StructuralNode node = entry.getValue();
            PhysicsBlock block = region.getBlock(node.position);
            
            return block == null || 
                   (currentTime - node.lastAnalysis) > 60000; // 1 minute
        });
    }
    
    /**
     * Requests structural analysis for a position.
     * 
     * @param position the position to analyze
     */
    public void requestAnalysis(Vector3i position) {
        pendingAnalysis.offer(position);
    }
    
    /**
     * Gets the structural integrity at a position.
     * 
     * @param position the position
     * @return the structural integrity (0.0 to 1.0)
     */
    public float getStructuralIntegrity(Vector3i position) {
        StructuralNode node = structuralNodes.get(position);
        if (node == null) {
            return 1.0f;
        }
        
        PhysicsBlock block = region.getBlock(position);
        return block != null ? block.getStructuralIntegrity() : 0.0f;
    }
    
    /**
     * Gets the current stress at a position.
     * 
     * @param position the position
     * @return the current stress
     */
    public float getCurrentStress(Vector3i position) {
        StructuralNode node = structuralNodes.get(position);
        return node != null ? node.currentStress : 0.0f;
    }
    
    /**
     * Checks if a position is properly supported.
     * 
     * @param position the position
     * @return true if supported
     */
    public boolean isSupported(Vector3i position) {
        StructuralNode node = structuralNodes.get(position);
        return node != null && node.isSupported;
    }
    
    /**
     * Gets the number of active structural nodes.
     * 
     * @return the number of active nodes
     */
    public int getActiveNodeCount() {
        return structuralNodes.size();
    }
}