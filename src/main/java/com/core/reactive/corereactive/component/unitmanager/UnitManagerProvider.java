package com.core.reactive.corereactive.component.unitmanager;

import com.core.reactive.corereactive.component.unitmanager.model.Unit;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public interface UnitManagerProvider<UNIT extends Unit> {
    Mono<Long> getOffset();
    Mono<Long> getClient();
    Mono<Long> getClientList(Long client);
    Mono<Long> getClientListLength(Long client);
    ConcurrentHashMap<Long, UNIT> getMapUnit();
}
