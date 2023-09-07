package com.core.reactive.corereactive.core;

import com.core.reactive.corereactive.component.gametime.GameTime;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.UnitManagerComponent;
import com.core.reactive.corereactive.script.OrbWalker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Core {
    private final GameTime gameTime;
    private final RendererComponent rendererComponent;
    private final UnitManagerComponent unitManagerComponent;
    private final OrbWalker orbWalker;

    public void run() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(System::gc, 0, 1, TimeUnit.SECONDS);
        while (true) {
            Mono<Boolean> loadMemory = gameTime.update()
                    .flatMap(gameTimeOk -> rendererComponent.update())
                    .flatMap(render -> unitManagerComponent.update());

            loadMemory.block();
            Mono<Boolean> loadScript = orbWalker.update();

            loadScript.block();
        }
    }
}