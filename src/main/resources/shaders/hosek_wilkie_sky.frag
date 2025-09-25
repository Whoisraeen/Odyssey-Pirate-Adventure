#version 330 core

in vec3 v_WorldDirection;
in vec3 v_ViewDirection;

uniform vec3 u_SunDirection;
uniform float u_Turbidity;
uniform float u_GroundAlbedo;
uniform float u_SolarIntensity;
uniform float u_TimeOfDay; // 0.0 to 24.0 hours
uniform vec3 u_SunColor;
uniform float u_Exposure;

out vec4 FragColor;

// Hosek-Wilkie sky model coefficients
// These are precomputed based on turbidity and ground albedo
uniform float u_A[3]; // X, Y, Z coefficients
uniform float u_B[3];
uniform float u_C[3];
uniform float u_D[3];
uniform float u_E[3];
uniform float u_F[3];
uniform float u_G[3];
uniform float u_H[3];
uniform float u_I[3];

// Solar disk coefficients
uniform float u_SolarA[3];
uniform float u_SolarB[3];
uniform float u_SolarC[3];
uniform float u_SolarD[3];
uniform float u_SolarE[3];

const float PI = 3.14159265359;

// Convert CIE XYZ to RGB
vec3 xyzToRgb(vec3 xyz) {
    mat3 xyzToRgbMatrix = mat3(
         3.2404542, -1.5371385, -0.4985314,
        -0.9692660,  1.8760108,  0.0415560,
         0.0556434, -0.2040259,  1.0572252
    );
    return xyzToRgbMatrix * xyz;
}

// Hosek-Wilkie sky model function
float hosekWilkie(float theta, float gamma, float A, float B, float C, float D, float E, float F, float G, float H, float I) {
    float cosGamma = cos(gamma);
    float cosTheta = cos(theta);
    
    float expTerm = exp(-B / (cosTheta + 0.01));
    float rayleighTerm = (1.0 + A * exp(B / (cosTheta + 0.01))) * (1.0 + C * exp(D * gamma) + E * cosGamma * cosGamma + F * cosGamma + G * cosTheta + H * sqrt(cosTheta));
    
    return expTerm * rayleighTerm;
}

// Solar disk radiance
float solarDisk(float theta, float gamma, float A, float B, float C, float D, float E) {
    float cosGamma = cos(gamma);
    float cosTheta = cos(theta);
    
    return A * exp(B / (cosTheta + 0.01)) * (1.0 + C * exp(D * gamma) + E * cosGamma * cosGamma);
}

// Calculate sun intensity based on time of day
float calculateSunIntensity(float timeOfDay) {
    // Convert time to solar angle (0 = midnight, 12 = noon)
    float solarAngle = (timeOfDay - 12.0) * PI / 12.0;
    float sunHeight = sin(solarAngle);
    
    // Atmospheric extinction
    float airMass = 1.0 / max(sunHeight, 0.01);
    float extinction = exp(-0.1 * airMass);
    
    return max(extinction * sunHeight, 0.0);
}

// Enhanced atmospheric scattering
vec3 calculateAtmosphericScattering(vec3 viewDir, vec3 sunDir, float sunIntensity) {
    float theta = acos(max(viewDir.y, 0.0)); // Angle from zenith
    float gamma = acos(max(dot(viewDir, sunDir), 0.0)); // Angle from sun
    
    // Calculate sky radiance for each component (X, Y, Z)
    vec3 skyRadiance;
    skyRadiance.x = hosekWilkie(theta, gamma, u_A[0], u_B[0], u_C[0], u_D[0], u_E[0], u_F[0], u_G[0], u_H[0], u_I[0]);
    skyRadiance.y = hosekWilkie(theta, gamma, u_A[1], u_B[1], u_C[1], u_D[1], u_E[1], u_F[1], u_G[1], u_H[1], u_I[1]);
    skyRadiance.z = hosekWilkie(theta, gamma, u_A[2], u_B[2], u_C[2], u_D[2], u_E[2], u_F[2], u_G[2], u_H[2], u_I[2]);
    
    // Add solar disk
    if (gamma < 0.1) { // Within solar disk
        vec3 solarRadiance;
        solarRadiance.x = solarDisk(theta, gamma, u_SolarA[0], u_SolarB[0], u_SolarC[0], u_SolarD[0], u_SolarE[0]);
        solarRadiance.y = solarDisk(theta, gamma, u_SolarA[1], u_SolarB[1], u_SolarC[1], u_SolarD[1], u_SolarE[1]);
        solarRadiance.z = solarDisk(theta, gamma, u_SolarA[2], u_SolarB[2], u_SolarC[2], u_SolarD[2], u_SolarE[2]);
        
        skyRadiance += solarRadiance * sunIntensity;
    }
    
    return skyRadiance * u_SolarIntensity;
}

// Simple fallback sky gradient for when Hosek-Wilkie is not available
vec3 fallbackSky(vec3 viewDir, vec3 sunDir, float sunIntensity) {
    float height = max(viewDir.y, 0.0);
    
    // Time-based sky colors
    vec3 dayColor = vec3(0.5, 0.7, 1.0);
    vec3 sunsetColor = vec3(1.0, 0.6, 0.3);
    vec3 nightColor = vec3(0.05, 0.05, 0.2);
    
    // Interpolate based on sun height
    float sunHeight = max(sunDir.y, 0.0);
    vec3 skyColor;
    
    if (sunHeight > 0.1) {
        // Day
        skyColor = mix(dayColor * 0.8, dayColor, height);
    } else if (sunHeight > -0.1) {
        // Sunset/sunrise
        float sunsetFactor = (sunHeight + 0.1) / 0.2;
        vec3 currentColor = mix(sunsetColor, dayColor, sunsetFactor);
        skyColor = mix(currentColor * 0.6, currentColor, height);
    } else {
        // Night
        skyColor = mix(nightColor * 0.5, nightColor, height);
    }
    
    // Add sun glow
    float sunDot = max(dot(viewDir, sunDir), 0.0);
    vec3 sunGlow = u_SunColor * pow(sunDot, 256.0) * sunIntensity;
    
    return skyColor + sunGlow;
}

void main() {
    vec3 viewDir = normalize(v_WorldDirection);
    vec3 sunDir = normalize(u_SunDirection);
    
    // Calculate sun intensity based on time of day
    float sunIntensity = calculateSunIntensity(u_TimeOfDay) * u_SolarIntensity;
    
    vec3 skyColor;
    
    // Use Hosek-Wilkie model if coefficients are available
    if (u_A[0] != 0.0 || u_A[1] != 0.0 || u_A[2] != 0.0) {
        vec3 xyzColor = calculateAtmosphericScattering(viewDir, sunDir, sunIntensity);
        skyColor = xyzToRgb(xyzColor);
    } else {
        // Fallback to simple sky gradient
        skyColor = fallbackSky(viewDir, sunDir, sunIntensity);
    }
    
    // Apply exposure and tone mapping
    skyColor *= u_Exposure;
    skyColor = skyColor / (skyColor + vec3(1.0)); // Simple Reinhard tone mapping
    
    // Gamma correction
    skyColor = pow(skyColor, vec3(1.0 / 2.2));
    
    FragColor = vec4(max(skyColor, vec3(0.0)), 1.0);
}