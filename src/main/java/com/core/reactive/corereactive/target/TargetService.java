package com.core.reactive.corereactive.target;

import com.core.reactive.corereactive.component.renderer.RendererComponent;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.component.unitmanager.impl.ChampionComponent;
import com.core.reactive.corereactive.component.unitmanager.impl.MinionComponent;
import com.core.reactive.corereactive.component.unitmanager.model.AiManager;
import com.core.reactive.corereactive.component.unitmanager.model.Champion;
import com.core.reactive.corereactive.component.unitmanager.model.Minion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;
    private final RendererComponent rendererComponent;

    Vector3 getObjectPositionAfterTime(Champion obj, double time, double distanceBuffer) {
        AiManager aiManager = obj.getAiManager();
        double speed = aiManager.getMoveSpeed();

        if (aiManager.getIsDashing()) {
            speed = aiManager.getDashSpeed();
        }

        List<Vector3> waypoints = new ArrayList<>();
        waypoints.add(aiManager.getServerPos());

        List<Vector3> futureWaypoints = getFuturePoints(obj);
        waypoints.addAll(futureWaypoints);

        int waypointsSize = waypoints.size();

        if (time == 0 || !aiManager.getIsMoving()) {
            return aiManager.getServerPos();
        }

        double distance = (speed * time) - distanceBuffer;

        for (int i = 1; i < waypointsSize; i++) {
            double wayDistance = this.distanceBetweenTargets(waypoints.get(i - 1), waypoints.get(i));
            if (wayDistance >= distance) {
                return addVector3(waypoints.get(i - 1), scaleVector3(normalizeVector3(subtractVector3(waypoints.get(i),waypoints.get(i - 1))), distance));
            }
            if (i == waypointsSize - 1) {
                return waypoints.get(i);
            }
            distance = distance - wayDistance;
        }
        return waypoints.get(0);
    }


    boolean isSpecificObjectInWay(Vector3 sourcePos, Vector3 targetPos, Minion collisionObject, double projectileRadius) {
        Vector3 sourceToTarget = subtractVector3(targetPos,sourcePos);
        sourceToTarget.setY(0.0f);
        float distance = length(sourceToTarget);

        Vector3 objPos = collisionObject.getPosition();
        Vector3 sourceToObj = subtractVector3(objPos,sourcePos);
        sourceToObj.setY(0.0f);
        if (length(sourceToObj) > distance) {
            return false;
        }

        float dot1 = sourceToObj.getX() * sourceToTarget.getX() + sourceToObj.getZ() * sourceToTarget.getZ();
        float dot2 = sourceToTarget.getX() * sourceToTarget.getX() + sourceToTarget.getZ() * sourceToTarget.getZ();

        if (dot1 < 0.0f) {
            return false;
        }

        float t = dot1 / dot2;

        Vector3 projection = addVector3(sourcePos, Vector3.builder()
                .x(sourceToTarget.getX() * t)
                .y(0.0f)
                .z(sourceToTarget.getZ() * t)
                .build());
        projection.setY(0.0f);

        Vector3 distVector = subtractVector3(objPos, projection);
        distVector.setY(0.0f);

        return length(distVector) <= projectileRadius + 48;
    }

    float length(Vector3 pos) {
        return (float) Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY() + pos.getZ() * pos.getZ());
    }

    boolean isAnyObjectInWay(Vector3 sourcePos, Vector3 targetPos, Champion sourceObject, double projectileRadius) {
            Vector3 sourceToTarget = subtractVector3(targetPos, sourcePos);
            sourceToTarget.setY(0.0f);

            for (Minion minion : this.minionComponent.getMapUnit().values()) {
                if (!minion.getIsTargeteable()){
                    continue;
                }
                if (!minion.getIsVisible()) {
                    continue;
                }
                if (!minion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(minion.getTeam(), sourceObject.getTeam())) {
                    continue;
                }
                if (isSpecificObjectInWay(sourcePos, targetPos, minion, projectileRadius)) {
                    return true;
                }
            }
            return false;
    }

    boolean checkCollision(Vector3 sourcePos, Vector3 targetPos, Champion sourceObject, Double spellRadius) {
        return !isAnyObjectInWay(sourcePos, targetPos, sourceObject, spellRadius);
    }

    public Mono<Vector2> getPrediction(Double spellRange, Double spellSpeed, Double spellDelay, Double spellRadius) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        return Mono.fromCallable(() -> {
            for (Champion champion : this.championComponent.getMapUnit().values()) {
                if (!champion.getIsTargeteable()){
                    continue;
                }
                if (!champion.getIsVisible()) {
                    continue;
                }
                if (!champion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(champion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) <= spellRange-champion.getJsonCommunityDragon().getGameplayRadius();
                if (inDistance){
                    Vector3 sourcePos = localPLayer.getAiManager().getServerPos();
                    AiManager targetAiManager = champion.getAiManager();
                    double distance = this.distanceBetweenTargets(sourcePos, targetAiManager.getServerPos());
                    if (distance > spellRange){
                        return null;
                    }
                    List<Vector3> waypoints = getFuturePoints(champion);
                    int waypointsSize = waypoints.size();
                    if (waypointsSize <= 0 || !targetAiManager.getIsMoving())
                    {
                        Vector3 position = targetAiManager.getServerPos();
                        if (checkCollision(sourcePos, position, localPLayer, spellRadius)){
                            return this.rendererComponent.worldToScreen(position.getX(), position.getY(), position.getZ());
                        }
                    }
                    double travelTime = (distance / spellSpeed) + spellDelay;
                    Vector3 predictedPos = getObjectPositionAfterTime(champion, travelTime, 0.0);
                    distance = this.distanceBetweenTargets(predictedPos, sourcePos);
                    double missileTime = (distance / spellSpeed) + spellDelay;

                    while (Math.abs(travelTime - missileTime) > 0.01) {
                        travelTime = missileTime;
                        predictedPos = getObjectPositionAfterTime(champion, travelTime, 0.0f);

                        distance = distanceBetweenTargets(predictedPos, sourcePos);
                        if (distance > spellRange) {
                            return null;
                        }
                        missileTime = (distance / spellSpeed) + spellDelay;
                    }
                    if (checkCollision(sourcePos, predictedPos, localPLayer, spellRadius)){
                        return this.rendererComponent.worldToScreen(predictedPos.getX(), predictedPos.getY(), predictedPos.getZ());
                    }
                }
            }
            return null;
        });
    }

    public Mono<Vector2> getBestChampionInSpell(Double spellRange, Double spellSpeed, Double spellDelay, Double spellRadius) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        return Mono.fromCallable(() -> {
            for (Champion champion : this.championComponent.getMapUnit().values()) {
                if (!champion.getIsTargeteable()){
                    continue;
                }
                if (!champion.getIsVisible()) {
                    continue;
                }
                if (!champion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(champion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) < spellRange-champion.getJsonCommunityDragon().getGameplayRadius();
                if (inDistance){
                    AiManager ai = champion.getAiManager();
                    AiManager ailocal = localPLayer.getAiManager();
                    int ping = 33;
                    double flytimeMax = spellRange / spellSpeed;
                    double tMin = spellDelay + (double) ping / 2000.0;
                    double tMax = flytimeMax + spellDelay + (double) ping / 1000.0;
                    List<Vector3> waypoints = getFuturePoints(champion);
                    int pathSize = waypoints.size();
                    int[] pathBounds = {-1, -1};
                    double pathTime = 0.0;
                    double targetMoveSpeed = ai.getIsDashing() ? (double) ai.getDashSpeed() : (double) ai.getMoveSpeed();
                    if (!ai.getIsMoving()) {
                        Vector3 servPos = ai.getServerPos();
                        return this.rendererComponent.worldToScreen(servPos.getX(), servPos.getY(), servPos.getZ());
                    }
                    for (int i = 0; i < pathSize - 1; i++) {
                        Vector3 curVector = waypoints.get(i);
                        Vector3 nextVector = waypoints.get(i + 1);
                        double t = distanceBetweenTargets2D(
                                this.rendererComponent.worldToScreen(nextVector.getX(), nextVector.getY(), nextVector.getZ())
                                ,this.rendererComponent.worldToScreen(curVector.getX(), curVector.getY(), curVector.getZ())
                        );
                        if (pathTime <= tMin && pathTime + t >= tMin) {
                            pathBounds[0] = i;
                        }
                        if (pathTime <= tMax && pathTime + t >= tMax) {
                            pathBounds[1] = i;
                        }
                        if (pathBounds[0] >= 0 && pathBounds[1] >= 0) {
                            break;
                        }
                        pathTime += t;
                    }
                    if (pathBounds[0] >= 0 && pathBounds[1] >= 0) {
                        int currPathIndex = pathBounds[0];
                        while (true) {
                            Vector3 curVector = waypoints.get(currPathIndex);
                            Vector3 nextVector = waypoints.get(currPathIndex + 1);
                            Vector3 direction = normalizeVector3(subtractVector3(nextVector, curVector));
                            double extender = champion.getJsonCommunityDragon().getGameplayRadius();
                            double distance = spellRadius;
                            int steps = (int) (this.distanceBetweenTargets(curVector, nextVector) / distance);
                            if (0 < steps && steps < 1000) {
                                for (int i = 1; i < steps - 1; i++) {
                                    Vector3 center = addVector3(curVector, scaleVector3(direction, distance * i));
                                    Vector3 ptA = subtractVector3(center, scaleVector3(direction, extender));
                                    Vector3 ptB = addVector3(center, scaleVector3(direction, extender));
                                    Vector3 targetServPosVector = Vector3.builder()
                                            .x(ai.getServerPos().getX())
                                            .y(ai.getServerPos().getY())
                                            .z(ai.getServerPos().getZ())
                                            .build();

                                    Vector3 predictedPos = addVector3(center, scaleVector3(normalizeVector3(ai.getVelocity()), extender));
                                    double flytime = this.distanceBetweenTargets(ailocal.getServerPos(), ai.getServerPos()) / spellSpeed;
                                    double t = flytime + spellDelay + (double) ping / 2000.0;
                                    double arriveTimeA = this.distanceBetweenTargets(targetServPosVector, ptA) / targetMoveSpeed;
                                    double arriveTimeB = this.distanceBetweenTargets(targetServPosVector, ptB) / targetMoveSpeed;
                                    if (Math.min(arriveTimeA, arriveTimeB) <= t && t <= Math.max(arriveTimeA, arriveTimeB)) {
                                        return this.rendererComponent.worldToScreen(predictedPos.getX(), predictedPos.getY(), predictedPos.getZ());
                                    }
                                }
                            }
                            if (currPathIndex == pathBounds[1]) {
                                break;
                            }
                            currPathIndex++;
                        }
                    }
                }
            }
            return null;
        });
    }
    public Mono<Champion> getBestChampionInRange(Double range) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        return Mono.fromCallable(() -> {
            double minAutos = 0.0;
            Champion championFinal = Champion.builder().build();
            for (Champion champion : this.championComponent.getMapUnit().values()) {
                if (!champion.getIsTargeteable()){
                    continue;
                }
                if (!champion.getIsVisible()) {
                    continue;
                }
                if (!champion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(champion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) - champion.getJsonCommunityDragon().getGameplayRadius() <= range + localPLayer.getJsonCommunityDragon().getGameplayRadius();
                if (inDistance){
                    Double minAttacks = getMinAttacks(
                            (double) localPLayer.getBaseAttack(),
                            (double) localPLayer.getBonusAttack(),
                            (double) champion.getHealth(),
                            (double) champion.getArmor()
                    );
                    if (minAttacks < minAutos || minAutos == 0.0){
                        minAutos = minAttacks;
                        championFinal = champion;
                    }
                }
            }
            return championFinal;
        });
    }

    public Mono<Minion> getBestMinionInRange(Double range) {
        return Mono.fromCallable(() -> {
            Champion localPLayer = this.championComponent.getLocalPlayer();
            double minAutos = 0.0;
            Minion minionFinal = Minion.builder().build();
            for (Minion minion : this.minionComponent.getMapUnit().values()) {
                if (!minion.getIsTargeteable()){
                    continue;
                }
                if (!minion.getIsVisible()) {
                    continue;
                }
                if (!minion.getIsAlive()) {
                    continue;
                }
                if (Objects.equals(minion.getTeam(), localPLayer.getTeam())) {
                    continue;
                }
                boolean inDistance = (this.distanceBetweenTargets(localPLayer.getPosition(), minion.getPosition())).compareTo(range + localPLayer.getJsonCommunityDragon().getGameplayRadius()) < 0;
                if (inDistance){
                    Double minAttacks = getMinAttacks((double) localPLayer.getBaseAttack(), (double) localPLayer.getBonusAttack(), (double) minion.getHealth(), (double) minion.getArmor());
                    if (minAttacks.compareTo(minAutos) < 0 || minAutos == 0.0){
                        minAutos = minAttacks;
                        minionFinal = minion;
                    }
                }
            }
            return minionFinal;
        });
    }

    private Double distanceBetweenTargets(Vector3 position, Vector3 position2) {
        Double xDiff = (double) Math.abs(position.getX() - position2.getX());
        Double yDiff = (double) Math.abs(position.getY() - position2.getY());
        Double zDiff = (double) Math.abs(position.getZ() - position2.getZ());

        double sumOfSquares = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;

        return Math.sqrt(sumOfSquares);
    }

    private Double distanceBetweenTargets2D(Vector2 vector1, Vector2 vector2) {
        Double xDiff = (double) Math.abs(vector1.getX() - vector2.getX());
        Double yDiff = (double) Math.abs(vector1.getY() - vector2.getY());

        double sumOfSquares = xDiff * xDiff + yDiff * yDiff;

        return Math.sqrt(sumOfSquares);
    }

    private Vector3 normalizeVector3(Vector3 vector) {
        double length = Math.sqrt(vector.getX() * vector.getX() + vector.getY() * vector.getY() + vector.getZ() * vector.getZ());
        if (length != 0) {
            return Vector3.builder()
                    .x((float) (vector.getX() / length))
                    .y((float) (vector.getY() / length))
                    .z((float) (vector.getZ() / length))
                    .build();
        } else {
            return Vector3.builder()
                    .x(0)
                    .y(0)
                    .z(0)
                    .build();
        }
    }

    private Vector3 scaleVector3(Vector3 vector, double factor) {
        return Vector3.builder()
                .x((float) (vector.getX() * factor))
                .y((float) (vector.getY() * factor))
                .z((float) (vector.getZ() * factor))
                .build();
    }

    private Vector3 addVector3(Vector3 position1, Vector3 position2) {
        return Vector3.builder()
                .x(position1.getX() + position2.getX())
                .y(position1.getY() + position2.getY())
                .z(position1.getZ() + position2.getZ())
                .build();
    }

    private Vector3 subtractVector3(Vector3 position1, Vector3 position2) {
        return Vector3.builder()
                .x(position1.getX() - position2.getX())
                .y(position1.getY() - position2.getY())
                .z(position1.getZ() - position2.getZ())
                .build();
    }

    private List<Vector3> getFuturePoints(Champion target) {
        List<Vector3> futurePoints = new ArrayList<>();
//        Vector3 targetServerPos = target.getAiManager().getServerPos();
        futurePoints.add(target.getAiManager().getServerPos());
        List<Vector3> waypoints = target.getAiManager().getWaypoints().getNavigationPath();
        int segmentsCount = target.getAiManager().getWaypoints().getCurrentSize();
        int currentSegments = target.getAiManager().getWaypoints().getPassedWaypoint();
        if (waypoints.isEmpty()) {
            return futurePoints;
        }

        for (int i = currentSegments; i < segmentsCount; i++) {
//            Vector3 waypoint = waypoints.get(i);
            futurePoints.add(waypoints.get(i));
//            double dist1 = this.distanceBetweenTargets(targetServerPos, waypoint);
//            double dist2 = this.distanceBetweenTargets(targetServerPos, waypoints.get(i - 1));
//            double dist3 = this.distanceBetweenTargets(waypoints.get(i - 1), waypoint);
//
//            if (Math.abs(dist1 + dist2 - dist3) <= 20.0) {
//                futurePoints.add(waypoint);
//                break;
//            }

//            futurePoints.add(waypoint);
        }

        return futurePoints;
    }

    private Double getMinAttacks(Double playerBasicAttack, Double playerBonusAttack, Double targetHealth, Double targetArmor) {
        Double effectiveDamage = getEffectiveDamage(playerBasicAttack + playerBonusAttack, targetArmor);

        if (effectiveDamage > 0.0) {
            return targetHealth / effectiveDamage;
        } else {
            // Handle the case where effective damage is non-positive (division by zero or negative damage).
            return 0.0;
        }
    }

    private Double getEffectiveDamage(Double damage, Double armor) {
        Double oneHundred = 100.0;

        if (armor >= 0.0) {
            double divisor = oneHundred + armor;
            return (damage * oneHundred) / divisor;
        } else {
            Double divisor = oneHundred - armor;
            return (damage * 2.0) - (oneHundred / divisor);
        }
    }

}
