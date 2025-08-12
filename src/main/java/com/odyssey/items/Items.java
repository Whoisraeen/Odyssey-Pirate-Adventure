package com.odyssey.items;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all items in the game.
 * Provides access to items by registry name and manages item registration.
 */
public class Items {
    private static final Map<String, Item> ITEMS = new ConcurrentHashMap<>();
    private static final Map<Item.ItemCategory, List<Item>> ITEMS_BY_CATEGORY = new ConcurrentHashMap<>();
    private static final Map<String, List<Item>> ITEMS_BY_TAG = new ConcurrentHashMap<>();
    
    // Special items
    public static final Item AIR = register("odyssey:air", new Item("odyssey:air", 
        new Item.Properties().maxStackSize(0).displayName("Air")));
    
    // Basic materials
    public static final Item WOOD_PLANK = register("odyssey:wood_plank", new Item("odyssey:wood_plank",
        new Item.Properties()
            .category(Item.ItemCategory.BUILDING_BLOCKS)
            .burnTime(300)
            .tags("planks", "wood", "building")));
    
    public static final Item STONE = register("odyssey:stone", new Item("odyssey:stone",
        new Item.Properties()
            .category(Item.ItemCategory.BUILDING_BLOCKS)
            .tags("stone", "building")));
    
    public static final Item IRON_INGOT = register("odyssey:iron_ingot", new Item("odyssey:iron_ingot",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .tags("ingot", "metal", "iron")));
    
    public static final Item GOLD_INGOT = register("odyssey:gold_ingot", new Item("odyssey:gold_ingot",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .rarity(Item.ItemRarity.UNCOMMON)
            .tags("ingot", "metal", "gold", "precious")));
    
    public static final Item DIAMOND = register("odyssey:diamond", new Item("odyssey:diamond",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .rarity(Item.ItemRarity.RARE)
            .tags("gem", "precious", "diamond")));
    
    // Tools
    public static final Item WOODEN_PICKAXE = register("odyssey:wooden_pickaxe", new Item("odyssey:wooden_pickaxe",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .maxDamage(59)
            .tool(new Item.ToolProperties(Item.ToolProperties.ToolType.PICKAXE, 0, 2.0f, 1.0f, 1.2f, 
                Set.of("stone", "cobblestone", "coal_ore")))
            .enchantability(15)
            .repairMaterial("odyssey:wood_plank")
            .tags("tool", "pickaxe", "wooden")));
    
    public static final Item STONE_PICKAXE = register("odyssey:stone_pickaxe", new Item("odyssey:stone_pickaxe",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .maxDamage(131)
            .tool(new Item.ToolProperties(Item.ToolProperties.ToolType.PICKAXE, 1, 4.0f, 1.0f, 1.2f,
                Set.of("stone", "cobblestone", "coal_ore", "iron_ore")))
            .enchantability(5)
            .repairMaterial("odyssey:stone")
            .tags("tool", "pickaxe", "stone")));
    
    public static final Item IRON_PICKAXE = register("odyssey:iron_pickaxe", new Item("odyssey:iron_pickaxe",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .maxDamage(250)
            .tool(new Item.ToolProperties(Item.ToolProperties.ToolType.PICKAXE, 2, 6.0f, 1.0f, 1.2f,
                Set.of("stone", "cobblestone", "coal_ore", "iron_ore", "gold_ore", "redstone_ore")))
            .enchantability(14)
            .repairMaterial("odyssey:iron_ingot")
            .tags("tool", "pickaxe", "iron")));
    
    public static final Item DIAMOND_PICKAXE = register("odyssey:diamond_pickaxe", new Item("odyssey:diamond_pickaxe",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .rarity(Item.ItemRarity.RARE)
            .maxDamage(1561)
            .tool(new Item.ToolProperties(Item.ToolProperties.ToolType.PICKAXE, 3, 8.0f, 1.0f, 1.2f,
                Set.of("stone", "cobblestone", "coal_ore", "iron_ore", "gold_ore", "redstone_ore", "diamond_ore")))
            .enchantability(10)
            .repairMaterial("odyssey:diamond")
            .tags("tool", "pickaxe", "diamond")));
    
