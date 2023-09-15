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
public class AiManager {
    private Boolean isMoving;
    private Boolean isDashing;
    private Vector3 navBegin;
    private Vector3 navEnd;
    private Vector3 serverPos;
    private Vector3 velocity;
    private Float moveSpeed;
    private Float dashSpeed;
    private WaypointsStructure waypoints;
}
