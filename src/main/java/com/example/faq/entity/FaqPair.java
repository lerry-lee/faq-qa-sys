package com.example.faq.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 对应数据表：faq_pair
 */
@ApiModel("FAQ问答对：标准问-标准答")
@Data
public class FaqPair {
    private Integer id;
    //标准问的唯一标识，用于多表数据关联一致性
    @JsonProperty(value = "qa_id")
    private Integer qaId;
    //标准问
    @ApiModelProperty("标准问")
    @JsonProperty(value = "standard_question")
    private String standardQuestion;
    //标准答
    @ApiModelProperty("标准答")
    @JsonProperty(value = "standard_answer")
    private String standardAnswer;
}
