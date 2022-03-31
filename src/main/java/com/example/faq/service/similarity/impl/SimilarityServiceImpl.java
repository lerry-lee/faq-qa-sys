package com.example.faq.service.similarity.impl;

import com.alibaba.fastjson.JSON;
import com.example.faq.config.SimilarityConfig;
import com.example.faq.service.similarity.SimilarityService;
import com.example.faq.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
@Service
@Slf4j
public class SimilarityServiceImpl implements SimilarityService {

    @Autowired
    private SimilarityConfig similarityConfig;

    @Autowired
    private HttpUtil httpUtil;

    @Override
    public List<Float> similarityCalculation(List<String> textList1, List<String> textList2) {
        List<Float> scores = new ArrayList<>();
        //输入处理
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        for (int i = 0; i < textList1.size(); i++) {
            params.add("text_list1", textList1.get(i));
            params.add("text_list2", textList2.get(i));
        }

        //调用深度学习模型计算
        System.setProperty("java.net.preferIPv6Addresses", "true");
        String responseBody = httpUtil.sendHttpPostRequest(similarityConfig.getRequestUrl(), params);

        if (responseBody == null) {
            log.error("responseBody is null");
            return scores;
        }
        Map<String, List<BigDecimal>> data = (Map<String, List<BigDecimal>>) JSON.parse(responseBody);
        List<BigDecimal> origin_scores = data.get("scores");
        for (BigDecimal origin_score : origin_scores) {
            scores.add(origin_score.floatValue());
        }
        return scores;
    }
}
