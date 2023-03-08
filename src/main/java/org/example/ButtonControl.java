package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import javax.swing.*;

public class ButtonControl {
    private final List<ControlButton> buttons = new ArrayList<>();

    @SafeVarargs
    public final JButton addButton(
            final String text1,
            final String text2,
            final boolean loop,
            final BiFunction<ControlButton, Object, Callable<Object>>... commands) {
        ControlButton controlButton = new ControlButton(text1, text2, loop, commands);
        buttons.add(controlButton);
        controlButton.button.addActionListener((e) -> nextCommand(controlButton));
        return controlButton.button;
    }

    public synchronized void nextCommand(final ControlButton controlButton) {
        new Thread(
                        () -> {
                            final boolean isRunning = controlButton.startNext();
                            if (isRunning) {
                                if (controlButton == buttons.get(0)) {
                                    for (ControlButton cb : buttons) {
                                        if (cb != controlButton) {
                                            cb.button.setEnabled(false);
                                        }
                                    }
                                } else {
                                    for (ControlButton cb : buttons) {
                                        cb.button.setEnabled(false);
                                    }
                                }
                            } else {
                                for (ControlButton cb : buttons) {
                                    cb.button.setEnabled(true);
                                }
                            }
                        })
                .start();
    }
}
