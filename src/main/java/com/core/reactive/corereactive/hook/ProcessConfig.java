package com.core.reactive.corereactive.hook;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ProcessConfig {

    private static final String RIOT_WINDOW_CLASS = "RiotWindowClass";
    private static final int VALUE = 1024;

    public interface User32 extends StdCallLibrary {
        String USER_32 = "user32";
        User32 instance = (User32) Native.loadLibrary (USER_32, User32.class);
        WinDef.HWND FindWindowExA(WinDef.HWND parent, WinDef.HWND childAfter, String className, String windowName);
        WinDef.HWND FindWindowA(String className, String windowName);
        int GetWindowThreadProcessId(WinDef.HWND hWnd, IntByReference pref);
    }

    public interface Kernel32 extends StdCallLibrary {
        String KERNEL_32 = "kernel32";
        Kernel32 instance = (Kernel32) Native.loadLibrary (KERNEL_32, Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);
        int PROCESS_VM_READ = 0x0010;
        int PROCESS_QUERY_INFORMATION = 0x0400;
        int GetLastError();
        WinNT.HANDLE OpenProcess(int fdwAccess, boolean fInherit, int IDProcess);
        void ReadProcessMemory(WinNT.HANDLE hProcess, long lpBaseAddress, Pointer lpBuffer, int nSize, IntByReference lpNumberOfBytesRead);
    }

    public interface PsApi extends StdCallLibrary {
        String PSAPI = "psapi";
        PsApi instance = (PsApi) Native.loadLibrary (PSAPI, PsApi.class);
        boolean EnumProcessModules(WinNT.HANDLE handle, WinDef.HMODULE[] lphModule, int length, IntByReference intByReference);
    }

    @Bean
    public ProcessHook processHook() {
         ProcessHook processHook = ProcessHook.builder()
                .window(this.window())
                .process(this.process())
                .baseAddress(this.baseAddress())
                .pointerBaseAddress(this.basePointerAddress())
                .build();
         log.info("ProcessHook {}", processHook);
         return processHook;
    }

    @Bean
    public Kernel32 kernel32(){
        return Kernel32.instance;
    }

    private WinDef.HWND window() {
        return User32.instance.FindWindowA(RIOT_WINDOW_CLASS, null);
    }

    private WinNT.HANDLE process() {
        return Kernel32.instance.OpenProcess(Kernel32.PROCESS_VM_READ | Kernel32.PROCESS_QUERY_INFORMATION, true, this.processId());
    }

    private int processId() {
        IntByReference intByReference = new IntByReference();
        User32.instance.GetWindowThreadProcessId(this.window(), intByReference);
        return intByReference.getValue();
    }

    private long baseAddress() {
        WinNT.HANDLE processHandle=Kernel32.instance.OpenProcess(Kernel32.PROCESS_VM_READ | Kernel32.PROCESS_QUERY_INFORMATION, true, this.processId());
        WinDef.HMODULE[] hMods = new WinDef.HMODULE[VALUE];
        IntByReference intByReference = new IntByReference(VALUE);
        if (PsApi.instance.EnumProcessModules(processHandle, hMods, hMods.length, intByReference)) {
            return Pointer.nativeValue(hMods[0].getPointer());
        }
        return -1;
    }

    private Pointer basePointerAddress() {
        WinNT.HANDLE processHandle=Kernel32.instance.OpenProcess(Kernel32.PROCESS_VM_READ | Kernel32.PROCESS_QUERY_INFORMATION, true, this.processId());
        WinDef.HMODULE[] hMods = new WinDef.HMODULE[VALUE];
        IntByReference intByReference = new IntByReference(VALUE);
        if (PsApi.instance.EnumProcessModules(processHandle, hMods, hMods.length, intByReference)) {
            return hMods[0].getPointer();
        }
        return null;
    }
}
