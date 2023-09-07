package com.core.reactive.corereactive.util;

import com.core.reactive.corereactive.hook.ProcessConfig;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.KeyEvent;

@Service
public class KeyboardService {
    private final ProcessConfig.User32 user32;
    private static final String KI = "ki";
    public static final int KEYEVENTF_KEYDOWN = 0;
    public static final int KEYEVENTF_KEYUP = 2;

    public KeyboardService(ProcessConfig.User32 user32) {
        this.user32 = user32;
    }

    @SneakyThrows
    private Robot createRobot() {
        return new Robot();
    }

    public boolean isKeyDown(int vkCode) {
        short state = this.user32.GetAsyncKeyState(vkCode);
        return (0x1 & (state >> (Short.SIZE - 1))) != 0;
    }

    public void sendKeyDown(int c) {
        this.createRobot().keyPress(c);
//        robot.keyPress(KeyEvent.VK_O);
    }

    public void sendKeyUp(int c) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType(KI);
        input.input.ki.wScan = new WinDef.WORD(0);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        input.input.ki.wVk = new WinDef.WORD(c);
        input.input.ki.dwFlags = new WinDef.DWORD(KEYEVENTF_KEYUP);
        this.user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }
}
