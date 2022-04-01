# 项目介绍

一个简单的FAQ问答系统实现。基于检索和排序的两阶段框架，检索阶段基于Elasticsearch检索引擎、排序阶段基于语义匹配深度学习模型。后端基于SpringBoot系列框架。

# 功能说明

## 对话

人机对话：用户提出问题，系统给出回答。

## 管理

知识库管理：问答对数据的增删改查。

# 技术选型

## 后端

| 技术 | 说明                       |
| --- |--------------------------|
| Spring Boot | Java后端框架                 |
| Mybatis | Java持久层框架                |
| Rest high level client | Elasticsearch Java 客户端工具 |

## 数据库

| 技术 | 说明 |
| --- | --- |
| MySQL | 关系型数据库 |
| Redis | 缓存数据库 |
| Elasticsearch | 全文检索引擎 |

# 部署运行

## 环境搭建

### MySQL

1. 建库

```shell
# 在mysql命令行执行下面建库语句
CREATE DATABASE IF NOT EXISTS faqdb DEFAULT CHARSET utf8 COLLATE utf8_general_ci;
```

3. 导入表数据

```shell
# 到sql/路径下，可以看faqdb.sql，在mysql命令行执行下面语句
source faqdb.sql;
```

## 启动步骤

## 测试

打开浏览器访问`http://localhost:1234/faq/swagger-ui/`可以查看全部接口并进行测试；