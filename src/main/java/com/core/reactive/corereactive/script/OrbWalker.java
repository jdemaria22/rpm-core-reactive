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

    private Double canAttackTime = 0.0000000000;
    private Double canMoveTime = 0.0000000000;
    private Double canCastTime = 0.0000000000;

    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }

        if (this.isVkSpacePressed()) {
            this.keepKeyOPressed();
            //this.walk();
            return castQ();
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
                    Double range = (double) localPlayer.getAttackRange();
                    Double gameTime = this.gameTimeComponent.getGameTime();
                    return this.targetService.getBestChampionInRange(range)
                            .defaultIfEmpty(Champion.builder().build())
                            .flatMap(champion -> {
                                if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(champion.getPosition())) {
                                    Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                    Vector2 mousePos = this.mouseService.getCursorPos();
                                    this.canAttackTime = gameTime + 1.0 / attackSpeed;
                                    this.canMoveTime = gameTime + this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (40/2000);
                                    this.canCastTime = gameTime + this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (40/2000);
                                    this.mouseService.mouseMiddleDown();
                                    this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.gameTimeComponent.sleep(10);
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(10);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    this.gameTimeComponent.sleep(10);
                                    this.user32.BlockInput(new WinDef.BOOL(false));
                                    this.mouseService.mouseMiddleUp();
                                    return Mono.just(Boolean.TRUE);
                                } else if (canMoveTime < gameTime) {
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseRightClickNoMove();
                                }
                                return Mono.just(Boolean.TRUE);
                            });
                });
    }

    private Mono<Boolean> castQ(){
        return this.targetService.getBestChampionInSpell(1200.0, 2000.0, 0.25, 120.0)
                .flatMap(predictedPosition -> {
                    Double gameTime = this.gameTimeComponent.getGameTime();
                    Vector2 mousePos = this.mouseService.getCursorPos();
                        if (this.canCastTime < gameTime && predictedPosition != null) {
                            this.canCastTime = gameTime + 0.25;
                            this.user32.BlockInput(new WinDef.BOOL(true));
                            this.gameTimeComponent.sleep(10);
                            this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
                            this.gameTimeComponent.sleep(10);
                            this.keyboardService.sendKeyDown(KeyEvent.VK_Q);
                            this.gameTimeComponent.sleep(10);
                            this.keyboardService.sendKeyUp(KeyEvent.VK_Q);
                            this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                            this.gameTimeComponent.sleep(10);
                            this.user32.BlockInput(new WinDef.BOOL(false));
                            return Mono.just(Boolean.TRUE);
                        } else if (canMoveTime < gameTime + 0.25) {
                            this.gameTimeComponent.sleep(30);
                            this.mouseService.mouseRightClickNoMove();
                        }
                        return Mono.just(Boolean.TRUE);
                });
    }

    private Mono<Boolean> laneClear(){
        return this.apiService.getJsonActivePlayer()
                .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                .flatMap(attackSpeed -> {
                    Champion localPlayer =  championComponent.getLocalPlayer();
                    Double range = (double) localPlayer.getAttackRange();
                    Double gameTime = this.gameTimeComponent.getGameTime();
                    return this.targetService.getBestMinionInRange(range)
                            .defaultIfEmpty(Minion.builder().build())
                            .flatMap(minion -> {
                                if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(minion.getPosition())) {
                                    Vector2 position = this.rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                                    Vector2 mousePos = this.mouseService.getCursorPos();
                                    this.canAttackTime = gameTime + 1.0 / attackSpeed;
                                    this.canMoveTime = gameTime + this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (40/2000);
                                    this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.gameTimeComponent.sleep(10);
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(10);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    this.gameTimeComponent.sleep(10);
                                    this.user32.BlockInput(new WinDef.BOOL(false));
                                    return Mono.just(Boolean.TRUE);
                                } else if (canMoveTime < gameTime) {
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseRightClickNoMove();
                                }
                                return Mono.just(Boolean.TRUE);
                            });
                });
    }

    private Double getWindUpTime(Double baseAs, Double windup, Double windupMod, Double cAttackSpeed) {
        double baseWindupTime = (1.0 / baseAs) * windup;
        double part2;

        double divide2TimesWindupMinusPart1 = (1.0 / cAttackSpeed) * windup - baseWindupTime;

        if (windupMod != 0) {
            part2 = divide2TimesWindupMinusPart1 * windupMod;
        } else {
            part2 = divide2TimesWindupMinusPart1;
        }

        return baseWindupTime + part2;
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
