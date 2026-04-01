package com.sei.seipicbackend.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author hikari39_
 * @since 2026-04-02
 */
@Configuration
@ConfigurationProperties(prefix = "user")
@Data
public class UserConfig {
    private String salt;
    private String defaultPwd;
}