package com.gameengine.recording;

// 录制配置参数
public class RecordingConfig {
    public String outputPath;
    public float keyframeIntervalSec = 0.5f;    // 关键帧间隔时间
    public int sampleFps = 30;                  // 采样帧率
    public float positionThreshold = 0.5f;      // 位置变化阈值
    public int quantizeDecimals = 2;            // 量化小数位数
    public int queueCapacity = 2048;            // 录制数据队列容量 

    public RecordingConfig(String outputPath) {
        this.outputPath = outputPath;
    }
}


