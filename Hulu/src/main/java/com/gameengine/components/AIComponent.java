package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameLogic;
import com.gameengine.math.Vector2;
import com.gameengine.physics.AABB;
import com.gameengine.scene.Scene;

import java.util.List;

/**
 * AI组件，处理敌人的AI行为（如自动射击、寻找目标等）
 */
public class AIComponent extends Component<AIComponent> {
    private float currentCooldown; // 当前冷却时间
    private float fireInterval; // 射击间隔时间
    private String targetName; // 目标对象名称
    
    public AIComponent() {
        this.currentCooldown = 0;
        this.fireInterval = 2.0f;
        this.targetName = "Player";
    }
    
    public AIComponent(float fireInterval, String targetName) {
        this();
        this.fireInterval = fireInterval;
        this.targetName = targetName;
    }
    
    @Override
    public void initialize() {
        // AI组件初始化
    }
    
    @Override
    public void update(float deltaTime) {
        if (!enabled || owner == null) return;
        
        currentCooldown -= deltaTime;
        
        // 自动射击逻辑
        if (currentCooldown <= 0) {
            shootAtTarget();
            currentCooldown = fireInterval + (float)(Math.random() * 1.0f); // 随机冷却
        }
    }
    
    @Override
    public void render() {
        // AI组件不直接渲染
    }
    
    /**
     * 向目标射击
     */
    private void shootAtTarget() {
        Scene scene = owner.getScene();
        if (scene == null) return;
        
        // 查找目标
        List<GameObject> targets = scene.findGameObjectsByName(targetName);
        if (targets.isEmpty()) return;
        
        GameObject target = targets.get(0);
        TransformComponent targetTransform = target.getComponent(TransformComponent.class);
        TransformComponent enemyTransform = owner.getComponent(TransformComponent.class);
        
        if (targetTransform == null || enemyTransform == null) return;
        
        // 计算射击方向
        Vector2 enemyPos = enemyTransform.getPosition();
        Vector2 targetPos = targetTransform.getPosition();
        Vector2 direction = targetPos.subtract(enemyPos).normalize();
        
        // 创建子弹
        Bullet bullet = new Bullet("EnemyBullet", owner, direction, enemyPos);
        scene.addGameObject(bullet);
        
        // 注册子弹到物理系统
        GameLogic gameLogic = scene.getGameLogic();
        if (gameLogic != null) {
            gameLogic.getPhysicsWorld().registerPhysicsBody(
                bullet,
                new AABB(enemyPos, new Vector2(8, 8)),
                2 // 子弹层
            );
        }
    }
    
    /**
     * 设置射击间隔
     */
    public void setFireInterval(float interval) {
        this.fireInterval = interval;
    }
    
    /**
     * 设置目标名称
     */
    public void setTargetName(String name) {
        this.targetName = name;
    }
    
    public float getFireInterval() {
        return fireInterval;
    }
    
    public String getTargetName() {
        return targetName;
    }
}

