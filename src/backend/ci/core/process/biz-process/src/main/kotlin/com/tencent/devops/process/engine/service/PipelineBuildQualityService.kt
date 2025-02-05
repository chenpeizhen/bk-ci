/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.utils.QualityUtils
import com.tencent.devops.quality.QualityGateInElement
import com.tencent.devops.quality.QualityGateOutElement
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RuleCheckResult
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Service
class PipelineBuildQualityService(
    private val client: Client,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildDetailService: PipelineBuildDetailService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildQualityService::class.java)
    }

    fun buildManualQualityGateReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ) {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw NotFoundException("流水线${pipelineId}不存在")

        if (pipelineInfo.channelCode != channelCode) {
            throw NotFoundException("流水线${pipelineId}不存在")
        }

        val modelDetail = buildDetailService.get(buildId)
            ?: throw NotFoundException("构建任务${buildId}不存在")

        var find = false
        var taskType = ""
//        var elementName = ""
        modelDetail.model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach {
                it.elements.forEach { element ->
                    logger.info("${element.id}, ${element.name}")
                    if ((element is QualityGateInElement || element is QualityGateOutElement) && element.id == elementId) {
                        find = true
//                        elementName = element.name
                        if (element is QualityGateInElement) {
                            taskType = element.interceptTask!!
                        }
                        if (element is QualityGateOutElement) {
                            taskType = element.interceptTask!!
                        }
                        return@forEachIndexed
                    }
                }
            }
        }

        if (!find) {
            logger.warn("The element($elementId) of pipeline($pipelineId) is not exist")
            throw NotFoundException("原子${elementId}不存在")
        }

        // 校验审核权限
        val auditUserSet = getAuditUserList(client, projectId, pipelineId, buildId, taskType)
        if (!auditUserSet.contains(userId)) {
            throw PermissionForbiddenException("用户($userId)不在审核人员名单中")
        }

        logger.info("[$buildId]|buildManualReview|taskId=$elementId|userId=$userId|action=$action")
        pipelineRuntimeService.manualDealBuildTask(buildId, elementId, userId, action)
    }

    fun addQualityGateReviewUsers(projectId: String, pipelineId: String, buildId: String, model: Model) {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    if (element is QualityGateInElement && element.status == BuildStatus.REVIEWING.name) {
                        element.reviewUsers = getAuditUserList(
                            client,
                            projectId,
                            pipelineId,
                            buildId,
                            element.interceptTask ?: ""
                        )
                    }
                    if (element is QualityGateOutElement && element.status == BuildStatus.REVIEWING.name) {
                        element.reviewUsers = getAuditUserList(
                            client,
                            projectId,
                            pipelineId,
                            buildId,
                            element.interceptTask ?: ""
                        )
                    }
                }
            }
        }
    }

    fun fillingRuleInOutElement(
        projectId: String,
        pipelineId: String,
        startParams: Map<String, Any>,
        model: Model
    ): Model {
        val templateId = if (model.instanceFromTemplate == true) {
            templatePipelineDao.get(dslContext, pipelineId)?.templateId
        } else {
            null
        }
        val ruleMatchList = getMatchRuleList(projectId, pipelineId, templateId)
        logger.info("Rule match list for pipeline- $pipelineId, template- $templateId($ruleMatchList)")

        val cleaningModel = model.removeElements(setOf(QualityGateInElement.classType, QualityGateOutElement.classType))
        val fillingModel = if (ruleMatchList.isEmpty()) {
            cleaningModel
        } else {
            val convertList = ruleMatchList.map {
                val gatewayIds = it.ruleList.filter { !it.gatewayId.isNullOrBlank() }.map { it.gatewayId!! }
                mapOf("position" to it.controlStage.name,
                        "taskId" to it.taskId,
                        "gatewayIds" to gatewayIds)
            }
            QualityUtils.fillInOutElement(cleaningModel, startParams, convertList)
        }
        logger.info("FillingModel($fillingModel)")

        return fillingModel
    }

    fun getMatchRuleList(projectId: String, pipelineId: String, templateId: String?): List<QualityRuleMatchTask> {
        return try {
            client.get(ServiceQualityRuleResource::class).matchRuleList(
                projectId,
                pipelineId,
                templateId,
                LocalDateTime.now().timestamp()
            ).data ?: listOf()
        } catch (e: Exception) {
            logger.error("quality get match rule list fail: ${e.message}", e)
            return listOf()
        }
    }

    fun getAuditUserList(client: Client, projectId: String, pipelineId: String, buildId: String, taskId: String): Set<String> {
        return try {
            client.get(ServiceQualityRuleResource::class).getAuditUserList(
                projectId,
                pipelineId,
                buildId,
                taskId
            ).data ?: setOf()
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return setOf()
        }
    }

    fun check(client: Client, buildCheckParams: BuildCheckParams): RuleCheckResult {
        return try {
            client.get(ServiceQualityRuleResource::class).check(buildCheckParams).data!!
        } catch (e: Exception) {
            logger.error("quality check fail for build(${buildCheckParams.buildId})", e)
            return RuleCheckResult(
                success = false,
                failEnd = true,
                auditTimeoutSeconds = 15 * 6000,
                resultList = listOf()
            )
        }
    }
}