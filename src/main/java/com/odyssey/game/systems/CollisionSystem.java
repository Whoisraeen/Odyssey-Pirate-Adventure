package com.odyssey.game.systems;

import com.odyssey.game.ecs.Entity;
import com.odyssey.game.ecs.System;
import com.odyssey.game.components.TransformComponent;
import com.odyssey.game.components.PhysicsComponent;
import com.odyssey.game.components.ColliderComponent;
import com.odyssey.game.math.Vector2f;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * System that handles collision detection and response.
 */
public class CollisionSystem extends System {
    
    /**
     * Represents a collision between two entities.
     */
    public static class Collision {
        public Entity entityA;
        public Entity entityB;
        public Vector2f contactPoint;
        public Vector2f normal; // Normal pointing from A to B
        public float penetration;
        public boolean isTrigger;
        
        public Collision(Entity entityA, Entity entityB, Vector2f contactPoint, 
                        Vector2f normal, float penetration, boolean isTrigger) {
            this.entityA = entityA;
            this.entityB = entityB;
            this.contactPoint = new Vector2f(contactPoint);
            this.normal = new Vector2f(normal);
            this.penetration = penetration;
            this.isTrigger = isTrigger;
        }
    }
    
    private List<Collision> currentCollisions = new ArrayList<>();
    private Set<String> collisionPairs = new HashSet<>();
    
    public CollisionSystem() {
        setPriority(90); // Run after physics but before rendering
    }
    
    @Override
    public void update(float deltaTime) {
        currentCollisions.clear();
        collisionPairs.clear();
        
        List<Entity> colliderEntities = world.getEntitiesWith(TransformComponent.class, ColliderComponent.class);
        
        // Broad phase: Check all pairs of entities
        for (int i = 0; i < colliderEntities.size(); i++) {
            for (int j = i + 1; j < colliderEntities.size(); j++) {
                Entity entityA = colliderEntities.get(i);
                Entity entityB = colliderEntities.get(j);
                
                // Skip if same entity
                if (entityA.getId() == entityB.getId()) {
                    continue;
                }
                
                // Create unique pair identifier
                String pairId = Math.min(entityA.getId(), entityB.getId()) + ":" + 
                               Math.max(entityA.getId(), entityB.getId());
                
                if (collisionPairs.contains(pairId)) {
                    continue; // Already checked this pair
                }
                collisionPairs.add(pairId);
                
                // Check for collision
                Collision collision = checkCollision(entityA, entityB);
                if (collision != null) {
                    currentCollisions.add(collision);
                    
                    // Handle collision response
                    if (!collision.isTrigger) {
                        resolveCollision(collision);
                    }
                }
            }
        }
    }
    
    /**
     * Check if two entities are colliding.
     */
    private Collision checkCollision(Entity entityA, Entity entityB) {
        TransformComponent transformA = entityA.get(TransformComponent.class);
        TransformComponent transformB = entityB.get(TransformComponent.class);
        ColliderComponent colliderA = entityA.get(ColliderComponent.class);
        ColliderComponent colliderB = entityB.get(ColliderComponent.class);
        
        // Check collision layers (simple string comparison for now)
        if (!canCollide(colliderA.collisionLayer, colliderB.collisionLayer)) {
            return null;
        }
        
        // Get world positions
        Vector2f posA = new Vector2f(transformA.position).add(colliderA.offset);
        Vector2f posB = new Vector2f(transformB.position).add(colliderB.offset);
        
        // Broad phase: AABB check
        if (!aabbOverlap(colliderA, posA, colliderB, posB)) {
            return null;
        }
        
        // Narrow phase: Detailed collision detection
        return detectCollision(entityA, entityB, colliderA, posA, colliderB, posB);
    }
    
    /**
     * Check if two collision layers can collide.
     */
    private boolean canCollide(String layerA, String layerB) {
        // For now, all layers can collide with each other
        // This can be expanded to include a collision matrix
        return true;
    }
    
    /**
     * Check if two AABBs overlap.
     */
    private boolean aabbOverlap(ColliderComponent colliderA, Vector2f posA, 
                               ColliderComponent colliderB, Vector2f posB) {
        float[] boundsA = colliderA.getBounds(posA);
        float[] boundsB = colliderB.getBounds(posB);
        
        return !(boundsA[2] < boundsB[0] || boundsA[0] > boundsB[2] ||
                 boundsA[3] < boundsB[1] || boundsA[1] > boundsB[3]);
    }
    
