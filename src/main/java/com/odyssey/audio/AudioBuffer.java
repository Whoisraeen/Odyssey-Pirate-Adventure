package com.odyssey.audio;

import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Represents an audio buffer containing sound data.
 * Manages loading audio files and creating OpenAL buffers.
 */
public class AudioBuffer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioBuffer.class);
    
    private final int bufferId;
    private final String filename;
    private int format;
    private int sampleRate;
    private int channels;
    private int samples;
    private float duration;
    
    /**
     * Create an audio buffer from a file.
     * 
     * @param filename The path to the audio file
     */
    public AudioBuffer(String filename) {
        this.filename = filename;
        this.bufferId = AL10.alGenBuffers();
        
        if (AL10.alGetError() != AL10.AL_NO_ERROR) {
            throw new RuntimeException("Failed to create OpenAL buffer");
        }
        
        loadAudioFile(filename);
        LOGGER.debug("Created audio buffer for: {}", filename);
    }
    
    /**
     * Load audio data from file.
     */
    private void loadAudioFile(String filename) {
        try {
            // Load the audio file as a resource
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
            if (inputStream == null) {
                throw new IOException("Audio file not found: " + filename);
            }
            
            // Read the entire file into a ByteBuffer
            ByteBuffer audioData = readInputStream(inputStream);
            
            // Determine file format and load accordingly
            if (filename.toLowerCase().endsWith(".ogg")) {
                loadOggFile(audioData);
            } else if (filename.toLowerCase().endsWith(".wav")) {
                loadWavFile(audioData);
            } else {
                throw new UnsupportedOperationException("Unsupported audio format: " + filename);
            }
            
            // Free the temporary buffer
            MemoryUtil.memFree(audioData);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load audio file: {}", filename, e);
            throw new RuntimeException("Failed to load audio file: " + filename, e);
        }
    }
    
    /**
     * Load OGG Vorbis file using STB Vorbis.
     */
    private void loadOggFile(ByteBuffer audioData) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);
            
            ShortBuffer rawAudioBuffer = STBVorbis.stb_vorbis_decode_memory(
                audioData, channelsBuffer, sampleRateBuffer);
            
            if (rawAudioBuffer == null) {
                throw new RuntimeException("Failed to decode OGG file");
            }
            
            this.channels = channelsBuffer.get(0);
            this.sampleRate = sampleRateBuffer.get(0);
            this.samples = rawAudioBuffer.remaining();
            this.duration = (float) samples / (sampleRate * channels);
            
            // Determine OpenAL format
            if (channels == 1) {
                format = AL10.AL_FORMAT_MONO16;
            } else if (channels == 2) {
                format = AL10.AL_FORMAT_STEREO16;
            } else {
                throw new UnsupportedOperationException("Unsupported channel count: " + channels);
            }
            
            // Upload to OpenAL buffer
            AL10.alBufferData(bufferId, format, rawAudioBuffer, sampleRate);
            
            // Free the decoded buffer
            MemoryUtil.memFree(rawAudioBuffer);
            
            LOGGER.debug("Loaded OGG file: {} channels, {} Hz, {:.2f}s", channels, sampleRate, duration);
        }
    }
    
    /**
     * Load WAV file (basic implementation).
     * Note: This is a simplified WAV loader. For production, consider using a more robust library.
     */
    private void loadWavFile(ByteBuffer audioData) {
        // This is a basic WAV file parser
        // In a production environment, you might want to use a more robust library
        
        // Skip RIFF header (12 bytes)
        audioData.position(12);
        
        // Find fmt chunk
        while (audioData.remaining() > 8) {
            int chunkId = audioData.getInt();
            int chunkSize = audioData.getInt();
            
            if (chunkId == 0x20746D66) { // "fmt "
                // Read format data
                audioData.getShort(); // audioFormat - skip as not used
                this.channels = audioData.getShort();
                this.sampleRate = audioData.getInt();
                audioData.getInt(); // byteRate - skip as not used
                audioData.getShort(); // blockAlign - skip as not used
                short bitsPerSample = audioData.getShort();
                
                // Skip any extra format bytes
                audioData.position(audioData.position() + chunkSize - 16);
                
                // Determine OpenAL format
                if (bitsPerSample == 8) {
                    format = channels == 1 ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_STEREO8;
                } else if (bitsPerSample == 16) {
                    format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                } else {
                    throw new UnsupportedOperationException("Unsupported bits per sample: " + bitsPerSample);
                }
                
                break;
            } else {
                // Skip unknown chunk
                audioData.position(audioData.position() + chunkSize);
            }
        }
        
        // Find data chunk
        while (audioData.remaining() > 8) {
            int chunkId = audioData.getInt();
            int chunkSize = audioData.getInt();
            
            if (chunkId == 0x61746164) { // "data"
                // Read audio data
                ByteBuffer audioBuffer = MemoryUtil.memAlloc(chunkSize);
                audioData.get(audioBuffer.array(), 0, chunkSize);
                audioBuffer.flip();
                
                this.samples = chunkSize / (channels * 2); // Assuming 16-bit samples
                this.duration = (float) samples / sampleRate;
                
                // Upload to OpenAL buffer
                AL10.alBufferData(bufferId, format, audioBuffer, sampleRate);
                
                // Free the buffer
                MemoryUtil.memFree(audioBuffer);
                
                LOGGER.debug("Loaded WAV file: {} channels, {} Hz, {:.2f}s", channels, sampleRate, duration);
                break;
            } else {
                // Skip unknown chunk
                audioData.position(audioData.position() + chunkSize);
            }
        }
    }
    
    /**
     * Read an InputStream into a ByteBuffer.
     */
    private ByteBuffer readInputStream(InputStream inputStream) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(inputStream);
        ByteBuffer buffer = MemoryUtil.memAlloc(8192);
        
        while (channel.read(buffer) != -1) {
            if (buffer.remaining() == 0) {
                // Resize buffer if needed
                ByteBuffer newBuffer = MemoryUtil.memAlloc(buffer.capacity() * 2);
                buffer.flip();
                newBuffer.put(buffer);
                MemoryUtil.memFree(buffer);
                buffer = newBuffer;
            }
        }
        
        buffer.flip();
        channel.close();
        inputStream.close();
        
        return buffer;
    }
    
    /**
     * Get the OpenAL buffer ID.
     */
    public int getBufferId() {
        return bufferId;
    }
    
    /**
     * Get the filename.
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Get the audio format.
     */
    public int getFormat() {
        return format;
    }
    
    /**
     * Get the sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }
    
    /**
     * Get the number of channels.
     */
    public int getChannels() {
        return channels;
    }
    
    /**
     * Get the number of samples.
     */
    public int getSamples() {
        return samples;
    }
    
    /**
     * Get the duration in seconds.
     */
    public float getDuration() {
        return duration;
    }
    
    /**
     * Clean up the audio buffer.
     */
    public void cleanup() {
        AL10.alDeleteBuffers(bufferId);
        LOGGER.debug("Cleaned up audio buffer: {}", filename);
    }
}