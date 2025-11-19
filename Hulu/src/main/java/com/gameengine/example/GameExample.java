package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.game.MainMenuScene;

/**
 * 游戏入口类，负责启动游戏引擎和场景
 */
public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");
        
        try {
            // 创建游戏引擎
            GameEngine engine = GameEngine.getInstance(800, 600, "葫芦娃大作战");
            
            // 创建主菜单场景
            MainMenuScene mainMenuScene = new MainMenuScene();
            
            // 设置场景
            engine.setScene(mainMenuScene);
            
            // 运行游戏
            engine.run();
            
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("游戏结束");
    }
}
