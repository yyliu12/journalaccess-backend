package com.info25.journalindex.config;

import org.jooq.ExecuteContext;
import org.jooq.ExecuteListenerProvider;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqConfig {

    @Bean
    public Settings jooqSettings() {
        return new Settings()
            .withReturnIdentityOnUpdatableRecord(true)
            .withRenderQuotedNames(RenderQuotedNames.NEVER)
            .withRenderFormatted(false);
    }
    
    @Bean
    public ExecuteListenerProvider executeListenerProvider() {
        return new DefaultExecuteListenerProvider(new DefaultExecuteListener() {
            private long startTime;

            @Override
            public void executeStart(ExecuteContext ctx) {
                startTime = System.nanoTime();

                // Ensure there is a query and parameters to log
                if (ctx.params() != null && ctx.params().length > 0) {
                    StringBuilder sb = new StringBuilder("Executing Query Bind Parameters: [");

                    for (int i = 0; i < ctx.params().length; i++) {
                        sb.append(ctx.params()[i].getValue());
                        if (i < ctx.params().length - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");

                    System.out.println(sb.toString());
                }
            }

            @Override
            public void executeEnd(ExecuteContext ctx) {
                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
                System.out.println("SQL: " + ctx.sql() + " took " + elapsedMs + " ms");
            }
        });
    }

}
