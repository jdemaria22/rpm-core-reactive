package com.core.reactive.corereactive.component.unitmanager.minion;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
import com.core.reactive.corereactive.component.unitmanager.FunctionInfo;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculatorService;
import com.core.reactive.corereactive.util.Offset;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Slf4j
public class MinionComponent extends AbstractUnitManagerComponent<Minion> {
    private static final int SIZE_MINION = 0x4000;
    private final ConcurrentHashMap<Long, Minion> mapMinions = new ConcurrentHashMap<>();
    private final MinionInfoProvider minionInfoProvider;

    protected MinionComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculatorService distanceCalculatorService, RendererComponent rendererComponent, MinionInfoProvider minionInfoProvider) {
        super(readProcessMemoryService, apiService, distanceCalculatorService, rendererComponent);
        this.minionInfoProvider = minionInfoProvider;
    }

    @Override
    public Mono<ConcurrentHashMap<Long, Minion>> loadUnitMap(Long clientListLength, Long clientList) {
        return Mono.fromCallable(() -> {
            this.getMapMinions().clear();
            for (int i = 0; i < clientListLength.intValue(); i++){
                Long unitId = this.readProcessMemoryService.read(clientList + (0x8L * i), Long.class, false);
                if (unitId < 1) {
                    return this.getMapMinions();
                }
                this.getMapMinions().put(unitId, this.findUnitInfo(unitId, null));
            }
            return this.getMapMinions();
        });
    }

    @Override
    public Mono<Long> getOffset() {
        return Mono.just(Offset.minionList);
    }

    @Override
    public ConcurrentHashMap<Long, Minion> getMapUnit() {
        return this.mapMinions;
    }

    @Override
    public Minion findUnitInfo(Long address, Minion unit) {
        if (ObjectUtils.isEmpty(unit)){
            unit = Minion.builder().address(address).build();
        }
        Memory memory = this.readProcessMemoryService.readMemory(address, SIZE_MINION, false);
        FunctionInfo<Minion> functionInfo = new FunctionInfo<>(unit, memory);
        return Flux.fromIterable(this.minionInfoProvider.getUnitInfo()).flatMap(functionInfoMinionFunction -> Mono.just(functionInfoMinionFunction.apply(functionInfo))).blockLast();
    }
}
