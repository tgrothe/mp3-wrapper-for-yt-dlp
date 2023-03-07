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
    private final ButtonCommand[] commands;
    private ScheduledExecutorService loopExecutor;
    private Thread singularExecutor;
    private int index = 0;

    public ControlButton(
            final String text1,
            final String text2,
            final boolean loop,
            final ButtonCommand... commands) {
        this.text1 = text1;
        this.text2 = text2;
        this.loop = loop;
        this.commands = commands;
        this.button = new JButton(text1);

        for (int i = 1; i < commands.length; i++) {
            commands[i].setPreviousCommand(commands[i - 1]);
        }
    }

    public synchronized boolean next() {
        final int orgIndex = index;
        final boolean isRunning = orgIndex < commands.length;
        index = (index + 1) % (commands.length + 1);
        new Thread(
                        () -> {
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
                                        loopExecutor.scheduleAtFixedRate(
                                                commands[orgIndex], 0, 3, TimeUnit.SECONDS);
                                    }
                                } else {
                                    if (singularExecutor != null) {
                                        singularExecutor.join();
                                        singularExecutor = null;
                                    }
                                    if (isRunning) {
                                        singularExecutor = new Thread(commands[orgIndex]);
                                        singularExecutor.start();
                                    }
                                }
                                button.setText(isRunning ? text2 : text1);
                            } catch (InterruptedException ex) {
                                Main.exceptionOccurred(ex);
                            }
                        })
                .start();
        return isRunning;
    }
}
