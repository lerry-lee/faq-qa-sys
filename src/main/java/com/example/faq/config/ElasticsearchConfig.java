package com.example.faq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description ES连接参数
 */
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class ElasticsearchConfig {
    private String host;
    private Integer port;
}
