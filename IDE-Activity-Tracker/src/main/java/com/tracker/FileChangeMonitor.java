package com.tracker;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;


/**
 * Monitors file changes in a given project directory.
 * Uses Java NIO WatchService to detect file modifications, creations, and deletions.
 * Notifies a FileChangeListener with details about changed lines and code snippets.
 */
public class FileChangeMonitor implements Runnable {
    private final Path projectRoot;
    private final String repositoryName;
    private final Map<Path, List<String>> fileContents = new HashMap<>();
    private final FileChangeListener listener;


    /**
     * Constructs a FileChangeMonitor for the specified project root.
     * Initializes the file contents map with the current state of all files.
     *
     * @param projectRootPath the root directory of the project to monitor
     * @param listener the listener to notify on file changes
     */
    public FileChangeMonitor(String projectRootPath, FileChangeListener listener) {
        this.projectRoot = Paths.get(projectRootPath);
        this.repositoryName = projectRootPath;
        this.listener = listener;
        // Initialize fileContents with current file states
        try {
            Files.walk(projectRoot)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            fileContents.put(file, Files.readAllLines(file));
                        } catch (IOException e) {
                            fileContents.put(file, Collections.emptyList());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add this method to register all directories recursively
    private void registerAllDirs(Path start, WatchService watchService) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .filter(dir -> {
            String path = dir.toString();
            return !path.contains(".idea") &&
                    !path.contains(".git") &&
                    !path.contains("build") &&
                    !path.contains("target") &&
                    !path.contains("out");
                })
                .forEach(dir -> {
                    try {
                        dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY
                        ,StandardWatchEventKinds.ENTRY_DELETE
                        ,StandardWatchEventKinds.ENTRY_CREATE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Starts monitoring for file changes.
     * Runs an infinite loop to process file system events and notifies the listener on changes.
     */

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

    /**
     * Processes a file change event.
     * Calculates the diff between the old and new file contents, counts changed lines,
     * collects non-empty changed code snippets, and notifies the listener.
     *
     * @param file the file that has changed
     * @throws IOException if an I/O error occurs while reading the file
     */
    private void processFileChange(Path file) throws IOException {

        String fileName = file.getFileName().toString();
        if (fileName.endsWith("~") || fileName.endsWith(".tmp") ||fileName.endsWith(".")) return;
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

        // Calculate the diff between old and new file contents
        Patch<String> patch = DiffUtils.diff(oldContent, newContent);

        // Count the number of changed lines and collect non-empty changed code snippets
        int linesChanged = 0;
        List<String> codeSnippets = new ArrayList<>();
        List<String> deletedSnippets = new ArrayList<>();

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            // Count both added and deleted lines
            linesChanged += delta.getTarget().size();
            linesChanged+=delta.getSource().size();
            // Add only non-empty added lines to code snippets
            for (String line : delta.getTarget().getLines()) {
                if (!line.trim().isEmpty()) {
                    codeSnippets.add(line.trim());
                }
            }
            for (String line : delta.getSource().getLines()) {
                if (!line.trim().isEmpty()) {
                    deletedSnippets.add(line.trim());
                }
            }

        }

        if (listener!=null && linesChanged > 0 && (!codeSnippets.isEmpty() || !deletedSnippets.isEmpty())) {
            listener.onFileChanged(repositoryName ,file.toString(), linesChanged, codeSnippets,deletedSnippets);
        }

        fileContents.put(file, newContent);
    }
}