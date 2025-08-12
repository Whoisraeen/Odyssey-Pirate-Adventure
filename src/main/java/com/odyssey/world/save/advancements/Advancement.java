package com.odyssey.world.save.advancements;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Advancement {

    private String id;
    private String title;
    private String description;
    private AdvancementType type;
    private Map<String, AdvancementCriteria> criteria;
    private List<AdvancementReward> rewards;

    public Advancement(String id, String title, String description, AdvancementType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public AdvancementType getType() {
        return type;
    }

    public Map<String, AdvancementCriteria> getCriteria() {
        return criteria;
    }

    public void setCriteria(Map<String, AdvancementCriteria> criteria) {
        this.criteria = criteria;
    }

    public List<AdvancementReward> getRewards() {
        return rewards != null ? rewards : Collections.emptyList();
    }

    public void setRewards(List<AdvancementReward> rewards) {
        this.rewards = rewards;
    }
}
