package com.core.reactive.corereactive.component.unitmanager.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@Builder
@ToString
public class Spell {
    Integer level;
    Float readyAtSeconds;
    Float value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Spell spell = (Spell) o;
        return Objects.equals(value, spell.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
