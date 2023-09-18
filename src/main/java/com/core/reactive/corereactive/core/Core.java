package com.core.reactive.corereactive.core;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.UnitManagerComponent;
import com.core.reactive.corereactive.overlay.Overlay;
import com.core.reactive.corereactive.script.Drawing;
import com.core.reactive.corereactive.script.OrbWalker;
import com.core.reactive.corereactive.script.ScriptLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFW;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Core {
    private final GameTimeComponent gameTimeComponent;
    private final RendererComponent rendererComponent;
    private final UnitManagerComponent unitManagerComponent;
    private final OrbWalker orbWalker;
    private final Drawing drawing;
    private final Overlay overlay;

    public void run() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(System::gc, 0, 1, TimeUnit.SECONDS);
        long window = overlay.init();
        while (!GLFW.glfwWindowShouldClose(window)) {
            Flux.fromIterable(this.getMemoryLoaderServices())
                    .flatMap(MemoryLoaderService::update)
                    .all(result -> result).block();

            Flux.fromIterable(this.getScriptLoaderService())
                    .flatMap(ScriptLoaderService::update)
                    .all(result -> result).block();

            GLFW.glfwPollEvents();
        }
        overlay.terminate();
    }

    private List<MemoryLoaderService> getMemoryLoaderServices(){
        List<MemoryLoaderService> memoryLoaderServices = new ArrayList<>();
        memoryLoaderServices.add(unitManagerComponent);
        memoryLoaderServices.add(rendererComponent);
        memoryLoaderServices.add(gameTimeComponent);
        return memoryLoaderServices;
    }

    private List<ScriptLoaderService> getScriptLoaderService(){
        List<ScriptLoaderService> scriptLoaderServices = new ArrayList<>();
        scriptLoaderServices.add(orbWalker);
        scriptLoaderServices.add(drawing);
        return scriptLoaderServices;
    }
}