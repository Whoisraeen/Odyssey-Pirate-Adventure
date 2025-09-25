package com.odyssey.ship;

import com.odyssey.physics.OceanPhysics;
import com.odyssey.physics.WaveSystem;
import com.odyssey.ship.components.*;
import com.odyssey.util.Logger;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Ship physics system - handles realistic ship movement, buoyancy, and ocean interaction
 */
public class ShipPhysics {
    
    // Physics constants
    private static final float WATER_DENSITY = 1025.0f; // kg/m³ (seawater)
    private static final float AIR_DENSITY = 1.225f; // kg/m³
    private static final float GRAVITY = 9.81f; // m/s²
    private static final float DRAG_COEFFICIENT = 0.4f;
    private static final float ANGULAR_DRAG = 0.8f;
    
    // Ship reference
    private final Ship ship;
    
    // Physics state
    private Vector3f position;
    private Vector3f velocity;
    private Vector3f acceleration;
    private Quaternionf orientation;
    private Vector3f angularVelocity;
    private Vector3f angularAcceleration;
    
    // Ship properties
    private float mass;
    private float length;
    private float width;
    private float height;
    private float draft; // How deep the ship sits in water
    private Vector3f centerOfMass;
    private Vector3f centerOfBuoyancy;
    
    // Buoyancy calculation
    private float displacedVolume;
    private float buoyantForce;
    private float stability;
    
    // Forces
    private Vector3f totalForce;
    private Vector3f totalTorque;
    private Vector3f thrustForce;
    private Vector3f dragForce;
    private Vector3f windForce;
    private Vector3f waveForce;
    
    // Ocean interaction
    private OceanPhysics oceanPhysics;
    private WaveSystem waveSystem;
    
    // Performance metrics
    private float speed; // Current speed in knots
    private float maxSpeed; // Theoretical max speed
    private float maneuverability; // Turn rate modifier
    private float seaworthiness; // Stability in rough seas
    
    public ShipPhysics(Ship ship) {
        this.ship = ship;
        
        // Initialize vectors
        this.position = new Vector3f(ship.getPosition());
        this.velocity = new Vector3f();
        this.acceleration = new Vector3f();
        this.orientation = new Quaternionf(ship.getOrientation());
        this.angularVelocity = new Vector3f();
        this.angularAcceleration = new Vector3f();
        
        this.centerOfMass = new Vector3f();
        this.centerOfBuoyancy = new Vector3f();
        this.totalForce = new Vector3f();
        this.totalTorque = new Vector3f();
        this.thrustForce = new Vector3f();
        this.dragForce = new Vector3f();
        this.windForce = new Vector3f();
        this.waveForce = new Vector3f();
        
        // Calculate ship properties
        calculateShipProperties();
        
        Logger.world("Initialized physics for ship '{}' - Mass: {}, Draft: {}", 
                    ship.getName(), mass, draft);
    }
    
    /**
     * Updates ship physics for one frame
     */
    public void update(float deltaTime, OceanPhysics ocean, WaveSystem waves) {
        this.oceanPhysics = ocean;
        this.waveSystem = waves;
        
        // Reset forces
        totalForce.set(0, 0, 0);
        totalTorque.set(0, 0, 0);
        
        // Calculate all forces
        calculateBuoyancy();
        calculateThrust();
        calculateDrag();
        calculateWind();
        calculateWaves();
        calculateGravity();
        
        // Apply forces
        applyForces(deltaTime);
        
        // Update position and orientation
        updateMotion(deltaTime);
        
        // Update ship state
        updateShipState();
        
        // Calculate performance metrics
        updatePerformanceMetrics();
    }
    
