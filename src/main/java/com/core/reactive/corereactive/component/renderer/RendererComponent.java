package com.core.reactive.corereactive.component.renderer;

import com.core.reactive.corereactive.component.vector.Vector2;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class RendererComponent {

    private final ReadProcessMemoryService readProcessMemoryService;
    private final Float[] viewProjMatrix = new Float[16];
    private final Float[] projMatrix = new Float[16];
    private final Float[] viewMatrix = new Float[16];
    private final int width = 1920;
    private final int height = 1080;

    public void update() {
        updateMatrix(this.readProcessMemoryService.readMemory(Offset.viewProjMatrix, 128));
    }

    public Vector2 toVector2(float x, float y, float z) {
        float coordX = x * viewProjMatrix[0] + y * viewProjMatrix[4] + z * viewProjMatrix[8] + viewProjMatrix[12];
        float coordY = x * viewProjMatrix[1] + y * viewProjMatrix[5] + z * viewProjMatrix[9] + viewProjMatrix[13];
        float coordW = x * viewProjMatrix[3] + y * viewProjMatrix[7] + z * viewProjMatrix[11] + viewProjMatrix[15];

        if (coordW < 1)
            coordW = 1F;

        float middleX = coordX / coordW;
        float middleY = coordY / coordW;

        float screenX = (width / 2F * middleX) + (middleX + width / 2F);
        float screenY = -(height / 2F * middleY) + (middleY + height / 2F);
        return Vector2.builder().x(screenX).y(screenY).build();
    }

    private void sumMatrix() {
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j <= 3; j++) {
                float sum = 0F;
                for (int k = 0; k <= 3; k++)
                    sum += viewMatrix[i * 4 + k] * projMatrix[k * 4 + j];
                viewProjMatrix[i * 4 + j] = sum;
            }
        }
    }

    private void updateMatrix(Memory memory) {
        for (int i = 0; i <= viewMatrix.length - 1; i++) {
            viewMatrix[i] = memory.getFloat((long) i * Float.SIZE);
        }
        for (int i = 0; i <= projMatrix.length - 1; i++) {
            projMatrix[i] = memory.getFloat(64L + ((long) i * Float.SIZE));
        }

        sumMatrix();
    }

}
