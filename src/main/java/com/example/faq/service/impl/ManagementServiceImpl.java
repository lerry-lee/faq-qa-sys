package com.example.faq.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.example.faq.config.DialogueConfig;
import com.example.faq.config.ElasticsearchConfig;
import com.example.faq.config.RetrievalConfig;
import com.example.faq.dataObject.MultiQaTreeNode;
import com.example.faq.entity.FaqPair;
import com.example.faq.mapper.FaqPairMapper;
import com.example.faq.service.ManagementService;
import com.example.faq.util.RedisUtil;
import com.example.faq.util.RestClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
@Service
@Slf4j
public class ManagementServiceImpl implements ManagementService {

    @Autowired
    private ElasticsearchConfig ESConfig;

    @Autowired
    private RetrievalConfig retrievalConfig;

    @Autowired
    private DialogueConfig dialogueConfig;

    @Autowired
    private FaqPairMapper faqPairMapper;

    @Autowired
    private RestClientUtil restClientUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public int totalSynchronize(String tableIndexName) throws IOException {
        int account = 0;
        //查询数据库中所有数据
        List<FaqPair> faqPairList = faqPairMapper.selectAll();
        //es client初始化
        RestHighLevelClient client = restClientUtil.getClient(ESConfig.getHost(), ESConfig.getPort());
        //删除原索引
        try {
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(restClientUtil.getDeleteIndexRequest(tableIndexName), RequestOptions.DEFAULT);
            log.info("删除索引{} {}", tableIndexName, deleteIndexResponse.isAcknowledged());
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            if (e.status() == RestStatus.NOT_FOUND) {
                log.error("索引{}不存在，无法删除，将直接创建", tableIndexName);
            }
        }
        //创建新索引
        CreateIndexRequest createIndexRequest = restClientUtil.getCreateIndexRequest(tableIndexName);

        String jsonSource = readElasticsearchAPIJson(retrievalConfig.getIndex().getFaqPair(), "index");

        //index setting,mappings,7.0以上版本弃用_doc
        createIndexRequest.source(jsonSource, XContentType.JSON);

        try {
            CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            if (!createIndexResponse.isAcknowledged()) {
                log.error("创建索引{}失败", tableIndexName);
                return 0;
            }
        } catch (ElasticsearchException e) {
            e.printStackTrace();
            return 0;
        }

        log.info("创建索引{}成功", tableIndexName);

        //插入数据
        IndexRequest request = null;
        int size = faqPairList.size();
        for (FaqPair faqPair : faqPairList) {
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("qa_id", faqPair.getQaId());
            jsonMap.put("standard_question", faqPair.getStandardQuestion());
            jsonMap.put("standard_answer", faqPair.getStandardAnswer());
            request = restClientUtil.getIndexRequest(tableIndexName, jsonMap);
            try {
                IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                account++;
            } catch (ElasticsearchException e) {
                e.printStackTrace();
            }

        }
        log.info("从mysql中{}表查询到{}条数据，成功同步到es{}条数据", tableIndexName, size, account);
        //关闭client及时释放资源
        client.close();

        return account;
    }

    @Override
    public int updateMultiTree() {
        String path = dialogueConfig.getMultiTurnQa().getPath();
        //遍历多轮问答树的路径
        File dir = new File(dialogueConfig.getMultiTurnQa().getPath());
        if (!dir.exists()) {
            log.error("多轮问答树的路径{}不存在", path);
            return 0;
        }
        if (!dir.isDirectory()) {
            log.error("多轮问答树的路径{}不是一个目录", path);
            return 0;
        }
        String[] files = dir.list();
        if (files == null) {
            log.error("{}路径下无任何文件", path);
            return 0;
        }
        //将文件转换为对象，更新到redis中

        //建立question到qaId的唯一映射
        HashMap<String, Integer> question2id = new HashMap<>();

        int NumsOfTreeNode = files.length;
        int accout = 0;
        for (String file : files) {
            String filePath = dir + "/" + file;
            //将json文件转换为java对象
            MultiQaTreeNode node = readFileToObject(filePath);
            if (node == null) {
                log.error("读取多轮问答树{}出错，跳过", filePath);
                continue;
            }
            int qaId = node.getQaId();
            question2id.put(node.getQuestion(), qaId);
            //设置多轮问答树的qaId的为key
            redisUtil.set(dialogueConfig.getMQATreeKeyPrefix() + qaId, node);
            accout++;
        }

        redisUtil.set(dialogueConfig.getMQAQuestion2idKey(), question2id);

        //返回更新的多轮问答树的总数
        log.info("已更新{}个多轮问答树到redis中", accout);
        return NumsOfTreeNode;
    }

    /**
     * 读取保存常用esAPI的json文件
     *
     * @param indexName 索引名
     * @param APIType   方法
     * @return jsonString
     */
    public String readElasticsearchAPIJson(String indexName, String APIType) {
        String jsonFile;
        switch (APIType) {
            case "index":
                jsonFile = String.format("%s/index_API/PUT-%s.json", retrievalConfig.getElasticsearchAPIPath(), indexName);
                break;
            case "document":
                jsonFile = String.format("%s/document_API/PUT-%s.json", retrievalConfig.getElasticsearchAPIPath(), indexName);
                break;
            case "search":
                jsonFile = String.format("%s/search_API/GET-%s.json", retrievalConfig.getElasticsearchAPIPath(), indexName);
                break;
            default:
                jsonFile = null;
        }
        if (jsonFile == null) {
            log.error("没有对应{} API的操作", APIType);
            return null;
        }

        String jsonData = "";
        //读取多轮问答树json文件
        try {
            jsonData = FileUtils.readFileToString(new File(jsonFile), String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            log.error("读取esAPIJson文件{}出错", jsonFile);
            return null;
        }

        return jsonData;
    }

    /**
     * 读取多轮问答树文件，转换为对象
     *
     * @return MultiQaTreeNode
     */
    public MultiQaTreeNode readFileToObject(String file) {
        String jsonData;
        //读取多轮问答树json文件
        try {
            jsonData = FileUtils.readFileToString(new File(file), String.valueOf(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        //转换为MultiQaTreeNode多轮问答树对象
        return JSONObject.parseObject(jsonData, MultiQaTreeNode.class);
    }

}
