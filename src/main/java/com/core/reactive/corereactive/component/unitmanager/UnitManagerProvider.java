package com.core.reactive.corereactive.component.unitmanager;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public interface UnitManagerProvider<UNIT> {
    Mono<Long> getOffset();
    Mono<Long> getClient();
    Mono<Long> getClientList(Long client);
    Mono<Long> getClientListLength(Long client);
    ConcurrentHashMap<Long, UNIT> getMapUnit();
}
