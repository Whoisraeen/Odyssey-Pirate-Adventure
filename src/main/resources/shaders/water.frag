#version 330 core

in vec2 textureCoords;
in vec4 clipSpace;
in vec3 toCameraVector;
in vec3 fromLightVector;
in vec3 worldPosition;

out vec4 out_Color;

uniform sampler2D reflectionTexture;
uniform sampler2D refractionTexture;
uniform sampler2D dudvMap;
uniform sampler2D normalMap;
uniform sampler2D depthMap;
uniform sampler2D causticsTexture;
uniform sampler2D waterAnimationTexture;

uniform vec4 animationFrameUV;
uniform float useAnimatedTexture;
uniform float moveFactor;
uniform vec3 lightColor;
uniform float shineDamper;
uniform float reflectivity;
uniform float waveStrength;
uniform float near;
uniform float far;

const float tiling = 6.0;

void main(void) {
    vec2 ndc = (clipSpace.xy / clipSpace.w) / 2.0 + 0.5;
    vec2 reflectTexCoords = vec2(ndc.x, -ndc.y);
    vec2 refractTexCoords = vec2(ndc.x, ndc.y);
    
    float depth = texture(depthMap, refractTexCoords).r;
    float floorDistance = 2.0 * near * far / (far + near - (2.0 * depth - 1.0) * (far - near));
    
    depth = gl_FragCoord.z;
    float waterDistance = 2.0 * near * far / (far + near - (2.0 * depth - 1.0) * (far - near));
    float waterDepth = floorDistance - waterDistance;
    
    vec2 distortedTexCoords = texture(dudvMap, vec2(textureCoords.x + moveFactor, textureCoords.y)).rg * 0.1;
    distortedTexCoords = textureCoords + vec2(distortedTexCoords.x, distortedTexCoords.y + moveFactor);
    vec2 totalDistortion = (texture(dudvMap, distortedTexCoords).rg * 2.0 - 1.0) * waveStrength * clamp(waterDepth / 20.0, 0.0, 1.0);
    
    reflectTexCoords += totalDistortion;
    reflectTexCoords.x = clamp(reflectTexCoords.x, 0.001, 0.999);
    reflectTexCoords.y = clamp(reflectTexCoords.y, -0.999, -0.001);
    
    refractTexCoords += totalDistortion;
    refractTexCoords = clamp(refractTexCoords, 0.001, 0.999);
    
    vec4 reflectColor = texture(reflectionTexture, reflectTexCoords);
    vec4 refractColor = texture(refractionTexture, refractTexCoords);
    
    vec4 normalMapColor = texture(normalMap, distortedTexCoords);
    vec3 normal = vec3(normalMapColor.r * 2.0 - 1.0, normalMapColor.b * 3.0, normalMapColor.g * 2.0 - 1.0);
    normal = normalize(normal);
    
    // Add animated water texture if available
    vec4 animatedColor = vec4(0.0, 0.4, 0.6, 1.0); // Default water color
    if (useAnimatedTexture > 0.5) {
        vec2 animUV = mix(animationFrameUV.xy, animationFrameUV.zw, fract(textureCoords * tiling));
        animatedColor = texture(waterAnimationTexture, animUV);
    }
    
    vec3 viewVector = normalize(toCameraVector);
    float refractiveFactor = dot(viewVector, normal);
    refractiveFactor = pow(refractiveFactor, 0.8);
    refractiveFactor = clamp(refractiveFactor, 0.0, 1.0);
    
    vec3 reflectedLight = reflect(normalize(fromLightVector), normal);
    float specular = max(dot(reflectedLight, viewVector), 0.0);
    specular = pow(specular, shineDamper);
    vec3 specularHighlights = lightColor * specular * reflectivity * clamp(waterDepth / 5.0, 0.0, 1.0);
    
    // Add caustics effect
    vec2 causticsCoords = worldPosition.xz * 0.1 + moveFactor * 0.5;
    vec4 caustics = texture(causticsTexture, causticsCoords);
    refractColor += caustics * 0.3 * clamp(waterDepth / 10.0, 0.0, 1.0);
    
    // Blend animated texture with reflection/refraction
    vec4 finalColor = mix(reflectColor, refractColor, refractiveFactor);
    if (useAnimatedTexture > 0.5) {
        finalColor = mix(finalColor, animatedColor, 0.2);
    }
    
    out_Color = mix(finalColor, vec4(0.0, 0.3, 0.5, 1.0), 0.2) + vec4(specularHighlights, 0.0);
    out_Color.a = clamp(waterDepth / 5.0, 0.0, 1.0);
}