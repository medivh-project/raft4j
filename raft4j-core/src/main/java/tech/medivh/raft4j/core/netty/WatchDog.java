package tech.medivh.raft4j.core.netty;

import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import tech.medivh.raft4j.core.NodeStateMachine;

/**
 * node state detection.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
@Slf4j
public class WatchDog {

    private final NodeStateMachine owner;

    public WatchDog(NodeStateMachine owner) {
        this.owner = owner;
    }

    private final EventLoopGroup schedule = new DefaultEventLoopGroup(1);

    public void startWatch() {
        schedule.scheduleAtFixedRate(this::watch, 1500, 1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.info("watch dog listen leader heartbeat");
    }
    
    
    private void watch(){
        if(owner.getHeartbeat() < System.currentTimeMillis() - 1500){
            log.warn("leader heartbeat timeout");
        }
    }

}
