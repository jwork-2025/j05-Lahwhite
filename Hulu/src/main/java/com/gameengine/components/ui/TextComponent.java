// TextComponent.java
package com.gameengine.components.ui;

import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import java.awt.*;

public class TextComponent extends UIComponent<TextComponent> {
    private String text;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private int fontSize = 12;
    private boolean centered = false;
    private IRenderer renderer;

    public TextComponent(Vector2 position, String text, IRenderer renderer) {
        super(position, new Vector2(0, 0));  // 大小自动计算
        this.text = text;
        this.renderer = renderer;
    }

    @Override
    public void update(float deltaTime) {}

    @Override
    public void render() {
        if (!visible) return;
        
        // 注意：IRenderer接口的drawText方法不支持居中参数
        // 对于居中对齐，我们暂时保持位置不变，实际项目中可能需要更复杂的计算
        float drawX = position.x;
        float drawY = position.y;
        
        renderer.drawText(
            drawX, 
            drawY, 
            text, 
            color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f
        );
    }

    // Getters & Setters
    public void setText(String text) { this.text = text; }
    public void setColor(Color color) { this.color = color; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public void setCentered(boolean centered) { this.centered = centered; }
}