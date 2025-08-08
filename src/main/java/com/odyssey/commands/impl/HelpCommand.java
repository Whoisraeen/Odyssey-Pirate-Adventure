package com.odyssey.commands.impl;

import com.odyssey.commands.*;

import java.util.List;

/**
 * Help command for displaying command information.
 * Usage: /help [command]
 */
public class HelpCommand implements Command {
    
    private final CommandManager commandManager;
    
    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Shows help information for commands";
    }
    
    @Override
    public String getUsage() {
        return "/help [command]";
    }
    
    @Override
    public int getPermissionLevel() {
        return 0; // Available to all players
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("?");
    }
    
    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // Show list of available commands
            List<String> availableCommands = commandManager.getAvailableCommands(sender);
            
            if (availableCommands.isEmpty()) {
                return CommandResult.success("No commands available.");
            }
            
            StringBuilder message = new StringBuilder("Available commands:\n");
            for (String commandName : availableCommands) {
                Command command = commandManager.getCommand(commandName);
                if (command != null) {
                    message.append(String.format("  /%s - %s\n", commandName, command.getDescription()));
                }
            }
            message.append("Use /help <command> for detailed information.");
            
            return CommandResult.success(message.toString());
            
        } else if (args.length == 1) {
            // Show detailed help for specific command
            String commandName = args[0];
            Command command = commandManager.getCommand(commandName);
            
            if (command == null) {
                return CommandResult.failure("Unknown command: " + commandName);
            }
            
            if (!sender.hasPermission(command.getPermissionLevel())) {
                return CommandResult.failure("Unknown command: " + commandName);
            }
            
            StringBuilder message = new StringBuilder();
            message.append(String.format("Command: /%s\n", command.getName()));
            message.append(String.format("Description: %s\n", command.getDescription()));
            message.append(String.format("Usage: %s\n", command.getUsage()));
            message.append(String.format("Permission Level: %d\n", command.getPermissionLevel()));
            
            if (!command.getAliases().isEmpty()) {
                message.append(String.format("Aliases: %s\n", String.join(", ", command.getAliases())));
            }
            
            return CommandResult.success(message.toString());
            
        } else {
            return CommandResult.invalidArgs(getUsage());
        }
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return commandManager.getAvailableCommands(sender).stream()
                    .filter(cmd -> cmd.startsWith(partial))
                    .toList();
        }
        return List.of();
    }
    
    @Override
    public boolean validateArgs(CommandSender sender, String[] args) {
        return args.length <= 1;
    }
}