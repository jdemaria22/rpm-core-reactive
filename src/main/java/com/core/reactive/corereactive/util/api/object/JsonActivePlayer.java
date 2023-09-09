package com.core.reactive.corereactive.util.api.object;

import lombok.*;

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
