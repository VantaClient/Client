package today.vanta.util.system;

import java.io.File;

public class VantaFile {

    public static File getFile(String filePath) {
        return new File(FileUtil.getRunningPath() + "/" + filePath);
    }

    public static String getString(String filePath) {
        return FileUtil.getRunningPath() + "/" + filePath;
    }

}
