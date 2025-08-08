package com.odyssey.commands.impl;

import com.odyssey.commands.*;
import org.joml.Vector3f;

import java.util.List;

/**
 * Teleport command for moving players to specific coordinates.
 * Usage: /tp <x> <y> <z> or /tp <player>
 */
public class TeleportCommand implements Command {
    
    @Override
    public String getName() {
        return "tp";
    }
    
    @Override
    public String getDescription() {
        return "Teleports a player to specified coordinates or another player";
    }
    
    @Override
    public String getUsage() {
        return "/tp <x> <y> <z> or /tp <player>";
    }
    
    @Override
    public int getPermissionLevel() {
        return 2; // Admin level
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("teleport");
    }
    
    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (!sender.isPlayer()) {
            return CommandResult.playerOnly();
        }
        
        if (args.length == 3) {
            // Teleport to coordinates
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                float z = Float.parseFloat(args[2]);
                
                // TODO: Implement actual teleportation logic
                // For now, just return success message
                return CommandResult.success(String.format("Teleported to %.2f, %.2f, %.2f", x, y, z));
                
            } catch (NumberFormatException e) {
                return CommandResult.invalidArgs("Invalid coordinates. Use numbers for x, y, z");
            }
        } else if (args.length == 1) {
            // Teleport to player
            String targetPlayer = args[0];
            
            // TODO: Implement player lookup and teleportation
            return CommandResult.success("Teleported to player " + targetPlayer);
            
        } else {
            return CommandResult.invalidArgs(getUsage());
        }
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // TODO: Return list of online player names
            return List.of("player1", "player2"); // Placeholder
        }
        return List.of();
    }
    
    @Override
    public boolean validateArgs(CommandSender sender, String[] args) {
        return args.length == 1 || args.length == 3;
    }
}