package org.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Props {
    public final Properties properties = new Properties();
    private final File pf = new File("mp3-wrapper-for-yt-dlp.txt");

    public Props() {
        try {
            if (pf.exists()) {
                try (FileInputStream fis = new FileInputStream(pf)) {
                    properties.load(fis);
                }
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
            if (!properties.containsKey("fieldCmd2")) {
                properties.put(
                        "fieldCmd2",
                        "cmd /c start /wait powershell.exe -Command \"ffmpeg.exe -i '%1$s' -codec:a libmp3lame -b:a 128k '%2$s'\"");
            }
            if (!properties.containsKey("boxRename")) {
                properties.put("boxRename", "true");
            }
        } catch (Exception ex) {
            Main.exceptionOccurred(ex);
        }
    }

    public void storeProps(
            final String fieldSrc,
            final String fieldDst,
            final String fieldReg,
            final String fieldReg2,
            final String fieldCmd,
            final String fieldCmd2,
            final boolean boxRename) {
        try {
            properties.put("fieldSrc", fieldSrc);
            properties.put("fieldDst", fieldDst);
            properties.put("fieldReg", fieldReg);
            properties.put("fieldReg2", fieldReg2);
            properties.put("fieldCmd", fieldCmd);
            properties.put("fieldCmd2", fieldCmd2);
            properties.put("boxRename", String.valueOf(boxRename));
            try (FileOutputStream fos = new FileOutputStream(pf)) {
                properties.store(fos, "Properties for mp3-wrapper-for-yt-dlp");
            }
        } catch (Exception ex) {
            Main.exceptionOccurred(ex);
        }
    }
}
