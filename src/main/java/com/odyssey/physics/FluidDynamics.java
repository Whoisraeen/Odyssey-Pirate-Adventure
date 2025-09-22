package com.odyssey.physics;

import com.odyssey.util.Logger;
import com.odyssey.world.World;
import com.odyssey.world.Block;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Advanced fluid dynamics simulation for water flow, pressure, and interactions.
 * Handles water flow between blocks, pressure calculations, and fluid-object interactions.
 */
public class FluidDynamics {
    
    private final World world;
    private final OceanPhysics oceanPhysics;
    private final Logger logger;
    
    // Fluid simulation grid
    private final ConcurrentMap<FluidCell, FluidData> fluidCells;
    
    // Simulation parameters
    private static final float FLUID_DENSITY = 1000.0f; // kg/m³ - water density
    private static final float VISCOSITY = 0.001f; // Pa·s - water viscosity
    private static final float SURFACE_TENSION = 0.0728f; // N/m - water surface tension (reserved for future use)
    private static final float GRAVITY = 9.81f; // m/s² - gravitational acceleration
    
    // Flow parameters
    private static final float MAX_FLOW_RATE = 1.0f;
    private static final float MIN_FLOW_THRESHOLD = 0.01f;
    private static final float PRESSURE_MULTIPLIER = 0.1f;
    
    // Grid resolution
    private static final float CELL_SIZE = 0.5f; // Size of each fluid simulation cell in world units
    private static final int CELLS_PER_BLOCK = (int) (1.0f / CELL_SIZE); // Number of cells per world block (reserved for future use)
    
