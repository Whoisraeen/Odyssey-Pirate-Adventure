package com.odyssey.world.biomes;

/**
 * Represents a variant of a biome type with modified properties.
 * Allows for subtle variations within the same biome type.
 */
public class BiomeVariant {
    private float temperatureModifier = 0.0f;
    private float humidityModifier = 0.0f;
    private float elevationModifier = 0.0f;
    private float featureIntensity = 1.0f;
    private float rarityBonus = 1.0f;
    private boolean isSpecialVariant = false;
    private String specialType = "";
    
    public BiomeVariant() {}
    
    public BiomeVariant(float temperatureModifier, float humidityModifier, float elevationModifier, 
                       float featureIntensity, float rarityBonus) {
        this.temperatureModifier = temperatureModifier;
        this.humidityModifier = humidityModifier;
        this.elevationModifier = elevationModifier;
        this.featureIntensity = featureIntensity;
        this.rarityBonus = rarityBonus;
    }
    
    // Getters and setters
    public float getTemperatureModifier() { return temperatureModifier; }
    public void setTemperatureModifier(float temperatureModifier) { this.temperatureModifier = temperatureModifier; }
    
    public float getHumidityModifier() { return humidityModifier; }
    public void setHumidityModifier(float humidityModifier) { this.humidityModifier = humidityModifier; }
    
    public float getElevationModifier() { return elevationModifier; }
    public void setElevationModifier(float elevationModifier) { this.elevationModifier = elevationModifier; }
    
    public float getFeatureIntensity() { return featureIntensity; }
    public void setFeatureIntensity(float featureIntensity) { this.featureIntensity = featureIntensity; }
    
    public float getRarityBonus() { return rarityBonus; }
    public void setRarityBonus(float rarityBonus) { this.rarityBonus = rarityBonus; }
    
    public boolean isSpecialVariant() { return isSpecialVariant; }
    public void setSpecialVariant(boolean specialVariant) { isSpecialVariant = specialVariant; }
    
    public String getSpecialType() { return specialType; }
    public void setSpecialType(String specialType) { this.specialType = specialType; }
    
    @Override
    public String toString() {
        return String.format("BiomeVariant{temp=%.2f, humidity=%.2f, elevation=%.2f, intensity=%.2f, rarity=%.2f, special=%s}", 
            temperatureModifier, humidityModifier, elevationModifier, featureIntensity, rarityBonus, 
            isSpecialVariant ? specialType : "none");
    }
}