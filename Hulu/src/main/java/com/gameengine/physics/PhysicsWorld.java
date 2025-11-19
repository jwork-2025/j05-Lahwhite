package com.gameengine.physics;

import java.util.Iterator;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;

public class PhysicsWorld {
    private Scene scene;
    private Vector2 globalGravity; // 全局重力（替代每个组件单独设置）
    private float timeScale = 1.0f; // 时间缩放（用于慢动作）
    private List<PhysicsBody> physicsBodies = new ArrayList<>(); // 物理体列表

    // 物理体：关联游戏对象、物理组件、碰撞体
    private static class PhysicsBody {
        GameObject gameObject;
        PhysicsComponent physics;
        TransformComponent transform;
        CollisionShape collisionShape;
        int collisionLayer; // 碰撞层（用于过滤）

        public PhysicsBody(GameObject obj, PhysicsComponent physics, 
                          TransformComponent transform, CollisionShape shape, int layer) {
            this.gameObject = obj;
            this.physics = physics;
            this.transform = transform;
            this.collisionShape = shape;
            this.collisionLayer = layer;
        }
    }

    public PhysicsWorld(Scene scene) {
        this.scene = scene;
        this.globalGravity = new Vector2(0, 9.8f * 100); // 放大重力效果（像素/秒²）
    }

    // 注册物理体（在场景初始化时调用）
    public void registerPhysicsBody(GameObject obj, CollisionShape shape, int collisionLayer) {
        PhysicsComponent physics = obj.getComponent(PhysicsComponent.class);
        TransformComponent transform = obj.getComponent(TransformComponent.class);
        if (physics != null && transform != null) {
            physicsBodies.add(new PhysicsBody(obj, physics, transform, shape, collisionLayer));
        }
    }

    // 物理更新主逻辑（替代GameLogic.updatePhysics）
    public void update(float deltaTime) {
        deltaTime *= timeScale; // 应用时间缩放

        // 1. 应用全局重力
        applyGravity();

        // 2. 更新所有物体的运动状态
        updateMotions(deltaTime);

        // 3. 检测并处理碰撞
        detectAndResolveCollisions();

        // 4. 边界检查（替代原GameLogic中的边界逻辑）
        checkBoundaries();
    }

    // 应用全局重力（覆盖组件自身的重力设置）
    private void applyGravity() {
        for (PhysicsBody body : physicsBodies) {
            if (body.physics.isUseGravity()) {
                body.physics.applyForce(globalGravity.multiply(body.physics.getMass()));
            }
        }
    }

    // 更新运动状态（速度、位置）
    private void updateMotions(float deltaTime) {
        for (PhysicsBody body : physicsBodies) {
            // 调用PhysicsComponent的更新逻辑（保持组件自身的速度/加速度处理）
            body.physics.update(deltaTime);
            // 同步碰撞体位置（与TransformComponent保持一致）
            body.collisionShape.updatePosition(body.transform.getPosition());
        }
    }

    // 碰撞检测与响应
    private void detectAndResolveCollisions() {
        // 遍历所有可能的碰撞对（优化：后续可加入空间分区减少检测量）
        for (int i = 0; i < physicsBodies.size(); i++) {
            PhysicsBody a = physicsBodies.get(i);
            for (int j = i + 1; j < physicsBodies.size(); j++) {
                PhysicsBody b = physicsBodies.get(j);

                // 碰撞层过滤（不检测无需碰撞的层）
                if (!shouldCollide(a.collisionLayer, b.collisionLayer)) {
                    continue;
                }

                // 检测碰撞
                if (a.collisionShape.collidesWith(b.collisionShape)) {
                    // 1. 分离穿透物体
                    Vector2 separation = a.collisionShape.getSeparatingAxis(b.collisionShape);
                    resolvePenetration(a, b, separation);

                    // 2. 计算碰撞响应（动量守恒）
                    resolveCollisionResponse(a, b, separation);
                }
            }
        }
    }

    // 解决物体穿透
    private void resolvePenetration(PhysicsBody a, PhysicsBody b, Vector2 separation) {
        // 根据质量分配分离距离（质量大的物体移动少）
        float totalMass = a.physics.getMass() + b.physics.getMass();
        float ratioA = b.physics.getMass() / totalMass;
        float ratioB = a.physics.getMass() / totalMass;

        a.transform.translate(separation.multiply(-ratioA));
        b.transform.translate(separation.multiply(ratioB));
    }

