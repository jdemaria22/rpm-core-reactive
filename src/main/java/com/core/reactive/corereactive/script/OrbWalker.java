package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.hook.Config;
import com.core.reactive.corereactive.target.TargetService;
import com.core.reactive.corereactive.util.KeyboardService;
import com.core.reactive.corereactive.util.MouseService;
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
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrbWalker implements ScriptLoaderService {
    private static final String VAL = "1.0000000000";
    private final ApiService apiService;
    private final ChampionComponent championComponent;
    private final GameTimeComponent gameTimeComponent;
    private final MouseService mouseService;
    private final RendererComponent rendererComponent;
    private final KeyboardService keyboardService;
    private final Config.User32 user32;
    private final TargetService targetService;

    private BigDecimal canAttackTime = new BigDecimal("0.0000000000");
    private BigDecimal canMoveTime = new BigDecimal("0.0000000000");

    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }

        if (this.isVkSpacePressed()) {
            this.keepKeyOPressed();
            return walk();
        }

        if (this.isVkVPressed()) {
            this.keepKeyOPressed();
            return laneClear();
        }

        return Mono.fromCallable(() -> {
            this.keyboardService.sendKeyUp(KeyEvent.VK_O);
            return Boolean.TRUE;
        });
    }

    private Mono<Boolean> walk(){
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
                                    int scale = 5;
                                    MathContext mathContext = new MathContext(scale, RoundingMode.HALF_UP);
                                    BigDecimal value = new BigDecimal(VAL).divide(attackSpeed,RoundingMode.HALF_UP);
                                    this.canAttackTime = this.gameTimeComponent.getGameTime().add(value,mathContext);
                                    this.canMoveTime = this.gameTimeComponent.getGameTime().add(this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed),mathContext);
                                    this.mouseService.mouseMiddleDown();
                                    this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.gameTimeComponent.sleep(8);
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(8);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    this.user32.BlockInput(new WinDef.BOOL(false));
                                    this.mouseService.mouseMiddleUp();
                                    return Mono.just(Boolean.TRUE);
                                }
                                if (canMoveTime.compareTo(this.gameTimeComponent.getGameTime()) < 0) {
                                    this.mouseService.mouseRightClickNoMove();
                                    this.gameTimeComponent.sleep(30);
                                    return Mono.just(Boolean.TRUE);
                                }
                                return Mono.just(Boolean.TRUE);
                            });
                });
    }

    private Mono<Boolean> laneClear(){
        return this.apiService.getJsonActivePlayer()
                .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                .flatMap(attackSpeed -> {
                    Champion localPlayer =  championComponent.getLocalPlayer();
                    BigDecimal range = BigDecimal.valueOf(localPlayer.getAttackRange());
                    return this.targetService.getBestMinionInRange(range)
                            .defaultIfEmpty(Minion.builder().build())
                            .flatMap(minion -> {
                                if (this.canAttackTime.compareTo(this.gameTimeComponent.getGameTime()) < 0 && !ObjectUtils.isEmpty(minion.getPosition())) {
                                    Vector2 position = this.rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                                    Vector2 mousePos = this.mouseService.getCursorPos();
                                    int scale = 5;
                                    MathContext mathContext = new MathContext(scale, RoundingMode.HALF_UP);
                                    BigDecimal value = new BigDecimal(VAL).divide(attackSpeed,RoundingMode.HALF_UP);
                                    this.canAttackTime = this.gameTimeComponent.getGameTime().add(value,mathContext);
                                    this.canMoveTime = this.gameTimeComponent.getGameTime().add(this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed),mathContext);
                                    this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.gameTimeComponent.sleep(8);
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(8);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    this.user32.BlockInput(new WinDef.BOOL(false));
                                    //log.info("Basic Attack -> canMoveTime: {}", this.canMoveTime);
                                    //log.info("Basic Attack -> canAttackTime: {}", this.canAttackTime);
                                    return Mono.just(Boolean.TRUE);
                                }
                                if (canMoveTime.compareTo(this.gameTimeComponent.getGameTime()) < 0) {
                                    //log.info("Move -> canMoveTime: {}", this.canMoveTime);
                                    //log.info("Move -> getGameTime: {}", this.gameTimeComponent.getGameTime());
                                    this.mouseService.mouseRightClickNoMove();
                                    this.gameTimeComponent.sleep(30);
                                    return Mono.just(Boolean.TRUE);
                                }
                                return Mono.just(Boolean.TRUE);
                            });
                });
    }

    private BigDecimal getWindUpTime(BigDecimal baseAs, BigDecimal windup, BigDecimal windupMod, BigDecimal cAttackSpeed) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal one = BigDecimal.ONE;
        int scale = 5;
        MathContext mathContext = new MathContext(scale, RoundingMode.FLOOR);

        BigDecimal divide1 = one.divide(baseAs, mathContext);
        BigDecimal cAttackTime = one.divide(cAttackSpeed, mathContext);

        BigDecimal baseWindupTime = divide1.multiply(windup);
        BigDecimal part2;

        BigDecimal divide2TimesWindupMinusPart1 = cAttackTime.multiply(windup).subtract(baseWindupTime);

        if (windupMod.compareTo(zero) != 0) {
            part2 = divide2TimesWindupMinusPart1.multiply(windupMod);
        } else {
            part2 = divide2TimesWindupMinusPart1;
        }

        return baseWindupTime.add(part2);
    }

    private boolean isVkSpacePressed() {
        return this.keyboardService.isKeyDown(KeyEvent.VK_SPACE);
    }

    private boolean isVkVPressed() {
        return this.keyboardService.isKeyDown(KeyEvent.VK_V);
    }

    private void keepKeyOPressed(){
        if (!this.keyboardService.isKeyDown(KeyEvent.VK_O)) {
            this.keyboardService.sendKeyDown(KeyEvent.VK_O);
        }
    }
}
