package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.component.unitmanager.model.SpellBook;
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
    private final TargetService targetService;
    private final List<String> championsWithPredictionAbilities = Arrays.asList("Ezreal", "Morgana", "Samira", "Ahri");
    private Double canAttackTime = 0.0000000000;
    private Double canMoveTime = 0.0000000000;
    private Double canCastTime = 0.0000000000;
    private Double lastCast = 0.0000000000;
    private final Double ping = 0.33;


    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }
        if (isVkSpacePressed()) {
            this.keepKeyOPressed();
            return attackTarget().flatMap(attackTarget -> {
                if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                    return walk().flatMap(walk -> castW().flatMap(castW -> castQ()));
                } else {
                    return walk();
                }
            });
        }
        if (this.isVkVPressed()) {
            this.keepKeyOPressed();
            return laneClear().flatMap(attackTarget -> {
                if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                    return walk().flatMap(walk -> castW().flatMap(castW -> castQ()));
                } else {
                    return walk();
                }
            });
        }
        return Mono.defer(() -> {
            this.keyboardService.sendKeyUp(KeyEvent.VK_O);
            return Mono.just(Boolean.TRUE);
        });
    }

    private Mono<Boolean> walk() {
        Double gameTime = this.gameTimeComponent.getGameTime();

        if (gameTime - lastCast > 0.3) {
            if (this.canMoveTime < gameTime) {
                this.gameTimeComponent.sleep(40);
                this.mouseService.mouseRightClickNoMove();
                return Mono.just(Boolean.TRUE);
            }
        }

        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> attackTarget(){
        Double gameTime = this.gameTimeComponent.getGameTime();
        if (gameTime - lastCast > 0.6){
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        Double range = (double) localPlayer.getAttackRange();
                        Double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (30/1000);
                        return this.targetService.getBestChampionInRange(range)
                                .defaultIfEmpty(Champion.builder().build())
                                .flatMap(champion -> {
                                    if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(champion.getPosition())) {
                                        this.canAttackTime = gameTime + 1.0 / attackSpeed;
                                        this.canMoveTime = gameTime + windUpTime;
                                        this.canCastTime = gameTime + windUpTime + 0.1;
                                        Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        this.mouseService.mouseMiddleDown();
                                        this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                        this.gameTimeComponent.sleep(30);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.mouseService.mouseMiddleUp();
                                        this.gameTimeComponent.sleep(30);
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    return Mono.just(Boolean.FALSE);
                                });
                    });
        }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> castQ() {

        Double gameTime = gameTimeComponent.getGameTime();
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();
        int qLevel = spellBook.getQ().getLevel();

        if (canCast(gameTime, qCoolDown, qLevel) && gameTime - lastCast > 0.6) {
            Double spellRadiusQ = 120.0;
            Double spellDelayQ = 0.25;
            Double spellSpeedQ = 2000.0;
            Double spellRangeQ = 1200.0;

            // Use flatMap to work with the result of targetService.getPrediction
            return targetService.getPrediction(spellRangeQ, spellSpeedQ, spellDelayQ, spellRadiusQ)
                    .flatMap(predictedPosition -> {
                        this.canCastTime = gameTime + spellDelayQ + ping;
                        this.lastCast = gameTime;
                        this.canMoveTime = gameTime + spellDelayQ;

                        Vector3 localPlayerPosition = localPlayer.getPosition();
                        Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(
                                localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ()
                        );

                        if (isValidPoint(predictedPosition, screenLocalPlayerPosition, spellRangeQ)) {
                            return Mono.just(cast(predictedPosition, KeyEvent.VK_Q));
                        } else {
                            return Mono.just(Boolean.FALSE);
                        }
                    })
                    .defaultIfEmpty(Boolean.FALSE);
        }

        return Mono.just(Boolean.TRUE);
    }
    private Mono<Boolean> castW() {
        Double gameTime = gameTimeComponent.getGameTime();
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double wCoolDown = spellBook.getW().getReadyAtSeconds();
        int wLevel = spellBook.getW().getLevel();

        if (canCast(gameTime, wCoolDown, wLevel)  && gameTime - lastCast > 0.6) {
            Double spellRadiusW = 160.0;
            Double spellDelayW = 0.25;
            Double spellSpeedW = 1700.0;
            Double spellRangeW = 1200.0;

            // Use flatMap to work with the result of targetService.getPrediction
            return targetService.getPrediction(spellRangeW, spellSpeedW, spellDelayW, spellRadiusW)
                    .flatMap(predictedPosition -> {
                        this.canCastTime = gameTime + spellDelayW + ping;
                        this.lastCast = gameTime;
                        this.canMoveTime = gameTime + spellDelayW;

                        Vector3 localPlayerPosition = localPlayer.getPosition();
                        Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(
                                localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ()
                        );

                        if (isValidPoint(predictedPosition, screenLocalPlayerPosition, spellRangeW)) {
                            // Assuming cast returns Mono<Boolean>
                            return Mono.just(cast(predictedPosition, KeyEvent.VK_W));
                        } else {
                            // Returning Mono.just(Boolean.FALSE) if isValidPoint check fails
                            return Mono.just(Boolean.FALSE);
                        }
                    })
                    .defaultIfEmpty(Boolean.FALSE); // Handling the case when prediction returns null
        }

        return Mono.just(Boolean.TRUE);
    }


    private boolean cast(Vector2 predictedPosition, int key) {
        Vector2 mousePos = mouseService.getCursorPos();
        this.mouseService.blockInput(Boolean.TRUE);
        this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
        this.gameTimeComponent.sleep(5);
        this.keyboardService.sendKeyDown(key);
        this.gameTimeComponent.sleep(5);
        this.keyboardService.sendKeyUp(key);
        this.gameTimeComponent.sleep(30);
        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
        this.mouseService.blockInput(Boolean.FALSE);

        // Return true for success, or false for failure
        return true;
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
                                    this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                    this.gameTimeComponent.sleep(30);
                                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
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
    private boolean canCast(double gameTime, double coolDown, int level) {
        return this.canCastTime < gameTime &&
                gameTime - coolDown > 0 &&
                level > 0;
    }
    private boolean isValidPoint(Vector2 predictedPosition, Vector2 localPlayerPosition, Double spellRange) {
        return predictedPosition != null &&
                distanceBetweenTargets2D(localPlayerPosition, predictedPosition) < spellRange;
    }
    private Double distanceBetweenTargets2D(Vector2 vector1, Vector2 vector2) {
        Double xDiff = (double) Math.abs(vector1.getX() - vector2.getX());
        Double yDiff = (double) Math.abs(vector1.getY() - vector2.getY());

        double sumOfSquares = xDiff * xDiff + yDiff * yDiff;

        return Math.sqrt(sumOfSquares);
    }
}
