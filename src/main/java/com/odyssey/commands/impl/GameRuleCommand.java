package com.odyssey.commands.impl;

import com.odyssey.commands.Command;
import com.odyssey.commands.CommandResult;
import com.odyssey.commands.CommandSender;
import com.odyssey.core.Engine;
import com.odyssey.world.gamerules.GameRule;
import com.odyssey.world.gamerules.GameRuleManager;
import com.odyssey.world.gamerules.GameRuleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for managing game rules.
 * Allows viewing, setting, and resetting game rule values.
 */
public class GameRuleCommand implements Command {
    private final GameRuleManager gameRuleManager;

    public GameRuleCommand() {
        // Get the game rule manager from the world
        this.gameRuleManager = Engine.getInstance().getWorld().getGameRuleManager();
    }

    @Override
    public String getName() {
        return "gamerule";
    }

    @Override
    public String getDescription() {
        return "Manage game rules";
    }

    @Override
    public String getUsage() {
        return "/gamerule [rule] [value] | /gamerule list [type] | /gamerule reset [rule|all]";
    }

    @Override
    public int getPermissionLevel() {
        return 50; // Moderate permission level for game rule management
    }

    @Override
    public String[] getAliases() {
        return new String[]{"gr", "rules"};
    }

    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return showAllRules(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleList(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "help":
                return showHelp(sender);
            default:
                // Try to interpret as a rule name
                return handleRuleOperation(sender, args);
        }
    }

    /**
     * Shows all game rules and their current values.
     */
    private CommandResult showAllRules(CommandSender sender) {
        StringBuilder message = new StringBuilder("Current game rules:\n");
        
        for (GameRule rule : GameRule.values()) {
            Object value = gameRuleManager.getValue(rule);
            message.append(String.format("  %s: %s (%s) - %s\n", 
                rule.getName(), 
                gameRuleManager.getValueAsString(rule),
                rule.getType().name().toLowerCase(),
                rule.getDescription()));
        }
        
        return CommandResult.success(message.toString());
    }

