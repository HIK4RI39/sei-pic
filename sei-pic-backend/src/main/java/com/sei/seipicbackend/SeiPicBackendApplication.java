package com.sei.seipicbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.sei.seipicbackend.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class SeiPicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeiPicBackendApplication.class, args);
    }

}
