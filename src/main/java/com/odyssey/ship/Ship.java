package com.odyssey.ship;

import com.odyssey.util.Logger;
import com.odyssey.physics.OceanPhysics;
import com.odyssey.physics.WaveSystem;
import com.odyssey.rendering.Material;
import com.odyssey.rendering.Mesh;
import com.odyssey.rendering.RenderCommand;
import com.odyssey.ship.components.*;
import com.odyssey.util.MathUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete ship with modular components, physics simulation, and rendering.
 * Ships are built from individual components that can be damaged, repaired, and upgraded.
 */
public class Ship {
    
    // Ship identification
    private final String name;
    private final ShipType shipType;
    
    // Transform
    private final Vector3f position;
    private final Quaternionf rotation;
    private final Vector3f scale;
    private final Matrix4f transformMatrix;
    
    // Physics
    private final Vector3f velocity;
    private final Vector3f angularVelocity;
    private float mass;
    private float drag;
    private float angularDrag;
    
    // Ship components
    private final Map<ComponentType, List<ShipComponent>> components;
    private HullComponent hull;
    private SailComponent mainSail;
    private ShipComponent rudder;
    
    // Ship stats
    private float health;
    private float maxHealth;
    private float speed;
    private float maneuverability;
    private float stability;
    private int cargoCapacity;
    private int crewCapacity;
    
    // Rendering
    private Mesh shipMesh;
    private Material shipMaterial;
    private boolean needsMeshUpdate;
    
    // Damage system
    private final List<DamagePoint> damagePoints;
    private float waterLevel; // Water inside the ship
    
    // Ocean interaction
    private OceanPhysics oceanPhysics;
    private WaveSystem waveSystem;
    
    public Ship(String name, ShipType shipType, Vector3f position) {
        this.name = name;
        this.shipType = shipType;
        
        // Transform
        this.position = new Vector3f(position);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1.0f);
        this.transformMatrix = new Matrix4f();
        
        // Physics
        this.velocity = new Vector3f();
        this.angularVelocity = new Vector3f();
        this.mass = shipType.getBaseMass();
        this.drag = 0.1f;
        this.angularDrag = 0.2f;
        
        // Components
        this.components = new HashMap<>();
        for (ComponentType type : ComponentType.values()) {
            components.put(type, new ArrayList<>());
        }
        
        // Stats
        this.maxHealth = shipType.getBaseHealth();
        this.health = maxHealth;
        this.speed = 0.0f;
        this.maneuverability = shipType.getBaseManeuverability();
        this.stability = shipType.getBaseStability();
        this.cargoCapacity = shipType.getBaseCargoCapacity();
        this.crewCapacity = shipType.getBaseCrewCapacity();
        
        // Rendering
        this.needsMeshUpdate = true;
        
        // Damage
        this.damagePoints = new ArrayList<>();
        this.waterLevel = 0.0f;
        
        // Initialize basic components
        initializeBasicComponents();
        
