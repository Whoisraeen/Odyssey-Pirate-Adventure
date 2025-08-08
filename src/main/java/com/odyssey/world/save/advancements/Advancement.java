package com.odyssey.world.save.advancements;

import java.util.List;
import java.util.Map;

/**
 * Defines a maritime advancement with criteria and rewards.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class Advancement {
    private String id;
    private String title;
    private String description;
    private AdvancementType type;
    private String parentId;
    private Map<String, AdvancementCriteria> criteria;
    private List<AdvancementReward> rewards;
    private boolean hidden;
    private String iconItem;
    private AdvancementFrame frame;
    
    public Advancement(String id, String title, String description, AdvancementType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.hidden = false;
        this.frame = AdvancementFrame.TASK;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public AdvancementType getType() { return type; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public Map<String, AdvancementCriteria> getCriteria() { return criteria; }
    public void setCriteria(Map<String, AdvancementCriteria> criteria) { this.criteria = criteria; }
    public List<AdvancementReward> getRewards() { return rewards; }
    public void setRewards(List<AdvancementReward> rewards) { this.rewards = rewards; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public String getIconItem() { return iconItem; }
    public void setIconItem(String iconItem) { this.iconItem = iconItem; }
    public AdvancementFrame getFrame() { return frame; }
    public void setFrame(AdvancementFrame frame) { this.frame = frame; }
    
    public boolean hasParent() { return parentId != null; }
    public int getCriteriaCount() { return criteria != null ? criteria.size() : 0; }
}