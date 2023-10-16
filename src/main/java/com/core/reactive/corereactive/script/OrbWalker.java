package com.core.reactive.corereactive.script;

import com.core.reactive.corereactive.component.gametime.GameTimeComponent;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import com.core.reactive.corereactive.component.unitmanager.model.SpellBook;
import com.core.reactive.corereactive.component.unitmanager.model.Tower;
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
    private final List<String> championsWithPredictionAbilities = Arrays.asList("Ezreal", "Morgana", "Samira");
    private Double canAttackTime = 0.0000000000;
    private Double canMoveTime = 0.0000000000;
    private Double canCastTime = 0.0000000000;
    private Double lastCast = 0.0000000000;



    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()) {
            return Mono.just(Boolean.TRUE);
        }

        if (isVkSpacePressed()) {
            this.keepKeyOPressed();
            return attackTarget()
                    .zipWith(walk())
                    .flatMap(tuple2 -> {
                        boolean attackTargetResult = tuple2.getT1();
                        boolean walkResult = tuple2.getT2();
                        if (attackTargetResult && walkResult) {
                            return Mono.just(Boolean.TRUE);
                        }
                        if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                            return castW()
                                    .zipWith(castQ())
                                    .map(tuple -> {
                                        boolean castWResult = tuple.getT1();
                                        boolean castQResult = tuple.getT2();
                                        return castWResult && castQResult;
                                    });
                        } else {
                            // Aplica la lógica según tus necesidades si no tienes habilidades de predicción
                            return Mono.just(Boolean.FALSE);
                        }
                    });
        } else if (this.isVkVPressed()) {
            this.keepKeyOPressed();
            return attackTarget()
                    .zipWith(walk())
                    .flatMap(tupleAttackWalk -> {
                        boolean attackResult = tupleAttackWalk.getT1();
                        boolean walkResult = tupleAttackWalk.getT2();
                        if (attackResult && walkResult) {
                            return Mono.just(Boolean.TRUE);
                        }
                        if (championsWithPredictionAbilities.contains(championComponent.getLocalPlayer().getName())) {
                            return castW()
                                    .zipWith(castQ())
                                    .flatMap(tupleCast -> {
                                        boolean castWResult = tupleCast.getT1();
                                        boolean castQResult = tupleCast.getT2();
                                        if (castWResult || castQResult) {
                                            return Mono.just(Boolean.TRUE);
                                        }
                                        return laneClear()
                                                .zipWith(walk())
                                                .flatMap(tupleLaneClearWalk -> {
                                                    boolean laneClearResult = tupleLaneClearWalk.getT1();
                                                    boolean walkQResult = tupleLaneClearWalk.getT2();
                                                    if (laneClearResult && walkQResult) {
                                                        return Mono.just(Boolean.TRUE);
                                                    }
                                                    return ezrealCastWtoTowers().zipWith(ezrealCastQtoMinions())
                                                            .map(tuplecastWCastQ -> {
                                                                boolean castWresult = tuplecastWCastQ.getT1();
                                                                boolean castQresult = tuplecastWCastQ.getT2();
                                                                return castWresult && castQresult;
                                                            });
                                                });
                                    });
                        } else {
                            return laneClear()
                                    .zipWith(walk())
                                    .map(tupleLaneClearWalk -> {
                                        boolean laneClearResult = tupleLaneClearWalk.getT1();
                                        boolean walkResult2 = tupleLaneClearWalk.getT2();
                                        // Aplica la lógica según tus necesidades
                                        return laneClearResult && walkResult2;
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
            if (this.canMoveTime < this.getTimer()) {
                this.gameTimeComponent.sleep(30);
                this.mouseService.mouseRightClickNoMove();
                return Mono.just(Boolean.TRUE);
            }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> attackTarget(){
        if (this.getTimer() - lastCast > 0.35){
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer =  championComponent.getLocalPlayer();
                        Double range = (double) localPlayer.getAttackRange();
                        double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) ;
                        return this.targetService.getBestChampionInRange(range)
                                .defaultIfEmpty(Champion.builder().build())
                                .flatMap(champion -> {
                                    if (this.canAttackTime < this.getTimer() && !ObjectUtils.isEmpty(champion.getPosition())) {
                                        this.canMoveTime = this.getTimer() + windUpTime;
                                        this.canCastTime = this.getTimer() + windUpTime;
                                        this.canAttackTime = this.getTimer() + 1.0 / attackSpeed;
                                        Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        this.mouseService.blockInput(true);
                                        this.mouseService.mouseMiddleDown();
                                        this.mouseService.mouseRightClick((int) position.getX(),(int) position.getY());
                                        this.gameTimeComponent.sleep(10);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.mouseService.mouseMiddleUp();
                                        this.mouseService.blockInput(false);
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    return Mono.just(Boolean.FALSE);
                                });
                    });
        }
        return Mono.just(Boolean.FALSE);
    }
    private Mono<Boolean> laneClear() {
        if (this.getTimer() - lastCast > 0.35) {
            return this.apiService.getJsonActivePlayer()
                    .flatMap(jsonActivePlayer -> Mono.just(jsonActivePlayer.championStats.getAttackSpeed()))
                    .flatMap(attackSpeed -> {
                        Champion localPlayer = championComponent.getLocalPlayer();
                        Double range = (double) localPlayer.getAttackRange();
                        double windUpTime = this.getWindUpTime(localPlayer.getJsonCommunityDragon().getAttackSpeed(), localPlayer.getJsonCommunityDragon().getWindUp(), localPlayer.getJsonCommunityDragon().getWindupMod(), attackSpeed) ;

                        Mono<Boolean> towerAttack = this.targetService.getBestTowerInRange(range)
                                .defaultIfEmpty(Tower.builder().build())
                                .flatMap(tower -> {
                                    if (this.canAttackTime < this.getTimer() && !ObjectUtils.isEmpty(tower.getPosition())) {
                                        this.canMoveTime = this.getTimer() + windUpTime;
                                        this.canCastTime = this.getTimer() + windUpTime;
                                        this.canAttackTime = this.getTimer() + 1.0 / attackSpeed;
                                        Vector2 position = this.rendererComponent.worldToScreen(tower.getPosition().getX(), tower.getPosition().getY(), tower.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        this.mouseService.blockInput(true);
                                        this.mouseService.mouseRightClick((int) position.getX(), (int) position.getY());
                                        this.gameTimeComponent.sleep(10);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.mouseService.blockInput(false);
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    return Mono.just(Boolean.FALSE);
                                });

                        Mono<Boolean> minionAttack = this.targetService.getBestMinionInRange(range)
                                .defaultIfEmpty(Minion.builder().build())
                                .flatMap(minion -> {
                                    if (this.canAttackTime < this.getTimer() && !ObjectUtils.isEmpty(minion.getPosition())) {
                                        this.canMoveTime = this.getTimer() + windUpTime;
                                        this.canCastTime = this.getTimer() + windUpTime;
                                        this.canAttackTime = this.getTimer() + 1.0 / attackSpeed;
                                        Vector2 position = this.rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                                        Vector2 mousePos = this.mouseService.getCursorPos();
                                        this.mouseService.blockInput(true);
                                        this.mouseService.mouseRightClick((int) position.getX(), (int) position.getY());
                                        this.gameTimeComponent.sleep(10);
                                        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                                        this.mouseService.blockInput(false);
                                        return Mono.just(Boolean.TRUE);
                                    }
                                    return Mono.just(Boolean.FALSE);
                                });

                        return towerAttack.zipWith(minionAttack)
                                .flatMap(tupleAttackWalk -> {
                                    boolean attackResult = tupleAttackWalk.getT1();
                                    boolean walkResult = tupleAttackWalk.getT2();
                                    return Mono.just(attackResult && walkResult);
                                });
                    });
        }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> castQ() {
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();
        int qLevel = spellBook.getQ().getLevel();

        if (canCast(qCoolDown, qLevel) && this.getTimer() - lastCast > 0.35) {
            Double spellRadiusQ = 120.0;
            Double spellDelayQ = 0.25;
            Double spellSpeedQ = 2000.0;
            Double spellRangeQ = 1200.0;

            // Use flatMap to work with the result of targetService.getPrediction
            return targetService.getPrediction(spellRangeQ, spellSpeedQ, spellDelayQ, spellRadiusQ)
                    .flatMap(predictedPosition -> {
                        this.canCastTime = this.getTimer() + spellDelayQ;
                        this.lastCast = this.getTimer();
                        //this.canMoveTime = this.getTimer() + spellDelayQ;

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
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double wCoolDown = spellBook.getW().getReadyAtSeconds();
        int wLevel = spellBook.getW().getLevel();

        if (canCast(wCoolDown, wLevel)  && this.getTimer() - lastCast > 0.35) {
            Double spellRadiusW = 160.0;
            Double spellDelayW = 0.25;
            Double spellSpeedW = 1700.0;
            Double spellRangeW = 1200.0;

            // Use flatMap to work with the result of targetService.getPrediction
            return targetService.getPrediction(spellRangeW, spellSpeedW, spellDelayW, spellRadiusW)
                    .flatMap(predictedPosition -> {
                        this.canCastTime = this.getTimer() + spellDelayW;
                        this.lastCast = this.getTimer();
                        //this.canMoveTime = this.canMoveTime + spellDelayW;

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

    private Mono<Boolean> ezrealCastWtoTowers() {
        //TODO: Laneclear
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();

        int wLevel = spellBook.getW().getLevel();
        //double wDamage = getEzrealDamageW(wLevel);
        if (canCast(qCoolDown, wLevel) && this.getTimer() - lastCast > 0.35) {
            Double spellDelayW = 0.25;
            Double spellRangeW = (double) localPlayer.getAttackRange();
            return this.targetService.getBestTowerInRange(spellRangeW)
                    .defaultIfEmpty(Tower.builder().build())
                    .flatMap(tower -> {
                        if (tower.getPosition() != null){
                            this.canCastTime = this.getTimer() + spellDelayW;
                            //this.canMoveTime = this.canMoveTime + spellDelayW;
                            this.lastCast = this.getTimer();
                            Vector3 localPlayerPosition = localPlayer.getPosition();
                            Vector2 towerPosition = rendererComponent.worldToScreen(tower.getPosition().getX(), tower.getPosition().getY(), tower.getPosition().getZ());
                            Vector2 screenLocalPlayerPosition = rendererComponent.worldToScreen(
                                    localPlayerPosition.getX(), localPlayerPosition.getY(), localPlayerPosition.getZ()
                            );
                            if (isValidPoint(towerPosition, screenLocalPlayerPosition, spellRangeW)) {
                                return Mono.just(cast(towerPosition, KeyEvent.VK_W));
                            } else {
                                return Mono.just(Boolean.FALSE);
                            }
                        }
                        return Mono.just(Boolean.FALSE);
                    });
        }

        return Mono.just(Boolean.TRUE);
    }
    private Mono<Boolean> ezrealCastQtoMinions() {
        //TODO: Laneclear
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();

        int qLevel = spellBook.getQ().getLevel();
        double qDamage = getEzrealDamageQ(qLevel);
        if (canCast(qCoolDown, qLevel) && this.getTimer() - lastCast > 0.35) {
            Double spellDelayQ = 0.25;
            Double spellRangeQ = 1200.0;
            return this.targetService.getMinionToLastHitBySpell(spellRangeQ, qDamage)
                                        .defaultIfEmpty(Minion.builder().build())
                                        .flatMap(minion -> {
                                            if (minion.getPosition() != null){
                                                this.canCastTime = this.getTimer() + spellDelayQ;
                                                //this.canMoveTime = this.canMoveTime + spellDelayQ;
                                                this.lastCast = this.getTimer();
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
        this.mouseService.blockInput(true);
        this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
        this.keyboardService.sendKeyDown(key);
        this.keyboardService.sendKeyUp(key);
        this.gameTimeComponent.sleep(15);
        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
        this.mouseService.blockInput(false);

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
        //return (1.0 / cAttackSpeed) * ((windupMod != 0) ? (windup / windupMod) :windup));
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
    private boolean canCast(double coolDown, int level) {
        return this.canCastTime + 0.15 < this.getTimer() &&
                this.getTimer() - coolDown > 0 &&
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
    private Double getTimer(){
        return System.nanoTime() / 1_000_000_000.0;
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
//    private Double getEzrealDamageW(int wLvl){
//        Champion localPlayer =  championComponent.getLocalPlayer();
//        return switch (wLvl) {
//            case 1 ->
//                    80 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 0.6) + ((localPlayer.getAbilityPower()) * 0.7);
//            case 2 ->
//                    135 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 0.6) + ((localPlayer.getAbilityPower()) * 0.75);
//            case 3 ->
//                    190 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 0.6) + ((localPlayer.getAbilityPower()) * 0.80);
//            case 4 ->
//                    245 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 0.6) + ((localPlayer.getAbilityPower()) * 0.85);
//            case 5 ->
//                    300 + ((localPlayer.getBaseAttack() + localPlayer.getBonusAttack()) * 0.6) + ((localPlayer.getAbilityPower()) * 0.90);
//            default -> 0.0;
//        };
//    }
}
