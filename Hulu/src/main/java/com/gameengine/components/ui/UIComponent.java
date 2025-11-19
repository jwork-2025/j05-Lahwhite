// UIComponent.java
package com.gameengine.components.ui;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;

import java.awt.*;

public abstract class UIComponent<T extends UIComponent<T>> extends Component<T> {
    protected Vector2 position;  // 屏幕坐标(像素)
    protected Vector2 size;      // 组件大小
    protected boolean visible = true;
    protected int zIndex = 100;  // UI层级(确保在游戏对象之上)
    protected boolean isHovered = false;
    protected boolean isFocused = false;

    public UIComponent(Vector2 position, Vector2 size) {
        this.position = position;
        this.size = size;
    }

    @Override
    public void initialize() {}

    /**
     * 检查点是否在组件范围内(用于鼠标检测)
     */
    public boolean containsPoint(Vector2 point) {
        return point.x >= position.x && 
               point.x <= position.x + size.x && 
               point.y >= position.y && 
               point.y <= position.y + size.y;
    }

    /**
     * 处理鼠标事件
     */
    public void handleMouseEvent(Vector2 mousePos, boolean isClicked) {
        boolean wasHovered = isHovered;
        isHovered = containsPoint(mousePos);
        
        // 鼠标进入事件
        if (!wasHovered && isHovered) {
            onMouseEnter();
        }
        
        // 鼠标离开事件
        if (wasHovered && !isHovered) {
            onMouseExit();
        }
        
        // 点击事件
        if (isHovered && isClicked) {
            onMouseClick();
        }
    }

    // 事件回调(子类实现)
    protected void onMouseEnter() {}
    protected void onMouseExit() {}
    protected void onMouseClick() {}

    // Getters & Setters
    public Vector2 getPosition() { return position; }
    public void setPosition(Vector2 position) { this.position = position; }
    public Vector2 getSize() { return size; }
    public void setSize(Vector2 size) { this.size = size; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public int getZIndex() { return zIndex; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }
    public boolean isHovered() { return isHovered; }
    public boolean isFocused() { return isFocused; }
    public void setFocused(boolean focused) { isFocused = focused; }
}