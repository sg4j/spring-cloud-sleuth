package org.springframework.cloud.brave.autoconfig;

import brave.Tracing;
import brave.context.log4j2.ThreadContextCurrentTraceContext;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.brave.ErrorParser;
import org.springframework.cloud.brave.ExceptionMessageErrorParser;
import org.springframework.cloud.sleuth.DefaultSpanNamer;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.Reporter;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * to enable tracing via Spring Cloud Sleuth.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(value="spring.sleuth.enabled", matchIfMissing=true)
public class TraceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Tracing sleuthTracing(@Value("${spring.zipkin.service.name:${spring.application.name:default}}") String serviceName,
			Propagation.Factory factory,
			CurrentTraceContext currentTraceContext,
			Reporter<zipkin2.Span> reporter,
			Sampler sampler) {
		return Tracing.newBuilder()
				.sampler(sampler)
				.localServiceName(serviceName)
				.propagationFactory(factory)
				.currentTraceContext(currentTraceContext)
				.spanReporter(reporter).build();
	}

	@Bean
	@ConditionalOnMissingBean
	Sampler defaultTraceSampler() {
		return Sampler.NEVER_SAMPLE;
	}

	@Bean
	@ConditionalOnMissingBean
	SpanNamer sleuthSpanNamer() {
		return new DefaultSpanNamer();
	}

	@Bean
	@ConditionalOnMissingBean
	Propagation.Factory sleuthPropagation() {
		return Propagation.Factory.B3;
	}

	@Bean
	@ConditionalOnMissingBean
	CurrentTraceContext sleuthCurrentTraceContext() {
		return ThreadContextCurrentTraceContext.create();
	}

	@Bean
	@ConditionalOnMissingBean
	Reporter<zipkin2.Span> noOpSpanReporter() {
		return Reporter.NOOP;
	}

	@Bean
	@ConditionalOnMissingBean
	public ErrorParser defaultErrorParser() {
		return new ExceptionMessageErrorParser();
	}
}