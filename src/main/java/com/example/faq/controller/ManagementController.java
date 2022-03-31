package com.example.faq.controller;

import com.example.faq.config.RetrievalConfig;
import com.example.faq.response.CommonReturnType;
import com.example.faq.service.ManagementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
@Api(tags = "管理")
@RestController
@RequestMapping("/management")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
@Slf4j
public class ManagementController {
    private final static String ContentType = "application/x-www-form-urlencoded";

    @Autowired
    private ManagementService managementService;

    @Autowired
    private RetrievalConfig retrievalConfig;


    /**
     * 全量同步，将mysql中的一张表全部同步到redis中
     */
    @ApiOperation("全量同步")
    @RequestMapping(value = "/total_synchronize", method = RequestMethod.GET)
    public CommonReturnType totalSynchronize(@ApiParam("表/索引名") @RequestParam(name = "table_index_name") String tableIndexName) throws IOException {

        //检查表/索引名是否有效
        if (!retrievalConfig.getIndex().getFaqPair().equals(tableIndexName)) {
            log.error("{}不在可以同步的表/索引中", tableIndexName);
            return CommonReturnType.failed(String.format("%s不在可以同步的表/索引中", tableIndexName));
        }

        //统计耗时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int account = managementService.totalSynchronize(tableIndexName);
        stopWatch.stop();

        if (account == 0) {
            return CommonReturnType.failed(String.format("mysql表%s中0条数据被同步", tableIndexName));
        }

        return CommonReturnType.success(String.format("成功同步mysql表%s中%d条数据到es索引%s，耗时%dms", tableIndexName, account, tableIndexName, stopWatch.getTotalTimeMillis()));
    }
}
