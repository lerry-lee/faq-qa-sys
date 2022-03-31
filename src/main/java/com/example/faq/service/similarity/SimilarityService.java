package com.example.faq.service.similarity;

import java.util.List;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 相似度计算服务
 */
public interface SimilarityService {
    /**
     * 计算两个文本列表的相似度
     *
     * @param textList1 文本列表1
     * @param textList2 文本列表2
     * @return List<Float>
     */
    List<Float> similarityCalculation(List<String> textList1, List<String> textList2);
}
