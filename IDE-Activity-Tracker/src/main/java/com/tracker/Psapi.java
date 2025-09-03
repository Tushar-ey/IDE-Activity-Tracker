package com.tracker;

import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;


public interface Psapi extends StdCallLibrary {
    Psapi INSTANCE = Native.load("psapi", Psapi.class);
    int GetModuleBaseNameW(WinNT.HANDLE hProcess, WinNT.HANDLE hModule, char[] lpBaseName, int nSize);

}
