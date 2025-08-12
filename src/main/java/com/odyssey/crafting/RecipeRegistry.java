package com.odyssey.crafting;

import com.odyssey.items.Item;
import com.odyssey.items.Items;
import com.odyssey.items.ItemStack;
import com.odyssey.inventory.Inventory;
import com.odyssey.data.BinaryTagLibrary;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing all crafting recipes in the game.
 * Provides methods for registration, lookup, and recipe matching.
 */
public class RecipeRegistry {
    private static final RecipeRegistry INSTANCE = new RecipeRegistry();
    
    private final Map<String, Recipe> recipes = new ConcurrentHashMap<>();
    private final Map<Recipe.RecipeType, List<Recipe>> recipesByType = new ConcurrentHashMap<>();
    private final Map<String, List<Recipe>> recipesByCategory = new ConcurrentHashMap<>();
    private final Map<Item, List<Recipe>> recipesByResult = new ConcurrentHashMap<>();
    private final Map<Item, List<Recipe>> recipesByIngredient = new ConcurrentHashMap<>();
    
    // Cache for performance
    private final Map<String, List<Recipe>> matchingCache = new ConcurrentHashMap<>();
    private boolean cacheValid = true;
    
    private RecipeRegistry() {
        registerDefaultRecipes();
    }
    
    /**
     * Gets the singleton instance
     */
    public static RecipeRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers a recipe
     */
    public void register(Recipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            throw new IllegalArgumentException("Recipe and ID cannot be null");
        }
        
        recipes.put(recipe.getId(), recipe);
        
        // Update type index
        recipesByType.computeIfAbsent(recipe.getType(), k -> new ArrayList<>()).add(recipe);
        
        // Update category index
        recipesByCategory.computeIfAbsent(recipe.getCategory(), k -> new ArrayList<>()).add(recipe);
        
        // Update result index
        recipesByResult.computeIfAbsent(recipe.getResult().getItem(), k -> new ArrayList<>()).add(recipe);
        for (ItemStack extraResult : recipe.getExtraResults()) {
            recipesByResult.computeIfAbsent(extraResult.getItem(), k -> new ArrayList<>()).add(recipe);
        }
        
        // Update ingredient index
        for (Ingredient ingredient : recipe.getIngredients()) {
            for (Item item : ingredient.getItems()) {
                recipesByIngredient.computeIfAbsent(item, k -> new ArrayList<>()).add(recipe);
            }
        }
        
