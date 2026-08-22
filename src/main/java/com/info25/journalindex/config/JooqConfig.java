package com.info25.journalindex.config;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {
    @Bean
    public ExecuteListenerProvider executeListenerProvider() {
        return new DefaultExecuteListenerProvider(new DefaultExecuteListener() {
            private long startTime;

            @Override
            public void executeStart(ExecuteContext ctx) {
                startTime = System.nanoTime();
            }

            @Override
            public void executeEnd(ExecuteContext ctx) {
                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                System.out.println("SQL: " + ctx.sql() + " took " + elapsedMs + " ms");
            }
        });
    }

}
