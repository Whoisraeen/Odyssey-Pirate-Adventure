package com.odyssey.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * NBT-equivalent binary tag library for data serialization and storage.
 * Provides a comprehensive system for storing and retrieving structured data
 * in a compact binary format with version management and compression support.
 */
public class BinaryTagLibrary {
    private static final Logger logger = LoggerFactory.getLogger(BinaryTagLibrary.class);
    
    // Tag type constants
    public static final byte TAG_END = 0;
    public static final byte TAG_BYTE = 1;
    public static final byte TAG_SHORT = 2;
    public static final byte TAG_INT = 3;
    public static final byte TAG_LONG = 4;
    public static final byte TAG_FLOAT = 5;
    public static final byte TAG_DOUBLE = 6;
    public static final byte TAG_BYTE_ARRAY = 7;
    public static final byte TAG_STRING = 8;
    public static final byte TAG_LIST = 9;
    public static final byte TAG_COMPOUND = 10;
    public static final byte TAG_INT_ARRAY = 11;
    public static final byte TAG_LONG_ARRAY = 12;
    
    // Version constants
    public static final int CURRENT_VERSION = 1;
    public static final byte[] MAGIC_HEADER = {'O', 'B', 'T', 'G'}; // Odyssey Binary Tag
    
    private final Map<String, TagConverter> converters;
    private boolean compressionEnabled = true;
    private int compressionLevel = 6;
    
    public BinaryTagLibrary() {
        this.converters = new ConcurrentHashMap<>();
        registerDefaultConverters();
        logger.info("Binary tag library initialized");
    }
    
    /**
     * Registers default type converters
     */
    private void registerDefaultConverters() {
        // Primitive converters
        registerConverter("boolean", Boolean.class, 
            value -> new ByteTag((byte) (((Boolean) value) ? 1 : 0)),
            tag -> ((ByteTag) tag).getValue() != 0);
        
        registerConverter("uuid", UUID.class,
            value -> {
                UUID uuid = (UUID) value;
                CompoundTag compound = new CompoundTag();
                compound.putLong("most", uuid.getMostSignificantBits());
                compound.putLong("least", uuid.getLeastSignificantBits());
                return compound;
            },
            tag -> {
                CompoundTag compound = (CompoundTag) tag;
                return new UUID(compound.getLong("most"), compound.getLong("least"));
            });
        
        // Collection converters
        registerConverter("stringList", List.class,
            value -> {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) value;
                ListTag listTag = new ListTag();
                for (String str : list) {
                    listTag.add(new StringTag(str));
                }
                return listTag;
            },
            tag -> {
                ListTag listTag = (ListTag) tag;
                List<String> result = new ArrayList<>();
                for (Tag t : listTag.getValue()) {
                    if (t instanceof StringTag) {
                        result.add(((StringTag) t).getValue());
                    }
                }
                return result;
            });
        
