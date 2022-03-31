package com.example.faq.controller.viewObject;

import com.example.faq.dataObject.Answer;
import com.example.faq.dataObject.RecommendQuestion;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 对话结果的视图对象
 */
@Data
public class DialogueResultVO {
    //用户问题
    private String question;
    //回答：回答的内容、对应的标准问、置信度
    private Answer answer;
    //其他相似问题
    private List<RecommendQuestion> recommendQuestions;

    public DialogueResultVO() {
        this.answer = new Answer();
        this.recommendQuestions = new ArrayList<>();
    }
}
