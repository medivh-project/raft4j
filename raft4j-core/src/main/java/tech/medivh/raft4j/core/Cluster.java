package tech.medivh.raft4j.core;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * cluster info snapshot.
 *
 * @author gongxuanzhangmelt@gmail.com
 **/
public class Cluster implements Iterable<NodeInfo> {

    private final List<NodeInfo> nodeInfos;

    public Cluster(List<NodeInfo> nodeInfos) {
        this.nodeInfos = Collections.unmodifiableList(nodeInfos);
    }

    public NodeInfo select(int index) {
        return nodeInfos.get(index);
    }

    public int nodeCount() {                                                                                         
        return nodeInfos.size();
    }

    @NotNull
    @Override
    public Iterator<NodeInfo> iterator() {
        return nodeInfos.iterator();
    }
}