        logger.debug("Registered {} default converters", converters.size());
    }
    
    /**
     * Registers a custom type converter
     */
    public <T> void registerConverter(String name, Class<T> type, 
                                     java.util.function.Function<Object, Tag> serializer,
                                     java.util.function.Function<Tag, Object> deserializer) {
        converters.put(name, new TagConverter(type, serializer, deserializer));
        logger.debug("Registered converter for type: {}", name);
    }
    
    /**
     * Writes a compound tag to a byte array
     */
    public byte[] writeToBytes(CompoundTag tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeToStream(tag, baos);
        return baos.toByteArray();
    }
    
    /**
     * Writes a compound tag to an output stream
     */
    public void writeToStream(CompoundTag tag, OutputStream output) throws IOException {
        OutputStream stream = output;
        
        if (compressionEnabled) {
            stream = new GZIPOutputStream(output, compressionLevel);
        }
        
        try (DataOutputStream dos = new DataOutputStream(stream)) {
            // Write header
            dos.write(MAGIC_HEADER);
            dos.writeInt(CURRENT_VERSION);
            
            // Write root tag
            writeTag(dos, "", tag);
        }
        
        if (compressionEnabled) {
            ((GZIPOutputStream) stream).finish();
        }
    }
    
    /**
     * Reads a compound tag from a byte array
     */
    public CompoundTag readFromBytes(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        return readFromStream(bais);
    }
    
    /**
     * Reads a compound tag from an input stream
     */
    public CompoundTag readFromStream(InputStream input) throws IOException {
        InputStream stream = input;
        
        // Check if data is compressed
        stream.mark(4);
        byte[] header = new byte[4];
        stream.read(header);
        stream.reset();
        
        if (!Arrays.equals(header, MAGIC_HEADER)) {
            // Try with GZIP decompression
            stream = new GZIPInputStream(input);
        }
        
        try (DataInputStream dis = new DataInputStream(stream)) {
            // Read and verify header
            byte[] magic = new byte[4];
            dis.readFully(magic);
            if (!Arrays.equals(magic, MAGIC_HEADER)) {
                throw new IOException("Invalid file format: magic header mismatch");
            }
            
            int version = dis.readInt();
            if (version > CURRENT_VERSION) {
                throw new IOException("Unsupported version: " + version);
            }
            
            // Read root tag
            Tag tag = readTag(dis);
            if (!(tag instanceof CompoundTag)) {
                throw new IOException("Root tag must be a compound tag");
            }
            
            return (CompoundTag) tag;
        }
    }
    
    /**
     * Writes a tag to a data output stream
     */
    private void writeTag(DataOutputStream dos, String name, Tag tag) throws IOException {
        dos.writeByte(tag.getType());
        
        if (tag.getType() != TAG_END) {
            writeString(dos, name);
            writeTagPayload(dos, tag);
        }
    }
    
    /**
     * Writes tag payload data
     */
    private void writeTagPayload(DataOutputStream dos, Tag tag) throws IOException {
        switch (tag.getType()) {
            case TAG_BYTE:
                dos.writeByte(((ByteTag) tag).getValue());
                break;
            case TAG_SHORT:
                dos.writeShort(((ShortTag) tag).getValue());
                break;
            case TAG_INT:
                dos.writeInt(((IntTag) tag).getValue());
                break;
            case TAG_LONG:
                dos.writeLong(((LongTag) tag).getValue());
                break;
            case TAG_FLOAT:
                dos.writeFloat(((FloatTag) tag).getValue());
                break;
            case TAG_DOUBLE:
                dos.writeDouble(((DoubleTag) tag).getValue());
                break;
            case TAG_BYTE_ARRAY:
                byte[] bytes = ((ByteArrayTag) tag).getValue();
                dos.writeInt(bytes.length);
                dos.write(bytes);
                break;
            case TAG_STRING:
                writeString(dos, ((StringTag) tag).getValue());
                break;
            case TAG_LIST:
                ListTag list = (ListTag) tag;
                dos.writeByte(list.getElementType());
                dos.writeInt(list.size());
                for (Tag element : list.getValue()) {
                    writeTagPayload(dos, element);
                }
                break;
            case TAG_COMPOUND:
                CompoundTag compound = (CompoundTag) tag;
                for (Map.Entry<String, Tag> entry : compound.getValue().entrySet()) {
                    writeTag(dos, entry.getKey(), entry.getValue());
                }
                dos.writeByte(TAG_END);
                break;
            case TAG_INT_ARRAY:
                int[] ints = ((IntArrayTag) tag).getValue();
                dos.writeInt(ints.length);
                for (int i : ints) {
                    dos.writeInt(i);
                }
                break;
            case TAG_LONG_ARRAY:
                long[] longs = ((LongArrayTag) tag).getValue();
                dos.writeInt(longs.length);
                for (long l : longs) {
                    dos.writeLong(l);
                }
                break;
            default:
                throw new IOException("Unknown tag type: " + tag.getType());
        }
    }
    
    /**
     * Reads a tag from a data input stream
     */
    private Tag readTag(DataInputStream dis) throws IOException {
        byte type = dis.readByte();
        
        if (type == TAG_END) {
            return new EndTag();
        }
        
        String name = readString(dis);
        return readTagPayload(dis, type);
    }
    
    /**
     * Reads tag payload data
     */
    private Tag readTagPayload(DataInputStream dis, byte type) throws IOException {
        switch (type) {
            case TAG_BYTE:
                return new ByteTag(dis.readByte());
            case TAG_SHORT:
                return new ShortTag(dis.readShort());
            case TAG_INT:
                return new IntTag(dis.readInt());
            case TAG_LONG:
                return new LongTag(dis.readLong());
            case TAG_FLOAT:
                return new FloatTag(dis.readFloat());
            case TAG_DOUBLE:
                return new DoubleTag(dis.readDouble());
            case TAG_BYTE_ARRAY:
                int byteLength = dis.readInt();
                byte[] bytes = new byte[byteLength];
                dis.readFully(bytes);
                return new ByteArrayTag(bytes);
            case TAG_STRING:
                return new StringTag(readString(dis));
            case TAG_LIST:
                byte elementType = dis.readByte();
                int listSize = dis.readInt();
                ListTag list = new ListTag();
                for (int i = 0; i < listSize; i++) {
                    list.add(readTagPayload(dis, elementType));
                }
                return list;
            case TAG_COMPOUND:
                CompoundTag compound = new CompoundTag();
                Tag tag;
                while ((tag = readTag(dis)).getType() != TAG_END) {
                    // Note: In a real implementation, we'd need to track the name
                    // For now, we'll use a placeholder approach
                    compound.put("tag_" + compound.size(), tag);
                }
                return compound;
            case TAG_INT_ARRAY:
                int intLength = dis.readInt();
                int[] ints = new int[intLength];
                for (int i = 0; i < intLength; i++) {
                    ints[i] = dis.readInt();
                }
                return new IntArrayTag(ints);
            case TAG_LONG_ARRAY:
                int longLength = dis.readInt();
                long[] longs = new long[longLength];
                for (int i = 0; i < longLength; i++) {
                    longs[i] = dis.readLong();
                }
                return new LongArrayTag(longs);
            default:
                throw new IOException("Unknown tag type: " + type);
        }
    }
    
    /**
     * Writes a string to the output stream
     */
    private void writeString(DataOutputStream dos, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }
    
    /**
     * Reads a string from the input stream
     */
    private String readString(DataInputStream dis) throws IOException {
        short length = dis.readShort();
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Creates a compound tag from a map
     */
    public CompoundTag createCompound(Map<String, Object> data) {
        CompoundTag compound = new CompoundTag();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            Tag tag = convertToTag(value);
            if (tag != null) {
                compound.put(key, tag);
            }
        }
        
        return compound;
    }
    
    /**
     * Converts a compound tag to a map
     */
    public Map<String, Object> toMap(CompoundTag compound) {
        Map<String, Object> result = new HashMap<>();
        
        for (Map.Entry<String, Tag> entry : compound.getValue().entrySet()) {
            String key = entry.getKey();
            Tag tag = entry.getValue();
            
            Object value = convertFromTag(tag);
            if (value != null) {
                result.put(key, value);
            }
        }
        
        return result;
    }
    
    /**
     * Converts a Java object to a tag
     */
    private Tag convertToTag(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Byte) {
            return new ByteTag((Byte) value);
        } else if (value instanceof Short) {
            return new ShortTag((Short) value);
        } else if (value instanceof Integer) {
            return new IntTag((Integer) value);
        } else if (value instanceof Long) {
            return new LongTag((Long) value);
        } else if (value instanceof Float) {
            return new FloatTag((Float) value);
        } else if (value instanceof Double) {
            return new DoubleTag((Double) value);
        } else if (value instanceof String) {
            return new StringTag((String) value);
        } else if (value instanceof byte[]) {
            return new ByteArrayTag((byte[]) value);
        } else if (value instanceof int[]) {
            return new IntArrayTag((int[]) value);
        } else if (value instanceof long[]) {
            return new LongArrayTag((long[]) value);
        } else if (value instanceof List) {
            ListTag list = new ListTag();
            for (Object item : (List<?>) value) {
                Tag tag = convertToTag(item);
                if (tag != null) {
                    list.add(tag);
                }
            }
            return list;
        } else if (value instanceof Map) {
            CompoundTag compound = new CompoundTag();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Tag tag = convertToTag(entry.getValue());
                if (tag != null) {
                    compound.put(entry.getKey(), tag);
                }
            }
            return compound;
        }
        
        // Try custom converters
        for (TagConverter converter : converters.values()) {
            if (converter.getType().isInstance(value)) {
                return converter.getSerializer().apply(value);
            }
        }
        
        logger.warn("Cannot convert object of type {} to tag", value.getClass().getSimpleName());
        return null;
    }
    
    /**
     * Converts a tag to a Java object
     */
    private Object convertFromTag(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        switch (tag.getType()) {
            case TAG_BYTE:
                return ((ByteTag) tag).getValue();
            case TAG_SHORT:
                return ((ShortTag) tag).getValue();
            case TAG_INT:
                return ((IntTag) tag).getValue();
            case TAG_LONG:
                return ((LongTag) tag).getValue();
            case TAG_FLOAT:
                return ((FloatTag) tag).getValue();
            case TAG_DOUBLE:
                return ((DoubleTag) tag).getValue();
            case TAG_STRING:
                return ((StringTag) tag).getValue();
            case TAG_BYTE_ARRAY:
                return ((ByteArrayTag) tag).getValue();
            case TAG_INT_ARRAY:
                return ((IntArrayTag) tag).getValue();
            case TAG_LONG_ARRAY:
                return ((LongArrayTag) tag).getValue();
            case TAG_LIST:
                ListTag list = (ListTag) tag;
                List<Object> result = new ArrayList<>();
                for (Tag element : list.getValue()) {
                    result.add(convertFromTag(element));
                }
                return result;
            case TAG_COMPOUND:
                return toMap((CompoundTag) tag);
            default:
                logger.warn("Cannot convert tag of type {} to object", tag.getType());
                return null;
        }
    }
    
    // Configuration methods
    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
        logger.debug("Compression {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
    
    public void setCompressionLevel(int level) {
        this.compressionLevel = Math.max(1, Math.min(9, level));
        logger.debug("Compression level set to {}", this.compressionLevel);
    }
    
    public int getCompressionLevel() {
        return compressionLevel;
    }
    
    // Tag classes
    public interface Tag {
        byte getType();
    }
    
    public static class EndTag implements Tag {
        @Override
        public byte getType() { return TAG_END; }
    }
    
    public static class ByteTag implements Tag {
        private final byte value;
        
        public ByteTag(byte value) { this.value = value; }
        public byte getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_BYTE; }
    }
    
    public static class ShortTag implements Tag {
        private final short value;
        
        public ShortTag(short value) { this.value = value; }
        public short getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_SHORT; }
    }
    
    public static class IntTag implements Tag {
        private final int value;
        
        public IntTag(int value) { this.value = value; }
        public int getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_INT; }
    }
    
    public static class LongTag implements Tag {
        private final long value;
        
        public LongTag(long value) { this.value = value; }
        public long getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_LONG; }
    }
    
    public static class FloatTag implements Tag {
        private final float value;
        
        public FloatTag(float value) { this.value = value; }
        public float getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_FLOAT; }
    }
    
    public static class DoubleTag implements Tag {
        private final double value;
        
        public DoubleTag(double value) { this.value = value; }
        public double getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_DOUBLE; }
    }
    
    public static class ByteArrayTag implements Tag {
        private final byte[] value;
        
        public ByteArrayTag(byte[] value) { this.value = value.clone(); }
        public byte[] getValue() { return value.clone(); }
        
        @Override
        public byte getType() { return TAG_BYTE_ARRAY; }
    }
    
    public static class StringTag implements Tag {
        private final String value;
        
        public StringTag(String value) { this.value = value; }
        public String getValue() { return value; }
        
        @Override
        public byte getType() { return TAG_STRING; }
    }
    
    public static class ListTag implements Tag {
        private final List<Tag> value;
        private byte elementType = TAG_END;
        
        public ListTag() { this.value = new ArrayList<>(); }
        public ListTag(byte elementType, List<Tag> value) { 
            this.elementType = elementType;
            this.value = value;
        }
        public List<Tag> getValue() { return value; }
        
        public void add(Tag tag) {
            if (value.isEmpty()) {
                elementType = tag.getType();
            }
            value.add(tag);
        }
        
        public int size() { return value.size(); }
        public byte getElementType() { return elementType; }
        
        public CompoundTag getCompound(int index) {
            if (index >= 0 && index < value.size()) {
                Tag tag = value.get(index);
                return tag instanceof CompoundTag ? (CompoundTag) tag : new CompoundTag();
            }
            return new CompoundTag();
        }
        
        public String getString(int index) {
            if (index >= 0 && index < value.size()) {
                Tag tag = value.get(index);
                return tag instanceof StringTag ? ((StringTag) tag).getValue() : "";
            }
            return "";
        }
        
        public ListTag copy() {
            ListTag copy = new ListTag(elementType, new ArrayList<>());
            for (Tag tag : value) {
                copy.add(copyTag(tag));
            }
            return copy;
        }
        
        private Tag copyTag(Tag tag) {
            switch (tag.getType()) {
                case TAG_BYTE: return new ByteTag(((ByteTag) tag).getValue());
                case TAG_SHORT: return new ShortTag(((ShortTag) tag).getValue());
                case TAG_INT: return new IntTag(((IntTag) tag).getValue());
                case TAG_LONG: return new LongTag(((LongTag) tag).getValue());
                case TAG_FLOAT: return new FloatTag(((FloatTag) tag).getValue());
                case TAG_DOUBLE: return new DoubleTag(((DoubleTag) tag).getValue());
                case TAG_STRING: return new StringTag(((StringTag) tag).getValue());
                case TAG_BYTE_ARRAY: return new ByteArrayTag(((ByteArrayTag) tag).getValue());
                case TAG_INT_ARRAY: return new IntArrayTag(((IntArrayTag) tag).getValue());
                case TAG_LONG_ARRAY: return new LongArrayTag(((LongArrayTag) tag).getValue());
                case TAG_COMPOUND: return ((CompoundTag) tag).copy();
                case TAG_LIST: return ((ListTag) tag).copy();
                default: return tag;
            }
        }
        
        @Override
        public byte getType() { return TAG_LIST; }
    }
    
    public static class CompoundTag implements Tag {
        private final Map<String, Tag> value;
        
        public CompoundTag() { this.value = new HashMap<>(); }
        public Map<String, Tag> getValue() { return value; }
        
        public void put(String key, Tag tag) { value.put(key, tag); }
        public Tag get(String key) { return value.get(key); }
        public boolean contains(String key) { return value.containsKey(key); }
        
        // Convenience methods
        public void putByte(String key, byte value) { put(key, new ByteTag(value)); }
        public void putShort(String key, short value) { put(key, new ShortTag(value)); }
        public void putInt(String key, int value) { put(key, new IntTag(value)); }
        public void putLong(String key, long value) { put(key, new LongTag(value)); }
        public void putFloat(String key, float value) { put(key, new FloatTag(value)); }
        public void putDouble(String key, double value) { put(key, new DoubleTag(value)); }
        public void putString(String key, String value) { put(key, new StringTag(value)); }
        public void putByteArray(String key, byte[] value) { put(key, new ByteArrayTag(value)); }
        public void putIntArray(String key, int[] value) { put(key, new IntArrayTag(value)); }
        public void putLongArray(String key, long[] value) { put(key, new LongArrayTag(value)); }
        
        public byte getByte(String key) {
            Tag tag = get(key);
            return tag instanceof ByteTag ? ((ByteTag) tag).getValue() : 0;
        }
        
        public short getShort(String key) {
            Tag tag = get(key);
            return tag instanceof ShortTag ? ((ShortTag) tag).getValue() : 0;
        }
        
        public int getInt(String key) {
            Tag tag = get(key);
            return tag instanceof IntTag ? ((IntTag) tag).getValue() : 0;
        }
        
        public long getLong(String key) {
            Tag tag = get(key);
            return tag instanceof LongTag ? ((LongTag) tag).getValue() : 0L;
        }
        
        public float getFloat(String key) {
            Tag tag = get(key);
            return tag instanceof FloatTag ? ((FloatTag) tag).getValue() : 0.0f;
        }
        
        public double getDouble(String key) {
            Tag tag = get(key);
            return tag instanceof DoubleTag ? ((DoubleTag) tag).getValue() : 0.0;
        }
        
        public String getString(String key) {
            Tag tag = get(key);
            return tag instanceof StringTag ? ((StringTag) tag).getValue() : "";
        }
        
        public byte[] getByteArray(String key) {
            Tag tag = get(key);
            return tag instanceof ByteArrayTag ? ((ByteArrayTag) tag).getValue() : new byte[0];
        }
        
        public int[] getIntArray(String key) {
            Tag tag = get(key);
            return tag instanceof IntArrayTag ? ((IntArrayTag) tag).getValue() : new int[0];
        }
        
        public long[] getLongArray(String key) {
            Tag tag = get(key);
            return tag instanceof LongArrayTag ? ((LongArrayTag) tag).getValue() : new long[0];
        }
        
        // Additional missing methods
        public void putBoolean(String key, boolean value) { putByte(key, (byte) (value ? 1 : 0)); }
        public void putBoolean(String key, Boolean value) { putBoolean(key, value != null && value); }
        public boolean getBoolean(String key) { return getByte(key) != 0; }
        
        public CompoundTag getCompound(String key) {
            Tag tag = get(key);
            return tag instanceof CompoundTag ? (CompoundTag) tag : new CompoundTag();
        }
        
        public ListTag getList(String key) {
            Tag tag = get(key);
            return tag instanceof ListTag ? (ListTag) tag : new ListTag(TAG_END, new ArrayList<>());
        }
        
        public boolean isEmpty() { return value.isEmpty(); }
        public int size() { return value.size(); }
        public Set<String> getAllKeys() { return value.keySet(); }
        public Tag remove(String key) { return value.remove(key); }
        
        public CompoundTag copy() {
            CompoundTag copy = new CompoundTag();
            for (Map.Entry<String, Tag> entry : value.entrySet()) {
                copy.put(entry.getKey(), copyTag(entry.getValue()));
            }
            return copy;
        }
        
        private Tag copyTag(Tag tag) {
            switch (tag.getType()) {
                case TAG_BYTE: return new ByteTag(((ByteTag) tag).getValue());
                case TAG_SHORT: return new ShortTag(((ShortTag) tag).getValue());
                case TAG_INT: return new IntTag(((IntTag) tag).getValue());
                case TAG_LONG: return new LongTag(((LongTag) tag).getValue());
                case TAG_FLOAT: return new FloatTag(((FloatTag) tag).getValue());
                case TAG_DOUBLE: return new DoubleTag(((DoubleTag) tag).getValue());
                case TAG_STRING: return new StringTag(((StringTag) tag).getValue());
                case TAG_BYTE_ARRAY: return new ByteArrayTag(((ByteArrayTag) tag).getValue());
                case TAG_INT_ARRAY: return new IntArrayTag(((IntArrayTag) tag).getValue());
                case TAG_LONG_ARRAY: return new LongArrayTag(((LongArrayTag) tag).getValue());
                case TAG_COMPOUND: return ((CompoundTag) tag).copy();
                case TAG_LIST: return ((ListTag) tag).copy();
                default: return tag;
            }
        }
        
        @Override
        public byte getType() { return TAG_COMPOUND; }
    }
    
    public static class IntArrayTag implements Tag {
        private final int[] value;
        
        public IntArrayTag(int[] value) { this.value = value.clone(); }
        public int[] getValue() { return value.clone(); }
        
        @Override
        public byte getType() { return TAG_INT_ARRAY; }
    }
    
    public static class LongArrayTag implements Tag {
        private final long[] value;
        
        public LongArrayTag(long[] value) { this.value = value.clone(); }
        public long[] getValue() { return value.clone(); }
        
        @Override
        public byte getType() { return TAG_LONG_ARRAY; }
    }
    
    // Tag converter class
    private static class TagConverter {
        private final Class<?> type;
        private final java.util.function.Function<Object, Tag> serializer;
        private final java.util.function.Function<Tag, Object> deserializer;
        
        public TagConverter(Class<?> type, 
                           java.util.function.Function<Object, Tag> serializer,
                           java.util.function.Function<Tag, Object> deserializer) {
            this.type = type;
            this.serializer = serializer;
            this.deserializer = deserializer;
        }
        
        public Class<?> getType() { return type; }
        public java.util.function.Function<Object, Tag> getSerializer() { return serializer; }
        public java.util.function.Function<Tag, Object> getDeserializer() { return deserializer; }
    }
}