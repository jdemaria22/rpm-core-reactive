package com.core.reactive.corereactive.component;

import reactor.core.publisher.Mono;

public interface MemoryLoaderService {
    Mono<Boolean> update();
}
