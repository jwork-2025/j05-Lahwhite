package com.gameengine.core;

import com.gameengine.graphics.GPURenderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import com.gameengine.graphics.IRenderer;
import com.gameengine.recording.RecordingService;
import com.gameengine.recording.RecordingConfig;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 游戏引擎
 */
public class GameEngine {
    // 单例实例（静态私有）
    private static GameEngine instance;

    private IRenderer renderer;              // 渲染器
    private InputManager inputManager;      // 输入管理器
    private Scene currentScene;             // 当前场景
    private boolean running;                // 引擎运行状态
    private float targetFPS;                // 目标帧率
    private float deltaTime;                // 时间间隔
    private long lastTime;                  // 上一帧时间
    @SuppressWarnings("unused")
    private String title;                   // 窗口标题
    private RecordingService recordingService; // 录像服务
    
    private GameEngine(int width, int height, String title) {
        this.title = title;
        this.renderer = new GPURenderer(width, height, title);
        this.inputManager = InputManager.getInstance();
        this.running = false;
        this.targetFPS = 60.0f;
        this.deltaTime = 0.0f;
        this.lastTime = System.nanoTime();
        
        // 初始化录像服务
        initializeRecordingService(width, height);
    }
    
    

    /**
     * 初始化录像服务
     */
    private void initializeRecordingService(int width, int height) {
        try {
            // 创建带时间戳的输出路径
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String outputPath = "recording_" + timestamp + ".jsonl";
            
            System.out.println("录像服务初始化成功，输出路径: " + outputPath);
        } catch (Exception e) {
            System.err.println("录像服务初始化失败: " + e.getMessage());
            recordingService = null;
        }
    }
    
    /**
     * 开始录像
     */
    public void startRecording() {
        if (recordingService != null && currentScene != null) {
            try {
                recordingService.start(currentScene, renderer.getWidth(), renderer.getHeight());
                System.out.println("开始录像...");
            } catch (IOException e) {
                System.err.println("开始录像失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 停止录像
     */
    public void stopRecording() {
        if (recordingService != null) {
            recordingService.stop();
            System.out.println("停止录像，文件已保存");
        }
    }
    
    public void enableRecording(com.gameengine.recording.RecordingService service) {
        this.recordingService = service;
        try {
            if (service != null && currentScene != null) {
                service.start(currentScene, renderer.getWidth(), renderer.getHeight());
            }
        } catch (Exception e) {
            System.err.println("录制启动失败: " + e.getMessage());
        }
    }

    /**
     * 初始化游戏引擎
     */
    public boolean initialize() {
        return true; // GPURenderer在构造函数中自动初始化
    }
    
    /**
     * 运行游戏引擎
     */
    public void run() {
        if (!initialize()) {
            System.err.println("游戏引擎初始化失败");
            return;
        }
        
        running = true;
        
        // 初始化当前场景
        if (currentScene != null) {
            currentScene.initialize();
        }
        
        // 游戏主循环
        while (running && !renderer.shouldClose()) {
            long currentTime = System.nanoTime();
            deltaTime = (currentTime - lastTime) / 1_000_000_000.0f;
            lastTime = currentTime;
            
            // 限制deltaTime防止大跳跃
            if (deltaTime > 0.1f) {
                deltaTime = 0.1f;
            }
            
            update();
            render();
            renderer.pollEvents();
            
            // 检查窗口是否关闭
            if (renderer.shouldClose()) {
                running = false;
            }
            
            // 简单的帧率控制
            try {
                Thread.sleep(16); // 约60FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        cleanup();
    }
    
    /**
     * 更新游戏逻辑
     */
    private void update() {
        // 更新场景
        if (currentScene != null) {
            currentScene.update(deltaTime);
        }
        
        // 更新录像数据
        if (recordingService != null && recordingService.isRecording() && currentScene != null) {
            recordingService.update(deltaTime, currentScene, inputManager);
        }
        
        // 更新输入
        inputManager.update();
        
        // 检查退出条件
        if (inputManager.isKeyPressed(27)) { // ESC键
            running = false;
            // 注意：cleanup将在run方法结束时统一调用
        }
    }
    
    /**
     * 渲染游戏
     */
    private void render() {
        renderer.beginFrame();
        // 渲染场景
        if (currentScene != null) {
            currentScene.render();
        }
        renderer.endFrame();
    }
    
    /**
     * 设置当前场景
     */
    public void setScene(Scene scene) {
        // 如果当前正在录像，先停止录像
        if (recordingService != null && recordingService.isRecording()) {
            stopRecording();
        }
        
        this.currentScene = scene;
        if (scene != null) {
            scene.setEngine(this); 
            if (running) {
                scene.initialize();
            }
        }
    }
    
    /**
     * 获取当前场景
     */
    public Scene getCurrentScene() {
        return currentScene;
    }
    
    /**
     * 停止游戏引擎
     */
    public void stop() {
        running = false;
    }
    
    // 移除重复的私有cleanup方法
    
    /**
     * 获取渲染器
     */
    public IRenderer getRenderer() {
        return renderer;
    }
    
    /**
     * 获取输入管理器
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * 获取时间间隔
     */
    public float getDeltaTime() {
        return deltaTime;
    }
    
    /**
     * 设置目标帧率
     */
    public void setTargetFPS(float fps) {
        this.targetFPS = fps;
        // 简单帧率控制通过run方法中的sleep实现，无需额外的timer
    }
    
    /**
     * 获取目标帧率
     */
    public float getTargetFPS() {
        return targetFPS;
    }

    /**
     * 单例获取方法（确保全局唯一实例）
     * 首次调用时需传入初始化参数，后续调用可忽略参数
     */
    public static synchronized GameEngine getInstance(int width, int height, String title) {
        if (instance == null) {
            instance = new GameEngine(width, height, title);
        }
        return instance;
    }

    /*
     * 重载方法：允许在实例已创建后直接获取（无需参数）
     */
    public static GameEngine getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GameEngine尚未初始化，请先调用带参数的getInstance方法");
        }
        return instance;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        running = false;
        
        // 停止录像
        if (recordingService != null && recordingService.isRecording()) {
            stopRecording();
        }
        
        if (currentScene != null) {
            currentScene.clear();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
    }
    
    /**
     * 检查引擎是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 获取录像服务实例
     */
    public RecordingService getRecordingService() {
        return recordingService;
    }

}
