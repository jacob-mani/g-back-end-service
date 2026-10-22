package com.ford.grid;

import com.ford.aps.spring.configuration.EnableApsService;
import com.ford.cloudnative.annotations.EnableFordSecurityTools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Hooks;


@SpringBootApplication(exclude = ZipkinAutoConfiguration.class)
@EnableFordSecurityTools
@EnableAspectJAutoProxy
@EnableApsService
@EnableAsync
@EnableScheduling
public class GirdApplication {


	public static void main(String[] args) {
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(GirdApplication.class, args);
	}
}
