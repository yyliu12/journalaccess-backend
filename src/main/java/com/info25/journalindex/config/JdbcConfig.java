package com.info25.journalindex.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.NamingStrategy;

import com.info25.journalindex.services.ConfigService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableJdbcRepositories(basePackages = "com.info25.journalindex.repositories")
public class JdbcConfig extends AbstractJdbcConfiguration {
    @Bean
    public HikariDataSource dataSource(ConfigService configService) {
        // postgres data source
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(
                "jdbc:oracle:thin:@(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.us-ashburn-1.oraclecloud.com))(connect_data=(service_name=g272e32638d7bab_w9uz9p24kt9idmcm_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))");
        c.setUsername("admin");
        c.setPassword(configService.getConfigOption("postgresSecret"));
        c.setMaximumPoolSize(5);
        c.setLeakDetectionThreshold(1000);
        c.setDriverClassName("oracle.jdbc.OracleDriver");
        return new HikariDataSource(c);
    }

    @Bean
    @Override
    public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
            JdbcCustomConversions customConversions,
            RelationalManagedTypes rmt) {

        JdbcMappingContext mappingContext = super.jdbcMappingContext(namingStrategy, customConversions, rmt);
        mappingContext.setForceQuote(false);

        return mappingContext;
    }

}
