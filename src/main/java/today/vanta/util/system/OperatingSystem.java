package today.vanta.util.system;

import org.apache.commons.io.IOUtils;
import today.vanta.Vanta;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;

public enum OperatingSystem {
    LINUX,
    SOLARIS,
    WINDOWS {
        protected String[] getURLOpenCommand(URL url) {
            return new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
        }
    },
    OSX {
        protected String[] getURLOpenCommand(URL url) {
            return new String[]{"open", url.toString()};
        }
    },
    UNKNOWN;

    public void open(URL url) {
        try {
            Process process = AccessController.doPrivileged((PrivilegedExceptionAction<Process>) () -> Runtime.getRuntime().exec(this.getURLOpenCommand(url)));

            for (String string : IOUtils.readLines(process.getErrorStream())) {
                Vanta.instance.logger.error(string);
            }

            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
        } catch (IOException | PrivilegedActionException exception) {
            Vanta.instance.logger.error("Couldn't open url '{}'", url, exception);
        }
    }

    public void open(URI uRI) {
        try {
            this.open(uRI.toURL());
        } catch (MalformedURLException malformedURLException) {
            Vanta.instance.logger.error("Couldn't open uri '{}'", uRI, malformedURLException);
        }
    }

    public void open(File file) {
        try {
            this.open(file.toURI().toURL());
        } catch (MalformedURLException malformedURLException) {
            Vanta.instance.logger.error("Couldn't open file '{}'", file, malformedURLException);
        }
    }

    protected String[] getURLOpenCommand(URL url) {
        String string = url.toString();
        if ("file".equals(url.getProtocol())) {
            string = string.replace("file:", "file://");
        }

        return new String[]{"xdg-open", string};
    }

    public void open(String string) {
        try {
            this.open((new URI(string)).toURL());
        } catch (MalformedURLException | IllegalArgumentException | URISyntaxException exception) {
            Vanta.instance.logger.error("Couldn't open uri '{}'", string, exception);
        }
    }

    public static OperatingSystem getOperatingSystem() {
        String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (string.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (string.contains("mac")) {
            return OperatingSystem.OSX;
        } else if (string.contains("solaris")) {
            return OperatingSystem.SOLARIS;
        } else if (string.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else if (string.contains("linux")) {
            return OperatingSystem.LINUX;
        } else {
            return string.contains("unix") ? OperatingSystem.LINUX : OperatingSystem.UNKNOWN;
        }
    }
}