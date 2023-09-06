package com.core.reactive.corereactive.util;

import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.hook.ProcessConfig;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MouseService {
    private final ProcessConfig.User32 user32;

    private static final String MI = "mi";
    public static final int MOUSEEVENTF_MOVE = 1;
    public static final int MOUSEEVENTF_LEFTDOWN = 2;
    public static final int MOUSEEVENTF_LEFTUP = 4;
    public static final int MOUSEEVENTF_RIGHTDOWN = 8;
    public static final int MOUSEEVENTF_RIGHTUP = 16;
    public static final int MOUSEEVENTF_MIDDLEDOWN = 32;
    public static final int MOUSEEVENTF_MIDDLEUP = 64;
    public static final int MOUSEEVENTF_WHEEL = 2048;

    public void mouseMove(int x, int y) {
        user32.SetCursorPos(x, y);
    }

    public void mouseLeftClickNoMove() {
        mouseAction(-1, -1, MOUSEEVENTF_LEFTDOWN);
        mouseAction(-1, -1, MOUSEEVENTF_LEFTUP);
    }

    public void mouseRightClickNoMove() {
        mouseAction(-1, -1, MOUSEEVENTF_RIGHTDOWN);
        mouseAction(-1, -1, MOUSEEVENTF_RIGHTUP);
    }

    public void mouseLeftClick(int x, int y) {
        mouseAction(x, y, MOUSEEVENTF_LEFTDOWN);
        mouseAction(x, y, MOUSEEVENTF_LEFTUP);
    }

    public void mouseRightClick(int x, int y) {
        user32.SetCursorPos(x, y);
        this.mouseRightClickNoMove();
    }

    public void mouseMiddleClick(int x, int y) {
        mouseAction(x, y, MOUSEEVENTF_MIDDLEDOWN);
        mouseAction(x, y, MOUSEEVENTF_MIDDLEUP);
    }

    public void blockInput(Boolean b) {
        user32.BlockInput(new WinDef.BOOL(b));
    }

    public Vector2 getCursorPos() {
        WinDef.POINT point = new WinDef.POINT();
        user32.GetCursorPos(point);
        return Vector2.builder().x(point.x).y(point.y).build();
    }

    private void mouseAction(int x, int y, int flags) {
        WinUser.INPUT input = new WinUser.INPUT();

        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        input.input.setType(MI);
        if (x != -1) {
            input.input.mi.dx = new WinDef.LONG(x);
        }
        if (y != -1) {
            input.input.mi.dy = new WinDef.LONG(y);
        }
        input.input.mi.time = new WinDef.DWORD(0);
        input.input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        input.input.mi.dwFlags = new WinDef.DWORD(flags);
        user32.SendInput(new WinDef.DWORD(1), new WinUser.INPUT[] { input }, input.size());
    }

}
