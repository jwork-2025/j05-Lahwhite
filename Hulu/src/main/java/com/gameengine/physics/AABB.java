// AABB.java（轴对齐矩形碰撞体）
package com.gameengine.physics;

import com.gameengine.math.Vector2;

public class AABB implements CollisionShape {
    private Vector2 min;
    private Vector2 max;

    public AABB(Vector2 position, Vector2 size) {
        this.min = new Vector2(position);
        this.max = new Vector2(position).add(size);
    }

    @Override
    public void updatePosition(Vector2 position) {
        Vector2 size = max.subtract(min);
        this.min = new Vector2(position);
        this.max = new Vector2(position).add(size);
    }

    @Override
    public boolean collidesWith(CollisionShape other) {
        if (other instanceof AABB) {
            AABB otherAabb = (AABB) other;
            return min.x <= otherAabb.max.x && 
                   max.x >= otherAabb.min.x && 
                   min.y <= otherAabb.max.y && 
                   max.y >= otherAabb.min.y;
        }
        // 可扩展圆形、OBB等碰撞检测
        return false;
    }

    // AABB.java 中修正
    @Override
    public Vector2 getSeparatingAxis(CollisionShape other) {
        if (other instanceof AABB) {
            AABB otherAabb = (AABB) other;
            // 计算X轴和Y轴的重叠量（穿透深度）
            float overlapX = Math.min(max.x, otherAabb.max.x) - Math.max(min.x, otherAabb.min.x);
            float overlapY = Math.min(max.y, otherAabb.max.y) - Math.max(min.y, otherAabb.min.y);
        
            // 确保只在碰撞时返回分离向量（重叠量为正）
            if (overlapX <= 0 || overlapY <= 0) {
                return new Vector2(0, 0); // 无碰撞，返回零向量
            }
        
            // 选择重叠量小的轴作为分离轴，方向为从当前AABB指向另一个
            if (overlapX < overlapY) {
                // X轴分离：当前AABB在左侧则向右分离，反之向左
                float direction = (min.x < otherAabb.min.x) ? 1 : -1;
                return new Vector2(overlapX * direction, 0);
            } else {
                // Y轴分离：当前AABB在下方则向上分离，反之向下
                float direction = (min.y < otherAabb.min.y) ? 1 : -1;
                return new Vector2(0, overlapY * direction);
            }
        }
        return new Vector2(0, 0);
    }

    /**
     * 获取碰撞体的半尺寸（从中心到边缘的距离）
     */
    public Vector2 getHalfExtents() {
        Vector2 size = max.subtract(min); // 计算完整尺寸（宽和高）
        return size.multiply(0.5f); // 返回半尺寸
    }

    /**
     * 获取碰撞体的完整尺寸
     */
    public Vector2 getSize() {
        return max.subtract(min);
    }
}