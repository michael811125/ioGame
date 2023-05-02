/*
 * ioGame 
 * Copyright (C) 2021 - 2023  渔民小镇 （262610965@qq.com、luoyizhu@gmail.com） . All Rights Reserved.
 * # iohao.com . 渔民小镇
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.iohao.game.bolt.broker.server.processor;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.Connection;
import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcServer;
import com.iohao.game.action.skeleton.core.exception.ActionErrorEnum;
import com.iohao.game.action.skeleton.protocol.HeadMetadata;
import com.iohao.game.action.skeleton.protocol.RequestMessage;
import com.iohao.game.action.skeleton.protocol.ResponseMessage;
import com.iohao.game.bolt.broker.core.common.AbstractAsyncUserProcessor;
import com.iohao.game.bolt.broker.core.common.IoGameGlobalConfig;
import com.iohao.game.bolt.broker.server.BrokerServer;
import com.iohao.game.bolt.broker.server.aware.BrokerServerAware;
import com.iohao.game.bolt.broker.server.balanced.BalancedManager;
import com.iohao.game.bolt.broker.server.balanced.ExternalBrokerClientLoadBalanced;
import com.iohao.game.bolt.broker.server.balanced.LogicBrokerClientLoadBalanced;
import com.iohao.game.bolt.broker.server.balanced.region.BrokerClientProxy;
import com.iohao.game.bolt.broker.server.balanced.region.BrokerClientRegion;
import com.iohao.game.common.kit.log.IoGameLoggerFactory;
import lombok.Setter;
import org.slf4j.Logger;

/**
 * 对外服务器消息处理
 * <pre>
 *     接收真实用户的请求，把请求转发到逻辑服
 * </pre>
 *
 * @author 渔民小镇
 * @date 2022-05-14
 */
public class ExternalRequestMessageBrokerProcessor extends AbstractAsyncUserProcessor<RequestMessage>
        implements BrokerServerAware {
    static final Logger log = IoGameLoggerFactory.getLoggerMsg();

    @Setter
    BrokerServer brokerServer;

    @Override
    public void handleRequest(BizContext bizCtx, AsyncContext asyncCtx, RequestMessage request) {
        if (IoGameGlobalConfig.requestResponseLog) {
            extractedPrint(request);
        }

        // 逻辑服的负载均衡
        BalancedManager balancedManager = brokerServer.getBalancedManager();
        LogicBrokerClientLoadBalanced loadBalanced = balancedManager.getLogicBalanced();

        // 得到路由对应的逻辑服区域
        HeadMetadata headMetadata = request.getHeadMetadata();
        int cmdMerge = headMetadata.getCmdMerge();
        BrokerClientRegion brokerClientRegion = loadBalanced.getBrokerClientRegion(cmdMerge);

        if (brokerClientRegion == null) {
            //  通知对外服， 路由不存在
            extractedNotRoute(bizCtx, request);
            return;
        }

        // 从逻辑服区域得到一个逻辑服来处理请求
        BrokerClientProxy brokerClientProxy = brokerClientRegion.getBrokerClientProxy(headMetadata);
        if (brokerClientProxy == null) {
            //  通知对外服， 路由不存在
            extractedNotRoute(bizCtx, request);
            return;
        }

        try {
            brokerClientProxy.oneway(request);
        } catch (RemotingException | InterruptedException | NullPointerException e) {
            log.error(e.getMessage(), e);
        }

    }

    private void extractedPrint(RequestMessage request) {

        log.info("游戏网关把对外服 请求 转发到逻辑服 : {}", request);

        BalancedManager balancedManager = brokerServer.getBalancedManager();
        ExternalBrokerClientLoadBalanced externalLoadBalanced = balancedManager.getExternalLoadBalanced();

        for (BrokerClientProxy brokerClientProxy : externalLoadBalanced.listBrokerClientProxy()) {
            log.info("brokerClientProxy : {}", brokerClientProxy);
        }
    }

    private void extractedNotRoute(BizContext bizCtx, RequestMessage requestMessage) {
        // 路由不存在
        Connection connection = bizCtx.getConnection();
        ResponseMessage responseMessage = requestMessage.createResponseMessage();

        ActionErrorEnum errorCode = ActionErrorEnum.cmdInfoErrorCode;
        responseMessage.setValidatorMsg(errorCode.getMsg())
                .setResponseStatus(errorCode.getCode());

        RpcServer rpcServer = brokerServer.getRpcServer();

        try {
            rpcServer.oneway(connection, responseMessage);
        } catch (RemotingException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String interest() {
        return RequestMessage.class.getName();
    }
}
