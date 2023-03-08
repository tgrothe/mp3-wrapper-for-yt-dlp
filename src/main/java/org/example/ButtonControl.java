package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.swing.*;

public class ButtonControl {
    private final List<ControlButton> buttons = new ArrayList<>();

    @SafeVarargs
    public final JButton addButton(
            final String text1,
            final String text2,
            final boolean loop,
            final Function<Object, Callable<Object>>... commands) {
        ControlButton b = new ControlButton(text1, text2, loop, commands);
        buttons.add(b);
        b.button.addActionListener((e) -> nextButtonClick(b));
        return b.button;
    }

    public void clickButton(final int index) {
        nextButtonClick(buttons.get(index));
    }

    private synchronized void nextButtonClick(final ControlButton b) {
        new Thread(
                        () -> {
                            final boolean isRunning = b.startNext();
                            if (isRunning) {
                                if (b == buttons.get(0)) {
                                    for (ControlButton controlButton : buttons) {
                                        if (controlButton != b) {
                                            controlButton.button.setEnabled(false);
                                        }
                                    }
                                } else {
                                    for (ControlButton controlButton : buttons) {
                                        controlButton.button.setEnabled(false);
                                    }
                                }
                            } else {
                                for (ControlButton controlButton : buttons) {
                                    controlButton.button.setEnabled(true);
                                }
                            }
                        })
                .start();
    }
}
