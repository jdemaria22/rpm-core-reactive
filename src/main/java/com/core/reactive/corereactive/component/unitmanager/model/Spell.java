package com.core.reactive.corereactive.component.unitmanager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class Spell {
    int level;
    float readyAtSeconds;
    float value;
}
