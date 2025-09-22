package com.odyssey.ship;

import com.odyssey.ship.components.*;
import com.odyssey.util.Logger;
import com.odyssey.economy.Currency;
import org.joml.Vector3f;

import java.util.*;

/**
 * Ship builder - handles construction and customization of ships.
 * 
 * <p>This class provides a comprehensive ship construction system that allows players to:
 * <ul>
 *   <li>Design ships by selecting ship types and adding components</li>
 *   <li>Validate ship designs for structural integrity and functionality</li>
 *   <li>Calculate costs and performance metrics</li>
 *   <li>Save and load ship designs as templates</li>
 *   <li>Build complete ships ready for deployment</li>
 * </ul>
 * 
 * <p>The ship builder follows a modular approach where ships are constructed from
 * individual components (hull, sails, engines, cannons, etc.) that can be positioned,
 * upgraded, and configured according to the player's needs.
 * 
 * <p>Key features:
 * <ul>
 *   <li><strong>Component Management:</strong> Add, remove, move, and upgrade ship components</li>
 *   <li><strong>Design Validation:</strong> Check for required components, compatibility, and structural integrity</li>
 *   <li><strong>Cost Calculation:</strong> Real-time cost tracking with currency breakdown</li>
 *   <li><strong>Template System:</strong> Save successful designs for reuse</li>
 *   <li><strong>Performance Analysis:</strong> Calculate speed, maneuverability, and combat effectiveness</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>{@code
 * ShipBuilder builder = new ShipBuilder();
 * Ship ship = builder
 *     .startBuild(ShipType.FRIGATE, "HMS Victory")
 *     .addComponent(ComponentType.HULL, "Main Hull", new Vector3f(0, 0, 0))
 *     .addComponent(ComponentType.SAIL, "Main Sail", new Vector3f(0, 5, 0))
 *     .addComponent(ComponentType.CANNON, "Port Cannons", new Vector3f(-3, 1, 0))
 *     .validateDesign()
 *     .buildShip();
 * }</pre>
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since 1.0
 */
public class ShipBuilder {
    
    // Logger instance
    private final Logger logger;
    
    // Build configuration
    private ShipType shipType;
    private String shipName;
    private ShipType.ShipClass shipClass;
    
    // Component lists
    private final List<ShipComponent> components;
    private final Map<ComponentType, List<ShipComponent>> componentsByType;
    private final Map<String, ShipComponent> componentsByName;
    
    // Build validation
    private final List<String> validationErrors;
    private final List<String> validationWarnings;
    private boolean isValid;
    
    // Cost calculation
    private float totalCost;
    private float totalMass;
    private float totalHealth;
    private final Map<Currency, Float> costBreakdown;
    
    // Build constraints
    private final Map<ComponentType, Integer> componentLimits;
    private final Set<ComponentType> requiredComponents;
    
    // Shipyard capabilities
    private final Set<ShipType> availableShipTypes;
    private final Set<ComponentType> availableComponents;
    private final Map<ComponentType, Integer> maxComponentTier;
    
    public ShipBuilder() {
        this.logger = Logger.getLogger(ShipBuilder.class);
        this.components = new ArrayList<>();
        this.componentsByType = new EnumMap<>(ComponentType.class);
        this.componentsByName = new HashMap<>();
        this.validationErrors = new ArrayList<>();
        this.validationWarnings = new ArrayList<>();
        this.costBreakdown = new EnumMap<>(Currency.class);
        this.componentLimits = new EnumMap<>(ComponentType.class);
        this.requiredComponents = EnumSet.noneOf(ComponentType.class);
        this.availableShipTypes = EnumSet.allOf(ShipType.class);
        this.availableComponents = EnumSet.allOf(ComponentType.class);
        this.maxComponentTier = new EnumMap<>(ComponentType.class);
        
        initializeDefaults();
    }
    
