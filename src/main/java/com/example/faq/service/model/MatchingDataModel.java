package com.example.faq.service.model;

import lombok.Data;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 匹配数据的领域模型
 */
@Data
public class MatchingDataModel {
    //docId
    private String id;
    //问答知识库的qa_id
    private Integer qaId;
    //标准问
    private String standardQuestion;
    //标准答
    private String standardAnswer;
    //相似问
    private String similarQuestion;
    //相关度得分
    private Float relevanceScore;
    //相似度得分
    private Float similarityScore;
    //置信度
    private Float confidence;
}
