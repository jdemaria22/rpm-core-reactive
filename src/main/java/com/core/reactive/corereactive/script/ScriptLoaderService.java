package com.core.reactive.corereactive.script;

import reactor.core.publisher.Mono;

public interface ScriptLoaderService {
    Mono<Boolean> update();
}