    // Maritime tools
    public static final Item CUTLASS = register("odyssey:cutlass", new Item("odyssey:cutlass",
        new Item.Properties()
            .category(Item.ItemCategory.COMBAT)
            .rarity(Item.ItemRarity.UNCOMMON)
            .maxDamage(300)
            .weapon(new Item.WeaponProperties(Item.WeaponProperties.WeaponType.CUTLASS, 7.0f, 1.6f, 3.0f, 0.1f, 1.5f))
            .enchantability(12)
            .repairMaterial("odyssey:iron_ingot")
            .tags("weapon", "sword", "cutlass", "maritime")));
    
    public static final Item FLINTLOCK_PISTOL = register("odyssey:flintlock_pistol", new Item("odyssey:flintlock_pistol",
        new Item.Properties()
            .category(Item.ItemCategory.COMBAT)
            .rarity(Item.ItemRarity.UNCOMMON)
            .maxDamage(200)
            .maxStackSize(1)
            .weapon(new Item.WeaponProperties(Item.WeaponProperties.WeaponType.FLINTLOCK, 12.0f, 0.5f, 8.0f, 0.2f, 2.0f))
            .enchantability(8)
            .tags("weapon", "firearm", "flintlock", "maritime")));
    
    public static final Item SPYGLASS = register("odyssey:spyglass", new Item("odyssey:spyglass",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .rarity(Item.ItemRarity.UNCOMMON)
            .maxDamage(100)
            .maxStackSize(1)
            .tags("tool", "spyglass", "maritime", "navigation")));
    
    public static final Item COMPASS = register("odyssey:compass", new Item("odyssey:compass",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .maxStackSize(1)
            .tags("tool", "compass", "navigation")));
    
    public static final Item MAP = register("odyssey:map", new Item("odyssey:map",
        new Item.Properties()
            .category(Item.ItemCategory.TOOLS)
            .maxStackSize(1)
            .tags("tool", "map", "navigation")));
    
    // Food items
    public static final Item BREAD = register("odyssey:bread", new Item("odyssey:bread",
        new Item.Properties()
            .category(Item.ItemCategory.FOOD)
            .food(new Item.FoodProperties.Builder()
                .nutrition(5)
                .saturation(6.0f)
                .eatTime(32)
                .build())
            .tags("food", "bread")));
    
    public static final Item FISH = register("odyssey:fish", new Item("odyssey:fish",
        new Item.Properties()
            .category(Item.ItemCategory.FOOD)
            .food(new Item.FoodProperties.Builder()
                .nutrition(2)
                .saturation(0.4f)
                .meat()
                .eatTime(32)
                .build())
            .tags("food", "fish", "raw")));
    
    public static final Item COOKED_FISH = register("odyssey:cooked_fish", new Item("odyssey:cooked_fish",
        new Item.Properties()
            .category(Item.ItemCategory.FOOD)
            .food(new Item.FoodProperties.Builder()
                .nutrition(5)
                .saturation(6.0f)
                .meat()
                .eatTime(32)
                .build())
            .tags("food", "fish", "cooked")));
    
    public static final Item RUM = register("odyssey:rum", new Item("odyssey:rum",
        new Item.Properties()
            .category(Item.ItemCategory.FOOD)
            .rarity(Item.ItemRarity.UNCOMMON)
            .food(new Item.FoodProperties.Builder()
                .nutrition(1)
                .saturation(0.2f)
                .alwaysEat()
                .eatTime(16)
                .effect("strength", 1200, 0, 0.8f)
                .effect("nausea", 600, 0, 0.3f)
                .build())
            .tags("food", "drink", "alcohol", "maritime")));
    
    // Maritime materials
    public static final Item ROPE = register("odyssey:rope", new Item("odyssey:rope",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .tags("rope", "maritime", "crafting")));
    
    public static final Item SAIL_CLOTH = register("odyssey:sail_cloth", new Item("odyssey:sail_cloth",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .tags("cloth", "sail", "maritime", "crafting")));
    
    public static final Item CANNON_BALL = register("odyssey:cannon_ball", new Item("odyssey:cannon_ball",
        new Item.Properties()
            .category(Item.ItemCategory.COMBAT)
            .maxStackSize(16)
            .tags("ammunition", "cannon", "maritime")));
    
