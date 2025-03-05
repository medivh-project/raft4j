package tech.medivh.raft4j.core.netty.message;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public interface ResponseCode {

    int RESPONSE_FLAG = 1 << 15;

    int SUCCESS = 1 | RESPONSE_FLAG;

    int NOT_SUPPORT = 2 | RESPONSE_FLAG;

    int SYSTEM_ERROR = 3 | RESPONSE_FLAG;


}
