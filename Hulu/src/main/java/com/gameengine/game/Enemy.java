package com.gameengine.game;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.physics.AABB;
import com.gameengine.scene.Scene;

import java.util.Random;

/**
 * 敌人角色类
 */
public class Enemy extends GameObject {
    private IRenderer renderer;
    
    public Enemy(Vector2 position, IRenderer renderer) {
        super("Enemy");
        this.renderer = renderer;
        
        // 添加变换组件
        TransformComponent transform = addComponent(new TransformComponent(position));
        
        // 添加渲染组件 - 橙色矩形
        RenderComponent render = addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(20, 20),
            new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)  // 橙色
        ));
        render.setRenderer(renderer);
        
        // 添加物理组件
        Random random = new Random();
        PhysicsComponent physics = addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2(
            (random.nextFloat() - 0.5f) * 100,
            (random.nextFloat() - 0.5f) * 100
        ));
        physics.setFriction(0.98f);
        
        // 添加AI组件 - 自动射击玩家
        addComponent(new AIComponent(2.0f, "Player"));
    }
    
    /**
     * 注册物理体到物理世界
     */
    public void registerPhysics(Scene scene) {
        GameLogic gameLogic = scene.getGameLogic();
        if (gameLogic == null) return;
        
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null) return;
        
        gameLogic.getPhysicsWorld().registerPhysicsBody(
            this,
            new AABB(transform.getPosition(), new Vector2(20, 20)),
            1 // 敌人层
        );
    }
}