    // 碰撞响应（动量守恒+恢复系数）
    private void resolveCollisionResponse(PhysicsBody a, PhysicsBody b, Vector2 separation) {
        Vector2 velA = a.physics.getVelocity();
        Vector2 velB = b.physics.getVelocity();
        float massA = a.physics.getMass();
        float massB = b.physics.getMass();
        float e = 0.8f; // 恢复系数（0为完全非弹性，1为完全弹性）

        // 修正后
        // 1. 获取碰撞法线（从分离轴获取，假设separation为碰撞方向向量）
        Vector2 normal = separation.normalize();
        Vector2 tangent = new Vector2(-normal.y, normal.x); // 切线方向

        // 2. 分解速度到法线和切线方向
        float velAN = velA.dot(normal);
        float velAT = velA.dot(tangent);
        float velBN = velB.dot(normal);
        float velBT = velB.dot(tangent);

        // 3. 计算法线方向新速度（动量守恒）
        float newVelAN = (massA * velAN + massB * velBN - massB * e * (velAN - velBN)) / (massA + massB);
        float newVelBN = (massA * velAN + massB * velBN - massA * e * (velBN - velAN)) / (massA + massB);

        // 4. 合成新速度（切线方向速度不变）
        Vector2 newVelA = normal.multiply(newVelAN).add(tangent.multiply(velAT));
        Vector2 newVelB = normal.multiply(newVelBN).add(tangent.multiply(velBT));

        a.physics.setVelocity(newVelA);
        b.physics.setVelocity(newVelB);
    }

    // 边界检查（场景边缘碰撞）
    private void checkBoundaries() {
        float sceneWidth = 800;
        float sceneHeight = 600;

        for (Iterator<PhysicsBody> iterator = physicsBodies.iterator(); iterator.hasNext(); ) {
            PhysicsBody body = iterator.next();
            Vector2 pos = body.transform.getPosition();
            Vector2 vel = body.physics.getVelocity();
        
            AABB aabb = (AABB) body.collisionShape;
            float width = aabb.getHalfExtents().x * 2;
            float height = aabb.getHalfExtents().y * 2;

            // 判断是否是子弹（碰撞层为2）
            boolean isBullet = body.collisionLayer == 2;

            // 边界检查逻辑
            boolean outOfBounds = false;
            if (pos.x <= 0 || pos.x >= sceneWidth - width) {
                outOfBounds = true;
                if (!isBullet) { // 非子弹才反弹
                    vel.x = -vel.x * 0.8f;
                    body.physics.setVelocity(vel);
                }
            }

            if (pos.y <= 0 || pos.y >= sceneHeight - height) {
                outOfBounds = true;
                if (!isBullet) { // 非子弹才反弹
                    vel.y = -vel.y * 0.8f;
                    body.physics.setVelocity(vel);
                }
            }

            // 子弹触边则销毁
            if (isBullet && outOfBounds) {
                body.gameObject.destroy(); // 销毁子弹游戏对象
                iterator.remove(); // 从物理世界移除
                continue;
            }

            // 非子弹物体限制在场景内（保持原逻辑）
            if (!isBullet) {
                pos.x = Math.max(0, Math.min(pos.x, sceneWidth - width));
                pos.y = Math.max(0, Math.min(pos.y, sceneHeight - height));
                body.transform.setPosition(pos);
            }
        }
    }

    // 碰撞层过滤规则（示例：0=玩家，1=敌人，2=平台，玩家与平台碰撞，敌人与平台碰撞）
    private boolean shouldCollide(int layerA, int layerB) {
        if (layerA == 0 && layerB == 2) return true; // 玩家碰平台
        if (layerA == 1 && layerB == 2) return true; // 敌人碰平台
        if (layerA == 0 && layerB == 1) return true; // 玩家碰敌人
        return false;
    }

    // Getters & Setters
    public void setGlobalGravity(Vector2 gravity) {
        this.globalGravity = gravity;
    }

    public void setTimeScale(float scale) {
        this.timeScale = Math.max(0, scale);
    }

    // 获取当前全局重力（供组件使用）
    public Vector2 getGlobalGravity() {
        return globalGravity;
    }
}