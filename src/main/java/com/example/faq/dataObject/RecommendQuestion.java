package com.example.faq.dataObject;

import lombok.Data;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 其他相似问题推荐
 */
@Data
public class RecommendQuestion {
    //标准问
    private String stdQ;
    //标准答
    private String stdA;
    //置信度
    private Float confidence;
}
