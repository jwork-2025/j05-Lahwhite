package com.gameengine.graphics;

// 根据RenderBackend创建具体渲染器实例（目前仅创建GPURenderer），解耦渲染器创建逻辑。
public class RendererFactory {
    public static IRenderer createRenderer(RenderBackend backend, int width, int height, String title) {
        if (backend == RenderBackend.GPU) {
            return new GPURenderer(width, height, title);
        }
        throw new IllegalArgumentException("不支持的渲染后端: " + backend);
    }
}

