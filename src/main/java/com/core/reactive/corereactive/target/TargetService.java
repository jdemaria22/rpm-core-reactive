package com.core.reactive.corereactive.target;

import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.MinionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;
    public Mono<Champion> getBestChampionInRange(BigDecimal range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponent.getLocalPlayer();
            Float health = 0.0F;
            Champion championFinal = Champion.builder().build();
            for (Champion champion : this.championComponent.getMapUnit().values()) {
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
                BigDecimal championRadius = champion.getJsonCommunityDragon().getGameplayRadius();
                BigDecimal localPlayerRadius = localPLayer.getJsonCommunityDragon().getGameplayRadius();
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()).subtract(championRadius).compareTo(range.add(localPlayerRadius))) < 0;
                if (inDistance){
                    health = champion.getHealth();
                    championFinal = champion;
                }
            }
            return championFinal;
        });
    }

    public Mono<Minion> getBestMinionInRange(BigDecimal range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponent.getLocalPlayer();
            Float health = 0.0F;
            Minion minionFinal = Minion.builder().build();
            for (Minion minion : this.minionComponent.getMapUnit().values()) {
                if (!minion.getIsTargeteable()){
                    continue;
                }
                if (!minion.getIsVisible()) {
                    continue;
                }
                if (!minion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(minion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                if (health > minion.getHealth()) {
                    continue;
                }
                BigDecimal localPlayerRadius = localPLayer.getJsonCommunityDragon().getGameplayRadius();
                BigDecimal minionRadius = new BigDecimal("65.0");
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), minion.getPosition()).subtract(minionRadius).compareTo(range.add(localPlayerRadius))) < 0;
                if (inDistance){
                    health = minion.getHealth();
                    minionFinal = minion;
                }
            }
            return minionFinal;
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
