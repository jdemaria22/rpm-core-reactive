package com.core.reactive.corereactive.util;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponentV2;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TargetService {
    private final ChampionComponentV2 championComponentV2;

    public Mono<Champion> getBestChampionInRange(BigDecimal range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponentV2.getLocalPlayer();
            Float health = 0.0F;
            Champion championFinal = Champion.builder().build();
            for (Champion champion : this.championComponentV2.getMapUnit().values()) {
                if (!champion.getIsTargeteable()){
                    continue;
                }
                if (!champion.getIsVisible()) {
                    continue;
                }
                if (!champion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(champion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                if (health > champion.getHealth()) {
                    continue;
                }
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()).subtract(champion.getJsonCommunityDragon().getGameplayRadius())).compareTo(range.add(localPLayer.getJsonCommunityDragon().getGameplayRadius())) < 0;
                if (inDistance){
                    health = champion.getHealth();
                    championFinal = champion;
                }
            }
            return championFinal;
        });
    }

    private BigDecimal distanceBetweenTargets(Vector3 position, Vector3 position2) {
        BigDecimal xDiff = BigDecimal.valueOf(Math.abs(position.getX() - position2.getX()));
        BigDecimal yDiff = BigDecimal.valueOf(Math.abs(position.getY() - position2.getY()));
        BigDecimal zDiff = BigDecimal.valueOf(Math.abs(position.getZ() - position2.getZ()));

        BigDecimal sumOfSquares = xDiff.pow(2).add(yDiff.pow(2)).add(zDiff.pow(2));

        return BigDecimal.valueOf(Math.sqrt(sumOfSquares.doubleValue()));
    }

}