        Logger.world("Created {} ship '{}' at position {}", shipType, name, position);
    }
    
    /**
     * Initializes ship systems after construction
     */
    public void initializeSystems() {
        // Update ship stats based on components
        updateShipStats();
        
        // Initialize physics
        updateTransformMatrix();
        
        // Mark mesh for update
        needsMeshUpdate = true;
        
        Logger.world("Initialized systems for ship '{}'", name);
    }
    
    /**
     * Updates ship physics and systems
     */
    public void update(float deltaTime) {
        // Update components
        updateComponents(deltaTime);
        
        // Update physics
        updatePhysics(deltaTime);
        
        // Update damage and flooding
        updateDamage(deltaTime);
        
        // Update rendering
        if (needsMeshUpdate) {
            updateMesh();
            needsMeshUpdate = false;
        }
        
        // Update transform matrix
        updateTransformMatrix();
    }
    
    /**
     * Renders the ship
     */
    public RenderCommand getRenderCommand() {
        if (shipMesh == null || shipMaterial == null) {
            return null;
        }
        
        RenderCommand command = new RenderCommand();
        command.setMesh(shipMesh);
        // Note: RenderCommand doesn't have a material field or setter, using shader instead
        command.setModelMatrix(new Matrix4f(transformMatrix));
        command.setWorldPosition(new Vector3f(position));
        command.setRenderQueue(RenderCommand.RenderQueue.OPAQUE);
        
        return command;
    }
    
    /**
     * Adds a component to the ship
     */
    public void addComponent(ShipComponent component) {
        ComponentType type = component.getType();
        components.get(type).add(component);
        
        // Update ship stats based on component
        updateShipStats();
        needsMeshUpdate = true;
        
        Logger.world("Added {} component to ship '{}'", type, name);
    }
    
    /**
     * Removes a component from the ship
     */
    public void removeComponent(ShipComponent component) {
        ComponentType type = component.getType();
        if (components.get(type).remove(component)) {
            updateShipStats();
            needsMeshUpdate = true;
            Logger.world("Removed {} component from ship '{}'", type, name);
        }
    }
    
    /**
     * Gets components of a specific type
     */
    public List<ShipComponent> getComponents(ComponentType type) {
        return new ArrayList<>(components.get(type));
    }
    
    /**
     * Gets all components of the ship
     */
    public List<ShipComponent> getComponents() {
        List<ShipComponent> allComponents = new ArrayList<>();
        for (List<ShipComponent> componentList : components.values()) {
            allComponents.addAll(componentList);
        }
        return allComponents;
    }
    
    /**
     * Applies damage to the ship at a specific point
     */
    public void takeDamage(Vector3f localPosition, float damage, DamageType damageType) {
        // Find closest component
        ShipComponent closestComponent = findClosestComponent(localPosition);
        
        if (closestComponent != null) {
            closestComponent.takeDamage(damage, damageType);
        }
        
        // Add damage point for visual effects
        DamagePoint damagePoint = new DamagePoint(localPosition, damage, damageType);
        damagePoints.add(damagePoint);
        
        // Reduce overall health
        health -= damage * 0.1f; // Components absorb most damage
        health = Math.max(0, health);
        
        // Check for hull breaches
        if (closestComponent instanceof HullComponent && closestComponent.getHealthPercentage() < 0.5f) {
            // Hull breach causes flooding
            waterLevel += damage * 0.01f;
        }
        
        needsMeshUpdate = true;
        Logger.world("Ship '{}' took {} {} damage at {}", name, damage, damageType, localPosition);
    }
    
    /**
     * Repairs the ship
     */
    public void repair(float repairAmount) {
        // Repair components
        for (List<ShipComponent> componentList : components.values()) {
            for (ShipComponent component : componentList) {
                if (component.getHealthPercentage() < 1.0f) {
                    component.repair(repairAmount * 0.1f);
                }
            }
        }
        
        // Repair overall health
        health += repairAmount;
        health = Math.min(maxHealth, health);
        
        // Reduce water level (pumping out water)
        waterLevel -= repairAmount * 0.02f;
        waterLevel = Math.max(0, waterLevel);
        
        needsMeshUpdate = true;
        Logger.world("Repaired ship '{}' by {}", name, repairAmount);
    }
    
    /**
     * Sets sail configuration
     */
    public void setSail(float sailAmount) {
        if (mainSail != null) {
            // Assuming SailComponent has a method to set deployment
            // mainSail.setSailAmount(sailAmount);
        }
    }
    
    /**
     * Sets rudder angle
     */
    public void setRudder(float rudderAngle) {
        if (rudder != null) {
            // Assuming rudder component has a method to set angle
            // rudder.setAngle(rudderAngle);
        }
    }
    
    /**
     * Checks if the ship is sinking
     */
    public boolean isSinking() {
        return waterLevel > 0.8f || health <= 0;
    }
    
    /**
     * Gets ship buoyancy at current position
     */
    public float getBuoyancy() {
        if (oceanPhysics == null) return 0.0f;
        
        float waterHeight = oceanPhysics.getWaterHeight(position.x, position.z);
        float submersion = Math.max(0, waterHeight - position.y);
        
        // Calculate displaced water volume
        float hullVolume = hull != null ? hull.getVolume() : 100.0f;
        float displacedVolume = Math.min(hullVolume, submersion * 10.0f); // Approximate
        
        // Reduce buoyancy based on flooding
        displacedVolume *= (1.0f - waterLevel);
        
        return displacedVolume * 1000.0f * 9.81f; // Water density * gravity
    }
    
    // Getters and setters
    public String getName() { return name; }
    public ShipType getShipType() { return shipType; }
    public Vector3f getPosition() { return new Vector3f(position); }
    public Quaternionf getRotation() { return new Quaternionf(rotation); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getHealthPercentage() { return health / maxHealth; }
    public float getSpeed() { return speed; }
    public float getWaterLevel() { return waterLevel; }
    
    public void setOceanPhysics(OceanPhysics oceanPhysics) { this.oceanPhysics = oceanPhysics; }
    public void setWaveSystem(WaveSystem waveSystem) { this.waveSystem = waveSystem; }
    
    /**
     * Initializes basic ship components
     */
    private void initializeBasicComponents() {
        // Create hull
        hull = new HullComponent("Main Hull", new Vector3f(0, 0, 0), 
                                HullComponent.HullMaterial.WOOD, 1.0f);
        addComponent(hull);
        
        // Create main sail
        mainSail = new SailComponent("Main Sail", new Vector3f(0, 5, 0), 
                                   SailComponent.SailType.SQUARE_SAIL, 1);
        addComponent(mainSail);
        
        // Create rudder - using generic ShipComponent since RudderComponent doesn't exist
        // TODO: Create RudderComponent class
        rudder = new HullComponent("Rudder", new Vector3f(0, -1, -8), 
                                 HullComponent.HullMaterial.WOOD, 0.5f);
        addComponent(rudder);
        
        // Add cannons based on ship type
        int cannonCount = shipType.getBaseCannons();
        for (int i = 0; i < cannonCount; i++) {
            float side = (i % 2 == 0) ? -1.0f : 1.0f;
            float position = (i / 2) * 2.0f - cannonCount / 4.0f;
            
            CannonComponent cannon = new CannonComponent("Cannon " + (i + 1), 
                                                       new Vector3f(side * 3.0f, 1.0f, position),
                                                       CannonComponent.CannonType.MEDIUM_CANNON);
            addComponent(cannon);
        }
    }
    
    /**
     * Updates all ship components
     */
    private void updateComponents(float deltaTime) {
        for (List<ShipComponent> componentList : components.values()) {
            for (ShipComponent component : componentList) {
                component.update(deltaTime);
            }
        }
    }
    
    /**
     * Updates ship physics
     */
    private void updatePhysics(float deltaTime) {
        if (oceanPhysics == null) return;
        
        // Get ocean forces
        Vector3f oceanForce = oceanPhysics.calculateForces(position, velocity, mass, 0.5f);
        
        // Get wave forces
        Vector3f waveForce = new Vector3f();
        if (waveSystem != null) {
            waveForce = waveSystem.calculateWaveForce(position, 5.0f, mass);
        }
        
        // Calculate sail force
        Vector3f sailForce = calculateSailForce();
        
        // Calculate rudder force
        Vector3f rudderForce = calculateRudderForce();
        
        // Sum all forces
        Vector3f totalForce = new Vector3f(oceanForce)
            .add(waveForce)
            .add(sailForce)
            .add(rudderForce);
        
        // Add buoyancy
        float buoyancy = getBuoyancy();
        totalForce.y += buoyancy - mass * 9.81f; // Subtract weight
        
        // Apply drag
        Vector3f dragForce = new Vector3f(velocity).negate().mul(drag * velocity.lengthSquared());
        totalForce.add(dragForce);
        
        // Update velocity
        Vector3f acceleration = new Vector3f(totalForce).div(mass);
        velocity.add(new Vector3f(acceleration).mul(deltaTime));
        
        // Update position
        position.add(new Vector3f(velocity).mul(deltaTime));
        
        // Update angular velocity (simplified)
        angularVelocity.mul(1.0f - angularDrag * deltaTime);
        
        // Apply angular velocity to rotation
        if (angularVelocity.lengthSquared() > 0.001f) {
            Quaternionf deltaRotation = new Quaternionf().rotateAxis(
                angularVelocity.length() * deltaTime,
                angularVelocity.x / angularVelocity.length(),
                angularVelocity.y / angularVelocity.length(),
                angularVelocity.z / angularVelocity.length()
            );
            rotation.mul(deltaRotation);
        }
        
        // Update speed
        speed = velocity.length();
    }
    
    /**
     * Calculates force from sails
     */
    private Vector3f calculateSailForce() {
        Vector3f sailForce = new Vector3f();
        
        if (mainSail != null && oceanPhysics != null) {
            Vector3f windVelocity = oceanPhysics.getWindVelocity(position);
            
            // Calculate relative wind
            Vector3f relativeWind = new Vector3f(windVelocity).sub(velocity);
            
            // Get ship forward direction
            Vector3f forward = new Vector3f(0, 0, 1);
            rotation.transform(forward);
            
            // Calculate sail efficiency based on wind angle
            float windAngle = (float) Math.acos(MathUtils.clamp(relativeWind.normalize().dot(forward), -1, 1));
            float sailEfficiency = (float) Math.sin(windAngle) * mainSail.getSailAmount();
            
            // Calculate sail force
            float windSpeed = relativeWind.length();
            float forceMagnitude = 0.5f * 1.225f * windSpeed * windSpeed * mainSail.getSailArea() * sailEfficiency;
            
            sailForce = new Vector3f(relativeWind).normalize().mul(forceMagnitude);
        }
        
        return sailForce;
    }
    
    /**
     * Calculates force from rudder
     */
    private Vector3f calculateRudderForce() {
        Vector3f rudderForce = new Vector3f();
        
        if (rudder != null && speed > 0.1f) {
            // TODO: Implement proper rudder angle when RudderComponent is created
            float rudderAngle = 0.0f; // rudder.getAngle();
            
            // Rudder creates lateral force proportional to speed and angle
            float forceMagnitude = speed * speed * Math.abs(rudderAngle) * 100.0f;
            
            // Force is perpendicular to ship direction
            Vector3f right = new Vector3f(1, 0, 0);
            rotation.transform(right);
            
            rudderForce = new Vector3f(right).mul(forceMagnitude * Math.signum(rudderAngle));
            
            // Also create angular velocity
            angularVelocity.y += rudderAngle * speed * 0.1f;
        }
        
        return rudderForce;
    }
    
    /**
     * Updates damage and flooding
     */
    private void updateDamage(float deltaTime) {
        // Water level affects ship performance
        if (waterLevel > 0) {
            // Reduce speed and maneuverability
            float waterEffect = 1.0f - waterLevel * 0.5f;
            drag *= (2.0f - waterEffect);
            angularDrag *= (2.0f - waterEffect);
            
            // Continuous damage from flooding
            if (waterLevel > 0.5f) {
                health -= waterLevel * 10.0f * deltaTime;
                health = Math.max(0, health);
            }
        }
        
        // Remove old damage points
        damagePoints.removeIf(dp -> dp.age > 60.0f);
        
        // Age damage points
        for (DamagePoint dp : damagePoints) {
            dp.age += deltaTime;
        }
    }
    
    /**
     * Updates ship stats based on components
     */
    private void updateShipStats() {
        // Reset to base stats
        mass = shipType.getBaseMass();
        maxHealth = shipType.getBaseHealth();
        
        // Add component contributions
        for (List<ShipComponent> componentList : components.values()) {
            for (ShipComponent component : componentList) {
                mass += component.getMass();
                maxHealth += component.getHealth();
            }
        }
        
        // Ensure health doesn't exceed max
        health = Math.min(health, maxHealth);
    }
    
    /**
     * Finds the closest component to a position
     */
    private ShipComponent findClosestComponent(Vector3f position) {
        ShipComponent closest = null;
        float closestDistance = Float.MAX_VALUE;
        
        for (List<ShipComponent> componentList : components.values()) {
            for (ShipComponent component : componentList) {
                float distance = component.getPosition().distance(position);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = component;
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Updates the ship's mesh
     */
    private void updateMesh() {
        // TODO: Implement procedural ship mesh generation based on components
        // For now, use a placeholder mesh
        if (shipMesh == null) {
            shipMesh = Mesh.createCube("ShipMesh", 2.0f); // Placeholder
            shipMaterial = new Material("ShipMaterial", Material.MaterialType.SHIP);
        }
    }
    
    /**
     * Updates the transform matrix
     */
    private void updateTransformMatrix() {
        transformMatrix.identity()
            .translate(position)
            .rotate(rotation)
            .scale(scale);
    }
    
    // Getter and setter methods for physics integration
    
    /**
     * Sets the ship's position
     */
    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
        updateTransformMatrix();
    }
    
    /**
     * Gets the ship's orientation
     */
    public Quaternionf getOrientation() {
        return new Quaternionf(rotation);
    }
    
    /**
     * Sets the ship's orientation
     */
    public void setOrientation(Quaternionf newOrientation) {
        this.rotation.set(newOrientation);
        updateTransformMatrix();
    }
    
    /**
     * Sets the ship's velocity
     */
    public void setVelocity(Vector3f newVelocity) {
        this.velocity.set(newVelocity);
    }
    
    /**
     * Damage point for visual effects
     */
    private static class DamagePoint {
        final Vector3f position;
        final float damage;
        final DamageType type;
        float age;
        
        DamagePoint(Vector3f position, float damage, DamageType type) {
            this.position = new Vector3f(position);
            this.damage = damage;
            this.type = type;
            this.age = 0.0f;
        }
    }
}