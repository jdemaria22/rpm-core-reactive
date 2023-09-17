package com.core.reactive.corereactive.component.unitmanager.minion;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.FunctionInfo;
import com.core.reactive.corereactive.component.unitmanager.UnitInfoProvider;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.util.Offset;
import com.sun.jna.Memory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class MinionInfoProvider implements UnitInfoProvider<Minion> {

    @Override
    public List<Function<FunctionInfo<Minion>, Minion>> getUnitInfo() {
        List<Function<FunctionInfo<Minion>, Minion>> list = new ArrayList<>();
        list.add(this.setTeam());
        list.add(this.setName());
        list.add(this.setBaseAttack());
        list.add(this.setBonusAttack());
        list.add(this.setHealth());
        list.add(this.setArmor());
        list.add(this.setMagicDamage());
        list.add(this.setPosition());
        list.add(this.setIsAlive());
        list.add(this.setIsTargeteable());
        list.add(this.setIsVisible());
        list.add(this.setAttackRange());
        return list;
    }

    private Function<FunctionInfo<Minion>, Minion> setTeam() {
        return functionInfo -> {
            functionInfo.getUnit().setTeam(functionInfo.getMemory().getInt(Offset.objTeam));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setName() {
        return functionInfo -> {
            functionInfo.getUnit().setName(functionInfo.getMemory().getString(Offset.objName));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setBaseAttack() {
        return functionInfo -> {
            functionInfo.getUnit().setBaseAttack(functionInfo.getMemory().getFloat(Offset.objBaseAttack));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setBonusAttack() {
        return functionInfo -> {
            functionInfo.getUnit().setBonusAttack(functionInfo.getMemory().getFloat(Offset.objBonusAttack));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setHealth() {
        return functionInfo -> {
            functionInfo.getUnit().setHealth(functionInfo.getMemory().getFloat(Offset.objHealth));
            return functionInfo.getUnit();
        };
    }

    public Function<FunctionInfo<Minion>, Minion> setArmor() {
        return functionInfo -> {
            functionInfo.getUnit().setArmor(functionInfo.getMemory().getFloat(Offset.objArmor));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setMagicDamage() {
        return functionInfo -> {
            functionInfo.getUnit().setMagicDamage(functionInfo.getMemory().getFloat(Offset.objMagicDamage));
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setPosition() {
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

    private Function<FunctionInfo<Minion>, Minion> setIsAlive() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isAlive = memory.getByte(Offset.objSpawnCount) % 2 == 0;
            functionInfo.getUnit().setIsAlive(isAlive);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setIsTargeteable() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isTargetable = memory.getByte(Offset.objTargetable) != 0;
            functionInfo.getUnit().setIsTargeteable(isTargetable);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setIsVisible() {
        return functionInfo -> {
            Memory memory = functionInfo.getMemory();
            boolean isVisible = memory.getByte(Offset.objVisible) != 0;
            functionInfo.getUnit().setIsVisible(isVisible);
            return functionInfo.getUnit();
        };
    }

    private Function<FunctionInfo<Minion>, Minion> setAttackRange() {
        return functionInfo -> {
            functionInfo.getUnit().setAttackRange(functionInfo.getMemory().getFloat(Offset.objAttackRange));
            return functionInfo.getUnit();
        };
    }
    
}