    public static final Item GUNPOWDER = register("odyssey:gunpowder", new Item("odyssey:gunpowder",
        new Item.Properties()
            .category(Item.ItemCategory.MATERIALS)
            .tags("explosive", "gunpowder", "crafting")));
    
    // Treasure items
    public static final Item DOUBLOON = register("odyssey:doubloon", new Item("odyssey:doubloon",
        new Item.Properties()
            .category(Item.ItemCategory.TREASURE)
            .rarity(Item.ItemRarity.RARE)
            .tags("treasure", "coin", "gold", "currency")));
    
    public static final Item EMERALD = register("odyssey:emerald", new Item("odyssey:emerald",
        new Item.Properties()
            .category(Item.ItemCategory.TREASURE)
            .rarity(Item.ItemRarity.RARE)
            .tags("treasure", "gem", "emerald", "precious")));
    
    public static final Item PEARL = register("odyssey:pearl", new Item("odyssey:pearl",
        new Item.Properties()
            .category(Item.ItemCategory.TREASURE)
            .rarity(Item.ItemRarity.EPIC)
            .tags("treasure", "pearl", "precious", "maritime")));
    
    public static final Item TREASURE_MAP = register("odyssey:treasure_map", new Item("odyssey:treasure_map",
        new Item.Properties()
            .category(Item.ItemCategory.TREASURE)
            .rarity(Item.ItemRarity.EPIC)
            .maxStackSize(1)
            .tags("treasure", "map", "quest", "maritime")));
    
    /**
     * Registers an item
     */
    public static <T extends Item> T register(String registryName, T item) {
        if (ITEMS.containsKey(registryName)) {
            throw new IllegalArgumentException("Item with registry name '" + registryName + "' already exists!");
        }
        
        ITEMS.put(registryName, item);
        
        // Add to category map
        ITEMS_BY_CATEGORY.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
        
        // Add to tag maps
        for (String tag : item.getTags()) {
            ITEMS_BY_TAG.computeIfAbsent(tag, k -> new ArrayList<>()).add(item);
        }
        
        return item;
    }
    
    /**
     * Gets an item by registry name
     */
    public static Item getItem(String registryName) {
        return ITEMS.get(registryName);
    }
    
    /**
     * Gets an item by registry name, returning a default if not found
     */
    public static Item getItem(String registryName, Item defaultItem) {
        return ITEMS.getOrDefault(registryName, defaultItem);
    }
    
    /**
     * Checks if an item exists
     */
    public static boolean hasItem(String registryName) {
        return ITEMS.containsKey(registryName);
    }
    
