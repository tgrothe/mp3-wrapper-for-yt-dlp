package org.example;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Props {
    public final Properties properties = new Properties();
    private final File pf = new File("MP3WatcherProps.txt");

    public Props() {
        try {
            if (pf.exists()) {
                properties.load(new FileInputStream(pf));
            }
            if (!properties.containsKey("fieldSrc")) {
                properties.put("fieldSrc", "C:\\Users\\xxx\\Music\\yt-dlp\\");
            }
            if (!properties.containsKey("fieldDst")) {
                properties.put("fieldDst", "D:\\yt-dlp\\");
            }
            if (!properties.containsKey("fieldReg")) {
                properties.put(
                        "fieldReg", "^https://www\\.youtube\\.com/watch\\?v=([^?&]{10,12}).*$");
            }
            if (!properties.containsKey("fieldReg2")) {
                properties.put(
                        "fieldReg2",
                        """
                        public class AdvancedRenamer {
                            public static String rename(String prefix) {
                                prefix = prefix.replaceAll("&", "n");
                                prefix = prefix.replaceAll("_", " ");
                                if (prefix.matches("^.+ \\\\[.{10,12}]$")) {
                                    prefix = prefix.replaceAll(" \\\\[.{10,12}]$", "");
                                } else if (prefix.matches("^.+ .{10,12} $")) {
                                    prefix = prefix.replaceAll(" .{10,12} $", "");
                                }
                                prefix = prefix.replaceAll("[^-()\\\\w]+", " ");
                                prefix = prefix.replaceAll(" {2,}", " ");
                                prefix = prefix.trim().toLowerCase(java.util.Locale.ROOT);
                                return prefix;
                            }
                        }""");
            }
            if (!properties.containsKey("fieldCmd")) {
                properties.put(
                        "fieldCmd",
                        "cmd /c start /wait powershell.exe -Command \"cd '%1$s' ; yt-dlp.exe -f bestaudio -x --audio-format mp3 --audio-quality 320K -- %2$s\"");
            }
            if (!properties.containsKey("boxRename")) {
                properties.put("boxRename", "true");
            }
            if (!properties.containsKey("boxCopy")) {
                properties.put("boxCopy", "true");
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

    public void storeProps(
            String fieldSrc,
            String fieldDst,
            String fieldReg,
            String fieldReg2,
            String fieldCmd,
            boolean boxRename,
            boolean boxCopy) {
        try {
            properties.put("fieldSrc", fieldSrc);
            properties.put("fieldDst", fieldDst);
            properties.put("fieldReg", fieldReg);
            properties.put("fieldReg2", fieldReg2);
            properties.put("fieldCmd", fieldCmd);
            properties.put("boxRename", String.valueOf(boxRename));
            properties.put("boxCopy", String.valueOf(boxCopy));
            try (FileOutputStream fos = new FileOutputStream(pf)) {
                properties.store(fos, "Properties for MP3Watcher");
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
}
