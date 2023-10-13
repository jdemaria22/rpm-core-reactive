package com.core.reactive.corereactive.component.unitmanager.impl;

// Importaciones necesarias...

import com.core.reactive.corereactive.component.MemoryLoaderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class UnitManagerComponent implements MemoryLoaderService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;
    private final TowerComponent towerComponent;

    @Override
    public Mono<Boolean> update() {
        return Flux.fromIterable(this.getMemoryLoader())
                .flatMap(MemoryLoaderService::update)
                .all(result -> result);
    }

    public List<MemoryLoaderService> getMemoryLoader(){
        List<MemoryLoaderService> memoryLoaderServices = new ArrayList<>();
        memoryLoaderServices.add(championComponent);
        memoryLoaderServices.add(minionComponent);
        memoryLoaderServices.add(towerComponent);
        return memoryLoaderServices;
    }
}