        invalidateCache();
    }
    
    /**
     * Unregisters a recipe
     */
    public void unregister(String recipeId) {
        Recipe recipe = recipes.remove(recipeId);
        if (recipe != null) {
            // Remove from type index
            List<Recipe> typeList = recipesByType.get(recipe.getType());
            if (typeList != null) {
                typeList.remove(recipe);
                if (typeList.isEmpty()) {
                    recipesByType.remove(recipe.getType());
                }
            }
            
            // Remove from category index
            List<Recipe> categoryList = recipesByCategory.get(recipe.getCategory());
            if (categoryList != null) {
                categoryList.remove(recipe);
                if (categoryList.isEmpty()) {
                    recipesByCategory.remove(recipe.getCategory());
                }
            }
            
            // Remove from result index
            removeFromResultIndex(recipe);
            
            // Remove from ingredient index
            removeFromIngredientIndex(recipe);
            
            invalidateCache();
        }
    }
    
    /**
     * Gets a recipe by ID
     */
    public Recipe getRecipe(String id) {
        return recipes.get(id);
    }
    
    /**
     * Gets all recipes
     */
    public Collection<Recipe> getAllRecipes() {
        return new ArrayList<>(recipes.values());
    }
    
    /**
     * Gets recipes by type
     */
    public List<Recipe> getRecipesByType(Recipe.RecipeType type) {
        return new ArrayList<>(recipesByType.getOrDefault(type, Collections.emptyList()));
    }
    
    /**
     * Gets recipes by category
     */
    public List<Recipe> getRecipesByCategory(String category) {
        return new ArrayList<>(recipesByCategory.getOrDefault(category, Collections.emptyList()));
    }
    
    /**
     * Gets recipes that produce a specific item
     */
    public List<Recipe> getRecipesForResult(Item item) {
        return new ArrayList<>(recipesByResult.getOrDefault(item, Collections.emptyList()));
    }
    
    /**
     * Gets recipes that use a specific ingredient
     */
    public List<Recipe> getRecipesWithIngredient(Item item) {
        return new ArrayList<>(recipesByIngredient.getOrDefault(item, Collections.emptyList()));
    }
    
    /**
     * Finds recipes that match the given inventory
     */
    public List<Recipe> findMatchingRecipes(Inventory inventory) {
        return findMatchingRecipes(inventory, null);
    }
    
    /**
     * Finds recipes of a specific type that match the given inventory
     */
    public List<Recipe> findMatchingRecipes(Inventory inventory, Recipe.RecipeType type) {
        String cacheKey = generateCacheKey(inventory, type);
        
        if (cacheValid && matchingCache.containsKey(cacheKey)) {
            return new ArrayList<>(matchingCache.get(cacheKey));
        }
        
        List<Recipe> matching = new ArrayList<>();
        Collection<Recipe> recipesToCheck = type != null ? getRecipesByType(type) : getAllRecipes();
        
        for (Recipe recipe : recipesToCheck) {
            if (recipe.matches(inventory)) {
                matching.add(recipe);
            }
        }
        
        // Sort by priority (experience reward, then crafting time)
        matching.sort((r1, r2) -> {
            int expCompare = Integer.compare(r2.getExperienceReward(), r1.getExperienceReward());
            if (expCompare != 0) return expCompare;
            return Integer.compare(r1.getCraftingTime(), r2.getCraftingTime());
        });
        
        if (cacheValid) {
            matchingCache.put(cacheKey, new ArrayList<>(matching));
        }
        
        return matching;
    }
    
    /**
     * Finds the best matching recipe for the given inventory
     */
    public Recipe findBestMatch(Inventory inventory) {
        return findBestMatch(inventory, null);
    }
    
    /**
     * Finds the best matching recipe of a specific type for the given inventory
     */
    public Recipe findBestMatch(Inventory inventory, Recipe.RecipeType type) {
        List<Recipe> matching = findMatchingRecipes(inventory, type);
        return matching.isEmpty() ? null : matching.get(0);
    }
    
    /**
     * Gets all recipe types
     */
    public Set<Recipe.RecipeType> getRecipeTypes() {
        return new HashSet<>(recipesByType.keySet());
    }
    
    /**
     * Gets all recipe categories
     */
    public Set<String> getRecipeCategories() {
        return new HashSet<>(recipesByCategory.keySet());
    }
    
    /**
     * Gets recipes by search query
     */
    public List<Recipe> searchRecipes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(recipes.values());
        }
        
        String lowerQuery = query.toLowerCase().trim();
        
        return recipes.values().stream()
            .filter(recipe -> 
                recipe.getId().toLowerCase().contains(lowerQuery) ||
                recipe.getCategory().toLowerCase().contains(lowerQuery) ||
                recipe.getResult().getItem().getDisplayName().toLowerCase().contains(lowerQuery) ||
                recipe.getType().name().toLowerCase().contains(lowerQuery)
            )
            .collect(Collectors.toList());
    }
    
    /**
     * Gets recipe statistics
     */
    public RecipeStats getStats() {
        return new RecipeStats(
            recipes.size(),
            recipesByType.size(),
            recipesByCategory.size(),
            (int) recipes.values().stream().mapToInt(Recipe::getExperienceReward).average().orElse(0),
            (int) recipes.values().stream().mapToInt(Recipe::getCraftingTime).average().orElse(0)
        );
    }
    
    /**
     * Clears all recipes
     */
    public void clear() {
        recipes.clear();
        recipesByType.clear();
        recipesByCategory.clear();
        recipesByResult.clear();
        recipesByIngredient.clear();
        invalidateCache();
    }
    
    /**
     * Reloads default recipes
     */
    public void reload() {
        clear();
        registerDefaultRecipes();
    }
    
    /**
     * Validates all recipes
     */
    public List<String> validateRecipes() {
        List<String> errors = new ArrayList<>();
        
        for (Recipe recipe : recipes.values()) {
            // Check if result item exists
            if (recipe.getResult().isEmpty()) {
                errors.add("Recipe " + recipe.getId() + " has empty result");
            }
            
            // Check if ingredient items exist
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) {
                    errors.add("Recipe " + recipe.getId() + " has empty ingredient");
                }
            }
            
            // Check for circular dependencies
            if (hasCircularDependency(recipe)) {
                errors.add("Recipe " + recipe.getId() + " has circular dependency");
            }
        }
        
        return errors;
    }
    
    /**
     * Exports recipes to NBT
     */
    public BinaryTagLibrary.CompoundTag exportNBT() {
        BinaryTagLibrary.CompoundTag tag = new BinaryTagLibrary.CompoundTag();
        
        BinaryTagLibrary.ListTag recipesList = new BinaryTagLibrary.ListTag();
        for (Recipe recipe : recipes.values()) {
            recipesList.add(recipe.serializeNBT());
        }
        tag.put("recipes", recipesList);
        
        return tag;
    }
    
    /**
     * Imports recipes from NBT
     */
    public void importNBT(BinaryTagLibrary.CompoundTag tag) {
        clear();
        
        BinaryTagLibrary.ListTag recipesList = tag.getList("recipes");
        for (int i = 0; i < recipesList.size(); i++) {
            BinaryTagLibrary.CompoundTag recipeTag = recipesList.getCompound(i);
            // Recipe deserialization would be implemented here
            // For now, we'll skip this as it requires more complex deserialization logic
        }
        
        registerDefaultRecipes();
    }
    
    /**
     * Registers default recipes
     */
    private void registerDefaultRecipes() {
        // Basic crafting recipes
        registerBasicCraftingRecipes();
        
        // Maritime-specific recipes
        registerMaritimeRecipes();
        
        // Smelting recipes
        registerSmeltingRecipes();
        
        // Cooking recipes
        registerCookingRecipes();
        
        // Advanced crafting recipes
        registerAdvancedRecipes();
    }
    
    private void registerBasicCraftingRecipes() {
        // Wood planks from logs
        register(Recipe.builder("wood_planks", Recipe.RecipeType.CRAFTING)
            .ingredient(Ingredient.builder().tag("logs").build())
            .result(Items.WOOD_PLANK, 4)
            .category("basic")
            .craftingTime(10)
            .experience(1)
            .build());
        
        // Sticks from planks
        register(Recipe.builder("sticks", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.WOOD_PLANK, 2)
            .result(Items.STICK, 4)
            .category("basic")
            .craftingTime(5)
            .build());
        
        // Wooden pickaxe
        register(Recipe.builder("wooden_pickaxe", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.WOOD_PLANK, 3)
            .ingredient(Items.STICK, 2)
            .result(Items.WOODEN_PICKAXE)
            .category("tools")
            .shaped(3, 3)
            .craftingTime(20)
            .experience(2)
            .build());
        
        // Stone pickaxe
        register(Recipe.builder("stone_pickaxe", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.STONE, 3)
            .ingredient(Items.STICK, 2)
            .result(Items.STONE_PICKAXE)
            .category("tools")
            .shaped(3, 3)
            .craftingTime(30)
            .experience(3)
            .build());
        
        // Iron pickaxe
        register(Recipe.builder("iron_pickaxe", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.IRON_INGOT, 3)
            .ingredient(Items.STICK, 2)
            .result(Items.IRON_PICKAXE)
            .category("tools")
            .shaped(3, 3)
            .craftingTime(40)
            .experience(5)
            .build());
    }
    
    private void registerMaritimeRecipes() {
        // Rope from hemp/fiber
        register(Recipe.builder("rope", Recipe.RecipeType.CRAFTING)
            .ingredient(Ingredient.builder().tag("fiber").count(3).build())
            .result(Items.ROPE, 2)
            .category("maritime")
            .craftingTime(15)
            .experience(1)
            .build());
        
        // Sail cloth
        register(Recipe.builder("sail_cloth", Recipe.RecipeType.TAILORING)
            .ingredient(Ingredient.builder().tag("fabric").count(4).build())
            .ingredient(Items.ROPE, 2)
            .result(Items.SAIL_CLOTH)
            .category("maritime")
            .craftingTime(60)
            .experience(3)
            .requireTool("needle")
            .build());
        
        // Cutlass
        register(Recipe.builder("cutlass", Recipe.RecipeType.BLACKSMITHING)
            .ingredient(Items.IRON_INGOT, 2)
            .ingredient(Items.WOOD_PLANK, 1)
            .result(Items.CUTLASS)
            .category("weapons")
            .craftingTime(80)
            .experience(8)
            .requireTool("anvil")
            .requireTool("hammer")
            .build());
        
        // Flintlock pistol
        register(Recipe.builder("flintlock_pistol", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.IRON_INGOT, 3)
            .ingredient(Items.WOOD_PLANK, 2)
            .ingredient(Items.GUNPOWDER, 1)
            .result(Items.FLINTLOCK_PISTOL)
            .category("weapons")
            .craftingTime(120)
            .experience(15)
            .requireSkill("gunsmithing")
            .build());
        
        // Spyglass
        register(Recipe.builder("spyglass", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.COPPER_INGOT, 2)
            .ingredient(Ingredient.builder().tag("glass").build())
            .result(Items.SPYGLASS)
            .category("tools")
            .craftingTime(60)
            .experience(10)
            .build());
        
        // Compass
        register(Recipe.builder("compass", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.IRON_INGOT, 4)
            .ingredient(Items.REDSTONE, 1)
            .result(Items.COMPASS)
            .category("tools")
            .craftingTime(40)
            .experience(8)
            .build());
        
        // Cannon ball
        register(Recipe.builder("cannon_ball", Recipe.RecipeType.CRAFTING)
            .ingredient(Items.IRON_INGOT, 2)
            .result(Items.CANNON_BALL, 4)
            .category("ammunition")
            .craftingTime(30)
            .experience(2)
            .build());
    }
    
    private void registerSmeltingRecipes() {
        // Iron ingot from ore
        register(Recipe.builder("iron_ingot_smelting", Recipe.RecipeType.SMELTING)
            .ingredient(Ingredient.builder().tag("iron_ore").build())
            .result(Items.IRON_INGOT)
            .category("smelting")
            .craftingTime(200)
            .experience(5)
            .property("fuel_required", 100)
            .build());
        
        // Copper ingot from ore
        register(Recipe.builder("copper_ingot_smelting", Recipe.RecipeType.SMELTING)
            .ingredient(Ingredient.builder().tag("copper_ore").build())
            .result(Items.COPPER_INGOT)
            .category("smelting")
            .craftingTime(180)
            .experience(4)
            .property("fuel_required", 80)
            .build());
        
        // Gold ingot from ore
        register(Recipe.builder("gold_ingot_smelting", Recipe.RecipeType.SMELTING)
            .ingredient(Ingredient.builder().tag("gold_ore").build())
            .result(Items.GOLD_INGOT)
            .category("smelting")
            .craftingTime(220)
            .experience(8)
            .property("fuel_required", 120)
            .build());
    }
    
    private void registerCookingRecipes() {
        // Cooked fish
        register(Recipe.builder("cooked_fish", Recipe.RecipeType.COOKING)
            .ingredient(Ingredient.builder().tag("raw_fish").build())
            .result(Items.COOKED_FISH)
            .category("food")
            .craftingTime(100)
            .experience(2)
            .property("fuel_required", 20)
            .build());
        
        // Bread
        register(Recipe.builder("bread", Recipe.RecipeType.COOKING)
            .ingredient(Ingredient.builder().tag("wheat").count(3).build())
            .result(Items.BREAD)
            .category("food")
            .craftingTime(80)
            .experience(3)
            .property("fuel_required", 30)
            .build());
    }
    
    private void registerAdvancedRecipes() {
        // Treasure map (special recipe)
        register(Recipe.builder("treasure_map", Recipe.RecipeType.TREASURE_HUNTING)
            .ingredient(Ingredient.builder().tag("paper").build())
            .ingredient(Items.COMPASS, 1)
            .ingredient(Ingredient.builder().tag("ink").build())
            .result(Items.TREASURE_MAP)
            .category("treasure")
            .craftingTime(200)
            .experience(20)
            .requireSkill("cartography")
            .property("requires_exploration", true)
            .build());
    }
    
    private void removeFromResultIndex(Recipe recipe) {
        List<Recipe> resultList = recipesByResult.get(recipe.getResult().getItem());
        if (resultList != null) {
            resultList.remove(recipe);
            if (resultList.isEmpty()) {
                recipesByResult.remove(recipe.getResult().getItem());
            }
        }
        
        for (ItemStack extraResult : recipe.getExtraResults()) {
            List<Recipe> extraList = recipesByResult.get(extraResult.getItem());
            if (extraList != null) {
                extraList.remove(recipe);
                if (extraList.isEmpty()) {
                    recipesByResult.remove(extraResult.getItem());
                }
            }
        }
    }
    
    private void removeFromIngredientIndex(Recipe recipe) {
        for (Ingredient ingredient : recipe.getIngredients()) {
            for (Item item : ingredient.getItems()) {
                List<Recipe> ingredientList = recipesByIngredient.get(item);
                if (ingredientList != null) {
                    ingredientList.remove(recipe);
                    if (ingredientList.isEmpty()) {
                        recipesByIngredient.remove(item);
                    }
                }
            }
        }
    }
    
    private String generateCacheKey(Inventory inventory, Recipe.RecipeType type) {
        StringBuilder key = new StringBuilder();
        
        // Add inventory contents to key
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                key.append(i).append(":").append(stack.getItem().getRegistryName())
                   .append(":").append(stack.getCount()).append(";");
            }
        }
        
        if (type != null) {
            key.append("type:").append(type.name());
        }
        
        return key.toString();
    }
    
    private boolean hasCircularDependency(Recipe recipe) {
        // Simple circular dependency check
        // In practice, this would be more sophisticated
        Item resultItem = recipe.getResult().getItem();
        
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.getItems().contains(resultItem)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void invalidateCache() {
        matchingCache.clear();
        cacheValid = true;
    }
    
    /**
     * Recipe statistics class
     */
    public static class RecipeStats {
        private final int totalRecipes;
        private final int recipeTypes;
        private final int categories;
        private final int averageExperience;
        private final int averageCraftingTime;
        
        public RecipeStats(int totalRecipes, int recipeTypes, int categories, 
                          int averageExperience, int averageCraftingTime) {
            this.totalRecipes = totalRecipes;
            this.recipeTypes = recipeTypes;
            this.categories = categories;
            this.averageExperience = averageExperience;
            this.averageCraftingTime = averageCraftingTime;
        }
        
        public int getTotalRecipes() { return totalRecipes; }
        public int getRecipeTypes() { return recipeTypes; }
        public int getCategories() { return categories; }
        public int getAverageExperience() { return averageExperience; }
        public int getAverageCraftingTime() { return averageCraftingTime; }
        
        @Override
        public String toString() {
            return String.format("RecipeStats{recipes=%d, types=%d, categories=%d, avgExp=%d, avgTime=%d}",
                totalRecipes, recipeTypes, categories, averageExperience, averageCraftingTime);
        }
    }
}