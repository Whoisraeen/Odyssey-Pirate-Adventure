package com.odyssey.world.save.advancements;

public class AdvancementReward {

    public enum Type {
        EXPERIENCE, GOLD, ITEM, TITLE
    }

    private Type type;
    private int value;
    private String itemId;
    private String title;

    public AdvancementReward(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public AdvancementReward(Type type, String itemId) {
        this.type = type;
        this.itemId = itemId;
    }

    public AdvancementReward(Type type, String itemId, int value) {
        this.type = type;
        this.itemId = itemId;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
