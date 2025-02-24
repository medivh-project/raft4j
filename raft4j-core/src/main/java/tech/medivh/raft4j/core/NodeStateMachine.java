package tech.medivh.raft4j.core;

import lombok.Data;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Data
public class NodeStateMachine {

    private volatile NodeState state = NodeState.FOLLOWER;

    private volatile long heartbeat = 0;


}
