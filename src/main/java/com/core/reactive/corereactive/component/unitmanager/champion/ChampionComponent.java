package com.core.reactive.corereactive.component.unitmanager.champion;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculator;
import com.core.reactive.corereactive.util.Offset;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
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
    private final ConcurrentHashMap<Long,Champion> championList = new ConcurrentHashMap<>();
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
//        log.info("heroClient {}", heroClient);
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x8L, Long.class, false);
    }

    private Mono<Long> getAiHeroLength(Long heroClient) {
        return this.readProcessMemoryService.reactiveRead(heroClient + 0x10L, Long.class, false);
    }

    private Mono<ConcurrentHashMap<Long, Champion>> getChampionInfo (Long heroArrayLen, Long heroArray) {
        return Mono.fromCallable(() -> {
//            log.info("heroArray {}", heroArray);
//            log.info("heroArrayLen {}", heroArrayLen);
            for (int i = 0; i < 2; i++){
                Long unitId = this.readProcessMemoryService.read(heroArray + (0x8L * i), Long.class, false);
                if (this.existsChampionInList(unitId)) {
                    this.findInfoChampion(this.championList.get(unitId), unitId);
                } else {
                    Champion champion = Champion.builder().address(unitId).build();
                    this.championList.put(unitId, this.findInfoChampion(champion, unitId));
                    champion.setJsonCommunityDragon(apiService.getJsonCommunityDragon(champion).block());
                }
            }
            return this.championList;
        });
    }

    public Mono<Champion> getBestTargetInRange(BigDecimal range) {
        return Flux.fromIterable(this.championList.values())
                .filter(champion -> {
                    if (!champion.getIsTargeteable()){
                        log.info("getIsTargeteable");
                        return false;
                    }
                    if (!champion.getIsVisible()) {
                        log.info("getIsVisible");
                        return false;
                    }
                    if (!champion.getIsAlive()) {
                        log.info("getIsAlive");
                        return false;
                    }
                    if (Objects.equals(champion.getTeam(), this.getLocalPlayer().getTeam())) {
                        return false;
                    }
                    Vector2 localPlayer = rendererComponent.worldToScreen(this.getLocalPlayer().getPosition());
                    BigDecimal localPlayerX = BigDecimal.valueOf(localPlayer.getX());
                    BigDecimal localPlayerY = BigDecimal.valueOf(localPlayer.getY());

                    Vector2 target = rendererComponent.worldToScreen(champion.getPosition());
                    BigDecimal targetX = BigDecimal.valueOf(target.getX());
                    BigDecimal targetY = BigDecimal.valueOf(target.getY());
                    log.info("target {}", targetY);
                    return distanceCalculator.inDistance(localPlayerX, localPlayerY, targetX, targetY, range, champion.getJsonCommunityDragon().getGameplayRadius(), this.getLocalPlayer().getJsonCommunityDragon().getGameplayRadius());
                }).next();
    }



    private Mono<Champion> getLocalPlayer(ConcurrentHashMap<Long,Champion> championList) {
        return readProcessMemoryService.reactiveRead(Offset.localPlayer, Long.class, true)
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
        champion.setIsAlive(memory.getByte(Offset.objSpawnCount) != 0);
        champion.setIsTargeteable(memory.getByte(Offset.objTargetable) != 0);
        champion.setIsVisible(memory.getByte(Offset.objVisible) != 0);
        champion.setAttackRange(memory.getFloat(Offset.objAttackRange));
        return champion;
    }

}