    /**
     * Starts building a new ship of the specified type
     */
    public ShipBuilder startBuild(ShipType shipType, String shipName) {
        // Clear previous build
        clearBuild();
        
        this.shipType = shipType;
        this.shipName = shipName;
        this.shipClass = shipType.getShipClass();
        
        // Set up component limits for this ship type
        setupComponentLimits();
        
        // Add required components automatically
        addRequiredComponents();
        
        logger.debug(Logger.WORLD, "Started building {} '{}' ({})", 
                    shipType.getName(), shipName, shipClass.getDisplayName());
        
        return this;
    }
    
    /**
     * Adds a component to the ship
     */
    public ShipBuilder addComponent(ComponentType componentType, String componentName, 
                                   Vector3f position) {
        if (shipType == null) {
            throw new IllegalStateException("Must start build before adding components");
        }
        
        // Validate component addition
        if (!canAddComponent(componentType)) {
            validationErrors.add("Cannot add " + componentType.getName() + 
                               " - limit exceeded or incompatible");
            return this;
        }
        
        // Create the component
        ShipComponent component = createComponent(componentType, componentName, position);
        if (component == null) {
            validationErrors.add("Failed to create component: " + componentName);
            return this;
        }
        
        // Add to collections
        components.add(component);
        componentsByType.computeIfAbsent(componentType, _ -> new ArrayList<>()).add(component);
        componentsByName.put(componentName, component);
        
        logger.debug(Logger.WORLD, "Added {} '{}' to ship build", componentType.getName(), componentName);
        
        return this;
    }
    
    /**
     * Removes a component from the ship
     */
    public ShipBuilder removeComponent(String componentName) {
        ShipComponent component = componentsByName.get(componentName);
        if (component == null) {
            validationWarnings.add("Component not found: " + componentName);
            return this;
        }
        
        // Check if component is required
        if (requiredComponents.contains(component.getComponentType())) {
            validationErrors.add("Cannot remove required component: " + componentName);
            return this;
        }
        
        // Remove from collections
        components.remove(component);
        componentsByType.get(component.getComponentType()).remove(component);
        componentsByName.remove(componentName);
        
        logger.debug(Logger.WORLD, "Removed component '{}' from ship build", componentName);
        
        return this;
    }
    
    /**
     * Moves a component to a new position
     */
    public ShipBuilder moveComponent(String componentName, Vector3f newPosition) {
        ShipComponent component = componentsByName.get(componentName);
        if (component == null) {
            validationWarnings.add("Component not found: " + componentName);
            return this;
        }
        
        // Validate new position
        if (!isValidPosition(component.getComponentType(), newPosition)) {
            validationErrors.add("Invalid position for component: " + componentName);
            return this;
        }
        
        component.setPosition(newPosition);
        logger.debug(Logger.WORLD, "Moved component '{}' to position {}", componentName, newPosition);
        
        return this;
    }
    
    /**
     * Upgrades a component to the specified level
     */
    public ShipBuilder upgradeComponent(String componentName, int upgradeLevel) {
        ShipComponent component = componentsByName.get(componentName);
        if (component == null) {
            validationWarnings.add("Component not found: " + componentName);
            return this;
        }
        
        // Check if upgrade is available
        int maxTier = maxComponentTier.getOrDefault(component.getComponentType(), 5);
        if (upgradeLevel > maxTier) {
            validationErrors.add("Upgrade level " + upgradeLevel + 
                               " not available for " + componentName);
            return this;
        }
        
        component.setUpgradeLevel(upgradeLevel);
        logger.debug(Logger.WORLD, "Upgraded component '{}' to level {}", componentName, upgradeLevel);
        
        return this;
    }
    
