package com.example.faq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 对话参数配置类：从yml读取配置参数，代码内部无需关注参数具体数值
 */
@Configuration
@ConfigurationProperties(prefix = "dialogue")
@Data
public class DialogueConfig {
    private ConfidenceRank confidenceRank;
    private Status status;
    private MultiRoundQa multiRoundQa;
    private HotData hotData;

    //redis中多轮问答树的key前缀
    private final String MQATreeKeyPrefix = "MQATreeNode_";
    //redis中question映射id的key
    private final String MQAQuestion2idKey = "MQA_question2id";
    //redis中用户对话状态的key前缀
    private final String DialogueStatusKeyPrefix = "dialogue_status_userId_";
    //redis中热点数据的question映射id的key
    private final String HotDataQuestion2idKey = "hot_data_question2id";
    //redis中热点数据的key前缀
    private final String HotDataKeyPrefix = "hot_data_";

    @Data
    public static class ConfidenceRank {
        private Integer size;
        private Weights weights;
        private Float threshold;

        @Data
        public static class Weights {
            private Float relevanceWeight;
            private Float similarityWeight;
        }
    }

    @Data
    public static class Status {
        private Integer expireTime;
    }

    @Data
    public static class MultiRoundQa {
        private String path;
    }

    @Data
    public static class HotData {
        private Boolean open;
        private Integer expireTime;
    }
}
