package com.example.faq.service;

import com.example.faq.dataObject.DialogueStatus;

import java.io.IOException;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
public interface DialogueService {
    /**
     * 回答用户问题
     *
     * @param dialogueStatus 初始的对话状态模型
     * @return 完成的对话状态模型
     */
    DialogueStatus answer(DialogueStatus dialogueStatus) throws IOException;
}
