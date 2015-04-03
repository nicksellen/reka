package reka.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DaemonThreadFactory implements ThreadFactory {
	
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final int priority;
    
    public DaemonThreadFactory(String basename) {
    	this(basename, Thread.NORM_PRIORITY);
    }
    
    public DaemonThreadFactory(String basename, int priority) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = basename + "-" + poolNumber.getAndIncrement() + "-";
        this.priority = priority;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        
        if (!t.isDaemon()) {
        	t.setDaemon(true);
        }
        
        if (t.getPriority() != priority) {
            t.setPriority(priority);
        }
        
        return t;
    }
    
}