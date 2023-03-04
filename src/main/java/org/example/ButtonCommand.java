package org.example;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class ButtonCommand implements Runnable {
    private final Function<Object, Callable<Object>> command;
    private ButtonCommand previousCommand;
    private Object result;

    public ButtonCommand(final Function<Object, Callable<Object>> command) {
        this.command = command;
    }

    @Override
    public void run() {
        try {
            if (previousCommand != null) {
                result = command.apply(previousCommand.result).call();
            } else {
                result = command.apply(null).call();
            }
        } catch (Exception ex) {
            Main.exceptionOccurred(ex);
        }
    }

    public void setPreviousCommand(final ButtonCommand previousCommand) {
        this.previousCommand = previousCommand;
    }
}