    /**
     * Calculates buoyancy force and stability with improved accuracy
     */
    private void calculateBuoyancy() {
        // Sample water level at multiple points along the hull for better accuracy
        int samplePoints = 7;
        float totalBuoyancy = 0.0f;
        Vector3f buoyancyCenter = new Vector3f();
        float totalSubmersion = 0.0f;
        
        for (int i = 0; i < samplePoints; i++) {
            for (int j = 0; j < samplePoints; j++) {
                // Sample points across the ship's hull
                float x = position.x + (i - samplePoints/2) * length / samplePoints;
                float z = position.z + (j - samplePoints/2) * width / samplePoints;
                
                float waterLevel = oceanPhysics.getWaterHeight(x, z);
                float hullBottom = position.y - draft;
                float submersion = Math.max(0, waterLevel - hullBottom);
                
                if (submersion > 0) {
                    // Calculate local displaced volume
                    float localVolume = (length / samplePoints) * (width / samplePoints) * submersion;
                    float localBuoyancy = localVolume * WATER_DENSITY * GRAVITY;
                    
                    totalBuoyancy += localBuoyancy;
                    totalSubmersion += submersion;
                    
                    // Accumulate buoyancy center
                    buoyancyCenter.add(x * localBuoyancy, waterLevel * localBuoyancy, z * localBuoyancy);
                }
            }
        }
        
        if (totalBuoyancy > 0) {
            buoyantForce = totalBuoyancy / (samplePoints * samplePoints);
            buoyancyCenter.div(totalBuoyancy);
            centerOfBuoyancy.set(buoyancyCenter);
            
            // Apply buoyant force at the calculated center of buoyancy
            Vector3f buoyancyForce = new Vector3f(0, buoyantForce, 0);
            totalForce.add(buoyancyForce);
            
            // Calculate displaced volume for stability calculations
            float avgSubmersion = totalSubmersion / (samplePoints * samplePoints);
            float submersionRatio = Math.min(1.0f, avgSubmersion / height);
            displacedVolume = calculateDisplacedVolume(submersionRatio);
            
            // Enhanced stability calculation
            calculateStability(submersionRatio);
            
            // Apply restoring torque for stability
            Vector3f restoring = calculateRestoringTorque();
            totalTorque.add(restoring);
            
            // Add dynamic stability based on ship motion
            addDynamicStabilityForces();
        } else {
            buoyantForce = 0;
            displacedVolume = 0;
            stability = 0;
        }
    }
    
    /**
     * Calculates thrust from sails and engines
     */
    private void calculateThrust() {
        thrustForce.set(0, 0, 0);
        
        // Get thrust from sails
        Vector3f sailThrust = calculateSailThrust();
        thrustForce.add(sailThrust);
        
        // Get thrust from engines
        Vector3f engineThrust = calculateEngineThrust();
        thrustForce.add(engineThrust);
        
        // Apply thrust force
        totalForce.add(thrustForce);
        
        // Calculate torque from off-center thrust
        Vector3f thrustTorque = new Vector3f();
        centerOfMass.cross(thrustForce, thrustTorque);
        totalTorque.add(thrustTorque);
    }
    
    /**
     * Calculates drag forces (water and air resistance)
     */
    private void calculateDrag() {
        dragForce.set(0, 0, 0);
        
        // Water drag (dominant when in water)
        if (position.y <= oceanPhysics.getWaterHeight(position.x, position.z)) {
            Vector3f waterDrag = calculateWaterDrag();
            dragForce.add(waterDrag);
        }
        
        // Air drag (always present)
        Vector3f airDrag = calculateAirDrag();
        dragForce.add(airDrag);
        
        // Apply drag force (opposite to velocity)
        totalForce.add(dragForce);
        
        // Angular drag
        Vector3f angularDrag = new Vector3f(angularVelocity).mul(-ANGULAR_DRAG);
        totalTorque.add(angularDrag);
    }
    
    /**
     * Calculates wind forces on sails and hull
     */
    private void calculateWind() {
        windForce.set(0, 0, 0);
        
        // Get wind from ocean physics
        Vector3f windVelocity = oceanPhysics.getWindVelocity(position);
        
        // Relative wind (wind - ship velocity)
        Vector3f relativeWind = new Vector3f(windVelocity).sub(velocity);
        
        // Wind force on sails
        Vector3f sailWind = calculateSailWindForce(relativeWind);
        windForce.add(sailWind);
        
        // Wind force on hull (smaller effect)
        Vector3f hullWind = calculateHullWindForce(relativeWind);
        windForce.add(hullWind);
        
        totalForce.add(windForce);
    }
    
