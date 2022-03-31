package com.example.faq.service.impl;

import com.example.faq.config.DialogueConfig;
import com.example.faq.controller.viewObject.DialogueResultVO;
import com.example.faq.dataObject.Answer;
import com.example.faq.dataObject.DialogueStatus;
import com.example.faq.dataObject.MultiQaTreeNode;
import com.example.faq.dataObject.RecommendQuestion;
import com.example.faq.response.CodeMsg;
import com.example.faq.service.DialogueService;
import com.example.faq.service.model.MatchingDataModel;
import com.example.faq.service.retrieval.RetrievalService;
import com.example.faq.service.retrieval.model.RetrievalDataModel;
import com.example.faq.service.similarity.SimilarityService;
import com.example.faq.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * @Author: lerry_li
 * @CreateDate: 2022/03/31
 * @Description
 */
@Service
@Slf4j
public class DialogueServiceImpl implements DialogueService {

    @Autowired
    private DialogueConfig dialogueConfig;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public DialogueStatus answer(DialogueStatus dialogueStatus) throws IOException {

        //清空原answer
        dialogueStatus.setAnswer(new Answer());
        //清空原recommendQuestion
        dialogueStatus.setRecommendQuestions(new ArrayList<>());

        Integer userId = dialogueStatus.getUserId();
        String question = dialogueStatus.getQuestion();

        //1.是否需要检索
        //1.1 若处于多轮问答中，则不需要检索
        if (dialogueStatus.isMulti()) {
            //处理多轮问答
            DialogueStatus dialogueStatusAfterProcess = processMultiRound(dialogueStatus);
            //若成功处理，直接返回
            if (dialogueStatusAfterProcess.getCodeMsg() == CodeMsg.SUCCESS_MULTI) {
                return dialogueStatusAfterProcess;
            }
        }
        //1.2 查看redis中是否有对应的热点数据
        HashMap<String, String> hotDataQuestion2id = null;
        if (dialogueConfig.getHotData().getOpen()) {
            hotDataQuestion2id = (HashMap<String, String>) redisUtil.get(dialogueConfig.getHotDataQuestion2idKey());
            if (hotDataQuestion2id != null && hotDataQuestion2id.containsKey(question)) {
                //根据id对应的key找到对应的缓存数据
                DialogueResultVO dialogueResultVO = (DialogueResultVO) redisUtil.get(dialogueConfig.getHotDataKeyPrefix() + hotDataQuestion2id.get(question));
                if (dialogueResultVO != null) {
                    //将缓存中的数据拷贝到领域模型中
                    BeanUtils.copyProperties(dialogueResultVO, dialogueStatus);
                    //判断是多轮还是单轮
                    HashMap<String, Integer> MQAQuestion2Id = (HashMap<String, Integer>) redisUtil.get(dialogueConfig.getMQAQuestion2idKey());
                    String stdQuestion = dialogueResultVO.getAnswer().getStdQ();
                    //若多轮问答树为空或者不包含该数据对应的标准问，则设置状态为单轮
                    if (MQAQuestion2Id == null || !MQAQuestion2Id.containsKey(stdQuestion)) {
                        //更新对话状态多轮
                        dialogueStatus.setMultiQaTreeNode(null);
                        dialogueStatus.setMulti(false);
                        //设置状态码
                        dialogueStatus.setCodeMsg(CodeMsg.SUCCESS_SINGLE);
                        log.info("(userId={})当前用户的问题命中缓存数据\"{}\"，redis中多轮问答树不包含该问题对应的标准问\"{}\",状态判定为单轮", userId, question, stdQuestion);
                    }
                    //否则设置状态为多轮
                    else {
                        //设置状态码
                        dialogueStatus.setCodeMsg(CodeMsg.SUCCESS_MULTI);
                        //更新对话状态
                        dialogueStatus.setMulti(true);
                        MultiQaTreeNode currNode = (MultiQaTreeNode) redisUtil.get(dialogueConfig.getMQATreeKeyPrefix() + MQAQuestion2Id.get(stdQuestion));
                        dialogueStatus.setMultiQaTreeNode(currNode);
                        log.info("(userId={})当前用户的问题命中缓存数据\"{}\"，redis中多轮问答树包含该问题对应的标准问\"{}\",状态判定为多轮", userId, question, stdQuestion);
                    }

                    return dialogueStatus;
                }
            }
        }

        //否则需要进行检索
        //2.检索
        List<RetrievalDataModel> retrievalDataModelList = retrievalService.searchSimilarQuestions(question);
        //判断es是否正常工作，若有异常则返回
        if (retrievalDataModelList == null) {
            dialogueStatus.setCodeMsg(CodeMsg.ELASTICSEARCH_EXCEPTION);
            return dialogueStatus;
        }
        int total_counts = retrievalDataModelList.size();
        //若未识别该问题
        if (total_counts == 0) {
            //更新对话状态
            dialogueStatus.setMulti(false);
            dialogueStatus.setMultiQaTreeNode(null);
            //设置状态码
            dialogueStatus.setCodeMsg(CodeMsg.UNRECOGNIZED_QUESTION);
            //设置兜底回答
            dialogueStatus.getAnswer().setContent("抱歉您说的我暂时还无法理解哈，请尝试其它问法呢~");
            return dialogueStatus;
        }
        //3.语义相似度计算
        List<String> questionList = new ArrayList<>(total_counts);
        List<String> similarQuestionList = new ArrayList<>(total_counts);
        for (RetrievalDataModel retrievalDataModel : retrievalDataModelList) {
            questionList.add(question);
            similarQuestionList.add(retrievalDataModel.getSimilarQuestion());
        }
        List<Float> similarityScoreList = similarityService.similarityCalculation(questionList, similarQuestionList);

        //若相似度模型返回为空，则忽略相似度
        if (similarityScoreList == null || similarityScoreList.size() == 0) {
            log.error("(userId={})相似度模型返回为空，请检查模型是否启动、url/参数是否正确", userId);

            //设置状态码
            dialogueStatus.setCodeMsg(CodeMsg.SIMILARITY_NULL_EXCEPTION);

            //填充语义相似度为0
            similarityScoreList = new ArrayList<>();
            for (int i = 0; i < total_counts; i++) {
                similarityScoreList.add(0F);
            }

            log.info("(userId={})当前用户提问\"{}\"，相似度模型返回为null，只使用es检索返回的结果", userId, question);

        }
        //4.问答处理
        //4.1组装matchingDataModel
        List<MatchingDataModel> matchingDataModelList = new ArrayList<>(total_counts);
        for (int i = 0; i < total_counts; i++) {
            matchingDataModelList.add(new MatchingDataModel());
            BeanUtils.copyProperties(retrievalDataModelList.get(i), matchingDataModelList.get(i));
            matchingDataModelList.get(i).setSimilarityScore(similarityScoreList.get(i));
        }
        //4.2综合相关度得分和相似度得分，两者加权求和为置信度，按置信度排序
        matchingDataModelList.sort((o1, o2) -> {
            //综合相关度得分和相似度得分，加权求和
            Float confidence1 = o1.getConfidence();
            if (confidence1 == null) {
                Float relevanceScore1 = o1.getRelevanceScore();
                Float similarityScore1 = o1.getSimilarityScore();
                confidence1 = dialogueConfig.getConfidenceRank().getWeights().getRelevanceWeight() * relevanceScore1 + dialogueConfig.getConfidenceRank().getWeights().getSimilarityWeight() * similarityScore1;
                o1.setConfidence(confidence1);
            }
            Float confidence2 = o2.getConfidence();
            if (confidence2 == null) {
                Float relevanceScore2 = o2.getRelevanceScore();
                Float similarityScore2 = o2.getSimilarityScore();
                confidence2 = dialogueConfig.getConfidenceRank().getWeights().getRelevanceWeight() * relevanceScore2 + dialogueConfig.getConfidenceRank().getWeights().getSimilarityWeight() * similarityScore2;
                o2.setConfidence(confidence2);
            }

            return confidence2.compareTo(confidence1);
        });
        //4.3填充结果
        //记录识别为多轮的标记
        boolean isRecognizeMultiRound = false;
        for (int i = 0; i < dialogueConfig.getConfidenceRank().getSize() && i < total_counts; i++) {
            MatchingDataModel matchingDataModel = matchingDataModelList.get(i);
            //置信度最高的结果作为answer
            if (i == 0) {
                String bestAnswer = matchingDataModel.getStandardAnswer();
                //标准问同步到status
                dialogueStatus.getAnswer().setStdQ(matchingDataModel.getStandardQuestion());
                dialogueStatus.getAnswer().setConfidence(matchingDataModel.getConfidence());
                dialogueStatus.getAnswer().setSimQ(matchingDataModel.getSimilarQuestion());
                //若识别为多轮，则进入首轮多轮问答处理
                if (bestAnswer.equals("多轮")) {
                    dialogueStatus = firstProcessMultiRound(dialogueStatus);
                    //判断是否成功处理首轮多轮
                    if (dialogueStatus.getCodeMsg() != CodeMsg.SUCCESS_MULTI) {
                        return dialogueStatus;
                    }
                    isRecognizeMultiRound = true;
                }
                //如果未识别为多轮，则设置答案，否则使用多轮首轮的处理结果
                if (!isRecognizeMultiRound) {
                    dialogueStatus.getAnswer().setContent(bestAnswer);
                }
            }
            //其他的作为相关问题推荐
            else {
                RecommendQuestion recommendQuestion = new RecommendQuestion();
                recommendQuestion.setSimQ(matchingDataModel.getSimilarQuestion());
                recommendQuestion.setStdQ(matchingDataModel.getStandardQuestion());
                recommendQuestion.setConfidence(matchingDataModel.getConfidence());
                dialogueStatus.getRecommendQuestions().add(recommendQuestion);
            }
        }

        //更新对话状态
        //如果未识别为多轮，则设置多轮状态为false
        if (!isRecognizeMultiRound) {
            dialogueStatus.setMulti(false);
            //设置状态码
            dialogueStatus.setCodeMsg(CodeMsg.SUCCESS_SINGLE);
        } else {
            dialogueStatus.setCodeMsg(CodeMsg.SUCCESS_MULTI);
        }

        //5.问答处理过后，考虑将数据加入缓存
        if (dialogueConfig.getHotData().getOpen()) {
            DialogueResultVO vo = new DialogueResultVO();
            BeanUtils.copyProperties(dialogueStatus, vo);
            //更新hotDataQuestion2id和hotData
            if (hotDataQuestion2id == null) {
                hotDataQuestion2id = new HashMap<>();
            }
            //判断redis是否缓存热点数据
            //生成随机不重复id token
            String hotDataIdToken = UUID.randomUUID().toString().replace("-", "");
            //将question映射到id token上
            hotDataQuestion2id.put(question, hotDataIdToken);
            redisUtil.set(dialogueConfig.getHotDataQuestion2idKey(), hotDataQuestion2id);
            String hotDataKey = dialogueConfig.getHotDataKeyPrefix() + hotDataIdToken;
            redisUtil.set(hotDataKey, vo);
            redisUtil.expire(hotDataKey, dialogueConfig.getHotData().getExpireTime());
        }

        return dialogueStatus;
    }

