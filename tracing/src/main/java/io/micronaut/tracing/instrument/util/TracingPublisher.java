/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.tracing.instrument.util;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Consumer;

import static io.micronaut.tracing.interceptor.TraceInterceptor.logError;

/**
 * A reactive streams publisher that traces
 *
 * @author graemerocher
 * @since 1.0
 */
public class TracingPublisher<T> implements Publisher<T> {

    private final Publisher<T> publisher;
    private final Tracer tracer;
    private final Tracer.SpanBuilder spanBuilder;
    private final Consumer<Span> onSubscribe;
    private final Span parentSpan;


    /**
     * Creates a new tracing publisher for the given arguments
     *
     * @param publisher The target publisher
     * @param tracer The tracer
     * @param operationName The operation name that should be started
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, String operationName) {
        this(publisher, tracer, tracer.buildSpan(operationName));
    }
    /**
     * Creates a new tracing publisher for the given arguments. This constructor will just add tracing of the
     * existing span if it is presnet
     *
     * @param publisher The target publisher
     * @param tracer The tracer
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer) {
        this(publisher, tracer, (Tracer.SpanBuilder)null);
    }
    /**
     * Creates a new tracing publisher for the given arguments. This constructor will just add tracing of the
     * existing span if it is presnet
     *
     * @param publisher The target publisher
     * @param tracer The tracer
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, Consumer<Span> onSubscribe) {
        this(publisher, tracer, null, onSubscribe);
    }
    /**
     * Creates a new tracing publisher for the given arguments
     *
     * @param publisher The target publisher
     * @param tracer The tracer
     * @param spanBuilder The span builder that represents the span that will be created when the publisher is subscribed to
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, Tracer.SpanBuilder spanBuilder) {
        this(publisher, tracer, spanBuilder, null);
    }

    /**
     * Creates a new tracing publisher for the given arguments
     *
     * @param publisher The target publisher
     * @param tracer The tracer
     * @param spanBuilder The span builder that represents the span that will be created when the publisher is subscribed to
     * @param onSubscribe A consumer that will be called when the publisher is subscribed to
     */
    public TracingPublisher(Publisher<T> publisher, Tracer tracer, Tracer.SpanBuilder spanBuilder, Consumer<Span> onSubscribe) {
        this.publisher = publisher;
        this.tracer = tracer;
        this.spanBuilder = spanBuilder;
        this.onSubscribe = onSubscribe;
        this.parentSpan = tracer.activeSpan();
        if(parentSpan != null && spanBuilder != null) {
            spanBuilder.asChildOf(parentSpan);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        Span span;
        if(spanBuilder != null) {
            span = spanBuilder.start();
        }
        else {
            span = parentSpan;
        }
        if(span != null) {
            onSubscribe.accept(span);
            try(Scope ignored = tracer.scopeManager().activate(span, false)) {
                publisher.subscribe(new Subscriber<T>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        try(Scope ignored = tracer.scopeManager().activate(span, false)) {
                            actual.onSubscribe(s);
                        }
                    }

                    @Override
                    public void onNext(T t) {
                        try(Scope ignored = tracer.scopeManager().activate(span, false)) {
                            actual.onNext(t);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        try(Scope ignored = tracer.scopeManager().activate(span, false)) {
                            logError(span, t);
                            actual.onError(t);
                        }
                    }

                    @Override
                    public void onComplete() {
                        try(Scope ignored = tracer.scopeManager().activate(span, false)) {
                            actual.onComplete();
                            span.finish();
                        }
                    }
                });
            }
        }
        else {
            publisher.subscribe(actual);
        }
    }

}