    /**
     * Detailed collision detection between two colliders.
     */
    private Collision detectCollision(Entity entityA, Entity entityB,
                                    ColliderComponent colliderA, Vector2f posA,
                                    ColliderComponent colliderB, Vector2f posB) {
        
        // Circle vs Circle
        if (colliderA.type == ColliderComponent.ColliderType.CIRCLE &&
            colliderB.type == ColliderComponent.ColliderType.CIRCLE) {
            return circleVsCircle(entityA, entityB, colliderA, posA, colliderB, posB);
        }
        
        // Rectangle vs Rectangle
        if (colliderA.type == ColliderComponent.ColliderType.RECTANGLE &&
            colliderB.type == ColliderComponent.ColliderType.RECTANGLE) {
            return rectangleVsRectangle(entityA, entityB, colliderA, posA, colliderB, posB);
        }
        
        // Circle vs Rectangle
        if (colliderA.type == ColliderComponent.ColliderType.CIRCLE &&
            colliderB.type == ColliderComponent.ColliderType.RECTANGLE) {
            return circleVsRectangle(entityA, entityB, colliderA, posA, colliderB, posB);
        }
        
        if (colliderA.type == ColliderComponent.ColliderType.RECTANGLE &&
            colliderB.type == ColliderComponent.ColliderType.CIRCLE) {
            Collision collision = circleVsRectangle(entityB, entityA, colliderB, posB, colliderA, posA);
            if (collision != null) {
                // Swap entities and flip normal
                Entity temp = collision.entityA;
                collision.entityA = collision.entityB;
                collision.entityB = temp;
                collision.normal.multiply(-1);
            }
            return collision;
        }
        
        // For now, other collision types are not implemented
        return null;
    }
    
    /**
     * Circle vs Circle collision detection.
     */
    private Collision circleVsCircle(Entity entityA, Entity entityB,
                                    ColliderComponent colliderA, Vector2f posA,
                                    ColliderComponent colliderB, Vector2f posB) {
        Vector2f direction = new Vector2f(posB).subtract(posA);
        float distance = direction.length();
        float radiusSum = colliderA.radius + colliderB.radius;
        
        if (distance < radiusSum) {
            float penetration = radiusSum - distance;
            Vector2f normal = distance > 0 ? direction.normalize() : new Vector2f(1, 0);
            Vector2f contactPoint = new Vector2f(posA).add(new Vector2f(normal).multiply(colliderA.radius));
            
            boolean isTrigger = colliderA.isTrigger || colliderB.isTrigger;
            return new Collision(entityA, entityB, contactPoint, normal, penetration, isTrigger);
        }
        
        return null;
    }
    
    /**
     * Rectangle vs Rectangle collision detection.
     */
    private Collision rectangleVsRectangle(Entity entityA, Entity entityB,
                                         ColliderComponent colliderA, Vector2f posA,
                                         ColliderComponent colliderB, Vector2f posB) {
        float[] boundsA = colliderA.getBounds(posA);
        float[] boundsB = colliderB.getBounds(posB);
        
        float overlapX = Math.min(boundsA[2], boundsB[2]) - Math.max(boundsA[0], boundsB[0]);
        float overlapY = Math.min(boundsA[3], boundsB[3]) - Math.max(boundsA[1], boundsB[1]);
        
        if (overlapX > 0 && overlapY > 0) {
            Vector2f normal;
            float penetration;
            
            if (overlapX < overlapY) {
                // Horizontal collision
                normal = posA.x < posB.x ? new Vector2f(-1, 0) : new Vector2f(1, 0);
                penetration = overlapX;
            } else {
                // Vertical collision
                normal = posA.y < posB.y ? new Vector2f(0, -1) : new Vector2f(0, 1);
                penetration = overlapY;
            }
            
            Vector2f contactPoint = new Vector2f(
                (Math.max(boundsA[0], boundsB[0]) + Math.min(boundsA[2], boundsB[2])) / 2,
                (Math.max(boundsA[1], boundsB[1]) + Math.min(boundsA[3], boundsB[3])) / 2
            );
            
            boolean isTrigger = colliderA.isTrigger || colliderB.isTrigger;
            return new Collision(entityA, entityB, contactPoint, normal, penetration, isTrigger);
        }
        
        return null;
    }
    
