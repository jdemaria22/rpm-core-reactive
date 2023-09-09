package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculator;
import com.core.reactive.corereactive.util.Offset;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Slf4j
public class ChampionComponentV2 extends AbstractUnitManagerComponent<Champion> {

    private static final int SIZE_CHAMPION = 0x4000;
    private final ConcurrentHashMap<Long, Champion> mapChampion = new ConcurrentHashMap<>();
    private Champion localPlayer = Champion.builder().build();

    protected ChampionComponentV2(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculator distanceCalculator, RendererComponent rendererComponent) {
        super(readProcessMemoryService, apiService, distanceCalculator, rendererComponent);
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
        if (ObjectUtils.isEmpty(champion)){
            champion = Champion.builder().address(address).build();
        }
        Memory memory = this.readProcessMemoryService.readMemory(address, SIZE_CHAMPION, false);
        champion.setTeam(memory.getInt(Offset.objTeam));
        champion.setName(memory.getString(Offset.objName));
        Vector3 vector3 = Vector3.builder()
                .x(memory.getFloat(Offset.objPositionX))
                .y(memory.getFloat(Offset.objPositionX + 0x4))
                .z(memory.getFloat(Offset.objPositionX + 0x8))
                .build();
        champion.setPosition(vector3);
        champion.setIsAlive(memory.getByte(Offset.objSpawnCount) %2 == 0 );
        champion.setIsTargeteable(memory.getByte(Offset.objTargetable) != 0);
        champion.setIsVisible(memory.getByte(Offset.objVisible) != 0);
        champion.setAttackRange(memory.getFloat(Offset.objAttackRange));
        champion.setHealth(memory.getFloat(Offset.objHealth));
        try {
            champion.setJsonCommunityDragon(apiService.getJsonCommunityDragon(champion).block());
        } catch (Exception ignored) {}
        return champion;
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
