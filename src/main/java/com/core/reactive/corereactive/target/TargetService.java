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

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetService {
    private final ChampionComponent championComponent;
    private final MinionComponent minionComponent;
    private final RendererComponent rendererComponent;

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
                Double targetGameplayRadius = (champion.getJsonCommunityDragon() != null) ? champion.getJsonCommunityDragon().getGameplayRadius() : 65.0;
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) <= spellRange-targetGameplayRadius;
                if (inDistance){
                    List<Vector3> waypoints = getFuturePoints(champion);
                    if (waypoints.size()<=1 || !champion.getAiManager().getIsMoving()){
                        if (checkCollision(localPLayer.getPosition(), waypoints.get(0), localPLayer, spellRadius)){
                            return this.rendererComponent.worldToScreen(waypoints.get(0).getX(), waypoints.get(0).getY(), waypoints.get(0).getZ());
                        }
                    }
                    double travelTime = (distanceBetweenTargets(champion.getPosition(), localPLayer.getPosition()) / spellSpeed) + spellDelay /*+ spell->chanelingTime*/;
                    Vector3 predictedPos = posAfterTime(champion, travelTime, spellRadius);
                    double distanceMissile = distanceBetweenTargets(predictedPos, localPLayer.getPosition());
                    double missileTime = (distanceMissile / spellSpeed) + spellDelay;
                    while (Math.abs(travelTime - missileTime) > 0.01) {
                        travelTime = missileTime;
                        predictedPos = posAfterTime(champion, travelTime, spellRadius);
                        distanceMissile = distanceBetweenTargets(predictedPos, localPLayer.getPosition());
                        if (distanceMissile > spellRange)
                            return null;
                        missileTime = (distanceMissile / spellSpeed) + spellDelay /*+ spell->chanelingTime*/;
                    }
                    Integer hitChance = getHitChances(champion,targetGameplayRadius, spellSpeed, spellDelay, spellRadius, localPLayer,predictedPos);
                    if (checkCollision(localPLayer.getPosition(), predictedPos, localPLayer, spellRadius) && hitChance > 2){
                        return this.rendererComponent.worldToScreen(predictedPos.getX(), predictedPos.getY(), predictedPos.getZ());
                    }
                }
            }
            return null;
        });
    }

    Integer getHitChances(Champion champion,double targetGameplayRadius, double spellSpeed, double spellDelay, double spellWidth, Champion localPLayer, Vector3 predictedPos){
        List<Vector3> navigationPath = champion.getAiManager().getWaypoints().getNavigationPath();
        Vector3 lastWaypoint = (!navigationPath.isEmpty()) ? navigationPath.get(navigationPath.size() - 1) : null;
        if (lastWaypoint == null){
            return 0;
        }
        boolean isMovingSameDirection = isMovingInSameDirection(localPLayer, champion);
        double distanceToWaypoint = distanceBetweenTargets(champion.getPosition(), lastWaypoint);
        Vector3 pos1 = subtractVector3(lastWaypoint, champion.getPosition());
        Vector3 pos2 = subtractVector3(predictedPos, champion.getPosition());
        double angle = calculateAngle(pos1, pos2);
        double timeTillHit = ((distanceBetweenTargets(champion.getPosition(), localPLayer.getPosition()) - targetGameplayRadius) / spellSpeed) + spellDelay;
        boolean frequentDirectionChanges = detectFrequentDirectionChanges(champion.getMovementHistory());
        if (timeTillHit < 0.05 || !champion.getAiManager().getIsMoving()) {
            return 3;
        }
        if (frequentDirectionChanges){
            return 1;
        } else if (distanceBetweenTargets(champion.getPosition(), lastWaypoint) <250){
            return 3;
        } else if (angle > 105 && angle < 150 && distanceToWaypoint < 600){
            return 1;
        } else if (spellWidth >= 90 && spellSpeed > 2999 && spellDelay < 0.7){
            return 3;
        } else if (isMovingSameDirection || distanceToWaypoint > 600){
            if (angle > 130 || angle < 15) {
                return 3;
            }
        }
        return 2;
    }

    Vector3 posAfterTime(Champion obj, double time, double missileWidth) {
        AiManager pathController = obj.getAiManager();
        double speed = pathController.getMoveSpeed();
        if (pathController.getIsDashing()) {
            speed = pathController.getDashSpeed();
        }
        List<Vector3> waypoints = getFuturePoints(obj);
        int waypointsSize = waypoints.size();

        if (waypointsSize <= 1 || time == 0 || !pathController.getIsMoving()) {
            return waypoints.get(0);
        }
        double distance = (speed * time) - (missileWidth- (missileWidth * 0.1));
        for (int i = 1; i < waypointsSize; i++) {
            double wayDistance;
            if (i ==1){
                wayDistance = distanceBetweenTargets(waypoints.get(0),waypoints.get(i));
            } else {
                wayDistance = distanceBetweenTargets(waypoints.get(i-1),waypoints.get(i));
            }
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


    public Mono<Vector2> getBestChampionInSpell(Double spellRange, Double spellSpeed, Double spellDelay, Double spellRadius) {
        Champion localPLayer = this.championComponent.getLocalPlayer();
        Vector3 localPlayerPos = localPLayer.getPosition();
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
                    Vector3 targetPos = champion.getPosition();
                    int ping = 33;
                    double flyTimeMax = spellRange / spellSpeed;
                    double tMin = spellDelay + (double) ping / 2000.0;
                    double tMax = flyTimeMax + spellDelay + (double) ping / 1000.0;
                    List<Vector3> waypoints = getFuturePoints(champion);
                    int pathSize = waypoints.size();
                    int[] pathBounds = {-1, -1};
                    double pathTime = 0.0;
                    double targetMoveSpeed = ai.getIsDashing() ? (double) ai.getDashSpeed() : (double) ai.getMoveSpeed();
                    if (!ai.getIsMoving())
                    {
                        if (checkCollision(localPlayerPos, targetPos, localPLayer, spellRadius)){
                            return this.rendererComponent.worldToScreen(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                        }
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
                                    //Vector3 predictedPos = addVector3(center, scaleVector3(normalizeVector3(ai.getVelocity()), extender));
                                    double flyTime = this.distanceBetweenTargets(localPlayerPos, targetPos) / spellSpeed;
                                    double t = flyTime + spellDelay + (double) ping / 2000.0;
                                    double arriveTimeA = this.distanceBetweenTargets(targetPos, ptA) / targetMoveSpeed;
                                    double arriveTimeB = this.distanceBetweenTargets(targetPos, ptB) / targetMoveSpeed;
                                    if (Math.min(arriveTimeA, arriveTimeB) <= t && t <= Math.max(arriveTimeA, arriveTimeB)) {
                                        return this.rendererComponent.worldToScreen(center.getX(), center.getY(), center.getZ());
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
                Double targetGameplayRadius = (champion.getJsonCommunityDragon() != null) ? champion.getJsonCommunityDragon().getGameplayRadius() : 65.0;
                boolean inDistance = this.distanceBetweenTargets(localPLayer.getPosition(), champion.getPosition()) - targetGameplayRadius <= range + localPLayer.getJsonCommunityDragon().getGameplayRadius();
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
        futurePoints.add(target.getAiManager().getServerPos());
        List<Vector3> waypoints = target.getAiManager().getWaypoints().getNavigationPath();
        if (waypoints.isEmpty()) {
            return futurePoints;
        }
        for (int i = target.getAiManager().getWaypoints().getPassedWaypoint(); i < target.getAiManager().getWaypoints().getCurrentSize(); i++) {
            futurePoints.add(waypoints.get(i));
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

    public double calculateAngle(Vector3 origin, Vector3 target) {
        double deltaX = target.getX() - origin.getX();
        double deltaY = target.getY() - origin.getY();

        // Calcular el ángulo en radianes
        double angleRad = Math.atan2(deltaY, deltaX);

        // Convertir el ángulo a grados
        double angleDegree = Math.toDegrees(angleRad);

        // Asegurarse de que el ángulo esté en el rango [0, 360)
        angleDegree = (angleDegree + 360) % 360;

        return angleDegree;
    }

    public boolean isMovingInSameDirection(Champion source, Champion target) {
        List<Vector3> sourceWaypoints = source.getAiManager().getWaypoints().getNavigationPath();

        if (sourceWaypoints.isEmpty())
            return false;

        Vector3 sourceLastWaypoint = sourceWaypoints.get(sourceWaypoints.size() - 1);

        if (sourceLastWaypoint.equals(source.getPosition()) || !source.getAiManager().getIsMoving())
            return false;

        List<Vector3> targetWaypoints = target.getAiManager().getWaypoints().getNavigationPath();

        if (targetWaypoints.isEmpty())
            return false;

        Vector3 targetLastWaypoint = targetWaypoints.get(targetWaypoints.size() - 1);

        if (targetLastWaypoint.equals(target.getPosition()) || !target.getAiManager().getIsMoving())
            return false;

        Vector3 pos1 = subtractVector3(sourceLastWaypoint, source.getPosition());
        Vector3 pos2 = subtractVector3(targetLastWaypoint, target.getPosition());
        double angle = calculateAngle(pos1, pos2);

        return angle < 20;
    }

    boolean detectFrequentDirectionChanges(List<Vector3> movementHistory) {
        if (movementHistory.size() < 3) {
            // No hay suficientes datos para determinar cambios de dirección
            return false;
        }

        int changesCount = 0;
        double angleThreshold = 200.0; // Umbral de ángulo para considerar un cambio de dirección

        for (int i = 2; i < movementHistory.size(); i++) {
            Vector3 previousDirection = normalizeVector3(subtractVector3(movementHistory.get(i - 1), movementHistory.get(i - 2)));
            Vector3 currentDirection = normalizeVector3(subtractVector3(movementHistory.get(i), movementHistory.get(i - 1)));

            double angle = calculateAngle(previousDirection, currentDirection);
            // Si el ángulo entre las direcciones es mayor que el umbral, considerarlo como un cambio de dirección
            if (angle > angleThreshold) {
                changesCount++;
            }
        }

        int requiredChanges = 5; // Número mínimo de cambios para considerar "frecuente"
        return changesCount >= requiredChanges;
    }
}

