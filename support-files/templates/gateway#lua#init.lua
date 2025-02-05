--[[
Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.

Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.

BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.

A copy of the MIT License is included in this file.


Terms of the MIT License:
---------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]]

config = {
  evn = "__BKCI_ENV__",
  static_dir = "__INSTALL_PATH__/__MODULE__/frontend",
  static_dir_gray = "__INSTALL_PATH__/__MODULE__/frontend-gray",
  docs_dir = "__INSTALL_PATH__/__MODULE__/docs",
  static_dir_codecc = "__INSTALL_PATH__/codecc/frontend",
  http_schema = "__HTTP_SCHEMA__", -- 蓝鲸PaaS平台访问协议 http or https, 如果有对接才配置修改，开源默认没对接
  paas_domain = "__PAAS_FQDN__",   -- 蓝鲸PaaS平台域名, 如果有对接才配置修改，开源默认没对接
  service_name = "",  -- 指定后台微服务名称，如果对接后端是boot-assembly的单体微服务，则该配置项为bk-ci, 否则请置空会自动路由相应微服务
  allow_hosts = {
    __BKCI_ALLOW_HOST__
  },
  allow_headers = "Authorization,Content-Type,Accept,Origin,User-Agent,Cache-Control,Keep-Alive,X-Requested-With,If-Modified-Since,X-CSRFToken,X-DEVOPS-PROJECT-ID,X-DEVOPS-TASK-ID",
  ns = {
    ip = {
      "127.0.0.1"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  ns_gray = {
    ip = {
      "127.0.0.1"
    },
    port = __BKCI_CONSUL_DNS_PORT__,
    http_port = __BKCI_CONSUL_PORT__,
    domain = "__BKCI_CONSUL_DOMAIN__",
    tag = "__BKCI_CONSUL_TAG__",
    nodes_url = "/v1/catalog/nodes"
  },
  paasCIDomain = "__BKCI_PAASCI_FQDN__",
  job = {
    domain = "__BKCI_JOB_FQDN__"
  },
  redis = {
    host = "__REDIS_IP0__",
    port = __REDIS_PORT__,
    pass = "__REDIS_PASS__",  -- redis 密码，没有密码的话，把这行注释掉
    database = __REDIS_DB__,         -- 默认选择db0
    max_idle_time = 600000, -- 保留在连接池的时间
    pool_size = 10         -- 连接池的大小
  },
  oauth = {  -- 对接蓝鲸权限中心才需要的配置
    ip = "__IAM_IP0__",
    env = "__IAM_ENV__",
    port = "__IAM_HTTP_PORT__",
    host = "__IAM_HOST__",
    url = "__IAM_TOKEN_URL__",     -- 接口路径
    app_code = "__APP_CODE__",
    app_secret = "__APP_TOKEN__",
  },
  artifactory = {
    port = "__JFROG_HTTP_PORT__",
    docker = "__DOCKER_PORT__",
    host = "__BKCI_GATEWAY_IP0__",
    domain = "__JFROG_FQDN__",
    user = "__JFROG_USERNAME__",
    password = "__JFROG_PASSWORD__"
  },
  influxdb = {
    ip = "__INFLUXDB_IP0__",
    port = "__INFLUXDB_PORT__",
    db = "__INFLUXDB_DB__",
    user = "__INFLUXDB_USERNAME__",
    password = "__INFLUXDB_PASSWORD__"
  },
  itlogin = {
    ip = "__BKCI_ITLOGIN_IP__",
    port = __BKCI_ITLOGIN_PORT__,
    host = "__BKCI_ITLOGIN_HOST__"
  },
  bkrepo = {
    domain = "__BKREPO_HOST__"
  }
}
  
require("init_common")
require("ip_whitelist")
  