package com.ford.grid.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class GCAMPDatabaseConfiguration {

    @Value("${spring.datasource.gcamp.url}")
    private String dataSourceURL;

    @Value("${spring.datasource.gcamp.driver}")
    private String driverClassName;

    @Value("${spring.datasource.gcamp.username}")
    private String databaseUserName;

    @Value("${spring.datasource.gcamp.pwd}")
    private String databasePassword;

    @Bean("gcampDataSource")
    public DataSource dataSource() {
        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url(dataSourceURL);
        dataSourceBuilder.driverClassName(driverClassName);
        dataSourceBuilder.username(databaseUserName);
        dataSourceBuilder.password(databasePassword);
        return dataSourceBuilder.build();
    }

    @Bean("gcampJdbcTemplate")
    public JdbcTemplate gcampJdbcTemplate(@Qualifier("gcampDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}