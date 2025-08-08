package com.odyssey.world.save.advancements;

/**
 * Defines rewards for completing advancements.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AdvancementReward {
    private RewardType type;
    private int value;
    private String itemId;
    private String title;
    private String description;
    
    public AdvancementReward(RewardType type, int value) {
        this.type = type;
        this.value = value;
    }
    
    public AdvancementReward(RewardType type, String itemId, int value) {
        this.type = type;
        this.itemId = itemId;
        this.value = value;
    }
    
    public AdvancementReward(RewardType type, String title, String description) {
        this.type = type;
        this.title = title;
        this.description = description;
    }
    
    public RewardType getType() { return type; }
    public int getValue() { return value; }
    public String getItemId() { return itemId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}

enum RewardType {
    EXPERIENCE, GOLD, ITEM, TITLE
}