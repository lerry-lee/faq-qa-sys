package com.example.faq.dataObject;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 多轮对话数据保存在树形结构中，一轮对话用一颗树表示
 */
@Data
public class MultiQaTreeNode implements Serializable {
    //对应的qaId，一棵多轮问答树不同层节点的qaId是相同的，都为根节点question所对应的qaId
    private Integer qaId;
    //当前节点的问题
    private String question;
    //当前节点的回答
    private String answer;
    //当前节点的子节点
    private List<MultiQaTreeNode> childNodes;
}
