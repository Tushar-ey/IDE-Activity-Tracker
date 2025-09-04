package com.tracker;


import java.io.File;
import java.nio.file.*;

public class CopilotChecker {

    public static boolean isCopilotEnabledVSCode() {
        String userHome = System.getProperty("user.home");
        Path extensionsPath = Paths.get(userHome, ".vscode", "extensions");
        Path disabledPath = Paths.get(userHome, ".vscode", "extensions", "disabled");

        if (!Files.exists(extensionsPath)) return false;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString().toLowerCase();
                if (name.startsWith("github.copilot")) {
                    // Check if extension is in disabled folder
                    if (Files.exists(disabledPath.resolve(entry.getFileName()))) {
                        return false;
                    }
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
        File jetbrainsDir = new File(userHome, "AppData/Roaming/JetBrains");
        if (jetbrainsDir.exists()) {
            File[] ideaVersions = jetbrainsDir.listFiles((dir, name) -> name.startsWith("Idea"));
            if (ideaVersions != null) {
                for (File versionDir : ideaVersions) {
                    File pluginDir = new File(versionDir, "plugins/github-copilot-intellij");
                    if (pluginDir.exists()) {
                        File disabledPluginsFile = new File(versionDir, "disabled_plugins.txt");
                        if (disabledPluginsFile.exists()) {
                            try {
                                String content = new String(java.nio.file.Files.readAllBytes(disabledPluginsFile.toPath()));
                                if (content.contains("com.github.copilot")) {
                                    return false;
                                }
                            } catch (Exception e) {
                                System.err.println("Error reading disabled_plugins.txt: " + e.getMessage());
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isCopilotEnabled() {
        String ideName = IDEActivityMonitor.getIDEName();
        if ("IntelliJ IDEA".equalsIgnoreCase(ideName)) {
            return isCopilotEnabledIntelliJ();
        } else if ("VS Code".equalsIgnoreCase(ideName)) {
            return isCopilotEnabledVSCode();
        }
        return false;
    }
}
