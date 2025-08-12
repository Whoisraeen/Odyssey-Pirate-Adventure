package com.odyssey.items;

import java.util.*;

/**
 * Base class for all items in the game.
 * Defines properties and behavior for items.
 */
public class Item {
    private final String registryName;
    private final Properties properties;
    
    // Cached values
    private transient String displayName;
    private transient List<String> defaultLore;
    
    public Item(String registryName, Properties properties) {
        this.registryName = registryName;
        this.properties = properties;
    }
    
    /**
     * Gets the registry name of this item
     */
    public String getRegistryName() {
        return registryName;
    }
    
    /**
     * Gets the display name of this item
     */
    public String getDisplayName() {
        if (displayName == null) {
            displayName = properties.displayName != null ? properties.displayName : 
                         formatRegistryName(registryName);
        }
        return displayName;
    }
    
    /**
     * Gets the default lore for this item
     */
    public List<String> getDefaultLore() {
        if (defaultLore == null) {
            defaultLore = properties.lore != null ? new ArrayList<>(properties.lore) : new ArrayList<>();
        }
        return new ArrayList<>(defaultLore);
    }
    
    /**
     * Gets the maximum stack size
     */
    public int getMaxStackSize() {
        return properties.maxStackSize;
    }
    
    /**
     * Gets the maximum damage this item can take
     */
    public int getMaxDamage() {
        return properties.maxDamage;
    }
    
    /**
     * Checks if this item is damageable
     */
    public boolean isDamageable() {
        return properties.maxDamage > 0;
    }
    
    /**
     * Gets the item category
     */
    public ItemCategory getCategory() {
        return properties.category;
    }
    
    /**
     * Gets the item rarity
     */
    public ItemRarity getRarity() {
        return properties.rarity;
    }
    
    /**
     * Checks if this item is edible
     */
    public boolean isEdible() {
        return properties.foodProperties != null;
    }
    
    /**
     * Gets the food properties
     */
    public FoodProperties getFoodProperties() {
        return properties.foodProperties;
    }
    
    /**
     * Checks if this item is a tool
     */
    public boolean isTool() {
        return properties.toolProperties != null;
    }
    
    /**
     * Gets the tool properties
     */
    public ToolProperties getToolProperties() {
        return properties.toolProperties;
    }
    
    /**
     * Checks if this item is armor
     */
    public boolean isArmor() {
        return properties.armorProperties != null;
    }
    
    /**
     * Gets the armor properties
     */
    public ArmorProperties getArmorProperties() {
        return properties.armorProperties;
    }
    
    /**
     * Checks if this item is a weapon
     */
    public boolean isWeapon() {
        return properties.weaponProperties != null;
    }
    
    /**
     * Gets the weapon properties
     */
    public WeaponProperties getWeaponProperties() {
        return properties.weaponProperties;
    }
    
    /**
     * Gets the item tags
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(properties.tags);
    }
    
    /**
     * Checks if this item has a specific tag
     */
    public boolean hasTag(String tag) {
        return properties.tags.contains(tag);
    }
    
    /**
     * Gets the burn time for this item (fuel value)
     */
    public int getBurnTime() {
        return properties.burnTime;
    }
    
    /**
     * Checks if this item can be used as fuel
     */
    public boolean isFuel() {
        return properties.burnTime > 0;
    }
    
    /**
     * Gets the enchantability of this item
     */
    public int getEnchantability() {
        return properties.enchantability;
    }
    
    /**
     * Checks if this item can be enchanted
     */
    public boolean isEnchantable() {
        return properties.enchantability > 0;
    }
    
    /**
     * Gets the repair material for this item
     */
    public String getRepairMaterial() {
        return properties.repairMaterial;
    }
    
    /**
     * Checks if this item can be repaired
     */
    public boolean isRepairable() {
        return properties.repairMaterial != null && isDamageable();
    }
    
    /**
     * Called when this item is used
     */
    public ItemUseResult onUse(ItemStack stack, Object user, Object world, Object target) {
        return ItemUseResult.PASS;
    }
    
