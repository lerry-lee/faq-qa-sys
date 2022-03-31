package com.example.faq.service.retrieval.model;

import lombok.Data;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
@Data
public class RetrievalDataModel {
    //docId
    private String id;
    //问答对的qa_id
    private Integer qaId;
    //标准问
    private String standardQuestion;
    //标准答
    private String standardAnswer;
    //相关度得分
    private Float relevanceScore;
}
