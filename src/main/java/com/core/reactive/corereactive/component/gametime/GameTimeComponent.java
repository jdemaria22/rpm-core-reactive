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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
@Getter
@RequiredArgsConstructor
@Slf4j
public class    GameTimeComponent implements MemoryLoaderService {
    private BigDecimal gameTime;
    private final ReadProcessMemoryService readProcessMemoryService;

    @Override
    public Mono<Boolean> update() {
        int scale = 5;
        MathContext mathContext = new MathContext(scale, RoundingMode.HALF_UP);
        return this.readProcessMemoryService.reactiveRead(Offset.gameTime, Float.class, true)
                .flatMap(floatTime -> {
                    if (floatTime < 1.0) {
                        return Mono.just(Boolean.FALSE);
                    }
                    this.gameTime = new BigDecimal(floatTime,mathContext);
                    return Mono.just(Boolean.TRUE);
                });
    }

    @SneakyThrows
    public void sleep(int ms){
        Thread.sleep(ms);
    }
}
