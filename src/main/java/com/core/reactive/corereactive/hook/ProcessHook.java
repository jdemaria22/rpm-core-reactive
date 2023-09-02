package com.core.reactive.corereactive.hook;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class ProcessHook {
    private WinDef.HWND window;
    private WinNT.HANDLE process;
    private int processId;
    private long baseAddress;
    private Pointer pointerBaseAddress;
}
