// CollisionShape.java（碰撞体接口）
package com.gameengine.physics;

import com.gameengine.math.Vector2;

public interface CollisionShape {
    // 检测与另一个碰撞体是否碰撞
    boolean collidesWith(CollisionShape other);
    // 获取碰撞后的分离向量（用于解决穿透）
    Vector2 getSeparatingAxis(CollisionShape other);
    // 更新碰撞体位置（与TransformComponent同步）
    void updatePosition(Vector2 position);
}