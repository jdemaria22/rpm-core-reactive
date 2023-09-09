package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import com.sun.jna.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MinionComponent {
    private final ReadProcessMemoryService readProcessMemoryService;
    private final ConcurrentHashMap<Long, Minion> minionList = new ConcurrentHashMap<>();
    public Mono<Boolean> update() {
        return this.getAiMinionClient()
                .flatMap(aiMinionClient -> this.getAiMinionList(aiMinionClient)
                        .flatMap(aiMinionList -> this.getAiMinionLength(aiMinionClient)
                                .flatMap(minionArrayLength -> this.getMinionInfo(minionArrayLength, aiMinionList))))
                .flatMap(map -> Mono.just(Boolean.TRUE));
    }

    private Mono<Long> getAiMinionClient() {
        return this.readProcessMemoryService.reactiveRead(Offset.minionList , Long.class, true)
                .flatMap(client -> Mono.fromCallable(() -> client));
    }

    private Mono<Long> getAiMinionList(Long heroClient) {
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x8, Long.class, false);
    }

    private Mono<Long> getAiMinionLength(Long heroClient) {
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x10, Long.class, false);
    }

    private Mono<ConcurrentHashMap<Long, Minion>> getMinionInfo (Long minionArrayLen, Long minionArray) {
        return Mono.fromCallable(() -> {
            for (int i = 0; i < minionArrayLen.intValue(); i++) {
                Long unitId = this.readProcessMemoryService.read(minionArray + (0x8L * i), Long.class, false);
                if (unitId < 1) {
                    return this.minionList;
                }
                if (this.existsMinionInList(unitId)) {
                    this.findMinionChampion(this.minionList.get(unitId), unitId);
                } else {
                    Minion minion = Minion.builder().address(unitId).build();
                    this.minionList.put(unitId, this.findMinionChampion(minion, unitId));
                }
            }
            return this.minionList;
        });
    }

    private boolean existsMinionInList(Long address) {
        return this.minionList.containsKey(address);
    }

    private Minion findMinionChampion(Minion minion, long idUnit) {
        Memory memory = this.readProcessMemoryService.readMemory(idUnit, 0x4000, false);
        minion.setTeam(memory.getInt(Offset.objTeam));
        minion.setName(memory.getString(Offset.objName));
        Vector3 vector3 = Vector3.builder()
                .x(memory.getFloat(Offset.objPositionX))
                .y(memory.getFloat(Offset.objPositionX + 0x4))
                .z(memory.getFloat(Offset.objPositionX + 0x8))
                .build();
        minion.setPosition(vector3);
        minion.setIsAlive(memory.getByte(Offset.objSpawnCount) %2 == 0 );
        minion.setIsTargeteable(memory.getByte(Offset.objTargetable) != 0);
        minion.setIsVisible(memory.getByte(Offset.objVisible) != 0);
        return minion;
    }
}
