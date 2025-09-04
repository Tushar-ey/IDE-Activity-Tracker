package com.tracker;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import static com.tracker.CopilotChecker.isCopilotEnabledVSCode;

public class IDEActivityMonitor {


    public static boolean isIDEActive() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return isIDEActiveWindows();
        } else if (os.contains("mac")) {
            return isIDEActiveMac();
        } else {
            return false;
        }
    }


    public static String getIDEName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return getIDENameWindows();
        } else if (os.contains("mac")) {
            return getIDENameMac();
        } else {
            return "Unsupported OS";
        }
    }


    private static boolean isIDEActiveWindows() {
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

        WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false, pid.getValue());

        if (process == null) {
            return false;
        }
        // Get module handle
        char[] exeName = new char[1024];
        Psapi.INSTANCE.GetModuleBaseNameW(process, null, exeName, exeName.length);
        String processName = Native.toString(exeName).toLowerCase();
        Kernel32.INSTANCE.CloseHandle(process);
        return processName.contains("idea") || processName.contains("code");

    }

    public static String getActiveWindowTitle() {
        char[] buffer = new char[1024];
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.GetWindowText(hwnd, buffer, 1024);
        return Native.toString(buffer);
    }

    private static String getIDENameWindows() {
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pid);

        WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(
                WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
                false, pid.getValue());

        if (process == null) {
            return "Unknown";
        }
        char[] exeName = new char[1024];
        Psapi.INSTANCE.GetModuleBaseNameW(process, null, exeName, exeName.length);
        String processName = Native.toString(exeName).toLowerCase();

        Kernel32.INSTANCE.CloseHandle(process);
        if (processName.contains("code")) {
            return "VS Code";
        } else if (processName.contains("idea")) {
            return "IntelliJ IDEA";
        } else {
            return "Unknown IDE";
        }
    }
    private static boolean isIDEActiveMac() {
        try {
            Process process = Runtime.getRuntime().exec(
                    "osascript -e 'tell application \"System Events\" to get name of first application process whose frontmost is true'"
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String appName = reader.readLine().toLowerCase();
            return appName.contains("intellij") || appName.contains("visual studio code");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private static String getIDENameMac() {
        try {
            Process process = Runtime.getRuntime().exec(
                    "osascript -e 'tell application \"System Events\" to get name of first application process whose frontmost is true'"
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String appName = reader.readLine().toLowerCase();

            if (appName.contains("visual studio code")) {
                return "VS Code";
            } else if (appName.contains("intellij")) {
                return "IntelliJ IDEA";
            } else {
                return "Unknown";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
    public static boolean isIDERunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("idea64.exe") || line.startsWith("code.exe")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getIntelliJProductInfoPath() {
        String jetBrainsPath = System.getenv("ProgramFiles") + "\\JetBrains";
        File jetBrainsDir = new File(jetBrainsPath);
        if (jetBrainsDir.exists() && jetBrainsDir.isDirectory()) {
            File[] dirs = jetBrainsDir.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.getName().startsWith("IntelliJ IDEA")) {
                        return dir.getAbsolutePath() + "\\product-info.json";
                    }
                }
            }
        }
        return null;
    }

    public static String getIDEVersion() {
        String ideName = getIDEName();
        if ("IntelliJ IDEA".equals(ideName)) {
            String ideaPath = getIntelliJProductInfoPath();
            try (BufferedReader reader = new BufferedReader(new FileReader(ideaPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"version\"")) {
                        return line.split(":")[1].replace("\"", "").replace(",", "").trim();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to read IntelliJ IDEA version: " + e.getMessage());
            }
        } else if ("VS Code".equals(ideName)) {
            String codePath = System.getenv("ProgramFiles") + "\\Microsoft VS Code\\resources\\app\\package.json";
            try (BufferedReader reader = new BufferedReader(new FileReader(codePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"version\"")) {
                        return line.split(":")[1].replace("\"", "").replace(",", "").trim();
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to read VS Code version: " + e.getMessage());
            }
        }
        return "";
    }

    private static String getIDEProcessName() {
        String ide = getIDEName().toLowerCase();
        if (ide.contains("code")) return "code.exe";
        if (ide.contains("idea")) return "idea64.exe";
        return "";
    }
}