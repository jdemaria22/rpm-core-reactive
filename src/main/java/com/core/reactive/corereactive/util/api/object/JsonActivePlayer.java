package com.core.reactive.corereactive.util.api.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JsonActivePlayer {
    public Abilities abilities;
    public ChampionStats championStats;
    public FullRunes fullRunes;
}