    public FluidDynamics() {
        this.world = null;
        this.oceanPhysics = null;
        this.logger = Logger.getLogger(FluidDynamics.class);
        this.fluidCells = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the fluid dynamics system.
     * Sets up the fluid simulation grid and prepares for fluid calculations.
     */
    public void initialize() {
        logger.debug(Logger.WORLD, "FluidDynamics initialized");
    }
    
    /**
     * Clean up fluid dynamics resources.
     * Clears all fluid cells and releases memory.
     */
    public void cleanup() {
        fluidCells.clear();
        logger.debug(Logger.WORLD, "FluidDynamics cleaned up");
    }
    
    /**
     * Updates fluid simulation for the given time step.
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        float dt = (float) deltaTime;
        
        // Update fluid flow between cells
        updateFluidFlow(dt);
        
        // Update pressure calculations
        updatePressure(dt);
        
        // Update viscosity effects
        updateViscosity(dt);
        
        // Clean up empty cells to save memory
        cleanupEmptyCells();
    }
    
    /**
     * Updates fluid simulation (legacy method for backward compatibility).
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(float deltaTime) {
        update((double) deltaTime);
    }
    
    /**
     * Adds fluid at a specific world position.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param amount Amount of fluid to add (in density units)
     */
    public void addFluid(float worldX, float worldY, float worldZ, float amount) {
        FluidCell cell = getFluidCell(worldX, worldY, worldZ);
        FluidData data = fluidCells.computeIfAbsent(cell, _ -> new FluidData());
        
        data.density += amount;
        data.density = Math.min(data.density, FLUID_DENSITY);
    }
    
    /**
     * Removes fluid from a specific world position.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @param amount Amount of fluid to remove (in density units)
     */
    public void removeFluid(float worldX, float worldY, float worldZ, float amount) {
        FluidCell cell = getFluidCell(worldX, worldY, worldZ);
        FluidData data = fluidCells.get(cell);
        
        if (data != null) {
            data.density -= amount;
            data.density = Math.max(data.density, 0);
            
            if (data.density <= 0) {
                fluidCells.remove(cell);
            }
        }
    }
    
    /**
     * Gets fluid density at a specific world position.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Fluid density at the position (0 if no fluid present)
     */
    public float getFluidDensity(float worldX, float worldY, float worldZ) {
        FluidCell cell = getFluidCell(worldX, worldY, worldZ);
        FluidData data = fluidCells.get(cell);
        
        return data != null ? data.density : 0.0f;
    }
    
    /**
     * Gets fluid velocity at a specific world position.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Fluid velocity vector at the position
     */
    public Vector3f getFluidVelocity(float worldX, float worldY, float worldZ) {
        FluidCell cell = getFluidCell(worldX, worldY, worldZ);
        FluidData data = fluidCells.get(cell);
        
        if (data != null) {
            return new Vector3f(data.velocity);
        }
        
        // Fall back to ocean physics if no local fluid data
        return oceanPhysics.getWaterVelocity(worldX, worldY, worldZ);
    }
    
    /**
     * Gets fluid pressure at a specific world position.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Fluid pressure at the position (0 if no fluid present)
     */
    public float getFluidPressure(float worldX, float worldY, float worldZ) {
        FluidCell cell = getFluidCell(worldX, worldY, worldZ);
        FluidData data = fluidCells.get(cell);
        
        return data != null ? data.pressure : 0.0f;
    }
    
    /**
     * Calculates fluid force on an object at a given position.
     * Includes drag force, pressure force, and buoyancy force.
     * 
     * @param position Object position in world coordinates
     * @param velocity Object velocity vector
     * @param volume Object volume for force calculations
     * @param dragCoefficient Drag coefficient for the object
     * @return Total fluid force vector acting on the object
     */
    public Vector3f calculateFluidForce(Vector3f position, Vector3f velocity, float volume, float dragCoefficient) {
        Vector3f fluidVelocity = getFluidVelocity(position.x, position.y, position.z);
        Vector3f relativeVelocity = new Vector3f(velocity).sub(fluidVelocity);
        
        float fluidDensity = getFluidDensity(position.x, position.y, position.z);
        if (fluidDensity <= 0) {
            return new Vector3f(0, 0, 0);
        }
        
        // Drag force
        float speed = relativeVelocity.length();
        Vector3f dragForce = new Vector3f(0, 0, 0);
        
        if (speed > 0.001f) {
            float dragMagnitude = 0.5f * fluidDensity * speed * speed * dragCoefficient * volume;
            dragForce = new Vector3f(relativeVelocity).normalize().negate().mul(dragMagnitude);
        }
        
        // Pressure force
        Vector3f pressureGradient = calculatePressureGradient(position.x, position.y, position.z);
        Vector3f pressureForce = new Vector3f(pressureGradient).negate().mul(volume);
        
        // Buoyancy force
        Vector3f buoyancyForce = new Vector3f(0, fluidDensity * GRAVITY * volume, 0);
        
        return new Vector3f(dragForce).add(pressureForce).add(buoyancyForce);
    }
    
    /**
     * Simulates fluid splash at a specific world position.
     * Creates a spherical splash effect with particles and initial velocities.
     * 
     * @param worldX World X coordinate of splash center
     * @param worldY World Y coordinate of splash center
     * @param worldZ World Z coordinate of splash center
     * @param intensity Splash intensity (affects radius and particle velocities)
     */
    public void createSplash(float worldX, float worldY, float worldZ, float intensity) {
        // Create splash particles by adding fluid in a sphere around the impact point
        int radius = (int) Math.ceil(intensity * 2.0f);
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (distance <= radius) {
                        float amount = intensity * (1.0f - distance / radius);
                        addFluid(worldX + dx * CELL_SIZE, worldY + dy * CELL_SIZE, worldZ + dz * CELL_SIZE, amount);
                        
                        // Add initial velocity for splash effect
                        FluidCell cell = getFluidCell(worldX + dx * CELL_SIZE, worldY + dy * CELL_SIZE, worldZ + dz * CELL_SIZE);
                        FluidData data = fluidCells.get(cell);
                        if (data != null) {
                            data.velocity.x += (dx / distance) * intensity * 2.0f;
                            data.velocity.y += Math.abs(dy / distance) * intensity * 3.0f;
                            data.velocity.z += (dz / distance) * intensity * 2.0f;
                        }
                    }
                }
            }
        }
        
        logger.debug("Created splash at ({}, {}, {}) with intensity {}", worldX, worldY, worldZ, intensity);
    }
    
    /**
     * Checks if fluid can flow from one position to another.
     * Determines if the destination is blocked by solid blocks.
     * 
     * @param fromX Source X coordinate
     * @param fromY Source Y coordinate
     * @param fromZ Source Z coordinate
     * @param toX Destination X coordinate
     * @param toY Destination Y coordinate
     * @param toZ Destination Z coordinate
     * @return true if fluid can flow, false if blocked
     */
    public boolean canFluidFlow(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        // Check if destination is not blocked by solid blocks
        int blockX = (int) Math.floor(toX);
        int blockY = (int) Math.floor(toY);
        int blockZ = (int) Math.floor(toZ);
        
        Block.BlockType block = world.getBlock(blockX, blockY, blockZ);
        
        // Fluid can flow through air, water, and some transparent blocks
        return block == Block.BlockType.AIR || 
               block == Block.BlockType.WATER || 
               block == Block.BlockType.SEAWEED ||
               block == Block.BlockType.CORAL;
    }
    
    /**
     * Updates fluid flow between cells based on pressure differences and gravity.
     * This is the core fluid simulation method that handles fluid movement.
     * 
     * @param deltaTime Time step for the simulation
     */
    private void updateFluidFlow(float deltaTime) {
        ConcurrentMap<FluidCell, Vector3f> flowAccumulator = new ConcurrentHashMap<>();
        
        for (FluidCell cell : fluidCells.keySet()) {
            FluidData data = fluidCells.get(cell);
            if (data == null || data.density <= MIN_FLOW_THRESHOLD) {
                continue;
            }
            
            // Check flow to neighboring cells
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        
                        FluidCell neighbor = new FluidCell(
                            cell.x + dx, cell.y + dy, cell.z + dz
                        );
                        
                        float fromX = cell.x * CELL_SIZE;
                        float fromY = cell.y * CELL_SIZE;
                        float fromZ = cell.z * CELL_SIZE;
                        float toX = neighbor.x * CELL_SIZE;
                        float toY = neighbor.y * CELL_SIZE;
                        float toZ = neighbor.z * CELL_SIZE;
                        
                        if (!canFluidFlow(fromX, fromY, fromZ, toX, toY, toZ)) {
                            continue;
                        }
                        
                        FluidData neighborData = fluidCells.computeIfAbsent(neighbor, _ -> new FluidData());
                        
                        // Calculate pressure difference
                        float pressureDiff = data.pressure - neighborData.pressure;
                        
                        // Add gravity effect for vertical flow
                        if (dy < 0) {
                            pressureDiff += GRAVITY * data.density * CELL_SIZE;
                        }
                        
                        if (pressureDiff > 0) {
                            float flowRate = Math.min(MAX_FLOW_RATE * deltaTime, 
                                                    data.density * 0.1f) * pressureDiff * PRESSURE_MULTIPLIER;
                            
                            if (flowRate > MIN_FLOW_THRESHOLD) {
                                // Accumulate flow
                                Vector3f flow = new Vector3f(dx, dy, dz).normalize().mul(flowRate);
                                flowAccumulator.computeIfAbsent(cell, _ -> new Vector3f()).sub(flow);
                                flowAccumulator.computeIfAbsent(neighbor, _ -> new Vector3f()).add(flow);
                            }
                        }
                    }
                }
            }
        }
        
        // Apply accumulated flows
        for (FluidCell cell : flowAccumulator.keySet()) {
            Vector3f flow = flowAccumulator.get(cell);
            FluidData data = fluidCells.get(cell);
            if (data != null) {
                data.density += flow.length();
                data.velocity.add(flow.x, flow.y, flow.z);
            }
        }
    }
    
    /**
     * Updates pressure calculations for all fluid cells.
     * Pressure is calculated based on density and depth.
     * 
     * @param deltaTime Time step for the simulation
     */
    private void updatePressure(float deltaTime) {
        for (FluidCell cell : fluidCells.keySet()) {
            FluidData data = fluidCells.get(cell);
            if (data == null) continue;
            
            // Calculate pressure based on density and depth
            float depth = calculateDepthPressure(cell.y * CELL_SIZE);
            float densityPressure = data.density * 0.001f; // Convert density to pressure
            
            data.pressure = depth + densityPressure;
        }
    }
    
    /**
     * Updates viscosity effects on fluid velocities.
     * Applies damping to simulate fluid viscosity.
     * 
     * @param deltaTime Time step for the simulation
     */
    private void updateViscosity(float deltaTime) {
        for (FluidCell cell : fluidCells.keySet()) {
            FluidData data = fluidCells.get(cell);
            if (data == null) continue;
            
            // Apply viscosity damping
            float dampingFactor = 1.0f - (VISCOSITY * deltaTime * 10.0f);
            data.velocity.mul(Math.max(0.1f, dampingFactor));
        }
    }
    
    /**
     * Calculates pressure gradient at a world position using finite differences.
     * This is used to determine fluid flow direction and force calculations.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return Pressure gradient vector
     */
    private Vector3f calculatePressureGradient(float worldX, float worldY, float worldZ) {
        Vector3f gradient = new Vector3f();
        
        // Calculate gradient using finite differences
        float dx = CELL_SIZE;
        float pressureX1 = getFluidPressure(worldX + dx, worldY, worldZ);
        float pressureX2 = getFluidPressure(worldX - dx, worldY, worldZ);
        gradient.x = (pressureX1 - pressureX2) / (2.0f * dx);
        
        float pressureY1 = getFluidPressure(worldX, worldY + dx, worldZ);
        float pressureY2 = getFluidPressure(worldX, worldY - dx, worldZ);
        gradient.y = (pressureY1 - pressureY2) / (2.0f * dx);
        
        float pressureZ1 = getFluidPressure(worldX, worldY, worldZ + dx);
        float pressureZ2 = getFluidPressure(worldX, worldY, worldZ - dx);
        gradient.z = (pressureZ1 - pressureZ2) / (2.0f * dx);
        
        return gradient;
    }
    
    /**
     * Calculates depth-based pressure for fluid cells.
     * Pressure increases with depth below sea level.
     * 
     * @param worldY World Y coordinate (depth)
     * @return Pressure value based on depth
     */
    private float calculateDepthPressure(float worldY) {
        float seaLevel = oceanPhysics.getSeaLevel();
        float depth = Math.max(0, seaLevel - worldY);
        return FLUID_DENSITY * GRAVITY * depth * 0.0001f; // Scale down for simulation
    }
    
    /**
     * Gets fluid cell coordinate for a world position.
     * Converts world coordinates to discrete cell coordinates.
     * 
     * @param worldX World X coordinate
     * @param worldY World Y coordinate
     * @param worldZ World Z coordinate
     * @return FluidCell representing the cell containing this position
     */
    private FluidCell getFluidCell(float worldX, float worldY, float worldZ) {
        return new FluidCell(
            (int) Math.floor(worldX / CELL_SIZE),
            (int) Math.floor(worldY / CELL_SIZE),
            (int) Math.floor(worldZ / CELL_SIZE)
        );
    }
    
    /**
     * Removes empty fluid cells to save memory.
     * Called periodically to clean up cells with zero density.
     */
    private void cleanupEmptyCells() {
        fluidCells.entrySet().removeIf(entry -> entry.getValue().density <= 0);
    }
    
    /**
     * Represents a fluid simulation cell in the 3D grid.
     * Each cell contains fluid data and is identified by discrete coordinates.
     */
    private static class FluidCell {
        final int x, y, z;
        
        FluidCell(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof FluidCell)) return false;
            FluidCell other = (FluidCell) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return x * 31 * 31 + y * 31 + z;
        }
    }
    
    /**
     * Fluid data for a simulation cell.
     * Contains density, pressure, and velocity information for fluid calculations.
     */
    private static class FluidData {
        float density;
        float pressure;
        final Vector3f velocity;
        
        FluidData() {
            this.density = 0.0f;
            this.pressure = 0.0f;
            this.velocity = new Vector3f();
        }
    }
}