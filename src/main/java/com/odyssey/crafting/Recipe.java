package com.odyssey.crafting;

import com.odyssey.items.Item;
import com.odyssey.items.ItemStack;
import com.odyssey.inventory.Inventory;
import com.odyssey.data.BinaryTagLibrary;
import java.util.*;

/**
 * Represents a crafting recipe with ingredients and results.
 * Supports various recipe types and conditions.
 */
public class Recipe {
    private final String id;
    private final RecipeType type;
    private final List<Ingredient> ingredients;
    private final ItemStack result;
    private final List<ItemStack> extraResults;
    private final Map<String, Object> properties;
    private final Set<String> requiredTools;
    private final Set<String> requiredSkills;
    private final int craftingTime;
    private final int experienceReward;
    private final String category;
    private final boolean shapeless;
    private final int width;
    private final int height;
    
    private Recipe(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.ingredients = new ArrayList<>(builder.ingredients);
        this.result = builder.result;
        this.extraResults = new ArrayList<>(builder.extraResults);
        this.properties = new HashMap<>(builder.properties);
        this.requiredTools = new HashSet<>(builder.requiredTools);
        this.requiredSkills = new HashSet<>(builder.requiredSkills);
        this.craftingTime = builder.craftingTime;
        this.experienceReward = builder.experienceReward;
        this.category = builder.category;
        this.shapeless = builder.shapeless;
        this.width = builder.width;
        this.height = builder.height;
    }
    
    /**
     * Gets the recipe ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the recipe type
     */
    public RecipeType getType() {
        return type;
    }
    
    /**
     * Gets the ingredients
     */
    public List<Ingredient> getIngredients() {
        return new ArrayList<>(ingredients);
    }
    
    /**
     * Gets the main result
     */
    public ItemStack getResult() {
        return result.copy();
    }
    
    /**
     * Gets extra results (byproducts)
     */
    public List<ItemStack> getExtraResults() {
        List<ItemStack> results = new ArrayList<>();
        for (ItemStack stack : extraResults) {
            results.add(stack.copy());
        }
        return results;
    }
    
    /**
     * Gets all results including main and extra
     */
    public List<ItemStack> getAllResults() {
        List<ItemStack> results = new ArrayList<>();
        results.add(result.copy());
        results.addAll(getExtraResults());
        return results;
    }
    
    /**
     * Gets recipe properties
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * Gets a specific property
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Gets required tools
     */
    public Set<String> getRequiredTools() {
        return new HashSet<>(requiredTools);
    }
    
    /**
     * Gets required skills
     */
    public Set<String> getRequiredSkills() {
        return new HashSet<>(requiredSkills);
    }
    
    /**
     * Gets crafting time in ticks
     */
    public int getCraftingTime() {
        return craftingTime;
    }
    
    /**
     * Gets experience reward
     */
    public int getExperienceReward() {
        return experienceReward;
    }
    
    /**
     * Gets recipe category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Checks if this is a shapeless recipe
     */
    public boolean isShapeless() {
        return shapeless;
    }
    
    /**
     * Gets recipe width (for shaped recipes)
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets recipe height (for shaped recipes)
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Checks if the recipe matches the given inventory
     */
    public boolean matches(Inventory inventory) {
        if (shapeless) {
            return matchesShapeless(inventory);
        } else {
            return matchesShaped(inventory);
        }
    }
    
    /**
     * Checks if the recipe matches as a shapeless recipe
     */
    private boolean matchesShapeless(Inventory inventory) {
        List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);
        
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                boolean matched = false;
                
                for (Iterator<Ingredient> iter = remainingIngredients.iterator(); iter.hasNext();) {
                    Ingredient ingredient = iter.next();
                    if (ingredient.matches(stack)) {
                        iter.remove();
                        matched = true;
                        break;
                    }
                }
                
