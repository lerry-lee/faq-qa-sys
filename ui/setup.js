const ghost = 'http://localhost:1234/faq/dialogue/ask'

const bot = new ChatSDK({
    // 初始化query
    // query: '你好',
    config: {
        navbar: {
            title: 'FAQ问答系统'
        },
        // 机器人头像
        robot: {
            avatar: 'img/robot.png'
        },
        // 用户头像
        user: {
            avatar: 'img/user.png',
        },
        // 输入框占位符
        placeholder: '输入您想要咨询的问题',
    },
    requests: {
        /**
         *
         * 问答接口
         * @param {object} msg - 消息
         * @param {string} msg.type - 消息类型
         * @param {string} msg.content - 消息内容
         * @return {object}
         */
        send: function (msg) {
            const data = msg.content;
            // 发送文本消息时
            if (msg.type === 'text') {
                return {
                    url: ghost,
                    data: {
                        user_id: 1,
                        question: data.text
                    }
                };
            }
        },
    },
    // 收到消息的数据处理
    handlers: {
        /**
         *
         * 解析请求返回的数据
         * @param {object} res - 请求返回的数据
         * @param {object} requestType - 请求类型
         * @return {array}
         */
        parseResponse: function (res, requestType) {
            // 根据 requestType 处理数据
            // 如果是send
            if (requestType === 'send') {
                // console.log(res);
                // 是单轮
                if (res.code === 20001) {
                    return {
                        type: 'text',
                        content: {
                            text: res.data.answer.content,

                        }
                    }
                }
                // 是多轮
                else if (res.code === 20002) {
                    let option_list = [];
                    let i = 0;
                    const options = res.data.answer.options;
                    // 判断是否到达叶节点，即无后续选项
                    if (options.length > 0) {
                        for (let idx in options) {
                            option_list[i] = {title: options[idx]};
                            i++;
                        }
                        return [
                            {
                                type: 'text',
                                content: {
                                    text: res.data.answer.content,
                                },
                            },
                            {
                                type: 'card',
                                content: {
                                    code: 'slot',
                                    data: {
                                        hideShortcuts: true,
                                        list: option_list
                                    }
                                }
                            }]
                    } else {
                        return {
                            type: 'text',
                            content: {
                                text: res.data.answer.content,
                            },
                        }
                    }
                }
                //未识别的问题设置兜底回答
                else if (res.code === 40001) {
                    return {
                        type: 'text',
                        content: {
                            text: '抱歉，您说的我还不能理解~',

                        }
                    }
                }
                // 否则，非单轮和多轮，即未正常得到回复
                return {
                    type: 'text',
                    content: {
                        text: res.msg,
                    },
                }
            }
            // 不需要处理的数据直接返回
            else {
                return res;
            }
        },
    },
});

bot.run();