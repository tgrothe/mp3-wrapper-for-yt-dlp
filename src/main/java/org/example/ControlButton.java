package org.example;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.swing.*;

public class ControlButton {
    public final JButton button;
    private final String text1;
    private final String text2;
    private final boolean loop;
    private final CommandGroup commandGroup;
    private ScheduledExecutorService loopExecutor;
    private Thread singularExecutor;

    public ControlButton(
            final String text1,
            final String text2,
            final boolean loop,
            final Function<Object, Callable<Object>>[] commands) {
        this.text1 = text1;
        this.text2 = text2;
        this.loop = loop;
        this.commandGroup = new CommandGroup(commands);
        this.button = new JButton(text1);
    }

    public synchronized boolean startNext() {
        final Runnable runnable = commandGroup.getNextRunnable();
        final boolean isRunning = commandGroup.isRunning();
        try {
            if (loop) {
                if (loopExecutor != null) {
                    loopExecutor.shutdown();
                    //noinspection ResultOfMethodCallIgnored
                    loopExecutor.awaitTermination(1, TimeUnit.HOURS);
                    loopExecutor = null;
                }
                if (isRunning) {
                    loopExecutor = Executors.newSingleThreadScheduledExecutor();
                    loopExecutor.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
                }
            } else {
                if (singularExecutor != null) {
                    singularExecutor.join();
                    singularExecutor = null;
                }
                if (isRunning) {
                    singularExecutor = new Thread(runnable);
                    singularExecutor.start();
                }
            }
            button.setText(isRunning ? text2 : text1);
        } catch (InterruptedException ex) {
            Main.exceptionOccurred(ex);
        }
        return isRunning;
    }
}
