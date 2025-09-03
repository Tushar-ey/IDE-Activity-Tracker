package com.tracker;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class ActivitySessionTracker {

    private long sessionStartTime = 0;
    private long activeTimeMillis = 0;
    private boolean isSessionActive = false;
    private boolean wasIDEActive = false;
    private LocalDateTime sessionStartTimestamp = null;

    private static final int IDLE_THRESHOLD_SECONDS = 60;

    public void updateSession(boolean ideInFocus) {
        boolean idle = isUserIdle();

        if (ideInFocus && !idle) {
            if (!isSessionActive) {
                sessionStartTime = System.currentTimeMillis();
                sessionStartTimestamp = LocalDateTime.now();
                isSessionActive = true;
            }
            wasIDEActive = true;
        } else {
            if (isSessionActive && wasIDEActive) {
                long now = System.currentTimeMillis();
                activeTimeMillis += (now - sessionStartTime);
                isSessionActive = false;
                wasIDEActive = false;
            }
        }
    }

    public String getActiveDuration() {
        long totalTime = activeTimeMillis;
        if (isSessionActive && !isUserIdle()) {
            totalTime += (System.currentTimeMillis() - sessionStartTime);
        }

        long seconds = totalTime / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return minutes + "m " + remainingSeconds + "s";
    }

    public int getActiveSeconds() {
        long totalTime = activeTimeMillis;
        if (isSessionActive && !isUserIdle()) {
            totalTime += (System.currentTimeMillis() - sessionStartTime);
        }
        return (int) (totalTime / 1000);
    }

    public String getSessionStartTime() {
        if (sessionStartTimestamp != null) {
            return sessionStartTimestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
        return "Not started";
    }


    public void resetSession() {
        sessionStartTime = 0;
        activeTimeMillis = 0;
        isSessionActive = false;
        wasIDEActive = false;
    }

    public boolean isUserIdle() {
        WinUser.LASTINPUTINFO lastInputInfo = new WinUser.LASTINPUTINFO();
        lastInputInfo.cbSize = lastInputInfo.size();

        if (User32.INSTANCE.GetLastInputInfo(lastInputInfo)) {
            long currentTime = Kernel32.INSTANCE.GetTickCount();
            long idleTime = currentTime - lastInputInfo.dwTime;
            return idleTime > IDLE_THRESHOLD_SECONDS * 1000;
        }
        return false;
    }

    public static class LASTINPUTINFO extends Structure {
        public int cbSize;
        public int dwTime;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("cbSize", "dwTime");
        }
    }
}
