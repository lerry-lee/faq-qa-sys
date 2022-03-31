package com.example.faq.controller;

import com.example.faq.config.DialogueConfig;
import com.example.faq.controller.viewObject.DialogueResultVO;
import com.example.faq.dataObject.DialogueStatus;
import com.example.faq.response.CommonReturnType;
import com.example.faq.service.DialogueService;
import com.example.faq.util.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @Author: lerry_li
 * @CreateDate: 2021/01/17
 * @Description
 */
@Api(tags = "对话")
@RestController
@RequestMapping("/dialogue")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")   //处理跨域请求
@Slf4j
public class DialogueController {
    @Autowired
    private DialogueConfig dialogueConfig;

    @Autowired
    private DialogueService dialogueService;

    @Autowired
    private RedisUtil redisUtil;

    @ApiOperation("提问问题")
    @RequestMapping(value = "/ask", method = RequestMethod.GET)
    public CommonReturnType ask(
            @ApiParam("用户问题") @RequestParam(name = "question") String question,
            @ApiParam("用户id") @RequestParam(name = "user_id") Integer userId) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        //首先检查redis中有没有用户的对话状态
        DialogueStatus statusModel = (DialogueStatus) redisUtil.get(dialogueConfig.getDialogueStatusKeyPrefix() + userId);
        //没有则为用户创建一个对话状态
        if (statusModel == null) {
            statusModel = new DialogueStatus();
            statusModel.setUserId(userId);
        }
        //有则更新问题和robotId
        statusModel.setQuestion(question);
        //调用service回答
        statusModel = dialogueService.answer(statusModel);
        //更新对话状态到redis
        String key = dialogueConfig.getDialogueStatusKeyPrefix() + statusModel.getUserId();
        redisUtil.set(key, statusModel);
        redisUtil.expire(key, dialogueConfig.getStatus().getExpireTime());
        //创建视图对象
        DialogueResultVO vo = new DialogueResultVO();
        BeanUtils.copyProperties(statusModel, vo);

        stopWatch.stop();
        log.info("(userId={})当前用户提问\"{}\"，处理耗时{}ms", userId, question, stopWatch.getTotalTimeMillis());

        return CommonReturnType.create(vo, statusModel.getCodeMsg());
    }
}
