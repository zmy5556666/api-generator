package org.example.apigenerator.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan("org.example.apigenerator.mapper")
public class MybatisConfig {

    @Bean
    public DataSource dataSource() {
        System.out.println("========== 正在强制手动创建 DataSource (绕过YAML解析) ==========");

        return DataSourceBuilder.create()
                .driverClassName("com.mysql.cj.jdbc.Driver")
                // 确保数据库名 api_generator_db 正确
                .url("jdbc:mysql://localhost:3306/api_generator_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai")
                .username("root")
                // 填入你真实的 MySQL 密码
                .password("123456")
                .build();
    }
}