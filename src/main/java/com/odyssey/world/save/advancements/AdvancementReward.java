package com.odyssey.world.save.advancements;

public class AdvancementReward {

    public enum Type {
        EXPERIENCE, GOLD, ITEM, TITLE
    }

    private Type type;
    private int value;
    private String itemId;
    private String title;

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
}
