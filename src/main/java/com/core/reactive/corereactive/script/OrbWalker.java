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
import com.core.reactive.corereactive.util.api.object.JsonActivePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
@Getter
@Setter
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
    private Double lastAttack = 0.0000000000;

    @Override
    public Mono<Boolean> update() {
        if (!this.championComponent.getLocalPlayer().getIsAlive()) {
            return Mono.just(Boolean.TRUE);
        }
        if (isVkSpacePressed()) {
            this.keepKeyOPressed();
            return Mono.just(Boolean.TRUE.equals(attackTarget().block()));
        } else if (this.isVkVPressed()) {
                this.keepKeyOPressed();
                boolean attackTargetResult = Boolean.TRUE.equals(attackTarget().block());
                if (attackTargetResult) {
                    return Mono.just(Boolean.TRUE);
                }
                boolean laneClearResult = Boolean.TRUE.equals(laneClear().block());
                return Mono.just(laneClearResult);
        }
        return Mono.defer(() -> {
            this.keyboardService.sendKeyUp(KeyEvent.VK_O);
            return Mono.just(Boolean.TRUE);
        });
    }
    private Mono<Boolean> attackTarget() {
        if (this.gameTimeComponent.getGameTime() - this.lastCast > 0.35) {
            JsonActivePlayer jsonActivePlayer = this.apiService.getJsonActivePlayer().block();
            if (jsonActivePlayer != null) {
                double attackSpeed = jsonActivePlayer.championStats.getAttackSpeed();
                Champion localPlayer = this.championComponent.getLocalPlayer();
                Double range = (double) localPlayer.getAttackRange();
                double windUpTime = this.getWindUpTime(
                        localPlayer.getJsonCommunityDragon().getAttackSpeed(),
                        localPlayer.getJsonCommunityDragon().getWindUp(),
                        localPlayer.getJsonCommunityDragon().getWindupMod(),
                        attackSpeed
                );
                Champion champion = this.targetService.getBestChampionInRange(range).defaultIfEmpty(Champion.builder().build()).block();
                if (champion != null && this.canAttackTime < this.gameTimeComponent.getGameTime() && !ObjectUtils.isEmpty(champion.getPosition())) {
                    Vector2 position = this.rendererComponent.worldToScreen(champion.getPosition().getX(), champion.getPosition().getY(), champion.getPosition().getZ());
                    Vector2 mousePos = this.mouseService.getCursorPos();
                    this.mouseService.clipCursor((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.blockInput(true);
                    this.mouseService.mouseMiddleDown();
                    this.mouseService.mouseRightClick((int) position.getX(), (int) position.getY());
                    this.gameTimeComponent.sleep(10);
                    this.mouseService.releaseCursor();
                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.mouseMiddleUp();
                    this.mouseService.blockInput(false);
                    this.canMoveTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canCastTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canAttackTime = this.gameTimeComponent.getGameTime() + (1.0 / attackSpeed) + 33.0/2000.0;
                    this.lastAttack = this.gameTimeComponent.getGameTime();
                    return Mono.just(Boolean.TRUE);
                }
            }
        }
        if (this.canMoveTime < this.gameTimeComponent.getGameTime()) {
            this.mouseService.mouseRightClickNoMove();
            this.canMoveTime = this.gameTimeComponent.getGameTime() + 0.03;
            return Mono.just(Boolean.TRUE);
        }
        return Mono.just(Boolean.FALSE);
    }

    private Mono<Boolean> laneClear() {
        if (this.gameTimeComponent.getGameTime() - this.lastCast > 0.35) {
            JsonActivePlayer jsonActivePlayer = this.apiService.getJsonActivePlayer().block();
            if (jsonActivePlayer != null) {
                double attackSpeed = jsonActivePlayer.championStats.getAttackSpeed();
                Champion localPlayer = championComponent.getLocalPlayer();
                Double range = (double) localPlayer.getAttackRange();
                double windUpTime = this.getWindUpTime(
                        localPlayer.getJsonCommunityDragon().getAttackSpeed(),
                        localPlayer.getJsonCommunityDragon().getWindUp(),
                        localPlayer.getJsonCommunityDragon().getWindupMod(),
                        attackSpeed
                );

                Tower tower = this.targetService.getBestTowerInRange(range).defaultIfEmpty(Tower.builder().build()).block();
                if (tower != null && this.canAttackTime < this.gameTimeComponent.getGameTime() && !ObjectUtils.isEmpty(tower.getPosition())) {
                    Vector2 position = this.rendererComponent.worldToScreen(tower.getPosition().getX(), tower.getPosition().getY(), tower.getPosition().getZ());
                    Vector2 mousePos = this.mouseService.getCursorPos();
                    this.mouseService.clipCursor((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.blockInput(true);
                    this.mouseService.mouseRightClick((int) position.getX(), (int) position.getY());
                    this.gameTimeComponent.sleep(10);
                    this.mouseService.releaseCursor();
                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.blockInput(false);
                    this.canMoveTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canCastTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canAttackTime = this.gameTimeComponent.getGameTime() + (1.0 / attackSpeed) + 33.0/2000.0;
                    return Mono.just(Boolean.TRUE);
                }

                Minion minion = this.targetService.getBestMinionInRange(range).defaultIfEmpty(Minion.builder().build()).block();
                if (minion != null && this.canAttackTime < this.gameTimeComponent.getGameTime() && !ObjectUtils.isEmpty(minion.getPosition())) {
                    Vector2 position = this.rendererComponent.worldToScreen(minion.getPosition().getX(), minion.getPosition().getY(), minion.getPosition().getZ());
                    Vector2 mousePos = this.mouseService.getCursorPos();
                    this.mouseService.clipCursor((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.blockInput(true);
                    this.mouseService.mouseRightClick((int) position.getX(), (int) position.getY());
                    this.gameTimeComponent.sleep(10);
                    this.mouseService.releaseCursor();
                    this.mouseService.mouseMove((int) mousePos.getX(), (int) mousePos.getY());
                    this.mouseService.blockInput(false);
                    this.canMoveTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canCastTime = this.gameTimeComponent.getGameTime() + windUpTime;
                    this.canAttackTime = this.gameTimeComponent.getGameTime() + (1.0 / attackSpeed) + 33.0/2000.0;
                    return Mono.just(Boolean.TRUE);
                }

                return Mono.just(Boolean.FALSE);
            }
        }

        if (this.canMoveTime < this.gameTimeComponent.getGameTime()) {
            this.mouseService.mouseRightClickNoMove();
            this.canMoveTime = this.gameTimeComponent.getGameTime() + 0.03;
            return Mono.just(Boolean.TRUE);
        }
        return Mono.just(Boolean.FALSE);
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
}
