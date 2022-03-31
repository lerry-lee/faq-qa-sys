package com.example.faq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 相似度计算模型连接参数
 */
@Configuration
@ConfigurationProperties(prefix = "similarity")
@Data
public class SimilarityConfig {
    private String requestUrl;
}