    /**
     * Gets all registered items
     */
    public static Collection<Item> getAllItems() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }
    
    /**
     * Gets all registry names
     */
    public static Set<String> getAllRegistryNames() {
        return Collections.unmodifiableSet(ITEMS.keySet());
    }
    
    /**
     * Gets items by category
     */
    public static List<Item> getItemsByCategory(Item.ItemCategory category) {
        return ITEMS_BY_CATEGORY.getOrDefault(category, Collections.emptyList());
    }
    
    /**
     * Gets items by tag
     */
    public static List<Item> getItemsByTag(String tag) {
        return ITEMS_BY_TAG.getOrDefault(tag, Collections.emptyList());
    }
    
    /**
     * Gets all categories that have items
     */
    public static Set<Item.ItemCategory> getUsedCategories() {
        return Collections.unmodifiableSet(ITEMS_BY_CATEGORY.keySet());
    }
    
    /**
     * Gets all tags that have items
     */
    public static Set<String> getUsedTags() {
        return Collections.unmodifiableSet(ITEMS_BY_TAG.keySet());
    }
    
    /**
     * Searches for items by name (case-insensitive)
     */
    public static List<Item> searchItems(String query) {
        String lowerQuery = query.toLowerCase();
        List<Item> results = new ArrayList<>();
        
        for (Item item : ITEMS.values()) {
            if (item.getDisplayName().toLowerCase().contains(lowerQuery) ||
                item.getRegistryName().toLowerCase().contains(lowerQuery)) {
                results.add(item);
            }
        }
        
        return results;
    }
    
    /**
     * Gets items by rarity
     */
    public static List<Item> getItemsByRarity(Item.ItemRarity rarity) {
        List<Item> results = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.getRarity() == rarity) {
                results.add(item);
            }
        }
        return results;
    }
    
    /**
     * Gets all tools
     */
    public static List<Item> getTools() {
        List<Item> tools = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.isTool()) {
                tools.add(item);
            }
        }
        return tools;
    }
    
    /**
     * Gets all weapons
     */
    public static List<Item> getWeapons() {
        List<Item> weapons = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.isWeapon()) {
                weapons.add(item);
            }
        }
        return weapons;
    }
    
    /**
     * Gets all armor
     */
    public static List<Item> getArmor() {
        List<Item> armor = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.isArmor()) {
                armor.add(item);
            }
        }
        return armor;
    }
    
    /**
     * Gets all food items
     */
    public static List<Item> getFood() {
        List<Item> food = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.isEdible()) {
                food.add(item);
            }
        }
        return food;
    }
    
    /**
     * Gets all fuel items
     */
    public static List<Item> getFuel() {
        List<Item> fuel = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (item.isFuel()) {
                fuel.add(item);
            }
        }
        return fuel;
    }
    
    /**
     * Gets statistics about registered items
     */
    public static Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_items", ITEMS.size());
        stats.put("categories", ITEMS_BY_CATEGORY.size());
        stats.put("tags", ITEMS_BY_TAG.size());
        
        for (Item.ItemCategory category : Item.ItemCategory.values()) {
            int count = ITEMS_BY_CATEGORY.getOrDefault(category, Collections.emptyList()).size();
            stats.put("category_" + category.name().toLowerCase(), count);
        }
        
        for (Item.ItemRarity rarity : Item.ItemRarity.values()) {
            int count = getItemsByRarity(rarity).size();
            stats.put("rarity_" + rarity.name().toLowerCase(), count);
        }
        
        stats.put("tools", getTools().size());
        stats.put("weapons", getWeapons().size());
        stats.put("armor", getArmor().size());
        stats.put("food", getFood().size());
        stats.put("fuel", getFuel().size());
        
        return stats;
    }
    
    /**
     * Validates all registered items
     */
    public static List<String> validateItems() {
        List<String> issues = new ArrayList<>();
        
        for (Map.Entry<String, Item> entry : ITEMS.entrySet()) {
            String registryName = entry.getKey();
            Item item = entry.getValue();
            
            // Check registry name format
            if (!registryName.contains(":")) {
                issues.add("Item '" + registryName + "' does not have a namespace");
            }
            
            // Check if item's registry name matches the key
            if (!registryName.equals(item.getRegistryName())) {
                issues.add("Item registry name mismatch: key='" + registryName + "', item='" + item.getRegistryName() + "'");
            }
            
            // Check for null display name
            if (item.getDisplayName() == null || item.getDisplayName().trim().isEmpty()) {
                issues.add("Item '" + registryName + "' has null or empty display name");
            }
            
            // Check tool properties
            if (item.isTool() && item.getToolProperties() == null) {
                issues.add("Item '" + registryName + "' is marked as tool but has no tool properties");
            }
            
            // Check weapon properties
            if (item.isWeapon() && item.getWeaponProperties() == null) {
                issues.add("Item '" + registryName + "' is marked as weapon but has no weapon properties");
            }
            
            // Check armor properties
            if (item.isArmor() && item.getArmorProperties() == null) {
                issues.add("Item '" + registryName + "' is marked as armor but has no armor properties");
            }
            
            // Check food properties
            if (item.isEdible() && item.getFoodProperties() == null) {
                issues.add("Item '" + registryName + "' is marked as edible but has no food properties");
            }
            
            // Check repair material
            if (item.isRepairable() && !hasItem(item.getRepairMaterial())) {
                issues.add("Item '" + registryName + "' has invalid repair material: '" + item.getRepairMaterial() + "'");
            }
        }
        
        return issues;
    }
    
    /**
     * Clears all registered items (for testing)
     */
    public static void clear() {
        ITEMS.clear();
        ITEMS_BY_CATEGORY.clear();
        ITEMS_BY_TAG.clear();
    }
    
    /**
     * Gets the number of registered items
     */
    public static int getItemCount() {
        return ITEMS.size();
    }
}