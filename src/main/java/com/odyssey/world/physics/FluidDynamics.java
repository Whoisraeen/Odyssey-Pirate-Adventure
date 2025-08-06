package com.odyssey.world.physics;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Handles fluid dynamics simulation including flow, pressure, and viscosity.
 * 
 * <p>This system simulates realistic fluid behavior with:
 * <ul>
 *   <li>Pressure-based flow calculations</li>
 *   <li>Viscosity and surface tension effects</li>
 *   <li>Multi-fluid interactions (water, lava, etc.)</li>
 *   <li>Evaporation and condensation</li>
 *   <li>Fluid-solid interactions</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class FluidDynamics {
    private static final Logger logger = LoggerFactory.getLogger(FluidDynamics.class);
    
    /** Gravity acceleration constant. */
    private static final float GRAVITY = 9.81f;
    
    /** Minimum fluid level to consider for flow. */
    private static final float MIN_FLUID_LEVEL = 0.001f;
    
    /** Maximum flow rate per update. */
    private static final float MAX_FLOW_RATE = 0.1f;
    
    /** Viscosity values for different fluid types. */
    private static final Map<BlockType, Float> FLUID_VISCOSITY = Map.of(
        BlockType.WATER, 0.001f,
        BlockType.LAVA, 0.1f
    );
    
    /** Density values for different fluid types. */
    private static final Map<BlockType, Float> FLUID_DENSITY = Map.of(
        BlockType.WATER, 1000.0f,
        BlockType.LAVA, 3000.0f
    );
    
    /** Surface tension values for different fluid types. */
    private static final Map<BlockType, Float> SURFACE_TENSION = Map.of(
        BlockType.WATER, 0.072f,
        BlockType.LAVA, 0.4f
    );
    
    /** Evaporation rates for different fluid types (per second at 20°C). */
    private static final Map<BlockType, Float> EVAPORATION_RATE = Map.of(
        BlockType.WATER, 0.0001f,
        BlockType.LAVA, 0.0f
    );
    
    private final PhysicsRegion region;
    private final Queue<FluidUpdate> pendingUpdates = new ConcurrentLinkedQueue<>();
    private final Map<Vector3i, FluidCell> fluidCells = new ConcurrentHashMap<>();
    
    /**
     * Represents a fluid update operation.
     */
    private static class FluidUpdate {
        final Vector3i position;
        final BlockType fluidType;
        final float amount;
        final Vector3f velocity;
        
        FluidUpdate(Vector3i position, BlockType fluidType, float amount, Vector3f velocity) {
            this.position = new Vector3i(position);
            this.fluidType = fluidType;
            this.amount = amount;
            this.velocity = new Vector3f(velocity);
        }
    }
    
    /**
     * Represents a fluid cell with flow properties.
     */
    private static class FluidCell {
        float level = 0.0f;
        BlockType type = BlockType.AIR;
        Vector3f velocity = new Vector3f();
        float pressure = 0.0f;
        float temperature = 20.0f;
        long lastUpdate = System.currentTimeMillis();
        
        void reset() {
            level = 0.0f;
            type = BlockType.AIR;
            velocity.set(0, 0, 0);
            pressure = 0.0f;
        }
    }
    
    /**
     * Creates a new fluid dynamics system.
     * 
     * @param region the physics region to operate on
     */
    public FluidDynamics(PhysicsRegion region) {
        this.region = region;
    }
    
    /**
     * Updates fluid dynamics for all blocks in the region.
     * 
     * @param deltaTime the time elapsed since last update
     */
    public void update(double deltaTime) {
        // Process pending fluid updates
        processPendingUpdates();
        
        // Update fluid cells
        updateFluidCells(deltaTime);
        
        // Calculate pressure and flow
        calculatePressure();
        calculateFlow(deltaTime);
        
        // Apply environmental effects
        applyEvaporation(deltaTime);
        applyCondensation(deltaTime);
        
        // Clean up inactive cells
        cleanupInactiveCells();
    }
    
    /**
     * Processes pending fluid updates from external sources.
     */
    private void processPendingUpdates() {
        FluidUpdate update;
        while ((update = pendingUpdates.poll()) != null) {
            FluidCell cell = getOrCreateFluidCell(update.position);
            
            if (cell.type == BlockType.AIR || cell.type == update.fluidType) {
                cell.type = update.fluidType;
                cell.level = Math.min(1.0f, cell.level + update.amount);
                cell.velocity.add(update.velocity);
                cell.lastUpdate = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Updates individual fluid cells.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void updateFluidCells(double deltaTime) {
        for (Map.Entry<Vector3i, FluidCell> entry : fluidCells.entrySet()) {
            Vector3i pos = entry.getKey();
            FluidCell cell = entry.getValue();
            
            if (cell.level < MIN_FLUID_LEVEL) {
                continue;
            }
            
            // Update velocity with gravity
            cell.velocity.y -= GRAVITY * (float) deltaTime;
            
            // Apply viscosity damping
            float viscosity = FLUID_VISCOSITY.getOrDefault(cell.type, 0.001f);
            float dampingFactor = 1.0f - (viscosity * 100.0f * (float) deltaTime);
            cell.velocity.mul(Math.max(0.1f, dampingFactor));
            
            // Update temperature based on environment
            updateFluidTemperature(pos, cell, deltaTime);
            
            // Update physics block if it exists
            PhysicsBlock block = region.getBlock(pos);
            if (block != null) {
                block.setFluidLevel(cell.level);
                block.setFluidVelocity(cell.velocity);
                block.setTemperature(cell.temperature);
            }
        }
    }
    
    /**
     * Calculates pressure for all fluid cells.
     */
    private void calculatePressure() {
        for (Map.Entry<Vector3i, FluidCell> entry : fluidCells.entrySet()) {
            Vector3i pos = entry.getKey();
            FluidCell cell = entry.getValue();
            
            if (cell.level < MIN_FLUID_LEVEL) {
                continue;
            }
            
            // Calculate hydrostatic pressure
            float density = FLUID_DENSITY.getOrDefault(cell.type, 1000.0f);
            cell.pressure = calculateHydrostaticPressure(pos, cell, density);
            
            // Add dynamic pressure from velocity
            float dynamicPressure = 0.5f * density * cell.velocity.lengthSquared();
            cell.pressure += dynamicPressure;
        }
    }
    
    /**
     * Calculates hydrostatic pressure at a position.
     * 
     * @param pos the position
     * @param cell the fluid cell
     * @param density the fluid density
     * @return the hydrostatic pressure
     */
    private float calculateHydrostaticPressure(Vector3i pos, FluidCell cell, float density) {
        float pressure = 0.0f;
        
        // Calculate pressure from fluid column above
        for (int y = pos.y + 1; y < pos.y + 10; y++) {
            Vector3i abovePos = new Vector3i(pos.x, y, pos.z);
            FluidCell aboveCell = fluidCells.get(abovePos);
            
            if (aboveCell == null || aboveCell.level < MIN_FLUID_LEVEL) {
                break;
            }
            
            if (aboveCell.type == cell.type) {
                pressure += GRAVITY * density * aboveCell.level;
            }
        }
        
        return pressure;
    }
    
    /**
     * Calculates fluid flow between adjacent cells.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void calculateFlow(double deltaTime) {
        List<FlowOperation> flowOps = new ArrayList<>();
        
        for (Map.Entry<Vector3i, FluidCell> entry : fluidCells.entrySet()) {
            Vector3i pos = entry.getKey();
            FluidCell cell = entry.getValue();
            
            if (cell.level < MIN_FLUID_LEVEL) {
                continue;
            }
            
            // Check flow to adjacent cells
            for (Vector3i direction : getFlowDirections()) {
                Vector3i neighborPos = new Vector3i(pos).add(direction);
                FluidCell neighbor = getOrCreateFluidCell(neighborPos);
                
                float flowRate = calculateFlowRate(cell, neighbor, direction, deltaTime);
                if (flowRate > 0.0f) {
                    flowOps.add(new FlowOperation(pos, neighborPos, flowRate, cell.type));
                }
            }
        }
        
        // Apply flow operations
        applyFlowOperations(flowOps);
    }
    
    /**
     * Calculates flow rate between two cells.
     * 
     * @param source the source cell
     * @param target the target cell
     * @param direction the flow direction
     * @param deltaTime the time elapsed
     * @return the flow rate
     */
    private float calculateFlowRate(FluidCell source, FluidCell target, Vector3i direction, double deltaTime) {
        // Can't flow if target is full or incompatible
        if (target.level >= 1.0f || (target.type != BlockType.AIR && target.type != source.type)) {
            return 0.0f;
        }
        
        // Calculate pressure difference
        float pressureDiff = source.pressure - target.pressure;
        
        // Add gravitational component for vertical flow
        if (direction.y < 0) {
            pressureDiff += GRAVITY * FLUID_DENSITY.getOrDefault(source.type, 1000.0f);
        }
        
        if (pressureDiff <= 0.0f) {
            return 0.0f;
        }
        
        // Calculate flow rate based on pressure difference and viscosity
        float viscosity = FLUID_VISCOSITY.getOrDefault(source.type, 0.001f);
        float flowRate = (pressureDiff / viscosity) * (float) deltaTime;
        
        // Limit flow rate
        flowRate = Math.min(flowRate, MAX_FLOW_RATE);
        flowRate = Math.min(flowRate, source.level);
        flowRate = Math.min(flowRate, 1.0f - target.level);
        
        return Math.max(0.0f, flowRate);
    }
    
    /**
     * Gets the flow directions (6 cardinal directions).
     * 
     * @return array of flow directions
     */
    private Vector3i[] getFlowDirections() {
        return new Vector3i[] {
            new Vector3i(0, -1, 0),  // Down (priority)
            new Vector3i(1, 0, 0),   // East
            new Vector3i(-1, 0, 0),  // West
            new Vector3i(0, 0, 1),   // North
            new Vector3i(0, 0, -1),  // South
            new Vector3i(0, 1, 0)    // Up
        };
    }
    
    /**
     * Represents a flow operation between two cells.
     */
    private static class FlowOperation {
        final Vector3i source;
        final Vector3i target;
        final float amount;
        final BlockType fluidType;
        
        FlowOperation(Vector3i source, Vector3i target, float amount, BlockType fluidType) {
            this.source = new Vector3i(source);
            this.target = new Vector3i(target);
            this.amount = amount;
            this.fluidType = fluidType;
        }
    }
    
    /**
     * Applies flow operations to move fluid between cells.
     * 
     * @param flowOps the flow operations to apply
     */
    private void applyFlowOperations(List<FlowOperation> flowOps) {
        for (FlowOperation op : flowOps) {
            FluidCell source = fluidCells.get(op.source);
            FluidCell target = getOrCreateFluidCell(op.target);
            
            if (source == null || source.level < op.amount) {
                continue;
            }
            
            // Transfer fluid
            source.level -= op.amount;
            target.level += op.amount;
            target.type = op.fluidType;
            
            // Transfer momentum
            Vector3f momentum = new Vector3f(source.velocity).mul(op.amount);
            target.velocity.add(momentum);
            
            // Update timestamps
            target.lastUpdate = System.currentTimeMillis();
            
            // Clean up source if empty
            if (source.level < MIN_FLUID_LEVEL) {
                source.reset();
            }
        }
    }
    
    /**
     * Applies evaporation effects to fluids.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void applyEvaporation(double deltaTime) {
        for (FluidCell cell : fluidCells.values()) {
            if (cell.level < MIN_FLUID_LEVEL) {
                continue;
            }
            
            float evapRate = EVAPORATION_RATE.getOrDefault(cell.type, 0.0f);
            if (evapRate > 0.0f) {
                // Temperature affects evaporation rate
                float tempFactor = Math.max(0.1f, (cell.temperature - 20.0f) / 80.0f + 1.0f);
                float evaporation = evapRate * tempFactor * (float) deltaTime;
                
                cell.level = Math.max(0.0f, cell.level - evaporation);
                
                if (cell.level < MIN_FLUID_LEVEL) {
                    cell.reset();
                }
            }
        }
    }
    
    /**
     * Applies condensation effects.
     * 
     * @param deltaTime the time elapsed since last update
     */
    private void applyCondensation(double deltaTime) {
        // Simple condensation model - could be expanded
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (Map.Entry<Vector3i, FluidCell> entry : fluidCells.entrySet()) {
            Vector3i pos = entry.getKey();
            FluidCell cell = entry.getValue();
            
            // Check for condensation conditions
            if (cell.temperature < 15.0f && random.nextFloat() < 0.001f) {
                // Small chance of water condensation in cold areas
                if (cell.type == BlockType.AIR) {
                    cell.type = BlockType.WATER;
                    cell.level = 0.01f;
                    cell.lastUpdate = System.currentTimeMillis();
                }
            }
        }
    }
    
    /**
     * Updates fluid temperature based on environment.
     * 
     * @param pos the position
     * @param cell the fluid cell
     * @param deltaTime the time elapsed
     */
    private void updateFluidTemperature(Vector3i pos, FluidCell cell, double deltaTime) {
        // Get ambient temperature from physics block
        PhysicsBlock block = region.getBlock(pos);
        float ambientTemp = block != null ? block.getTemperature() : 20.0f;
        
        // Thermal equilibrium
        float tempDiff = ambientTemp - cell.temperature;
        float thermalRate = 0.1f * (float) deltaTime;
        cell.temperature += tempDiff * thermalRate;
        
        // Special cases for lava
        if (cell.type == BlockType.LAVA) {
            cell.temperature = Math.max(cell.temperature, 1000.0f);
        }
    }
    
    /**
     * Cleans up inactive fluid cells to save memory.
     */
    private void cleanupInactiveCells() {
        long currentTime = System.currentTimeMillis();
        fluidCells.entrySet().removeIf(entry -> {
            FluidCell cell = entry.getValue();
            return cell.level < MIN_FLUID_LEVEL && 
                   (currentTime - cell.lastUpdate) > 30000; // 30 seconds
        });
    }
    
    /**
     * Gets or creates a fluid cell at the specified position.
     * 
     * @param pos the position
     * @return the fluid cell
     */
    private FluidCell getOrCreateFluidCell(Vector3i pos) {
        return fluidCells.computeIfAbsent(pos, k -> new FluidCell());
    }
    
    /**
     * Adds fluid at the specified position.
     * 
     * @param position the position
     * @param fluidType the fluid type
     * @param amount the amount to add
     * @param velocity the initial velocity
     */
    public void addFluid(Vector3i position, BlockType fluidType, float amount, Vector3f velocity) {
        pendingUpdates.offer(new FluidUpdate(position, fluidType, amount, velocity));
    }
    
    /**
     * Removes fluid from the specified position.
     * 
     * @param position the position
     * @param amount the amount to remove
     * @return the actual amount removed
     */
    public float removeFluid(Vector3i position, float amount) {
        FluidCell cell = fluidCells.get(position);
        if (cell == null || cell.level < MIN_FLUID_LEVEL) {
            return 0.0f;
        }
        
        float removed = Math.min(cell.level, amount);
        cell.level -= removed;
        
        if (cell.level < MIN_FLUID_LEVEL) {
            cell.reset();
        }
        
        return removed;
    }
    
    /**
     * Gets the fluid level at the specified position.
     * 
     * @param position the position
     * @return the fluid level
     */
    public float getFluidLevel(Vector3i position) {
        FluidCell cell = fluidCells.get(position);
        return cell != null ? cell.level : 0.0f;
    }
    
    /**
     * Gets the fluid type at the specified position.
     * 
     * @param position the position
     * @return the fluid type
     */
    public BlockType getFluidType(Vector3i position) {
        FluidCell cell = fluidCells.get(position);
        return cell != null ? cell.type : BlockType.AIR;
    }
    
    /**
     * Gets the fluid velocity at the specified position.
     * 
     * @param position the position
     * @return the fluid velocity
     */
    public Vector3f getFluidVelocity(Vector3i position) {
        FluidCell cell = fluidCells.get(position);
        return cell != null ? new Vector3f(cell.velocity) : new Vector3f();
    }
    
    /**
     * Gets the number of active fluid cells.
     * 
     * @return the number of active fluid cells
     */
    public int getActiveCellCount() {
        return (int) fluidCells.values().stream()
                .filter(cell -> cell.level >= MIN_FLUID_LEVEL)
                .count();
    }
}