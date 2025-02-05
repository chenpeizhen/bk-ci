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

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityHisDetailMetadata
import com.tencent.devops.model.quality.tables.TQualityHisOriginMetadata
import com.tencent.devops.model.quality.tables.records.TQualityHisDetailMetadataRecord
import com.tencent.devops.quality.api.v2.pojo.QualityHisMetadata
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class QualityHisMetadataDao {

    fun saveHisOriginMetadata(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String, buildNo: String, callbackStr: String) {
        with(TQualityHisOriginMetadata.T_QUALITY_HIS_ORIGIN_METADATA) {
            dslContext.insertInto(this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    BUILD_NO,
                    RESULT_DATA)
                    .values(
                            projectId,
                            pipelineId,
                            buildId,
                            buildNo,
                            callbackStr
                    )
                    .execute()
        }
    }

    fun batchSaveHisDetailMetadata(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String, buildNo: String, elementType: String, qualityMetadataList: List<QualityHisMetadata>) {
        // BUILD_ID + DATA_ID 唯一
        with(TQualityHisDetailMetadata.T_QUALITY_HIS_DETAIL_METADATA) {
            val insertCommand = qualityMetadataList.map {
                dslContext.insertInto(this,
                        this.DATA_ID,
                        this.DATA_NAME,
                        this.DATA_TYPE,
                        this.DATA_DESC,
                        this.DATA_VALUE,
                        this.ELEMENT_TYPE,
                        this.ELEMENT_DETAIL,
                        this.PROJECT_ID,
                        this.PIPELINE_ID,
                        this.BUILD_ID,
                        this.BUILD_NO,
                        this.EXTRA)
                        .values(
                                it.enName,
                                it.cnName,
                                it.type.name,
                                it.msg,
                                it.value,
                                elementType,
                                it.detail,
                                projectId,
                                pipelineId,
                                buildId,
                                buildNo,
                                it.extra
                        )
                        .onDuplicateKeyUpdate()
                        .set(DATA_TYPE, it.type.name)
                        .set(DATA_DESC, it.msg)
                        .set(DATA_VALUE, it.value)
                        .set(ELEMENT_TYPE, elementType)
                        .set(ELEMENT_DETAIL, it.detail)
                        .set(EXTRA, it.extra)
            }
            dslContext.batch(insertCommand).execute()
        }
    }

    fun getHisMetadata(dslContext: DSLContext, buildId: String): Result<TQualityHisDetailMetadataRecord>? {
        return with(TQualityHisDetailMetadata.T_QUALITY_HIS_DETAIL_METADATA) {
            dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .fetch()
        }
    }
}