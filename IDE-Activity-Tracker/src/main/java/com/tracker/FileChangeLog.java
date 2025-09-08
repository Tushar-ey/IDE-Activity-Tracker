package com.tracker;

import java.util.List;

public class FileChangeLog {
    public String filePath;
    public int linesChanged;
    public List<String> codeSnippets;
    public List<String> deletedSnippets;

    public FileChangeLog(String filePath, int linesChanged, List<String> codeSnippets,List<String> deletedSnippets) {
        this.filePath = filePath;
        this.linesChanged = linesChanged;
        this.codeSnippets = codeSnippets;
        this.deletedSnippets = deletedSnippets;
    }
}
