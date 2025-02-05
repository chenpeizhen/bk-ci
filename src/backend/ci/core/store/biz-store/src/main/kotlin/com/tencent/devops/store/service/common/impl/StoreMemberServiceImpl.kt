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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TStoreMemberRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.STORE_MEMBER_ADD_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.STORE_MEMBER_DELETE_NOTIFY_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreNotifyService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors

abstract class StoreMemberServiceImpl : StoreMemberService {

    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeNotifyService: StoreNotifyService

    private val executorService = Executors.newFixedThreadPool(5)

    private val logger = LoggerFactory.getLogger(StoreMemberService::class.java)

    /**
     * store组件成员列表
     */
    override fun list(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMemberItem?>> {
        logger.info("getStoreMemberList userId is:$userId,storeCode is:$storeCode,storeType is:$storeType")
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val records = storeMemberDao.list(dslContext, storeCode, null, storeType.type.toByte())
        logger.info("getStoreMemberList records is:$records")
        // 获取调试项目对应的名称
        val projectCodeList = mutableListOf<String>()
        records?.forEach {
            val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, it.username, storeCode, storeType)
            if (null != testProjectCode) projectCodeList.add(testProjectCode)
        }
        logger.info("getStoreMemberList projectCodeList is:$projectCodeList")
        val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
        val members = mutableListOf<StoreMemberItem?>()
        records?.forEach {
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, it.username, storeCode, storeType)
            members.add(
                generateStoreMemberItem(it, projectMap?.get(projectCode) ?: "")
            )
        }
        return Result(members)
    }

    /**
     * 查看store组件成员信息
     */
    override fun viewMemberInfo(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<StoreMemberItem?> {
        logger.info("viewMemberInfo userId is:$userId,storeCode is:$storeCode,storeType is:$storeType")
        val memberRecord = storeMemberDao.getMemberInfo(dslContext, userId, storeCode, storeType.type.toByte())
        logger.info("viewMemberInfo memberRecord is:$memberRecord")
        return if (null != memberRecord) {
            // 获取调试项目对应的名称
            val projectCodeList = mutableListOf<String>()
            val projectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, memberRecord.username, storeCode, storeType)
            if (null != projectCode) projectCodeList.add(projectCode)
            logger.info("getStoreMemberList projectCodeList is:$projectCodeList")
            val projectMap = client.get(ServiceProjectResource::class).getNameByCode(projectCodeList.joinToString(",")).data
            Result(generateStoreMemberItem(memberRecord, projectMap?.get(projectCode) ?: ""))
        } else {
            Result(data = null)
        }
    }

    override fun batchListMember(storeCodeList: List<String?>, storeType: StoreTypeEnum): Result<HashMap<String, MutableList<String>>> {
        val ret = hashMapOf<String, MutableList<String>>()
        val records = storeMemberDao.batchList(dslContext, storeCodeList, storeType.type.toByte())
        records?.forEach {
            val list = if (ret.containsKey(it.storeCode)) {
                ret[it.storeCode]!!
            } else {
                val tmp = mutableListOf<String>()
                ret[it.storeCode] = tmp
                tmp
            }
            list.add(it.username)
        }
        return Result(ret)
    }

    /**
     * 添加store组件成员
     */
    override fun add(userId: String, storeMemberReq: StoreMemberReq, storeType: StoreTypeEnum, collaborationFlag: Boolean?): Result<Boolean> {
        logger.info("addMember userId is:$userId,storeMemberReq is:$storeMemberReq,storeType is:$storeType")
        val storeCode = storeMemberReq.storeCode
        val type = storeMemberReq.type.type.toByte()
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val receivers = mutableSetOf<String>()
        for (item in storeMemberReq.member) {
            if (storeMemberDao.isStoreMember(dslContext, item, storeCode, storeType.type.toByte())) {
                continue
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.addStoreMember(context, userId, storeCode, item, type, storeType.type.toByte())
                if (null != collaborationFlag && !collaborationFlag) {
                    // 协作申请方式，添加成员时无需再添加调试项目
                    val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(context, userId, storeCode, storeType)
                    storeProjectRelDao.addStoreProjectRel(
                        dslContext = context,
                        userId = item,
                        storeCode = storeCode,
                        projectCode = testProjectCode!!,
                        type = StoreProjectTypeEnum.TEST.type.toByte(),
                        storeType = storeType.type.toByte()
                    )
                }
            }
            receivers.add(item)
        }
        executorService.submit<Result<Boolean>> {
            val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(storeCode))
            storeNotifyService.sendNotifyMessage(
                templateCode = STORE_MEMBER_ADD_NOTIFY_TEMPLATE + "_$storeType",
                sender = DEVOPS,
                receivers = receivers,
                bodyParams = bodyParams
            )
        }
        return Result(true)
    }

    /**
     * 删除store组件成员
     */
    override fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        logger.info("deleteMember userId is:$userId,id is:$id,storeCode is:$storeCode,storeType is:$storeType")
        if (!storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val record = storeMemberDao.getById(dslContext, id)
        if (record != null) {
            if ((record.type).toInt() == 0) {
                val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
                if (validateAdminResult.isNotOk()) {
                    return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
                }
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                storeMemberDao.delete(context, id)
                // 删除成员对应的调试项目
                storeProjectRelDao.deleteUserStoreTestProject(
                    dslContext = context,
                    userId = record.username,
                    storeProjectType = StoreProjectTypeEnum.TEST,
                    storeCode = storeCode,
                    storeType = storeType
                )
            }
            executorService.submit<Result<Boolean>> {
                val receivers = mutableSetOf(record.username)
                val bodyParams = mapOf("storeAdmin" to userId, "storeName" to getStoreName(storeCode))
                storeNotifyService.sendNotifyMessage(
                    templateCode = STORE_MEMBER_DELETE_NOTIFY_TEMPLATE + "_$storeType",
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    /**
     * 获取组件名称
     */
    abstract fun getStoreName(storeCode: String): String

    /**
     * 更改store组件成员的调试项目
     */
    override fun changeMemberTestProjectCode(
        accessToken: String,
        userId: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        logger.info("changeMemberTestProjectCode userId is:$userId,accessToken is:$accessToken")
        logger.info("changeMemberTestProjectCode projectCode is:$projectCode,storeCode is:$storeCode,storeType is:$storeType")
        if (!storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType.type.toByte())) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val validateFlag: Boolean?
        try {
            // 判断用户是否项目的成员
            validateFlag = client.get(ServiceProjectResource::class).verifyUserProjectPermission(
                accessToken = accessToken,
                projectCode = projectCode,
                userId = userId
            ).data
        } catch (e: Exception) {
            logger.error("verifyUserProjectPermission error is :$e", e)
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
        }
        logger.info("the validateFlag is :$validateFlag")
        if (null == validateFlag || !validateFlag) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 更新用户的调试项目
            storeProjectRelDao.updateUserStoreTestProject(
                dslContext = context,
                userId = userId,
                projectCode = projectCode,
                storeProjectType = StoreProjectTypeEnum.TEST,
                storeCode = storeCode,
                storeType = storeType
            )
        }
        return Result(true)
    }

    /**
     * 判断store组件是否有超过一个管理员
     */
    override fun isStoreHasAdmins(storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        val adminCount = storeMemberDao.countAdmin(dslContext, storeCode, storeType.type.toByte())
        if (adminCount <= 1) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_COMPONENT_ADMIN_COUNT_ERROR)
        }
        return Result(true)
    }

    /**
     * 判断是否为成员
     */
    override fun isStoreMember(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreMember(dslContext, userId, storeCode, storeType)
    }

    /**
     * 判断是否为管理员
     */
    override fun isStoreAdmin(userId: String, storeCode: String, storeType: Byte): Boolean {
        return storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType)
    }

    private fun generateStoreMemberItem(memberRecord: TStoreMemberRecord, projectName: String): StoreMemberItem {
        return StoreMemberItem(
            id = memberRecord.id as String,
            userName = memberRecord.username as String,
            projectName = projectName,
            type = StoreMemberTypeEnum.getAtomMemberType((memberRecord.type as Byte).toInt()),
            creator = memberRecord.creator as String,
            modifier = memberRecord.modifier as String,
            createTime = DateTimeUtil.toDateTime(memberRecord.createTime),
            updateTime = DateTimeUtil.toDateTime(memberRecord.updateTime)
        )
    }
}