package com.core.reactive.corereactive.util;

import com.core.reactive.corereactive.hook.Config;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.awt.Robot;

@Service
public class KeyboardService {
    private final Config.User32 user32;
    private static final String KI = "ki";
    public static final int KEYEVENTF_KEYDOWN = 0;
    public static final int KEYEVENTF_KEYUP = 2;

    public KeyboardService(Config.User32 user32) {
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
    }

    public void sendKeyUp(int c) {
        this.createRobot().keyRelease(c);
    }
}
