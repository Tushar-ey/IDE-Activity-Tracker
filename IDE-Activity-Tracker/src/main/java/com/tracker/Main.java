package com.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    /**
     * Entry point for the Tracker application.
     * <p>
     * Loads configuration, starts file change monitoring, and continuously
     * tracks IDE activity and file changes. Logs session data when the IDE exits.
     * </p>
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {

        try {
            ConfigManager.initializeConfig();
            JsonNode config = ConfigManager.getConfig();
            System.out.println(" Config loaded:");
            String userId= config.get("userId").asText();
            System.out.println("User ID: " +userId );
             String email= config.get("email").asText();
            System.out.println("Email: " + config.get("email").asText());
            System.out.println("Project Root: " + config.get("projectRootPath").asText());

            // Shared variables for file change info
//            CopyOnWriteArrayList<FileChangeLog> fileChangeLogs = new CopyOnWriteArrayList<>();
            ConcurrentHashMap<String, FileChangeLog> fileChangeMap = new ConcurrentHashMap<>();


            FileChangeListener listener = (repositoryName, filePath, changed, snippets,deletedSnippets) -> {
              //  fileChangeLogs.add(new FileChangeLog(filePath, changed, snippets));
                fileChangeMap.compute(filePath, (path, existingLog) -> {
                    if (existingLog == null) {
                        return new FileChangeLog(filePath, changed, new ArrayList<>(snippets),new ArrayList<>(deletedSnippets));
                    } else {
                        existingLog.linesChanged += changed;
                        existingLog.codeSnippets.addAll(snippets);
                        existingLog.deletedSnippets.addAll(deletedSnippets);
                        return existingLog;
                    }
                });

            };
            String projectRoot = config.get("projectRootPath").asText();
            FileChangeMonitor monitor = new FileChangeMonitor(projectRoot, listener);
            new Thread(monitor).start();
            // Next: Start monitoring modules here
            System.out.println("Monitoring IDE focus (VS Code / IntelliJ)...");

            boolean wasIDERunning=false;
            AtomicReference<String> lastKnownIDEName = new AtomicReference<>("Unknown IDE");
            AtomicReference<String> lastKnownIDEVersion = new AtomicReference<>("Unknown");
            AtomicReference<Boolean> copilotUsedDuringSession = new AtomicReference<>(false);
            ActivitySessionTracker sessionTracker = new ActivitySessionTracker();
            while (true) {
                boolean ideRunning = IDEActivityMonitor.isIDERunning();
                System.out.println(ideRunning);
                boolean ideActive = IDEActivityMonitor.isIDEActive();
                String ideName = IDEActivityMonitor.getIDEName();
                String version= IDEActivityMonitor.getIDEVersion();
//                String activeWindowTitle = IDEActivityMonitor.getActiveWindowTitle();
                boolean copilotEnabled = CopilotChecker.isCopilotEnabled();

                if(ideRunning) {
                    sessionTracker.updateSession(ideActive);
                    wasIDERunning = true;
                    if (!ideName.equals("Unknown IDE") && !ideName.isBlank()) {
                        lastKnownIDEName.set(ideName);
                    }

                    if (!version.equals("Unknown") && !version.isBlank()) {
                        lastKnownIDEVersion.set(version);
                    }

                    if (copilotEnabled) {
                        copilotUsedDuringSession.set(true);
                    }
                }
                else {
                    System.out.println("IdeRunning: "+ ideRunning);
                    if (wasIDERunning) {
                        System.out.println(wasIDERunning +"value");
                        // IDE just exited, log the session
                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode json = mapper.createObjectNode();
                        json.put("userId", userId);
                        json.put("email", email);
                        json.put("ide", lastKnownIDEName.get());
                        json.put("version", lastKnownIDEVersion.get());
                        json.put("copilot_enabled", copilotUsedDuringSession.get());
                        json.put("start_time", sessionTracker.getSessionStartTime());
                        json.put("active_minutes", sessionTracker.getActiveDuration());
                        json.putPOJO("file_changes", new ArrayList<>(fileChangeMap.values()));

                        ActivityLogger.log(json);
                        // Reset session state
                        wasIDERunning = false;
                        lastKnownIDEName.set("Unknown");
                        lastKnownIDEVersion.set("Unknown");
                        copilotUsedDuringSession.set(false);
//                        linesChanged.set(0);
//                        codeSnippets.clear();
//                        changedFileName.set("");
                        sessionTracker.resetSession();
                    }
                }
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode json = mapper.createObjectNode();
                json.put("userId", userId);
                json.put("email", email);
                json.put("ide", lastKnownIDEName.get());
                json.put("version", lastKnownIDEVersion.get());
                json.put("copilot_enabled", copilotUsedDuringSession.get());
                json.put("start_time", sessionTracker.getSessionStartTime());
                json.put("active_minutes", sessionTracker.getActiveDuration());
                json.putPOJO("file_changes", new ArrayList<>(fileChangeMap.values()));


//
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
                // Sleep for 5 seconds before next check
                Thread.sleep(10000);
            }
        }
        catch (Exception e) {
            System.err.println(" Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
