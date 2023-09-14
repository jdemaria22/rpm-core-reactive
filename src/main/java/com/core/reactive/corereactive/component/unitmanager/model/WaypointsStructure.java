package com.core.reactive.corereactive.component.unitmanager.model;


import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class WaypointsStructure {
    private List<Vector3> navigationPath;
    private Vector3 currentWaypoint;
    private int currentSize;
    private int maxSize;
    private int passedWaypoint;
}