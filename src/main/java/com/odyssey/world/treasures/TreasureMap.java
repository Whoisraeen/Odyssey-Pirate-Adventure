package com.odyssey.world.treasures;

import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a treasure map with procedurally generated clues and treasure location.
 * Maps contain riddles, landmarks, and visual representations of the area.
 */
public class TreasureMap {
    private final String mapId;
    private final Vector3f treasureLocation;
    private final List<String> riddles;
    private final List<Landmark> landmarks;
    private final MapData mapData;
    private final TreasureType treasureType;
    private final int difficultyLevel;
    private boolean discovered = false;
    
    public TreasureMap(String mapId, Vector3f treasureLocation, TreasureType treasureType, int difficultyLevel) {
        this.mapId = mapId;
        this.treasureLocation = new Vector3f(treasureLocation);
        this.treasureType = treasureType;
        this.difficultyLevel = difficultyLevel;
        this.riddles = new ArrayList<>();
        this.landmarks = new ArrayList<>();
        this.mapData = new MapData();
        
        generateMapContent();
    }
    
    /**
     * Generates the map content including riddles and landmarks.
     */
    private void generateMapContent() {
        Random random = new Random(mapId.hashCode());
        
        // Generate landmarks around the treasure location
        generateLandmarks(random);
        
        // Generate riddles based on landmarks and treasure location
        generateRiddles(random);
        
        // Initialize map data with basic area around treasure
        initializeMapData();
    }
    
    /**
     * Generates landmarks that can be referenced in riddles.
     */
    private void generateLandmarks(Random random) {
        int landmarkCount = 3 + difficultyLevel; // More landmarks for harder maps
        
        for (int i = 0; i < landmarkCount; i++) {
            // Generate landmarks within a reasonable distance from treasure
            float angle = random.nextFloat() * 2 * (float)Math.PI;
            float distance = 50 + random.nextFloat() * 200; // 50-250 blocks away
            
            Vector3f landmarkPos = new Vector3f(
                treasureLocation.x + (float)Math.cos(angle) * distance,
                treasureLocation.y,
                treasureLocation.z + (float)Math.sin(angle) * distance
            );
            
            LandmarkType type = LandmarkType.values()[random.nextInt(LandmarkType.values().length)];
            landmarks.add(new Landmark(type, landmarkPos, generateLandmarkDescription(type, random)));
        }
    }
    
    /**
     * Generates riddles that reference landmarks and provide clues to treasure location.
     */
    private void generateRiddles(Random random) {
        String[] riddleTemplates = {
            "Where the %s meets the %s, count %d paces %s to find thy prize.",
            "From the ancient %s, sail %s until ye spy the %s, then dig where X marks the spot.",
            "When the sun sets behind the %s, the shadow points to riches untold.",
            "Three %s form a triangle, and at its heart lies the treasure ye seek.",
            "Follow the %s current from the %s, and where it bends, fortune awaits."
        };
        
        int riddleCount = Math.min(3, difficultyLevel + 1);
        
        for (int i = 0; i < riddleCount; i++) {
            String template = riddleTemplates[random.nextInt(riddleTemplates.length)];
            String riddle = generateRiddleFromTemplate(template, random);
            riddles.add(riddle);
        }
        
        // Add a final riddle that's more direct for easier maps
        if (difficultyLevel <= 2) {
            riddles.add("The treasure lies buried beneath the sand, marked by an ancient stone.");
        }
    }
    
    /**
     * Generates a specific riddle from a template using available landmarks.
     */
    private String generateRiddleFromTemplate(String template, Random random) {
        if (landmarks.isEmpty()) {
            return "The treasure awaits those brave enough to seek it.";
        }
        
        Landmark landmark1 = landmarks.get(random.nextInt(landmarks.size()));
        Landmark landmark2 = landmarks.size() > 1 ? 
            landmarks.get(random.nextInt(landmarks.size())) : landmark1;
        
        String[] directions = {"north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest"};
        String direction = directions[random.nextInt(directions.length)];
        int paces = 10 + random.nextInt(50);
        
        return String.format(template, 
            landmark1.getType().getDisplayName(),
            landmark2.getType().getDisplayName(),
            paces,
            direction);
    }
    