    /**
     * Validates the current ship design
     */
    public boolean validateDesign() {
        validationErrors.clear();
        validationWarnings.clear();
        
        // Check basic requirements
        validateBasicRequirements();
        
        // Check component compatibility
        validateComponentCompatibility();
        
        // Check structural integrity
        validateStructuralIntegrity();
        
        // Check balance and performance
        validatePerformance();
        
        // Calculate costs
        calculateCosts();
        
        isValid = validationErrors.isEmpty();
        
        if (isValid) {
            logger.debug(Logger.WORLD, "Ship design '{}' validated successfully", shipName);
        } else {
            logger.debug(Logger.WORLD, "Ship design '{}' has {} errors", shipName, validationErrors.size());
        }
        
        return isValid;
    }
    
    /**
     * Builds the ship if design is valid
     */
    public Ship buildShip() {
        if (!validateDesign()) {
            throw new IllegalStateException("Cannot build ship - design has errors: " + 
                                          String.join(", ", validationErrors));
        }
        
        // Create the ship
        Ship ship = new Ship(shipName, shipType, new Vector3f(0, 0, 0));
        
        // Add all components to the ship
        for (ShipComponent component : components) {
            ship.addComponent(component);
        }
        
        // Initialize ship systems
        ship.initializeSystems();
        
        logger.debug(Logger.WORLD, "Successfully built ship '{}' ({})", shipName, shipType.getName());
        
        return ship;
    }
    
    /**
     * Gets a preview of the ship without building it
     */
    public ShipPreview getPreview() {
        validateDesign();
        
        return new ShipPreview(
            shipType, shipName, shipClass,
            new ArrayList<>(components),
            totalCost, totalMass, totalHealth,
            new HashMap<>(costBreakdown),
            new ArrayList<>(validationErrors),
            new ArrayList<>(validationWarnings),
            isValid
        );
    }
    
    /**
     * Saves the current design as a template
     */
    public ShipTemplate saveAsTemplate(String templateName) {
        validateDesign();
        
        Map<ComponentType, List<ComponentTemplate>> componentTemplates = new HashMap<>();
        
        for (Map.Entry<ComponentType, List<ShipComponent>> entry : componentsByType.entrySet()) {
            List<ComponentTemplate> templates = new ArrayList<>();
            for (ShipComponent component : entry.getValue()) {
                templates.add(new ComponentTemplate(
                    component.getComponentType(),
                    component.getName(),
                    new Vector3f(component.getPosition()),
                    component.getUpgradeLevel()
                ));
            }
            componentTemplates.put(entry.getKey(), templates);
        }
        
        return new ShipTemplate(templateName, shipType, componentTemplates, totalCost);
    }
    
    /**
     * Loads a ship design from a template
     */
    public ShipBuilder loadFromTemplate(ShipTemplate template, String newShipName) {
        startBuild(template.getShipType(), newShipName);
        
        for (Map.Entry<ComponentType, List<ComponentTemplate>> entry : 
             template.getComponentTemplates().entrySet()) {
            
            for (ComponentTemplate compTemplate : entry.getValue()) {
                addComponent(compTemplate.getComponentType(), 
                           compTemplate.getName(), 
                           compTemplate.getPosition());
                
                if (compTemplate.getUpgradeLevel() > 0) {
                    upgradeComponent(compTemplate.getName(), compTemplate.getUpgradeLevel());
                }
            }
        }
        
        logger.debug(Logger.WORLD, "Loaded ship design from template '{}'", template.getName());
        
        return this;
    }
    
    private void initializeDefaults() {
        // Set default component limits
        componentLimits.put(ComponentType.HULL, 1);
        componentLimits.put(ComponentType.SAIL, 6);
        componentLimits.put(ComponentType.ENGINE, 2);
        componentLimits.put(ComponentType.CANNON, 20);
        componentLimits.put(ComponentType.RUDDER, 1);
        componentLimits.put(ComponentType.ANCHOR, 2);
        
        // Set required components
        requiredComponents.add(ComponentType.HULL);
        requiredComponents.add(ComponentType.RUDDER);
        
        // Set default max tiers
        for (ComponentType type : ComponentType.values()) {
            maxComponentTier.put(type, 3);
        }
    }
    
