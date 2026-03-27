package com.sei.seipicbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class SeiPicBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeiPicBackendApplication.class, args);
    }

}
