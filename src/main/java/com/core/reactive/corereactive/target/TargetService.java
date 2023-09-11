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
import java.math.RoundingMode;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;

    public Mono<Champion> getBestChampionInRange(BigDecimal range) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        return Mono.fromCallable(() -> {
            BigDecimal minAutos = BigDecimal.ZERO;
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
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()).subtract(champion.getJsonCommunityDragon().getGameplayRadius())).compareTo(range.add(localPLayer.getJsonCommunityDragon().getGameplayRadius())) < 0;
                if (inDistance){
                    BigDecimal minAttacks = getMinAttacks(BigDecimal.valueOf(localPLayer.getBaseAttack()), BigDecimal.valueOf(localPLayer.getBonusAttack()), BigDecimal.valueOf(champion.getHealth()), BigDecimal.valueOf(champion.getArmor()));
                    if (minAttacks.compareTo(minAutos) < 0 || minAutos.compareTo(BigDecimal.ZERO) == 0){
                        minAutos = minAttacks;
                        championFinal = champion;
                    }
                }
            }
            return championFinal;
        });
    }

    public Mono<Minion> getBestMinionInRange(BigDecimal range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponent.getLocalPlayer();
            BigDecimal minAutos = BigDecimal.ZERO;
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
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), minion.getPosition())).compareTo(range.add(localPLayer.getJsonCommunityDragon().getGameplayRadius())) < 0;
                if (inDistance){
                    BigDecimal minAttacks = getMinAttacks(BigDecimal.valueOf(localPLayer.getBaseAttack()), BigDecimal.valueOf(localPLayer.getBonusAttack()), BigDecimal.valueOf(minion.getHealth()), BigDecimal.valueOf(minion.getArmor()));
                    if (minAttacks.compareTo(minAutos) < 0 || minAutos.compareTo(BigDecimal.ZERO) == 0){
                        minAutos = minAttacks;
                        minionFinal = minion;
                    }
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

    private BigDecimal getMinAttacks(BigDecimal playerBasicAttack, BigDecimal playerBonusAttack, BigDecimal targetHealth, BigDecimal targetArmor) {
        BigDecimal effectiveDamage = getEffectiveDamage(playerBasicAttack.add(playerBonusAttack), targetArmor);

        if (effectiveDamage.compareTo(BigDecimal.ZERO) > 0) {
            return targetHealth.divide(effectiveDamage, 15, RoundingMode.HALF_UP);
        } else {
            // Handle the case where effective damage is non-positive (division by zero or negative damage).
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getEffectiveDamage(BigDecimal damage, BigDecimal armor) {
        BigDecimal oneHundred = BigDecimal.valueOf(100);

        if (armor.compareTo(BigDecimal.ZERO) >= 0) {
            BigDecimal divisor = oneHundred.add(armor);
            return damage.multiply(oneHundred).divide(divisor, 15, RoundingMode.HALF_UP);
        } else {
            BigDecimal divisor = oneHundred.subtract(armor);
            return damage.multiply(BigDecimal.valueOf(2)).subtract(oneHundred.divide(divisor, 15, RoundingMode.HALF_UP));
        }
    }
}
