// ButtonComponent.java
package com.gameengine.components.ui;

import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import java.awt.*;

public class ButtonComponent extends UIComponent<ButtonComponent> {
    private String text;
    private Color normalColor = new Color(0.2f, 0.2f, 0.8f, 1.0f);
    private Color hoverColor = new Color(0.3f, 0.3f, 0.9f, 1.0f);
    private Color pressColor = new Color(0.1f, 0.1f, 0.7f, 1.0f);
    private Color textColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private Runnable onClick;
    private boolean isPressed = false;
    private IRenderer renderer;

    public ButtonComponent(Vector2 position, Vector2 size, String text, IRenderer renderer) {
        super(position, size);
        this.text = text;
        this.renderer = renderer;
    }

    // 修正后
    @Override
    public void update(float deltaTime) {
        InputManager input = InputManager.getInstance();
        Vector2 mousePos = input.getMousePosition();
        boolean isClicked = input.isMouseButtonPressed(0); // 左键点击
    
        handleMouseEvent(mousePos, isClicked);
        input.updatePreviousMouseState(); // 记录上一帧状态（需在InputManager中实现）
    }

    @Override
    public void render() {
        if (!visible) return;
        
        // 根据状态选择颜色
        Color currentColor = isPressed ? pressColor : (isHovered ? hoverColor : normalColor);
        
        // 绘制按钮背景
        renderer.drawRect(
            position.x, position.y, 
            size.x, size.y, 
            currentColor.getRed() / 255.0f, currentColor.getGreen() / 255.0f, currentColor.getBlue() / 255.0f, currentColor.getAlpha() / 255.0f
        );
        
        // 绘制按钮文本
        // 注意：IRenderer接口的drawText方法从(x, y)坐标开始向右下方渲染文本
        // 为了实现居中，需要计算文本的左上角位置而不是中心位置
        float textWidth = text.length() * 19.2f; // 估算文本宽度（每个字符约19.2像素，基于字体大小32和0.6的宽度比例）
        float textHeight = 32.0f; // 字体大小
        float textX = position.x + (size.x - textWidth) / 2;
        float textY = position.y + (size.y - textHeight) / 2;
        
        renderer.drawText(
            textX, 
            textY, 
            text, 
            textColor.getRed() / 255.0f, textColor.getGreen() / 255.0f, textColor.getBlue() / 255.0f, textColor.getAlpha() / 255.0f
        );
    }

    @Override
    protected void onMouseClick() {
        if (onClick != null) {
            onClick.run();
        }
    }

    @Override
    protected void onMouseEnter() {
        isPressed = false;
    }

    @Override
    protected void onMouseExit() {
        isPressed = false;
    }

    // 设置点击回调
    public void setOnClickListener(Runnable onClick) {
        this.onClick = onClick;
    }

    // Getters & Setters
    public void setText(String text) { this.text = text; }
    public void setNormalColor(Color color) { this.normalColor = color; }
}