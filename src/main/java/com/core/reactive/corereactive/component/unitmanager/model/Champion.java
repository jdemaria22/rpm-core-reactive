package com.core.reactive.corereactive.component.unitmanager.model;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.util.api.object.JsonCommunityDragon;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@ToString
public class Champion extends Unit {
    private float attackRange;
    private Integer team;
    private float baseAttack;
    private float bonusAttack;
    private float health;
    private float armor;
    private float magicDamage;
    private Vector3 position;
    private Boolean isVisible;
    private Boolean isTargeteable;
    private Boolean isAlive;
    private JsonCommunityDragon jsonCommunityDragon;
    private AiManager aiManager;
    private Map<Long, Spell> spells;

    public Spell getSpellQ(){
        return spells.get(0L);
    }
    public Spell getSpellW(){
        return spells.get(1L);
    }
    public Spell getSpellE(){
        return spells.get(2L);
    }

    public Spell getSpellR(){
        return spells.get(3L);
    }

    public Spell getSpellD(){
        return spells.get(4L);
    }

    public Spell getSpellF(){
        return spells.get(5L);
    }

}
