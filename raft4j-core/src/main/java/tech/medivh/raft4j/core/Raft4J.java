package tech.medivh.raft4j.core;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public enum Raft4J {

    V1(0);

    private final int version;

    Raft4J(int version) {
        this.version = version;
    }

    public static final int CURRENT_VERSION = V1.version;
}