    private void clearBuild() {
        components.clear();
        componentsByType.clear();
        componentsByName.clear();
        validationErrors.clear();
        validationWarnings.clear();
        costBreakdown.clear();
        
        shipType = null;
        shipName = null;
        shipClass = null;
        totalCost = 0.0f;
        totalMass = 0.0f;
        totalHealth = 0.0f;
        isValid = false;
    }
    
    private void setupComponentLimits() {
        // Adjust limits based on ship type
        switch (shipClass) {
            case LIGHT:
                componentLimits.put(ComponentType.CANNON, 8);
                componentLimits.put(ComponentType.SAIL, 4);
                break;
            case MEDIUM:
                componentLimits.put(ComponentType.CANNON, 16);
                componentLimits.put(ComponentType.SAIL, 6);
                break;
            case HEAVY:
                componentLimits.put(ComponentType.CANNON, 32);
                componentLimits.put(ComponentType.SAIL, 8);
                break;
            case MERCHANT:
            case PIRATE:
            case SUPERNATURAL:
                // Special ships have unique limits
                componentLimits.put(ComponentType.CANNON, 12);
                componentLimits.put(ComponentType.ENGINE, 1);
                break;
        }
    }
    
    private void addRequiredComponents() {
        // Add hull component
        addComponent(ComponentType.HULL, "Main Hull", new Vector3f(0, 0, 0));
        
        // Add rudder
        addComponent(ComponentType.RUDDER, "Main Rudder", new Vector3f(0, -2, -10));
        
        // Add basic sail for non-steam ships
        if (shipType != ShipType.MAN_OF_WAR && shipType != ShipType.SHIP_OF_THE_LINE) {
            addComponent(ComponentType.SAIL, "Main Sail", new Vector3f(0, 5, 0));
        }
    }
    
    private boolean canAddComponent(ComponentType componentType) {
        // Check if component type is available
        if (!availableComponents.contains(componentType)) {
            return false;
        }
        
        // Check component limits
        int currentCount = componentsByType.getOrDefault(componentType, new ArrayList<>()).size();
        int limit = componentLimits.getOrDefault(componentType, Integer.MAX_VALUE);
        
        if (currentCount >= limit) {
            return false;
        }
        
        // Check ship type compatibility
        return componentType.isCompatibleWith(shipType);
    }
    
    private ShipComponent createComponent(ComponentType componentType, String name, Vector3f position) {
        switch (componentType) {
            case HULL:
                return new HullComponent(name, position, HullComponent.HullMaterial.WOOD, 1.0f);
            case SAIL:
                return new SailComponent(name, position, SailComponent.SailType.SQUARE_SAIL, 1);
            case ENGINE:
                return new EngineComponent(name, position, EngineComponent.EngineType.MEDIUM_STEAM);
            case CANNON:
                return new CannonComponent(name, position, CannonComponent.CannonType.MEDIUM_CANNON);
            // TODO: Add other component types
            default:
                logger.debug(Logger.WORLD, "Component type {} not implemented yet", componentType.getName());
                return null;
        }
    }
    
    private boolean isValidPosition(ComponentType componentType, Vector3f position) {
        // Basic position validation - can be expanded
        
        // Components can't be too far from center
        float maxDistance = shipType.getLength() / 2.0f + 5.0f;
        if (position.length() > maxDistance) {
            return false;
        }
        
        // Hull must be at center
        if (componentType == ComponentType.HULL && position.length() > 2.0f) {
            return false;
        }
        
        // Engines should be below deck
        if (componentType == ComponentType.ENGINE && position.y > 0) {
            return false;
        }
        
        // Sails should be above deck
        if (componentType == ComponentType.SAIL && position.y < 2.0f) {
            return false;
        }
        
        return true;
    }
    
