package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.unitmanager.champion.ChampionComponent;
import com.core.reactive.corereactive.overlay.Overlay;
import lombok.RequiredArgsConstructor;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class Drawing implements ScriptLoaderService{
    private final Overlay overlay;
    private final ChampionComponent championComponent;

    @Override
    public Mono<Boolean> update() {
        return Mono.fromCallable(() -> {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            this.drawChamp();
            GLFW.glfwSwapBuffers(overlay.getWindow());
            return Boolean.TRUE;
        });
    }

    private void drawChamp() {
        AtomicInteger val = new AtomicInteger();
        int progressBarWidth = 200;
        int progressBarHeight = 10;
        this.championComponent.getMapChampion().forEach((aLong, champion) -> {
            float maxProgress = champion.getMaxHealth();
            float currentProgress = champion.getHealth();
            float progressLength = currentProgress / maxProgress * progressBarWidth;
            GL11.glColor3f(0.0f, 1.0f, 0.0f);
            GL11.glRectf(10, val.get() + 10, 10 + progressLength, val.get() + 10 + progressBarHeight);
            val.set(val.get() + 40);
        });
    }
}
