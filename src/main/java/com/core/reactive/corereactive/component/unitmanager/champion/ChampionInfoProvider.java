package com.core.reactive.corereactive.component.unitmanager.champion;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.FunctionInfo;
import com.core.reactive.corereactive.component.unitmanager.UnitInfoProvider;
import com.core.reactive.corereactive.component.unitmanager.model.AiManager;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Spell;
import com.core.reactive.corereactive.component.unitmanager.model.WaypointsStructure;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import com.sun.jna.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ChampionInfoProvider implements UnitInfoProvider<Champion> {
    public static final int SIZE_SPELL_BOOK = 48;
    public static final long POS = 0L;
    public static final int OFFSET = 0;
    public static final int SIZE_SPELL_ARRAY = 6;

    private final ReadProcessMemoryService readProcessMemoryService;

    @Override
    public List<Function<FunctionInfo<Champion>, Champion>> getUnitInfo() {
        List<Function<FunctionInfo<Champion>, Champion>> list = new ArrayList<>();
        list.add(this.setTeam());
        list.add(this.setName());
        list.add(this.setBaseAttack());
        list.add(this.setBonusAttack());
        list.add(this.setHealth());
        list.add(this.setMaxHealth());
        list.add(this.setArmor());
        list.add(this.setMagicDamage());
        list.add(this.setPosition());
        list.add(this.setIsAlive());
        list.add(this.setIsTargeteable());
        list.add(this.setIsVisible());
        list.add(this.setAttackRange());
        list.add(this.setAiManager());
        list.add(this.setSpellBook());
        return list;
    }
    private Function<FunctionInfo<Champion>, Champion> setTeam() {
        return functionInfo -> {
            functionInfo.getUnit().setTeam(functionInfo.getMemory().getInt(Offset.objName));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setName() {
        return functionInfo -> {
            functionInfo.getUnit().setName(functionInfo.getMemory().getString(Offset.objName));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setBaseAttack() {
        return functionInfo -> {
            functionInfo.getUnit().setBaseAttack(functionInfo.getMemory().getFloat(Offset.objBaseAttack));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setBonusAttack() {
        return functionInfo -> {
            functionInfo.getUnit().setBonusAttack(functionInfo.getMemory().getFloat(Offset.objBonusAttack));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setHealth() {
        return functionInfo -> {
            functionInfo.getUnit().setHealth(functionInfo.getMemory().getFloat(Offset.objHealth));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setMaxHealth() {
        return functionInfo -> {
            functionInfo.getUnit().setMaxHealth(functionInfo.getMemory().getFloat(Offset.objMaxHealth));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setArmor() {
        return functionInfo -> {
            functionInfo.getUnit().setArmor(functionInfo.getMemory().getFloat(Offset.objArmor));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setMagicDamage() {
        return functionInfo -> {
            functionInfo.getUnit().setMagicDamage(functionInfo.getMemory().getFloat(Offset.objMagicDamage));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setPosition() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            Vector3 vector3 = Vector3.builder()
                    .x(memory.getFloat(Offset.objPositionX))
                    .y(memory.getFloat(Offset.objPositionX + 0x4))
                    .z(memory.getFloat(Offset.objPositionX + 0x8))
                    .build();
            functionInfo.getUnit().setPosition(vector3);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setIsAlive() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isAlive = memory.getByte(Offset.objSpawnCount) % 2 == 0;
            functionInfo.getUnit().setIsAlive(isAlive);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setIsTargeteable() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isTargetable = memory.getByte(Offset.objTargetable) != 0;
            functionInfo.getUnit().setIsTargeteable(isTargetable);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setIsVisible() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isVisible = memory.getByte(Offset.objVisible) != 0;
            functionInfo.getUnit().setIsVisible(isVisible);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setAttackRange() {
        return functionInfo -> {
            functionInfo.getUnit().setAttackRange(functionInfo.getMemory().getFloat(Offset.objAttackRange));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setSpellBook() {
        return functionInfo -> {
            functionInfo.getUnit().setSpells(this.findSpellBook(functionInfo.getUnit().getAddress()));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Champion>, Champion> setAiManager() {
        return functionInfo -> {
            functionInfo.getUnit().setAiManager(this.findAiManager(functionInfo.getUnit().getAddress()));
            return functionInfo.getUnit();
        };
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
        Memory memoryWaypointsStructure = this.readProcessMemoryService.readMemory(address, 1064, false);
        int waypointCurrentSize = memoryWaypointsStructure.getInt(Offset.aiManagerWaypointCurrentSize);
        int waypointMaxSize = memoryWaypointsStructure.getInt(Offset.aiManagerWaypointMaxSize);
        int waypointPassedWaypoints = memoryWaypointsStructure.getInt(Offset.aiManagerPassedWaypoints);
        Vector3 currentWaypoint = Vector3.builder()
                .x(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint))
                .y(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint + 0x4))
                .z(memoryWaypointsStructure.getFloat(Offset.aiManagerWaypoint + 0x8))
                .build();
        if (waypointCurrentSize*12 > 0){
            Memory memoryWaypoints = this.readProcessMemoryService.readMemory(aiManagerArray, waypointCurrentSize*12, false);
            long numVector = memoryWaypoints.size() / 12;
            for (long i = 0; i < numVector; i++) {
                long offset = i * 12;
                waypoints.add(Vector3.builder()
                        .x(memoryWaypoints.getFloat(offset))
                        .y(memoryWaypoints.getFloat(offset + 0x4))
                        .z(memoryWaypoints.getFloat(offset + 0x8))
                        .build());
            }
        }
        return WaypointsStructure.builder()
                .navigationPath(waypoints)
                .currentWaypoint(currentWaypoint)
                .currentSize(waypointCurrentSize)
                .maxSize(waypointMaxSize)
                .passedWaypoint(waypointPassedWaypoints)
                .build();
    }

    private Map<Long, Spell> findSpellBook(long address) {
        Map<Long, Spell> spellMap = new HashMap<>();
        Memory memory = this.readProcessMemoryService.readMemory(address + Offset.objSpellBook, SIZE_SPELL_BOOK, false);
        Long pos = POS;
        for (long l : memory.getLongArray(OFFSET, SIZE_SPELL_ARRAY)) {
            Spell spellSlot = Spell.builder()
                    .level(this.readProcessMemoryService.read(l + Offset.spellBookSlotLevel, Long.class, false).intValue())
                    .value(this.readProcessMemoryService.read(l + Offset.spellBookSlotDamage, Float.class, false))
                    .readyAtSeconds(this.readProcessMemoryService.read(l + Offset.spellBookSlotTime, Float.class, false))
                    .build();
            spellMap.put(pos, spellSlot);
            pos++;
        }
        return spellMap;
    }
}
