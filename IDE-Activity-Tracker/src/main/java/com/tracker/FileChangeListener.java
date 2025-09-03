package com.tracker;

import java.util.List;

public interface FileChangeListener {
    void onFileChanged(String fileName, int linesChanged, List<String> codeSnippets);
}