    /**
     * Generates a description for a landmark based on its type.
     */
    private String generateLandmarkDescription(LandmarkType type, Random random) {
        String[] adjectives = {"ancient", "weathered", "mysterious", "towering", "crumbling", "majestic"};
        String adjective = adjectives[random.nextInt(adjectives.length)];
        
        return adjective + " " + type.getDisplayName();
    }
    
    /**
     * Initializes the map data with the area around the treasure.
     */
    private void initializeMapData() {
        int mapSize = 64; // 64x64 block area
        int centerX = (int)treasureLocation.x;
        int centerZ = (int)treasureLocation.z;
        
        mapData.initialize(centerX - mapSize/2, centerZ - mapSize/2, mapSize, mapSize);
        
        // Mark treasure location
        mapData.markTreasureLocation((int)treasureLocation.x, (int)treasureLocation.z);
        
        // Mark landmarks
        for (Landmark landmark : landmarks) {
            mapData.markLandmark((int)landmark.getPosition().x, (int)landmark.getPosition().z, landmark.getType());
        }
    }
    
    /**
     * Updates the map data as the player explores with the map equipped.
     */
    public void updateExploredArea(Vector3f playerPosition, int radius) {
        mapData.revealArea((int)playerPosition.x, (int)playerPosition.z, radius);
    }
    
    /**
     * Checks if the player is near the treasure location.
     */
    public boolean isNearTreasure(Vector3f playerPosition, float threshold) {
        return treasureLocation.distance(playerPosition) <= threshold;
    }
    
    /**
     * Marks the treasure as discovered.
     */
    public void discoverTreasure() {
        this.discovered = true;
    }
    
    // Getters
    public String getMapId() { return mapId; }
    public Vector3f getTreasureLocation() { return new Vector3f(treasureLocation); }
    public List<String> getRiddles() { return new ArrayList<>(riddles); }
    public List<Landmark> getLandmarks() { return new ArrayList<>(landmarks); }
    public MapData getMapData() { return mapData; }
    public TreasureType getTreasureType() { return treasureType; }
    public int getDifficultyLevel() { return difficultyLevel; }
    public boolean isDiscovered() { return discovered; }
    
    /**
     * Represents a landmark on the treasure map.
     */
    public static class Landmark {
        private final LandmarkType type;
        private final Vector3f position;
        private final String description;
        
        public Landmark(LandmarkType type, Vector3f position, String description) {
            this.type = type;
            this.position = new Vector3f(position);
            this.description = description;
        }
        
        public LandmarkType getType() { return type; }
        public Vector3f getPosition() { return new Vector3f(position); }
        public String getDescription() { return description; }
    }
    
    /**
     * Types of landmarks that can appear on treasure maps.
     */
    public enum LandmarkType {
        PALM_TREE("palm tree"),
        ROCK_FORMATION("rock formation"),
        SHIPWRECK("shipwreck"),
        LIGHTHOUSE("lighthouse"),
        CAVE_ENTRANCE("cave entrance"),
        CORAL_REEF("coral reef"),
        BEACH("beach"),
        CLIFF("cliff"),
        WATERFALL("waterfall"),
        RUINS("ancient ruins");
        
        private final String displayName;
        
        LandmarkType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Types of treasures that can be found.
     */
    public enum TreasureType {
        GOLD_COINS(1),
        PRECIOUS_GEMS(2),
        ANCIENT_ARTIFACTS(3),
        LEGENDARY_WEAPON(4),
        SHIP_BLUEPRINTS(2),
        RARE_MATERIALS(1);
        
        private final int rarity;
        
        TreasureType(int rarity) {
            this.rarity = rarity;
        }
        
        public int getRarity() { return rarity; }
    }
}