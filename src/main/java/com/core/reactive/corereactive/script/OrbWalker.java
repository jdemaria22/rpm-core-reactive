package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.hook.Config;
import com.core.reactive.corereactive.util.KeyboardService;
import com.core.reactive.corereactive.util.MouseService;
import com.core.reactive.corereactive.util.TargetService;
import com.core.reactive.corereactive.util.api.ApiService;
import com.sun.jna.platform.win32.WinDef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrbWalker implements ScriptLoaderService {
    private final ApiService apiService;
    private final ChampionComponent championComponent;
    private final GameTimeComponent gameTimeComponent;
    private final MouseService mouseService;
    private final RendererComponent rendererComponent;
    private final KeyboardService keyboardService;
    private final Config.User32 user32;
    private final TargetService targetService;

    private BigDecimal canAttackTime = new BigDecimal("0.000000000000");
    private BigDecimal canMoveTime = new BigDecimal("0.000000000000");

    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }
        if (this.keyboardService.isKeyDown(0x20)){
            this.keyboardService.sendKeyDown(KeyEvent.VK_O);
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        BigDecimal range = BigDecimal.valueOf(localPlayer.getAttackRange());
                        return this.targetService.getBestChampionInRange(range)
                                .defaultIfEmpty(Champion.builder().build())
                                .flatMap(champion -> {
                                    if (this.canAttackTime.compareTo(this.gameTimeComponent.getGameTime()) < 0 && !ObjectUtils.isEmpty(champion.getPosition())) {
                                        Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        BigDecimal attackSpeedValue = attackSpeed.setScale(15, RoundingMode.HALF_UP);
                                        BigDecimal value = new BigDecimal("1.00000000000000").divide(attackSpeedValue,RoundingMode.HALF_UP);
                                        this.canAttackTime = this.gameTimeComponent.getGameTime().add(value);
                                        this.canMoveTime = this.gameTimeComponent.getGameTime().add(this.getWindUpTime(champion.getJsonCommunityDragon().getAttackSpeed(), champion.getJsonCommunityDragon().getWindUp(), champion.getJsonCommunityDragon().getWindupMod(), attackSpeed));
                                        this.mouseService.mouseMiddleDown();
                                        this.user32.BlockInput(new WinDef.BOOL(true));
                                        this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                        this.gameTimeComponent.sleep(30);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.user32.BlockInput(new WinDef.BOOL(false));
                                        this.mouseService.mouseMiddleUp();
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    if (canMoveTime.compareTo(this.gameTimeComponent.getGameTime()) < 0) {
                                        this.gameTimeComponent.sleep(30);
                                        this.mouseService.mouseRightClickNoMove();
                                    }
                                    return Mono.just(Boolean.TRUE);
                                });
                    });
        }
        return Mono.fromCallable(() -> Boolean.TRUE);
    }

    public BigDecimal getWindUpTime(BigDecimal baseAs, BigDecimal windup, BigDecimal windupMod, BigDecimal cAttackSpeed) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal one = BigDecimal.ONE;
        int scale = 10;
        MathContext mathContext = new MathContext(scale, RoundingMode.HALF_UP);

        BigDecimal divide1 = one.divide(baseAs, mathContext);
        BigDecimal divide2 = one.divide(cAttackSpeed, mathContext);

        BigDecimal part1 = divide1.multiply(windup);
        BigDecimal part2;

        BigDecimal divide2TimesWindupMinusPart1 = divide2.multiply(windup).subtract(part1);

        if (windupMod.compareTo(zero) != 0) {
            part2 = divide2TimesWindupMinusPart1.multiply(windupMod);
        } else {
            part2 = divide2TimesWindupMinusPart1;
        }

        return part1.add(part2);
    }

}
