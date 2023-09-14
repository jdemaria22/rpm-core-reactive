package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
import com.core.reactive.corereactive.component.unitmanager.model.AiManager;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.WaypointsStructure;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.DistanceCalculatorService;
import com.core.reactive.corereactive.util.Offset;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Slf4j
public class ChampionComponent extends AbstractUnitManagerComponent<Champion> {

    private static final int SIZE_CHAMPION = 0x4000;
    private final ConcurrentHashMap<Long, Champion> mapChampion = new ConcurrentHashMap<>();
    private Champion localPlayer = Champion.builder().build();

    protected ChampionComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculatorService distanceCalculatorService, RendererComponent rendererComponent) {
        super(readProcessMemoryService, apiService, distanceCalculatorService, rendererComponent);
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
        champion.setTeam(memory.getInt(Offset.objTeam));
        champion.setName(memory.getString(Offset.objName));
        champion.setBaseAttack(memory.getFloat(Offset.objBaseAttack));
        champion.setBonusAttack(memory.getFloat(Offset.objBonusAttack));
        champion.setHealth(memory.getFloat(Offset.objHealth));
        champion.setArmor(memory.getFloat(Offset.objArmor));
        champion.setMagicDamage(memory.getFloat(Offset.objMagicDamage));
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
        champion.setAiManager(this.findAiManager(address));
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

    private AiManager findAiManager(Long address) {
        long v1 = address + Offset.aiManager;
        int v3b = (int) (this.readProcessMemoryService.read(v1 + 16L, Long.class, false) & 0xFF);
        long v7 = this.readProcessMemoryService.read(v1 + (8L * v3b + 24L), Long.class, false);
        long v5 = this.readProcessMemoryService.read(v1 + (8L), Long.class, false);
        v7 = v7 ^ ~v5;
        long addressAiManager = this.readProcessMemoryService.read(v7 + 16, Long.class, false);
        Memory memory = this.readProcessMemoryService.readMemory(addressAiManager, 1064, false);
        return AiManager.builder()
                .isMoving(memory.getByte(Offset.aiManagerIsMoving) != 0)
                .isDashing(memory.getByte(Offset.aiManagerIsDashing) != 0)
                .navBegin(Vector3.builder()
                        .x(memory.getFloat(Offset.aiManagerStartPath))
                        .y(memory.getFloat(Offset.aiManagerStartPath + 0x4))
                        .z(memory.getFloat(Offset.aiManagerStartPath + 0x8))
                        .build())
                .navEnd(Vector3.builder()
                        .x(memory.getFloat(Offset.aiManagerEndPath))
                        .y(memory.getFloat(Offset.aiManagerEndPath + 0x4))
                        .z(memory.getFloat(Offset.aiManagerEndPath + 0x8))
                        .build())
                .serverPos(Vector3.builder()
                        .x(memory.getFloat(Offset.aiManagerServerPosition))
                        .y(memory.getFloat(Offset.aiManagerServerPosition + 0x4))
                        .z(memory.getFloat(Offset.aiManagerServerPosition + 0x8))
                        .build())
                .velocity(Vector3.builder()
                        .x(memory.getFloat(Offset.aiManagerVelocity))
                        .y(memory.getFloat(Offset.aiManagerVelocity + 0x4))
                        .z(memory.getFloat(Offset.aiManagerVelocity + 0x8))
                        .build())
                .moveSpeed(memory.getFloat(Offset.aiManagerMovementSpeed))
                .dashSpeed(memory.getFloat(Offset.aiManagerDashSpeed))
                .waypoints(this.getWaypoints(addressAiManager))
                .build();
    }

    private WaypointsStructure getWaypoints(Long address) {
        List<Vector3> waypoints = new ArrayList<>();
        long aiManagerArray = this.readProcessMemoryService.read(address + Offset.aiManagerWaypointArray, Long.class, false);
        Memory memoryWaypoints = this.readProcessMemoryService.readMemory(aiManagerArray, 200, false);
        Memory memoryWaypointsStructure = this.readProcessMemoryService.readMemory(address, 1064, false);
        long numVector = memoryWaypoints.size() / 12;
        for (long i = 0; i < numVector; i++) {
            long offset = i * 12;
            waypoints.add(Vector3.builder()
                    .x(memoryWaypoints.getFloat(offset))
                    .y(memoryWaypoints.getFloat(offset + 0x4))
                    .z(memoryWaypoints.getFloat(offset + 0x8))
                    .build());
        }
        return WaypointsStructure.builder()
                .navigationPath(waypoints)
                .currentWaypoint(Vector3.builder()
                        .x(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint))
                        .y(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint + 0x4))
                        .z(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint + 0x8))
                        .build())
                .currentSize((int) memoryWaypointsStructure.getFloat(Offset.aiManagerWaypointCurrentSize))
                .maxSize((int) memoryWaypointsStructure.getFloat(Offset.aiManagerWaypointMaxSize))
                .passedWaypoint((int) memoryWaypointsStructure.getFloat(Offset.aiManagerPassedWaypoints))
                .build();
    }
}
