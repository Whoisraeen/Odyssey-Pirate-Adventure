package com.odyssey.world.save.stats;

/**
 * Enumeration of maritime achievements that can be unlocked by players.
 * Each achievement represents a significant milestone in maritime gameplay.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum MaritimeAchievement {
    // Navigation Achievements
    LONGEST_VOYAGE("Master Navigator", "Complete the longest single voyage", "distance"),
    FIRST_ISLAND("Island Hopper", "Discover your first island", "discovery"),
    HUNDRED_ISLANDS("Archipelago Explorer", "Discover 100 unique islands", "discovery"),
    CIRCUMNAVIGATOR("World Sailor", "Sail around the entire world", "distance"),
    
    // Combat Achievements
    FIRST_VICTORY("First Blood", "Win your first naval battle", "combat"),
    BATTLE_MASTER("Battle Master", "Win 100 naval battles", "combat"),
    KRAKEN_SLAYER("Kraken Slayer", "Defeat a legendary kraken", "combat"),
    FLEET_DESTROYER("Fleet Destroyer", "Sink 50 enemy ships", "combat"),
    
    // Treasure and Economy
    FIRST_TREASURE("Treasure Hunter", "Find your first treasure chest", "treasure"),
    LARGEST_TREASURE("Legendary Hoard", "Find the largest treasure in the world", "treasure"),
    MILLIONAIRE("Maritime Millionaire", "Accumulate 1,000,000 gold", "economy"),
    TRADE_BARON("Trade Baron", "Complete 500 trade deals", "economy"),
    
    // Exploration Achievements
    DEEPEST_DIVE("Deep Sea Explorer", "Reach the deepest point in the ocean", "exploration"),
    CAVE_EXPLORER("Cave Diver", "Explore 50 underwater caves", "exploration"),
    SHIPWRECK_HUNTER("Shipwreck Hunter", "Investigate 25 shipwrecks", "exploration"),
    SECRET_KEEPER("Secret Keeper", "Discover 10 hidden secrets", "exploration"),
    
    // Survival Achievements
    STORM_SURVIVOR("Storm Rider", "Survive 10 severe storms", "survival"),
    WHIRLPOOL_ESCAPE("Whirlpool Escape Artist", "Escape from 5 whirlpools", "survival"),
    LIGHTNING_SURVIVOR("Lightning Rod", "Survive 20 lightning strikes", "survival"),
    DEATH_DEFIER("Death Defier", "Survive 100 near-death experiences", "survival"),
    
    // Shipbuilding Achievements
    MASTER_BUILDER("Master Shipwright", "Build 10 different ship designs", "building"),
    SPEED_DEMON("Speed Demon", "Achieve the fastest ship speed record", "building"),
    FLEET_COMMANDER("Fleet Commander", "Own 5 ships simultaneously", "building"),
    UPGRADE_MASTER("Upgrade Master", "Perform 100 ship upgrades", "building"),
    
    // Fishing and Marine Life
    ANGLER("Master Angler", "Catch 1000 fish", "fishing"),
    RARE_CATCH("Rare Species Hunter", "Catch 50 rare fish species", "fishing"),
    MARINE_BIOLOGIST("Marine Biologist", "Encounter 100 different marine creatures", "marine"),
    LEVIATHAN_FRIEND("Leviathan Whisperer", "Befriend a legendary sea creature", "marine"),
    
    // Social Achievements
    CREW_CAPTAIN("Crew Captain", "Recruit 20 crew members", "social"),
    ALLIANCE_BUILDER("Alliance Builder", "Form 10 player alliances", "social"),
    HELPFUL_SAILOR("Helpful Sailor", "Answer 100 help requests", "social"),
    COMMUNITY_LEADER("Community Leader", "Lead a successful community event", "social"),
    
    // Time and Dedication
    VETERAN_SAILOR("Veteran Sailor", "Play for 100 hours", "dedication"),
    DAILY_SAILOR("Daily Sailor", "Play for 30 consecutive days", "dedication"),
    MARATHON_SESSION("Marathon Sailor", "Play for 8 hours in a single session", "dedication"),
    LOYAL_CAPTAIN("Loyal Captain", "Play for one full year", "dedication"),
    
    // Special Achievements
    FIRST_PLAYER("Pioneer", "Be the first player on the server", "special"),
    BETA_TESTER("Beta Tester", "Participate in beta testing", "special"),
    BUG_HUNTER("Bug Hunter", "Report 10 confirmed bugs", "special"),
    COMMUNITY_CONTRIBUTOR("Community Contributor", "Contribute to the community", "special");
    
    private final String title;
    private final String description;
    private final String category;
    
    /**
     * Creates a new maritime achievement.
     * 
     * @param title the achievement title
     * @param description the achievement description
     * @param category the achievement category
     */
    MaritimeAchievement(String title, String description, String category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }
    
    /**
     * Gets the achievement title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the achievement description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the achievement category.
     * 
     * @return the category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Checks if this is a combat achievement.
     * 
     * @return true if this is a combat achievement
     */
    public boolean isCombat() {
        return category.equals("combat");
    }
    
    /**
     * Checks if this is an exploration achievement.
     * 
     * @return true if this is an exploration achievement
     */
    public boolean isExploration() {
        return category.equals("exploration") || category.equals("discovery");
    }
    
    /**
     * Checks if this is an economy achievement.
     * 
     * @return true if this is an economy achievement
     */
    public boolean isEconomy() {
        return category.equals("economy") || category.equals("treasure");
    }
    
    /**
     * Checks if this is a social achievement.
     * 
     * @return true if this is a social achievement
     */
    public boolean isSocial() {
        return category.equals("social");
    }
    
    /**
     * Checks if this is a rare achievement.
     * 
     * @return true if this is a rare achievement
     */
    public boolean isRare() {
        return this == KRAKEN_SLAYER || this == LARGEST_TREASURE || 
               this == CIRCUMNAVIGATOR || this == LEVIATHAN_FRIEND ||
               this == FIRST_PLAYER || this == DEEPEST_DIVE;
    }
    
    /**
     * Gets the achievement rarity.
     * 
     * @return the rarity level
     */
    public AchievementRarity getRarity() {
        if (isRare()) {
            return AchievementRarity.LEGENDARY;
        } else if (category.equals("special")) {
            return AchievementRarity.EPIC;
        } else if (this == BATTLE_MASTER || this == HUNDRED_ISLANDS || 
                   this == MILLIONAIRE || this == VETERAN_SAILOR) {
            return AchievementRarity.RARE;
        } else if (title.contains("Master") || title.contains("Commander")) {
            return AchievementRarity.UNCOMMON;
        } else {
            return AchievementRarity.COMMON;
        }
    }
    
    /**
     * Gets the point value of this achievement.
     * 
     * @return the point value
     */
    public int getPointValue() {
        switch (getRarity()) {
            case LEGENDARY: return 1000;
            case EPIC: return 500;
            case RARE: return 250;
            case UNCOMMON: return 100;
            case COMMON: return 50;
            default: return 25;
        }
    }
}