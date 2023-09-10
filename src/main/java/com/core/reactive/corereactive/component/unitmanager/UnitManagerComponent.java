package com.core.reactive.corereactive.component.unitmanager;

import com.core.reactive.corereactive.component.unitmanager.champion.ChampionComponent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class UnitManagerComponent {
    private final ChampionComponent championComponent;
    public Mono<Boolean> update() {
        return this.championComponent.update();
    }
}
