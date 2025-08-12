package com.odyssey.crafting;

import com.odyssey.items.Item;
import com.odyssey.items.ItemStack;
import com.odyssey.data.BinaryTagLibrary;
import java.util.*;
import java.util.function.Predicate;

/**
 * Represents an ingredient in a recipe.
 * Can match items by type, tag, or custom predicate.
 */
public class Ingredient {
    private final Set<Item> items;
    private final Set<String> tags;
    private final Predicate<ItemStack> customMatcher;
    private final int count;
    private final boolean consumeContainer;
    private final ItemStack containerItem;
    private final Map<String, Object> properties;
    
    private Ingredient(Builder builder) {
        this.items = new HashSet<>(builder.items);
        this.tags = new HashSet<>(builder.tags);
        this.customMatcher = builder.customMatcher;
        this.count = builder.count;
        this.consumeContainer = builder.consumeContainer;
        this.containerItem = builder.containerItem;
        this.properties = new HashMap<>(builder.properties);
    }
    
    /**
     * Gets the required count
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Gets the accepted items
     */
    public Set<Item> getItems() {
        return new HashSet<>(items);
    }
    
    /**
     * Gets the accepted tags
     */
    public Set<String> getTags() {
        return new HashSet<>(tags);
    }
    
    /**
     * Gets ingredient properties
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    /**
     * Checks if this ingredient matches the given item stack
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        // Check count requirement
        if (stack.getCount() < count) {
            return false;
        }
        
        // Check specific items
        if (!items.isEmpty() && items.contains(stack.getItem())) {
            return true;
        }
        
        // Check tags
        if (!tags.isEmpty()) {
            for (String tag : tags) {
                if (stack.getItem().hasTag(tag)) {
                    return true;
                }
            }
        }
        
        // Check custom matcher
        if (customMatcher != null) {
            return customMatcher.test(stack);
        }
        
        return items.isEmpty() && tags.isEmpty() && customMatcher == null;
    }
    
    /**
     * Gets the container item after consumption
     */
    public ItemStack getContainerItem(ItemStack consumed) {
        if (!consumeContainer && !containerItem.isEmpty()) {
            return containerItem.copy();
        }
        
        // Check if the consumed item has a container item
        Item item = consumed.getItem();
        if (item.hasContainerItem()) {
            return new ItemStack(item.getContainerItem(), 1);
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Gets all possible matching items
     */
    public List<ItemStack> getMatchingStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        
        // Add specific items
        for (Item item : items) {
            stacks.add(new ItemStack(item, count));
        }
        
        // Add items from tags (this would require a tag registry in practice)
        // For now, we'll just return the specific items
        
        return stacks;
    }
    
    /**
     * Checks if this ingredient is empty
     */
    public boolean isEmpty() {
        return items.isEmpty() && tags.isEmpty() && customMatcher == null;
    }
    
    /**
     * Serializes the ingredient to NBT
     */
    public BinaryTagLibrary.CompoundTag serializeNBT() {
        BinaryTagLibrary.CompoundTag tag = new BinaryTagLibrary.CompoundTag();
        
        tag.putInt("count", count);
        tag.putBoolean("consumeContainer", consumeContainer);
        
        // Serialize items
        BinaryTagLibrary.ListTag itemsList = new BinaryTagLibrary.ListTag();
        for (Item item : items) {
            itemsList.add(new BinaryTagLibrary.StringTag(item.getRegistryName()));
        }
        tag.put("items", itemsList);
        
        // Serialize tags
        BinaryTagLibrary.ListTag tagsList = new BinaryTagLibrary.ListTag();
        for (String itemTag : tags) {
            tagsList.add(new BinaryTagLibrary.StringTag(itemTag));
        }
        tag.put("tags", tagsList);
        
        // Serialize container item
        if (!containerItem.isEmpty()) {
            tag.put("containerItem", containerItem.serializeNBT());
        }
        
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
        
        return tag;
    }
    
    /**
     * Creates an ingredient from a single item
     */
    public static Ingredient of(Item item) {
        return builder().item(item).build();
    }
    
    /**
     * Creates an ingredient from a single item with count
     */
    public static Ingredient of(Item item, int count) {
        return builder().item(item).count(count).build();
    }
    
    /**
     * Creates an ingredient from an item stack
     */
    public static Ingredient of(ItemStack stack) {
        return builder().item(stack.getItem()).count(stack.getCount()).build();
    }
    
    /**
     * Creates an ingredient from multiple items
     */
    public static Ingredient of(Item... items) {
        Builder builder = builder();
        for (Item item : items) {
            builder.item(item);
        }
        return builder.build();
    }
    
    /**
     * Creates an ingredient from a tag
     */
    public static Ingredient ofTag(String tag) {
        return builder().tag(tag).build();
    }
    
    /**
     * Creates an ingredient from a tag with count
     */
    public static Ingredient ofTag(String tag, int count) {
        return builder().tag(tag).count(count).build();
    }
    
    /**
     * Creates an ingredient with a custom matcher
     */
    public static Ingredient of(Predicate<ItemStack> matcher) {
        return builder().customMatcher(matcher).build();
    }
    
    /**
     * Creates an ingredient with a custom matcher and count
     */
    public static Ingredient of(Predicate<ItemStack> matcher, int count) {
        return builder().customMatcher(matcher).count(count).build();
    }
    
    /**
     * Creates an empty ingredient
     */
    public static Ingredient empty() {
        return builder().build();
    }
    
    /**
     * Creates an ingredient builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Ingredient builder class
     */
    public static class Builder {
        private final Set<Item> items = new HashSet<>();
        private final Set<String> tags = new HashSet<>();
        private Predicate<ItemStack> customMatcher;
        private int count = 1;
        private boolean consumeContainer = true;
        private ItemStack containerItem = ItemStack.EMPTY;
        private final Map<String, Object> properties = new HashMap<>();
        
        public Builder item(Item item) {
            items.add(item);
            return this;
        }
        
        public Builder items(Item... items) {
            this.items.addAll(Arrays.asList(items));
            return this;
        }
        
        public Builder items(Collection<Item> items) {
            this.items.addAll(items);
            return this;
        }
        
        public Builder tag(String tag) {
            tags.add(tag);
            return this;
        }
        
        public Builder tags(String... tags) {
            this.tags.addAll(Arrays.asList(tags));
            return this;
        }
        
        public Builder tags(Collection<String> tags) {
            this.tags.addAll(tags);
            return this;
        }
        
        public Builder customMatcher(Predicate<ItemStack> matcher) {
            this.customMatcher = matcher;
            return this;
        }
        
        public Builder count(int count) {
            this.count = Math.max(1, count);
            return this;
        }
        
        public Builder consumeContainer(boolean consume) {
            this.consumeContainer = consume;
            return this;
        }
        
        public Builder containerItem(ItemStack container) {
            this.containerItem = container;
            return this;
        }
        
        public Builder containerItem(Item container) {
            return containerItem(new ItemStack(container, 1));
        }
        
        public Builder property(String key, Object value) {
            properties.put(key, value);
            return this;
        }
        
        // Convenience methods for common ingredient types
        public Builder wood() {
            return tag("wood");
        }
        
        public Builder stone() {
            return tag("stone");
        }
        
        public Builder metal() {
            return tag("metal");
        }
        
        public Builder food() {
            return tag("food");
        }
        
        public Builder tool() {
            return tag("tool");
        }
        
        public Builder weapon() {
            return tag("weapon");
        }
        
        public Builder armor() {
            return tag("armor");
        }
        
        public Builder fuel() {
            return tag("fuel");
        }
        
        public Builder liquid() {
            return tag("liquid");
        }
        
        public Builder gem() {
            return tag("gem");
        }
        
        public Builder rope() {
            return tag("rope");
        }
        
        public Builder sail() {
            return tag("sail");
        }
        
        public Builder cannon() {
            return tag("cannon");
        }
        
        public Builder treasure() {
            return tag("treasure");
        }
        
        // Damage-based matching
        public Builder damaged() {
            return customMatcher(stack -> stack.isDamaged());
        }
        
        public Builder undamaged() {
            return customMatcher(stack -> !stack.isDamaged());
        }
        
        public Builder maxDamage(int maxDamage) {
            return customMatcher(stack -> stack.getDamage() <= maxDamage);
        }
        
        public Builder minDamage(int minDamage) {
            return customMatcher(stack -> stack.getDamage() >= minDamage);
        }
        
        // Enchantment-based matching
        public Builder enchanted() {
            return customMatcher(stack -> stack.isEnchanted());
        }
        
        public Builder unenchanted() {
            return customMatcher(stack -> !stack.isEnchanted());
        }
        
        // NBT-based matching
        public Builder hasNbt() {
            return customMatcher(stack -> stack.hasNbt());
        }
        
        public Builder noNbt() {
            return customMatcher(stack -> !stack.hasNbt());
        }
        
        public Builder nbtMatches(String key, Object value) {
            return customMatcher(stack -> {
                if (!stack.hasNbt()) return false;
                BinaryTagLibrary.CompoundTag nbt = stack.getNbt();
                
                if (value instanceof String) {
                    return nbt.getString(key).equals(value);
                } else if (value instanceof Integer) {
                    return nbt.getInt(key) == (Integer) value;
                } else if (value instanceof Boolean) {
                    return nbt.getBoolean(key) == (Boolean) value;
                } else if (value instanceof Double) {
                    return nbt.getDouble(key) == (Double) value;
                }
                
                return false;
            });
        }
        
        public Ingredient build() {
            return new Ingredient(this);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Ingredient{");
        
        if (!items.isEmpty()) {
            sb.append("items=").append(items.size()).append(", ");
        }
        
        if (!tags.isEmpty()) {
            sb.append("tags=").append(tags).append(", ");
        }
        
        if (customMatcher != null) {
            sb.append("customMatcher=true, ");
        }
        
        sb.append("count=").append(count);
        sb.append("}");
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Ingredient that = (Ingredient) obj;
        return count == that.count &&
               consumeContainer == that.consumeContainer &&
               Objects.equals(items, that.items) &&
               Objects.equals(tags, that.tags) &&
               Objects.equals(containerItem, that.containerItem) &&
               Objects.equals(properties, that.properties);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(items, tags, count, consumeContainer, containerItem, properties);
    }
}