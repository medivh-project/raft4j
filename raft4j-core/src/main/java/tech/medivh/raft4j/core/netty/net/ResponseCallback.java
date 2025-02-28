package tech.medivh.raft4j.core.netty.net;

import tech.medivh.raft4j.core.RaftMessage;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface ResponseCallback {

    /**
     * @param response
     **/
    void onResponse(RaftMessage response);
}
