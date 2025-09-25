package com.odyssey.ship;

import com.odyssey.util.Logger;
import com.odyssey.util.MathUtils;
import com.odyssey.physics.OceanPhysics;
import com.odyssey.physics.WaveSystem;
import com.odyssey.rendering.Material;
import com.odyssey.rendering.Mesh;
import com.odyssey.rendering.RenderCommand;
import com.odyssey.ship.components.*;
import com.odyssey.ship.components.RudderComponent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete ship with modular components, physics simulation, and rendering.
 * Ships are built from individual components that can be damaged, repaired, and upgraded.
 */
public class Ship implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
    private RudderComponent rudder;
    
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
    
    // Ship physics handler
    private ShipPhysics shipPhysics;
    
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
        
        // Initialize ship physics
        this.shipPhysics = new ShipPhysics(this);
        
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
        
        // Set parent ship reference
        component.setParentShip(this);
        
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
    public Map<ComponentType, List<ShipComponent>> getComponents() {
        return components;
    }
    
    /**
     * Gets all components as a flat list
     */
    public List<ShipComponent> getAllComponents() {
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
            rudder.setRudderAngle(rudderAngle);
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
    public String getId() { return name; } // Use name as ID for now
    public ShipType getShipType() { return shipType; }
    public Vector3f getPosition() { return new Vector3f(position); }
    public Quaternionf getRotation() { return new Quaternionf(rotation); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getHealthPercentage() { return health / maxHealth; }
    public float getSpeed() { return speed; }
    public float getWaterLevel() { return waterLevel; }
    public float getMass() { return mass; }
    public ShipPhysics getShipPhysics() { return shipPhysics; }
    
    public void setOceanPhysics(OceanPhysics oceanPhysics) { this.oceanPhysics = oceanPhysics; }
    public void setWaveSystem(WaveSystem waveSystem) { this.waveSystem = waveSystem; }
    
    /**
     * Applies weather effects to the ship and its components
     * @param weather The weather condition to apply
     */
    public void applyWeatherEffects(com.odyssey.world.weather.WeatherCondition weather) {
        if (weather == null) return;
        
        // Apply weather effects to sails
        for (ShipComponent component : components.get(ComponentType.SAIL)) {
            if (component instanceof SailComponent) {
                SailComponent sail = (SailComponent) component;
                // Update wind conditions for the sail
                sail.setWindConditions(
                    new Vector3f(weather.getWindSpeed(), 0, 0), 
                    weather.getWindSpeed()
                );
                
                // Apply storm damage if severe weather
                if (weather.getWindSpeed() > 25.0f) {
                    sail.takeDamage(weather.getWindSpeed() * 0.1f, DamageType.STORM);
                }
            }
        }
        
        // Apply weather effects to hull
        if (hull != null && weather.getWindSpeed() > 30.0f) {
            // Severe weather can damage hull
            hull.takeDamage(weather.getWindSpeed() * 0.05f, DamageType.STORM);
        }
        
        // Update ship stability based on weather
        float weatherStabilityModifier = 1.0f - (weather.getWindSpeed() / 50.0f) * 0.3f;
        this.stability = shipType.getBaseStability() * Math.max(0.3f, weatherStabilityModifier);
    }
    
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
        
        // Create rudder
        rudder = new RudderComponent("Rudder", new Vector3f(0, -1, -8), 
                                   RudderComponent.RudderType.BALANCED_RUDDER);
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
            // Get actual rudder angle from RudderComponent
            float rudderAngle = rudder.getCurrentAngle();
            
            // Use RudderComponent's built-in force calculation
            Vector3f turningForce = rudder.calculateTurningForce(speed, velocity);
            
            // Transform force to world coordinates
            rotation.transform(turningForce);
            rudderForce.add(turningForce);
            
            // Also create angular velocity based on rudder effectiveness
            float angularEffect = rudderAngle * speed * rudder.getTurningEfficiency() * 0.1f;
            angularVelocity.y += angularEffect;
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
     * Updates the ship's mesh based on components
     */
    private void updateMesh() {
        if (!needsMeshUpdate && shipMesh != null) {
            return;
        }
        
        // Clean up old mesh
        if (shipMesh != null) {
            shipMesh.cleanup();
        }
        
        // Generate new mesh from components
        shipMesh = generateProceduralMesh();
        if (shipMaterial == null) {
            shipMaterial = new Material("ShipMaterial", Material.MaterialType.SHIP);
        }
        
        needsMeshUpdate = false;
        Logger.world("Updated mesh for ship '{}'", name);
    }
    
    /**
     * Generates a procedural mesh based on ship components
     */
    private Mesh generateProceduralMesh() {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int vertexOffset = 0;
        
        // Generate mesh for each component type
        for (Map.Entry<ComponentType, List<ShipComponent>> entry : components.entrySet()) {
            ComponentType type = entry.getKey();
            List<ShipComponent> componentList = entry.getValue();
            
            for (ShipComponent component : componentList) {
                if (component.isDestroyed()) {
                    continue; // Skip destroyed components
                }
                
                ComponentMeshData meshData = generateComponentMesh(component, type);
                if (meshData != null) {
                    // Add vertices with component position offset
                    Vector3f componentPos = component.getPosition();
                    for (Vector3f pos : meshData.positions) {
                        positions.add(new Vector3f(pos).add(componentPos));
                    }
                    
                    // Add normals and texture coordinates
                    normals.addAll(meshData.normals);
                    texCoords.addAll(meshData.texCoords);
                    
                    // Add indices with vertex offset
                    for (Integer index : meshData.indices) {
                        indices.add(index + vertexOffset);
                    }
                    
                    vertexOffset += meshData.positions.size();
                }
            }
        }
        
        // Create mesh using MeshManager if available, otherwise create directly
        if (positions.isEmpty()) {
            // Fallback to basic cube if no components
            return Mesh.createCube("ShipMesh_" + name, 2.0f);
        }
        
        return new Mesh("ShipMesh_" + name, 
                       convertToVertexArray(positions, normals, texCoords),
                       indices.stream().mapToInt(Integer::intValue).toArray());
    }
    
    /**
     * Generates mesh data for a specific component
     */
    private ComponentMeshData generateComponentMesh(ShipComponent component, ComponentType type) {
        switch (type) {
            case HULL:
                return generateHullMesh(component);
            case SAIL:
                return generateSailMesh(component);
            case CANNON:
                return generateCannonMesh(component);
            case ENGINE:
                return generateEngineMesh(component);
            case MAST:
                return generateMastMesh(component);
            case RUDDER:
                return generateRudderMesh(component);
            case ANCHOR:
                return generateAnchorMesh(component);
            case FIGUREHEAD:
                return generateFigureheadMesh(component);
            case CARGO_HOLD:
                return generateCargoHoldMesh(component);
            case CREW_QUARTERS:
                return generateCrewQuartersMesh(component);
            default:
                return generateGenericComponentMesh(component);
        }
    }
    
    /**
     * Generates hull mesh - the main body of the ship
     */
    private ComponentMeshData generateHullMesh(ShipComponent component) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Hull dimensions based on ship type
        float length = shipType.getLength();
        float width = shipType.getWidth();
        float height = shipType.getHeight();
        
        // Generate hull shape - simplified boat hull
        int lengthSegments = 16;
        int widthSegments = 8;
        int heightSegments = 4;
        
        // Generate vertices for hull shape
        for (int h = 0; h <= heightSegments; h++) {
            float y = (h / (float) heightSegments - 0.5f) * height;
            float hullWidth = width * (1.0f - Math.abs(y / height) * 0.3f); // Tapered hull
            
            for (int l = 0; l <= lengthSegments; l++) {
                float z = (l / (float) lengthSegments - 0.5f) * length;
                float bowSternTaper = 1.0f - Math.abs(z / (length * 0.5f)) * 0.7f; // Tapered bow/stern
                float currentWidth = hullWidth * bowSternTaper;
                
                for (int w = 0; w <= widthSegments; w++) {
                    float x = (w / (float) widthSegments - 0.5f) * currentWidth;
                    
                    // Create curved hull bottom
                    if (h == 0) {
                        y = -height * 0.5f + Math.abs(x / currentWidth) * height * 0.2f;
                    }
                    
                    positions.add(new Vector3f(x, y, z));
                    
                    // Calculate normal (simplified)
                    Vector3f normal = new Vector3f(x, 1.0f, z).normalize();
                    normals.add(normal);
                    
                    // Texture coordinates
                    texCoords.add(new Vector2f(w / (float) widthSegments, l / (float) lengthSegments));
                }
            }
        }
        
        // Generate indices for hull triangles
        for (int h = 0; h < heightSegments; h++) {
            for (int l = 0; l < lengthSegments; l++) {
                for (int w = 0; w < widthSegments; w++) {
                    int baseIndex = h * (lengthSegments + 1) * (widthSegments + 1) + 
                                   l * (widthSegments + 1) + w;
                    int nextHeight = baseIndex + (lengthSegments + 1) * (widthSegments + 1);
                    
                    // Create quad faces
                    addQuadIndices(indices, baseIndex, baseIndex + 1, 
                                  nextHeight + 1, nextHeight);
                }
            }
        }
        
        return new ComponentMeshData(positions, normals, texCoords, indices);
    }
    
    /**
     * Generates sail mesh - cloth-like rectangular sail
     */
    private ComponentMeshData generateSailMesh(ShipComponent component) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Sail dimensions
        float sailWidth = 4.0f;
        float sailHeight = 6.0f;
        int segments = 8;
        
        // Generate sail vertices with slight curve for wind effect
        for (int h = 0; h <= segments; h++) {
            for (int w = 0; w <= segments; w++) {
                float x = (w / (float) segments - 0.5f) * sailWidth;
                float y = h / (float) segments * sailHeight;
                float z = Math.abs(x) * 0.3f; // Curved sail effect
                
                positions.add(new Vector3f(x, y, z));
                normals.add(new Vector3f(0, 0, 1)); // Facing forward
                texCoords.add(new Vector2f(w / (float) segments, h / (float) segments));
            }
        }
        
        // Generate sail indices
        for (int h = 0; h < segments; h++) {
            for (int w = 0; w < segments; w++) {
                int baseIndex = h * (segments + 1) + w;
                addQuadIndices(indices, baseIndex, baseIndex + 1, 
                              baseIndex + segments + 2, baseIndex + segments + 1);
            }
        }
        
        return new ComponentMeshData(positions, normals, texCoords, indices);
    }
    
    /**
     * Generates cannon mesh - cylindrical cannon with carriage
     */
    private ComponentMeshData generateCannonMesh(ShipComponent component) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        // Cannon barrel (cylinder)
        float barrelLength = 2.0f;
        float barrelRadius = 0.2f;
        int segments = 12;
        
        // Generate cylinder for cannon barrel
        for (int i = 0; i <= segments; i++) {
            float angle = 2.0f * (float) Math.PI * i / segments;
            float x = barrelRadius * (float) Math.cos(angle);
            float y = barrelRadius * (float) Math.sin(angle);
            
            // Front and back of barrel
            positions.add(new Vector3f(x, y, 0));
            positions.add(new Vector3f(x, y, barrelLength));
            
            Vector3f normal = new Vector3f(x, y, 0).normalize();
            normals.add(normal);
            normals.add(normal);
            
            texCoords.add(new Vector2f(i / (float) segments, 0));
            texCoords.add(new Vector2f(i / (float) segments, 1));
        }
        
        // Generate cylinder indices
        for (int i = 0; i < segments; i++) {
            int current = i * 2;
            int next = ((i + 1) % segments) * 2;
            
            // Side faces
            addQuadIndices(indices, current, current + 1, next + 1, next);
        }
        
        // Add cannon carriage (simplified box)
        addBoxToMesh(positions, normals, texCoords, indices, 
                    new Vector3f(0, -0.5f, barrelLength * 0.5f), 
                    new Vector3f(1.0f, 0.5f, 1.5f));
        
        return new ComponentMeshData(positions, normals, texCoords, indices);
    }
    
    /**
     * Generates engine mesh - rectangular engine block
     */
    private ComponentMeshData generateEngineMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(2.0f, 1.5f, 3.0f)); // Engine block
    }
    
    /**
     * Generates mast mesh - tall cylindrical pole
     */
    private ComponentMeshData generateMastMesh(ShipComponent component) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        float mastHeight = 8.0f;
        float mastRadius = 0.3f;
        int segments = 8;
        
        // Generate cylinder for mast
        for (int h = 0; h <= 1; h++) {
            for (int i = 0; i <= segments; i++) {
                float angle = 2.0f * (float) Math.PI * i / segments;
                float x = mastRadius * (float) Math.cos(angle);
                float z = mastRadius * (float) Math.sin(angle);
                float y = h * mastHeight;
                
                positions.add(new Vector3f(x, y, z));
                normals.add(new Vector3f(x, 0, z).normalize());
                texCoords.add(new Vector2f(i / (float) segments, h));
            }
        }
        
        // Generate mast indices
        for (int i = 0; i < segments; i++) {
            addQuadIndices(indices, i, i + 1, i + segments + 2, i + segments + 1);
        }
        
        return new ComponentMeshData(positions, normals, texCoords, indices);
    }
    
    /**
     * Generates rudder mesh - flat vertical fin
     */
    private ComponentMeshData generateRudderMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(0.2f, 2.0f, 1.0f)); // Thin vertical rudder
    }
    
    /**
     * Generates anchor mesh - anchor shape
     */
    private ComponentMeshData generateAnchorMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(0.5f, 1.0f, 0.5f)); // Simplified anchor
    }
    
    /**
     * Generates figurehead mesh - decorative bow piece
     */
    private ComponentMeshData generateFigureheadMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(0.8f, 1.2f, 0.6f)); // Decorative figurehead
    }
    
    /**
     * Generates cargo hold mesh - large storage compartment
     */
    private ComponentMeshData generateCargoHoldMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(3.0f, 2.0f, 4.0f)); // Large cargo space
    }
    
    /**
     * Generates crew quarters mesh - living compartment
     */
    private ComponentMeshData generateCrewQuartersMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(2.5f, 2.0f, 3.0f)); // Crew living space
    }
    
    /**
     * Generates generic component mesh - default box
     */
    private ComponentMeshData generateGenericComponentMesh(ShipComponent component) {
        return generateBoxMesh(new Vector3f(1.0f, 1.0f, 1.0f)); // Default cube
    }
    
    /**
     * Helper method to generate a simple box mesh
     */
    private ComponentMeshData generateBoxMesh(Vector3f size) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        addBoxToMesh(positions, normals, texCoords, indices, new Vector3f(0, 0, 0), size);
        return new ComponentMeshData(positions, normals, texCoords, indices);
    }
    
    /**
     * Helper method to add a box to existing mesh data
     */
    private void addBoxToMesh(List<Vector3f> positions, List<Vector3f> normals, 
                             List<Vector2f> texCoords, List<Integer> indices,
                             Vector3f center, Vector3f size) {
        int startIndex = positions.size();
        float hx = size.x * 0.5f;
        float hy = size.y * 0.5f;
        float hz = size.z * 0.5f;
        
        // Box vertices (8 corners)
        Vector3f[] corners = {
            new Vector3f(center.x - hx, center.y - hy, center.z - hz), // 0
            new Vector3f(center.x + hx, center.y - hy, center.z - hz), // 1
            new Vector3f(center.x + hx, center.y + hy, center.z - hz), // 2
            new Vector3f(center.x - hx, center.y + hy, center.z - hz), // 3
            new Vector3f(center.x - hx, center.y - hy, center.z + hz), // 4
            new Vector3f(center.x + hx, center.y - hy, center.z + hz), // 5
            new Vector3f(center.x + hx, center.y + hy, center.z + hz), // 6
            new Vector3f(center.x - hx, center.y + hy, center.z + hz)  // 7
        };
        
        // Face normals
        Vector3f[] faceNormals = {
            new Vector3f(0, 0, -1), // Front
            new Vector3f(0, 0, 1),  // Back
            new Vector3f(-1, 0, 0), // Left
            new Vector3f(1, 0, 0),  // Right
            new Vector3f(0, -1, 0), // Bottom
            new Vector3f(0, 1, 0)   // Top
        };
        
        // Face vertex indices
        int[][] faces = {
            {0, 1, 2, 3}, // Front
            {5, 4, 7, 6}, // Back
            {4, 0, 3, 7}, // Left
            {1, 5, 6, 2}, // Right
            {4, 5, 1, 0}, // Bottom
            {3, 2, 6, 7}  // Top
        };
        
        // Add vertices for each face
        for (int face = 0; face < 6; face++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                positions.add(corners[faces[face][vertex]]);
                normals.add(faceNormals[face]);
                texCoords.add(new Vector2f(vertex % 2, vertex / 2));
            }
            
            // Add indices for face triangles
            int faceStart = startIndex + face * 4;
            addQuadIndices(indices, faceStart, faceStart + 1, faceStart + 2, faceStart + 3);
        }
    }
    
    /**
     * Helper method to add quad indices as two triangles
     */
    private void addQuadIndices(List<Integer> indices, int a, int b, int c, int d) {
        // First triangle
        indices.add(a);
        indices.add(b);
        indices.add(c);
        
        // Second triangle
        indices.add(c);
        indices.add(d);
        indices.add(a);
    }
    
    /**
     * Converts position, normal, and texture coordinate lists to vertex array
     */
    private float[] convertToVertexArray(List<Vector3f> positions, List<Vector3f> normals, List<Vector2f> texCoords) {
        float[] vertices = new float[positions.size() * 8]; // pos(3) + normal(3) + uv(2)
        
        for (int i = 0; i < positions.size(); i++) {
            Vector3f pos = positions.get(i);
            Vector3f normal = i < normals.size() ? normals.get(i) : new Vector3f(0, 1, 0);
            Vector2f uv = i < texCoords.size() ? texCoords.get(i) : new Vector2f(0, 0);
            
            int offset = i * 8;
            vertices[offset] = pos.x;
            vertices[offset + 1] = pos.y;
            vertices[offset + 2] = pos.z;
            vertices[offset + 3] = normal.x;
            vertices[offset + 4] = normal.y;
            vertices[offset + 5] = normal.z;
            vertices[offset + 6] = uv.x;
            vertices[offset + 7] = uv.y;
        }
        
        return vertices;
    }
    
    /**
     * Data class for component mesh information
     */
    private static class ComponentMeshData {
        final List<Vector3f> positions;
        final List<Vector3f> normals;
        final List<Vector2f> texCoords;
        final List<Integer> indices;
        
        ComponentMeshData(List<Vector3f> positions, List<Vector3f> normals, 
                         List<Vector2f> texCoords, List<Integer> indices) {
            this.positions = positions;
            this.normals = normals;
            this.texCoords = texCoords;
            this.indices = indices;
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
     * Gets the ship's forward direction vector based on current rotation.
     * @return normalized forward direction vector
     */
    public Vector3f getForwardDirection() {
        // Forward is typically the negative Z direction in ship coordinates
        Vector3f forward = new Vector3f(0, 0, -1);
        rotation.transform(forward);
        return forward.normalize();
    }
    
    /**
     * Cleanup ship resources
     */
    public void cleanup() {
        if (shipMesh != null) {
            shipMesh.cleanup();
            shipMesh = null;
        }
        
        // Cleanup component resources
        for (List<ShipComponent> componentList : components.values()) {
            for (ShipComponent component : componentList) {
                // Components may have their own cleanup logic
                if (component instanceof HullComponent) {
                    // Hull-specific cleanup if needed
                } else if (component instanceof EngineComponent) {
                    // Engine-specific cleanup if needed
                }
            }
        }
        
        Logger.world("Cleaned up ship: {}", name);
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