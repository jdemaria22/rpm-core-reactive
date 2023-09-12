package com.core.reactive.corereactive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DistanceCalculatorService {
    public Double distance(Double playerX, Double playerY, Double targetX, Double targetY) {
        Double xDifference = playerX - targetX;
        Double yDifference = playerY - targetY;
        return hypot(xDifference, yDifference);
    }

    public boolean inDistance(Double playerX, Double playerY, Double targetX, Double targetY, Double playerAttackRange, Double targetRadius, Double playerRadius) {
        Double dist = distance(playerX, playerY, targetX, targetY);
        Double radiusSum = playerAttackRange + playerRadius + targetRadius;

        return dist - radiusSum <= 0;
    }

    private Double hypot(Double x, Double y) {
        return Math.sqrt(x * x + y * y);
    }

    private Double sqrt(Double value) {
        return Math.sqrt(value);
    }
}