    /**
     * Circle vs Rectangle collision detection.
     */
    private Collision circleVsRectangle(Entity entityA, Entity entityB,
                                       ColliderComponent colliderA, Vector2f posA,
                                       ColliderComponent colliderB, Vector2f posB) {
        float[] boundsB = colliderB.getBounds(posB);
        
        // Find closest point on rectangle to circle center
        float closestX = Math.max(boundsB[0], Math.min(posA.x, boundsB[2]));
        float closestY = Math.max(boundsB[1], Math.min(posA.y, boundsB[3]));
        
        Vector2f closest = new Vector2f(closestX, closestY);
        Vector2f direction = new Vector2f(posA).subtract(closest);
        float distance = direction.length();
        
        if (distance < colliderA.radius) {
            float penetration = colliderA.radius - distance;
            Vector2f normal = distance > 0 ? direction.normalize() : new Vector2f(0, 1);
            Vector2f contactPoint = new Vector2f(closest);
            
            boolean isTrigger = colliderA.isTrigger || colliderB.isTrigger;
            return new Collision(entityA, entityB, contactPoint, normal, penetration, isTrigger);
        }
        
        return null;
    }
    
    /**
     * Resolve a collision by separating the entities and applying physics response.
     */
    private void resolveCollision(Collision collision) {
        PhysicsComponent physicsA = collision.entityA.get(PhysicsComponent.class);
        PhysicsComponent physicsB = collision.entityB.get(PhysicsComponent.class);
        TransformComponent transformA = collision.entityA.get(TransformComponent.class);
        TransformComponent transformB = collision.entityB.get(TransformComponent.class);
        
        // Separate the entities
        float totalMass = 0;
        if (physicsA != null && !physicsA.isStatic) totalMass += physicsA.mass;
        if (physicsB != null && !physicsB.isStatic) totalMass += physicsB.mass;
        
        if (totalMass > 0) {
            Vector2f separation = new Vector2f(collision.normal).multiply(collision.penetration);
            
            if (physicsA != null && !physicsA.isStatic) {
                float ratioA = physicsB != null && !physicsB.isStatic ? physicsB.mass / totalMass : 1.0f;
                transformA.position.subtract(new Vector2f(separation).multiply(ratioA));
            }
            
            if (physicsB != null && !physicsB.isStatic) {
                float ratioB = physicsA != null && !physicsA.isStatic ? physicsA.mass / totalMass : 1.0f;
                transformB.position.add(new Vector2f(separation).multiply(ratioB));
            }
        }
        
        // Apply collision response (velocity changes)
        if (physicsA != null && physicsB != null && totalMass > 0) {
            Vector2f relativeVelocity = new Vector2f(physicsB.velocity).subtract(physicsA.velocity);
            float velocityAlongNormal = relativeVelocity.dot(collision.normal);
            
            if (velocityAlongNormal > 0) {
                return; // Objects are separating
            }
            
            float restitution = Math.min(physicsA.restitution, physicsB.restitution);
            float impulseScalar = -(1 + restitution) * velocityAlongNormal;
            impulseScalar /= (1 / physicsA.mass + 1 / physicsB.mass);
            
            Vector2f impulse = new Vector2f(collision.normal).multiply(impulseScalar);
            
            if (!physicsA.isStatic) {
                physicsA.velocity.subtract(new Vector2f(impulse).multiply(1 / physicsA.mass));
            }
            if (!physicsB.isStatic) {
                physicsB.velocity.add(new Vector2f(impulse).multiply(1 / physicsB.mass));
            }
        }
    }
    
    /**
     * Get all current collisions.
     */
    public List<Collision> getCurrentCollisions() {
        return new ArrayList<>(currentCollisions);
    }
    
    /**
     * Check if two specific entities are colliding.
     */
    public boolean areColliding(Entity entityA, Entity entityB) {
        for (Collision collision : currentCollisions) {
            if ((collision.entityA.getId() == entityA.getId() && collision.entityB.getId() == entityB.getId()) ||
                (collision.entityA.getId() == entityB.getId() && collision.entityB.getId() == entityA.getId())) {
                return true;
            }
        }
        return false;
    }
}