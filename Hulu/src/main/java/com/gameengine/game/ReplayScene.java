package com.gameengine.game;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.game.EntityFactory;
import com.gameengine.recording.RecordingStorage;


import java.io.File;
import java.util.*;

public class ReplayScene extends Scene {
    private final GameEngine engine;
    private String recordingPath;
    private IRenderer renderer;
    private InputManager input;
    private float time;
    private boolean DEBUG_REPLAY = false;
    private float debugAccumulator = 0f;
    private RecordingStorage storage;

    private static class Keyframe {
        static class EntityInfo {
            Vector2 pos;
            String rt; // RECTANGLE/CIRCLE/LINE/CUSTOM/null
            float w, h;
            float r=0.9f,g=0.9f,b=0.2f,a=1.0f; // 默认颜色
            String id;
            String uniqueId; // 唯一标识符，用于正确匹配敌人对象
        }
        double t;
        java.util.List<EntityInfo> entities = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final java.util.List<GameObject> objectList = new ArrayList<>();

    // 如果 path 为 null，则先展示 recordings 目录下的文件列表，供用户选择
    public ReplayScene(GameEngine engine, String path) {
        super("Replay");
        this.engine = engine;
        this.recordingPath = path;
        this.storage = new com.gameengine.recording.FileRecordingStorage();
        // 只有在提供有效路径时才初始化，否则等待用户选择
        if (path != null && !path.isEmpty()) {
            initialize();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        // 确保storage被初始化
        if (this.storage == null) {
            this.storage = new com.gameengine.recording.FileRecordingStorage();
        }
        // 重置状态，防止从列表进入后残留
        this.time = 0f;
        this.keyframes.clear();
        this.objectList.clear();
        if (recordingPath != null && !recordingPath.isEmpty()) {
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
            
        } else {
            // 仅进入文件选择模式
            this.recordingFiles = null;
            this.selectedIndex = 0;
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // 如果没有设置录像路径，显示文件选择界面并处理按键
        if (recordingPath == null || recordingPath.isEmpty()) {
            handleFileSelection();
            return;
        }
        
        // 播放模式下的按键处理 - 使用与文件选择界面相同的双重检测方式
        if ((input.isKeyPressed(27) || input.isKeyJustPressed(27) || 
             input.isKeyPressed(InputManager.KEY_ESCAPE) || input.isKeyJustPressed(InputManager.KEY_ESCAPE)) || 
            (input.isKeyPressed(8) || input.isKeyJustPressed(8))) { // ESC/BACK
            // 消费按键事件
            input.consumeKey(27);
            input.consumeKey(InputManager.KEY_ESCAPE);
            input.consumeKey(8);
            engine.setScene(new MainMenuScene());
            return;
        }

        if (keyframes.size() < 1) return;
        time += deltaTime;
        // 限制在最后关键帧处停止（也可选择循环播放）
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) {
            time = (float)lastT;
        }

        // 查找区间
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) { a = k1; b = k2; break; }
        }
        double span = Math.max(1e-6, b.t - a.t);
        double u = Math.min(1.0, Math.max(0.0, (time - a.t) / span));
        // 调试输出节流
        