    /**
     * Called when this item is right-clicked
     */
    public ItemUseResult onRightClick(ItemStack stack, Object user, Object world) {
        return ItemUseResult.PASS;
    }
    
    /**
     * Called when this item is used to break a block
     */
    public boolean onBlockBreak(ItemStack stack, Object block, Object world, Object pos, Object player) {
        if (isDamageable()) {
            stack.damageItem(1);
        }
        return true;
    }
    
    /**
     * Called when this item is used to attack an entity
     */
    public boolean onAttackEntity(ItemStack stack, Object target, Object attacker) {
        if (isDamageable()) {
            stack.damageItem(2);
        }
        return true;
    }
    
    /**
     * Called every tick while this item is in inventory
     */
    public void onInventoryTick(ItemStack stack, Object world, Object entity, int slot, boolean selected) {
        // Override in subclasses
    }
    
    /**
     * Called when this item is crafted
     */
    public void onCrafted(ItemStack stack, Object world, Object player) {
        // Override in subclasses
    }
    
    /**
     * Gets the mining speed for a specific block
     */
    public float getMiningSpeed(ItemStack stack, Object blockState) {
        if (isTool() && getToolProperties() != null) {
            return getToolProperties().getMiningSpeed(blockState);
        }
        return 1.0f;
    }
    
    /**
     * Checks if this item can harvest a specific block
     */
    public boolean canHarvest(Object blockState) {
        if (isTool() && getToolProperties() != null) {
            return getToolProperties().canHarvest(blockState);
        }
        return false;
    }
    
    /**
     * Checks if this item has a container item
     */
    public boolean hasContainerItem() {
        // Override in subclasses that have container items
        return false;
    }
    
    /**
     * Gets the container item for this item
     */
    public Item getContainerItem() {
        // Override in subclasses that have container items
        return null;
    }
    
    /**
     * Formats a registry name into a display name
     */
    private String formatRegistryName(String registryName) {
        String[] parts = registryName.split(":");
        String name = parts.length > 1 ? parts[1] : parts[0];
        
        // Convert snake_case to Title Case
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }
    