    private void validateBasicRequirements() {
        // Check required components
        for (ComponentType required : requiredComponents) {
            if (!componentsByType.containsKey(required) || 
                componentsByType.get(required).isEmpty()) {
                validationErrors.add("Missing required component: " + required.getName());
            }
        }
        
        // Check ship name
        if (shipName == null || shipName.trim().isEmpty()) {
            validationErrors.add("Ship name cannot be empty");
        }
        
        // Check minimum components for functionality
        boolean hasPropulsion = componentsByType.containsKey(ComponentType.SAIL) ||
                               componentsByType.containsKey(ComponentType.ENGINE);
        if (!hasPropulsion) {
            validationErrors.add("Ship needs propulsion (sails or engine)");
        }
    }
    
    private void validateComponentCompatibility() {
        for (ShipComponent component : components) {
            if (!component.getComponentType().isCompatibleWith(shipType)) {
                validationErrors.add("Component " + component.getName() + 
                                   " is not compatible with " + shipType.getName());
            }
        }
    }
    
    private void validateStructuralIntegrity() {
        // Check mass distribution
        float totalMass = 0.0f;
        Vector3f centerOfMass = new Vector3f();
        
        for (ShipComponent component : components) {
            float mass = component.getMass();
            totalMass += mass;
            centerOfMass.add(new Vector3f(component.getPosition()).mul(mass));
        }
        
        if (totalMass > 0) {
            centerOfMass.div(totalMass);
            
            // Center of mass should be near ship center
            if (Math.abs(centerOfMass.x) > 2.0f) {
                validationWarnings.add("Ship may be unbalanced - center of mass offset");
            }
        }
        
        // Check if ship is too heavy
        if (totalMass > shipType.getMaxMass()) {
            validationErrors.add("Ship is overweight: " + totalMass + " > " + shipType.getMaxMass());
        }
    }
    
    private void validatePerformance() {
        // Calculate estimated performance
        float totalSailArea = 0.0f;
        float totalEnginePower = 0.0f;
        
        List<SailComponent> sails = (List<SailComponent>) (List<?>) 
            componentsByType.getOrDefault(ComponentType.SAIL, new ArrayList<>());
        for (SailComponent sail : sails) {
            totalSailArea += sail.getSailArea();
        }
        
        List<EngineComponent> engines = (List<EngineComponent>) (List<?>) 
            componentsByType.getOrDefault(ComponentType.ENGINE, new ArrayList<>());
        for (EngineComponent engine : engines) {
            totalEnginePower += engine.getMaxPower();
        }
        
        // Check if ship has enough propulsion for its mass
        float requiredPropulsion = totalMass * 0.1f; // Rough estimate
        float availablePropulsion = totalSailArea * 0.5f + totalEnginePower * 0.3f;
        
        if (availablePropulsion < requiredPropulsion) {
            validationWarnings.add("Ship may be underpowered for its mass");
        }
    }
    
    private void calculateCosts() {
        totalCost = 0.0f;
        totalMass = 0.0f;
        totalHealth = 0.0f;
        costBreakdown.clear();
        
        for (ShipComponent component : components) {
            // Component base cost
            float componentCost = component.getComponentType().getBaseCost();
            
            // Upgrade cost
            componentCost *= (1.0f + component.getUpgradeLevel() * 0.5f);
            
            totalCost += componentCost;
            totalMass += component.getMass();
            totalHealth += component.getMaxHealth();
            
            // Add to cost breakdown
            Currency currency = Currency.GOLD; // Default currency
            costBreakdown.put(currency, costBreakdown.getOrDefault(currency, 0.0f) + componentCost);
        }
        
        // Add ship type base cost
        totalCost += shipType.getBaseCost();
        costBreakdown.put(Currency.GOLD, 
            costBreakdown.getOrDefault(Currency.GOLD, 0.0f) + shipType.getBaseCost());
    }
    
