package com.gameengine.game;

import java.io.File;

import com.gameengine.components.ui.ButtonComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

/**
 * 主菜单场景类，包含游戏标题和功能按钮
 */
public class MainMenuScene extends Scene {
    private IRenderer renderer;
    
    public MainMenuScene() {
        super("MainMenuScene");
    }
    
    @Override
    public void initialize() {
        super.initialize();
        
        GameEngine engine = getEngine();
        if (engine == null) {
            throw new IllegalStateException("Scene must be set to engine before initialization");
        }
        
        this.renderer = engine.getRenderer();
        
        createTitle();
        createButtons();
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 渲染所有对象
        super.render();
    }
    
    /**
     * 创建游戏标题
     */
    private void createTitle() {
        GameObject titleObject = new GameObject("GameTitle");
        addGameObject(titleObject);
        
        // 添加标题渲染组件
        titleObject.addComponent(new TitleComponent(new Vector2(400, 150), renderer));
    }
    
    /**
     * 创建功能按钮
     */
    private void createButtons() {
        GameEngine engine = getEngine();
        
        // 创建回放按钮
        GameObject replayButtonObj = new GameObject("ReplayButton");
        ButtonComponent replayButton = new ButtonComponent(
                new Vector2(400 - 100, 250), 
                new Vector2(200, 50), 
                "Replay Game", 
                renderer
        );
        replayButton.setOnClickListener(() -> {
            // 切换到回放场景
            Scene replay = new ReplayScene(engine,null);
            engine.setScene(replay);
        });
        replayButtonObj.addComponent(replayButton);
        addGameObject(replayButtonObj);
        
        // 创建开始游戏按钮
        GameObject startButtonObj = new GameObject("StartButton");
        ButtonComponent startButton = new ButtonComponent(
                new Vector2(400 - 100, 320), 
                new Vector2(200, 50), 
                "Game Start", 
                renderer
        );
        startButton.setOnClickListener(() -> {
            // 切换到游戏场景
            engine.setScene(new GameScene());
            // 移植的记录功能
            new File("recordings").mkdirs();
            String path = "recordings/session_" + System.currentTimeMillis() + ".jsonl";
            RecordingConfig cfg = new RecordingConfig(path);
            RecordingService svc = new RecordingService(cfg);
            engine.enableRecording(svc);
        });
        startButtonObj.addComponent(startButton);
        addGameObject(startButtonObj);
        
        // 创建退出按钮
        GameObject exitButtonObj = new GameObject("ExitButton");
        ButtonComponent exitButton = new ButtonComponent(
                new Vector2(400 - 100, 390), 
                new Vector2(200, 50), 
                "Exit", 
                renderer
        );
        exitButton.setOnClickListener(() -> {
            // 退出游戏
            System.exit(0);
        });
        exitButtonObj.addComponent(exitButton);
        addGameObject(exitButtonObj);
    }
    
    /**
     * 标题渲染组件
     */
    private class TitleComponent extends ButtonComponent {
        public TitleComponent(Vector2 position, IRenderer renderer) {
            super(position, new Vector2(400, 80), "葫芦娃大作战", renderer);
        }
        
        @Override
        public void render() {
            if (!visible) return;
            
            // 绘制标题文本
            // 注意：IRenderer接口的drawText方法不支持居中参数
            float centerX = position.x + size.x/2;
            float centerY = position.y + size.y/2;
            
            renderer.drawText(
                    centerX, 
                    centerY, 
                    "Black Myth : Huluwa",
                    1.0f, 0.8f, 0.2f, 1.0f // 金色文本
            );
        }
        
        @Override
        public void handleMouseEvent(Vector2 mousePos, boolean isClicked) {
            // 标题不响应鼠标事件
        }
    }
}