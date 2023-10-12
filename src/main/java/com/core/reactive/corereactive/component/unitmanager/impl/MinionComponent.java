package com.core.reactive.corereactive.component.unitmanager.impl;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.AbstractUnitManagerComponent;
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
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
@Slf4j
public class MinionComponent extends AbstractUnitManagerComponent<Minion> {
    private static final int SIZE_MINION = 0x4000;
    private final ConcurrentHashMap<Long, Minion> mapMinions = new ConcurrentHashMap<>();

    protected MinionComponent(ReadProcessMemoryService readProcessMemoryService, ApiService apiService, DistanceCalculatorService distanceCalculatorService, RendererComponent rendererComponent) {
        super(readProcessMemoryService, apiService, distanceCalculatorService, rendererComponent);
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
        unit.setTeam(memory.getInt(Offset.objTeam));
        unit.setName(memory.getString(Offset.objName));
        unit.setBaseAttack(memory.getFloat(Offset.objBaseAttack));
        unit.setBonusAttack(memory.getFloat(Offset.objBonusAttack));
        unit.setHealth(memory.getFloat(Offset.objHealth));
        unit.setArmor(memory.getFloat(Offset.objArmor));
        unit.setBonusArmor(memory.getFloat(Offset.objBonusArmor));
        unit.setMagicDamage(memory.getFloat(Offset.objAbilityPower));
        Vector3 vector3 = Vector3.builder()
                .x(memory.getFloat(Offset.objPositionX))
                .y(memory.getFloat(Offset.objPositionX + 0x4))
                .z(memory.getFloat(Offset.objPositionX + 0x8))
                .build();
        unit.setPosition(vector3);
        unit.setIsAlive(memory.getByte(Offset.objSpawnCount) %2 == 0 );
        unit.setIsTargeteable(memory.getByte(Offset.objTargetable) != 0);
        unit.setIsVisible(memory.getByte(Offset.objVisible) != 0);
        unit.setAttackRange(memory.getFloat(Offset.objAttackRange));
        return unit;
    }
}
