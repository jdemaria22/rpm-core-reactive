package com.core.reactive.corereactive.component.unitmanager.model;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@ToString
public class Minion extends Unit {
    private Long address;
    private Float attackRange;
    private Integer team;
    private String name;
    private Float baseAttack;
    private Float bonusAttack;
    private Float health;
    private Float armor;
    private Float magicDamage;
    private Vector3 position;
    private Boolean isVisible;
    private Boolean isTargeteable;
    private Boolean isAlive;
}
