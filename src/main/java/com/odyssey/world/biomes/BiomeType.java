package com.odyssey.world.biomes;

import java.util.*;

/**
 * Defines the properties and characteristics of a biome type.
 * Contains all static data that defines what makes a biome unique.
 */
public class BiomeType {
    private final String name;
    private final float temperature;
    private final float humidity;
    private final float elevation;
    private final int color;
    private final Precipitation precipitation;
    private final Category category;
    private final Map<String, Float> features;
    private final List<MobSpawn> mobSpawns;
    private final BiomeProperties properties;
    
    private BiomeType(Builder builder) {
        this.name = builder.name;
        this.temperature = builder.temperature;
        this.humidity = builder.humidity;
        this.elevation = builder.elevation;
        this.color = builder.color;
        this.precipitation = builder.precipitation;
        this.category = builder.category;
        this.features = Collections.unmodifiableMap(new HashMap<>(builder.features));
        this.mobSpawns = Collections.unmodifiableList(new ArrayList<>(builder.mobSpawns));
        this.properties = builder.properties;
    }
    
    // Getters
    public String getName() { return name; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getElevation() { return elevation; }
    public int getColor() { return color; }
    public Precipitation getPrecipitation() { return precipitation; }
    public Category getCategory() { return category; }
    public Map<String, Float> getFeatures() { return features; }
    public List<MobSpawn> getMobSpawns() { return mobSpawns; }
    public BiomeProperties getProperties() { return properties; }
    
    /**
     * Gets feature probability
     */
    public float getFeatureProbability(String feature) {
        return features.getOrDefault(feature, 0.0f);
    }
    
    /**
     * Checks if biome has feature
     */
    public boolean hasFeature(String feature) {
        return features.containsKey(feature);
    }
    
    /**
     * Gets mob spawns for a specific mob type
     */
    public List<MobSpawn> getMobSpawns(String mobType) {
        return mobSpawns.stream()
            .filter(spawn -> spawn.getMobType().equals(mobType))
            .collect(ArrayList::new, (list, spawn) -> list.add(spawn), ArrayList::addAll);
    }
    
    @Override
    public String toString() {
        return String.format("BiomeType{name='%s', temp=%.2f, humidity=%.2f, elevation=%.2f, category=%s}", 
            name, temperature, humidity, elevation, category);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BiomeType biomeType = (BiomeType) obj;
        return Objects.equals(name, biomeType.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    /**
     * Precipitation types
     */
    public enum Precipitation {
        NONE,
        RAIN,
        SNOW,
        STORM
    }
    
    /**
     * Biome categories
     */
    public enum Category {
        OCEAN,
        COASTAL,
        ISLAND,
        SPECIAL
    }
    
    /**
     * Mob spawn configuration
     */
    public static class MobSpawn {
        private final String mobType;
        private final int weight;
        private final int minGroup;
        private final int maxGroup;
        
        public MobSpawn(String mobType, int weight, int minGroup, int maxGroup) {
            this.mobType = mobType;
            this.weight = weight;
            this.minGroup = minGroup;
            this.maxGroup = maxGroup;
        }
        
        public String getMobType() { return mobType; }
        public int getWeight() { return weight; }
        public int getMinGroup() { return minGroup; }
        public int getMaxGroup() { return maxGroup; }
        
        @Override
        public String toString() {
            return String.format("MobSpawn{type='%s', weight=%d, group=%d-%d}", 
                mobType, weight, minGroup, maxGroup);
        }
    }
    
    /**
     * Extended biome properties
     */
    public static class BiomeProperties {
        private float depthMin = 0.0f;
        private float depthMax = 10.0f;
        private float waveIntensity = 0.5f;
        private float currentStrength = 0.3f;
        private float pressure = 1.0f;
        private float visibility = 1.0f;
        private float tidalRange = 0.5f;
        private float erosionRate = 0.1f;
        private float fertility = 0.5f;
        private float geothermalActivity = 0.0f;
        private float biodiversity = 0.5f;
        private float dangerLevel = 0.3f;
        private float mysticalEnergy = 0.0f;
        
        // Getters and setters
        public float getDepthMin() { return depthMin; }
        public void setDepthMin(float depthMin) { this.depthMin = depthMin; }
        
        public float getDepthMax() { return depthMax; }
        public void setDepthMax(float depthMax) { this.depthMax = depthMax; }
        
        public float getWaveIntensity() { return waveIntensity; }
        public void setWaveIntensity(float waveIntensity) { this.waveIntensity = waveIntensity; }
        
        public float getCurrentStrength() { return currentStrength; }
        public void setCurrentStrength(float currentStrength) { this.currentStrength = currentStrength; }
        
        public float getPressure() { return pressure; }
        public void setPressure(float pressure) { this.pressure = pressure; }
        
        public float getVisibility() { return visibility; }
        public void setVisibility(float visibility) { this.visibility = visibility; }
        
        public float getTidalRange() { return tidalRange; }
        public void setTidalRange(float tidalRange) { this.tidalRange = tidalRange; }
        
        public float getErosionRate() { return erosionRate; }
        public void setErosionRate(float erosionRate) { this.erosionRate = erosionRate; }
        
        public float getFertility() { return fertility; }
        public void setFertility(float fertility) { this.fertility = fertility; }
        
        public float getGeothermalActivity() { return geothermalActivity; }
        public void setGeothermalActivity(float geothermalActivity) { this.geothermalActivity = geothermalActivity; }
        
        public float getBiodiversity() { return biodiversity; }
        public void setBiodiversity(float biodiversity) { this.biodiversity = biodiversity; }
        
        public float getDangerLevel() { return dangerLevel; }
        public void setDangerLevel(float dangerLevel) { this.dangerLevel = dangerLevel; }
        
        public float getMysticalEnergy() { return mysticalEnergy; }
        public void setMysticalEnergy(float mysticalEnergy) { this.mysticalEnergy = mysticalEnergy; }
    }
    
    /**
     * Builder for creating biome types
     */
    public static class Builder {
        private String name;
        private float temperature = 0.5f;
        private float humidity = 0.5f;
        private float elevation = 0.0f;
        private int color = 0x00FF00;
        private Precipitation precipitation = Precipitation.RAIN;
        private Category category = Category.ISLAND;
        private Map<String, Float> features = new HashMap<>();
        private List<MobSpawn> mobSpawns = new ArrayList<>();
        private BiomeProperties properties = new BiomeProperties();
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder humidity(float humidity) {
            this.humidity = humidity;
            return this;
        }
        
        public Builder elevation(float elevation) {
            this.elevation = elevation;
            return this;
        }
        
        public Builder color(int color) {
            this.color = color;
            return this;
        }
        
        public Builder precipitation(Precipitation precipitation) {
            this.precipitation = precipitation;
            return this;
        }
        
        public Builder category(Category category) {
            this.category = category;
            return this;
        }
        
        public Builder addFeature(String feature, float probability) {
            this.features.put(feature, Math.max(0.0f, Math.min(1.0f, probability)));
            return this;
        }
        
        public Builder addMobSpawn(String mobType, int weight, int minGroup, int maxGroup) {
            this.mobSpawns.add(new MobSpawn(mobType, weight, minGroup, maxGroup));
            return this;
        }
        
        public Builder setDepthRange(float min, float max) {
            this.properties.setDepthMin(min);
            this.properties.setDepthMax(max);
            return this;
        }
        
        public Builder setWaveIntensity(float intensity) {
            this.properties.setWaveIntensity(intensity);
            return this;
        }
        
        public Builder setCurrentStrength(float strength) {
            this.properties.setCurrentStrength(strength);
            return this;
        }
        
        public Builder setPressure(float pressure) {
            this.properties.setPressure(pressure);
            return this;
        }
        
        public Builder setVisibility(float visibility) {
            this.properties.setVisibility(visibility);
            return this;
        }
        
        public Builder setTidalRange(float range) {
            this.properties.setTidalRange(range);
            return this;
        }
        
        public Builder setErosionRate(float rate) {
            this.properties.setErosionRate(rate);
            return this;
        }
        
        public Builder setFertility(float fertility) {
            this.properties.setFertility(fertility);
            return this;
        }
        
        public Builder setGeothermalActivity(float activity) {
            this.properties.setGeothermalActivity(activity);
            return this;
        }
        
        public Builder setBiodiversity(float biodiversity) {
            this.properties.setBiodiversity(biodiversity);
            return this;
        }
        
        public Builder setDangerLevel(float danger) {
            this.properties.setDangerLevel(danger);
            return this;
        }
        
        public Builder setMysticalEnergy(float energy) {
            this.properties.setMysticalEnergy(energy);
            return this;
        }
        
        public BiomeType build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Biome name cannot be null or empty");
            }
            return new BiomeType(this);
        }
    }
}