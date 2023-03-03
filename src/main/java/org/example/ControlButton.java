package org.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

public class ControlButton {
    public final JButton button;
    private final String text1;
    private final String text2;
    private final boolean loop;
    private final Runnable[] runs;
    private ScheduledExecutorService loopExecutor;
    private volatile int index = 0;

    public ControlButton(
            final String text1, final String text2, final boolean loop, final Runnable... runs) {
        this.text1 = text1;
        this.text2 = text2;
        this.loop = loop;
        this.runs = runs;
        this.button = new JButton(text1);
    }

    public synchronized boolean nextClick() {
        if (index < runs.length) {
            if (loop) {
                if (loopExecutor == null
                        || loopExecutor.isShutdown()
                        || loopExecutor.isTerminated()) {
                    loopExecutor = Executors.newSingleThreadScheduledExecutor();
                }
                loopExecutor.scheduleAtFixedRate(runs[index], 0, 3, TimeUnit.SECONDS);
            } else {
                new Thread(runs[index]).start();
            }
            index++;
            button.setText(text2);
            return true;
        }
        if (loop) {
            loopExecutor.shutdown();
            try {
                //noinspection ResultOfMethodCallIgnored
                loopExecutor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException ex) {
                Main.exceptionOccurred(ex);
            }
        }
        index = 0;
        button.setText(text1);
        return false;
    }
}
