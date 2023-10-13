package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Tower;
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

import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Slf4j
public class TowerComponent extends AbstractUnitManagerComponent<Tower> {
    private static final int SIZE_TOWER = 0x4000;
    private final ConcurrentHashMap<Long, Tower> mapTowers = new ConcurrentHashMap<>();

    protected TowerComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculatorService distanceCalculatorService, RendererComponent rendererComponent) {
        super(readProcessMemoryService, apiService, distanceCalculatorService, rendererComponent);
    }

    @Override
    public Mono<ConcurrentHashMap<Long, Tower>> loadUnitMap(Long clientListLength, Long clientList) {
        return Mono.fromCallable(() -> {
            this.getMapTowers().clear();
            for (int i = 0; i < clientListLength.intValue(); i++){
                Long unitId = this.readProcessMemoryService.read(clientList + (0x8L * i), Long.class, false);
                if (unitId < 1) {
                    return this.getMapTowers();
                }
                this.getMapTowers().put(unitId, this.findUnitInfo(unitId, null));
            }
            return this.getMapTowers();
        });
    }
    @Override
    public Mono<Long> getOffset() {
        return Mono.just(Offset.towerList);
    }

    @Override
    public ConcurrentHashMap<Long, Tower> getMapUnit() {
        return this.mapTowers;
    }

    @Override
    public Tower findUnitInfo(Long address, Tower tower) {
        if (ObjectUtils.isEmpty(tower)){
            tower = Tower.builder().address(address).build();
        }
        Memory memory = this.readProcessMemoryService.readMemory(address, SIZE_TOWER, false);
        tower.setTeam(memory.getInt(Offset.objTeam));
        tower.setHealth(memory.getFloat(Offset.objHealth));
        tower.setArmor(memory.getFloat(Offset.objArmor));
        tower.setBonusArmor(memory.getFloat(Offset.objBonusArmor));
        Vector3 vector3 = Vector3.builder()
                .x(memory.getFloat(Offset.objPositionX))
                .y(memory.getFloat(Offset.objPositionX + 0x4))
                .z(memory.getFloat(Offset.objPositionX + 0x8))
                .build();
        tower.setPosition(vector3);
        tower.setIsAlive(memory.getByte(Offset.objSpawnCount) %2 == 0 );
        tower.setIsTargeteable(memory.getByte(Offset.objTargetable) != 0);
        tower.setIsVisible(memory.getByte(Offset.objVisible) != 0);
        tower.setAttackRange(memory.getFloat(Offset.objAttackRange));
        return tower;
    }
}
