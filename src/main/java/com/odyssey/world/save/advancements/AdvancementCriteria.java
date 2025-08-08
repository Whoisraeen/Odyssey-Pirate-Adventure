package com.odyssey.world.save.advancements;

/**
 * Defines criteria for advancement completion.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class AdvancementCriteria {
    private String id;
    private CriteriaType type;
    private String trigger;
    private int requiredCount;
    private String description;
    
    public AdvancementCriteria(String id, CriteriaType type, String trigger, int requiredCount) {
        this.id = id;
        this.type = type;
        this.trigger = trigger;
        this.requiredCount = requiredCount;
    }
    
    public String getId() { return id; }
    public CriteriaType getType() { return type; }
    public String getTrigger() { return trigger; }
    public int getRequiredCount() { return requiredCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

enum CriteriaType {
    STATISTIC, ITEM_USE, LOCATION, KILL, CRAFT, CUSTOM
}