    // Getters
    public ShipType getShipType() { return shipType; }
    public String getShipName() { return shipName; }
    public List<ShipComponent> getComponents() { return new ArrayList<>(components); }
    public List<String> getValidationErrors() { return new ArrayList<>(validationErrors); }
    public List<String> getValidationWarnings() { return new ArrayList<>(validationWarnings); }
    public boolean isValid() { return isValid; }
    public float getTotalCost() { return totalCost; }
    public float getTotalMass() { return totalMass; }
    public float getTotalHealth() { return totalHealth; }
    public Map<Currency, Float> getCostBreakdown() { return new HashMap<>(costBreakdown); }
    
    /**
     * Gets the available ship types for building
     */
    public Set<ShipType> getAvailableShipTypes() { return availableShipTypes; }
    
    /**
     * Ship preview data class
     */
    public static class ShipPreview {
        private final ShipType shipType;
        private final String shipName;
        private final ShipType.ShipClass shipClass;
        private final List<ShipComponent> components;
        private final float totalCost;
        private final float totalMass;
        private final float totalHealth;
        private final Map<Currency, Float> costBreakdown;
        private final List<String> validationErrors;
        private final List<String> validationWarnings;
        private final boolean isValid;
        
        public ShipPreview(ShipType shipType, String shipName, ShipType.ShipClass shipClass,
                          List<ShipComponent> components, float totalCost, float totalMass,
                          float totalHealth, Map<Currency, Float> costBreakdown,
                          List<String> validationErrors, List<String> validationWarnings,
                          boolean isValid) {
            this.shipType = shipType;
            this.shipName = shipName;
            this.shipClass = shipClass;
            this.components = components;
            this.totalCost = totalCost;
            this.totalMass = totalMass;
            this.totalHealth = totalHealth;
            this.costBreakdown = costBreakdown;
            this.validationErrors = validationErrors;
            this.validationWarnings = validationWarnings;
            this.isValid = isValid;
        }
        
        // Getters
        public ShipType getShipType() { return shipType; }
        public String getShipName() { return shipName; }
        public ShipType.ShipClass getShipClass() { return shipClass; }
        public List<ShipComponent> getComponents() { return components; }
        public float getTotalCost() { return totalCost; }
        public float getTotalMass() { return totalMass; }
        public float getTotalHealth() { return totalHealth; }
        public Map<Currency, Float> getCostBreakdown() { return costBreakdown; }
        public List<String> getValidationErrors() { return validationErrors; }
        public List<String> getValidationWarnings() { return validationWarnings; }
        public boolean isValid() { return isValid; }
    }
    
    /**
     * Ship template for saving/loading designs
     */
    public static class ShipTemplate {
        private final String name;
        private final ShipType shipType;
        private final Map<ComponentType, List<ComponentTemplate>> componentTemplates;
        private final float estimatedCost;
        
        public ShipTemplate(String name, ShipType shipType,
                           Map<ComponentType, List<ComponentTemplate>> componentTemplates,
                           float estimatedCost) {
            this.name = name;
            this.shipType = shipType;
            this.componentTemplates = componentTemplates;
            this.estimatedCost = estimatedCost;
        }
        
        public String getName() { return name; }
        public ShipType getShipType() { return shipType; }
        public Map<ComponentType, List<ComponentTemplate>> getComponentTemplates() { return componentTemplates; }
        public float getEstimatedCost() { return estimatedCost; }
    }
    
    /**
     * Component template for ship templates
     */
    public static class ComponentTemplate {
        private final ComponentType componentType;
        private final String name;
        private final Vector3f position;
        private final int upgradeLevel;
        
        public ComponentTemplate(ComponentType componentType, String name, 
                               Vector3f position, int upgradeLevel) {
            this.componentType = componentType;
            this.name = name;
            this.position = position;
            this.upgradeLevel = upgradeLevel;
        }
        
        public ComponentType getComponentType() { return componentType; }
        public String getName() { return name; }
        public Vector3f getPosition() { return position; }
        public int getUpgradeLevel() { return upgradeLevel; }
    }
}