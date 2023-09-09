package com.core.reactive.corereactive.core;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.UnitManagerComponent;
import com.core.reactive.corereactive.script.OrbWalker;
import com.core.reactive.corereactive.script.ScriptLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public void run() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(System::gc, 0, 1, TimeUnit.SECONDS);

        while (true) {
//            Flux.fromIterable(this.getMemoryLoaderServices())
//                    .flatMap(MemoryLoaderService::update).blockLast();

            Mono<Boolean> loadMemory = gameTimeComponent.update()
                    .flatMap(gameTimeOk -> rendererComponent.update())
                    .flatMap(render -> unitManagerComponent.update());

            loadMemory.block();

            log.info("unitManagerComponent {}", unitManagerComponent.getChampionComponent().getChampionList());
//            log.info("unitManagerComponent {}", unitManagerComponent.getChampionComponentV2().getLocalPlayer());
//            Flux.fromIterable(this.getScriptLoaderService())
//                    .flatMap(ScriptLoaderService::update).blockLast();
//            Mono<Boolean> loadScript = orbWalker.update();

//            loadScript.block();
        }
    }

    public List<MemoryLoaderService> getMemoryLoaderServices(){
        List<MemoryLoaderService> memoryLoaderServices = new ArrayList<>();
        memoryLoaderServices.add(unitManagerComponent);
        memoryLoaderServices.add(rendererComponent);
        memoryLoaderServices.add(gameTimeComponent);
        return memoryLoaderServices;
    }

    public List<ScriptLoaderService> getScriptLoaderService(){
        List<ScriptLoaderService> scriptLoaderServices = new ArrayList<>();
        scriptLoaderServices.add(orbWalker);
        return scriptLoaderServices;
    }
}