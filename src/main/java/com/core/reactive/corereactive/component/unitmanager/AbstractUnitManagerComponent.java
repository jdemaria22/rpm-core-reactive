package com.core.reactive.corereactive.component.unitmanager;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculator;
import com.core.reactive.corereactive.util.api.ApiService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractUnitManagerComponent<UNIT> implements UnitManagerProvider<UNIT>, UnitManagerService<UNIT>{
    protected static final int LENGTH_VALUE = 0x10;
    protected static final int LIST_VALUE = 0x8;
    protected ReadProcessMemoryService readProcessMemoryService;
    protected ApiService apiService;
    protected DistanceCalculator distanceCalculator;
    protected RendererComponent rendererComponent;

    protected AbstractUnitManagerComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculator distanceCalculator, RendererComponent rendererComponent) {
        this.readProcessMemoryService = readProcessMemoryService;
        this.apiService = apiService;
        this.distanceCalculator = distanceCalculator;
        this.rendererComponent = rendererComponent;
    }

    @Override
    public Mono<Boolean> update() {
        return this.getClient()
                .flatMap(client -> this.getClientList(client)
                        .flatMap(list -> this.getClientListLength(client)
                            .flatMap(lengthList -> this.loadUnitMap(lengthList, list))
                        )
                )
                .flatMap(s -> Mono.just(Boolean.TRUE));
    }

    @Override
    public Mono<Long> getClient() {
        return this.getOffset()
                .flatMap(off -> this.readProcessMemoryService.reactiveRead(off , Long.class, true)
                        .flatMap(client -> Mono.fromCallable(() -> client)));
    }

    @Override
    public Mono<Long> getClientList(Long client) {
        return this.readProcessMemoryService.reactiveRead(client + LIST_VALUE, Long.class, false);
    }

    @Override
    public Mono<Long> getClientListLength(Long client) {
        return this.readProcessMemoryService.reactiveRead(client + LENGTH_VALUE, Long.class, false);
    }

    @Override
    public Mono<ConcurrentHashMap<Long, UNIT>> loadUnitMap(Long clientListLength, Long clientList) {
        return Mono.fromCallable(() -> {
            for (int i = 0; i < clientListLength.intValue(); i++){
                Long unitId = this.readProcessMemoryService.read(clientList + (0x8 * i), Long.class, false);
                if (unitId < 1) {
                    return this.getMapUnit();
                }
                if (this.existsUnitInMap(unitId)) {
                    this.findUnitInfo(unitId, this.getMapUnit().get(unitId));
                } else {
                    this.getMapUnit().put(unitId, this.findUnitInfo(unitId, null));
                }
            }
            return this.getMapUnit();
        });
    }

    @Override
    public Boolean existsUnitInMap(Long address) {
        return this.getMapUnit().containsKey(address);
    }

}
