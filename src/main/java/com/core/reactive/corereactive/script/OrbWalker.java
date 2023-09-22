package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.component.unitmanager.model.SpellBook;
import com.core.reactive.corereactive.hook.Config;
import com.core.reactive.corereactive.target.TargetService;
import com.core.reactive.corereactive.util.KeyboardService;
import com.core.reactive.corereactive.util.MouseService;
import com.core.reactive.corereactive.util.api.ApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

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
    private final List<String> championsWithPredictionAbilities = Arrays.asList("Ezreal", "Morgana", "Samira");
    private Double canAttackTime = 0.0000000000;
    private Double canMoveTime = 0.0000000000;
    private Double canCastTime = 0.0000000000;


    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }
        if (isVkSpacePressed()) {
            this.keepKeyOPressed();
            return attackTarget().flatMap(attackTarget -> {
                if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                    return walk().flatMap(walk -> castW().flatMap(castW -> walk().flatMap(wakW -> castQ().flatMap(castQ -> walk()))));
                } else {
                    return walk();
                }
            });
        }
        if (this.isVkVPressed()) {
            this.keepKeyOPressed();
            return laneClear().flatMap(attackTarget -> {
                if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                    return walk().flatMap(walk -> castW().flatMap(castW -> walk().flatMap(wakW -> castQ().flatMap(castQ -> walk()))));
                } else {
                    return walk();
                }
            });
        }
        return Mono.fromCallable(() -> {
            this.keyboardService.sendKeyUp(KeyEvent.VK_O);
            return Boolean.TRUE;
        });
    }

    private Mono<Boolean> walk() {
        return Mono.fromCallable(() -> {
            double gameTime = gameTimeComponent.getGameTime();
            if (this.canMoveTime < gameTime) {
                this.gameTimeComponent.sleep(35);
                this.mouseService.mouseRightClickNoMove();
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        });
    }
    private Mono<Boolean> attackTarget(){
        return this.apiService.getJsonActivePlayer()
                .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                .flatMap(attackSpeed -> {
                    Champion localPlayer =  championComponent.getLocalPlayer();
                    Double range = (double) localPlayer.getAttackRange();
                    Double gameTime = this.gameTimeComponent.getGameTime();
                    Double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (30/1000);
                    return this.targetService.getBestChampionInRange(range)
                            .defaultIfEmpty(Champion.builder().build())
                            .flatMap(champion -> {
                                if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(champion.getPosition())) {
                                    Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                    Vector2 mousePos = this.mouseService.getCursorPos();
                                    this.canAttackTime = gameTime + 1.0 / attackSpeed;
                                    this.canMoveTime = gameTime + windUpTime;
                                    this.canCastTime = gameTime + windUpTime +0.1;
                                    this.mouseService.mouseMiddleDown();
                                    //this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    //this.user32.BlockInput(new WinDef.BOOL(false));
                                    this.mouseService.mouseMiddleUp();
                                    return Mono.just(Boolean.TRUE);
                                }
                                return Mono.just(Boolean.TRUE);
                            });
                });
    }

    private Mono<Boolean> castQ() {
        Double spellRadiusQ = 60.0;
        Double spellDelayQ = 0.25;
        Double spellSpeedQ = 2000.0;
        Double spellRangeQ = 1200.0;
        return targetService.getPrediction(spellRangeQ, spellSpeedQ, spellDelayQ, spellRadiusQ)
                .flatMap(predictedPosition -> {
                    double gameTime = gameTimeComponent.getGameTime();
                    Champion localPlayer = championComponent.getLocalPlayer();
                    SpellBook spellBook = localPlayer.getSpellBook();
                    double qCoolDown = spellBook.getQ().getReadyAtSeconds();
                    int qLevel = spellBook.getQ().getLevel();
                    Vector2 mousePos = mouseService.getCursorPos();
                    Vector3 localPlayerPosition = localPlayer.getPosition();
                    Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ());

                    if (canCastQ(gameTime, predictedPosition, qCoolDown, qLevel, screenLocalPlayerPosition)) {
                        castQAbilitiy(predictedPosition, mousePos);
                    }
                    return Mono.just(true);
                });
    }

    private boolean canCastQ(double gameTime, Vector2 predictedPosition, double qCoolDown, int qLevel, Vector2 localPlayerPosition) {
        return this.canCastTime < gameTime &&
                predictedPosition != null &&
                gameTime - qCoolDown > 0 &&
                qLevel > 0 &&
                distanceBetweenTargets2D(localPlayerPosition, predictedPosition) < 1200.0;
    }

    private void castQAbilitiy(Vector2 predictedPosition, Vector2 mousePos) {
        this.canCastTime = gameTimeComponent.getGameTime() + 0.25;
        //user32.BlockInput(new WinDef.BOOL(true));
        this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
        this.keyboardService.sendKeyDown(KeyEvent.VK_Q);
        this.keyboardService.sendKeyUp(KeyEvent.VK_Q);
        this.gameTimeComponent.sleep(30);
        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
        //user32.BlockInput(new WinDef.BOOL(false));
    }

    private Mono<Boolean> castW() {
        Double spellRadiusW = 60.0;
        Double spellDelayW = 0.25;
        Double spellSpeedW = 2000.0;
        Double spellRangeW = 1200.0;
        return targetService.getPrediction(spellRangeW, spellSpeedW, spellDelayW, spellRadiusW)
                .flatMap(predictedPosition -> {
                    double gameTime = gameTimeComponent.getGameTime();
                    Champion localPlayer = championComponent.getLocalPlayer();
                    SpellBook spellBook = localPlayer.getSpellBook();
                    double wCoolDown = spellBook.getW().getReadyAtSeconds();
                    int wLevel = spellBook.getW().getLevel();
                    Vector2 mousePos = mouseService.getCursorPos();
                    Vector3 localPlayerPosition = localPlayer.getPosition();
                    Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ());

                    if (canCastW(gameTime, predictedPosition, wCoolDown, wLevel, screenLocalPlayerPosition)) {
                        castWAbilitiy(predictedPosition, mousePos);
                    }
                    return Mono.just(true);
                });
    }

    private boolean canCastW(double gameTime, Vector2 predictedPosition, double qCoolDown, int wLevel, Vector2 localPlayerPosition) {
        return this.canCastTime < gameTime &&
                predictedPosition != null &&
                gameTime - qCoolDown > 0 &&
                wLevel > 0 &&
                distanceBetweenTargets2D(localPlayerPosition, predictedPosition) < 1200.0;
    }

    private void castWAbilitiy(Vector2 predictedPosition, Vector2 mousePos) {
        this.canCastTime = gameTimeComponent.getGameTime() + 0.25;
        //user32.BlockInput(new WinDef.BOOL(true));
        this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
        this.keyboardService.sendKeyDown(KeyEvent.VK_W);
        this.keyboardService.sendKeyUp(KeyEvent.VK_W);
        this.gameTimeComponent.sleep(30);
        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
        //user32.BlockInput(new WinDef.BOOL(false));
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
                                    //this.user32.BlockInput(new WinDef.BOOL(true));
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    //this.user32.BlockInput(new WinDef.BOOL(false));
                                    return Mono.just(Boolean.TRUE);
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

    private Double distanceBetweenTargets2D(Vector2 vector1, Vector2 vector2) {
        Double xDiff = (double) Math.abs(vector1.getX() - vector2.getX());
        Double yDiff = (double) Math.abs(vector1.getY() - vector2.getY());

        double sumOfSquares = xDiff * xDiff + yDiff * yDiff;

        return Math.sqrt(sumOfSquares);
    }
}
