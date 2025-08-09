package com.odyssey.world.gamerules;

/**
 * Enumeration of all available game rules in The Odyssey.
 * Each game rule has a type, default value, and description.
 */
public enum GameRule {
    // Core gameplay rules
    DO_DAYLIGHT_CYCLE("doDaylightCycle", GameRuleType.BOOLEAN, true, "Whether the daylight cycle and moon phases progress"),
    DO_WEATHER_CYCLE("doWeatherCycle", GameRuleType.BOOLEAN, true, "Whether weather naturally changes"),
    DO_TIDAL_CYCLE("doTidalCycle", GameRuleType.BOOLEAN, true, "Whether tidal cycles affect ocean levels"),
    
    // Entity behavior rules
    DO_MOB_SPAWNING("doMobSpawning", GameRuleType.BOOLEAN, true, "Whether mobs spawn naturally"),
    DO_MOB_GRIEFING("doMobGriefing", GameRuleType.BOOLEAN, true, "Whether mobs can modify the world"),
    MOB_EXPLOSION_DROP_DECAY("mobExplosionDropDecay", GameRuleType.BOOLEAN, true, "Whether mob explosions drop fewer items"),
    
    // Player rules
    KEEP_INVENTORY("keepInventory", GameRuleType.BOOLEAN, false, "Whether players keep items after death"),
    DO_IMMEDIATE_RESPAWN("doImmediateRespawn", GameRuleType.BOOLEAN, false, "Whether players respawn immediately without death screen"),
    SHOW_DEATH_MESSAGES("showDeathMessages", GameRuleType.BOOLEAN, true, "Whether death messages are shown in chat"),
    
    // World interaction rules
    DO_FIRE_TICK("doFireTick", GameRuleType.BOOLEAN, true, "Whether fire spreads and naturally extinguishes"),
    DO_INSOMNIA("doInsomnia", GameRuleType.BOOLEAN, true, "Whether phantoms spawn when players haven't slept"),
    DO_PATROL_SPAWNING("doPatrolSpawning", GameRuleType.BOOLEAN, true, "Whether patrols spawn"),
    DO_TRADER_SPAWNING("doTraderSpawning", GameRuleType.BOOLEAN, true, "Whether wandering traders spawn"),
    
    // Chat and command rules
    ANNOUNCE_ADVANCEMENTS("announceAdvancements", GameRuleType.BOOLEAN, true, "Whether advancements are announced in chat"),
    COMMAND_BLOCK_OUTPUT("commandBlockOutput", GameRuleType.BOOLEAN, true, "Whether command blocks notify admins when they perform commands"),
    DISABLE_ELYTRA_MOVEMENT_CHECK("disableElytraMovementCheck", GameRuleType.BOOLEAN, false, "Whether the server should skip checking player speed when the player is wearing elytra"),
    DISABLE_RAIDS("disableRaids", GameRuleType.BOOLEAN, false, "Whether raids are disabled"),
    
    // Redstone and technical rules
    DO_LIMITED_CRAFTING("doLimitedCrafting", GameRuleType.BOOLEAN, false, "Whether players should only be able to craft recipes that they've unlocked first"),
    MAX_COMMAND_CHAIN_LENGTH("maxCommandChainLength", GameRuleType.INTEGER, 65536, "The maximum length of a chain of commands that can be executed during one tick"),
    MAX_ENTITY_CRAMMING("maxEntityCramming", GameRuleType.INTEGER, 24, "The maximum number of pushable entities a mob or player can push"),
    
    // Performance and technical rules
    RANDOM_TICK_SPEED("randomTickSpeed", GameRuleType.INTEGER, 3, "How often a random block tick occurs per chunk section per game tick"),
    REDUCED_DEBUG_INFO("reducedDebugInfo", GameRuleType.BOOLEAN, false, "Whether the debug screen shows all or reduced information"),
    SEND_COMMAND_FEEDBACK("sendCommandFeedback", GameRuleType.BOOLEAN, true, "Whether the feedback from commands executed by a player should show up in chat"),
    SPECTATORS_GENERATE_CHUNKS("spectatorsGenerateChunks", GameRuleType.BOOLEAN, true, "Whether players in spectator mode can generate chunks"),
    