                if (!matched) {
                    return false; // Extra item that doesn't match any ingredient
                }
            }
        }
        
        return remainingIngredients.isEmpty(); // All ingredients matched
    }
    
    /**
     * Checks if the recipe matches as a shaped recipe
     */
    private boolean matchesShaped(Inventory inventory) {
        // For shaped recipes, we need to check the pattern
        // This is a simplified implementation - in practice, you'd want to support
        // pattern matching with rotation and mirroring
        
        if (inventory.getSize() < width * height) {
            return false;
        }
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int ingredientIndex = y * width + x;
                int inventorySlot = y * Math.max(width, 3) + x; // Assuming 3x3 crafting grid minimum
                
                if (ingredientIndex < ingredients.size() && inventorySlot < inventory.getSize()) {
                    Ingredient ingredient = ingredients.get(ingredientIndex);
                    ItemStack stack = inventory.getItem(inventorySlot);
                    
                    if (!ingredient.matches(stack)) {
                        return false;
                    }
                } else if (inventorySlot < inventory.getSize() && !inventory.getItem(inventorySlot).isEmpty()) {
                    return false; // Extra item in pattern
                }
            }
        }
        
        return true;
    }
    
    /**
     * Consumes ingredients from the inventory
     */
    public boolean consumeIngredients(Inventory inventory) {
        if (!matches(inventory)) {
            return false;
        }
        
        // Create a copy to test consumption
        Map<Integer, ItemStack> toConsume = new HashMap<>();
        List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);
        
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                for (Iterator<Ingredient> iter = remainingIngredients.iterator(); iter.hasNext();) {
                    Ingredient ingredient = iter.next();
                    if (ingredient.matches(stack)) {
                        ItemStack consumed = stack.copy();
                        consumed.setCount(ingredient.getCount());
                        toConsume.put(slot, consumed);
                        iter.remove();
                        break;
                    }
                }
            }
        }
        
        // Actually consume the items
        for (Map.Entry<Integer, ItemStack> entry : toConsume.entrySet()) {
            int slot = entry.getKey();
            ItemStack toRemove = entry.getValue();
            ItemStack existing = inventory.getItem(slot);
            
            existing.shrink(toRemove.getCount());
            if (existing.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        
        return true;
    }
    
    /**
     * Gets the remaining items after crafting (containers, etc.)
     */
    public List<ItemStack> getRemainingItems(Inventory inventory) {
        List<ItemStack> remaining = new ArrayList<>();
        
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                for (Ingredient ingredient : ingredients) {
                    if (ingredient.matches(stack)) {
                        ItemStack container = ingredient.getContainerItem(stack);
                        if (!container.isEmpty()) {
                            remaining.add(container);
                        }
                        break;
                    }
                }
            }
        }
        
        return remaining;
    }
    
    /**
     * Serializes the recipe to NBT
     */
    public BinaryTagLibrary.CompoundTag serializeNBT() {
        BinaryTagLibrary.CompoundTag tag = new BinaryTagLibrary.CompoundTag();
        
        tag.putString("id", id);
        tag.putString("type", type.name());
        tag.putString("category", category);
        tag.putBoolean("shapeless", shapeless);
        tag.putInt("width", width);
        tag.putInt("height", height);
        tag.putInt("craftingTime", craftingTime);
        tag.putInt("experienceReward", experienceReward);
        
        // Serialize ingredients
        BinaryTagLibrary.ListTag ingredientsList = new BinaryTagLibrary.ListTag();
        for (Ingredient ingredient : ingredients) {
            ingredientsList.add(ingredient.serializeNBT());
        }
        tag.put("ingredients", ingredientsList);
        
        // Serialize result
        tag.put("result", result.serializeNBT());
        
        // Serialize extra results
        BinaryTagLibrary.ListTag extraResultsList = new BinaryTagLibrary.ListTag();
        for (ItemStack stack : extraResults) {
            extraResultsList.add(stack.serializeNBT());
        }
        tag.put("extraResults", extraResultsList);
        
        // Serialize properties
        BinaryTagLibrary.CompoundTag propertiesTag = new BinaryTagLibrary.CompoundTag();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                propertiesTag.putString(key, (String) value);
            } else if (value instanceof Integer) {
                propertiesTag.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                propertiesTag.putDouble(key, (Double) value);
            } else if (value instanceof Boolean) {
                propertiesTag.putBoolean(key, (Boolean) value);
            }
        }
        tag.put("properties", propertiesTag);
        
        // Serialize required tools and skills
        BinaryTagLibrary.ListTag toolsList = new BinaryTagLibrary.ListTag();
        for (String tool : requiredTools) {
            toolsList.add(new BinaryTagLibrary.StringTag(tool));
        }
        tag.put("requiredTools", toolsList);
        
        BinaryTagLibrary.ListTag skillsList = new BinaryTagLibrary.ListTag();
        for (String skill : requiredSkills) {
            skillsList.add(new BinaryTagLibrary.StringTag(skill));
        }
        tag.put("requiredSkills", skillsList);
        
        return tag;
    }
    
    /**
     * Creates a recipe builder
     */
    public static Builder builder(String id, RecipeType type) {
        return new Builder(id, type);
    }
    
    /**
     * Recipe builder class
     */
    public static class Builder {
        private final String id;
        private final RecipeType type;
        private final List<Ingredient> ingredients = new ArrayList<>();
        private ItemStack result = ItemStack.EMPTY;
        private final List<ItemStack> extraResults = new ArrayList<>();
        private final Map<String, Object> properties = new HashMap<>();
        private final Set<String> requiredTools = new HashSet<>();
        private final Set<String> requiredSkills = new HashSet<>();
        private int craftingTime = 20; // 1 second default
        private int experienceReward = 0;
        private String category = "misc";
        private boolean shapeless = true;
        private int width = 1;
        private int height = 1;
        
        private Builder(String id, RecipeType type) {
            this.id = id;
            this.type = type;
        }
        
        public Builder ingredient(Ingredient ingredient) {
            ingredients.add(ingredient);
            return this;
        }
        
        public Builder ingredient(Item item) {
            return ingredient(Ingredient.of(item));
        }
        
        public Builder ingredient(Item item, int count) {
            return ingredient(Ingredient.of(item, count));
        }
        
        public Builder ingredient(ItemStack stack) {
            return ingredient(Ingredient.of(stack));
        }
        
        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }
        
        public Builder result(Item item) {
            return result(new ItemStack(item, 1));
        }
        
        public Builder result(Item item, int count) {
            return result(new ItemStack(item, count));
        }
        
        public Builder extraResult(ItemStack result) {
            extraResults.add(result);
            return this;
        }
        
        public Builder extraResult(Item item) {
            return extraResult(new ItemStack(item, 1));
        }
        
        public Builder extraResult(Item item, int count) {
            return extraResult(new ItemStack(item, count));
        }
        
        public Builder property(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        public Builder requireTool(String tool) {
            requiredTools.add(tool);
            return this;
        }
        
        public Builder requireSkill(String skill) {
            requiredSkills.add(skill);
            return this;
        }
        
        public Builder craftingTime(int ticks) {
            this.craftingTime = ticks;
            return this;
        }
        
        public Builder experience(int experience) {
            this.experienceReward = experience;
            return this;
        }
        
        public Builder category(String category) {
            this.category = category;
            return this;
        }
        
        public Builder shapeless() {
            this.shapeless = true;
            return this;
        }
        
        public Builder shaped(int width, int height) {
            this.shapeless = false;
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Recipe build() {
            if (result.isEmpty()) {
                throw new IllegalStateException("Recipe must have a result");
            }
            return new Recipe(this);
        }
    }
    
    /**
     * Recipe types
     */
    public enum RecipeType {
        CRAFTING,
        SMELTING,
        COOKING,
        BREWING,
        ENCHANTING,
        SHIPBUILDING,
        CANNON_CRAFTING,
        SAIL_MAKING,
        ROPE_MAKING,
        TREASURE_HUNTING,
        ALCHEMY,
        BLACKSMITHING,
        CARPENTRY,
        TAILORING
    }
}