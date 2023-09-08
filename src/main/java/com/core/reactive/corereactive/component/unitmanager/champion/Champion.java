package com.core.reactive.corereactive.component.unitmanager.champion;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.util.api.object.JsonCommunityDragon;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class Champion {
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
    private JsonCommunityDragon jsonCommunityDragon;
}
