package com.core.reactive.corereactive.component.renderer;

import com.core.reactive.corereactive.component.MemoryLoaderService;
import com.core.reactive.corereactive.component.renderer.vector.Vector2;
import com.core.reactive.corereactive.component.renderer.vector.Vector3;
import com.core.reactive.corereactive.rpm.ReadProcessMemoryService;
import com.core.reactive.corereactive.util.Offset;
import com.sun.jna.Memory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Getter
@Slf4j
public class RendererComponent implements MemoryLoaderService {

    public static final int SIZE_MATRIX = 16;
    private final ReadProcessMemoryService readProcessMemoryService;
    private final Float[] viewProjMatrix = new Float[SIZE_MATRIX];
    private final Float[] projMatrix = new Float[SIZE_MATRIX];
    private final Float[] viewMatrix = new Float[SIZE_MATRIX];
    private Vector2 minimapPos;
    private Vector2 minimapSize;
    private final float width = 1920.0F;
    private final float height = 1080.0F;

    @Override
    public Mono<Boolean> update() {
        return Mono.fromCallable(() -> {
            this.updateMatrix(this.readProcessMemoryService.readMemory(Offset.viewProjMatrix, 128, true));
            this.updateMinimap();
            return Boolean.TRUE;
        });

    }

    private void updateMatrix(Memory memory) {
        for (int i = 0; i < SIZE_MATRIX ; i++) {
            viewMatrix[i] = memory.getFloat((i * 4));
        }

        for (int i = 0; i < SIZE_MATRIX; i++) {
            projMatrix[i] = memory.getFloat(0x40 + (i * 4));
        }

        this.multiplyMatrices();
    }

    private void updateMinimap() {
        Long minimapObj  = this.readProcessMemoryService.read(Offset.minimapObject, Long.class, true);
        Long minimapHud  = this.readProcessMemoryService.read(minimapObj + Offset.minimapObjectHud, Long.class, false);
//        log.info("minimapObj {}", minimapObj);
//        log.info("minimapHud {}", minimapHud);
        Memory memoryPos = this.readProcessMemoryService.readMemory(minimapHud, 0x80, false);
        this.minimapPos = Vector2.builder()
                .x(memoryPos.getFloat(Offset.minimapHudPos))
                .y(memoryPos.getFloat(Offset.minimapHudPos+0x4))
                .build();

        this.minimapSize = Vector2.builder()
                .x(memoryPos.getFloat(Offset.minimapHudSize))
                .y(memoryPos.getFloat(Offset.minimapHudSize+0x4))
                .build();
    }
    public Vector2 worldToScreen(Vector3 vector3) {
        float cordX = vector3.getX() * viewProjMatrix[0] + vector3.getY() * viewProjMatrix[4] + vector3.getZ() * viewProjMatrix[8] + viewProjMatrix[12];
        float cordY = vector3.getX() * viewProjMatrix[1] + vector3.getY() * viewProjMatrix[5] + vector3.getZ() * viewProjMatrix[9] + viewProjMatrix[13];
        float cordW = vector3.getX() * viewProjMatrix[3] + vector3.getY() * viewProjMatrix[7] + vector3.getZ() * viewProjMatrix[11] + viewProjMatrix[15];

        if (cordW < 1)
            cordW = 1F;

        float middleX = cordX / cordW;
        float middleY = cordY / cordW;

        float screenX = (width / 2F * middleX) + (middleX + width / 2F);
        float screenY = -(height / 2F * middleY) + (middleY + height / 2F);
        return Vector2.builder().x(screenX).y(screenY).build();
    }

    public Vector2 worldToScreen(float x, float y, float z) {
        float clipCoordsX = x * viewProjMatrix[0] + y * viewProjMatrix[4] + z * viewProjMatrix[8] + viewProjMatrix[12];
        float clipCoordsY = x * viewProjMatrix[1] + y * viewProjMatrix[5] + z * viewProjMatrix[9] + viewProjMatrix[13];
        float clipCoordsW = x * viewProjMatrix[3] + y * viewProjMatrix[7] + z * viewProjMatrix[11] + viewProjMatrix[15];

        if (clipCoordsW <= 0.0f) {
            clipCoordsW = 0.1f;
        }

        float M_x = clipCoordsX / clipCoordsW;
        float M_y = clipCoordsY / clipCoordsW;

        float out_x = (width / 2.0f) * M_x + (M_x + width / 2.0f);
        float out_y = -(height / 2.0f) * M_y + (M_y + height / 2.0f);
        return Vector2.builder().x(out_x).y(out_y).build();
    }

    public Vector2 worldToMinimap(Vector3 pos) {
        Vector2 result = Vector2.builder()
                .x(pos.getX()/15000.0f)
                .y(pos.getZ()/15000.0f)
                .build();
        result.setX(this.minimapPos.getX() + result.getX() * this.minimapSize.getX());
        result.setY(this.minimapPos.getY() + this.minimapSize.getY() - (result.getY() * this.minimapSize.getY()));
        return result;
    }

    private void multiplyMatrices() {
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j <= 3; j++) {
                float sum = 0F;
                for (int k = 0; k <= 3; k++)
                    sum += viewMatrix[i * 4 + k] * projMatrix[k * 4 + j];
                viewProjMatrix[i * 4 + j] = sum;
            }
        }
    }

}
