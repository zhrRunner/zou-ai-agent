# Retention Storage Info新增存储接口实现方案

## 背景

当前，用户在使用MCP平台访问存储配置中心的新增存储配置的过程中存在两个问题：

1. 新建存储的资产表信息（TableName、DatabaseName）不存在，前端显示success，之后却查不到该记录

2. 新建存储的资产3V字段不存在，前端显示success，之后却查不到该记录

以上问题说明落库并未成功，插入结果应准确的反馈给用户

为解决上述问题，使用户明确新增操作的状态，优化用户操作体验，同时增强DR系统对接效率，现决定对存储配置中心的addStorageInfo接口进行功能优化。

## 设计

### 问题分析

**针对问题1：**原有设计默认插入标志insertResult为true，当资产信息表查询不到记录时，会跳过插入逻辑，直接返回success，导致并未落库但前端显示插入成功，查询新存储时并没有记录的问题

解决方法：查询相关资产信息表之前新增判空逻辑即可

**针对问题2：**原有设计默认插入标志insertResult为true，当可以查询到资产信息表但对应的资产没有3V信息的时候，会因为没有在vregion列表里而continue，导致retentionStorageInfos为空，从而跳过插入逻辑，直接返回success，导致并未落库但前端显示插入成功，查询新存储时并没有记录的问题

解决方法：由于这个问题的可能出现在遍历资产信息列表的过程中，不能直接新建分支置为失败，考虑增加3v字段非空判断逻辑，如果为空加入message列表提供给前端，并且在遍历retentionStorageInfos列表前新增判空逻辑，如果为空，表示没有用户输入的资产，不满足新增条件

### 设计流程

<img src="src/main/resources/document/whiteboard_exported_image.png" alt="241751873892_.pic" style="zoom:33%;" />：

#### 流程说明

1. **开始**
2. 接收 POST 请求
3. **参数校验**
    - 请求参数的非空校验错误 → 返回：参数校验不通过
    - 参数验证错误 → 返回：参数校验不通过
4. 根据 SceneType 获取 vregionList
5. 生成可能的业务资源
6. 获取资源库信息（biz_resource_id，biz_info）
7. **判断是否有资源分布**
    - 否 → 返回：没有可用业务资源分布
    - 是 → 继续
8. **判断资源分布中是否有 Redis/Abase**
    - 是
        - 获取对应的 key_template 列表
        - 进行处理（processKeyTemplate）
    - 否
9. 遍历资源分布，处理 resource1, resource2, resourceN
10. **判断 vregionList 是否为空**
    - 是 → 返回 message
    - 否 → 继续
11. 输入存储任务分发
12. **分发任务判断**
    - 否 → 返回：没有资源分布的可用入库
    - 是 → 继续
13. 遍历存储任务分发信息，处理 retentionStorageInfo, retentionStorageInfo1, retentionStorageInfo2, retentionStorageInfoN
14. **判断是否有存储入库信息**
    - 否 → 返回 message
    - 是 → 继续
15. 从存储入库信息中进行入库操作
    - 入库失败 → 返回：未补全目标入库
    - 入库成功 → 返回：补全入库成功

---

#### 主要分支与异常处理

- 参数校验失败，直接返回错误信息
- 没有可用业务资源分布，直接返回错误信息
- vregionList 为空，直接返回 message
- 没有资源分布的可用入库，直接返回错误信息
- 入库失败，返回未补全目标入库
- 入库成功，返回补全入库成功

### 分支与错误码说明

| 编号 | 分支说明                             | 处理结果 | 状态码 | 返回message                                                  |
| ---- | ------------------------------------ | -------- | ------ | ------------------------------------------------------------ |
| 1    | 参数验证不合法（包括参数非法性验证） | 失败     | 400    | **字段 cannot be null / database、table、psm只允许包含数字、字母、下划线、句点以及短横线！** |
| 2    | 查询不到对应的资产表                 | 失败     | 400    | 该资产不在商业化资产信息表中                                 |
| 3    | 没有满足条件的可插入存储             | 失败     | 409    | 没有满足条件的可插入存储，该资产在商业化资产信息表中区域缺失区域信息/该场景不适用对应的地区 |
| 4    | 部分存储插入失败                     | 失败     | 500    | 插入失败：**区域未写入成功；**区域已存在在系统中，本次未处理 |
| 5    | 全部插入成功                         | 成功     | 200    | SUCCESS：**区域已存在在系统中，本次未处理**                  |

## API设计



链接：/dr/api/info

Method：**POST**

说明：添加存储信息

请求参数：

Request Body

| 字段名称           | 类型    | 说明                                                         | 备注     |
| ------------------ | ------- | ------------------------------------------------------------ | -------- |
| deletionType       | String  | 删除方式：1：账号删除 2：滚动删除                            | Not null |
| ttIThreshold       | Long    | 滚动删除保留天数，deletion_type=2时必填                      |          |
| resourceType       | String  | 资源类型                                                     | Not null |
| resourceRegion     | String  | va/sg/gcp/ttp                                                | Not null |
| resourceDatabase   | String  | 数据库名                                                     |          |
| resourceTable      | String  | 表名                                                         |          |
| resourcePsm        | String  | psm                                                          | Not null |
| resourceOwner      | String  | 资源owner                                                    | Not null |
| resourceDepartment | String  | 资源所属的部门                                               |          |
| enableSceneType    | String  | 1：Retention Policy 2：Texas Protected Data Removal 3：PA-Revamp Data Reset 5：CPRA 3P Data Clear History 6：mixed age |          |
| enableRestore      | Integer | 是否需要恢复 0:否，1:是                                      |          |

返回参数

通用返回类ResponseEntity

| 字段名称 | 类型   | 说明    |
| -------- | ------ | ------- |
| code     | int    | 状态码  |
| msg      | String | message |
| data     | T      | 数据    |

## 排期



### 人员安排

M10N DR：邹浩冉

### 里程碑设定

| 任务                              | 排期  |
| --------------------------------- | ----- |
| 阅读原有新增存储接口              | 0.5天 |
| 给出解决方案，输出文档&文档review | 2天   |
| 代码编写&测试&上线                | 1.5天 |