package com.odyssey.world.save.stats;

/**
 * Enumeration of all maritime statistics types tracked in The Odyssey.
 * Each statistic represents a different aspect of maritime gameplay.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum StatType {
    // Navigation and Travel
    DISTANCE_SAILED("Distance Sailed", "blocks", "Total distance sailed across all voyages"),
    LONGEST_VOYAGE_DISTANCE("Longest Voyage", "blocks", "Longest single voyage distance"),
    ISLANDS_DISCOVERED("Islands Discovered", "count", "Number of unique islands discovered"),
    PORTS_VISITED("Ports Visited", "count", "Number of different ports visited"),
    TIME_AT_SEA("Time at Sea", "minutes", "Total time spent sailing"),
    
    // Ship and Construction
    SHIPS_BUILT("Ships Built", "count", "Number of ships constructed"),
    SHIP_UPGRADES("Ship Upgrades", "count", "Number of ship upgrades performed"),
    FASTEST_SHIP_SPEED("Fastest Ship Speed", "blocks/sec", "Fastest recorded ship speed"),
    SHIPS_LOST("Ships Lost", "count", "Number of ships lost to damage or sinking"),
    CANNONS_FIRED("Cannons Fired", "count", "Total number of cannon shots fired"),
    
    // Combat and Battles
    BATTLES_WON("Battles Won", "count", "Number of naval battles won"),
    BATTLES_LOST("Battles Lost", "count", "Number of naval battles lost"),
    ENEMY_SHIPS_SUNK("Enemy Ships Sunk", "count", "Number of enemy ships destroyed"),
    DAMAGE_DEALT("Damage Dealt", "points", "Total damage dealt in combat"),
    DAMAGE_TAKEN("Damage Taken", "points", "Total damage received in combat"),
    
    // Treasure and Economy
    TREASURES_FOUND("Treasures Found", "count", "Number of treasure chests discovered"),
    LARGEST_TREASURE_FOUND("Largest Treasure", "gold", "Value of the largest treasure found"),
    GOLD_EARNED("Gold Earned", "gold", "Total gold earned from all sources"),
    GOLD_SPENT("Gold Spent", "gold", "Total gold spent on purchases"),
    TRADE_DEALS_COMPLETED("Trade Deals", "count", "Number of successful trade transactions"),
    
    // Fishing and Marine Life
    FISH_CAUGHT("Fish Caught", "count", "Total number of fish caught"),
    RARE_FISH_CAUGHT("Rare Fish Caught", "count", "Number of rare fish species caught"),
    FISHING_TIME("Fishing Time", "minutes", "Total time spent fishing"),
    MARINE_CREATURES_ENCOUNTERED("Marine Encounters", "count", "Number of marine creatures encountered"),
    KRAKENS_DEFEATED("Krakens Defeated", "count", "Number of krakens successfully defeated"),
    
    // Exploration and Discovery
    DEEPEST_DIVE("Deepest Dive", "blocks", "Deepest underwater depth reached"),
    UNDERWATER_TIME("Underwater Time", "minutes", "Total time spent underwater"),
    SHIPWRECKS_EXPLORED("Shipwrecks Explored", "count", "Number of shipwrecks investigated"),
    CAVES_EXPLORED("Caves Explored", "count", "Number of underwater caves explored"),
    SECRETS_DISCOVERED("Secrets Discovered", "count", "Number of hidden secrets found"),
    
    // Weather and Environment
    STORMS_SURVIVED("Storms Survived", "count", "Number of severe storms weathered"),
    WHIRLPOOLS_ESCAPED("Whirlpools Escaped", "count", "Number of whirlpools successfully escaped"),
    TIDAL_WAVES_SURVIVED("Tidal Waves Survived", "count", "Number of tidal waves survived"),
    LIGHTNING_STRIKES_SURVIVED("Lightning Survived", "count", "Number of lightning strikes survived"),
    
    // Social and Multiplayer
    CREW_MEMBERS_RECRUITED("Crew Recruited", "count", "Number of crew members recruited"),
    ALLIANCES_FORMED("Alliances Formed", "count", "Number of player alliances formed"),
    MESSAGES_SENT("Messages Sent", "count", "Number of messages sent to other players"),
    HELP_REQUESTS_ANSWERED("Help Answered", "count", "Number of help requests answered"),
    
    // Crafting and Resources
    ITEMS_CRAFTED("Items Crafted", "count", "Total number of items crafted"),
    RESOURCES_GATHERED("Resources Gathered", "count", "Total resources collected"),
    TOOLS_BROKEN("Tools Broken", "count", "Number of tools broken from use"),
    REPAIRS_PERFORMED("Repairs Performed", "count", "Number of ship repairs completed"),
    
    // Time and Sessions
    PLAY_SESSIONS("Play Sessions", "count", "Number of game sessions played"),
    LONGEST_SESSION("Longest Session", "minutes", "Duration of longest play session"),
    DAYS_PLAYED("Days Played", "count", "Number of different days played"),
    
    // Deaths and Respawns
    DEATHS("Deaths", "count", "Total number of deaths"),
    DROWNING_DEATHS("Drowning Deaths", "count", "Number of deaths by drowning"),
    COMBAT_DEATHS("Combat Deaths", "count", "Number of deaths in combat"),
    RESPAWNS("Respawns", "count", "Total number of respawns");
    
    private final String displayName;
    private final String unit;
    private final String description;
    
    /**
     * Creates a new statistic type.
     * 
     * @param displayName the human-readable name
     * @param unit the unit of measurement
     * @param description the description of the statistic
     */
    StatType(String displayName, String unit, String description) {
        this.displayName = displayName;
        this.unit = unit;
        this.description = description;
    }
    
    /**
     * Gets the display name of the statistic.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the unit of measurement.
     * 
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }
    
    /**
     * Gets the description of the statistic.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this statistic represents a distance measurement.
     * 
     * @return true if this is a distance statistic
     */
    public boolean isDistance() {
        return unit.equals("blocks") && (this == DISTANCE_SAILED || this == LONGEST_VOYAGE_DISTANCE || this == DEEPEST_DIVE);
    }
    
    /**
     * Checks if this statistic represents a time measurement.
     * 
     * @return true if this is a time statistic
     */
    public boolean isTime() {
        return unit.equals("minutes");
    }
    
    /**
     * Checks if this statistic represents a count.
     * 
     * @return true if this is a count statistic
     */
    public boolean isCount() {
        return unit.equals("count");
    }
    
    /**
     * Checks if this statistic represents a currency amount.
     * 
     * @return true if this is a currency statistic
     */
    public boolean isCurrency() {
        return unit.equals("gold");
    }
    
    /**
     * Checks if this statistic is combat-related.
     * 
     * @return true if this is a combat statistic
     */
    public boolean isCombat() {
        return this == BATTLES_WON || this == BATTLES_LOST || this == ENEMY_SHIPS_SUNK || 
               this == DAMAGE_DEALT || this == DAMAGE_TAKEN || this == CANNONS_FIRED ||
               this == COMBAT_DEATHS || this == KRAKENS_DEFEATED;
    }
    
    /**
     * Checks if this statistic is exploration-related.
     * 
     * @return true if this is an exploration statistic
     */
    public boolean isExploration() {
        return this == ISLANDS_DISCOVERED || this == SHIPWRECKS_EXPLORED || 
               this == CAVES_EXPLORED || this == SECRETS_DISCOVERED || 
               this == DEEPEST_DIVE || this == UNDERWATER_TIME;
    }
    
    /**
     * Checks if this statistic is economy-related.
     * 
     * @return true if this is an economy statistic
     */
    public boolean isEconomy() {
        return this == TREASURES_FOUND || this == LARGEST_TREASURE_FOUND || 
               this == GOLD_EARNED || this == GOLD_SPENT || this == TRADE_DEALS_COMPLETED;
    }
    
    /**
     * Gets the category of this statistic.
     * 
     * @return the statistic category
     */
    public StatCategory getCategory() {
        if (isCombat()) return StatCategory.COMBAT;
        if (isExploration()) return StatCategory.EXPLORATION;
        if (isEconomy()) return StatCategory.ECONOMY;
        
        switch (this) {
            case DISTANCE_SAILED:
            case LONGEST_VOYAGE_DISTANCE:
            case ISLANDS_DISCOVERED:
            case PORTS_VISITED:
            case TIME_AT_SEA:
                return StatCategory.NAVIGATION;
                
            case SHIPS_BUILT:
            case SHIP_UPGRADES:
            case FASTEST_SHIP_SPEED:
            case SHIPS_LOST:
            case REPAIRS_PERFORMED:
                return StatCategory.SHIPBUILDING;
                
            case FISH_CAUGHT:
            case RARE_FISH_CAUGHT:
            case FISHING_TIME:
            case MARINE_CREATURES_ENCOUNTERED:
                return StatCategory.MARINE_LIFE;
                
            case STORMS_SURVIVED:
            case WHIRLPOOLS_ESCAPED:
            case TIDAL_WAVES_SURVIVED:
            case LIGHTNING_STRIKES_SURVIVED:
                return StatCategory.SURVIVAL;
                
            case CREW_MEMBERS_RECRUITED:
            case ALLIANCES_FORMED:
            case MESSAGES_SENT:
            case HELP_REQUESTS_ANSWERED:
                return StatCategory.SOCIAL;
                
            case ITEMS_CRAFTED:
            case RESOURCES_GATHERED:
            case TOOLS_BROKEN:
                return StatCategory.CRAFTING;
                
            default:
                return StatCategory.GENERAL;
        }
    }
    
    /**
     * Formats a statistic value for display.
     * 
     * @param value the statistic value
     * @return the formatted string
     */
    public String formatValue(long value) {
        if (isTime()) {
            long hours = value / 60;
            long minutes = value % 60;
            if (hours > 0) {
                return String.format("%d:%02d", hours, minutes);
            } else {
                return String.format("%d min", minutes);
            }
        } else if (isDistance()) {
            if (value >= 1000) {
                return String.format("%.1f km", value / 1000.0);
            } else {
                return String.format("%d m", value);
            }
        } else if (isCurrency()) {
            return String.format("%,d gold", value);
        } else {
            return String.format("%,d", value);
        }
    }
}