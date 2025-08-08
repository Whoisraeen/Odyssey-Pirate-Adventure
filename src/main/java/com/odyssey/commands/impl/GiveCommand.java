package com.odyssey.commands.impl;

import com.odyssey.commands.*;

import java.util.List;

/**
 * Give command for providing items to players.
 * Usage: /give <player> <item> [amount]
 */
public class GiveCommand implements Command {
    
    @Override
    public String getName() {
        return "give";
    }
    
    @Override
    public String getDescription() {
        return "Gives items to a player";
    }
    
    @Override
    public String getUsage() {
        return "/give <player> <item> [amount]";
    }
    
    @Override
    public int getPermissionLevel() {
        return 2; // Admin level
    }
    
    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (args.length < 2 || args.length > 3) {
            return CommandResult.invalidArgs(getUsage());
        }
        
        String targetPlayer = args[0];
        String itemName = args[1];
        int amount = 1;
        
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    return CommandResult.invalidArgs("Amount must be positive");
                }
            } catch (NumberFormatException e) {
                return CommandResult.invalidArgs("Invalid amount. Must be a number");
            }
        }
        
        // TODO: Implement actual item giving logic
        // For now, just return success message
        return CommandResult.success(String.format("Gave %d %s to %s", amount, itemName, targetPlayer));
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // TODO: Return list of online player names
            return List.of("player1", "player2"); // Placeholder
        } else if (args.length == 2) {
            // TODO: Return list of available items
            return List.of("wood", "stone", "iron", "rope", "sail", "cannon"); // Placeholder
        }
        return List.of();
    }
    
    @Override
    public boolean validateArgs(CommandSender sender, String[] args) {
        return args.length >= 2 && args.length <= 3;
    }
}