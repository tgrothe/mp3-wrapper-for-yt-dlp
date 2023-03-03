package org.example;

import io.codeworth.panelmatic.PanelBuilder;
import io.codeworth.panelmatic.PanelMatic;

import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private final Props props = new Props();
    private final JTextField fieldSrc = new JTextField(props.properties.getProperty("fieldSrc"));
    private final JTextField fieldDst = new JTextField(props.properties.getProperty("fieldDst"));
    private final JTextField fieldReg = new JTextField(props.properties.getProperty("fieldReg"));
    private final JTextArea fieldReg2 = new JTextArea(props.properties.getProperty("fieldReg2"));
    private final JTextField fieldCmd = new JTextField(props.properties.getProperty("fieldCmd"));
    private final JCheckBox boxRename =
            new JCheckBox("", Boolean.parseBoolean(props.properties.getProperty("boxRename")));
    private final JCheckBox boxCopy =
            new JCheckBox("", Boolean.parseBoolean(props.properties.getProperty("boxCopy")));
    private final JTextArea area = new JTextArea("Copy your YouTube URL to clipboard...\n\n");
    private final JButton[] buttons = {new JButton("Start!"), new JButton("Bulk processing"), new JButton("Settings")};
    private final Task[] tasks = new Task[buttons.length];
    private volatile boolean isRunning = false;
    private Method renameMethod;

    public static void exceptionOccurred(final Exception ex) {
        JOptionPane.showMessageDialog(
                null,
                "An exception occurred!\n\n" + ex,
                "Exception ex",
                JOptionPane.WARNING_MESSAGE);
        System.exit(0);
    }

    public class Task {
        private final JButton button;
        private final String buttonText;
        private final Runnable runnable;
        private final boolean loop;
        private ScheduledExecutorService executor;

        public Task(final JButton button, final Runnable runnable, final boolean loop) {
            this.button = button;
            this.buttonText = button.getText();
            this.runnable = runnable;
            this.loop = loop;
        }

        public void start() {
            button.setText("Running...");
            for (JButton b : buttons) {
                if (b != button) {
                    b.setEnabled(false);
                }
            }
            isRunning = true;
            executor = Executors.newSingleThreadScheduledExecutor();
            if (loop) {
                executor.scheduleAtFixedRate(runnable, 0, 3, TimeUnit.SECONDS);
            } else {
                executor.execute(runnable);
            }
        }

        public void stop() {
            button.setText("wait...");
            isRunning = false;
            executor.shutdown();
            new Thread(() -> {
                try {
                    if (executor.awaitTermination(10, TimeUnit.MINUTES)) {
                        button.setText(buttonText);
                        for (JButton b : buttons) {
                            if (b != button) {
                                b.setEnabled(true);
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Main.exceptionOccurred(ex);
                }
            }).start();
        }
    }

    public Main() {
        fieldReg2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);

        JPanel panel = new JPanel(new FlowLayout());
        for (JButton b : buttons) {
            panel.add(b);
        }

        JFrame frame = new JFrame("Download, rename, copy/sync");
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(area));
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        isRunning = false;
                    }
                });
        frame.setVisible(true);

        tasks[0] = new Task(buttons[0], this::buttonAction0, true);
        tasks[1] = new Task(buttons[1], () -> buttonAction1(frame), false);
        tasks[2] = new Task(buttons[2], () -> buttonAction2(frame), false);
        for (int i = 0; i < buttons.length; i++) {
            final int fi = i;
            buttons[fi].addActionListener(
                    e -> {
                        if (isRunning) {
                            tasks[fi].stop();
                        } else {
                            tasks[fi].start();
                        }
                    });
        }

        buttons[2].doClick();
    }

    private void append(final String s) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            area.append(s + "\n");
            area.setCaretPosition(area.getText().length());
            area.revalidate();
            area.repaint();
        } else {
            SwingUtilities.invokeAndWait(
                    () -> {
                        area.append(s + "\n");
                        area.setCaretPosition(area.getText().length());
                    });
        }
    }

    private void buttonAction0() {
        try {
            Pattern pat1 = Pattern.compile(fieldReg.getText());
            String data =
                    (String)
                            Toolkit.getDefaultToolkit()
                                    .getSystemClipboard()
                                    .getData(DataFlavor.stringFlavor);
            if (data != null && data.matches(pat1.pattern())) {
                StringSelection dummyStr =
                        new StringSelection("dummy text");
                Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(dummyStr, dummyStr);
                Matcher mat1 = pat1.matcher(data);
                if (mat1.find()) {
                    String videoId = mat1.group(1);
                    processVideoId(videoId);
                }
            }
        } catch (Exception ex) {
            exceptionOccurred(ex);
        }
    }

    /*
    Naming: Please see here: https://english.stackexchange.com/questions/141884/which-is-a-better-and-commonly-used-word-bulk-or-batch
     */
    private void buttonAction1(final JFrame frame) {
        final JTextArea urls = new JTextArea();
        urls.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        final JDialog dialog = new JDialog(frame, "Bulk processing", true);
        dialog.getContentPane()
                .add(
                        PanelMatic.begin()
                                .addHeader(
                                        PanelBuilder.HeaderLevel.H3, "Add one URL per line here...")
                                .add("URLs:", urls)
                                .get());
        dialog.pack();
        dialog.setSize(600, 600);
        dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        startBulk(urls.getText());
                    }
                });
        dialog.setVisible(true);
    }

    private void buttonAction2(final JFrame frame) {
        final JDialog dialog = new JDialog(frame, "Customization", true);
        dialog.getContentPane()
                .add(
                        PanelMatic.begin()
                                .addHeader(PanelBuilder.HeaderLevel.H3, "Customization")
                                .add("Source folder:", fieldSrc)
                                .add("To copy folder:", fieldDst)
                                .add("RegEx (do not change):", fieldReg)
                                .add("Replace RegEx:", fieldReg2)
                                .add("CLI Command (do not change):", fieldCmd)
                                .add("Should rename?", boxRename)
                                .add("Should copy/sync?", boxCopy)
                                .get());
        dialog.pack();
        dialog.setSize(dialog.getWidth() + 50, dialog.getHeight());
        dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(final WindowEvent e) {
                        fieldSrc.setText(
                                new File(fieldSrc.getText()).getAbsolutePath() + File.separator);
                        fieldDst.setText(
                                new File(fieldDst.getText()).getAbsolutePath() + File.separator);
                        props.storeProps(
                                fieldSrc.getText(),
                                fieldDst.getText(),
                                fieldReg.getText(),
                                fieldReg2.getText(),
                                fieldCmd.getText(),
                                boxRename.isSelected(),
                                boxCopy.isSelected());
                        tasks[2].stop();
                    }
                });
        dialog.setVisible(true);
    }

    private void startBulk(final String urls) {
        new Thread(() -> {
            try {
                Pattern pat1 = Pattern.compile(fieldReg.getText());
                String[] lines = urls.split("\n");
                for (String l : lines) {
                    if (l != null && !l.isBlank() && l.matches(pat1.pattern())) {
                        Matcher mat1 = pat1.matcher(l);
                        if (mat1.find()) {
                            String videoId = mat1.group(1);
                            processVideoId(videoId);
                        }
                    }
                }
                tasks[1].stop();
            } catch (Exception ex) {
                exceptionOccurred(ex);
            }
        }).start();
    }

    private void processVideoId(final String videoId) throws Exception {
        append("Found video id " + videoId);
        download(videoId);
        compileRenamer();
        renameFiles();
        copyFiles();
        append("Finish video id " + videoId);
    }

    private void download(final String videoId) throws Exception {
        append("Start download " + videoId);
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        //noinspection deprecation
        Process pr = rt.exec(String.format(fieldCmd.getText(), fieldSrc.getText(), videoId));
        pr.waitFor();
        append("Finish download " + videoId);
    }

    private void compileRenamer() throws Exception {
        append("Start compile renamer");
        // Prepare source somehow.
        String source = fieldReg2.getText();
        // Save source in .java file.
        File root = Files.createTempDirectory("java").toFile();
        File sourceFile = new File(root, "AdvancedRenamer.java");
        append("mkdirs = " + sourceFile.getParentFile().mkdirs());
        append("write = " + Files.writeString(sourceFile.toPath(), source));
        // Compile source file.
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        append("compiler.run = " + compiler.run(null, null, null, sourceFile.getPath()));
        // Load and instantiate compiled class.
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
        Class<?> cls = Class.forName("AdvancedRenamer", true, classLoader);
        renameMethod = cls.getDeclaredMethod("rename", String.class);
        //  Object instance = cls.getDeclaredConstructor().newInstance();
        append("Finish compile renamer");
    }

    private void renameFiles() throws Exception {
        if (!boxRename.isSelected()) {
            return;
        }
        append("Start rename files");
        File[] files = new File(fieldSrc.getText()).listFiles();
        assert files != null;
        for (File f : files) {
            String fn = f.getName();
            if (fn.endsWith(".mp3")) {
                String prefix = fn.substring(0, fn.lastIndexOf('.'));
                prefix = (String) renameMethod.invoke(null, prefix);
                if (!new File(fieldSrc.getText() + prefix + ".mp3").exists()) {
                    String a = f.getAbsolutePath();
                    String b = fieldSrc.getText() + prefix + ".mp3";
                    append(
                            String.format(
                                    "Rename %s to %s ... %b",
                                    a, b, new File(a).renameTo(new File(b))));
                }
            }
        }
        append("Finish rename files");
    }

    private void copyFiles() throws Exception {
        if (!new File(fieldDst.getText()).exists() || !boxCopy.isSelected()) {
            return;
        }
        append("Start copy files");
        File[] files = new File(fieldSrc.getText()).listFiles();
        assert files != null;
        for (File f : files) {
            String fn = f.getName();
            if (fn.endsWith(".mp3")) {
                String prefix = fn.substring(0, fn.lastIndexOf('.'));
                prefix = (String) renameMethod.invoke(null, prefix);
                if (!new File(fieldDst.getText() + prefix + ".mp3").exists()) {
                    String a = f.getAbsolutePath();
                    String b = fieldDst.getText() + prefix + ".mp3";
                    append(String.format("Copy %s to %s ...", a, b));
                    Files.copy(Path.of(a), Path.of(b), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        append("Finish copy files");
    }

    public static void main(final String[] args) {
        new Main();
    }
}
