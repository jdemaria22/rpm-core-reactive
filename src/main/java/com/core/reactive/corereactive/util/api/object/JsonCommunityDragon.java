package com.core.reactive.corereactive.util.api.object;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class JsonCommunityDragon {
    private BigDecimal attackSpeed;
    private BigDecimal gameplayRadius;
    private BigDecimal windUp;
    private BigDecimal windupMod;
}