    /**
     * Calculates wave forces and motion with enhanced accuracy
     */
    private void calculateWaves() {
        waveForce.set(0, 0, 0);
        
        if (waveSystem == null) return;
        
        // Sample wave forces at multiple points along the hull for better accuracy
        int samplePoints = 7;
        Vector3f totalWaveForce = new Vector3f();
        Vector3f totalWaveTorque = new Vector3f();
        
        for (int i = 0; i < samplePoints; i++) {
            for (int j = 0; j < samplePoints; j++) {
                // Sample points across the ship's hull
                float x = position.x + (i - samplePoints/2) * length / samplePoints;
                float z = position.z + (j - samplePoints/2) * width / samplePoints;
                
                // Get wave properties at this point
                float waveHeight = waveSystem.getWaveHeight(x, z);
                Vector3f waveVelocity = waveSystem.getWaveVelocity(x, z);
                
                // Calculate local wave force based on hull interaction
                Vector3f localForce = calculateWaveForceAtPoint(x, z, waveHeight, waveVelocity);
                totalWaveForce.add(localForce);
                
                // Calculate torque from this force
                Vector3f leverArm = new Vector3f(x - position.x, 0, z - position.z);
                Vector3f localTorque = new Vector3f();
                leverArm.cross(localForce, localTorque);
                totalWaveTorque.add(localTorque);
            }
        }
        
        // Average the forces and apply them
        totalWaveForce.div(samplePoints * samplePoints);
        totalWaveTorque.div(samplePoints * samplePoints);
        
        // Apply wave forces with damping based on ship size and stability
        float waveDamping = Math.max(0.1f, stability * 0.5f);
        totalWaveForce.mul(waveDamping);
        totalWaveTorque.mul(waveDamping);
        
        waveForce.set(totalWaveForce);
        totalForce.add(waveForce);
        
        // Enhanced wave-induced rolling and pitching
        Vector3f waveTorque = calculateWaveTorque();
        waveTorque.add(totalWaveTorque);
        totalTorque.add(waveTorque);
        
        // Add dynamic wave response based on ship motion
        addWaveInducedMotion(totalWaveForce, waveTorque);
    }
    
    /**
     * Applies gravitational force
     */
    private void calculateGravity() {
        Vector3f gravity = new Vector3f(0, -mass * GRAVITY, 0);
        totalForce.add(gravity);
    }
    
    /**
     * Applies all calculated forces to update acceleration
     */
    private void applyForces(float deltaTime) {
        // Linear acceleration: F = ma -> a = F/m
        acceleration.set(totalForce).div(mass);
        
        // Angular acceleration: τ = Iα -> α = τ/I
        // Simplified moment of inertia calculation
        float momentOfInertia = mass * (length * length + width * width) / 12.0f;
        angularAcceleration.set(totalTorque).div(momentOfInertia);
        
        // Apply damping
        velocity.mul(0.999f); // Linear damping
        angularVelocity.mul(0.995f); // Angular damping
    }
    
    /**
     * Updates position and orientation based on velocity
     */
    private void updateMotion(float deltaTime) {
        // Update velocity: v = v + a*dt
        velocity.add(new Vector3f(acceleration).mul(deltaTime));
        angularVelocity.add(new Vector3f(angularAcceleration).mul(deltaTime));
        
        // Update position: p = p + v*dt
        position.add(new Vector3f(velocity).mul(deltaTime));
        
        // Update orientation: q = q + ω*dt (simplified)
        Quaternionf deltaRotation = new Quaternionf().rotateXYZ(
            angularVelocity.x * deltaTime,
            angularVelocity.y * deltaTime,
            angularVelocity.z * deltaTime
        );
        orientation.mul(deltaRotation);
        orientation.normalize();
        
        // Clamp to water surface (ships can't fly or go underground)
        float waterLevel = oceanPhysics.getWaterHeight(position.x, position.z);
        if (position.y < waterLevel - height) {
            position.y = waterLevel - height;
            velocity.y = Math.max(0, velocity.y); // Stop sinking
        }
    }
    
