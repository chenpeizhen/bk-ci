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

package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.BuildCheckParams
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import com.tencent.devops.quality.pojo.RuleCheckResult
import com.tencent.devops.quality.service.v2.QualityRuleCheckService
import com.tencent.devops.quality.service.v2.QualityRuleService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityRuleResourceImpl @Autowired constructor(
    private val ruleCheckService: QualityRuleCheckService,
    private val ruleService: QualityRuleService
) : ServiceQualityRuleResource {

    override fun matchRuleList(projectId: String, pipelineId: String, templateId: String?, startTime: Long): Result<List<QualityRuleMatchTask>> {
        val ruleList = mutableListOf<QualityRuleMatchTask>()
        ruleList.addAll(ruleCheckService.userGetMatchRuleList(projectId, pipelineId))
        ruleList.addAll(ruleCheckService.userGetMatchTemplateList(projectId, templateId))
        return Result(ruleList)
    }

    override fun getAuditUserList(projectId: String, pipelineId: String, buildId: String, taskId: String): Result<Set<String>> {
        return Result(ruleCheckService.getAuditUserList(projectId, pipelineId, buildId, taskId))
    }

    override fun check(buildCheckParams: BuildCheckParams): Result<RuleCheckResult> {
        return Result(ruleCheckService.check(buildCheckParams))
    }

    override fun copyRule(request: CopyRuleRequest): Result<List<String>> {
        return Result(ruleService.copyRule(request))
    }
}
