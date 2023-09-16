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
    private Long address;
    private float attackRange;
    private Integer team;
    private String name;
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
    private SpellBook spellBook;
}
