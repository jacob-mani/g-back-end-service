package com.ford.grid.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class WersDBDataSourceConfig {

    @Value("${spring.datasource.wers.url}")
    private String dataSourceUrl;

    @Value("${spring.datasource.wers.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.wers.username}")
    private String userName;

    @Value("${spring.datasource.wers.password}")
    private String password;

    @Bean("db2")
    public DataSource dataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(dataSourceUrl);
        dataSourceBuilder.driverClassName(driverClassName);
        dataSourceBuilder.username(userName);
        dataSourceBuilder.password(password);
        return dataSourceBuilder.build();
    }

    @Bean("wersdb")
    public JdbcTemplate topicsJdbcTemplate(@Qualifier("db2") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}

