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



    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()){
            return Mono.just(Boolean.TRUE);
        }
        if (isVkSpacePressed()) {
            this.keepKeyOPressed();
            return attackTarget()
                    .zipWith(walk())
                    .flatMap(tuple2 -> {
                        boolean attackTargetResult = tuple2.getT1();
                        boolean walkResult = tuple2.getT2();

                        if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                            return castW()
                                    .zipWith(castQ())
                                    .map(tuple -> {
                                        boolean castWResult = tuple.getT1();
                                        boolean castQResult = tuple.getT2();

                                        // Aplica la lógica según tus necesidades
                                        boolean allResults = attackTargetResult && walkResult && castWResult && castQResult;
                                        return allResults ? Boolean.TRUE : Boolean.FALSE;
                                    });
                        } else {
                            // Aplica la lógica según tus necesidades si no tienes habilidades de predicción
                            return Mono.just(attackTargetResult && walkResult);
                        }
                    });
        } else if (this.isVkVPressed()) {
            this.keepKeyOPressed();
            return attackTarget()
                    .zipWith(walk())
                    .flatMap(tupleAttackWalk -> {
                        boolean attackResult = tupleAttackWalk.getT1();
                        boolean walkResult = tupleAttackWalk.getT2();
                        if (attackResult && walkResult){
                            return Mono.just(Boolean.TRUE);
                        }
                        if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                            return castW()
                                    .zipWith(castQ())
                                    .flatMap(tupleCast -> {
                                        boolean castWResult = tupleCast.getT1();
                                        boolean castQResult = tupleCast.getT2();
                                        if (castWResult || castQResult){
                                            return Mono.just(Boolean.TRUE);
                                        }
                                        return laneClear()
                                                .zipWith(walk())
                                                .flatMap(tupleLaneClearWalk -> {
                                                    return castAbility();
                                                });
                                    });
                        } else {
                            return laneClear()
                                    .zipWith(walk())
                                    .map(tupleLaneClearWalk -> {
                                        boolean laneClearResult = tupleLaneClearWalk.getT1();
                                        boolean walkResult2 = tupleLaneClearWalk.getT2();
                                            // Aplica la lógica según tus necesidades
                                            boolean allResults = laneClearResult && walkResult2;
                                            return allResults ? Boolean.TRUE : Boolean.FALSE;
                                        });
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
            if (this.canMoveTime < gameTime) {
                this.gameTimeComponent.sleep(40);
                this.mouseService.mouseRightClickNoMove();
                return Mono.just(Boolean.TRUE);
            }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> attackTarget(){
        Double gameTime = this.gameTimeComponent.getGameTime();
        if (gameTime - lastCast > 0.5){
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        Double range = (double) localPlayer.getAttackRange();
                        double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (30/1000);
                        return this.targetService.getBestChampionInRange(range)
                                .defaultIfEmpty(Champion.builder().build())
                                .flatMap(champion -> {
                                    if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(champion.getPosition())) {
                                        this.canMoveTime = gameTime + windUpTime + 0.07;
                                        this.canCastTime = gameTime + windUpTime + 0.2;
                                        this.canAttackTime = gameTime + 1.0 / attackSpeed;
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
    private Mono<Boolean> laneClear(){
        Double gameTime = this.gameTimeComponent.getGameTime();
        if (gameTime - lastCast > 0.5){
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        Double range = (double) localPlayer.getAttackRange();
                        double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) + (40/2000);
                        return this.targetService.getBestMinionInRange(range)
                                .defaultIfEmpty(Minion.builder().build())
                                .flatMap(minion -> {
                                    if (this.canAttackTime < gameTime && !ObjectUtils.isEmpty(minion.getPosition())) {
                                        this.canMoveTime = gameTime + windUpTime + 0.07;
                                        this.canCastTime = gameTime + windUpTime + 0.2;
                                        this.canAttackTime = gameTime + 1.0 / attackSpeed;
                                        Vector2 position = this.rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                        this.gameTimeComponent.sleep(30);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.gameTimeComponent.sleep(30);

                                        return Mono.just(Boolean.TRUE);
                                    }
                                    return Mono.just(Boolean.TRUE);
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
                        this.canCastTime = gameTime + spellDelayQ;
                        this.lastCast = gameTime;
                        //this.canMoveTime = gameTime + spellDelayQ;

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

        return Mono.just(Boolean.FALSE);
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
                        this.canCastTime = gameTime + spellDelayW;
                        this.lastCast = gameTime;
                        //this.canMoveTime = gameTime + spellDelayW;

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

        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> castAbility() {
        //TODO: Laneclear
        Double gameTime = gameTimeComponent.getGameTime();
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();

        int qLevel = spellBook.getQ().getLevel();
        double qDamage = getEzrealDamageQ(qLevel);
        if (canCast(gameTime, qCoolDown, qLevel) && gameTime - lastCast > 0.6) {
            Double spellDelayQ = 0.25;
            Double spellRangeQ = 1200.0;
            return this.targetService.getMinionToLastHitBySpell(spellRangeQ, qDamage)
                                        .defaultIfEmpty(Minion.builder().build())
                                        .flatMap(minion -> {
                                            if (minion.getPosition() != null){
                                                this.canCastTime = gameTime + spellDelayQ;
                                                this.lastCast = gameTime;
                                                //this.canMoveTime = gameTime + spellDelayQ;

                                                Vector3 localPlayerPosition = localPlayer.getPosition();
                                                Vector2 minionPosition = rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                                                Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(
                                                        localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ()
                                                );

                                                if (isValidPoint(minionPosition, screenLocalPlayerPosition, spellRangeQ)) {
                                                    return Mono.just(cast(minionPosition, KeyEvent.VK_Q));
                                                } else {
                                                    return Mono.just(Boolean.FALSE);
                                                }
                                            }
                                            return Mono.just(Boolean.FALSE);
                                        });
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

    private Double getEzrealDamageQ(int qLvl){
        Champion localPlayer =  championComponent.getLocalPlayer();
     return switch (qLvl) {
            case 1 ->
                    20 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 1.3) + ((localPlayer.getAbilityPower()) * 0.15);
            case 2 ->
                    45 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 1.3) + ((localPlayer.getAbilityPower()) * 0.15);
            case 3 ->
                    70 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 1.3) + ((localPlayer.getAbilityPower()) * 0.15);
            case 4 ->
                    95 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 1.3) + ((localPlayer.getAbilityPower()) * 0.15);
            case 5 ->
                    120 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 1.3) + ((localPlayer.getAbilityPower()) * 0.15);
            default -> 0.0;
        };
    }
}
