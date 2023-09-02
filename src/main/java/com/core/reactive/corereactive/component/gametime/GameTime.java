package com.core.reactive.corereactive.component.gametime;

import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Getter
@RequiredArgsConstructor
public class GameTime {
    private float gameTime;

    private final ReadProcessMemoryService readProcessMemoryService;
    public void update() {
        this.gameTime = this.readProcessMemoryService.read(Offset.gameTime, Float.class);
    }
    public Mono<Float> updateReactive() {
        return Mono.fromCallable(() -> {
            this.gameTime = this.readProcessMemoryService.read(Offset.gameTime, Float.class);
            return this.gameTime;
        });
    }
}
