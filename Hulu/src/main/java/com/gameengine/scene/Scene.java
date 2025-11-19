package com.gameengine.scene;

import com.gameengine.core.GameObject;
import com.gameengine.core.Component;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;

// 移除具体游戏逻辑的import
import java.util.*;
import java.util.stream.Collectors;

/**
 * 场景类，管理游戏对象和组件
 */
public class Scene {
    private String name;
    private List<GameObject> gameObjects;
    private List<GameObject> objectsToAdd;
    private List<GameObject> objectsToRemove;
    private boolean initialized;
    // 移除未使用的组件索引
    private GameEngine engine; // 新增：存储所属引擎
    private GameLogic gameLogic; // 新增：存储关联的游戏逻辑
    
    public Scene(String name) {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.objectsToAdd = new ArrayList<>();
        this.objectsToRemove = new ArrayList<>();
        this.initialized = false;
        // 移除组件索引初始化
        this.engine = null; // 初始化引擎引用为null
        this.gameLogic = null; // 初始化游戏逻辑为null
    }
    
    /**
     * 初始化场景
     */
    public void initialize() {
        for (GameObject obj : gameObjects) {
            obj.initialize();
        }
        initialized = true;
    }
    
    /**
     * 更新场景
     */
    public void update(float deltaTime) {
        // 添加新对象
        for (GameObject obj : objectsToAdd) {
            gameObjects.add(obj);
            if (initialized) {
                obj.initialize();
            }
        }
        objectsToAdd.clear();
        
        // 移除标记的对象
        for (GameObject obj : objectsToRemove) {
            gameObjects.remove(obj);
        }
        objectsToRemove.clear();
        
        // 更新所有活跃的游戏对象
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isActive()) {
                obj.update(deltaTime);
            } else {
                iterator.remove();
            }
        }
    }
    
    /**
     * 渲染场景
     */
    public void render() {
        for (GameObject obj : gameObjects) {
            if (obj.isActive()) {
                obj.render();
            }
        }
    }
    
    /**
     * 添加游戏对象到场景
     */
    public void addGameObject(GameObject gameObject) {
        objectsToAdd.add(gameObject);
        gameObject.setScene(this); // 关键：将当前场景设置给游戏对象
    }
    
    /**
     * 根据组件类型查找游戏对象
     */
    public <T extends Component<T>> List<GameObject> findGameObjectsByComponent(Class<T> componentType) {
        return gameObjects.stream()
            .filter(obj -> obj.hasComponent(componentType))
            .collect(Collectors.toList());
    }

    /**
     * 根据对象名字查找游戏对象
     */
    public List<GameObject> findGameObjectsByName(String name) {
        List<GameObject> result = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (name.equals(obj.getName())) {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * 根据包含指定子串的名称查找游戏对象
     */
    public List<GameObject> findGameObjectsByNameContaining(String substring) {
        List<GameObject> result = new ArrayList<>();
        for (GameObject obj : gameObjects) {
            if (obj.getName() != null && obj.getName().contains(substring)) {
                result.add(obj);
            }
        }
        return result;
    }
    
    /**
     * 获取所有具有指定组件的游戏对象
     */
    public <T extends Component<T>> List<T> getComponents(Class<T> componentType) {
        return findGameObjectsByComponent(componentType).stream()
            .map(obj -> obj.getComponent(componentType))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * 清空场景
     */
    public void clear() {
        gameObjects.clear();
        objectsToAdd.clear();
        objectsToRemove.clear();
    }
    
    /**
     * 获取场景名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取所有游戏对象
     */
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }

    // 新增：设置所属引擎
    public void setEngine(GameEngine engine) {
        this.engine = engine;
    }

    // 新增：获取所属引擎
    public GameEngine getEngine() {
        return engine;
    }

    // 新增：设置关联的游戏逻辑
    public void setGameLogic(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    // 新增：获取关联的游戏逻辑
    public GameLogic getGameLogic() {
        return gameLogic;
    }
}