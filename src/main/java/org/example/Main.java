package org.example;

import io.codeworth.panelmatic.PanelBuilder;
import io.codeworth.panelmatic.PanelMatic;

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
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
    private final ButtonControl control = new ButtonControl();
    private volatile Method renameMethod;
    private volatile String urlsText;

    public static void exceptionOccurred(final Exception ex) {
        JOptionPane.showMessageDialog(
                null,
                "An exception occurred!\n\n" + ex,
                "Exception ex",
                JOptionPane.WARNING_MESSAGE);
        System.exit(0);
    }

    public Main() {
        fieldReg2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);

        JFrame frame = new JFrame("Download, rename, copy/sync");

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(
                control.addButton(
                        "Start!",
                        "runs...",
                        true,
                        (thisControlButton, previousResult) ->
                                () -> {
                                    buttonAction0();
                                    return null;
                                }));
        panel.add(
                control.addButton(
                        "Bulk process",
                        "runs...",
                        false,
                        (thisControlButton, previousResult) ->
                                () -> {
                                    buttonAction1(thisControlButton, frame);
                                    return null;
                                },
                        (thisControlButton, previousResult) ->
                                () -> {
                                    startBulk(thisControlButton);
                                    return null;
                                }));
        panel.add(
                control.addButton(
                        "Settings",
                        "runs...",
                        false,
                        (thisControlButton, previousResult) ->
                                () -> {
                                    buttonAction2(thisControlButton, frame);
                                    return null;
                                }));
        panel.add(
                control.addButton(
                        "Test",
                        "runs...",
                        false,
                        (thisControlButton, previousResult) ->
                                () -> {
                                    control.nextCommand(thisControlButton);
                                    long time = System.currentTimeMillis();
                                    return new long[] {time};
                                },
                        (thisControlButton, previousResult) ->
                                () -> {
                                    control.nextCommand(thisControlButton);
                                    long[] oldTimes = (long[]) previousResult;
                                    long[] newTimes = new long[oldTimes.length + 1];
                                    System.arraycopy(oldTimes, 0, newTimes, 0, oldTimes.length);
                                    newTimes[oldTimes.length] = System.currentTimeMillis();
                                    return newTimes;
                                },
                        (thisControlButton, previousResult) ->
                                () -> {
                                    control.nextCommand(thisControlButton);
                                    long[] oldTimes = (long[]) previousResult;
                                    long[] newTimes = new long[oldTimes.length + 1];
                                    System.arraycopy(oldTimes, 0, newTimes, 0, oldTimes.length);
                                    newTimes[oldTimes.length] = System.currentTimeMillis();
                                    String s = Arrays.toString(newTimes);
                                    append("newTimes = " + s);
                                    JOptionPane.showMessageDialog(frame, "The time was: " + s);
                                    return null;
                                }));

        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(area));
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        ((JButton) panel.getComponents()[2]).doClick();
    }

    private void append(final String s) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            // avoid this / avoid calling this method from EDT:
            // won't work if a modal window is in front
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
                StringSelection dummyStr = new StringSelection("dummy text");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(dummyStr, dummyStr);
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
    private void buttonAction1(final ControlButton thisControlButton, final JFrame frame) {
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
                        urlsText = urls.getText();
                        control.nextCommand(thisControlButton);
                    }
                });
        dialog.setVisible(true);
    }

    private void startBulk(final ControlButton thisControlButton) {
        try {
            Pattern pat1 = Pattern.compile(fieldReg.getText());
            String[] lines = urlsText.split("\n");
            for (String l : lines) {
                if (l != null && !l.isBlank() && l.matches(pat1.pattern())) {
                    Matcher mat1 = pat1.matcher(l);
                    if (mat1.find()) {
                        String videoId = mat1.group(1);
                        processVideoId(videoId);
                    }
                }
            }
            control.nextCommand(thisControlButton);
        } catch (Exception ex) {
            exceptionOccurred(ex);
        }
    }

    private void buttonAction2(final ControlButton thisControlButton, final JFrame frame) {
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
                        control.nextCommand(thisControlButton);
                    }
                });
        dialog.setVisible(true);
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
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] {root.toURI().toURL()});
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
