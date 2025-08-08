package com.odyssey.game.components;

import com.odyssey.game.ecs.Component;
import com.odyssey.game.math.Vector2f;

/**
 * Component that defines collision boundaries for an entity.
 */
public class ColliderComponent implements Component {
    public enum ColliderType {
        CIRCLE,
        RECTANGLE,
        POLYGON
    }
    
    public ColliderType type;
    public Vector2f offset; // Offset from entity position
    public boolean isTrigger; // If true, doesn't block movement but still detects collisions
    public String collisionLayer; // Layer for collision filtering
    
    // Circle collider properties
    public float radius;
    
    // Rectangle collider properties
    public float width;
    public float height;
    
    // Polygon collider properties (for complex shapes)
    public Vector2f[] vertices;
    
    public ColliderComponent(ColliderType type) {
        this.type = type;
        this.offset = new Vector2f();
        this.isTrigger = false;
        this.collisionLayer = "default";
    }
    
    /**
     * Create a circle collider.
     */
    public static ColliderComponent createCircle(float radius) {
        ColliderComponent collider = new ColliderComponent(ColliderType.CIRCLE);
        collider.radius = radius;
        return collider;
    }
    
    /**
     * Create a rectangle collider.
     */
    public static ColliderComponent createRectangle(float width, float height) {
        ColliderComponent collider = new ColliderComponent(ColliderType.RECTANGLE);
        collider.width = width;
        collider.height = height;
        return collider;
    }
    
    /**
     * Create a polygon collider.
     */
    public static ColliderComponent createPolygon(Vector2f[] vertices) {
        ColliderComponent collider = new ColliderComponent(ColliderType.POLYGON);
        collider.vertices = new Vector2f[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            collider.vertices[i] = new Vector2f(vertices[i]);
        }
        return collider;
    }
    
    /**
     * Get the bounding box of this collider.
     * Returns {minX, minY, maxX, maxY}
     */
    public float[] getBounds(Vector2f position) {
        switch (type) {
            case CIRCLE:
                float centerX = position.x + offset.x;
                float centerY = position.y + offset.y;
                return new float[] {
                    centerX - radius, centerY - radius,
                    centerX + radius, centerY + radius
                };
                
            case RECTANGLE:
                float rectX = position.x + offset.x - width / 2;
                float rectY = position.y + offset.y - height / 2;
                return new float[] {
                    rectX, rectY,
                    rectX + width, rectY + height
                };
                
            case POLYGON:
                if (vertices == null || vertices.length == 0) {
                    return new float[] {position.x, position.y, position.x, position.y};
                }
                
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
                
                for (Vector2f vertex : vertices) {
                    float x = position.x + offset.x + vertex.x;
                    float y = position.y + offset.y + vertex.y;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
                
                return new float[] {minX, minY, maxX, maxY};
                
            default:
                return new float[] {position.x, position.y, position.x, position.y};
        }
    }
    
    /**
     * Check if a point is inside this collider.
     */
    public boolean containsPoint(Vector2f position, Vector2f point) {
        switch (type) {
            case CIRCLE:
                Vector2f center = new Vector2f(position).add(offset);
                return center.distanceSquared(point) <= radius * radius;
                
            case RECTANGLE:
                float rectX = position.x + offset.x - width / 2;
                float rectY = position.y + offset.y - height / 2;
                return point.x >= rectX && point.x <= rectX + width &&
                       point.y >= rectY && point.y <= rectY + height;
                
            case POLYGON:
                // Point-in-polygon test using ray casting
                if (vertices == null || vertices.length < 3) return false;
                
                boolean inside = false;
                int j = vertices.length - 1;
                
                for (int i = 0; i < vertices.length; i++) {
                    Vector2f vi = new Vector2f(position).add(offset).add(vertices[i]);
                    Vector2f vj = new Vector2f(position).add(offset).add(vertices[j]);
                    
                    if (((vi.y > point.y) != (vj.y > point.y)) &&
                        (point.x < (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x)) {
                        inside = !inside;
                    }
                    j = i;
                }
                
                return inside;
                
            default:
                return false;
        }
    }
    
    @Override
    public Component copy() {
        ColliderComponent copy = new ColliderComponent(type);
        copy.offset.set(offset);
        copy.isTrigger = isTrigger;
        copy.collisionLayer = collisionLayer;
        copy.radius = radius;
        copy.width = width;
        copy.height = height;
        
        if (vertices != null) {
            copy.vertices = new Vector2f[vertices.length];
            for (int i = 0; i < vertices.length; i++) {
                copy.vertices[i] = new Vector2f(vertices[i]);
            }
        }
        
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("Collider{type=%s, trigger=%s, layer=%s}", 
                           type, isTrigger, collisionLayer);
    }
}