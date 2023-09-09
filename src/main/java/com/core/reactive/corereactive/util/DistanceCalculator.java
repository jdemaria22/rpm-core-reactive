package com.core.reactive.corereactive.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;

@Component
@RequiredArgsConstructor
public class DistanceCalculator {
    public BigDecimal distance(BigDecimal playerX, BigDecimal playerY, BigDecimal targetX, BigDecimal targetY) {
        BigDecimal xDifference = playerX.subtract(targetX);
        BigDecimal yDifference = playerY.subtract(targetY);
        return hypot(xDifference, yDifference);
    }

    public boolean inDistance(BigDecimal playerX, BigDecimal playerY, BigDecimal targetX, BigDecimal targetY, BigDecimal playerAttackRange, BigDecimal targetRadius, BigDecimal playerRadius) {
        BigDecimal dist = distance(playerX, playerY, targetX, targetY);
        BigDecimal radiusSum = playerAttackRange.add(playerRadius).add(targetRadius);

        return dist.subtract(radiusSum).compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal hypot(BigDecimal x, BigDecimal y) {
        // Utilizar el teorema de Pitágoras para calcular la hipotenusa
        return sqrt(x.multiply(x).add(y.multiply(y)));
    }

    private BigDecimal sqrt(BigDecimal value) {
        // Utilizar una implementación de cálculo de raíz cuadrada adecuada
        // Esto puede variar según tus necesidades específicas
        // Aquí asumimos que tienes una implementación válida de sqrt
        return value.sqrt(MathContext.DECIMAL128); // Usando un contexto de precisión alto
    }
}