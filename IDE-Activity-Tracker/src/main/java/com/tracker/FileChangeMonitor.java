package com.tracker;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

public class FileChangeMonitor implements Runnable {
    private final Path projectRoot;
    private final Map<Path, List<String>> fileContents = new HashMap<>();
    private final FileChangeListener listener;

    public FileChangeMonitor(String projectRootPath, FileChangeListener listener) {
        this.projectRoot = Paths.get(projectRootPath);
        this.listener = listener;
    }

    // Add this method to register all directories recursively
    private void registerAllDirs(Path start, WatchService watchService) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            registerAllDirs(projectRoot, watchService);
            while (true) {
                WatchKey key = watchService.take();
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = dir.resolve((Path) event.context());
                    if (Files.isRegularFile(changed)) {
                        processFileChange(changed);
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void processFileChange(Path file) throws IOException {

        String fileName = file.getFileName().toString();
        if (fileName.endsWith("~") || fileName.endsWith(".tmp")) return;
        List<String> oldContent = fileContents.getOrDefault(file, Collections.emptyList());
        List<String> newContent ;

        try {
            newContent = Files.readAllLines(file);
        } catch (FileSystemException e) {
            // File is locked, skip this event
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Patch<String> patch = DiffUtils.diff(oldContent, newContent);
        int linesChanged = 0;
        List<String> codeSnippets = new ArrayList<>();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            linesChanged += delta.getTarget().size();
            codeSnippets.addAll(delta.getTarget().getLines());
        }

        if (listener != null) {
            listener.onFileChanged(file.getFileName().toString(), linesChanged, codeSnippets);
        }

        fileContents.put(file, newContent);
    }
}