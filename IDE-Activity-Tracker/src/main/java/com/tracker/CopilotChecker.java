package com.tracker;


import java.io.File;
import java.nio.file.*;

public class CopilotChecker {

    public static boolean isCopilotEnabledVSCode() {
        String userHome = System.getProperty("user.home");
        Path extensionsPath = Paths.get(userHome, ".vscode", "extensions");

        if (!Files.exists(extensionsPath)) return false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().toLowerCase().startsWith("github.copilot")) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking VS Code Copilot: " + e.getMessage());
        }

        return false;
    }

    public static boolean isCopilotEnabledIntelliJ() {
        String userHome = System.getProperty("user.home");
        String[] possiblePaths = {
                userHome + "/AppData/Roaming/JetBrains", // Windows
                userHome + "/Library/Application Support/JetBrains" // macOS
        };

        for (String basePath : possiblePaths) {
            File jetbrainsDir = new File(basePath);
            if (jetbrainsDir.exists()) {
                File[] ideaVersions = jetbrainsDir.listFiles();
                if (ideaVersions != null) {
                    for (File versionDir : ideaVersions) {
                        File pluginDir = new File(versionDir, "plugins/github-copilot");
                        if (pluginDir.exists()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isCopilotEnabled() {
        return isCopilotEnabledVSCode() || isCopilotEnabledIntelliJ();
    }
}