    /**
     * Handles the list subcommand.
     */
    private CommandResult handleList(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // List all rules grouped by type
            StringBuilder message = new StringBuilder("Game rules by type:\n");
            
            for (GameRuleType type : GameRuleType.values()) {
                List<GameRule> rulesOfType = gameRuleManager.getRulesByType(type);
                if (!rulesOfType.isEmpty()) {
                    message.append(String.format("\n%s rules:\n", type.name()));
                    for (GameRule rule : rulesOfType) {
                        message.append(String.format("  %s: %s - %s\n",
                            rule.getName(),
                            gameRuleManager.getValueAsString(rule),
                            rule.getDescription()));
                    }
                }
            }
            
            return CommandResult.success(message.toString());
        } else {
            // List rules of specific type
            String typeName = args[1].toUpperCase();
            try {
                GameRuleType type = GameRuleType.valueOf(typeName);
                List<GameRule> rulesOfType = gameRuleManager.getRulesByType(type);
                
                if (rulesOfType.isEmpty()) {
                    return CommandResult.success("No game rules of type " + type.name().toLowerCase());
                }
                
                StringBuilder message = new StringBuilder(String.format("%s game rules:\n", type.name()));
                for (GameRule rule : rulesOfType) {
                    message.append(String.format("  %s: %s - %s\n",
                        rule.getName(),
                        gameRuleManager.getValueAsString(rule),
                        rule.getDescription()));
                }
                
                return CommandResult.success(message.toString());
            } catch (IllegalArgumentException e) {
                return CommandResult.error("Invalid type: " + args[1] + ". Valid types: " + 
                    Arrays.stream(GameRuleType.values())
                        .map(t -> t.name().toLowerCase())
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    /**
     * Handles the reset subcommand.
     */
    private CommandResult handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return CommandResult.invalidArgs("Usage: /gamerule reset <rule|all>");
        }

        String target = args[1];
        
        if ("all".equalsIgnoreCase(target)) {
            gameRuleManager.resetAllToDefaults();
            return CommandResult.success("All game rules reset to default values");
        } else {
            GameRule rule = GameRule.getByName(target);
            if (rule == null) {
                return CommandResult.error("Unknown game rule: " + target);
            }
            
            Object oldValue = gameRuleManager.getValue(rule);
            gameRuleManager.resetToDefault(rule);
            Object newValue = gameRuleManager.getValue(rule);
            
            return CommandResult.success(String.format("Game rule %s reset from %s to %s", 
                rule.getName(), oldValue, newValue));
        }
    }

    /**
     * Handles rule get/set operations.
     */
    private CommandResult handleRuleOperation(CommandSender sender, String[] args) {
        String ruleName = args[0];
        GameRule rule = GameRule.getByName(ruleName);
        
        if (rule == null) {
            return CommandResult.error("Unknown game rule: " + ruleName + 
                ". Use '/gamerule list' to see all available rules.");
        }

        if (args.length == 1) {
            // Get current value
            Object value = gameRuleManager.getValue(rule);
            return CommandResult.success(String.format("%s is currently set to: %s (%s)\n%s", 
                rule.getName(), 
                gameRuleManager.getValueAsString(rule),
                rule.getType().name().toLowerCase(),
                rule.getDescription()));
        } else {
            // Set new value
            String newValueStr = args[1];
            Object oldValue = gameRuleManager.getValue(rule);
            
            if (gameRuleManager.setValueFromString(rule, newValueStr)) {
                Object newValue = gameRuleManager.getValue(rule);
                return CommandResult.success(String.format("Game rule %s updated from %s to %s", 
                    rule.getName(), oldValue, newValue));
            } else {
                return CommandResult.error(String.format("Invalid value '%s' for game rule %s (type: %s)", 
                    newValueStr, rule.getName(), rule.getType().name().toLowerCase()));
            }
        }
    }

    /**
     * Shows help information for the gamerule command.
     */
    private CommandResult showHelp(CommandSender sender) {
        StringBuilder help = new StringBuilder();
        help.append("Game Rule Command Help:\n");
        help.append("  /gamerule - Show all game rules\n");
        help.append("  /gamerule <rule> - Show current value of a rule\n");
        help.append("  /gamerule <rule> <value> - Set a rule to a new value\n");
        help.append("  /gamerule list - Show all rules grouped by type\n");
        help.append("  /gamerule list <type> - Show rules of specific type\n");
        help.append("  /gamerule reset <rule> - Reset a rule to default\n");
        help.append("  /gamerule reset all - Reset all rules to defaults\n");
        help.append("\nRule types: ");
        help.append(Arrays.stream(GameRuleType.values())
            .map(t -> t.name().toLowerCase())
            .collect(Collectors.joining(", ")));
        
        return CommandResult.success(help.toString());
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete subcommands and rule names
            String partial = args[0].toLowerCase();
            
            // Add subcommands
            for (String subCmd : Arrays.asList("list", "reset", "help")) {
                if (subCmd.startsWith(partial)) {
                    completions.add(subCmd);
                }
            }
            
            // Add rule names
            for (GameRule rule : GameRule.values()) {
                if (rule.getName().toLowerCase().startsWith(partial)) {
                    completions.add(rule.getName());
                }
            }
        } else if (args.length == 2) {
            String firstArg = args[0].toLowerCase();
            String partial = args[1].toLowerCase();
            
            if ("list".equals(firstArg)) {
                // Complete rule types
                for (GameRuleType type : GameRuleType.values()) {
                    if (type.name().toLowerCase().startsWith(partial)) {
                        completions.add(type.name().toLowerCase());
                    }
                }
            } else if ("reset".equals(firstArg)) {
                // Complete rule names and "all"
                if ("all".startsWith(partial)) {
                    completions.add("all");
                }
                for (GameRule rule : GameRule.values()) {
                    if (rule.getName().toLowerCase().startsWith(partial)) {
                        completions.add(rule.getName());
                    }
                }
            } else {
                // Complete values for the rule
                GameRule rule = GameRule.getByName(firstArg);
                if (rule != null) {
                    switch (rule.getType()) {
                        case BOOLEAN:
                            if ("true".startsWith(partial)) completions.add("true");
                            if ("false".startsWith(partial)) completions.add("false");
                            break;
                        case INTEGER:
                            // Suggest some common values
                            for (String value : Arrays.asList("0", "1", "3", "10", "20", "100")) {
                                if (value.startsWith(partial)) {
                                    completions.add(value);
                                }
                            }
                            break;
                        case FLOAT:
                            // Suggest some common float values
                            for (String value : Arrays.asList("0.0", "0.5", "1.0", "2.0")) {
                                if (value.startsWith(partial)) {
                                    completions.add(value);
                                }
                            }
                            break;
                    }
                }
            }
        }

        return completions;
    }
}