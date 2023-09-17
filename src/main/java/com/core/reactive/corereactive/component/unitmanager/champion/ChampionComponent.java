package com.core.reactive.corereactive.component.unitmanager.champion;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
import com.core.reactive.corereactive.component.unitmanager.FunctionInfo;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
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
public class ChampionComponent extends AbstractUnitManagerComponent<Champion> {

    private static final int SIZE_CHAMPION = 0x4000;

    private final ConcurrentHashMap<Long, Champion> mapChampion = new ConcurrentHashMap<>();
    private Champion localPlayer = Champion.builder().build();
    private final ChampionInfoProvider championInfoProvider;

    protected ChampionComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculatorService distanceCalculatorService, RendererComponent rendererComponent, ChampionInfoProvider championInfoProvider) {
        super(readProcessMemoryService, apiService, distanceCalculatorService, rendererComponent);
        this.championInfoProvider = championInfoProvider;
    }

    @Override
    public Mono<Boolean> update() {
        return super.update()
                .flatMap(b -> this.findLocalPlayer()
                        .flatMap(champion -> {
                            this.localPlayer = champion;
                            return Mono.just(Boolean.TRUE);
                        }));
    }

    @Override
    public Mono<Long> getOffset() {
        return Mono.just(Offset.championList);
    }

    @Override
    public ConcurrentHashMap<Long, Champion> getMapUnit() {
        return this.mapChampion;
    }

    @Override
    public Champion findUnitInfo(Long address, Champion champion) {
        Memory memory = this.readProcessMemoryService.readMemory(address, SIZE_CHAMPION, false);
        if (ObjectUtils.isEmpty(champion)){
            champion = Champion.builder().address(address).build();
            champion.setName(memory.getString(Offset.objName));
            try {
                champion.setJsonCommunityDragon(apiService.getJsonCommunityDragon(champion).block());
            } catch (Exception exception) {
                log.info("error to get info from community dragon {}, {}", champion.getName(), exception.getMessage());
            }
        }
        FunctionInfo<Champion> functionInfo = new FunctionInfo<>(champion, memory);
        return Flux.fromIterable(this.championInfoProvider.getUnitInfo()).flatMap(functionInfoMinionFunction -> Mono.just(functionInfoMinionFunction.apply(functionInfo))).blockLast();
    }

    public Mono<Champion> findLocalPlayer() {
        return this.readProcessMemoryService.reactiveRead(Offset.localPlayer, Long.class, true)
                .flatMap(id -> {
                    if (this.mapChampion.containsKey(id)) {
                        Champion champion = this.mapChampion.get(id);
                        return Mono.just(champion);
                    }
                    return Mono.empty();
                });
    }

}