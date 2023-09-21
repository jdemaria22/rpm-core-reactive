package com.core.reactive.corereactive;

import com.core.reactive.corereactive.core.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CoreReactiveApplication {
	public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		ApplicationContext applicationContext = SpringApplication.run(CoreReactiveApplication.class, args);
		Core core = applicationContext.getBean(Core.class);
		core.run();
	}
}