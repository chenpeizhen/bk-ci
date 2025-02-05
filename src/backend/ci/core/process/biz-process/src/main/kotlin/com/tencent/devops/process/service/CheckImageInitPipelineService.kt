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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.CheckImageInitPipelineReq
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketCheckImageElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.process.engine.service.PipelineBuildService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.CheckImageInitPipelineResp
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CheckImageInitPipelineService @Autowired constructor(
    private val pipelineService: PipelineService,
    private val buildService: PipelineBuildService
) {
    private val logger = LoggerFactory.getLogger(CheckImageInitPipelineService::class.java)

    /**
     * 初始化流水线进行验证镜像合法性
     */
    fun initCheckImagePipeline(
        userId: String,
        projectCode: String,
        checkImageInitPipelineReq: CheckImageInitPipelineReq
    ): Result<CheckImageInitPipelineResp> {
        logger.info("initCheckImagePipeline userId is: $userId,projectCode is: $projectCode,checkImageInitPipelineReq is: $checkImageInitPipelineReq")
        var containerSeqId = 0
        val imageCode = checkImageInitPipelineReq.imageCode
        val imageName = checkImageInitPipelineReq.imageName
        val version = checkImageInitPipelineReq.version
        val imageType = checkImageInitPipelineReq.imageType
        // stage-1
        val stageFirstElement = ManualTriggerElement(id = "T-1-1-1")
        val stageFirstElements = listOf<Element>(stageFirstElement)
        val params = mutableListOf<BuildFormProperty>()
        params.add(BuildFormProperty("imageCode", true, BuildFormPropertyType.STRING, imageCode, null, null,
            null, null, null, null, null, null))
        params.add(BuildFormProperty("imageName", true, BuildFormPropertyType.STRING, imageName, null, null,
            null, null, null, null, null, null))
        params.add(BuildFormProperty("version", true, BuildFormPropertyType.STRING, version, null, null,
            null, null, null, null, null, null))
        if (null != imageType) {
            params.add(BuildFormProperty("imageType", true, BuildFormPropertyType.STRING, imageType, null, null,
                null, null, null, null, null, null))
        }
        val stageFirstContainer = TriggerContainer(
            id = containerSeqId.toString(),
            name = "构建触发",
            elements = stageFirstElements,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = params,
            buildNo = null
        )
        containerSeqId++
        val stageFirstContainers = listOf<Container>(stageFirstContainer)
        val stageFirst = Stage(stageFirstContainers, "stage-1")
        // stage-2
        val stageSecondCheckImageElement = MarketCheckImageElement(
            id = "T-2-1-1",
            registryUser = checkImageInitPipelineReq.registryUser,
            registryPwd = checkImageInitPipelineReq.registryPwd
        )
        val stageSecondElements = listOf(stageSecondCheckImageElement)
        val stageSecondContainer = VMBuildContainer(
            id = containerSeqId.toString(),
            elements = stageSecondElements,
            baseOS = VMBaseOS.LINUX,
            vmNames = emptySet(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 480,
            buildEnv = mapOf(),
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = DockerDispatchType(DockerVersion.TLINUX2_2.value)
        )
        val stageSecondContainers = listOf<Container>(stageSecondContainer)
        val stageSecond = Stage(stageSecondContainers, "stage-2")
        val stages = mutableListOf(stageFirst, stageSecond)
        val pipelineName = "im-$projectCode-$imageCode-${System.currentTimeMillis()}"
        val model = Model(pipelineName, pipelineName, stages)
        logger.info("model is:$model")
        // 保存流水线信息
        val pipelineId = pipelineService.createPipeline(userId, projectCode, model, ChannelCode.AM)
        logger.info("createPipeline result is:$pipelineId")
        // 异步启动流水线
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams["imageCode"] = imageCode
        startParams["imageName"] = imageName
        startParams["version"] = version
        if (null != imageType)
        startParams["imageType"] = imageType
        var imageCheckStatus = ImageStatusEnum.CHECKING
        var buildId: String? = null
        try {
            buildId = buildService.buildManualStartup(
                userId = userId,
                startType = StartType.SERVICE,
                projectId = projectCode,
                pipelineId = pipelineId,
                values = startParams,
                channelCode = ChannelCode.AM,
                checkPermission = false,
                isMobile = false,
                startByMessage = null
            )
            logger.info("buildManualStartup result is:$buildId")
        } catch (e: Exception) {
            logger.info("buildManualStartup error is :$e", e)
            imageCheckStatus = ImageStatusEnum.CHECK_FAIL
        }
        return Result(CheckImageInitPipelineResp(pipelineId, buildId, imageCheckStatus))
    }
}
