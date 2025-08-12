package com.odyssey.inventory;

import com.odyssey.items.Item;
import com.odyssey.items.ItemStack;
import com.odyssey.data.BinaryTagLibrary;
import java.util.*;
import java.util.function.Predicate;

/**
 * Represents an inventory that can hold ItemStacks.
 * Provides various operations for managing items.
 */
public class Inventory {
    private final ItemStack[] items;
    private final int size;
    private final String name;
    private final Set<InventoryListener> listeners;
    
    // Cached values
    private transient int cachedItemCount = -1;
    private transient boolean isDirty = true;
    
    /**
     * Creates an inventory with the specified size
     */
    public Inventory(int size) {
        this(size, "Inventory");
    }
    
    /**
     * Creates an inventory with the specified size and name
     */
    public Inventory(int size, String name) {
        this.size = Math.max(1, size);
        this.name = name;
        this.items = new ItemStack[this.size];
        this.listeners = new HashSet<>();
        
        // Initialize with empty stacks
        for (int i = 0; i < this.size; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }
    
    /**
     * Gets the size of this inventory
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Gets the name of this inventory
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets an item stack at the specified slot
     */
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= size) {
            return ItemStack.EMPTY;
        }
        return items[slot];
    }
    
    /**
     * Sets an item stack at the specified slot
     */
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size) {
            return;
        }
        
        ItemStack oldStack = items[slot];
        items[slot] = stack != null ? stack : ItemStack.EMPTY;
        
        if (!ItemStack.EMPTY.equals(oldStack) || !ItemStack.EMPTY.equals(items[slot])) {
            markDirty();
            notifyListeners(slot, oldStack, items[slot]);
        }
    }
    
    /**
     * Adds an item stack to the inventory
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack remaining = stack.copy();
        
        // First pass: try to merge with existing stacks
        for (int i = 0; i < size && !remaining.isEmpty(); i++) {
            ItemStack existing = items[i];
            if (!existing.isEmpty() && existing.isStackable(remaining)) {
                int merged = existing.merge(remaining);
                if (merged > 0) {
                    markDirty();
                    notifyListeners(i, existing.copy(), existing);
                }
            }
        }
        
        // Second pass: fill empty slots
        for (int i = 0; i < size && !remaining.isEmpty(); i++) {
            if (items[i].isEmpty()) {
                int toAdd = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack newStack = remaining.copy();
                newStack.setCount(toAdd);
                
                setItem(i, newStack);
                remaining.shrink(toAdd);
            }
        }
        
        return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
    }
    
    /**
     * Removes an item stack from the inventory
     */
    public ItemStack removeItem(int slot) {
        if (slot < 0 || slot >= size) {
            return ItemStack.EMPTY;
        }
        
        ItemStack removed = items[slot];
        setItem(slot, ItemStack.EMPTY);
        return removed;
    }
    
    /**
     * Removes a specific amount of an item from the inventory
     */
    public ItemStack removeItem(Item item, int count) {
        if (item == null || count <= 0) {
            return ItemStack.EMPTY;
        }
        
        ItemStack result = new ItemStack(item, 0);
        int remaining = count;
        
        for (int i = 0; i < size && remaining > 0; i++) {
            ItemStack stack = items[i];
            if (!stack.isEmpty() && stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                result.grow(toRemove);
                remaining -= toRemove;
                
                if (stack.isEmpty()) {
                    setItem(i, ItemStack.EMPTY);
                } else {
                    markDirty();
                    notifyListeners(i, stack.copy(), stack);
                }
            }
        }
        
        return result.isEmpty() ? ItemStack.EMPTY : result;
    }
    
    /**
     * Checks if the inventory contains a specific item
     */
    public boolean contains(Item item) {
        return contains(item, 1);
    }
    
    /**
     * Checks if the inventory contains a specific amount of an item
     */
    public boolean contains(Item item, int count) {
        return getItemCount(item) >= count;
    }
    
    /**
     * Checks if the inventory contains an item stack
     */
    public boolean contains(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        
        int needed = stack.getCount();
        for (ItemStack item : items) {
            if (!item.isEmpty() && item.isStackable(stack)) {
                needed -= item.getCount();
                if (needed <= 0) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Gets the total count of a specific item
     */
    public int getItemCount(Item item) {
        if (item == null) {
            return 0;
        }
        
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Gets the total number of items in the inventory
     */
    public int getTotalItemCount() {
        if (isDirty || cachedItemCount == -1) {
            cachedItemCount = 0;
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    cachedItemCount += stack.getCount();
                }
            }
            isDirty = false;
        }
        return cachedItemCount;
    }
    
    /**
     * Gets the number of occupied slots
     */
    public int getOccupiedSlots() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the number of empty slots
     */
    public int getEmptySlots() {
        return size - getOccupiedSlots();
    }
    
    /**
     * Checks if the inventory is empty
     */
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the inventory is full
     */
    public boolean isFull() {
        return getEmptySlots() == 0;
    }
    
    /**
     * Finds the first slot containing a specific item
     */
    public int findSlot(Item item) {
        for (int i = 0; i < size; i++) {
            if (!items[i].isEmpty() && items[i].getItem() == item) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Finds the first empty slot
     */
    public int findEmptySlot() {
        for (int i = 0; i < size; i++) {
            if (items[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Finds all slots containing a specific item
     */
    public List<Integer> findAllSlots(Item item) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (!items[i].isEmpty() && items[i].getItem() == item) {
                slots.add(i);
            }
        }
        return slots;
    }
    
    /**
     * Finds slots matching a predicate
     */
    public List<Integer> findSlots(Predicate<ItemStack> predicate) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (predicate.test(items[i])) {
                slots.add(i);
            }
        }
        return slots;
    }
    
    /**
     * Swaps items between two slots
     */
    public void swapItems(int slot1, int slot2) {
        if (slot1 < 0 || slot1 >= size || slot2 < 0 || slot2 >= size || slot1 == slot2) {
            return;
        }
        
        ItemStack temp = items[slot1];
        setItem(slot1, items[slot2]);
        setItem(slot2, temp);
    }
    
    /**
     * Moves an item from one slot to another
     */
    public void moveItem(int fromSlot, int toSlot) {
        if (fromSlot < 0 || fromSlot >= size || toSlot < 0 || toSlot >= size || fromSlot == toSlot) {
            return;
        }
        
        ItemStack fromStack = items[fromSlot];
        ItemStack toStack = items[toSlot];
        
        if (fromStack.isEmpty()) {
            return;
        }
        
        if (toStack.isEmpty()) {
            setItem(toSlot, fromStack);
            setItem(fromSlot, ItemStack.EMPTY);
        } else if (fromStack.isStackable(toStack)) {
            int merged = toStack.merge(fromStack);
            if (merged > 0) {
                markDirty();
                notifyListeners(toSlot, toStack.copy(), toStack);
                if (fromStack.isEmpty()) {
                    setItem(fromSlot, ItemStack.EMPTY);
                } else {
                    notifyListeners(fromSlot, fromStack.copy(), fromStack);
                }
            }
        }
    }
    
    /**
     * Splits an item stack in a slot
     */
    public ItemStack splitStack(int slot, int amount) {
        if (slot < 0 || slot >= size || amount <= 0) {
            return ItemStack.EMPTY;
        }
        
        ItemStack stack = items[slot];
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        ItemStack split = stack.split(amount);
        if (stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        } else {
            markDirty();
            notifyListeners(slot, stack.copy(), stack);
        }
        
        return split;
    }
    
    /**
     * Clears the inventory
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }
    
    /**
     * Sorts the inventory
     */
    public void sort() {
        sort(Comparator.comparing((ItemStack stack) -> stack.isEmpty() ? "" : stack.getItem().getRegistryName())
                      .thenComparing(stack -> stack.isEmpty() ? 0 : -stack.getCount()));
    }
    
    /**
     * Sorts the inventory with a custom comparator
     */
    public void sort(Comparator<ItemStack> comparator) {
        List<ItemStack> nonEmptyStacks = new ArrayList<>();
        
        // Collect non-empty stacks
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                nonEmptyStacks.add(stack);
            }
        }
        
        // Sort them
        nonEmptyStacks.sort(comparator);
        
        // Clear inventory and add sorted stacks
        clear();
        for (ItemStack stack : nonEmptyStacks) {
            addItem(stack);
        }
    }
    
    /**
     * Compacts the inventory by merging stackable items
     */
    public void compact() {
        Map<String, List<ItemStack>> stackGroups = new HashMap<>();
        
        // Group stackable items
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                String key = stack.getItem().getRegistryName() + ":" + stack.getDamage() + ":" + 
                           (stack.hasNbt() ? stack.getNbt().hashCode() : 0);
                stackGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(stack);
            }
        }
        
        // Clear and rebuild
        clear();
        for (List<ItemStack> group : stackGroups.values()) {
            if (!group.isEmpty()) {
                ItemStack template = group.get(0);
                int totalCount = group.stream().mapToInt(ItemStack::getCount).sum();
                
                while (totalCount > 0) {
                    int stackSize = Math.min(totalCount, template.getMaxStackSize());
                    ItemStack newStack = template.copy();
                    newStack.setCount(stackSize);
                    addItem(newStack);
                    totalCount -= stackSize;
                }
            }
        }
    }
    
    /**
     * Gets all items in the inventory
     */
    public List<ItemStack> getAllItems() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
        }
        return result;
    }
    
    /**
     * Gets all items of a specific type
     */
    public List<ItemStack> getItemsOfType(Item item) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.getItem() == item) {
                result.add(stack.copy());
            }
        }
        return result;
    }
    
    /**
     * Gets items matching a predicate
     */
    public List<ItemStack> getItems(Predicate<ItemStack> predicate) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (predicate.test(stack)) {
                result.add(stack.copy());
            }
        }
        return result;
    }
    
    /**
     * Adds an inventory listener
     */
    public void addListener(InventoryListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes an inventory listener
     */
    public void removeListener(InventoryListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies listeners of inventory changes
     */
    private void notifyListeners(int slot, ItemStack oldStack, ItemStack newStack) {
        for (InventoryListener listener : listeners) {
            listener.onInventoryChanged(this, slot, oldStack, newStack);
        }
    }
    
    /**
     * Marks the inventory as dirty
     */
    private void markDirty() {
        isDirty = true;
        cachedItemCount = -1;
    }
    
    /**
     * Serializes the inventory to NBT
     */
    public BinaryTagLibrary.CompoundTag serializeNBT() {
        BinaryTagLibrary.CompoundTag tag = new BinaryTagLibrary.CompoundTag();
        tag.putString("name", name);
        tag.putInt("size", size);
        
        BinaryTagLibrary.ListTag itemsList = new BinaryTagLibrary.ListTag();
        for (int i = 0; i < size; i++) {
            if (!items[i].isEmpty()) {
                BinaryTagLibrary.CompoundTag itemTag = items[i].serializeNBT();
                itemTag.putInt("Slot", i);
                itemsList.add(itemTag);
            }
        }
        tag.put("Items", itemsList);
        
        return tag;
    }
    
    /**
     * Deserializes an inventory from NBT
     */
    public static Inventory deserializeNBT(BinaryTagLibrary.CompoundTag tag) {
        String name = tag.getString("name");
        int size = tag.getInt("size");
        
        Inventory inventory = new Inventory(size, name);
        
        BinaryTagLibrary.ListTag itemsList = tag.getList("Items");
        for (int i = 0; i < itemsList.size(); i++) {
            BinaryTagLibrary.CompoundTag itemTag = itemsList.getCompound(i);
            int slot = itemTag.getInt("Slot");
            ItemStack stack = ItemStack.deserializeNBT(itemTag);
            
            if (slot >= 0 && slot < size && !stack.isEmpty()) {
                inventory.setItem(slot, stack);
            }
        }
        
        return inventory;
    }
    
    @Override
    public String toString() {
        return String.format("Inventory{name='%s', size=%d, occupied=%d, items=%d}", 
            name, size, getOccupiedSlots(), getTotalItemCount());
    }
    
    /**
     * Interface for inventory change listeners
     */
    public interface InventoryListener {
        void onInventoryChanged(Inventory inventory, int slot, ItemStack oldStack, ItemStack newStack);
    }
}