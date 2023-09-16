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
    private Spell q;
    private Spell w;
    private Spell e;
    private Spell r;
    private Spell d;
    private Spell f;
}