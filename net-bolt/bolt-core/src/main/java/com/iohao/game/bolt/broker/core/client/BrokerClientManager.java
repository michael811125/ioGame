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
package com.iohao.game.bolt.broker.core.client;

import com.alipay.remoting.ConnectionEventProcessor;
import com.alipay.remoting.ConnectionEventType;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.iohao.game.action.skeleton.core.BarSkeleton;
import com.iohao.game.bolt.broker.core.common.IoGameGlobalConfig;
import com.iohao.game.bolt.broker.core.loadbalance.ElementSelector;
import com.iohao.game.bolt.broker.core.loadbalance.ElementSelectorFactory;
import com.iohao.game.bolt.broker.core.loadbalance.RandomElementSelector;
import com.iohao.game.common.kit.log.IoGameLoggerFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jctools.maps.NonBlockingHashMap;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 管理 bolt client ， 接收新的集群信息，并增减相关 bolt client
 *
 * <pre>
 *     BrokerClientItem 与游戏网关是 1:1 的关系，
 *     如果启动了 N 个网关，那么 BrokerClientManager 下的 BrokerClientItem 就会有 N 个。
 * </pre>
 *
 * @author 渔民小镇
 * @date 2022-05-14
 */
@Getter
@Setter
@Accessors(chain = true)
public final class BrokerClientManager {
    static final Logger log = IoGameLoggerFactory.getLoggerCommon();


    /**
     * <pre>
     *     key : address ，broker 的地址，格式：ip:port
     *     value : 与 broker 建立连接的 bolt client
     * </pre>
     */
    final Map<String, BrokerClientItem> brokerClientItemMap = new NonBlockingHashMap<>();

    /** 连接 broker （游戏网关） 的地址 */
    BrokerAddress brokerAddress;
    /** 连接事件 */
    Map<ConnectionEventType, Supplier<ConnectionEventProcessor>> connectionEventProcessorMap;
    /** 用户处理器 */
    List<Supplier<UserProcessor<?>>> processorList;
    /** 业务框架 */
    BarSkeleton barSkeleton;
    /** 元素选择器生产工厂 */
    ElementSelectorFactory<BrokerClientItem> elementSelectorFactory = RandomElementSelector::new;
    /** BrokerClientItem 元素选择器 */
    ElementSelector<BrokerClientItem> elementSelector;
    /** 消息发送超时时间 */
    int timeoutMillis;
    BrokerClient brokerClient;

    public void init() {

        this.elementSelector = elementSelectorFactory.createElementSelector(Collections.emptyList());

        this.register(this.brokerAddress.getAddress());
    }

    public boolean contains(String address) {
        return this.brokerClientItemMap.containsKey(address);
    }

    public void register(String address) {
        BrokerClientItem brokerClientItem = new BrokerClientItem(address)
                .setTimeoutMillis(this.timeoutMillis)
                .setBarSkeleton(this.barSkeleton)
                .setBrokerClient(this.brokerClient)
                .setAwareInject(this.brokerClient.getAwareInject());

        // 添加连接处理器
        connectionEventProcessorMap.forEach((type, valueSupplier) -> {
            var processor = valueSupplier.get();
            brokerClientItem.addConnectionEventProcessor(type, processor);
        });

        // 注册用户处理器
        processorList.stream()
                .map(Supplier::get)
                .forEach(brokerClientItem::registerUserProcessor);

        // 初始化
        brokerClientItem.startup();

        // 添加映射关系
        brokerClientItemMap.put(address, brokerClientItem);

        // 生成负载对象
        this.resetSelector();
    }

    public Set<String> keySet() {
        return new HashSet<>(this.brokerClientItemMap.keySet());
    }

    public void remove(String address) {
        if (IoGameGlobalConfig.openLog) {
            log.info("broker （游戏网关）的机器减少了 address : {}", address);
        }

        // 移除
        this.brokerClientItemMap.remove(address);

        // 生成负载对象
        this.resetSelector();

        if (IoGameGlobalConfig.openLog) {
            log.info("当前网关数量 : {}", this.brokerClientItemMap.size());
        }

        // TODO: 2022/5/13 这里重连需要注意集群与单机的情况

    }

    public void remove(BrokerClientItem brokerClientItem) {
        brokerClientItem.setStatus(BrokerClientItem.Status.DISCONNECT);
        this.resetSelector();
    }

    void resetSelector() {
        // 生成负载对象；注意，这个 List 是不支持序列化的
        List<BrokerClientItem> brokerClientItems = brokerClientItemMap.values()
                .stream()
                .filter(brokerClientItem -> brokerClientItem.getStatus() == BrokerClientItem.Status.ACTIVE)
                .toList();

        // 重置负载对象
        this.elementSelector = this.elementSelectorFactory.createElementSelector(brokerClientItems);
    }

    public int countActiveItem() {
        return (int) brokerClientItemMap.values()
                .stream()
                .filter(brokerClientItem -> brokerClientItem.getStatus() == BrokerClientItem.Status.ACTIVE)
                .count();
    }

    public BrokerClientItem next() {
        return elementSelector.next();
    }

    public List<BrokerClientItem> listBrokerClientItem() {
        return new ArrayList<>(this.brokerClientItemMap.values());
    }

    public void forEach(Consumer<BrokerClientItem> consumer) {
        this.brokerClientItemMap.values().forEach(consumer);
    }
}