    /**
     * Updates the ship's transform based on physics
     */
    private void updateShipState() {
        ship.setPosition(position);
        ship.setOrientation(orientation);
        ship.setVelocity(velocity);
    }
    
    /**
     * Calculates ship properties from components
     */
    private void calculateShipProperties() {
        mass = 0;
        centerOfMass.set(0, 0, 0);
        
        // Sum up component masses and calculate center of mass
        List<ShipComponent> components = ship.getAllComponents();
        for (ShipComponent component : components) {
            float componentMass = component.getMass();
            mass += componentMass;
            
            Vector3f weightedPos = new Vector3f(component.getPosition()).mul(componentMass);
            centerOfMass.add(weightedPos);
        }
        
        if (mass > 0) {
            centerOfMass.div(mass);
        }
        
        // Get ship type properties
        ShipType shipType = ship.getShipType();
        length = shipType.getLength();
        width = shipType.getWidth();
        height = shipType.getHeight();
        draft = shipType.getDraft();
        
        // Calculate center of buoyancy (typically below center of mass)
        centerOfBuoyancy.set(centerOfMass);
        centerOfBuoyancy.y -= height * 0.3f;
    }
    
    private float calculateDisplacedVolume(float submersionRatio) {
        // Simplified hull shape - assume ship cross-section is roughly elliptical
        float hullVolume = length * width * height * 0.6f; // 60% of bounding box
        return hullVolume * submersionRatio;
    }
    
    private void calculateStability(float submersionRatio) {
        // Metacentric height calculation (simplified)
        float waterlineArea = length * width * submersionRatio;
        float momentOfArea = (length * width * width * width) / 12.0f * submersionRatio;
        
        if (displacedVolume > 0) {
            float metacentricRadius = momentOfArea / displacedVolume;
            float buoyancyCenter = centerOfBuoyancy.y;
            float metacentricHeight = buoyancyCenter + metacentricRadius - centerOfMass.y;
            
            stability = Math.max(0, metacentricHeight);
        } else {
            stability = 0;
        }
    }
    
    private Vector3f calculateRestoringTorque() {
        // Calculate restoring torque based on ship's roll and pitch
        Vector3f restoring = new Vector3f();
        
        // Get current roll and pitch from orientation
        Vector3f angles = new Vector3f();
        orientation.getEulerAnglesXYZ(angles);
        float roll = angles.z;  // Z-axis rotation
        float pitch = angles.x; // X-axis rotation
        
        // Restoring torque proportional to angle and stability
        restoring.x = -pitch * stability * mass * GRAVITY * 0.1f;
        restoring.z = -roll * stability * mass * GRAVITY * 0.1f;
        
        return restoring;
    }
    
    private Vector3f calculateSailThrust() {
        Vector3f thrust = new Vector3f();
        
        // Get wind velocity
        Vector3f wind = oceanPhysics.getWindVelocity(position);
        
        // Get sail components
        for (ShipComponent component : ship.getAllComponents()) {
            if (component instanceof SailComponent) {
                SailComponent sail = (SailComponent) component;
                if (sail.getCurrentDeployment() > 0 && !sail.isDestroyed()) {
                    // Update sail wind conditions
                    sail.updateWind(wind, wind.length());
                    
                    // Get thrust magnitude and convert to vector
                    float thrustMagnitude = sail.getThrust();
                    
                    // Calculate thrust direction (simplified - forward direction)
                    Vector3f forward = new Vector3f(0, 0, 1);
                    orientation.transform(forward);
                    
                    Vector3f sailThrust = new Vector3f(forward).mul(thrustMagnitude);
                    thrust.add(sailThrust);
                }
            }
        }
        
        return thrust;
    }
    
