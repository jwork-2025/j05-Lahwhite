package com.gameengine.game;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.physics.AABB;
import com.gameengine.scene.Scene;

/**
 * 玩家角色类
 */
public class Player extends GameObject {
    private IRenderer renderer;
    private Vector2 basePosition;
    private float fireCooldown = 0.5f;
    private float currentCooldown = 0;
    
    public Player(Vector2 position, IRenderer renderer) {
        super("Player");
        this.renderer = renderer;
        this.basePosition = new Vector2(position);
        
        // 添加变换组件
        addComponent(new TransformComponent(position));
        
        // 添加物理组件
        PhysicsComponent physics = addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.90f);
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        updateComponents(deltaTime);
        
        // 更新所有部位的位置
        updateBodyParts();
        
        // 处理射击输入（空格键发射）
        currentCooldown -= deltaTime;
        InputManager input = InputManager.getInstance();
        if (input.isKeyPressed(32) && currentCooldown <= 0) { // 空格键
            shoot();
            currentCooldown = fireCooldown;
        }
    }
    
    @Override
    public void render() {
        renderBodyParts();
    }
    
    private void updateBodyParts() {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            basePosition = transform.getPosition();
        }
    }
    
    private void renderBodyParts() {
        if (basePosition == null || renderer == null) return;
        
        // 渲染身体
        renderer.drawRect(
            basePosition.x - 8, basePosition.y - 10, 16, 20,
            1.0f, 0.0f, 0.0f, 1.0f  // 红色
        );
        
        // 渲染头部
        renderer.drawRect(
            basePosition.x - 6, basePosition.y - 22, 12, 12,
            1.0f, 0.5f, 0.0f, 1.0f  // 橙色
        );
        
        // 渲染左臂
        renderer.drawRect(
            basePosition.x - 13, basePosition.y - 5, 6, 12,
            1.0f, 0.8f, 0.0f, 1.0f  // 黄色
        );
        
        // 渲染右臂
        renderer.drawRect(
            basePosition.x + 7, basePosition.y - 5, 6, 12,
            0.0f, 1.0f, 0.0f, 1.0f  // 绿色
        );
    }
    
    private void shoot() {
        Vector2 direction = new Vector2(0, -1); // 向上发射
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null) return;
        
        // 在玩家位置前方生成子弹
        Vector2 bulletPos = new Vector2(
            transform.getPosition().x,
            transform.getPosition().y - 20
        );
        
        // 创建子弹并添加到场景
        Scene scene = getScene();
        if (scene == null) return;
        
        Bullet bullet = new Bullet("PlayerBullet", this, direction, bulletPos);
        scene.addGameObject(bullet);
        
        // 注册子弹到物理系统（碰撞层设为2）
        GameLogic gameLogic = scene.getGameLogic();
        if (gameLogic != null) {
            gameLogic.getPhysicsWorld().registerPhysicsBody(
                bullet,
                new AABB(bulletPos, new Vector2(8, 8)),
                2 // 子弹层
            );
        }
    }
}

