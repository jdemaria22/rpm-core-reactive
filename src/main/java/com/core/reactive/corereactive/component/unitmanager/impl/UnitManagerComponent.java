package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class UnitManagerComponent implements MemoryLoaderService {
    private final ChampionComponentV2 championComponentV2;
    private final MinionComponent minionComponent;
    private final ChampionComponent championComponent;
    @Override
    public Mono<Boolean> update() {
        return this.championComponent.update();
    }

}

