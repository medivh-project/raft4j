package tech.medivh.raft4j.core.netty;

import lombok.Data;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
@Data
public class RaftClientConfig {
    private int processThreadNum = Runtime.getRuntime().availableProcessors();
    
}
