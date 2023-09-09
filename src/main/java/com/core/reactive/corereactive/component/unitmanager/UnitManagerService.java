package com.core.reactive.corereactive.component.unitmanager;

import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public interface UnitManagerService<UNIT> {
    Mono<Boolean> update();
    Mono<ConcurrentHashMap<Long,UNIT>> loadUnitMap(Long clientListLength, Long clientList);
    Boolean existsUnitInMap(Long address);
    UNIT findUnitInfo(Long address, UNIT unit);
}
