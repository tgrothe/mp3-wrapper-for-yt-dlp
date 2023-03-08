package org.example;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CommandGroup {
    private final  ControlButton controlButton;
    private final List<BiFunction<ControlButton, Object, Callable<Object>>> commands = new ArrayList<>();
    private final Deque<Object> previousResults = new ArrayDeque<>();
    private int index = 0;

    public CommandGroup(final ControlButton controlButton, final BiFunction<ControlButton, Object, Callable<Object>>[] commands) {
        this.controlButton = controlButton;
        // See:
        // https://stackoverflow.com/questions/12462079/possible-heap-pollution-via-varargs-parameter
        this.commands.addAll(Arrays.asList(commands));
    }

    public Runnable getNextRunnable() {
        final int fi = index;
        index = (index + 1) % (commands.size() + 1);
        if (fi < commands.size()) {
            return () -> {
                try {
                    BiFunction<ControlButton, Object, Callable<Object>> command = commands.get(fi);
                    Object previousResult;
                    if (previousResults.isEmpty()) {
                        previousResult = command.apply(controlButton, null).call();
                    } else {
                        previousResult = command.apply(controlButton,previousResults.getLast()).call();
                    }
                    if (previousResult != null) {
                        previousResults.add(previousResult);
                    }
                } catch (Exception ex) {
                    Main.exceptionOccurred(ex);
                }
            };
        } else {
            previousResults.clear();
            return null;
        }
    }

    public boolean isRunning() {
        return index > 0;
    }
}
