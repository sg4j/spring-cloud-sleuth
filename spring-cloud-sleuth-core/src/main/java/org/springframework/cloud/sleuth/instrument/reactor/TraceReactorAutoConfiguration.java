package org.springframework.cloud.sleuth.instrument.reactor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceableScheduledExecutorService;
import org.springframework.cloud.sleuth.instrument.web.TraceWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * to enable tracing of Reactor components via Spring Cloud Sleuth.
 *
 * @author Stephane Maldini
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(value="spring.sleuth.reactor.enabled", matchIfMissing=true)
@ConditionalOnClass(Mono.class)
@AutoConfigureAfter(TraceWebFluxAutoConfiguration.class)
public class TraceReactorAutoConfiguration {

	@Configuration
	@ConditionalOnBean(Tracer.class)
	static class TraceReactorConfiguration {
		@Autowired Tracer tracer;
		@Autowired TraceKeys traceKeys;
		@Autowired SpanNamer spanNamer;
		@Autowired LastOperatorWrapper lastOperatorWrapper;

		@Bean
		@ConditionalOnNotWebApplication
		LastOperatorWrapper spanOperator() {
			return new LastOperatorWrapper() {
				@Override public void wrapLastOperator(Tracer tracer) {
					Hooks.onLastOperator(ReactorSleuth.spanOperator(tracer));
				}
			};
		}

		@Bean
		@ConditionalOnWebApplication
		LastOperatorWrapper noOpLastOperatorWrapper() {
			return new LastOperatorWrapper() {
				@Override public void wrapLastOperator(Tracer tracer) {
				}
			};
		}

		@PostConstruct
		public void setupHooks() {
			this.lastOperatorWrapper.wrapLastOperator(this.tracer);
			Schedulers.setFactory(new Schedulers.Factory() {
				@Override public ScheduledExecutorService decorateExecutorService(String schedulerType,
						Supplier<? extends ScheduledExecutorService> actual) {
					return new TraceableScheduledExecutorService(actual.get(),
							TraceReactorConfiguration.this.tracer,
							TraceReactorConfiguration.this.traceKeys,
							TraceReactorConfiguration.this.spanNamer);
				}
			});
		}

		@PreDestroy
		public void cleanupHooks() {
			Hooks.resetOnLastOperator();
			Schedulers.resetFactory();
		}
	}
}

interface LastOperatorWrapper {
	void wrapLastOperator(Tracer tracer);
}