package com.gameengine.components;

import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;

// 移除生命周期相关变量和逻辑
public class Bullet extends GameObject {
    private GameObject shooter; // 仅保留发射者记录
    
    public Bullet(String name, GameObject shooter, Vector2 direction, Vector2 position) {
        super(name);
        this.shooter = shooter;
        
        // 添加变换组件
        TransformComponent transform = addComponent(new TransformComponent(position));
        
        // 添加渲染组件（蓝色小矩形）
        RenderComponent render = addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(8, 8),
            new RenderComponent.Color(0.2f, 0.2f, 1.0f, 1.0f)
        ));
        
        // 添加物理组件
        PhysicsComponent physics = addComponent(new PhysicsComponent(0.1f));
        physics.setUseGravity(false); // 子弹不受重力影响
        physics.setVelocity(direction.normalize().multiply(100)); // 子弹速度
        physics.setFriction(1.0f); // 关键：取消摩擦力（避免速度衰减）
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // 移除生命周期检查逻辑，不再自动销毁
    }
    
    public GameObject getShooter() {
        return shooter;
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}