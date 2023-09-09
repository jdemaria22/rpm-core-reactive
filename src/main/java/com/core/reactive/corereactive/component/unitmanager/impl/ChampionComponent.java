package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculator;
import com.core.reactive.corereactive.util.Offset;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class ChampionComponent {
    private final ReadProcessMemoryService readProcessMemoryService;
    private final ConcurrentHashMap<Long, Champion> championList = new ConcurrentHashMap<>();
    private final ApiService apiService;
    private final DistanceCalculator distanceCalculator;
    private final RendererComponent rendererComponent;
    private Champion localPlayer = Champion.builder().build();

    public Mono<Boolean> update() {
        return this.getAiHeroClient()
                .flatMap(heroClient -> this.getAiHeroList(heroClient)
                        .flatMap(heroList -> this.getAiHeroLength(heroClient)
                            .flatMap(heroArrayLength -> this.getChampionInfo(heroArrayLength, heroList)))
                        .flatMap(this::getLocalPlayer)
                )
                .flatMap(localPlayer -> Mono.just(Boolean.TRUE));
    }

    private Mono<Long> getAiHeroClient() {
        return this.readProcessMemoryService.reactiveRead(Offset.championList , Long.class, true)
                .flatMap(client -> Mono.fromCallable(() -> client));
    }

    private Mono<Long> getAiHeroList(Long heroClient) {
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x8, Long.class, false);
    }

    private Mono<Long> getAiHeroLength(Long heroClient) {
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x10, Long.class, false);
    }

    private Mono<ConcurrentHashMap<Long, Champion>> getChampionInfo (Long heroArrayLen, Long heroArray) {
        return Mono.fromCallable(() -> {
            for (int i = 0; i < heroArrayLen.intValue(); i++){
                Long unitId = this.readProcessMemoryService.read(heroArray + (0x8 * i), Long.class, false);
                if (unitId < 1) {
                    return this.championList;
                }
                if (this.existsChampionInList(unitId)) {
                    this.findInfoChampion(this.championList.get(unitId), unitId);
                } else {
                    Champion champion = Champion.builder().address(unitId).build();
                    this.championList.put(unitId, this.findInfoChampion(champion, unitId));
                    try {
                        champion.setJsonCommunityDragon(apiService.getJsonCommunityDragon(champion).block());
                    } catch (Exception ignored) {}
                }
            }
            return this.championList;
        });
    }

    public Mono<Champion> getBestTargetInRange(BigDecimal range) {
        return Mono.fromCallable(() -> {
            Float health = 0.0F;
            Champion championFinal = Champion.builder().build();
            for (Champion champion : this.championList.values()) {
                if (!champion.getIsTargeteable()){
                    continue;
                }
                if (!champion.getIsVisible()) {
                    continue;
                }
                if (!champion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(champion.getTeam(), this.getLocalPlayer().getTeam())) {
                    continue;
                }
                if (health > champion.getHealth()) {
                    continue;
                }
                boolean inDistance = (this.distanceBetweenTargets(this.getLocalPlayer().getPosition(), champion.getPosition()).subtract(champion.getJsonCommunityDragon().getGameplayRadius())).compareTo(range.add(localPlayer.getJsonCommunityDragon().getGameplayRadius())) < 0;
                if (inDistance){
                    health = champion.getHealth();
                    championFinal = champion;
                }
            }
            return championFinal;
        });
    }

    private BigDecimal distanceBetweenTargets(Vector3 position, Vector3 position2) {
        BigDecimal xDiff = BigDecimal.valueOf(Math.abs(position.getX() - position2.getX()));
        BigDecimal yDiff = BigDecimal.valueOf(Math.abs(position.getY() - position2.getY()));
        BigDecimal zDiff = BigDecimal.valueOf(Math.abs(position.getZ() - position2.getZ()));

        BigDecimal sumOfSquares = xDiff.pow(2).add(yDiff.pow(2)).add(zDiff.pow(2));

        return BigDecimal.valueOf(Math.sqrt(sumOfSquares.doubleValue()));
    }

    private Mono<Champion> getLocalPlayer(ConcurrentHashMap<Long,Champion> championList) {
        return this.readProcessMemoryService.reactiveRead(Offset.localPlayer, Long.class, true)
                        .flatMap(id -> {
                            if (championList.containsKey(id)) {
                                Champion champion = championList.get(id);
                                this.localPlayer = champion;
                                return Mono.just(champion);
                            }
                            return Mono.empty();
                        });
    }

    private Boolean existsChampionInList(long unitId) {
        return this.championList.containsKey(unitId);
    }

    private Champion findInfoChampion(Champion champion, long idUnit) {
        Memory memory = this.readProcessMemoryService.readMemory(idUnit, 0x4000, false);
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
            champion.setJsonCommunityDragon(this.apiService.getJsonCommunityDragon(champion).block());
        } catch (Exception ignored) {}
        return champion;
    }

}
