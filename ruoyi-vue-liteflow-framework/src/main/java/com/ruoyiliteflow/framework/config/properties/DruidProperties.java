package com.ruoyiliteflow.framework.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * druid 配置属性
 * 
 * @author ruoyi
 */
@Configuration
public class DruidProperties
{
    @Value("${spring.datasource.druid.initialSize:5}")
    private int initialSize;

    @Value("${spring.datasource.druid.minIdle:10}")
    private int minIdle;

    @Value("${spring.datasource.druid.maxActive:20}")
    private int maxActive;

    @Value("${spring.datasource.druid.maxWait:60000}")
    private int maxWait;

    @Value("${spring.datasource.druid.connectTimeout:30000}")
    private int connectTimeout;

    @Value("${spring.datasource.druid.socketTimeout:60000}")
    private int socketTimeout;

    @Value("${spring.datasource.druid.timeBetweenEvictionRunsMillis:60000}")
    private int timeBetweenEvictionRunsMillis;

    @Value("${spring.datasource.druid.minEvictableIdleTimeMillis:300000}")
    private int minEvictableIdleTimeMillis;

    @Value("${spring.datasource.druid.maxEvictableIdleTimeMillis:900000}")
    private int maxEvictableIdleTimeMillis;

    @Value("${spring.datasource.druid.validationQuery:SELECT 1 FROM DUAL}")
    private String validationQuery;

    @Value("${spring.datasource.druid.testWhileIdle:true}")
    private boolean testWhileIdle;

    @Value("${spring.datasource.druid.testOnBorrow:false}")
    private boolean testOnBorrow;

    @Value("${spring.datasource.druid.testOnReturn:false}")
    private boolean testOnReturn;

    public DruidDataSource dataSource(DruidDataSource datasource)
    {
        /** 配置初始化大小、最小、最大 */
        datasource.setInitialSize(initialSize);
        datasource.setMaxActive(maxActive);
        datasource.setMinIdle(minIdle);

        /** 配置获取连接等待超时的时间 */
        datasource.setMaxWait(maxWait);
        
        /** 配置驱动连接超时时间，检测数据库建立连接的超时时间，单位是毫秒 */
        datasource.setConnectTimeout(connectTimeout);
        
        /** 配置网络超时时间，等待数据库操作完成的网络超时时间，单位是毫秒 */
        datasource.setSocketTimeout(socketTimeout);

        /** 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 */
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);

        /** 配置一个连接在池中最小、最大生存的时间，单位是毫秒 */
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);

        /**
         * 用来检测连接是否有效的sql，要求是一个查询语句，常用select 'x'。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。
         */
        datasource.setValidationQuery(validationQuery);
        /** 建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。 */
        datasource.setTestWhileIdle(testWhileIdle);
        /** 申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 */
        datasource.setTestOnBorrow(testOnBorrow);
        /** 归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 */
        datasource.setTestOnReturn(testOnReturn);
        return datasource;
    }
}
