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
package com.iohao.game.action.skeleton.pulse.message;

import com.iohao.game.action.skeleton.protocol.processor.SimpleServerInfo;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 脉冲信号 - 响应
 *
 * @author 渔民小镇
 * @date 2023-04-20
 */
@Setter
@Getter
public final class PulseSignalResponse extends PulseSignalMessage {
    @Serial
    private static final long serialVersionUID = -5782262610772950867L;
    SimpleServerInfo simpleServerInfo;

    @Override
    public String toString() {
        return "PulseSignalResponse{" +
                "simpleServerInfo=" + simpleServerInfo +
                "} " + super.toString();
    }
}
