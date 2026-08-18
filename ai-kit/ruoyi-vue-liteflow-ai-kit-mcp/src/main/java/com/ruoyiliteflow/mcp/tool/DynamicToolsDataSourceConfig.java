package com.ruoyiliteflow.mcp.tool;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 在主应用排除 DataSourceAutoConfiguration 时，按需创建 JDBC 连接以加载 ai_tool。
 */
@Configuration
@ConditionalOnProperty(prefix = "ruoyi.mcp.dynamic-tools", name = "enabled", havingValue = "true")
public class DynamicToolsDataSourceConfig
{
    @Bean
    public DataSource mcpDynamicDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName)
    {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource mcpDynamicDataSource)
    {
        return new JdbcTemplate(mcpDynamicDataSource);
    }
}
