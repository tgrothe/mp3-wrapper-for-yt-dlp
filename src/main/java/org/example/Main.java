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
    private volatile boolean isRunning = false;
    private Method renameMethod;

    public Main() {
        fieldReg2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setLineWrap(true);

        JButton button1 = new JButton("Start!");
        JButton button2 = new JButton("Bulk processing");
        JButton button3 = new JButton("Settings");
        JPanel panel = new JPanel(new FlowLayout());
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);

        JFrame frame = new JFrame("Download, rename, copy/sync");
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(area));
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        isRunning = false;
                    }
                });
        frame.setVisible(true);

        button1.addActionListener(
                e -> {
                    if (isRunning) {
                        isRunning = false;
                        button1.setText("Start!");
                    } else {
                        isRunning = true;
                        button1.setText("Stop");
                        new Thread(this::startRun).start();
                    }
                });
        button2.addActionListener(e -> {
            isRunning = false;
            button1.setText("Start!");
            showBulk(frame);
        });
        button3.addActionListener(e -> {
            isRunning = false;
            button1.setText("Start!");
            showSettings(frame);
        });

        showSettings(frame);
    }

    private void append(String s) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    area.append(s + "\n");
                    area.setCaretPosition(area.getText().length());
                });
    }

    @SuppressWarnings("all")
    private void startRun() {
        try {
            while (isRunning) {
                Pattern pat1 = Pattern.compile(fieldReg.getText());
                String data =
                        (String)
                                Toolkit.getDefaultToolkit()
                                        .getSystemClipboard()
                                        .getData(DataFlavor.stringFlavor);
                if (data != null && data.matches(pat1.pattern())) {
                    StringSelection dummyStr = new StringSelection("dummy text");
                    Toolkit.getDefaultToolkit()
                            .getSystemClipboard()
                            .setContents(dummyStr, dummyStr);
                    Matcher mat1 = pat1.matcher(data);
                    if (mat1.find()) {
                        final String video_id = mat1.group(1);
                        startVideoId(video_id);
                    }
                }

                Thread.sleep(3000);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "An exception occurred!\n\n" + e,
                    "Exception e",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    private void startVideoId(final String video_id) throws Exception {
        append("Found video id " + video_id);
        download(video_id);
        compileRenamer();
        renameFiles();
        copyFiles();
        append("Finish video id " + video_id);
    }

    private void download(String video_id) throws Exception {
        append("Start download " + video_id);
        java.lang.Runtime rt = java.lang.Runtime.getRuntime();
        @SuppressWarnings("all")
        Process pr = rt.exec(String.format(fieldCmd.getText(), fieldSrc.getText(), video_id));
        pr.waitFor();
        append("Finish download " + video_id);
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
        append("Start renameFiles()");
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
        append("Finish renameFiles()");
    }

    private void copyFiles() throws Exception {
        if (!new File(fieldDst.getText()).exists() || !boxCopy.isSelected()) {
            return;
        }
        append("Start copyFiles()");
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
        append("Finish copyFiles()");
    }

    private void showBulk(JFrame frame) {
        final JTextArea urls = new JTextArea();
        urls.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        final JDialog dialog = new JDialog(frame, "Bulk processing", true);
        dialog.getContentPane()
                .add(
                        PanelMatic.begin()
                                .addHeader(PanelBuilder.HeaderLevel.H3, "Add one URL per line here...")
                                .add("URLs:", urls)
                                .get());
        dialog.pack();
        dialog.setSize(600, 600);
        dialog.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        new Thread(() -> {
                            try {
                                Pattern pat1 = Pattern.compile(fieldReg.getText());
                                String[] lines = urls.getText().split("\n");
                                for (String l : lines) {
                                    if (l != null && !l.isBlank() && l.matches(pat1.pattern())) {
                                        Matcher mat1 = pat1.matcher(l);
                                        if (mat1.find()) {
                                            final String video_id = mat1.group(1);
                                            startVideoId(video_id);
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "An exception occurred!\n\n" + ex,
                                        "Exception e",
                                        JOptionPane.WARNING_MESSAGE);
                                System.exit(0);
                            }
                        }).start();
                    }
                }
        );
        dialog.setVisible(true);
    }

    private void showSettings(JFrame frame) {
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
                    public void windowClosing(WindowEvent e) {
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
                    }
                });
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        new Main();
    }
}
