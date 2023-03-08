package org.example;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class CommandGroup {
    private final List<Function<Object, Callable<Object>>> commands = new ArrayList<>();
    private final Deque<Object> previousResults = new ArrayDeque<>();
    private int index = 0;

    public CommandGroup(final Function<Object, Callable<Object>>[] commands) {
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
                    Object previousResult;
                    if (previousResults.isEmpty()) {
                        previousResult = commands.get(fi).apply(null).call();
                    } else {
                        previousResult = commands.get(fi).apply(previousResults.getLast()).call();
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
