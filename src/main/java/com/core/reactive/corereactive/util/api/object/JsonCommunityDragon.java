package com.core.reactive.corereactive.util.api.object;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class JsonCommunityDragon {
    private Double attackSpeed;
    private Double gameplayRadius;
    private Double windUp;
    private Double windupMod;
}
