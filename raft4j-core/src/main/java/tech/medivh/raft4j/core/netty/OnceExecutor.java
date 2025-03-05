package tech.medivh.raft4j.core.netty;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author gongxuanzhangmelt@gmail.com
 **/
public class OnceExecutor {

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private volatile State state = State.INIT;

    private final Runnable runnable;

    public OnceExecutor(Runnable runnable) {
        this.runnable = runnable;
    }

    public void execute() {
        if (isRunning.compareAndSet(false, true)) {
            state = State.RUNNING;
            runnable.run();
            state = State.FINISH;
        }
    }

    public State getState() {
        return state;
    }

    public enum State {
        INIT,
        RUNNING,
        FINISH
    }
}
