package com.odyssey.rendering;

import org.joml.Vector2f;

public class GerstnerWave {

    public float amplitude;
    public float frequency;
    public float phase;
    public Vector2f direction;

    public GerstnerWave(float amplitude, float frequency, float phase, Vector2f direction) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.phase = phase;
        this.direction = direction;
    }
}
