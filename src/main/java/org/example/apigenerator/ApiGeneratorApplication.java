package org.example.apigenerator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class ApiGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGeneratorApplication.class, args);
    }

}
