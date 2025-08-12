package com.odyssey.world.save.advancements;

import java.util.Collection;
import java.util.Collections;

public class Advancement {

    private String title;
    private Collection<AdvancementReward> rewards;

    public String getTitle() {
        return title;
    }

    public Collection<AdvancementReward> getRewards() {
        return rewards != null ? rewards : Collections.emptyList();
    }
}
