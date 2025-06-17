package com.info25.journalindex.config;

import javax.sql.DataSource;

import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@Configuration
@EnableJdbcRepositories(basePackages = "com.info25.journalindex.repositories")
public class JdbcConfig {
    @Bean
    public DataSource dataSource() {
        //sqlite data source
        return DataSourceBuilder.create()
                .url("jdbc:sqlite:C:\\Users\\yuyan\\Desktop\\journalindex\\data.db")
                .driverClassName("org.sqlite.JDBC")
                .build();
    }

    @Bean
    public Dialect dialect() {
        // Use SQLite dialect for Hibernate
        return new SqliteDialect();
    }
}