    private Vector3f calculateEngineThrust() {
        Vector3f thrust = new Vector3f();
        
        // Get engine components
        for (ShipComponent component : ship.getAllComponents()) {
            if (component instanceof EngineComponent) {
                EngineComponent engine = (EngineComponent) component;
                if (engine.isRunning() && !engine.isDestroyed()) {
                    // Get thrust magnitude and convert to vector
                    float thrustMagnitude = engine.getThrustForce();
                    
                    // Calculate thrust direction (simplified - forward direction)
                    Vector3f forward = new Vector3f(0, 0, 1);
                    orientation.transform(forward);
                    
                    Vector3f engineThrust = new Vector3f(forward).mul(thrustMagnitude);
                    thrust.add(engineThrust);
                }
            }
        }
        
        return thrust;
    }
    
    private Vector3f calculateWaterDrag() {
        // Drag = 0.5 * ρ * v² * Cd * A
        float speed = velocity.length();
        if (speed < 0.01f) return new Vector3f();
        
        float dragMagnitude = 0.5f * WATER_DENSITY * speed * speed * DRAG_COEFFICIENT * 
                             (length * draft); // Approximate underwater surface area
        
        Vector3f dragDirection = new Vector3f(velocity).normalize().negate();
        return new Vector3f(dragDirection).mul(dragMagnitude);
    }
    
    private Vector3f calculateAirDrag() {
        float speed = velocity.length();
        if (speed < 0.01f) return new Vector3f();
        
        float dragMagnitude = 0.5f * AIR_DENSITY * speed * speed * DRAG_COEFFICIENT * 
                             (length * height * 0.3f); // Approximate above-water area
        
        Vector3f dragDirection = new Vector3f(velocity).normalize().negate();
        return new Vector3f(dragDirection).mul(dragMagnitude);
    }
    
    private Vector3f calculateSailWindForce(Vector3f relativeWind) {
        Vector3f force = new Vector3f();
        
        // This would be calculated by individual sail components
        // For now, return zero as sails handle their own wind forces
        
        return force;
    }
    
    private Vector3f calculateHullWindForce(Vector3f relativeWind) {
        // Wind force on the hull above water
        float windSpeed = relativeWind.length();
        if (windSpeed < 0.1f) return new Vector3f();
        
        float windArea = length * (height - draft) * 0.5f; // Approximate side area
        float forceMagnitude = 0.5f * AIR_DENSITY * windSpeed * windSpeed * 0.8f * windArea;
        
        Vector3f windDirection = new Vector3f(relativeWind).normalize();
        return new Vector3f(windDirection).mul(forceMagnitude);
    }
    
    private Vector3f calculateWaveForceAtPoint(float x, float z, float waveHeight, Vector3f waveVel) {
        Vector3f force = new Vector3f();
        
        // Simplified wave force calculation
        float waveSpeed = waveVel.length();
        float forceMagnitude = WATER_DENSITY * waveSpeed * waveSpeed * 0.1f;
        
        // Force direction based on wave velocity
        if (waveSpeed > 0.01f) {
            force.set(waveVel).normalize().mul(forceMagnitude);
        }
        
        return force;
    }
    
    private Vector3f calculateWaveTorque() {
        Vector3f torque = new Vector3f();
        
        if (waveSystem == null) return torque;
        
        // Calculate wave slope at ship position for rolling/pitching
        float dx = 0.5f;
        float dz = 0.5f;
        
        float h1 = waveSystem.getWaveHeight(position.x - dx, position.z);
        float h2 = waveSystem.getWaveHeight(position.x + dx, position.z);
        float h3 = waveSystem.getWaveHeight(position.x, position.z - dz);
        float h4 = waveSystem.getWaveHeight(position.x, position.z + dz);
        
        float slopeX = (h2 - h1) / (2 * dx);
        float slopeZ = (h4 - h3) / (2 * dz);
        
        // Convert slope to torque
        torque.x = slopeZ * mass * GRAVITY * 0.1f; // Pitch
        torque.z = -slopeX * mass * GRAVITY * 0.1f; // Roll
        
        return torque;
    }
    
