package com.ruoyiliteflow;

import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        SpringApplication application = new SpringApplication(RuoYiApplication.class);
        application.setDefaultProperties(defaultProperties());
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  若依启动成功   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }

    private static Properties defaultProperties()
    {
        Properties properties = new Properties();
        properties.setProperty("spring.profiles.active", "druid");
        properties.setProperty("token.header", "Authorization");
        properties.setProperty("token.secret", "abcdefghijklmnopqrstuvwxyz");
        properties.setProperty("token.expireTime", "30");
        properties.setProperty("xss.enabled", "true");
        properties.setProperty("xss.excludes", "/system/notice");
        properties.setProperty("xss.urlPatterns", "/system/*,/monitor/*,/tool/*");
        properties.setProperty("referer.allowed-domains", "localhost,127.0.0.1");
        return properties;
    }
}
