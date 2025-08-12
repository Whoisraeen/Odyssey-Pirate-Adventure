package com.odyssey.world.save.advancements;

import java.util.HashMap;
import java.util.Map;

public class AdvancementProgress {

    private String advancementId;
    private boolean completed;
    private Map<String, Integer> criteriaProgress = new HashMap<>();

    public AdvancementProgress(String advancementId) {
        this.advancementId = advancementId;
    }

    public void updateCriteria(String criteriaId, int progress) {
        criteriaProgress.put(criteriaId, progress);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getCompletedCriteriaCount() {
        // This is a stub. A real implementation would check progress against a total.
        return (int) criteriaProgress.values().stream().filter(p -> p >= 100).count();
    }
}