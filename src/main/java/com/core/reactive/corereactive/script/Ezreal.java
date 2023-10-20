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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.awt.event.KeyEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class Ezreal implements ScriptLoaderService{
    private final ChampionComponent championComponent;
    private final GameTimeComponent gameTimeComponent;
    private final MouseService mouseService;
    private final RendererComponent rendererComponent;
    private final KeyboardService keyboardService;
    private final TargetService targetService;
    private final OrbWalker owService;
    @Override
    public Mono<Boolean> update() {
        if ("Ezreal".equals(championComponent.getLocalPlayer().getName())) {
            if (!this.championComponent.getLocalPlayer().getIsAlive()) {
                return Mono.just(Boolean.TRUE);
            }
            boolean killStealR = Boolean.TRUE.equals(killStealWithR().block());
            if (killStealR) {
                return Mono.just(Boolean.TRUE);
            }
            boolean killStealQ = Boolean.TRUE.equals(killStealWithQ().block());
            if (killStealQ) {
                return Mono.just(Boolean.TRUE);
            }
            if (isVkSpacePressed()) {
                    boolean castWResult = Boolean.TRUE.equals(castW().block());
                    boolean castQResult = Boolean.TRUE.equals(castQ().block());
                    return Mono.just(castWResult && castQResult);
            } else if (this.isVkVPressed()) {
                    boolean castWResult = Boolean.TRUE.equals(castW().block());
                    boolean castQResult = Boolean.TRUE.equals(castQ().block());
                    if (castWResult && castQResult) {
                        return Mono.just(Boolean.TRUE);
                    }
                    boolean castWtoTowersResult = Boolean.TRUE.equals(ezrealCastWtoTowers().block());
                    boolean castQtoMinionsResult = Boolean.TRUE.equals(ezrealCastQtoMinions().block());
                    return Mono.just(castWtoTowersResult && castQtoMinionsResult);
            }
        }
        return Mono.defer(() -> {
            return Mono.just(Boolean.TRUE);
        });
    }

    private Mono<Boolean> castQ() {
        Champion localPlayer = championComponent.getLocalPlayer();
        if (localPlayer.getMana() < 50){
            return Mono.just(Boolean.FALSE);
        }
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();
        int qLevel = spellBook.getQ().getLevel();
        if (canCast(qCoolDown, qLevel)) {
            Double spellRadiusQ = 120.0;
            Double spellDelayQ = 0.25;
            Double spellSpeedQ = 2000.0;
            Double spellRangeQ = 1150.0;
            return targetService.getPrediction(spellRangeQ, spellSpeedQ, spellDelayQ, spellRadiusQ)
                    .flatMap(predictedPosition -> {
                        owService.setCanCastTime(this.getTimer() + spellDelayQ + 40.0/2000.0);
                        owService.setLastCast(this.getTimer());
                        if (isValidPoint(predictedPosition)) {
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
        if (localPlayer.getMana() < 50){
            return Mono.just(Boolean.FALSE);
        }
        SpellBook spellBook = localPlayer.getSpellBook();
        double wCoolDown = spellBook.getW().getReadyAtSeconds();
        int wLevel = spellBook.getW().getLevel();
        if (canCast(wCoolDown, wLevel)) {
            Double spellRadiusW = 160.0;
            Double spellDelayW = 0.25;
            Double spellSpeedW = 1700.0;
            Double spellRangeW = 1200.0;
            return targetService.getPrediction(spellRangeW, spellSpeedW, spellDelayW, spellRadiusW)
                    .flatMap(predictedPosition -> {
                        owService.setCanCastTime(this.getTimer() + spellDelayW + 40.0/2000.0);
                        owService.setLastCast(this.getTimer());
                        if (isValidPoint(predictedPosition)) {
                            return Mono.just(cast(predictedPosition, KeyEvent.VK_W));
                        } else {
                            return Mono.just(Boolean.FALSE);
                        }
                    })
                    .defaultIfEmpty(Boolean.FALSE);
        }

        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> killStealWithR() {
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double rCoolDown = spellBook.getR().getReadyAtSeconds();
        int rLevel = spellBook.getR().getLevel();
        if (localPlayer.getMana() < 100 || rLevel < 1){
            return Mono.just(Boolean.FALSE);
        }
        if (canCast(rCoolDown, rLevel)) {
            Double spellRadiusR = 320.0;
            Double spellDelayR = 1.00;
            Double spellSpeedR = 2000.0;
            Double spellRangeR = 3000.0;//max Distance
            Double spellDamageR = getEzrealDamageR(rLevel);
            return targetService.getKSPrediction(spellRangeR, spellSpeedR, spellDelayR, spellRadiusR, spellDamageR, 1)
                    .flatMap(predictedPosition -> {
                        owService.setCanCastTime(this.getTimer() + spellDelayR + 40.0/2000.0);
                        owService.setLastCast(this.getTimer());
                        if (isValidPoint(predictedPosition)) {
                            return Mono.just(cast(predictedPosition, KeyEvent.VK_R));
                        } else {
                            return Mono.just(Boolean.FALSE);
                        }
                    })
                    .defaultIfEmpty(Boolean.FALSE);
        }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> killStealWithQ() {
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();
        int qLevel = spellBook.getQ().getLevel();
        if (localPlayer.getMana() < 50 || qLevel < 1){
            return Mono.just(Boolean.FALSE);
        }
        if (canCast(qCoolDown, qLevel)) {
            Double spellRadiusQ = 120.0;
            Double spellDelayQ = 0.25;
            Double spellSpeedQ = 2000.0;
            Double spellRangeQ = 1150.0;
            Double spellDamageQ = getEzrealDamageQ(qLevel);
            return targetService.getKSPrediction(spellRangeQ, spellSpeedQ, spellDelayQ, spellRadiusQ, spellDamageQ, 0)
                    .flatMap(predictedPosition -> {
                        owService.setCanCastTime(this.getTimer() + spellDelayQ + 40.0/2000.0);
                        owService.setLastCast(this.getTimer());
                        if (isValidPoint(predictedPosition)) {
                            return Mono.just(cast(predictedPosition, KeyEvent.VK_Q));
                        } else {
                            return Mono.just(Boolean.FALSE);
                        }
                    })
                    .defaultIfEmpty(Boolean.FALSE);
        }
        return Mono.just(Boolean.FALSE);
    }
    private Mono<Boolean> ezrealCastWtoTowers() {
        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double wCoolDown = spellBook.getW().getReadyAtSeconds();

        int wLevel = spellBook.getW().getLevel();
        //double wDamage = getEzrealDamageW(wLevel);
        if (canCast(wCoolDown, wLevel)) {
            Double spellDelayW = 0.25;
            Double spellRangeW = (double) localPlayer.getAttackRange();
            return this.targetService.getBestTowerInRange(spellRangeW)
                    .defaultIfEmpty(Tower.builder().build())
                    .flatMap(tower -> {
                        if (tower.getPosition() != null){
                            owService.setCanCastTime(this.getTimer() + spellDelayW + 40.0/2000.0);
                            owService.setLastCast(this.getTimer());
                            Vector2 towerPosition = rendererComponent.worldToScreen(tower.getPosition().getX(), tower.getPosition().getY(), tower.getPosition().getZ());
                            if (isValidPoint(towerPosition)) {
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

        Champion localPlayer = championComponent.getLocalPlayer();
        SpellBook spellBook = localPlayer.getSpellBook();
        double qCoolDown = spellBook.getQ().getReadyAtSeconds();

        int qLevel = spellBook.getQ().getLevel();
        double qDamage = getEzrealDamageQ(qLevel);
        if (canCast(qCoolDown, qLevel)) {
            Double spellDelayQ = 0.25;
            Double spellRangeQ = 1150.0;
            return this.targetService.getMinionToLastHitBySpell(spellRangeQ, qDamage)
                    .defaultIfEmpty(Minion.builder().build())
                    .flatMap(minion -> {
                        if (minion.getPosition() != null){
                            owService.setCanCastTime(this.getTimer() + spellDelayQ + 40.0/2000.0);
                            owService.setLastCast(this.getTimer());
                            Vector2 minionPosition = rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                            if (isValidPoint(minionPosition)) {
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
        this.mouseService.blockInput(true);
        Vector2 mousePos = mouseService.getCursorPos();
        this.mouseService.clipCursor((int) mousePos.getX(), (int) mousePos.getY());
        this.mouseService.mouseMove((int) predictedPosition.getX(), (int) predictedPosition.getY());
        this.keyboardService.sendKeyDown(key);
        this.keyboardService.sendKeyUp(key);
        this.gameTimeComponent.sleep(15);
        this.mouseService.releaseCursor();
        this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
        this.mouseService.blockInput(false);

        // Return true for success, or false for failure
        return true;
    }
    private boolean isVkSpacePressed() {
        return this.keyboardService.isKeyDown(KeyEvent.VK_SPACE);
    }
    private boolean isVkVPressed() {
        return this.keyboardService.isKeyDown(KeyEvent.VK_V);
    }
    private boolean canCast(double coolDown, int level) {
        return owService.getCanCastTime() + 0.11 < this.getTimer() &&
                this.getTimer() - coolDown > 0 &&
                level > 0 &&
                this.getTimer() - owService.getLastAttack() > 0.2;
    }
    private boolean isValidPoint(Vector2 predictedPosition) {
        return predictedPosition != null;
    }

    private Double getEzrealDamageQ(int qLvl){
        //This ability hits on Physical Damage
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
    private Double getEzrealDamageW(int wLvl){
        //This ability hits on Magic Damage
        Champion localPlayer =  championComponent.getLocalPlayer();
        return switch (wLvl) {
            case 1 ->
                    80 + (localPlayer.getBonusAttack() * 0.6) + ((localPlayer.getAbilityPower()) * 0.7);
            case 2 ->
                    135 + (localPlayer.getBonusAttack() * 0.6) + ((localPlayer.getAbilityPower()) * 0.75);
            case 3 ->
                    190 + (localPlayer.getBonusAttack() * 0.6) + ((localPlayer.getAbilityPower()) * 0.80);
            case 4 ->
                    245 + (localPlayer.getBonusAttack() * 0.6) + ((localPlayer.getAbilityPower()) * 0.85);
            case 5 ->
                    300 + (localPlayer.getBonusAttack() * 0.6) + ((localPlayer.getAbilityPower()) * 0.90);
            default -> 0.0;
        };
    }

    private Double getEzrealDamageR(int rLvl){
        //This ability hits on Magic Damage
        Champion localPlayer =  championComponent.getLocalPlayer();
        return switch (rLvl) {
            case 1 ->
                    350 + (localPlayer.getBonusAttack() * 1.0) + ((localPlayer.getAbilityPower()) * 0.9);
            case 2 ->
                    500 + (localPlayer.getBonusAttack() * 1.0) + ((localPlayer.getAbilityPower()) * 0.9);
            case 3 ->
                    650 + (localPlayer.getBonusAttack() * 1.0) + ((localPlayer.getAbilityPower()) * 0.9);
            default -> 0.0;
        };
    }
    private Double getTimer(){
        return System.nanoTime() / 1_000_000_000.0;
    }
}
