package com.example.faq.service;

import java.io.IOException;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
public interface ManagementService {
    /**
     * 全量同步，从mysql中同步一张表的所有数据到es对应的索引中
     *
     * @param tableIndexName 表名/索引名
     * @return 成功操作的数据总数
     */
    int totalSynchronize(String tableIndexName) throws IOException;

    /**
     * 更新多轮问答树到redis
     * @return 成功更新的数量
     */
    int updateMultiTree();
}
