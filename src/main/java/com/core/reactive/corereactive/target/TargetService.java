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

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;

    public Mono<Champion> getBestChampionInRange(Double range) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        return Mono.fromCallable(() -> {
            double minAutos = 0.0;
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
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) - champion.getJsonCommunityDragon().getGameplayRadius() <= range + localPLayer.getJsonCommunityDragon().getGameplayRadius();
                if (inDistance){
                    Double minAttacks = getMinAttacks(
                            (double) localPLayer.getBaseAttack(),
                            (double) localPLayer.getBonusAttack(),
                            (double) champion.getHealth(),
                            (double) champion.getArmor()
                    );
                    if (minAttacks < minAutos || minAutos == 0.0){
                        minAutos = minAttacks;
                        championFinal = champion;
                    }
                }
            }
            return championFinal;
        });
    }

    public Mono<Minion> getBestMinionInRange(Double range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponent.getLocalPlayer();
            Double minAutos = 0.0;
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
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), minion.getPosition())).compareTo(range + localPLayer.getJsonCommunityDragon().getGameplayRadius()) < 0;
                if (inDistance){
                    Double minAttacks = getMinAttacks((double) localPLayer.getBaseAttack(), (double) localPLayer.getBonusAttack(), (double) minion.getHealth(), (double) minion.getArmor());
                    if (minAttacks.compareTo(minAutos) < 0 || minAutos == 0.0){
                        minAutos = minAttacks;
                        minionFinal = minion;
                    }
                }
            }
            return minionFinal;
        });
    }

    private Double distanceBetweenTargets(Vector3 position, Vector3 position2) {
        Double xDiff = (double) Math.abs(position.getX() - position2.getX());
        Double yDiff = (double) Math.abs(position.getY() - position2.getY());
        Double zDiff = (double) Math.abs(position.getZ() - position2.getZ());

        double sumOfSquares = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;

        return Math.sqrt(sumOfSquares);
    }

    private Double getMinAttacks(Double playerBasicAttack, Double playerBonusAttack, Double targetHealth, Double targetArmor) {
        Double effectiveDamage = getEffectiveDamage(playerBasicAttack + playerBonusAttack, targetArmor);

        if (effectiveDamage > 0.0) {
            return targetHealth / effectiveDamage;
        } else {
            // Handle the case where effective damage is non-positive (division by zero or negative damage).
            return 0.0;
        }
    }

    private Double getEffectiveDamage(Double damage, Double armor) {
        Double oneHundred = 100.0;

        if (armor >= 0.0) {
            double divisor = oneHundred + armor;
            return (damage * oneHundred) / divisor;
        } else {
            Double divisor = oneHundred - armor;
            return (damage * 2.0) - (oneHundred / divisor);
        }
    }

}
