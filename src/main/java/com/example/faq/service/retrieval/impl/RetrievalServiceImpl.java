package com.example.faq.service.retrieval.impl;

import com.example.faq.config.ElasticsearchConfig;
import com.example.faq.config.RetrievalConfig;
import com.example.faq.service.retrieval.RetrievalService;
import com.example.faq.service.retrieval.model.RetrievalDataModel;
import com.example.faq.util.RestClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
public class RetrievalServiceImpl implements RetrievalService {


    @Autowired
    private ElasticsearchConfig ESConfig;

    @Autowired
    private RetrievalConfig RetrievalConfig;

    @Autowired
    private RestClientUtil restClientUtil;

    @Override
    public List<RetrievalDataModel> searchSimilarQuestions(String question) throws IOException {

        List<RetrievalDataModel> retrievalDataModelList = new ArrayList<>(RetrievalConfig.getSearch().getSize());
        RestHighLevelClient client;
        //初始化rest client
        try {
            client = restClientUtil.getClient(ESConfig.getHost(), ESConfig.getPort());
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return null;
        }
        //创建searchRequest
        SearchRequest request = restClientUtil.getSearchRequest(RetrievalConfig.getIndex().getStdQSimQ(), "similar_question", question, RetrievalConfig.getSearch().getSize());

        //以同步方式搜索问题，等待搜索结果
        SearchResponse response;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return null;
        }
        //状态
        RestStatus status = response.status();
        //耗时
        TimeValue took = response.getTook();

        SearchHits hits = response.getHits();
        long totalHits = hits.getTotalHits();
        if (totalHits == 0) {
            log.info("未识别的问题\"{}\"", question);
            return retrievalDataModelList;
        }
        //遍历docs中的数据
        SearchHit[] searchHits = hits.getHits();

        for (SearchHit hit : searchHits) {
            //docId
            String id = hit.getId();
            //相关度得分
            float score = hit.getScore();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            RetrievalDataModel retrievalDataModel = new RetrievalDataModel();
            retrievalDataModel.setId(id);
            retrievalDataModel.setRelevanceScore(score);
            retrievalDataModel.setStandardQuestion((String) sourceAsMap.get("standard_question"));
            retrievalDataModel.setSimilarQuestion((String) sourceAsMap.get("similar_question"));
            Integer qaId = (Integer) sourceAsMap.get("qa_id");
            retrievalDataModel.setQaId(qaId);

            //根据qaId搜索问答知识库，一个qaId只能对应一个标准问
            request = restClientUtil.getSearchRequest(RetrievalConfig.getIndex().getStdQStdA(), "qa_id", qaId, 1);
            try {
                response = client.search(request, RequestOptions.DEFAULT);
            } catch (ElasticsearchException e) {
                e.printStackTrace();
                return null;
            }

            SearchHit hit_ = response.getHits().getHits()[0];
            //遍历docs中的数据
            Map<String, Object> sourceAsMap_ = hit_.getSourceAsMap();
            retrievalDataModel.setStandardAnswer((String) sourceAsMap_.get("standard_answer"));

            retrievalDataModelList.add(retrievalDataModel);
        }
        client.close();
        return retrievalDataModelList;
    }

    @Override
    public Integer insertDocs(String indexName, List<Map<String, Object>> jsonMapList) throws IOException {
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getHost(), ESConfig.getPort());
        int account = 0;
        for (Map<String, Object> jsonMap : jsonMapList) {
            IndexRequest request = restClientUtil.getIndexRequest(indexName, jsonMap);
            client.index(request, RequestOptions.DEFAULT);
            account++;
        }
        client.close();
        log.info("成功插入{}个数据到索引{}中", account, indexName);

        return account;
    }
}
