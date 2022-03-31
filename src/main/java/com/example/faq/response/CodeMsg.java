package com.example.faq.response;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 状态码和对应说明
 */
public enum CodeMsg {
    //通用状态码10000系列，模块异常
    ELASTICSEARCH_EXCEPTION(10001, "elasticsearch异常"),
    MYSQL_EXCEPTION(10002, "mysql异常"),
    SIMILARITY_NULL_EXCEPTION(10003, "相似度计算模型异常"),

    //通用状态码20000系列，有返回值，无异常
    SUCCESS(20000, "success"),
    SUCCESS_SINGLE(20001, "success-->单轮"),
    SUCCESS_MULTI(20002, "success-->多轮"),

    //通用状态码30000系列，中间状态
    OPTIONS_NOT_HIT(30001, "处于多轮问答中，但未命中多轮问答的选项，此时将重新检索用户问题"),

    //通用状态码40000系列，无返回值
    FAILED(40000, "failed"),
    UNRECOGNIZED_QUESTION(40001, "failed-->无法识别的问题"),
    MULTI_ROUND_QA_NOT_FOUND(40002, "failed-->没有找到对应的多轮问答脚本"),
    MULTI_ROUND_QA_NULL(40003, "failed-->redis中多轮问答树为空"),
    MULTI_ROUND_QA_CHILD_NODE_NULL(40004, "failed-->多轮问答树子节点为空");

    @Getter
    @Setter
    private Integer code;
    @Getter
    @Setter
    private String msg;

    CodeMsg(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