        updateInterpolatedPositions(a, b, (float)u);
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.06f, 0.06f, 0.08f, 1.0f);
        if (recordingPath == null || recordingPath.isEmpty()) {
            renderFileList();
            return;
        }
        // 基于 Transform 手动绘制（回放对象没有附带 RenderComponent）
        super.render();
        String hint = "REPLAY: ESC to return";
        float w = hint.length() * 12.0f;
        renderer.drawText(renderer.getWidth()/2.0f - w/2.0f, 30, hint, 0.8f, 0.8f, 0.8f, 1.0f);
    }

    private void loadRecording(String path) {
        keyframes.clear();
        try {
            for (String line : storage.readLines(path)) {
                if (line.contains("\"type\":\"keyframe\"")) {
                    Keyframe kf = new Keyframe();
                    kf.t = com.gameengine.recording.RecordingJson.parseDouble(com.gameengine.recording.RecordingJson.field(line, "t"));
                    // 解析 entities 列表中的若干 {"id":"name","x":num,"y":num}
                    int idx = line.indexOf("\"entities\":[");
                    if (idx >= 0) {
                        int bracket = line.indexOf('[', idx);
                        String arr = bracket >= 0 ? com.gameengine.recording.RecordingJson.extractArray(line, bracket) : "";
                        String[] parts = com.gameengine.recording.RecordingJson.splitTopLevel(arr);
                        for (String p : parts) {
                            Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                            ei.id = com.gameengine.recording.RecordingJson.stripQuotes(com.gameengine.recording.RecordingJson.field(p, "id"));
                            // 解析uniqueId字段
                            ei.uniqueId = com.gameengine.recording.RecordingJson.stripQuotes(com.gameengine.recording.RecordingJson.field(p, "uniqueId"));
                            double x = com.gameengine.recording.RecordingJson.parseDouble(com.gameengine.recording.RecordingJson.field(p, "x"));
                            double y = com.gameengine.recording.RecordingJson.parseDouble(com.gameengine.recording.RecordingJson.field(p, "y"));
                            ei.pos = new Vector2((float)x, (float)y);
                            String rt = com.gameengine.recording.RecordingJson.stripQuotes(com.gameengine.recording.RecordingJson.field(p, "rt"));
                            ei.rt = rt;
                            ei.w = (float)com.gameengine.recording.RecordingJson.parseDouble(com.gameengine.recording.RecordingJson.field(p, "w"));
                            ei.h = (float)com.gameengine.recording.RecordingJson.parseDouble(com.gameengine.recording.RecordingJson.field(p, "h"));
                            String colorArr = com.gameengine.recording.RecordingJson.field(p, "color");
                            if (colorArr != null && colorArr.startsWith("[")) {
                                String c = colorArr.substring(1, Math.max(1, colorArr.indexOf(']', 1)));
                                String[] cs = c.split(",");
                                if (cs.length >= 3) {
                                    try {
                                        ei.r = Float.parseFloat(cs[0].trim());
                                        ei.g = Float.parseFloat(cs[1].trim());
                                        ei.b = Float.parseFloat(cs[2].trim());
                                        if (cs.length >= 4) ei.a = Float.parseFloat(cs[3].trim());
                                    } catch (Exception ignored) {}
                                }
                            }
                            kf.entities.add(ei);
                        }
                    }
                    keyframes.add(kf);
                }
            }
        } catch (Exception e) {
            
        }
        keyframes.sort(Comparator.comparingDouble(k -> k.t));
    }

    private void buildObjectsFromFirstKeyframe() {
        if (keyframes.isEmpty()) return;
        Keyframe kf0 = keyframes.get(0);
        // 按实体构建对象（使用预制），实现与游戏内一致外观
        objectList.clear();
        clear();
        for (int i = 0; i < kf0.entities.size(); i++) {
            Keyframe.EntityInfo ei = kf0.entities.get(i);
            GameObject obj = buildObjectFromEntity(ei, i);
            
            // 如果实体有uniqueId，我们可以在对象的名称中包含它，以便调试
            if (ei.uniqueId != null && !ei.uniqueId.isEmpty()) {
                // 为敌人对象保留唯一标识信息，便于调试和识别
                if (ei.id != null && (ei.id.startsWith("Enemy") || ei.id.startsWith("AIPlayer"))) {
                    // 不修改obj的name（可能会影响渲染逻辑），但我们通过映射来跟踪
                }
            }
            
            addGameObject(obj);
            objectList.add(obj);
        }
        time = 0f;
    }

    private void ensureObjectCount(int n) {
        while (objectList.size() < n) {
            GameObject obj = new GameObject("RObj#" + objectList.size());
            obj.addComponent(new TransformComponent(new Vector2(0, 0)));
            // 为回放对象添加可渲染组件（默认外观，稍后在 refreshRenderFromKeyframe 应用真实外观）
            addGameObject(obj);
            objectList.add(obj);
        }
        while (objectList.size() > n) {
            GameObject obj = objectList.remove(objectList.size() - 1);
            obj.setActive(false);
        }
    }

    
    private void updateInterpolatedPositions(Keyframe a, Keyframe b, float u) {
        // 确保有足够的对象
        int maxEntities = Math.max(a.entities.size(), b.entities.size());
        ensureObjectCount(maxEntities);
        
        // 使用Map来存储实体信息，便于快速查找匹配的实体
        Map<String, Keyframe.EntityInfo> bEntityMap = new HashMap<>();
        
        // 为b关键帧中的每个实体创建映射键
        for (Keyframe.EntityInfo ei : b.entities) {
            // 优先使用uniqueId（特别是对于敌人对象），没有则使用id
            String key = (ei.uniqueId != null && !ei.uniqueId.isEmpty()) ? 
                        (ei.id + "#" + ei.uniqueId) : 
                        ei.id;
            bEntityMap.put(key, ei);
        }
        
        // 更新每个对象的位置
        for (int i = 0; i < a.entities.size(); i++) {
            Keyframe.EntityInfo aEi = a.entities.get(i);
            String key = (aEi.uniqueId != null && !aEi.uniqueId.isEmpty()) ? 
                        (aEi.id + "#" + aEi.uniqueId) : 
                        aEi.id;
            
            // 查找b关键帧中匹配的实体
            Keyframe.EntityInfo bEi = bEntityMap.get(key);
            
            if (bEi != null && i < objectList.size()) {
                // 如果找到匹配的实体，进行插值计算
                Vector2 pa = aEi.pos;
                Vector2 pb = bEi.pos;
                float x = (float)((1.0 - u) * pa.x + u * pb.x);
                float y = (float)((1.0 - u) * pa.y + u * pb.y);
                GameObject obj = objectList.get(i);
                TransformComponent tc = obj.getComponent(TransformComponent.class);
                if (tc != null) tc.setPosition(new Vector2(x, y));
            }
        }
    
    }

    private GameObject buildObjectFromEntity(Keyframe.EntityInfo ei, int index) {
        GameObject obj;
        
        // 根据实体类型创建相应的对象
        if ("Player".equalsIgnoreCase(ei.id)) {
            obj = com.gameengine.game.EntityFactory.createPlayerVisual(renderer);
        } else if (ei.id != null && ei.id.startsWith("Enemy")) {
            // 敌人对象，使用矩形渲染
            float w2 = (ei.w > 0 ? ei.w : 20);
            float h2 = (ei.h > 0 ? ei.h : 20);
            obj = com.gameengine.game.EntityFactory.createAIVisual(renderer, w2, h2, ei.r, ei.g, ei.b, ei.a);
            // 为敌人对象设置名称，如果有uniqueId则在名称中包含部分信息以便调试
            if (ei.uniqueId != null && !ei.uniqueId.isEmpty()) {
                // 只使用uniqueId的前8个字符，避免名称过长
                String shortId = ei.uniqueId.substring(0, Math.min(8, ei.uniqueId.length()));
                obj.setName(ei.id + "_" + shortId);
            } else {
                obj.setName(ei.id);
            }
        } else if ("AIPlayer".equalsIgnoreCase(ei.id)) {
            float w2 = (ei.w > 0 ? ei.w : 20);
            float h2 = (ei.h > 0 ? ei.h : 20);
            obj = com.gameengine.game.EntityFactory.createAIVisual(renderer, w2, h2, ei.r, ei.g, ei.b, ei.a);
            // 为AIPlayer对象也设置唯一标识信息
            if (ei.uniqueId != null && !ei.uniqueId.isEmpty()) {
                String shortId = ei.uniqueId.substring(0, Math.min(8, ei.uniqueId.length()));
                obj.setName(ei.id + "_" + shortId);
            } else {
                obj.setName(ei.id);
            }
        } else {
            // 其他对象
            if ("CIRCLE".equals(ei.rt)) {
                GameObject tmp = new GameObject(ei.id == null ? ("Obj#"+index) : ei.id);
                tmp.addComponent(new TransformComponent(new Vector2(0,0)));
                com.gameengine.components.RenderComponent rc = tmp.addComponent(
                    new com.gameengine.components.RenderComponent(
                        com.gameengine.components.RenderComponent.RenderType.CIRCLE,
                        new Vector2(Math.max(1, ei.w), Math.max(1, ei.h)),
                        new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                    )
                );
                rc.setRenderer(renderer);
                obj = tmp;
            } else {
                obj = com.gameengine.game.EntityFactory.createAIVisual(renderer, Math.max(1, ei.w>0?ei.w:10), Math.max(1, ei.h>0?ei.h:10), ei.r, ei.g, ei.b, ei.a);
            }
            obj.setName(ei.id == null ? ("Obj#"+index) : ei.id);
        }
        
        // 设置位置
        TransformComponent tc = obj.getComponent(TransformComponent.class);
        if (tc == null) obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
        else tc.setPosition(new Vector2(ei.pos));
        
        return obj;
    }

    // ========== 文件列表模式 ==========
    private List<File> recordingFiles;
    private int selectedIndex = 0;

    private void ensureFilesListed() {
        if (recordingFiles != null) return;
        // 使用成员变量storage而不是创建新实例
        recordingFiles = storage.listRecordings();
    }

    private void handleFileSelection() {
        ensureFilesListed();
        
        // 添加调试输出，确认方法被调用
        // System.out.println("handleFileSelection called");
        
        // 切换到使用isKeyPressed而不是isKeyJustPressed，因为有时帧同步问题会导致justPressed错过按键
        // 同时保留isKeyJustPressed作为备选
        if (input.isKeyPressed(38) || input.isKeyPressed(InputManager.KEY_UP) || 
            input.isKeyJustPressed(38) || input.isKeyJustPressed(InputManager.KEY_UP)) { // up (AWT 38 / GLFW 265)
            // System.out.println("Up key pressed");
            selectedIndex = (selectedIndex - 1 + Math.max(1, recordingFiles.size())) % Math.max(1, recordingFiles.size());
            // 消费按键事件，防止连续触发
            input.consumeKey(38);
            input.consumeKey(InputManager.KEY_UP);
        } else if (input.isKeyPressed(40) || input.isKeyPressed(InputManager.KEY_DOWN) || 
                   input.isKeyJustPressed(40) || input.isKeyJustPressed(InputManager.KEY_DOWN)) { // down (AWT 40 / GLFW 264)
            // System.out.println("Down key pressed");
            selectedIndex = (selectedIndex + 1) % Math.max(1, recordingFiles.size());
            // 消费按键事件，防止连续触发
            input.consumeKey(40);
            input.consumeKey(InputManager.KEY_DOWN);
        } else if (input.isKeyPressed(10) || input.isKeyPressed(32) || 
                   input.isKeyPressed(InputManager.KEY_ENTER) || input.isKeyPressed(335) ||
                   input.isKeyJustPressed(10) || input.isKeyJustPressed(32) || 
                   input.isKeyJustPressed(InputManager.KEY_ENTER) || input.isKeyJustPressed(335)) { // enter/space (AWT 10/32, GLFW 257/335)
            // System.out.println("Enter/Space key pressed");
            if (!recordingFiles.isEmpty()) {
                String path = recordingFiles.get(selectedIndex).getAbsolutePath();
                this.recordingPath = path;
                clear();
                initialize();
            }
            // 消费按键事件
            input.consumeKey(10);
            input.consumeKey(32);
            input.consumeKey(InputManager.KEY_ENTER);
            input.consumeKey(335);
        } else if (input.isKeyPressed(InputManager.KEY_ESCAPE) || input.isKeyPressed(27) ||
                   input.isKeyJustPressed(InputManager.KEY_ESCAPE) || input.isKeyJustPressed(27)) { // esc
            // System.out.println("Escape key pressed");
            engine.setScene(new MainMenuScene());
            // 消费按键事件
            input.consumeKey(InputManager.KEY_ESCAPE);
            input.consumeKey(27);
        }
    }

    private void renderFileList() {
        ensureFilesListed();
        int w = renderer.getWidth();
        int h = renderer.getHeight();
        String title = "SELECT RECORDING";
        float tw = title.length() * 16f;
        renderer.drawText(w/2f - tw/2f, 80, title, 1f,1f,1f,1f);

        if (recordingFiles.isEmpty()) {
            String none = "NO RECORDINGS FOUND";
            float nw = none.length() * 14f;
            renderer.drawText(w/2f - nw/2f, h/2f, none, 0.9f,0.8f,0.2f,1f);
            String back = "ESC TO RETURN";
            float bw = back.length() * 12f;
            renderer.drawText(w/2f - bw/2f, h - 60, back, 0.7f,0.7f,0.7f,1f);
            return;
        }

        float startY = 140f;
        float itemH = 28f;
        for (int i = 0; i < recordingFiles.size(); i++) {
            String name = recordingFiles.get(i).getName();
            float x = 100f;
            float y = startY + i * itemH;
            if (i == selectedIndex) {
                renderer.drawRect(x - 10, y - 6, 600, 24, 0.3f,0.3f,0.4f,0.8f);
            }
            renderer.drawText(x, y, name, 0.9f,0.9f,0.9f,1f);
        }

        String hint = "UP/DOWN SELECT, ENTER PLAY, ESC RETURN";
        float hw = hint.length() * 12f;
        renderer.drawText(w/2f - hw/2f, h - 60, hint, 0.7f,0.7f,0.7f,1f);
    }

    // 解析相关逻辑已移至 RecordingJson
}


