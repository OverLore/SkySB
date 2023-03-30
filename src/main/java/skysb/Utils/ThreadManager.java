package skysb.Utils;

import skysb.skysb.SkySB;

import java.util.HashMap;
import java.util.Map;

public class ThreadManager {

    private final SkySB plugin;
    private final Map<String, Thread> threads;

    public ThreadManager(SkySB plugin) {
        this.plugin = plugin;
        this.threads = new HashMap<>();
    }

    public Thread startThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(name);
        thread.start();
        threads.put(name, thread);

        return thread;
    }

    public boolean stopThread(String name) {
        Thread thread = threads.get(name);
        if (thread == null) {
            return false;
        }
        thread.interrupt();
        threads.remove(name);
        return true;
    }

    public void stopAllThreads() {
        for (Thread thread : threads.values()) {
            thread.interrupt();
        }
        threads.clear();
    }

    public boolean isThreadRunning(String name) {
        Thread thread = threads.get(name);
        return thread != null && thread.isAlive();
    }

}