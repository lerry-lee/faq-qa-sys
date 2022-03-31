package com.example.faq.util;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description 使用High Level REST Client对ES进行操作
 */
@Component
public class RestClientUtil {

    /**
     * 初始化rest客户端，使用完毕后通过close方法关闭客户端以释放资源
     * client.close();
     *
     * @param host host
     * @param port port
     * @return RestHighLevelClient
     */
    public RestHighLevelClient getClient(String host, Integer port) {
        return new RestHighLevelClient(
                RestClient.builder(
                        //如果需要连接集群的多个节点，往后追加new HttpPost()
                        new HttpHost(host, port, "http")
                ));
    }


    /**
     * 创建一个SearchRequest，用于搜索文档（检索数据）
     *
     * @param indexName 索引名
     * @param filed     字段名
     * @param text      字段值
     * @param size      最相关的文档的数量
     * @return SearchRequest
     */
    public SearchRequest getSearchRequest(String indexName, String filed, Object text, Integer size) {
        //创建SearchRequest,将请求限制到索引indexName上
        SearchRequest searchRequest = new SearchRequest(indexName);
        //大多数搜索参数已添加到SearchSourceBuilder中。它为搜索请求正文中的所有内容提供设置器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(size);
        //搜索数据,match:{filed:text}
        searchSourceBuilder.query(QueryBuilders.matchQuery(filed, text));
        //将SearchSourceBuilder添加到SearchRequest中
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 创建一个IndexRequest，用于索引文档（插入数据）
     * 不指定docId
     *
     * @param indexName 索引名
     * @param jsonMap   doc
     * @return IndexRequest
     */
    public IndexRequest getIndexRequest(String indexName, Map<String, Object> jsonMap) {
        return new IndexRequest(indexName, "_doc")
                .source(jsonMap);
    }

    /**
     * 创建一个IndexRequest，用于索引文档（插入数据）
     * 指定docId
     *
     * @param indexName 索引名
     * @param jsonMap   doc
     * @return IndexRequest
     */
    public IndexRequest getIndexRequest(String indexName, Map<String, Object> jsonMap, String docId) {
        return new IndexRequest(indexName, "_doc", docId)
                .source(jsonMap);
    }

    /**
     * 创建一个DeleteIndexRequest，用于删除索引
     *
     * @param indexName 索引名
     * @return DeleteIndexRequest
     */
    public DeleteIndexRequest getDeleteIndexRequest(String indexName) {
        return new DeleteIndexRequest(indexName);
    }


    /**
     * 创建一个CreateIndexRequest，用于创建索引
     *
     * @param indexName 索引名
     * @return CreateIndexRequest
     */
    public CreateIndexRequest getCreateIndexRequest(String indexName) {
        CreateIndexRequest request = new CreateIndexRequest(indexName);

        return request;
    }


    public DeleteRequest getDeleteRequest(String indexName, String docId) {
        return new DeleteRequest(indexName, "_doc", docId);
    }
}