    @Override
    public String toString() {
        return "Item{" + registryName + "}";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Item)) return false;
        Item other = (Item) obj;
        return Objects.equals(registryName, other.registryName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(registryName);
    }
    
    /**
     * Item properties builder class
     */
    public static class Properties {
        private String displayName;
        private List<String> lore;
        private int maxStackSize = 64;
        private int maxDamage = 0;
        private ItemCategory category = ItemCategory.MISC;
        private ItemRarity rarity = ItemRarity.COMMON;
        private FoodProperties foodProperties;
        private ToolProperties toolProperties;
        private ArmorProperties armorProperties;
        private WeaponProperties weaponProperties;
        private Set<String> tags = new HashSet<>();
        private int burnTime = 0;
        private int enchantability = 0;
        private String repairMaterial;
        
        public Properties displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public Properties lore(String... lore) {
            this.lore = Arrays.asList(lore);
            return this;
        }
        
        public Properties lore(List<String> lore) {
            this.lore = new ArrayList<>(lore);
            return this;
        }
        
        public Properties maxStackSize(int maxStackSize) {
            this.maxStackSize = Math.max(1, Math.min(64, maxStackSize));
            return this;
        }
        
        public Properties maxDamage(int maxDamage) {
            this.maxDamage = Math.max(0, maxDamage);
            if (maxDamage > 0) {
                this.maxStackSize = 1; // Damageable items don't stack
            }
            return this;
        }
        
        public Properties category(ItemCategory category) {
            this.category = category;
            return this;
        }
        
        public Properties rarity(ItemRarity rarity) {
            this.rarity = rarity;
            return this;
        }
        
        public Properties food(FoodProperties foodProperties) {
            this.foodProperties = foodProperties;
            return this;
        }
        
        public Properties tool(ToolProperties toolProperties) {
            this.toolProperties = toolProperties;
            return this;
        }
        
        public Properties armor(ArmorProperties armorProperties) {
            this.armorProperties = armorProperties;
            return this;
        }
        
        public Properties weapon(WeaponProperties weaponProperties) {
            this.weaponProperties = weaponProperties;
            return this;
        }
        
        public Properties tag(String tag) {
            this.tags.add(tag);
            return this;
        }
        
        public Properties tags(String... tags) {
            Collections.addAll(this.tags, tags);
            return this;
        }
        
        public Properties burnTime(int burnTime) {
            this.burnTime = Math.max(0, burnTime);
            return this;
        }
        
        public Properties enchantability(int enchantability) {
            this.enchantability = Math.max(0, enchantability);
            return this;
        }
        
        public Properties repairMaterial(String repairMaterial) {
            this.repairMaterial = repairMaterial;
            return this;
        }
    }
    
    /**
     * Item categories
     */
    public enum ItemCategory {
        BUILDING_BLOCKS,
        DECORATIONS,
        REDSTONE,
        TRANSPORTATION,
        MISC,
        FOOD,
        TOOLS,
        COMBAT,
        BREWING,
        MATERIALS,
        MARITIME,
        TREASURE
    }
    
    /**
     * Item rarities
     */
    public enum ItemRarity {
        COMMON(0xFFFFFF),
        UNCOMMON(0xFFFF55),
        RARE(0x55FFFF),
        EPIC(0xFF55FF),
        LEGENDARY(0xFFAA00),
        MYTHIC(0xFF5555);
        
        private final int color;
        
        ItemRarity(int color) {
            this.color = color;
        }
        
        public int getColor() {
            return color;
        }
    }
    
    /**
     * Item use results
     */
    public enum ItemUseResult {
        SUCCESS,    // Action succeeded
        CONSUME,    // Action succeeded and item was consumed
        FAIL,       // Action failed
        PASS        // Action not handled, pass to next handler
    }
    
    /**
     * Food properties
     */
    public static class FoodProperties {
        private final int nutrition;
        private final float saturation;
        private final boolean isMeat;
        private final boolean canAlwaysEat;
        private final int eatTime;
        private final List<FoodEffect> effects;
        
        public FoodProperties(int nutrition, float saturation, boolean isMeat, boolean canAlwaysEat, int eatTime, List<FoodEffect> effects) {
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.isMeat = isMeat;
            this.canAlwaysEat = canAlwaysEat;
            this.eatTime = eatTime;
            this.effects = effects != null ? new ArrayList<>(effects) : new ArrayList<>();
        }
        
        public int getNutrition() { return nutrition; }
        public float getSaturation() { return saturation; }
        public boolean isMeat() { return isMeat; }
        public boolean canAlwaysEat() { return canAlwaysEat; }
        public int getEatTime() { return eatTime; }
        public List<FoodEffect> getEffects() { return new ArrayList<>(effects); }
        
        public static class Builder {
            private int nutrition = 0;
            private float saturation = 0.0f;
            private boolean isMeat = false;
            private boolean canAlwaysEat = false;
            private int eatTime = 32;
            private List<FoodEffect> effects = new ArrayList<>();
            
            public Builder nutrition(int nutrition) { this.nutrition = nutrition; return this; }
            public Builder saturation(float saturation) { this.saturation = saturation; return this; }
            public Builder meat() { this.isMeat = true; return this; }
            public Builder alwaysEat() { this.canAlwaysEat = true; return this; }
            public Builder eatTime(int eatTime) { this.eatTime = eatTime; return this; }
            public Builder effect(String effect, int duration, int amplifier, float probability) {
                this.effects.add(new FoodEffect(effect, duration, amplifier, probability));
                return this;
            }
            
            public FoodProperties build() {
                return new FoodProperties(nutrition, saturation, isMeat, canAlwaysEat, eatTime, effects);
            }
        }
    }
    
    /**
     * Food effect
     */
    public static class FoodEffect {
        private final String effect;
        private final int duration;
        private final int amplifier;
        private final float probability;
        
        public FoodEffect(String effect, int duration, int amplifier, float probability) {
            this.effect = effect;
            this.duration = duration;
            this.amplifier = amplifier;
            this.probability = probability;
        }
        
        public String getEffect() { return effect; }
        public int getDuration() { return duration; }
        public int getAmplifier() { return amplifier; }
        public float getProbability() { return probability; }
    }
    
    /**
     * Tool properties
     */
    public static class ToolProperties {
        private final ToolType toolType;
        private final int harvestLevel;
        private final float efficiency;
        private final float attackDamage;
        private final float attackSpeed;
        private final Set<String> effectiveBlocks;
        
        public ToolProperties(ToolType toolType, int harvestLevel, float efficiency, float attackDamage, float attackSpeed, Set<String> effectiveBlocks) {
            this.toolType = toolType;
            this.harvestLevel = harvestLevel;
            this.efficiency = efficiency;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            this.effectiveBlocks = effectiveBlocks != null ? new HashSet<>(effectiveBlocks) : new HashSet<>();
        }
        
        public ToolType getToolType() { return toolType; }
        public int getHarvestLevel() { return harvestLevel; }
        public float getEfficiency() { return efficiency; }
        public float getAttackDamage() { return attackDamage; }
        public float getAttackSpeed() { return attackSpeed; }
        public Set<String> getEffectiveBlocks() { return new HashSet<>(effectiveBlocks); }
        
        public float getMiningSpeed(Object blockState) {
            // Simplified - would check block type against effective blocks
            return efficiency;
        }
        
        public boolean canHarvest(Object blockState) {
            // Simplified - would check harvest level and block type
            return true;
        }
        
        public enum ToolType {
            PICKAXE, AXE, SHOVEL, HOE, SWORD, SHEARS, FISHING_ROD, FLINT_AND_STEEL, BOW, CROSSBOW, TRIDENT
        }
    }
    
    /**
     * Armor properties
     */
    public static class ArmorProperties {
        private final ArmorType armorType;
        private final int defense;
        private final float toughness;
        private final float knockbackResistance;
        private final int enchantability;
        
        public ArmorProperties(ArmorType armorType, int defense, float toughness, float knockbackResistance, int enchantability) {
            this.armorType = armorType;
            this.defense = defense;
            this.toughness = toughness;
            this.knockbackResistance = knockbackResistance;
            this.enchantability = enchantability;
        }
        
        public ArmorType getArmorType() { return armorType; }
        public int getDefense() { return defense; }
        public float getToughness() { return toughness; }
        public float getKnockbackResistance() { return knockbackResistance; }
        public int getEnchantability() { return enchantability; }
        
        public enum ArmorType {
            HELMET, CHESTPLATE, LEGGINGS, BOOTS
        }
    }
    
    /**
     * Weapon properties
     */
    public static class WeaponProperties {
        private final WeaponType weaponType;
        private final float attackDamage;
        private final float attackSpeed;
        private final float reach;
        private final float criticalChance;
        private final float criticalMultiplier;
        
        public WeaponProperties(WeaponType weaponType, float attackDamage, float attackSpeed, float reach, float criticalChance, float criticalMultiplier) {
            this.weaponType = weaponType;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
            this.reach = reach;
            this.criticalChance = criticalChance;
            this.criticalMultiplier = criticalMultiplier;
        }
        
        public WeaponType getWeaponType() { return weaponType; }
        public float getAttackDamage() { return attackDamage; }
        public float getAttackSpeed() { return attackSpeed; }
        public float getReach() { return reach; }
        public float getCriticalChance() { return criticalChance; }
        public float getCriticalMultiplier() { return criticalMultiplier; }
        
        public enum WeaponType {
            SWORD, AXE, PICKAXE, SHOVEL, HOE, BOW, CROSSBOW, TRIDENT, CUTLASS, FLINTLOCK, CANNON, HARPOON
        }
    }
}