package com.gameengine.recording;

import java.io.File;
import java.io.IOException;
import java.util.List;

// 存储抽象接口
public interface RecordingStorage {
    void openWriter(String path) throws IOException;
    void writeLine(String line) throws IOException;
    void closeWriter();

    Iterable<String> readLines(String path) throws IOException;
    List<File> listRecordings();
}


