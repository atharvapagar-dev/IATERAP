package com.adtech.iaterap.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    // PostgreSQL JDBC reads the JVM default while opening its first connection;
    // Spring's Hibernate timezone property is applied too late for that step.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(
                BackendApplication.class,
                args
        );
    }
}
