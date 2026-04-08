package com.info25.journalindex.config;

import com.info25.journalindex.services.ConfigService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@EnableJdbcRepositories(basePackages = "com.info25.journalindex.repositories")
public class JdbcConfig {
    @Bean
    public HikariDataSource dataSource(ConfigService configService) {
        //postgres data source
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl("jdbc:postgresql://localhost:5432/journal");
        c.setUsername("postgres");
        c.setPassword(configService.getConfigOption("postgresSecret"));
        c.setMaximumPoolSize(30);
        c.setLeakDetectionThreshold(1000);
        return new HikariDataSource(c);
    }


}
