package com.odyssey;

import com.odyssey.core.Engine;
import com.odyssey.core.GameConfig;
import com.odyssey.core.CrashReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Odyssey: Revolutionary Voxel Maritime Adventure
 * 
 * Main entry point for the game. This class initializes the game engine
 * and starts the main game loop.
 * 
 * Features:
 * - Dynamic living ocean ecosystem with tidal mechanics
 * - Modular ship building and fleet management
 * - Procedurally generated islands and biomes
 * - Advanced weather and physics systems
 * - Multiplayer support with persistent world
 */
public class OdysseyGame {
    private static final Logger logger = LoggerFactory.getLogger(OdysseyGame.class);
    
    public static void main(String[] args) {
        logger.info("Starting The Odyssey: Pirate Adventure");
        logger.info("Navigate the Boundless Azure - Build Your Fleet, Command Your Destiny, Shape the Seven Seas");
        
        GameConfig config = null;
        CrashReporter crashReporter = null;
        
        try {
            // Parse command line arguments
            config = parseArguments(args);
            crashReporter = new CrashReporter(config);
            
            // Initialize and start the game engine
            Engine engine = new Engine(config);
            engine.initialize();
            engine.run();
            
        } catch (Exception e) {
            logger.error("Fatal error occurred during game execution", e);
            
            // Report crash if crash reporter is available
            if (crashReporter != null) {
                crashReporter.reportCrash("Application startup/main execution", e);
            } else {
                // Fallback crash reporting without config
                CrashReporter fallbackReporter = new CrashReporter(config);
                fallbackReporter.reportCrash("Application startup (pre-config)", e);
            }
            
            System.exit(1);
        }
        
        logger.info("The Odyssey has ended. Fair winds and following seas!");
    }
    
    /**
     * Parse command line arguments to configure the game
     */
    private static GameConfig parseArguments(String[] args) {
        GameConfig config = new GameConfig();
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--windowed":
                    config.setFullscreen(false);
                    break;
                case "--fullscreen":
                    config.setFullscreen(true);
                    break;
                case "--width":
                    if (i + 1 < args.length) {
                        config.setWindowWidth(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--height":
                    if (i + 1 < args.length) {
                        config.setWindowHeight(Integer.parseInt(args[++i]));
                    }
                    break;
                case "--debug":
                    config.setDebugMode(true);
                    break;
                case "--seed":
                    if (i + 1 < args.length) {
                        config.setWorldSeed(Long.parseLong(args[++i]));
                    }
                    break;
                case "--help":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    logger.warn("Unknown argument: {}", args[i]);
                    break;
            }
        }
        
        return config;
    }
    
    private static void printUsage() {
        System.out.println("The Odyssey: Pirate Adventure");
        System.out.println("Usage: java -jar odyssey.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --windowed          Run in windowed mode");
        System.out.println("  --fullscreen        Run in fullscreen mode");
        System.out.println("  --width <pixels>    Set window width");
        System.out.println("  --height <pixels>   Set window height");
        System.out.println("  --debug             Enable debug mode");
        System.out.println("  --seed <number>     Set world generation seed");
        System.out.println("  --help              Show this help message");
    }
}