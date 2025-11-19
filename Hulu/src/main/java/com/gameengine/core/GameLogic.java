package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.Bullet;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.physics.AABB;
import com.gameengine.physics.PhysicsWorld;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;

// 多线程相关
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// 渲染相关
import com.gameengine.graphics.IRenderer;

/**
 * 游戏逻辑类，处理具体的游戏规则
 */
public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    private PhysicsWorld physicsWorld; // 新增物理世界
    
    private ExecutorService aiThreadPool; // 敌人AI线程池
    
    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        this.physicsWorld = new PhysicsWorld(scene); // 初始化物理世界
        // 创建物理执行器，线程数为 (CPU 可用核心数 - 1)，但至少2个线程
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.aiThreadPool = Executors.newFixedThreadPool(threadCount);
    }
    
    /**
     * 处理玩家输入
     */
    public void handlePlayerInput() {
        // 修正后
        List<GameObject> players = scene.findGameObjectsByName("Player"); 
        if (players.isEmpty()) return;
        
        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
        
        if (transform == null || physics == null) return;
        
        Vector2 movement = new Vector2();
        
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }
        
        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }
        
        // 边界检查
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 20) pos.x = 800 - 20;
        if (pos.y > 600 - 20) pos.y = 600 - 20;
        transform.setPosition(pos);
    }
    
    /**
     * 初始化时注册所有物理体（在Scene.initialize()中调用）
     */
    public void initializePhysicsBodies() {
        // 注册玩家
        List<GameObject> players = scene.findGameObjectsByName("Player");
        if (!players.isEmpty()) {
            GameObject player = players.get(0);
            physicsWorld.registerPhysicsBody(
                player,
                new AABB(player.getComponent(TransformComponent.class).getPosition(), new Vector2(20, 20)),
                0 // 玩家层
            );
        }

        // 注册敌人
        for (GameObject enemy : scene.getGameObjects()) {
            if (enemy.getName().equals("Enemy")) {
                physicsWorld.registerPhysicsBody(
                    enemy,
                    new AABB(enemy.getComponent(TransformComponent.class).getPosition(), new Vector2(20, 20)),
                    1 // 敌人层
                );
            }
        }
    }
    
    /**
     * 更新物理系统
     */
    public void updatePhysics(float deltaTime) {
        physicsWorld.update(deltaTime);
    }
    
    /**
     * 检查碰撞
     */
    public void checkCollisions() {
        // 获取玩家和所有敌人
        List<GameObject> players = scene.findGameObjectsByName("Player"); 
        if (players.isEmpty()) return;
        GameObject player = players.get(0);
        List<GameObject> enemies = new ArrayList<>();
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Enemy")) {
                enemies.add(obj);
            }
        }
        
        if (enemies.isEmpty()) {
            checkBulletCollisions();
            return;
        }
        
        // 多线程检查玩家与敌人的碰撞
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, enemies.size() / threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // 将敌人列表分批处理
        for (int i = 0; i < enemies.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, enemies.size());
            final List<GameObject> enemyBatch = enemies.subList(start, end);
            
            Future<Boolean> future = aiThreadPool.submit(() -> {
                for (GameObject enemy : enemyBatch) {
                    if (checkPlayerEnemyCollision(player, enemy)) {
                        return true; // 发现碰撞
                    }
                }
                return false; // 未发现碰撞
            });
            futures.add(future);
        }
        
        // 等待所有任务完成，检查是否有碰撞
        for (Future<Boolean> future : futures) {
            try {
                if (future.get(100, TimeUnit.MILLISECONDS)) {
                    // 发现碰撞，取消剩余任务
                    for (Future<Boolean> f : futures) {
                        f.cancel(true);
                    }
                    break;
                }
            } catch (Exception e) {
                // 忽略异常，继续处理
            }
        }

        // 检查子弹与角色的碰撞
        checkBulletCollisions();
    }

    /**
     * 检查玩家与单个敌人的碰撞
     * @param player 玩家对象
     * @param enemy 敌人对象
     * @return 是否发生碰撞
     */
    private boolean checkPlayerEnemyCollision(GameObject player, GameObject enemy) {
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
        
        if (playerTransform == null || enemyTransform == null) {
            return false;
        }
        
        AABB playerAabb = new AABB(
            playerTransform.getPosition(), 
            new Vector2(20, 20) // 玩家碰撞体大小
        );
        
        AABB enemyAabb = new AABB(
            enemyTransform.getPosition(), 
            new Vector2(20, 20) // 敌人碰撞体大小
        );
        
        if (playerAabb.collidesWith(enemyAabb)) {
            // 碰撞发生，重置玩家位置到中心
            playerTransform.setPosition(new Vector2(400, 300));
            // 重置玩家速度
            PhysicsComponent playerPhysics = player.getComponent(PhysicsComponent.class);
            if (playerPhysics != null) {
                playerPhysics.setVelocity(0, 0);
            }
            return true;
        }
        
        return false;
    }

    /**
     * 检查子弹与角色的碰撞（多线程版本）
     */
    private void checkBulletCollisions() {
        List<GameObject> bullets = scene.findGameObjectsByNameContaining("Bullet");
        List<GameObject> characters = new ArrayList<>();
        characters.addAll(scene.findGameObjectsByName("Player"));
        characters.addAll(scene.findGameObjectsByName("Enemy"));
    
        if (bullets.isEmpty() || characters.isEmpty()) {
            return;
        }
        
        // 多线程检查子弹与角色的碰撞
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, bullets.size() / threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        // 将子弹列表分批处理
        for (int i = 0; i < bullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, bullets.size());
            final List<GameObject> bulletBatch = bullets.subList(start, end);
            final List<GameObject> characterList = new ArrayList<>(characters); // 复制列表避免并发修改
            
            Future<Boolean> future = aiThreadPool.submit(() -> {
                for (GameObject bulletObj : bulletBatch) {
                    if (!(bulletObj instanceof Bullet)) continue;
                    Bullet bullet = (Bullet) bulletObj;
                    
                    for (GameObject character : characterList) {
                        if (checkBulletCharacterCollision(bullet, character)) {
                            return true; // 发现碰撞
                        }
                    }
                }
                return false; // 未发现碰撞
            });
            futures.add(future);
        }
        
        // 等待所有任务完成
        for (Future<Boolean> future : futures) {
            try {
                future.get(100, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // 忽略异常，继续处理
            }
        }
    }

    /**
     * 检查子弹与单个角色的碰撞
     * @param bullet 子弹对象
     * @param character 角色对象
     * @return 是否发生碰撞
     */
    private boolean checkBulletCharacterCollision(Bullet bullet, GameObject character) {
        // 跳过发射者自己
        if (character == bullet.getShooter()) {
            return false;
        }
        
        // 获取子弹碰撞体
        TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
        if (bulletTransform == null) {
            return false;
        }
        
        AABB bulletAabb = new AABB(
            bulletTransform.getPosition(), 
            new Vector2(8, 8)
        );
        
        // 检查碰撞
        TransformComponent charTransform = character.getComponent(TransformComponent.class);
        if (charTransform == null) {
            return false;
        }
        
        AABB charAabb = new AABB(
            charTransform.getPosition(), 
            new Vector2(20, 20)
        );
        
        if (bulletAabb.collidesWith(charAabb)) {
            // 销毁子弹
            bullet.destroy();
            
            // 如果是玩家碰到子弹，设置游戏结束状态但不销毁玩家
            if (character.getName().equals("Player")) {
                System.out.println("玩家确实碰到子弹了！");
                gameOver();
                // 停止玩家移动
                PhysicsComponent playerPhysics = character.getComponent(PhysicsComponent.class);
                if (playerPhysics != null) {
                    playerPhysics.setVelocity(0, 0);
                }
            } else {
                // 敌人死亡
                character.destroy();
                System.out.println("敌人确实碰到子弹了！");
                // 敌人死亡，增加分数等逻辑 
            }
            
            return true;
        }
        
        return false;
    }

    private boolean gameOverState = false;
    
    // 检查游戏是否结束
    public boolean isGameOver() {
        return gameOverState;
    }

    // 游戏结束处理
    private void gameOver() {
        System.out.println("游戏结束！");
        gameOverState = true;
        // 不停止引擎，保持游戏运行以显示结束界面
        // 可以添加游戏结束画面等逻辑
    }
    
    public void renderGameOver(IRenderer renderer) {
        if (!gameOverState) return;
        
        // 绘制游戏结束文本
        renderer.drawText(
            400, 300, 
            "游戏结束！", 
            1.0f, 0.5f, 0.5f, 1.0f
        );
        
        // 绘制提示文本
        renderer.drawText(
            400, 330, 
            "按回车键返回主菜单", 
            1.0f, 1.0f, 1.0f, 1.0f
        );
    }

    /**
     * 检查游戏结束状态下的按键输入
     */
    public void checkGameOverInput() {
        // System.out.println("成功进入 checkGameOverInput 方法");
        // 检查是否按下回车键（使用GLFW键码，对应值为257）
        if (gameOverState && inputManager.isKeyPressed(257)) {
            try {
                System.out.println("检测到回车键按下，正在返回主菜单...");
                // 加载主菜单场景
                Class<?> mainMenuSceneClass = Class.forName("com.gameengine.game.MainMenuScene");
                Scene mainMenuScene = (Scene) mainMenuSceneClass.getDeclaredConstructor().newInstance();
                // 重置游戏结束状态
                gameOverState = false;
                // 设置主菜单场景
                scene.getEngine().setScene(mainMenuScene);
            } catch (Exception e) {
                System.out.println("返回主菜单时出错：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 获取物理世界引用（供组件使用）
    public PhysicsWorld getPhysicsWorld() {
        return physicsWorld;
    }
}
