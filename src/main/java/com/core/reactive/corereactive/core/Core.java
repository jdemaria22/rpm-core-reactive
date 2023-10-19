package com.core.reactive.corereactive.core;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.UnitManagerComponent;
import com.core.reactive.corereactive.overlay.Overlay;
import com.core.reactive.corereactive.script.OrbWalker;
import com.core.reactive.corereactive.script.ScriptLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
    private final Overlay overlay;

    public void run() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(System::gc, 0, 2, TimeUnit.SECONDS);
        ExecutorService overlayExecutor = Executors.newSingleThreadExecutor();
        overlayExecutor.submit(this::updateOverlay);

        while (true) {
            updateComponentList(getMemoryLoaderServices());
            updateScriptList(getScriptLoaderService());
        }
    }

    private void updateOverlay() {
        while (true) {
            updateComponentList(getMemoryLoaderServices());
            overlay.update().subscribeOn(Schedulers.boundedElastic());
            this.gameTimeComponent.sleep(1000 / 5);
        }
    }

    private void updateComponentList(List<MemoryLoaderService> components) {
        Flux.fromIterable(components)
                .flatMap(MemoryLoaderService::update)
                .blockLast();
    }

    private void updateScriptList(List<ScriptLoaderService> scripts) {
        Flux.fromIterable(scripts)
                .flatMap(ScriptLoaderService::update)
                .blockLast();
    }

    private List<MemoryLoaderService> getMemoryLoaderServices() {
        List<MemoryLoaderService> memoryLoaderServices = new ArrayList<>();
        memoryLoaderServices.add(rendererComponent);
        memoryLoaderServices.add(unitManagerComponent);
        memoryLoaderServices.add(gameTimeComponent);
        return memoryLoaderServices;
    }

    private List<ScriptLoaderService> getScriptLoaderService() {
        List<ScriptLoaderService> scriptLoaderServices = new ArrayList<>();
        scriptLoaderServices.add(orbWalker);
        return scriptLoaderServices;
    }
}
