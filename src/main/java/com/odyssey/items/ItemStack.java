package com.odyssey.items;

import com.odyssey.data.BinaryTagLibrary;
import java.util.*;

/**
 * Represents a stack of items with quantity, metadata, and NBT data.
 * Core class for the inventory and crafting systems.
 */
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack();
    
    private Item item;
    private int count;
    private int damage;
    private BinaryTagLibrary.CompoundTag nbt;
    private Map<String, Object> metadata;
    
    // Cached values for performance
    private transient String displayName;
    private transient List<String> lore;
    private transient boolean isDirty = true;
    
    /**
     * Creates an empty item stack
     */
    public ItemStack() {
        this.item = Items.AIR;
        this.count = 0;
        this.damage = 0;
        this.nbt = null;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Creates an item stack with the specified item and count
     */
    public ItemStack(Item item, int count) {
        this.item = item != null ? item : Items.AIR;
        this.count = Math.max(0, count);
        this.damage = 0;
        this.nbt = null;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Creates an item stack with the specified item, count, and damage
     */
    public ItemStack(Item item, int count, int damage) {
        this.item = item != null ? item : Items.AIR;
        this.count = Math.max(0, count);
        this.damage = Math.max(0, damage);
        this.nbt = null;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Copy constructor
     */
    public ItemStack(ItemStack other) {
        this.item = other.item;
        this.count = other.count;
        this.damage = other.damage;
        this.nbt = other.nbt != null ? other.nbt.copy() : null;
        this.metadata = new HashMap<>(other.metadata);
        this.displayName = other.displayName;
        this.lore = other.lore != null ? new ArrayList<>(other.lore) : null;
        this.isDirty = other.isDirty;
    }
    
    /**
     * Checks if this item stack is empty
     */
    public boolean isEmpty() {
        return count <= 0 || item == Items.AIR;
    }
    
    /**
     * Gets the item
     */
    public Item getItem() {
        return item;
    }
    
    /**
     * Sets the item
     */
    public void setItem(Item item) {
        this.item = item != null ? item : Items.AIR;
        markDirty();
    }
    
    /**
     * Gets the count
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Sets the count
     */
    public void setCount(int count) {
        this.count = Math.max(0, count);
        markDirty();
    }
    
    /**
     * Grows the stack by the specified amount
     */
    public void grow(int amount) {
        setCount(count + amount);
    }
    
    /**
     * Shrinks the stack by the specified amount
     */
    public void shrink(int amount) {
        setCount(count - amount);
    }
    
    /**
     * Gets the damage value
     */
    public int getDamage() {
        return damage;
    }
    
    /**
     * Sets the damage value
     */
    public void setDamage(int damage) {
        this.damage = Math.max(0, Math.min(getMaxDamage(), damage));
        markDirty();
    }
    
    /**
     * Gets the maximum damage this item can take
     */
    public int getMaxDamage() {
        return item.getMaxDamage();
    }
    
    /**
     * Checks if this item is damageable
     */
    public boolean isDamageable() {
        return item.isDamageable() && getMaxDamage() > 0;
    }
    
    /**
     * Checks if this item is damaged
     */
    public boolean isDamaged() {
        return isDamageable() && damage > 0;
    }
    
    /**
     * Gets the durability percentage (0.0 to 1.0)
     */
    public float getDurabilityPercent() {
        if (!isDamageable()) return 1.0f;
        return 1.0f - ((float) damage / getMaxDamage());
    }
    
    /**
     * Damages the item by the specified amount
     */
    public void damageItem(int amount) {
        if (isDamageable()) {
            setDamage(damage + amount);
            if (damage >= getMaxDamage()) {
                setCount(0); // Item breaks
            }
        }
    }
    
    /**
     * Repairs the item by the specified amount
     */
    public void repairItem(int amount) {
        if (isDamageable()) {
            setDamage(Math.max(0, damage - amount));
        }
    }
    
    /**
     * Gets the NBT data
     */
    public BinaryTagLibrary.CompoundTag getNbt() {
        return nbt;
    }
    
    /**
     * Sets the NBT data
     */
    public void setNbt(BinaryTagLibrary.CompoundTag nbt) {
        this.nbt = nbt;
        markDirty();
    }
    
    /**
     * Gets or creates NBT data
     */
    public BinaryTagLibrary.CompoundTag getOrCreateNbt() {
        if (nbt == null) {
            nbt = new BinaryTagLibrary.CompoundTag();
        }
        return nbt;
    }
    
    /**
     * Checks if this item stack has NBT data
     */
    public boolean hasNbt() {
        return nbt != null && !nbt.isEmpty();
    }
    
    /**
     * Gets metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type, T defaultValue) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Sets metadata value
     */
    public void setMetadata(String key, Object value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
        markDirty();
    }
    
    /**
     * Checks if metadata exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Gets the display name
     */
    public String getDisplayName() {
        if (isDirty || displayName == null) {
            updateDisplayInfo();
        }
        return displayName;
    }
    
    /**
     * Sets a custom display name
     */
    public void setDisplayName(String name) {
        if (name == null || name.trim().isEmpty()) {
            getOrCreateNbt().remove("display");
        } else {
            BinaryTagLibrary.CompoundTag display = getOrCreateNbt().getCompound("display");
            if (display == null) {
                display = new BinaryTagLibrary.CompoundTag();
                getOrCreateNbt().put("display", display);
            }
            display.putString("Name", name);
        }
        markDirty();
    }
    
    /**
     * Gets the lore (description lines)
     */
    public List<String> getLore() {
        if (isDirty || lore == null) {
            updateDisplayInfo();
        }
        return lore != null ? new ArrayList<>(lore) : new ArrayList<>();
    }
    
    /**
     * Sets the lore
     */
    public void setLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            if (hasNbt()) {
                BinaryTagLibrary.CompoundTag display = nbt.getCompound("display");
                if (display != null) {
                    display.remove("Lore");
                }
            }
        } else {
            BinaryTagLibrary.CompoundTag display = getOrCreateNbt().getCompound("display");
            if (display == null) {
                display = new BinaryTagLibrary.CompoundTag();
                getOrCreateNbt().put("display", display);
            }
            BinaryTagLibrary.ListTag loreList = new BinaryTagLibrary.ListTag();
            for (String line : lore) {
                loreList.add(new BinaryTagLibrary.StringTag(line));
            }
            display.put("Lore", loreList);
        }
        markDirty();
    }
    
    /**
     * Updates display information from NBT
     */
    private void updateDisplayInfo() {
        // Get display name
        if (hasNbt()) {
            BinaryTagLibrary.CompoundTag display = nbt.getCompound("display");
            if (display != null) {
                if (display.contains("Name")) {
                    displayName = display.getString("Name");
                } else {
                    displayName = item.getDisplayName();
                }
                
                // Get lore
                if (display.contains("Lore")) {
                    BinaryTagLibrary.ListTag loreList = display.getList("Lore");
                    lore = new ArrayList<>();
                    for (int i = 0; i < loreList.size(); i++) {
                        lore.add(loreList.getString(i));
                    }
                } else {
                    lore = item.getDefaultLore();
                }
            } else {
                displayName = item.getDisplayName();
                lore = item.getDefaultLore();
            }
        } else {
            displayName = item.getDisplayName();
            lore = item.getDefaultLore();
        }
        
        isDirty = false;
    }
    
    /**
     * Marks the item stack as dirty (needs display update)
     */
    private void markDirty() {
        isDirty = true;
    }
    
    /**
     * Gets the maximum stack size
     */
    public int getMaxStackSize() {
        return item.getMaxStackSize();
    }
    
    /**
     * Checks if this item stack can be stacked with another
     */
    public boolean isStackable(ItemStack other) {
        if (other == null || other.isEmpty() || isEmpty()) {
            return false;
        }
        
        return item == other.item && 
               damage == other.damage && 
               Objects.equals(nbt, other.nbt) &&
               Objects.equals(metadata, other.metadata);
    }
    
    /**
     * Attempts to merge with another item stack
     */
    public int merge(ItemStack other) {
        if (!isStackable(other)) {
            return 0;
        }
        
        int maxStack = getMaxStackSize();
        int available = maxStack - count;
        int toMerge = Math.min(available, other.count);
        
        if (toMerge > 0) {
            grow(toMerge);
            other.shrink(toMerge);
        }
        
        return toMerge;
    }
    
    /**
     * Splits this item stack
     */
    public ItemStack split(int amount) {
        if (amount <= 0) {
            return EMPTY;
        }
        
        int toSplit = Math.min(amount, count);
        ItemStack result = copy();
        result.setCount(toSplit);
        shrink(toSplit);
        
        return result;
    }
    
    /**
     * Creates a copy of this item stack
     */
    public ItemStack copy() {
        return new ItemStack(this);
    }
    
    /**
     * Checks if this item stack is enchanted
     */
    public boolean isEnchanted() {
        return nbt != null && nbt.contains("Enchantments") && !nbt.getList("Enchantments").getValue().isEmpty();
    }
    
    /**
     * Checks if this item stack is equal to another
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemStack)) return false;
        
        ItemStack other = (ItemStack) obj;
        return item == other.item &&
               count == other.count &&
               damage == other.damage &&
               Objects.equals(nbt, other.nbt) &&
               Objects.equals(metadata, other.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(item, count, damage, nbt, metadata);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(count).append("x ").append(item.getRegistryName());
        if (damage > 0) {
            sb.append(" (damage: ").append(damage).append(")");
        }
        if (hasNbt()) {
            sb.append(" {NBT}");
        }
        return sb.toString();
    }
    
    /**
     * Serializes this item stack to NBT
     */
    public BinaryTagLibrary.CompoundTag serializeNBT() {
        BinaryTagLibrary.CompoundTag tag = new BinaryTagLibrary.CompoundTag();
        tag.putString("id", item.getRegistryName());
        tag.putInt("Count", count);
        if (damage > 0) {
            tag.putInt("Damage", damage);
        }
        if (hasNbt()) {
            tag.put("tag", nbt);
        }
        if (!metadata.isEmpty()) {
            BinaryTagLibrary.CompoundTag metaTag = new BinaryTagLibrary.CompoundTag();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                // Simple serialization - extend as needed
                metaTag.putString(entry.getKey(), entry.getValue().toString());
            }
            tag.put("metadata", metaTag);
        }
        return tag;
    }
    
    /**
     * Deserializes an item stack from NBT
     */
    public static ItemStack deserializeNBT(BinaryTagLibrary.CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }
        
        String itemId = tag.getString("id");
        Item item = Items.getItem(itemId);
        if (item == null) {
            return EMPTY;
        }
        
        int count = tag.getInt("Count");
        int damage = tag.getInt("Damage");
        
        ItemStack stack = new ItemStack(item, count, damage);
        
        if (tag.contains("tag")) {
            stack.setNbt(tag.getCompound("tag"));
        }
        
        if (tag.contains("metadata")) {
            BinaryTagLibrary.CompoundTag metaTag = tag.getCompound("metadata");
            for (String key : metaTag.getAllKeys()) {
                stack.setMetadata(key, metaTag.getString(key));
            }
        }
        
        return stack;
    }
}