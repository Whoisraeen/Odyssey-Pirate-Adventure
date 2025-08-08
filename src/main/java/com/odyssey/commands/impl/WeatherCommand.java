package com.odyssey.commands.impl;

import com.odyssey.commands.*;

import java.util.List;

/**
 * Weather command for controlling weather conditions.
 * Usage: /weather <clear|rain|storm|fog>
 */
public class WeatherCommand implements Command {
    
    private static final List<String> WEATHER_TYPES = List.of("clear", "rain", "storm", "fog");
    
    @Override
    public String getName() {
        return "weather";
    }
    
    @Override
    public String getDescription() {
        return "Controls the weather conditions";
    }
    
    @Override
    public String getUsage() {
        return "/weather <clear|rain|storm|fog>";
    }
    
    @Override
    public int getPermissionLevel() {
        return 2; // Admin level
    }
    
    @Override
    public CommandResult execute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            return CommandResult.invalidArgs(getUsage());
        }
        
        String weatherType = args[0].toLowerCase();
        
        if (!WEATHER_TYPES.contains(weatherType)) {
            return CommandResult.invalidArgs("Invalid weather type. Use: " + String.join(", ", WEATHER_TYPES));
        }
        
        // TODO: Implement actual weather control logic
        // This would integrate with the WeatherSystem
        return CommandResult.success("Weather changed to " + weatherType);
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return WEATHER_TYPES.stream()
                    .filter(type -> type.startsWith(partial))
                    .toList();
        }
        return List.of();
    }
    
    @Override
    public boolean validateArgs(CommandSender sender, String[] args) {
        return args.length == 1;
    }
}