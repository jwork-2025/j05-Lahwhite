package com.gameengine.recording;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// 录制服务核心，负责运行时收集游戏数据（输入、关键帧）并异步写入存储。
public class RecordingService {
    private final RecordingConfig config;
    private final BlockingQueue<String> lineQueue;                  // 待写入行队列
    private volatile boolean recording;                             // 录制状态标记（volatile保证多线程可见性）
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();  // 默认文件存储
    private double elapsed;                                         // 总运行时间
    private double keyframeElapsed;                                 // 自上次关键帧以来的时间
    @SuppressWarnings("unused")
    private double sampleAccumulator;                               // 采样时间累积器
    private final double warmupSec = 0.1;                           // 等待一帧让场景对象完成初始化
    private final DecimalFormat qfmt;                               // 数字格式化器：控制小数位数（减少存储体积）
    private Scene lastFrame;                                        // 用于停止时写最后关键帧
    private String recordingFileName;                              // 当前录制的文件名

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.sampleAccumulator = 0.0;
        // 修改小数格式化器，确保与示例文件格式匹配
        this.qfmt = new DecimalFormat();
        this.qfmt.setMaximumFractionDigits(2); // 保持与示例文件一致的小数位数
        this.qfmt.setMinimumFractionDigits(2); // 确保总是显示两位小数
        this.qfmt.setGroupingUsed(false);
        this.qfmt.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(java.util.Locale.US)); // 使用点号作为小数点
        
        // 确保录像保存路径存在
        String recordingsDir = getRecordingsDirectory();
        java.io.File dir = new java.io.File(recordingsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * 获取统一的录像保存目录
     */
    public static String getRecordingsDirectory() {
        // 使用当前工作目录下的recordings文件夹
        return System.getProperty("user.dir") + java.io.File.separator + "recordings";    
    }

    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        
        // 生成带时间戳的文件名
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        this.recordingFileName = "recording_" + timestamp + ".jsonl";
        String fullPath = getRecordingsDirectory() + java.io.File.separator + recordingFileName;
        
        storage.openWriter(fullPath);
        System.out.println("开始录制，文件保存到: " + fullPath);

        // 创建并启动写入线程：从队列消费数据并写入存储
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { 
                    storage.closeWriter(); 
                    System.out.println("录制完成，文件已保存: " + fullPath);
                } catch (Exception ignored) {}
            }
        }, "record-writer");

        recording = true;
        writerThread.start();

        // 写入header信息：版本、窗口宽高（回放时需匹配窗口尺寸）
        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
    }

    public void stop() {
        if (!recording) return;
        try {
            if (lastFrame != null) {
                writeKeyframe(lastFrame);
            }
        } catch (Exception ignored) {}
        recording = false;
        try { writerThread.join(500); } catch (InterruptedException ignored) {}
    }

    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;
        sampleAccumulator += deltaTime;
        lastFrame = scene;

        // 1. 记录输入事件（仅“刚按下”的按键）
        // input events (sample at native frequency, but只写有justPressed)
        Set<Integer> just = input.getJustPressedKeysSnapshot();
        if (!just.isEmpty()) {
            // 使用StringBuilder高效拼接 JSON 字符串（相比+拼接更节省内存）。
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"input\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
            boolean first = true;
            for (Integer k : just) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]}");
            enqueue(sb.toString());
        }

        // sampled deltas placeholder（可扩展）：此处先跳过，保持最小版本

        // 2. 周期性生成关键帧（满足暖机时间+间隔条件）
        // periodic keyframe（跳过开头暖机，避免空关键帧）
        if (elapsed >= warmupSec && keyframeElapsed >= config.keyframeIntervalSec) {
            if (writeKeyframe(scene)) {
                keyframeElapsed = 0.0;
            }
        }
    }

    private boolean writeKeyframe(Scene scene) {
        StringBuilder sb = new StringBuilder();
        // 确保时间戳格式与示例文件一致，保留两位小数
        double timestamp = Math.round(elapsed * 100.0) / 100.0;
        sb.append("{\"type\":\"keyframe\",\"t\":").append(timestamp).append(",\"entities\":[");
        List<GameObject> objs = scene.getGameObjects();
        boolean first = true;
        int count = 0;          // 记录有效实体数量（避免空关键帧）

        for (GameObject obj : objs) {
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc == null) continue;

            // 获取实体位置
            float x = tc.getPosition().x;
            float y = tc.getPosition().y;

            // 构建单个实体的JSON
            if (!first) sb.append(',');
            // 为所有对象使用name作为基本标识，但为敌人添加uniqueId以确保唯一性
            sb.append('{')
              .append("\"id\":\"").append(obj.getName()).append("\",").append("\"x\":")
              .append(Math.round(x * 100.0) / 100.0).append(',').append("\"y\":")
              .append(Math.round(y * 100.0) / 100.0); // 保持与示例一致的两位小数
            
            // 为敌人对象添加唯一标识符，解决录像回放时的位移问题
            if (obj.getName().startsWith("Enemy") || obj.getName().startsWith("AIPlayer")) {
                sb.append(',').append("\"uniqueId\":\"").append(obj.getUniqueId()).append("\"");
            }

            // 处理渲染信息
            if (obj.getName().equals("Player")) {
                // Player对象特殊处理，直接标记为CUSTOM
                sb.append(',').append("\"rt\":\"CUSTOM\"");
            } else if (obj.getName().startsWith("Enemy") || obj.getName().startsWith("AIPlayer")) {
                // 敌人或AIPlayer处理为RECTANGLE类型
                sb.append(',')
                  .append("\"rt\":\"RECTANGLE\",").append("\"w\":20,").append("\"h\":20,")
                  .append("\"color\":[0,0.8,1,1]"); // 与示例文件中的颜色保持一致
            } else if (obj.getName().startsWith("Decoration")) {
                // 装饰物处理为CIRCLE类型
                sb.append(',')
                  .append("\"rt\":\"CIRCLE\",").append("\"w\":5,").append("\"h\":5,")
                  .append("\"color\":[0.5,0.5,1,0.8]"); // 与示例文件中的颜色保持一致
            } else if (obj.getName().startsWith("Bullet")) {
                // 子弹处理，不添加额外属性
                sb.append(',')
                  .append("\"rt\":\"CIRCLE\",").append("\"w\":4,").append("\"h\":4,")
                  .append("\"color\":[0.2,0.2,1,1]"); // 子弹颜色
            } else {
                // 其他对象的处理
                com.gameengine.components.RenderComponent rc = obj.getComponent(com.gameengine.components.RenderComponent.class);
                if (rc != null) {
                    com.gameengine.components.RenderComponent.RenderType rt = rc.getRenderType();
                    com.gameengine.math.Vector2 sz = rc.getSize();
                    com.gameengine.components.RenderComponent.Color col = rc.getColor();
                    sb.append(',')
                      .append("\"rt\":\"").append(rt.name()).append("\",").append("\"w\":")
                      .append(Math.round(sz.x * 10.0) / 10.0).append(',').append("\"h\":")
                      .append(Math.round(sz.y * 10.0) / 10.0).append(',')
                      .append("\"color\":[")
                      .append(Math.round(col.r * 10.0) / 10.0).append(',')
                      .append(Math.round(col.g * 10.0) / 10.0).append(',')
                      .append(Math.round(col.b * 10.0) / 10.0).append(',')
                      .append(Math.round(col.a * 10.0) / 10.0).append(']');
                } else {
                    // 标记自定义渲染
                    sb.append(',').append("\"rt\":\"CUSTOM\"");
                }
            }

            sb.append('}');
            first = false;
            count++;
        }
        sb.append("]}");
        if (count == 0) return false;
        enqueue(sb.toString());
        return true;
    }

    // 将一行数据加入写入队列
    private void enqueue(String line) {
        if (!lineQueue.offer(line)) {
            // 简单丢弃策略：队列满时丢弃低优先级数据（此处直接丢弃）
            System.err.println("录制队列已满，丢弃数据");
        }
    }

    // getters and setters

    public boolean isRecording() {
        return recording;
    }

}