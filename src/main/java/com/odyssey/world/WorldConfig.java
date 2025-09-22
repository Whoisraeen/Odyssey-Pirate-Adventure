package com.odyssey.world;

/**
 * Configuration class for world generation parameters
 */
public class WorldConfig {
    
    // World size parameters
    private int chunkSize = 16;
    private float worldSize = 10000.0f;
    private int maxHeight = 128;
    private int minHeight = -64;
    private int seaLevel = 64;
    
    // Island generation
    private IslandConfig islandConfig = new IslandConfig();
    
    // Ocean parameters
    private float maxCurrentSpeed = 5.0f;
    private float waveHeight = 2.0f;
    private float waveFrequency = 0.1f;
    
    // Weather parameters
    private WeatherConfig weatherConfig = new WeatherConfig();
    
    // Trade routes
    private int tradeRouteCount = 20;
    
    // Navigation hazards
    private float hazardDensity = 0.1f;
    
    public WorldConfig() {
        // Default configuration
    }
    
    // Getters and setters
    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
    
    public float getWorldSize() { return worldSize; }
    public void setWorldSize(float worldSize) { this.worldSize = worldSize; }
    
    public int getMaxHeight() { return maxHeight; }
    public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }
    
    public int getMinHeight() { return minHeight; }
    public void setMinHeight(int minHeight) { this.minHeight = minHeight; }
    
    public int getSeaLevel() { return seaLevel; }
    public void setSeaLevel(int seaLevel) { this.seaLevel = seaLevel; }
    
    public IslandConfig getIslandConfig() { return islandConfig; }
    public void setIslandConfig(IslandConfig islandConfig) { this.islandConfig = islandConfig; }
    
    public float getMaxCurrentSpeed() { return maxCurrentSpeed; }
    public void setMaxCurrentSpeed(float maxCurrentSpeed) { this.maxCurrentSpeed = maxCurrentSpeed; }
    
    public float getWaveHeight() { return waveHeight; }
    public void setWaveHeight(float waveHeight) { this.waveHeight = waveHeight; }
    
    public float getWaveFrequency() { return waveFrequency; }
    public void setWaveFrequency(float waveFrequency) { this.waveFrequency = waveFrequency; }
    
    public WeatherConfig getWeatherConfig() { return weatherConfig; }
    public void setWeatherConfig(WeatherConfig weatherConfig) { this.weatherConfig = weatherConfig; }
    
    public int getTradeRouteCount() { return tradeRouteCount; }
    public void setTradeRouteCount(int tradeRouteCount) { this.tradeRouteCount = tradeRouteCount; }
    
    public float getHazardDensity() { return hazardDensity; }
    public void setHazardDensity(float hazardDensity) { this.hazardDensity = hazardDensity; }
    
    /**
     * Configuration for island generation
     */
    public static class IslandConfig {
        private float frequency = 0.001f;
        private float threshold = 0.3f;
        private float maxRadius = 200.0f;
        private float minRadius = 50.0f;
        private int maxIslandsPerChunk = 3;
        
        public float getFrequency() { return frequency; }
        public void setFrequency(float frequency) { this.frequency = frequency; }
        
        public float getThreshold() { return threshold; }
        public void setThreshold(float threshold) { this.threshold = threshold; }
        
        public float getMaxRadius() { return maxRadius; }
        public void setMaxRadius(float maxRadius) { this.maxRadius = maxRadius; }
        
        public float getMinRadius() { return minRadius; }
        public void setMinRadius(float minRadius) { this.minRadius = minRadius; }
        
        public int getMaxIslandsPerChunk() { return maxIslandsPerChunk; }
        public void setMaxIslandsPerChunk(int maxIslandsPerChunk) { this.maxIslandsPerChunk = maxIslandsPerChunk; }
    }
    
    /**
     * Configuration for weather system
     */
    public static class WeatherConfig {
        private float windStrength = 10.0f;
        private float stormFrequency = 0.1f;
        private float temperatureVariation = 20.0f;
        private float humidityVariation = 0.5f;
        
        public float getWindStrength() { return windStrength; }
        public void setWindStrength(float windStrength) { this.windStrength = windStrength; }
        
        public float getStormFrequency() { return stormFrequency; }
        public void setStormFrequency(float stormFrequency) { this.stormFrequency = stormFrequency; }
        
        public float getTemperatureVariation() { return temperatureVariation; }
        public void setTemperatureVariation(float temperatureVariation) { this.temperatureVariation = temperatureVariation; }
        
        public float getHumidityVariation() { return humidityVariation; }
        public void setHumidityVariation(float humidityVariation) { this.humidityVariation = humidityVariation; }
    }
}