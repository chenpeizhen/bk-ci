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

package com.tencent.devops.plugin.codecc.api

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.codecc.pojo.BlueShieldResponse
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CODECC"], description = "服务-创建异步任务")
@Path("/service/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("plugin") // 指明接入到哪个微服务
interface ServiceCodeccResource {

    @ApiOperation("提供根据流水线构建id查询构建号（页面上展示用的）、构建时间、构建人等信息件")
    @POST
    @Path("/callback")
    fun callback(
        callback: CodeccCallback
    ): Result<String>

    @ApiOperation("提供根据流水线构建id查询构建号（页面上展示用的）、构建时间、构建人等信息件")
    @POST
    @Path("/codeccBuildInfo")
    fun getCodeccBuildInfo(
        @ApiParam(value = "构建id集合", required = true)
        buildIds: Set<String>
    ): Result<Map<String, CodeccBuildInfo>>

    @ApiOperation("根据项目id获取codecc任务信息")
    @POST
    @Path("/task/byProject")
    fun getCodeccTaskByProject(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        projectIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>>

    @ApiOperation("根据流水线id获取codecc任务信息")
    @POST
    @Path("/task/byPipeline")
    fun getCodeccTaskByPipeline(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>>

    @ApiOperation("根据流水线id获取codecc任务结果")
    @POST
    @Path("/task/result")
    fun getCodeccTaskResult(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, CodeccCallback>>

    @ApiOperation("根据构建ID获取CodeCC任务结果")
    @POST
    @Path("/task/result/builds")
    fun getCodeccTaskResult(
        buildIds: Set<String>
    ): Result<Map<String, CodeccCallback>>
}