    /**
     * 多轮问答首轮处理
     *
     * @param statusModel 对话状态
     * @return DialogueStatus
     */
    public DialogueStatus firstProcessMultiRound(DialogueStatus statusModel) {
        String standardQuestion = statusModel.getAnswer().getStdQ();
        //首先到redis中查找多轮问答树的question2id的键值对
        HashMap<String, Integer> question2id = (HashMap<String, Integer>) redisUtil.get(dialogueConfig.getMQAQuestion2idKey());
        //若question2id不存在
        if (question2id == null) {
            log.info("(userId={})redis中多轮问答树为空", statusModel.getUserId());

            //设置状态码
            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_NULL);

            return statusModel;
        }
        //若question2id中没有对应的key
        if (!question2id.containsKey(standardQuestion)) {
            log.info("(userId={})没有找到根节点的question为\"{}\"的多轮问答树", statusModel.getUserId(), standardQuestion);

            //设置状态码
            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_NOT_FOUND);

            return statusModel;
        }
        //根据question对应的id，从redis中获取对应的多轮问答树
        Integer MQATreeId = question2id.get(standardQuestion);
        MultiQaTreeNode node = (MultiQaTreeNode) redisUtil.get(dialogueConfig.getMQATreeKeyPrefix() + MQATreeId);
        //填充statusModel
        statusModel.getAnswer().setContent(node.getAnswer());

        //清空选项
        statusModel.getAnswer().setOptions(new ArrayList<>());
        //填充选项
        List<MultiQaTreeNode> nodes = node.getChildNodes();

        if (nodes == null || nodes.size() == 0) {
            log.error("(userId={})子结点为空！请检查\"{}\"的多轮问答树是否正确读取", statusModel.getUserId(), standardQuestion);

            //设置状态码

            statusModel.setCodeMsg(CodeMsg.MULTI_ROUND_QA_CHILD_NODE_NULL);

            return statusModel;
        }

        for (MultiQaTreeNode childNode : node.getChildNodes()) {
            statusModel.getAnswer().getOptions().add(childNode.getQuestion());
        }

        //更新对话状态
        statusModel.setMulti(true);
        statusModel.setMultiQaTreeNode(node);
        statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);
        log.info("(userId={})当前用户提问\"{}\"，进入首轮多轮问答", statusModel.getUserId(), statusModel.getQuestion());

        return statusModel;
    }


    /**
     * 多轮问答中间过程处理
     *
     * @param statusModel 对话状态
     * @return DialogueStatus
     */
    public DialogueStatus processMultiRound(DialogueStatus statusModel) {
        String question = statusModel.getQuestion();
        MultiQaTreeNode node = statusModel.getMultiQaTreeNode();
        boolean getIntoNextMulti = false;
        //遍历子结点，找到用户提问命中的选项
        for (MultiQaTreeNode child_node : node.getChildNodes()) {
            if (child_node.getQuestion().equals(question)) {
                node = child_node;
                getIntoNextMulti = true;
                break;
            }
        }
        //若未命中多轮问答选项时，不进入下一轮多轮问答，返回
        if (!getIntoNextMulti) {
            log.info("(userId={})当前用户提问\"{}\"，未命中多轮问答的选项", statusModel.getUserId(), question);
            //更新状态
            statusModel.setCodeMsg(CodeMsg.OPTIONS_NOT_HIT);
            return statusModel;
        }
        //处理多轮问答数据
        //填充answer属性
        String answer_content = node.getAnswer();
        statusModel.getAnswer().setContent(answer_content);
        statusModel.getAnswer().setConfidence(1.0000F);
        //清空原options选项
        statusModel.getAnswer().setOptions(new ArrayList<>());
        //若当前多轮问答树节点没有子结点，表示当前的多轮问答已经结束
        if (node.getChildNodes() == null || node.getChildNodes().size() == 0) {
            statusModel.setMulti(false);
        }
        //否则遍历当前节点的子结点，填充options属性
        else {
            statusModel.setMulti(true);
            //填充选项
            for (MultiQaTreeNode childNode : node.getChildNodes()) {
                statusModel.getAnswer().getOptions().add(childNode.getQuestion());
            }
        }
        //更新对话状态
        statusModel.setMultiQaTreeNode(node);

        log.info("(userId={})当前用户提问\"{}\"，命中多轮问答的选项", statusModel.getUserId(), question);

        //设置状态码
        statusModel.setCodeMsg(CodeMsg.SUCCESS_MULTI);

        return statusModel;
    }
}
