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
    private Integer team;
    private Boolean isVisible;
    private Boolean isTargeteable;
    private Boolean isAlive;
    private Float health;
    private Vector3 position;
}
