package com.odyssey.world.weather;

/**
 * Represents weather conditions at a specific location and time
 */
public class WeatherCondition {
    
    private float temperature; // In Celsius
    private float humidity; // 0.0 to 1.0
    private float pressure; // In hPa (hectopascals)
    private float visibility; // In meters
    private PrecipitationType precipitation;
    private float precipitationIntensity; // 0.0 to 1.0
    private float cloudCover; // 0.0 to 1.0
    private float windSpeed; // In m/s
    private float windDirection; // In degrees (0-360)
    
    public WeatherCondition() {
        // Default conditions
        this.temperature = 20.0f;
        this.humidity = 0.6f;
        this.pressure = 1013.25f;
        this.visibility = 1000.0f;
        this.precipitation = PrecipitationType.NONE;
        this.precipitationIntensity = 0.0f;
        this.cloudCover = 0.3f;
        this.windSpeed = 5.0f;
        this.windDirection = 0.0f;
    }
    
    public WeatherCondition(float temperature, float humidity, float pressure) {
        this();
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
    }
    
    /**
     * Full constructor for creating weather conditions with all parameters
     */
    public WeatherCondition(float temperature, float humidity, float pressure, float visibility,
                           PrecipitationType precipitation, float precipitationIntensity, 
                           float windSpeed, float windDirection) {
        this.temperature = temperature;
        this.humidity = Math.max(0, Math.min(1, humidity));
        this.pressure = pressure;
        this.visibility = Math.max(0, visibility);
        this.precipitation = precipitation;
        this.precipitationIntensity = Math.max(0, Math.min(1, precipitationIntensity));
        this.cloudCover = 0.3f; // Default cloud cover
        this.windSpeed = Math.max(0, windSpeed);
        this.windDirection = windDirection % 360.0f;
    }
    
    /**
     * Gets a description of the current weather conditions
     */
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        
        // Temperature description
        if (temperature < 0) {
            description.append("Freezing");
        } else if (temperature < 10) {
            description.append("Cold");
        } else if (temperature < 20) {
            description.append("Cool");
        } else if (temperature < 30) {
            description.append("Warm");
        } else {
            description.append("Hot");
        }
        
        // Cloud cover
        if (cloudCover < 0.2f) {
            description.append(", clear skies");
        } else if (cloudCover < 0.5f) {
            description.append(", partly cloudy");
        } else if (cloudCover < 0.8f) {
            description.append(", mostly cloudy");
        } else {
            description.append(", overcast");
        }
        
        // Precipitation
        if (precipitation != PrecipitationType.NONE) {
            description.append(" with ");
            
            if (precipitationIntensity < 0.3f) {
                description.append("light ");
            } else if (precipitationIntensity < 0.7f) {
                description.append("moderate ");
            } else {
                description.append("heavy ");
            }
            
            description.append(precipitation.getDisplayName().toLowerCase());
        }
        
        // Wind conditions
        if (windSpeed > 15.0f) {
            description.append(", strong winds");
        } else if (windSpeed > 8.0f) {
            description.append(", moderate winds");
        } else if (windSpeed > 3.0f) {
            description.append(", light winds");
        } else {
            description.append(", calm");
        }
        
        return description.toString();
    }
    
    /**
     * Checks if conditions are suitable for sailing
     */
    public boolean isSuitableForSailing() {
        // Too dangerous in severe weather
        if (precipitationIntensity > 0.8f) return false;
        if (windSpeed > 25.0f) return false;
        if (visibility < 100.0f) return false;
        
        return true;
    }
    
    /**
     * Gets sailing difficulty factor (0.0 = easy, 1.0 = very difficult)
     */
    public float getSailingDifficulty() {
        float difficulty = 0.0f;
        
        // Wind factor
        if (windSpeed < 2.0f) {
            difficulty += 0.3f; // Too little wind
        } else if (windSpeed > 15.0f) {
            difficulty += (windSpeed - 15.0f) / 20.0f; // Too much wind
        }
        
        // Precipitation factor
        difficulty += precipitationIntensity * 0.4f;
        
        // Visibility factor
        if (visibility < 500.0f) {
            difficulty += (500.0f - visibility) / 500.0f * 0.3f;
        }
        
        // Pressure factor (low pressure = storms)
        if (pressure < 1000.0f) {
            difficulty += (1000.0f - pressure) / 50.0f * 0.2f;
        }
        
        return Math.min(1.0f, difficulty);
    }
    
    /**
     * Gets the comfort level for crew (0.0 = very uncomfortable, 1.0 = very comfortable)
     */
    public float getCrewComfort() {
        float comfort = 1.0f;
        
        // Temperature comfort
        float tempComfort = 1.0f - Math.abs(temperature - 22.0f) / 30.0f;
        comfort *= Math.max(0.2f, tempComfort);
        
        // Humidity comfort
        float humidityComfort = 1.0f - Math.abs(humidity - 0.5f) * 2.0f;
        comfort *= Math.max(0.3f, humidityComfort);
        
        // Precipitation discomfort
        comfort *= (1.0f - precipitationIntensity * 0.6f);
        
        // Wind comfort
        if (windSpeed > 12.0f) {
            comfort *= Math.max(0.4f, 1.0f - (windSpeed - 12.0f) / 20.0f);
        }
        
        return Math.max(0.0f, Math.min(1.0f, comfort));
    }
    
    // Getters and setters
    public float getTemperature() { return temperature; }
    public void setTemperature(float temperature) { this.temperature = temperature; }
    
    public float getHumidity() { return humidity; }
    public void setHumidity(float humidity) { this.humidity = Math.max(0, Math.min(1, humidity)); }
    
    public float getPressure() { return pressure; }
    public void setPressure(float pressure) { this.pressure = pressure; }
    
    public float getVisibility() { return visibility; }
    public void setVisibility(float visibility) { this.visibility = Math.max(0, visibility); }
    
    public PrecipitationType getPrecipitation() { return precipitation; }
    public void setPrecipitation(PrecipitationType precipitation) { this.precipitation = precipitation; }
    
    /**
     * Gets the precipitation type (alias for getPrecipitation())
     * @return the current precipitation type
     */
    public PrecipitationType getPrecipitationType() { return precipitation; }
    
    public float getPrecipitationIntensity() { return precipitationIntensity; }
    public void setPrecipitationIntensity(float intensity) { 
        this.precipitationIntensity = Math.max(0, Math.min(1, intensity)); 
    }
    
    public float getCloudCover() { return cloudCover; }
    public void setCloudCover(float cloudCover) { 
        this.cloudCover = Math.max(0, Math.min(1, cloudCover)); 
    }
    
    public float getWindSpeed() { return windSpeed; }
    public void setWindSpeed(float windSpeed) { this.windSpeed = Math.max(0, windSpeed); }
    
    public float getWindDirection() { return windDirection; }
    public void setWindDirection(float windDirection) { 
        this.windDirection = windDirection % 360.0f;
        if (this.windDirection < 0) this.windDirection += 360.0f;
    }
    
    @Override
    public String toString() {
        return String.format("Weather[temp=%.1f°C, humidity=%.1f%%, pressure=%.1fhPa, %s]",
            temperature, humidity * 100, pressure, getDescription());
    }
}