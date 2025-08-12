package com.odyssey.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Generic capability system for providing extensible functionality to game objects.
 * Allows objects to have modular capabilities that can be added, removed, and queried dynamically.
 */
public class CapabilitySystem {
    private static final Logger logger = LoggerFactory.getLogger(CapabilitySystem.class);
    
    private final Map<Class<? extends Capability>, CapabilityProvider<?>> providers;
    private final Map<Object, Map<Class<? extends Capability>, Capability>> objectCapabilities;
    private final Map<Class<? extends Capability>, Set<CapabilityListener<?>>> listeners;
    private final Set<String> registeredCapabilityNames;
    
    public CapabilitySystem() {
        this.providers = new ConcurrentHashMap<>();
        this.objectCapabilities = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
        this.registeredCapabilityNames = ConcurrentHashMap.newKeySet();
        
        registerDefaultCapabilities();
        logger.info("Capability system initialized");
    }
    
    /**
     * Registers default capabilities
     */
    private void registerDefaultCapabilities() {
        // Register common capabilities
        registerCapability(InventoryCapability.class, InventoryCapability::new);
        registerCapability(EnergyCapability.class, EnergyCapability::new);
        registerCapability(FluidCapability.class, FluidCapability::new);
        registerCapability(ItemHandlerCapability.class, ItemHandlerCapability::new);
        registerCapability(RedstoneCapability.class, RedstoneCapability::new);
        registerCapability(TemperatureCapability.class, TemperatureCapability::new);
        registerCapability(DurabilityCapability.class, DurabilityCapability::new);
        registerCapability(EnchantmentCapability.class, EnchantmentCapability::new);
        registerCapability(ExperienceCapability.class, ExperienceCapability::new);
        registerCapability(HealthCapability.class, HealthCapability::new);
        registerCapability(MovementCapability.class, MovementCapability::new);
        registerCapability(CombatCapability.class, CombatCapability::new);
        registerCapability(CraftingCapability.class, CraftingCapability::new);
        registerCapability(TradingCapability.class, TradingCapability::new);
        registerCapability(NavigationCapability.class, NavigationCapability::new);
        
        logger.debug("Registered {} default capabilities", providers.size());
    }
    
    /**
     * Registers a capability type with its provider
     */
    public <T extends Capability> void registerCapability(Class<T> capabilityClass, Supplier<T> provider) {
        if (capabilityClass == null || provider == null) {
            throw new IllegalArgumentException("Capability class and provider cannot be null");
        }
        
        providers.put(capabilityClass, new CapabilityProvider<>(provider));
        registeredCapabilityNames.add(capabilityClass.getSimpleName());
        
        logger.debug("Registered capability: {}", capabilityClass.getSimpleName());
    }
    
    /**
     * Registers a capability type with a custom provider
     */
    public <T extends Capability> void registerCapability(Class<T> capabilityClass, CapabilityProvider<T> provider) {
        if (capabilityClass == null || provider == null) {
            throw new IllegalArgumentException("Capability class and provider cannot be null");
        }
        
        providers.put(capabilityClass, provider);
        registeredCapabilityNames.add(capabilityClass.getSimpleName());
        
        logger.debug("Registered capability with custom provider: {}", capabilityClass.getSimpleName());
    }
    
