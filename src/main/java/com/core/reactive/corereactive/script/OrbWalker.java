package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTime;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.unitmanager.champion.Champion;
import com.core.reactive.corereactive.component.unitmanager.champion.ChampionComponent;
import com.core.reactive.corereactive.util.KeyboardService;
import com.core.reactive.corereactive.util.MouseService;
import com.core.reactive.corereactive.util.api.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrbWalker {
    private final ApiService apiService;
    private final ChampionComponent championComponent;
    private final GameTime gameTime;
    private final MouseService mouseService;
    private final RendererComponent rendererComponent;
    private final KeyboardService keyboardService;

    private BigDecimal canAttackTime = new BigDecimal("0.000000000000");
    private BigDecimal canMoveTime = new BigDecimal("0.000000000000");
    public Mono<Boolean> update() {
        if (keyboardService.isKeyDown(0x20)){
            return apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        BigDecimal range = BigDecimal.valueOf(localPlayer.getAttackRange());
                        return championComponent.getBestTargetInRange(range)
                                .defaultIfEmpty(Champion.builder().build())
                                .flatMap(champion -> {
                                    if (!ObjectUtils.isEmpty(champion.getPosition()) && canAttackTime.compareTo(gameTime.getGameTime()) < 0) {
                                        Vector2 position = rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                        Vector2 mousePos = mouseService.getCursorPos();
                                        BigDecimal attackSpeedValue = attackSpeed.setScale(15, RoundingMode.HALF_UP);
                                        BigDecimal value = new BigDecimal("1.00000000000000").divide(attackSpeedValue,RoundingMode.HALF_UP);
                                        canAttackTime = gameTime.getGameTime().add(value);
                                        canMoveTime = gameTime.getGameTime().add(this.getWindUpTime(champion.getJsonCommunityDragon().getAttackSpeed(), champion.getJsonCommunityDragon().getWindUp(), champion.getJsonCommunityDragon().getWindupMod(), attackSpeed));
                                        mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                        this.sleep(40);
                                        mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    if (canMoveTime.compareTo(gameTime.getGameTime()) < 0) {
                                        this.sleep(40);
                                        mouseService.mouseRightClickNoMove();
                                    }
                                    return Mono.just(Boolean.TRUE);
                                });
                    });
        }
        return Mono.fromCallable(() -> Boolean.TRUE);
    }

    private BigDecimal getWindUpTime(BigDecimal baseAs, BigDecimal windup, BigDecimal windupMod, BigDecimal cAttackSpeed) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal one = BigDecimal.ONE;
        int scale = 10;
        RoundingMode roundingMode = RoundingMode.HALF_UP;
        MathContext mathContext = new MathContext(scale, roundingMode);

        BigDecimal divide1 = one.divide(cAttackSpeed.multiply(windup), mathContext);
        BigDecimal multiply = one.divide(baseAs, mathContext).multiply(windup);
        BigDecimal part2;
        if (windupMod.compareTo(zero) != 0) {
            part2 = divide1
                    .subtract(multiply)
                    .multiply(windupMod);

        } else {
            part2 = divide1
                    .subtract(multiply);

        }
        return multiply.add(part2);
    }

    @SneakyThrows
    private void sleep(int ms){
        Thread.sleep(ms);
    }
}
