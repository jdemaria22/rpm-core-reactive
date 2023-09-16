package com.core.reactive.corereactive.component.unitmanager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class SpellBook {
    Spell q;
    Spell w;
    Spell e;
    Spell r;
    Spell d;
    Spell f;
}
