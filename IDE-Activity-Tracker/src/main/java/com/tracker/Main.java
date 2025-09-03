package com.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) {
        try {
            ConfigManager.initializeConfig();
            JsonNode config = ConfigManager.getConfig();

            System.out.println(" Config loaded:");
            System.out.println("User ID: " + config.get("userId").asText());
            System.out.println("Email: " + config.get("email").asText());
            System.out.println("Project Root: " + config.get("projectRootPath").asText());

            // Shared variables for file change info
            AtomicReference<String> changedFileName = new AtomicReference<>("");
            AtomicInteger linesChanged = new AtomicInteger(0);
            CopyOnWriteArrayList<String> codeSnippets = new CopyOnWriteArrayList<>();

            FileChangeListener listener = new FileChangeListener() {
                @Override
                public void onFileChanged(String fileName, int changed, List<String> snippets) {
                    changedFileName.set(fileName);
                    linesChanged.set(changed);
                    codeSnippets.clear();
                    codeSnippets.addAll(snippets);
                }
            };
            String projectRoot = config.get("projectRootPath").asText();
            FileChangeMonitor monitor = new FileChangeMonitor(projectRoot, listener);
            new Thread(monitor).start();

            // Next: Start monitoring modules here
            System.out.println("Monitoring IDE focus (VS Code / IntelliJ)...");

            ActivitySessionTracker sessionTracker = new ActivitySessionTracker();
            //boolean wasIDERunning=false;
            while (true) {
                //boolean ideRunning = IDEActivityMonitor.isIDERunning();
                boolean ideActive = IDEActivityMonitor.isIDEActive();
                sessionTracker.updateSession(ideActive);

                String ideName = IDEActivityMonitor.getIDEName();
                String activeWindowTitle = IDEActivityMonitor.getActiveWindowTitle();
                boolean copilotEnabled = CopilotChecker.isCopilotEnabled();

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode json = mapper.createObjectNode();
                json.put("ide_active", ideActive);
                json.put("ide", ideName);
                json.put("window_title", activeWindowTitle);
                json.put("copilot_enabled", copilotEnabled);
                json.put("start_time", sessionTracker.getSessionStartTime());
                json.put("active_minutes", sessionTracker.getActiveDuration());
                json.put("changed_file", changedFileName.get());
                json.put("lines_changed", linesChanged.get());
                json.putPOJO("code_snippets", codeSnippets);

//                    ActivityLogger.log(json);
//
//                    // Reset session state
//                    wasIDERunning = false;
//                    linesChanged.set(0);
//                    codeSnippets.clear();
//                    changedFileName.set("");
//                    sessionTracker.resetSession();
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));

                // Sleep for 5 seconds before next check
                Thread.sleep(5000);
            }
        }
        catch (Exception e) {
            System.err.println(" Error loading config: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
