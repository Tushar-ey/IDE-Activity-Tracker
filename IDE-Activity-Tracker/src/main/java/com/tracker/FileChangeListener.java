package com.tracker;

import java.util.List;

public interface FileChangeListener {
    void onFileChanged(String repositoryName, String filePath, int linesChanged, List<String> codeSnippets, List<String> deletedSnippets);
}