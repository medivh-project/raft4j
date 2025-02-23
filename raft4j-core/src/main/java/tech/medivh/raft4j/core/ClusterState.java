package tech.medivh.raft4j.core;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class ClusterState {
    
    private final EventLoopGroup watchDog = new DefaultEventLoopGroup(1);

    
    
}
