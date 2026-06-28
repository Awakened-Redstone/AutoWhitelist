package com.awakenedredstone.autowhitelist.discord.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

public class Reactor {
    public static Mono<Void> elastic(Runnable supplier) {
        return Mono.<Void>fromRunnable(supplier).subscribeOn(Schedulers.boundedElastic());
    }

    public static <T> Flux<T> elastic(Supplier<Publisher<T>> supplier) {
        return Flux.defer(supplier).subscribeOn(Schedulers.boundedElastic());
    }
}
