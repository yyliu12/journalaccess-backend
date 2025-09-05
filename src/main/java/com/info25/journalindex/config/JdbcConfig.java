package com.info25.journalindex.config;

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
    public HikariDataSource dataSource() {
        //postgres data source
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl("jdbc:postgresql://localhost:5432/journal");
        c.setUsername("postgres");
        c.setPassword("12345678");
        c.setMaximumPoolSize(30);
        c.setLeakDetectionThreshold(1000);
        return new HikariDataSource(c);
    }

    @Bean
    public Dialect dialect() {
        // Use SQLite dialect for Hibernate
        return new SqliteDialect();
    }
}
