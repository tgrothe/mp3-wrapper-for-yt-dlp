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
    private Thread threadExecutor;
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

    public synchronized boolean nextClick() {
        try {
            if (index < commands.length) {
                if (loop) {
                    if (loopExecutor != null) {
                        loopExecutor.shutdown();
                        //noinspection ResultOfMethodCallIgnored
                        loopExecutor.awaitTermination(1, TimeUnit.HOURS);
                    }
                    loopExecutor = Executors.newSingleThreadScheduledExecutor();
                    loopExecutor.scheduleAtFixedRate(commands[index], 0, 3, TimeUnit.SECONDS);
                } else {
                    if (threadExecutor != null) {
                        threadExecutor.join();
                    }
                    threadExecutor = new Thread(commands[index]);
                    threadExecutor.start();
                }
                index++;
                button.setText(text2);
                return true;
            }
            if (loop) {
                loopExecutor.shutdown();
                //noinspection ResultOfMethodCallIgnored
                loopExecutor.awaitTermination(1, TimeUnit.HOURS);
                loopExecutor = null;
            } else {
                threadExecutor.join();
                threadExecutor = null;
            }
            index = 0;
            button.setText(text1);
            return false;
        } catch (InterruptedException ex) {
            Main.exceptionOccurred(ex);
            return false;
        }
    }
}
