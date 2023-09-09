package com.core.reactive.corereactive.component.unitmanager.model;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class Minion {
    private Integer team;
    private Long address;
    private Boolean isVisible;
    private Boolean isTargeteable;
    private Boolean isAlive;
    private Vector3 position;
    private String name;
}
