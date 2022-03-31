package com.example.faq.dataObject;

import com.example.faq.response.CodeMsg;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 对话状态
 */
@Data
public class DialogueStatus implements Serializable {
    //用户ID
    private Integer userId;
    //用户问题
    private String question;
    //本轮问答的状态码和解释
    private CodeMsg codeMsg;
    //回答：回答的内容、对应的标准问、置信度
    private Answer answer;
    //其他相似问题
    private List<RecommendQuestion> recommendQuestions;
    //是否处于多轮问答中
    private boolean isMulti;
    //多轮问答树节点
    private MultiQaTreeNode multiQaTreeNode;


    public DialogueStatus() {
        this.answer = new Answer();
        this.recommendQuestions = new ArrayList<>();
        this.multiQaTreeNode = new MultiQaTreeNode();
    }
}