    // Maritime-specific rules
    DO_OCEAN_CURRENTS("doOceanCurrents", GameRuleType.BOOLEAN, true, "Whether ocean currents affect ship movement"),
    DO_WIND_EFFECTS("doWindEffects", GameRuleType.BOOLEAN, true, "Whether wind affects sails and ship movement"),
    SHIP_COLLISION_DAMAGE("shipCollisionDamage", GameRuleType.BOOLEAN, true, "Whether ships take damage from collisions"),
    KRAKEN_SPAWNING("krakenSpawning", GameRuleType.BOOLEAN, true, "Whether the Kraken can spawn in deep ocean"),
    TREASURE_RESPAWN_TIME("treasureRespawnTime", GameRuleType.INTEGER, 72000, "Time in ticks before treasure chests can respawn (default: 1 hour)"),
    
    // Pirate-specific rules
    PIRATE_SHIP_SPAWNING("pirateShipSpawning", GameRuleType.BOOLEAN, true, "Whether pirate ships spawn naturally"),
    BOUNTY_SYSTEM("bountySystem", GameRuleType.BOOLEAN, true, "Whether the bounty system is active"),
    CREW_LOYALTY_DECAY("crewLoyaltyDecay", GameRuleType.BOOLEAN, true, "Whether crew loyalty decreases over time"),
    
    // Economy rules
    TRADE_ROUTE_PROFITS("tradeRouteProfits", GameRuleType.FLOAT, 1.0f, "Multiplier for trade route profit calculations"),
    PORT_TAX_RATE("portTaxRate", GameRuleType.FLOAT, 0.1f, "Tax rate applied at ports (0.0 to 1.0)"),
    
    // Weather and environment
    STORM_INTENSITY("stormIntensity", GameRuleType.FLOAT, 1.0f, "Multiplier for storm intensity and frequency"),
    HURRICANE_SPAWNING("hurricaneSpawning", GameRuleType.BOOLEAN, true, "Whether hurricanes can spawn"),
    
    // Multiplayer rules
    PLAYER_SLEEPING_PERCENTAGE("playerSleepingPercentage", GameRuleType.INTEGER, 100, "Percentage of players that must sleep to skip night"),
    SPAWN_RADIUS("spawnRadius", GameRuleType.INTEGER, 10, "Radius around spawn point where players can spawn"),
    
    // Debug and development
    LOG_ADMIN_COMMANDS("logAdminCommands", GameRuleType.BOOLEAN, true, "Whether admin commands are logged"),
    UNIVERSAL_ANGER("universalAnger", GameRuleType.BOOLEAN, false, "Whether angered neutral mobs attack any nearby player"),
    FORGIVE_DEAD_PLAYERS("forgiveDeadPlayers", GameRuleType.BOOLEAN, true, "Whether angered neutral mobs stop being angry when the targeted player dies nearby");

    private final String name;
    private final GameRuleType type;
    private final Object defaultValue;
    private final String description;

    GameRule(String name, GameRuleType type, Object defaultValue, String description) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public GameRuleType getType() {
        return type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets a game rule by its name.
     * @param name The name of the game rule
     * @return The game rule, or null if not found
     */
    public static GameRule getByName(String name) {
        for (GameRule rule : values()) {
            if (rule.getName().equals(name)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Validates if a value is valid for this game rule.
     * @param value The value to validate
     * @return true if the value is valid for this rule's type
     */
    public boolean isValidValue(Object value) {
        return type.isValidValue(value);
    }

    /**
     * Converts a string value to the appropriate type for this game rule.
     * @param stringValue The string representation of the value
     * @return The converted value, or null if conversion failed
     */
    public Object parseValue(String stringValue) {
        return type.parseValue(stringValue);
    }
}