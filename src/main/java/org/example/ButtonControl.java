package org.example;

import javax.swing.*;
import java.util.ArrayList;

public class ButtonControl {
    private final ArrayList<ControlButton> buttons = new ArrayList<>();

    public JButton addButton(final String text1, final String text2, final boolean loop, final Runnable... runs) {
        ControlButton b = new ControlButton(text1, text2, loop, runs);
        buttons.add(b);
        b.button.addActionListener((e) -> {
            boolean isRunning = b.nextClick();
            if (isRunning) {
                for (ControlButton controlButton : buttons) {
                    if (controlButton != b) {
                        controlButton.button.setEnabled(false);
                    }
                }
            } else {
                for (ControlButton controlButton : buttons) {
                    if (controlButton != b) {
                        controlButton.button.setEnabled(true);
                    }
                }
            }
        });
        return b.button;
    }

    public void clickButton(final int index) {
        buttons.get(index).button.doClick();
    }
}
