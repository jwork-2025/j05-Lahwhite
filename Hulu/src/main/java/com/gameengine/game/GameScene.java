package com.gameengine.game;

import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.Random;

/**
 * 游戏主场景类，负责管理游戏对象和场景逻辑
 */
public class GameScene extends Scene {
    private IRenderer renderer;
    private Random random;
    private float enemySpawnTimer;
    private static final float ENEMY_SPAWN_INTERVAL = 2.0f;
    
    public GameScene() {
        super("GameScene");
        this.random = new Random();
        this.enemySpawnTimer = 0;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        GameEngine engine = getEngine();
        if (engine == null) {
            throw new IllegalStateException("Scene must be set to engine before initialization");
        }
        
        this.renderer = engine.getRenderer();
        
        // 创建游戏逻辑
        GameLogic gameLogic = new GameLogic(this);
        setGameLogic(gameLogic);
        
        // 创建游戏对象
        createPlayer();
        createInitialEnemies();
        
        // 初始化物理体
        gameLogic.initializePhysicsBodies();
        
        // 开始录像
        engine.startRecording();
        System.out.println("游戏开始，录像已启动");
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // 获取游戏逻辑
        GameLogic gameLogic = getGameLogic();
        if (gameLogic != null) {
            // 检查游戏是否结束
            if (!gameLogic.isGameOver()) {
                // 游戏未结束，正常更新游戏逻辑
                gameLogic.handlePlayerInput();
                gameLogic.updatePhysics(deltaTime);
                gameLogic.checkCollisions();
                
                // 生成新敌人
                enemySpawnTimer += deltaTime;
                if (enemySpawnTimer >= ENEMY_SPAWN_INTERVAL) {
                    createEnemy();
                    enemySpawnTimer = 0;
                }
            } else {
                // System.out.println("游戏结束状态，检查输入...");
                // 游戏结束状态，检查输入
                gameLogic.checkGameOverInput();
                
                // 停止录像（确保只停止一次）
                staticStopRecordingOnce(this);
            }
        }
    }
    
    /**
     * 静态方法，确保每个场景只停止录像一次
     */
    private static void staticStopRecordingOnce(GameScene scene) {
        // 使用场景的唯一标识作为键，确保每个场景只停止一次录像
        GameEngine engine = scene.getEngine();
        if (engine != null && engine.getRecordingService() != null && engine.getRecordingService().isRecording()) {
            engine.stopRecording();
        }
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 渲染所有对象
        super.render();
        
        // 检查游戏是否结束
        GameLogic gameLogic = getGameLogic();
        if (gameLogic != null && gameLogic.isGameOver()) {
            // 渲染游戏结束界面
            gameLogic.renderGameOver(renderer);
        }
    }
    
    /**
     * 创建玩家
     */
    private void createPlayer() {
        Player player = new Player(new Vector2(200, 150), renderer);
        addGameObject(player);
    }
    
    /**
     * 创建初始敌人
     */
    private void createInitialEnemies() {
        for (int i = 0; i < 3; i++) {
            createEnemy();
        }
    }
    
    /**
     * 创建敌人
     */
    private void createEnemy() {
        // 随机位置
        Vector2 position = new Vector2(
            random.nextFloat() * 800,
            random.nextFloat() * 600
        );
        
        Enemy enemy = new Enemy(position, renderer);
        addGameObject(enemy);
        
        // 注册物理体
        GameLogic gameLogic = getGameLogic();
        if (gameLogic != null) {
            enemy.registerPhysics(this);
        }
    }
}
