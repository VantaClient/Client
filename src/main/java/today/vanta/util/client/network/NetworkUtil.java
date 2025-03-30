package today.vanta.util.client.network;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;

public class NetworkUtil {

    public static String getHead(String uuid, int size) {
        return "https://minotar.net/helm/" + uuid +  "/" + size  + ".png";
    }

    public static String getBase64EncodedImage(String imageURL) throws IOException {
        java.net.URL url = new java.net.URL(imageURL);
        InputStream is = url.openStream();
        byte[] bytes = org.apache.commons.io.IOUtils.toByteArray(is);
        return Base64.encodeBase64String(bytes);
    }

}
