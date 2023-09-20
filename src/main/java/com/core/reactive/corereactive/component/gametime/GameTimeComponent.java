package com.core.reactive.corereactive.component.gametime;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class    GameTimeComponent implements MemoryLoaderService {
    private Double gameTime;
    private final ReadProcessMemoryService readProcessMemoryService;

    @Override
    public Mono<Boolean> update() {

        return this.readProcessMemoryService.reactiveRead(Offset.gameTime, Double.class, true)
                .flatMap(floatTime -> {
                    if (floatTime < 1.0) {
                        return Mono.just(Boolean.FALSE);
                    }
                    this.gameTime = floatTime;
                    return Mono.just(Boolean.TRUE);
                });
    }

    @SneakyThrows
    public void sleeper(int ms){
        Thread.sleep(ms);
    }

    @SneakyThrows
    public void sleep(int ms) {
        long startNanos = System.nanoTime();
        long targetNanos = startNanos + ms * 1_000_000L; // Convierte milisegundos a nanosegundos

        while (System.nanoTime() < targetNanos) {
            // Espera hasta que el tiempo transcurrido alcance el valor deseado
            // No hace nada en este bucle, solo espera.
        }
    }
}