    /**
     * Adds a capability to an object
     */
    @SuppressWarnings("unchecked")
    public <T extends Capability> boolean addCapability(Object object, Class<T> capabilityClass) {
        if (object == null || capabilityClass == null) {
            logger.warn("Cannot add capability: object or capability class is null");
            return false;
        }
        
        CapabilityProvider<?> provider = providers.get(capabilityClass);
        if (provider == null) {
            logger.warn("No provider registered for capability: {}", capabilityClass.getSimpleName());
            return false;
        }
        
        try {
            T capability = (T) provider.create();
            capability.setOwner(object);
            
            Map<Class<? extends Capability>, Capability> caps = objectCapabilities.computeIfAbsent(object, k -> new ConcurrentHashMap<>());
            caps.put(capabilityClass, capability);
            
            // Notify listeners
            notifyCapabilityAdded(object, capability);
            
            logger.debug("Added capability {} to object {}", capabilityClass.getSimpleName(), object.getClass().getSimpleName());
            return true;
        } catch (Exception e) {
            logger.error("Failed to create capability {}: {}", capabilityClass.getSimpleName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Adds a capability instance to an object
     */
    public <T extends Capability> boolean addCapability(Object object, T capability) {
        if (object == null || capability == null) {
            logger.warn("Cannot add capability: object or capability is null");
            return false;
        }
        
        capability.setOwner(object);
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.computeIfAbsent(object, k -> new ConcurrentHashMap<>());
        caps.put(capability.getClass(), capability);
        
        // Notify listeners
        notifyCapabilityAdded(object, capability);
        
        logger.debug("Added capability instance {} to object {}", capability.getClass().getSimpleName(), object.getClass().getSimpleName());
        return true;
    }
    
    /**
     * Removes a capability from an object
     */
    @SuppressWarnings("unchecked")
    public <T extends Capability> boolean removeCapability(Object object, Class<T> capabilityClass) {
        if (object == null || capabilityClass == null) {
            return false;
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        if (caps == null) {
            return false;
        }
        
        T capability = (T) caps.remove(capabilityClass);
        if (capability != null) {
            capability.onRemoved();
            
            // Notify listeners
            notifyCapabilityRemoved(object, capability);
            
            // Clean up empty capability map
            if (caps.isEmpty()) {
                objectCapabilities.remove(object);
            }
            
            logger.debug("Removed capability {} from object {}", capabilityClass.getSimpleName(), object.getClass().getSimpleName());
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets a capability from an object
     */
    @SuppressWarnings("unchecked")
    public <T extends Capability> Optional<T> getCapability(Object object, Class<T> capabilityClass) {
        if (object == null || capabilityClass == null) {
            return Optional.empty();
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        if (caps == null) {
            return Optional.empty();
        }
        
        T capability = (T) caps.get(capabilityClass);
        return Optional.ofNullable(capability);
    }
    
    /**
     * Checks if an object has a specific capability
     */
    public boolean hasCapability(Object object, Class<? extends Capability> capabilityClass) {
        if (object == null || capabilityClass == null) {
            return false;
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        return caps != null && caps.containsKey(capabilityClass);
    }
    
    /**
     * Gets all capabilities of an object
     */
    public Collection<Capability> getCapabilities(Object object) {
        if (object == null) {
            return Collections.emptyList();
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        return caps != null ? caps.values() : Collections.emptyList();
    }
    
    /**
     * Gets all capability classes of an object
     */
    public Set<Class<? extends Capability>> getCapabilityTypes(Object object) {
        if (object == null) {
            return Collections.emptySet();
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        return caps != null ? caps.keySet() : Collections.emptySet();
    }
    
    /**
     * Removes all capabilities from an object
     */
    public void removeAllCapabilities(Object object) {
        if (object == null) {
            return;
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.remove(object);
        if (caps != null) {
            for (Capability capability : caps.values()) {
                capability.onRemoved();
                notifyCapabilityRemoved(object, capability);
            }
            
            logger.debug("Removed all capabilities from object {}", object.getClass().getSimpleName());
        }
    }
    
    /**
     * Updates all capabilities for an object
     */
    public void updateCapabilities(Object object) {
        if (object == null) {
            return;
        }
        
        Map<Class<? extends Capability>, Capability> caps = objectCapabilities.get(object);
        if (caps != null) {
            for (Capability capability : caps.values()) {
                try {
                    capability.update();
                } catch (Exception e) {
                    logger.error("Error updating capability {} for object {}: {}", 
                        capability.getClass().getSimpleName(), object.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Registers a capability listener
     */
    @SuppressWarnings("unchecked")
    public <T extends Capability> void addListener(Class<T> capabilityClass, CapabilityListener<T> listener) {
        listeners.computeIfAbsent(capabilityClass, k -> ConcurrentHashMap.newKeySet()).add((CapabilityListener<?>) listener);
        logger.debug("Added listener for capability: {}", capabilityClass.getSimpleName());
    }
    
    /**
     * Unregisters a capability listener
     */
    public <T extends Capability> void removeListener(Class<T> capabilityClass, CapabilityListener<T> listener) {
        Set<CapabilityListener<?>> capListeners = listeners.get(capabilityClass);
        if (capListeners != null) {
            capListeners.remove(listener);
            if (capListeners.isEmpty()) {
                listeners.remove(capabilityClass);
            }
        }
        logger.debug("Removed listener for capability: {}", capabilityClass.getSimpleName());
    }
    
    /**
     * Notifies listeners when a capability is added
     */
    @SuppressWarnings("unchecked")
    private void notifyCapabilityAdded(Object object, Capability capability) {
        Set<CapabilityListener<?>> capListeners = listeners.get(capability.getClass());
        if (capListeners != null) {
            for (CapabilityListener<?> listener : capListeners) {
                try {
                    ((CapabilityListener<Capability>) listener).onCapabilityAdded(object, capability);
                } catch (Exception e) {
                    logger.error("Error in capability listener: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Notifies listeners when a capability is removed
     */
    @SuppressWarnings("unchecked")
    private void notifyCapabilityRemoved(Object object, Capability capability) {
        Set<CapabilityListener<?>> capListeners = listeners.get(capability.getClass());
        if (capListeners != null) {
            for (CapabilityListener<?> listener : capListeners) {
                try {
                    ((CapabilityListener<Capability>) listener).onCapabilityRemoved(object, capability);
                } catch (Exception e) {
                    logger.error("Error in capability listener: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Gets all registered capability names
     */
    public Set<String> getRegisteredCapabilities() {
        return new HashSet<>(registeredCapabilityNames);
    }
    
    /**
     * Checks if a capability is registered
     */
    public boolean isCapabilityRegistered(Class<? extends Capability> capabilityClass) {
        return providers.containsKey(capabilityClass);
    }
    
    /**
     * Gets the number of objects with capabilities
     */
    public int getObjectCount() {
        return objectCapabilities.size();
    }
    
    /**
     * Gets the total number of capability instances
     */
    public int getCapabilityInstanceCount() {
        return objectCapabilities.values().stream().mapToInt(Map::size).sum();
    }
    
    /**
     * Clears all capabilities and providers
     */
    public void clear() {
        // Remove all capabilities
        for (Map<Class<? extends Capability>, Capability> caps : objectCapabilities.values()) {
            for (Capability capability : caps.values()) {
                capability.onRemoved();
            }
        }
        
        objectCapabilities.clear();
        providers.clear();
        listeners.clear();
        registeredCapabilityNames.clear();
        
        logger.info("Cleared all capabilities and providers");
    }
    
    // Base capability interface
    public interface Capability {
        /**
         * Gets the owner of this capability
         */
        Object getOwner();
        
        /**
         * Sets the owner of this capability
         */
        void setOwner(Object owner);
        
        /**
         * Updates the capability
         */
        default void update() {}
        
        /**
         * Called when the capability is removed
         */
        default void onRemoved() {}
        
        /**
         * Gets the capability name
         */
        default String getName() {
            return getClass().getSimpleName();
        }
    }
    
    // Abstract base capability class
    public abstract static class AbstractCapability implements Capability {
        protected Object owner;
        
        @Override
        public Object getOwner() {
            return owner;
        }
        
        @Override
        public void setOwner(Object owner) {
            this.owner = owner;
        }
    }
    
    // Capability provider
    public static class CapabilityProvider<T extends Capability> {
        private final Supplier<T> factory;
        
        public CapabilityProvider(Supplier<T> factory) {
            this.factory = factory;
        }
        
        public T create() {
            return factory.get();
        }
    }
    
    // Capability listener interface
    public interface CapabilityListener<T extends Capability> {
        void onCapabilityAdded(Object object, T capability);
        void onCapabilityRemoved(Object object, T capability);
    }
    
    // Common capability implementations
    public static class InventoryCapability extends AbstractCapability {
        private final Map<String, Integer> items = new ConcurrentHashMap<>();
        private int maxSlots = 27;
        
        public void addItem(String item, int amount) {
            items.merge(item, amount, Integer::sum);
        }
        
        public boolean removeItem(String item, int amount) {
            int current = items.getOrDefault(item, 0);
            if (current >= amount) {
                items.put(item, current - amount);
                if (items.get(item) == 0) {
                    items.remove(item);
                }
                return true;
            }
            return false;
        }
        
        public int getItemCount(String item) {
            return items.getOrDefault(item, 0);
        }
        
        public Map<String, Integer> getItems() {
            return new HashMap<>(items);
        }
        
        public void setMaxSlots(int maxSlots) {
            this.maxSlots = maxSlots;
        }
        
        public int getMaxSlots() {
            return maxSlots;
        }
    }
    
    public static class EnergyCapability extends AbstractCapability {
        private int energy = 0;
        private int maxEnergy = 1000;
        private int transferRate = 100;
        
        public boolean addEnergy(int amount) {
            int newEnergy = Math.min(energy + amount, maxEnergy);
            boolean changed = newEnergy != energy;
            energy = newEnergy;
            return changed;
        }
        
        public boolean removeEnergy(int amount) {
            if (energy >= amount) {
                energy -= amount;
                return true;
            }
            return false;
        }
        
        public int getEnergy() { return energy; }
        public int getMaxEnergy() { return maxEnergy; }
        public int getTransferRate() { return transferRate; }
        
        public void setMaxEnergy(int maxEnergy) { this.maxEnergy = maxEnergy; }
        public void setTransferRate(int transferRate) { this.transferRate = transferRate; }
    }
    
    public static class FluidCapability extends AbstractCapability {
        private final Map<String, Integer> fluids = new ConcurrentHashMap<>();
        private int capacity = 1000;
        
        public boolean addFluid(String fluidType, int amount) {
            int current = fluids.getOrDefault(fluidType, 0);
            int total = fluids.values().stream().mapToInt(Integer::intValue).sum();
            
            if (total + amount <= capacity) {
                fluids.put(fluidType, current + amount);
                return true;
            }
            return false;
        }
        
        public boolean removeFluid(String fluidType, int amount) {
            int current = fluids.getOrDefault(fluidType, 0);
            if (current >= amount) {
                fluids.put(fluidType, current - amount);
                if (fluids.get(fluidType) == 0) {
                    fluids.remove(fluidType);
                }
                return true;
            }
            return false;
        }
        
        public int getFluidAmount(String fluidType) {
            return fluids.getOrDefault(fluidType, 0);
        }
        
        public Map<String, Integer> getFluids() {
            return new HashMap<>(fluids);
        }
        
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
    }
    
    public static class ItemHandlerCapability extends AbstractCapability {
        private final List<String> slots = new ArrayList<>();
        private int maxSlots = 9;
        
        public boolean insertItem(String item) {
            if (slots.size() < maxSlots) {
                slots.add(item);
                return true;
            }
            return false;
        }
        
        public String extractItem(int slot) {
            if (slot >= 0 && slot < slots.size()) {
                return slots.remove(slot);
            }
            return null;
        }
        
        public String getItem(int slot) {
            if (slot >= 0 && slot < slots.size()) {
                return slots.get(slot);
            }
            return null;
        }
        
        public List<String> getItems() {
            return new ArrayList<>(slots);
        }
        
        public int getSlotCount() { return slots.size(); }
        public int getMaxSlots() { return maxSlots; }
        public void setMaxSlots(int maxSlots) { this.maxSlots = maxSlots; }
    }
    
    public static class RedstoneCapability extends AbstractCapability {
        private int powerLevel = 0;
        private boolean powered = false;
        
        public void setPowerLevel(int level) {
            this.powerLevel = Math.max(0, Math.min(15, level));
            this.powered = this.powerLevel > 0;
        }
        
        public int getPowerLevel() { return powerLevel; }
        public boolean isPowered() { return powered; }
    }
    
    public static class TemperatureCapability extends AbstractCapability {
        private float temperature = 20.0f; // Celsius
        private float heatCapacity = 1.0f;
        
        public void addHeat(float heat) {
            temperature += heat / heatCapacity;
        }
        
        public void removeHeat(float heat) {
            temperature -= heat / heatCapacity;
        }
        
        public float getTemperature() { return temperature; }
        public void setTemperature(float temperature) { this.temperature = temperature; }
        public float getHeatCapacity() { return heatCapacity; }
        public void setHeatCapacity(float heatCapacity) { this.heatCapacity = heatCapacity; }
    }
    
    public static class DurabilityCapability extends AbstractCapability {
        private int durability = 100;
        private int maxDurability = 100;
        
        public boolean damage(int amount) {
            durability = Math.max(0, durability - amount);
            return durability == 0;
        }
        
        public void repair(int amount) {
            durability = Math.min(maxDurability, durability + amount);
        }
        
        public int getDurability() { return durability; }
        public int getMaxDurability() { return maxDurability; }
        public void setMaxDurability(int maxDurability) { this.maxDurability = maxDurability; }
        public float getDurabilityPercentage() { return (float) durability / maxDurability; }
    }
    
    public static class EnchantmentCapability extends AbstractCapability {
        private final Map<String, Integer> enchantments = new ConcurrentHashMap<>();
        
        public void addEnchantment(String enchantment, int level) {
            enchantments.put(enchantment, level);
        }
        
        public void removeEnchantment(String enchantment) {
            enchantments.remove(enchantment);
        }
        
        public int getEnchantmentLevel(String enchantment) {
            return enchantments.getOrDefault(enchantment, 0);
        }
        
        public Map<String, Integer> getEnchantments() {
            return new HashMap<>(enchantments);
        }
    }
    
    public static class ExperienceCapability extends AbstractCapability {
        private int experience = 0;
        private int level = 0;
        
        public void addExperience(int amount) {
            experience += amount;
            updateLevel();
        }
        
        private void updateLevel() {
            // Simple level calculation
            level = (int) Math.sqrt(experience / 100.0);
        }
        
        public int getExperience() { return experience; }
        public int getLevel() { return level; }
    }
    
    public static class HealthCapability extends AbstractCapability {
        private float health = 100.0f;
        private float maxHealth = 100.0f;
        
        public void damage(float amount) {
            health = Math.max(0, health - amount);
        }
        
        public void heal(float amount) {
            health = Math.min(maxHealth, health + amount);
        }
        
        public float getHealth() { return health; }
        public float getMaxHealth() { return maxHealth; }
        public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
        public boolean isDead() { return health <= 0; }
    }
    
    public static class MovementCapability extends AbstractCapability {
        private float speed = 1.0f;
        private boolean canFly = false;
        private boolean canSwim = true;
        
        public float getSpeed() { return speed; }
        public void setSpeed(float speed) { this.speed = speed; }
        public boolean canFly() { return canFly; }
        public void setCanFly(boolean canFly) { this.canFly = canFly; }
        public boolean canSwim() { return canSwim; }
        public void setCanSwim(boolean canSwim) { this.canSwim = canSwim; }
    }
    
    public static class CombatCapability extends AbstractCapability {
        private float attackDamage = 1.0f;
        private float attackSpeed = 1.0f;
        private float defense = 0.0f;
        
        public float getAttackDamage() { return attackDamage; }
        public void setAttackDamage(float attackDamage) { this.attackDamage = attackDamage; }
        public float getAttackSpeed() { return attackSpeed; }
        public void setAttackSpeed(float attackSpeed) { this.attackSpeed = attackSpeed; }
        public float getDefense() { return defense; }
        public void setDefense(float defense) { this.defense = defense; }
    }
    
    public static class CraftingCapability extends AbstractCapability {
        private final Set<String> knownRecipes = ConcurrentHashMap.newKeySet();
        
        public void learnRecipe(String recipe) {
            knownRecipes.add(recipe);
        }
        
        public void forgetRecipe(String recipe) {
            knownRecipes.remove(recipe);
        }
        
        public boolean knowsRecipe(String recipe) {
            return knownRecipes.contains(recipe);
        }
        
        public Set<String> getKnownRecipes() {
            return new HashSet<>(knownRecipes);
        }
    }
    
    public static class TradingCapability extends AbstractCapability {
        private final Map<String, Integer> prices = new ConcurrentHashMap<>();
        private boolean canTrade = true;
        
        public void setPrice(String item, int price) {
            prices.put(item, price);
        }
        
        public int getPrice(String item) {
            return prices.getOrDefault(item, -1);
        }
        
        public Map<String, Integer> getPrices() {
            return new HashMap<>(prices);
        }
        
        public boolean canTrade() { return canTrade; }
        public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
    }
    
    public static class NavigationCapability extends AbstractCapability {
        private float x, y, z;
        private float targetX, targetY, targetZ;
        private boolean hasTarget = false;
        
        public void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public void setTarget(float x, float y, float z) {
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
            this.hasTarget = true;
        }
        
        public void clearTarget() {
            this.hasTarget = false;
        }
        
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        public float getTargetX() { return targetX; }
        public float getTargetY() { return targetY; }
        public float getTargetZ() { return targetZ; }
        public boolean hasTarget() { return hasTarget; }
        
        public float getDistanceToTarget() {
            if (!hasTarget) return Float.MAX_VALUE;
            float dx = targetX - x;
            float dy = targetY - y;
            float dz = targetZ - z;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}