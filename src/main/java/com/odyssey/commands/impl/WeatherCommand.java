package com.odyssey.commands.impl;

import com.odyssey.commands.*;
import com.odyssey.core.Engine;
import com.odyssey.world.weather.WeatherSystem;
import com.odyssey.world.weather.WeatherSystem.WeatherType;

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
        
        // Get the weather system from the engine
        WeatherSystem weatherSystem = Engine.getInstance().getWeatherSystem();
        if (weatherSystem == null) {
            return CommandResult.failure("Weather system not available");
        }
        
        // Map command arguments to WeatherType enum and set appropriate intensity
        WeatherType targetWeather;
        float intensity;
        
        switch (weatherType) {
            case "clear" -> {
                targetWeather = WeatherType.CLEAR;
                intensity = 0.0f;
            }
            case "rain" -> {
                targetWeather = WeatherType.MODERATE_RAIN;
                intensity = 0.6f;
            }
            case "storm" -> {
                targetWeather = WeatherType.THUNDERSTORM;
                intensity = 0.9f;
            }
            case "fog" -> {
                targetWeather = WeatherType.FOG;
                intensity = 0.7f;
            }
            default -> {
                return CommandResult.invalidArgs("Unknown weather type: " + weatherType);
            }
        }
        
        // Force the weather change for 5 minutes (300 seconds)
        double duration = 300.0;
        weatherSystem.forceWeather(targetWeather, intensity, duration);
        
        return CommandResult.success("Weather changed to " + weatherType + " for " + (int)(duration / 60) + " minutes");
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