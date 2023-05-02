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
package com.iohao.game.bolt.broker.server;

import com.alipay.remoting.rpc.RpcServer;
import com.iohao.game.action.skeleton.toy.IoGameBanner;
import com.iohao.game.bolt.broker.cluster.BrokerClusterManager;
import com.iohao.game.bolt.broker.cluster.BrokerRunModeEnum;
import com.iohao.game.bolt.broker.server.balanced.BalancedManager;
import com.iohao.game.bolt.broker.server.service.BrokerClientModules;
import com.iohao.game.common.kit.log.IoGameLoggerFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;

/**
 * Broker Server （游戏网关服）
 * <pre>
 *     通过 {@link BrokerServerBuilder#build()} 构建游戏网关服
 * </pre>
 *
 * @author 渔民小镇
 * @date 2022-05-14
 */
@Getter
@Setter(AccessLevel.PACKAGE)
@Accessors(chain = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrokerServer {
    private static final Logger log = IoGameLoggerFactory.getLoggerCommonStdout();
    final BalancedManager balancedManager = new BalancedManager(this);

    /**
     * brokerId （游戏网关的id），服务器唯一标识
     * <pre>
     *     如果没设置，会随机分配一个
     *
     *     逻辑服的模块id，标记不同的逻辑服模块。
     *     开发者随意定义，只要确保每个逻辑服的模块 id 不相同就可以
     * </pre>
     */
    String brokerId;
    /** broker 端口（游戏网关端口） */
    int port;
    /** rpc server */
    RpcServer rpcServer;
    /** broker （游戏网关）的启动模式，默认单机模式 */
    BrokerRunModeEnum brokerRunMode;
    /** 集群管理器 */
    BrokerClusterManager brokerClusterManager;

    BrokerClientModules brokerClientModules;

    BrokerServer() {
    }

    void initRpcServer() {
        this.rpcServer = new RpcServer(this.port, true);
    }

    public void startup() {

        // 启动 bolt rpc
        this.rpcServer.startup();

        log.info("启动游戏网关 port: [{}] 启动模式: [{}] ", this.port, this.brokerRunMode);

        IoGameBanner.render();
        IoGameBanner.me().countDown();
    }

    public void shutdown() {
        this.rpcServer.shutdown();
    }

    public static BrokerServerBuilder newBuilder() {
        return new BrokerServerBuilder();
    }

}
