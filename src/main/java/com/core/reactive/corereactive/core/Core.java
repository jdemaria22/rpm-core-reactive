package com.core.reactive.corereactive.core;

import com.core.reactive.corereactive.component.gametime.GameTime;
import com.core.reactive.corereactive.component.renderer.RendererComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Core {
    private final GameTime gameTime;
    private final RendererComponent rendererComponent;

    public void run() {
        while (true) {
            gameTime.update();
            rendererComponent.update();
        }
    }
}
