package com.core.reactive.corereactive.component.unitmanager.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@ToString
public abstract class Unit {
    private Long address;
    private String name;
}
