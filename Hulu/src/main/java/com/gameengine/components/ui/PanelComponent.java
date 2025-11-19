// PanelComponent.java
package com.gameengine.components.ui;

import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.graphics.IRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PanelComponent extends UIComponent<PanelComponent> {
    private List<GameObject> childObjects = new ArrayList<>();
    private LayoutType layoutType = LayoutType.FREE;
    private Vector2 padding = new Vector2(5, 5);
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.1f, 0.8f);
    private Color borderColor = new Color(0.3f, 0.3f, 0.3f, 1.0f);
    private int borderWidth = 1;
    private IRenderer renderer;

    // 布局类型枚举
    public enum LayoutType {
        FREE,       // 自由布局
        VERTICAL,   // 垂直布局
        HORIZONTAL  // 水平布局
    }

    public PanelComponent(Vector2 position, Vector2 size, IRenderer renderer) {
        super(position, size);
        this.renderer = renderer;
    }

    @Override
    public void initialize() {
        // 初始化所有子对象
        for (GameObject child : childObjects) {
            child.initialize();
        }
    }

    @Override
    public void update(float deltaTime) {
        // 更新所有子对象
        for (GameObject child : childObjects) {
            child.update(deltaTime);
        }
        
        // 根据布局类型调整子对象位置
        if (layoutType != LayoutType.FREE) {
            arrangeChildren();
        }
    }

    @Override
    public void render() {
        if (!visible) return;
        
        // 绘制面板背景
        renderer.drawRect(
            position.x, position.y,
            size.x, size.y,
            backgroundColor.getRed() / 255f, backgroundColor.getGreen() / 255f, backgroundColor.getBlue() / 255f, backgroundColor.getAlpha() / 255f
        );
        
        // 绘制边框
        if (borderWidth > 0) {
            renderer.drawRect(
                position.x, position.y,
                size.x, borderWidth,
                borderColor.getRed() / 255f, borderColor.getGreen() / 255f, borderColor.getBlue() / 255f, borderColor.getAlpha() / 255f
            );
            renderer.drawRect(
                position.x, position.y + size.y - borderWidth,
                size.x, borderWidth,
                borderColor.getRed() / 255f, borderColor.getGreen() / 255f, borderColor.getBlue() / 255f, borderColor.getAlpha() / 255f
            );
            renderer.drawRect(
                position.x, position.y,
                borderWidth, size.y,
                borderColor.getRed() / 255f, borderColor.getGreen() / 255f, borderColor.getBlue() / 255f, borderColor.getAlpha() / 255f
            );
            renderer.drawRect(
                position.x + size.x - borderWidth, position.y,
                borderWidth, size.y,
                borderColor.getRed() / 255f, borderColor.getGreen() / 255f, borderColor.getBlue() / 255f, borderColor.getAlpha() / 255f
            );
        }
        
        // 渲染所有子对象
        for (GameObject child : childObjects) {
            child.render();
        }
    }

    @Override
    public void handleMouseEvent(Vector2 mousePos, boolean isClicked) {
        super.handleMouseEvent(mousePos, isClicked);
        
        // 将鼠标事件传递给子组件（坐标转换为面板局部坐标）
        if (isHovered) {
            Vector2 localMousePos = new Vector2(
                mousePos.x - position.x,
                mousePos.y - position.y
            );
            
            for (GameObject child : childObjects) {
                @SuppressWarnings("unchecked") // maybe
                UIComponent<?> uiComponent = child.getComponent(UIComponent.class);
                if (uiComponent != null && uiComponent.isVisible()) {
                    uiComponent.handleMouseEvent(localMousePos, isClicked);
                }
            }
        }
    }

    /**
     * 添加子对象到面板
     */
    public void addChild(GameObject child) {
        // 获取子对象的UI组件
        @SuppressWarnings("unchecked") // maybe
        UIComponent<?> uiComponent = child.getComponent(UIComponent.class);
        if (uiComponent != null) {
            childObjects.add(child);
            // 设置子对象的Z轴索引比面板高，确保在面板上方渲染
            uiComponent.setZIndex(this.zIndex + 1);
        }
    }

    /**
     * 移除子对象
     */
    public void removeChild(GameObject child) {
        childObjects.remove(child);
    }

    /**
     * 排列子对象（根据布局类型）
     */
    private void arrangeChildren() {
        float currentX = padding.x;
        float currentY = padding.y;
        float maxWidth = 0;
        float maxHeight = 0;

        for (GameObject child : childObjects) {
            @SuppressWarnings("unchecked") // maybe
            UIComponent<?> uiComponent = child.getComponent(UIComponent.class);
            if (uiComponent == null) continue;

            Vector2 childSize = uiComponent.getSize();
            
            // 设置子对象位置
            if (layoutType == LayoutType.VERTICAL) {
                uiComponent.setPosition(new Vector2(currentX, currentY));
                currentY += childSize.y + padding.y;
                maxWidth = Math.max(maxWidth, childSize.x);
            } else if (layoutType == LayoutType.HORIZONTAL) {
                uiComponent.setPosition(new Vector2(currentX, currentY));
                currentX += childSize.x + padding.x;
                maxHeight = Math.max(maxHeight, childSize.y);
            }
        }
    }

    // Getters & Setters
    public void setLayoutType(LayoutType layoutType) {
        this.layoutType = layoutType;
    }

    public void setPadding(Vector2 padding) {
        this.padding = padding;
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
    }

    public void setBorderWidth(int width) {
        this.borderWidth = width;
    }

    public List<GameObject> getChildren() {
        return new ArrayList<>(childObjects);
    }
}