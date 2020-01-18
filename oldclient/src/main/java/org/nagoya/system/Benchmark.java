package org.nagoya.system;


import org.nagoya.GUICommon;

public class Benchmark {
    private final String name;
    private final long startTime;
    private final boolean use;

    public Benchmark(boolean u, String n) {
        this.name = n + " ";
        this.use = u;
        this.startTime = System.nanoTime();
    }

    public void B(String s) {
        if (this.use) {
            GUICommon.debugMessage(() -> this.name + s + (System.nanoTime() - this.startTime));
        }
    }
}
