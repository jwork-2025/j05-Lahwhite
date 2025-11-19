package com.gameengine.input;

import com.gameengine.math.Vector2;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 输入管理器，处理键盘和鼠标输入
 */
public class InputManager {
    // 键位常量定义
    public static final int KEY_UP = 265;    // GLFW_KEY_UP
    public static final int KEY_DOWN = 264;  // GLFW_KEY_DOWN
    public static final int KEY_ENTER = 257; // GLFW_KEY_ENTER
    public static final int KEY_ESCAPE = 256; // GLFW_KEY_ESCAPE
    
    private static InputManager instance;
    private Set<Integer> pressedKeys;
    private Set<Integer> justPressedKeys;
    private Map<Integer, Boolean> keyStates;
    private Vector2 mousePosition;
    private boolean[] mouseButtons;
    private boolean[] mouseButtonsJustPressed;
    private boolean[] prevMouseButtons; // 新增：保存上一帧鼠标状态
    
    private InputManager() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
        keyStates = new HashMap<>();
        mousePosition = new Vector2();
        mouseButtons = new boolean[3]; // 左键、右键、中键
        mouseButtonsJustPressed = new boolean[3];
        prevMouseButtons = new boolean[3]; // 初始化上一帧鼠标状态数组
    }
    
    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }
    
    /**
     * 更新输入状态
     */
    public void update() {
        justPressedKeys.clear();
        for (int i = 0; i < mouseButtonsJustPressed.length; i++) {
            mouseButtonsJustPressed[i] = false;
        }
    }
    
    /**
     * 处理键盘按下事件
     */
    public void onKeyPressed(int keyCode) {
        if (!pressedKeys.contains(keyCode)) {
            justPressedKeys.add(keyCode);
        }
        pressedKeys.add(keyCode);
        keyStates.put(keyCode, true);
    }
    
    /**
     * 处理键盘释放事件
     */
    public void onKeyReleased(int keyCode) {
        pressedKeys.remove(keyCode);
        keyStates.put(keyCode, false);
    }
    
    /**
     * 处理鼠标移动事件
     */
    public void onMouseMoved(float x, float y) {
        mousePosition.x = x;
        mousePosition.y = y;
    }
    
    /**
     * 处理鼠标按下事件
     */
    public void onMousePressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            if (!mouseButtons[button]) {
                mouseButtonsJustPressed[button] = true;
            }
            mouseButtons[button] = true;
        }
    }
    
    /**
     * 处理鼠标释放事件
     */
    public void onMouseReleased(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            mouseButtons[button] = false;
        }
    }
    
    /**
     * 检查按键是否被按下
     */
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    /**
     * 检查按键是否刚刚被按下（只在这一帧为true）
     */
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }
    
    /**
     * 检查鼠标按键是否被按下
     */
    public boolean isMouseButtonPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button];
        }
        return false;
    }
    
    /**
     * 检查鼠标按键是否刚刚被按下
     */
    public boolean isMouseButtonJustPressed(int button) {
        if (button >= 0 && button < mouseButtons.length) {
            return mouseButtons[button] && !prevMouseButtons[button];
        }
        return false;
    }

    /*
     * 检测上一帧是否有按键按下
     */
    public boolean isAnyKeyJustPressed() {
        return !justPressedKeys.isEmpty();
    }

    /*
     * 检测是否有按键按下
     */
    public boolean isAnyKeyPressed() {
        return !pressedKeys.isEmpty();
    }
    
    /*
     *  获取所有刚刚被按下的按键快照（仅在这一帧为true）
     */
    public Set<Integer> getJustPressedKeysSnapshot() {
        return new HashSet<>(justPressedKeys);
    }

    /**
     * 保存当前鼠标状态到上一帧（在每一帧结束时调用）
     */
    public void updatePreviousMouseState() {
        // 复制当前鼠标按钮状态到上一帧数组
        System.arraycopy(mouseButtons, 0, prevMouseButtons, 0, mouseButtons.length);
    }
    
    /**
     * 获取鼠标位置
     */
    public Vector2 getMousePosition() {
        return new Vector2(mousePosition);
    }
    
    /**
     * 获取鼠标X坐标
     */
    public float getMouseX() {
        return mousePosition.x;
    }
    
    /**
     * 获取鼠标Y坐标
     */
    public float getMouseY() {
        return mousePosition.y;
    }
    
    /**
     * 消费按键事件（用于防止重复触发）
     */
    public void consumeKey(int keyCode) {
        justPressedKeys.remove(keyCode);
    }
}