    /**
     * Adds dynamic stability forces based on ship motion and current state
     */
    private void addDynamicStabilityForces() {
        // Add damping forces that oppose angular motion for stability
        Vector3f dampingTorque = new Vector3f(angularVelocity).mul(-stability * 0.5f);
        totalTorque.add(dampingTorque);
        
        // Add restoring forces when ship is tilted
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f shipUp = new Vector3f();
        orientation.transform(up, shipUp);
        
        // Calculate tilt angle and apply restoring torque
        float tiltAngle = (float) Math.acos(Math.max(-1, Math.min(1, shipUp.dot(up))));
        if (tiltAngle > 0.1f) {
            Vector3f restoring = new Vector3f();
            shipUp.cross(up, restoring);
            restoring.mul(tiltAngle * stability * mass * GRAVITY * 0.1f);
            totalTorque.add(restoring);
        }
    }
    
    /**
     * Adds wave-induced motion effects like heaving, rolling, and pitching
     */
    private void addWaveInducedMotion(Vector3f waveForce, Vector3f waveTorque) {
        // Add heaving motion (vertical oscillation)
        float waveFrequency = 0.5f; // Typical ocean wave frequency
        float time = System.currentTimeMillis() * 0.001f;
        float heaveAmplitude = waveForce.length() * 0.01f;
        float heaveMotion = (float) (heaveAmplitude * Math.sin(waveFrequency * time));
        
        Vector3f heaveForce = new Vector3f(0, heaveMotion * mass, 0);
        totalForce.add(heaveForce);
        
        // Add rolling motion damping to prevent excessive rolling
        float rollDamping = Math.max(0.1f, stability);
        Vector3f rollDampingTorque = new Vector3f(angularVelocity.x * -rollDamping, 0, 0);
        totalTorque.add(rollDampingTorque);
        
        // Add pitching motion damping
        Vector3f pitchDampingTorque = new Vector3f(0, 0, angularVelocity.z * -rollDamping);
        totalTorque.add(pitchDampingTorque);
    }
    
    private void updatePerformanceMetrics() {
        // Speed in knots (1 m/s ≈ 1.944 knots)
        speed = velocity.length() * 1.944f;
        
        // Calculate theoretical max speed based on hull length (hull speed)
        maxSpeed = (float) (1.34 * Math.sqrt(length)) * 1.944f;
        
        // Maneuverability based on ship size and current speed
        maneuverability = 1.0f / (1.0f + length * 0.1f + speed * 0.05f);
        
        // Seaworthiness based on stability and current conditions
        float waveHeight = waveSystem != null ? 
            waveSystem.getWaveHeight(position.x, position.z) : 0;
        seaworthiness = stability / (1.0f + waveHeight * 0.5f);
    }
    
    // Getters
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    public Quaternionf getOrientation() { return new Quaternionf(orientation); }
    public Vector3f getAngularVelocity() { return new Vector3f(angularVelocity); }
    
    public float getMass() { return mass; }
    public float getDraft() { return draft; }
    public Vector3f getCenterOfMass() { return new Vector3f(centerOfMass); }
    public float getDisplacedVolume() { return displacedVolume; }
    public float getBuoyantForce() { return buoyantForce; }
    public float getStability() { return stability; }
    
    public float getSpeed() { return speed; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getManeuverability() { return maneuverability; }
    public float getSeaworthiness() { return seaworthiness; }
    
    public Vector3f getTotalForce() { return new Vector3f(totalForce); }
    public Vector3f getThrustForce() { return new Vector3f(thrustForce); }
    public Vector3f getDragForce() { return new Vector3f(dragForce); }
    public Vector3f getWindForce() { return new Vector3f(windForce); }
    public Vector3f getWaveForce() { return new Vector3f(waveForce); }
    
    /**
     * Applies an external force to the ship
     * @param force The force vector to apply
     */
    public void applyExternalForce(Vector3f force) {
        totalForce.add(force);
    }
}