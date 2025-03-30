package today.vanta.util.client.network.account;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import today.vanta.Vanta;
import today.vanta.util.client.network.NetworkUtil;
import today.vanta.util.system.EncryptUtil;
import today.vanta.util.system.VantaFile;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class AccountSavingUtil {

    public static File ACCOUNT_FILE = VantaFile.getFile("accounts.json");
    public static List<Account> ACCOUNTS = new ArrayList<>();
    public static Account CURRENT_ACCOUNT = null;

    public static boolean loadConfig() {
        if (!ACCOUNT_FILE.exists()) return false;

        try (Reader reader = new FileReader(ACCOUNT_FILE)) {
            Type type = new TypeToken<Map<String, JsonObject>>() {
            }.getType();
            Map<String, JsonObject> accountMap = Vanta.instance.configStorage.GSON.fromJson(reader, type);
            Vanta.instance.logger.info("Loading alt accounts {}", ACCOUNT_FILE.getName());

            for (Map.Entry<String, JsonObject> entry : accountMap.entrySet()) {
                String name = entry.getKey();
                JsonObject jsonObject = entry.getValue();
                Account account = new Account(name, null);

                account.password = jsonObject.has("UUID") ? jsonObject.get("UUID").getAsString() : jsonObject.has("Token") ? EncryptUtil.decrypt(jsonObject.get("Password").getAsString()) : "";
                account.token = jsonObject.has("Token") ? EncryptUtil.decrypt(jsonObject.get("Token").getAsString()) : "";
                if (jsonObject.has("Skin"))
                    account.skin = jsonObject.get("Skin").getAsString();
                ACCOUNTS.add(account);
            }
            return true;
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when loading {} {}", ACCOUNT_FILE.getName(), e.getMessage());
            return false;
        }
    }

    public static void saveConfig() {
        try (Writer writer = new FileWriter(ACCOUNT_FILE)) {
            Map<String, JsonObject> categoryMap = new HashMap<>();

            for (Account acc : ACCOUNTS) {
                boolean pass = acc.isEmail();
                JsonObject accountObject = new JsonObject();
                accountObject.addProperty(pass ? "Password" : "UUID", pass ? EncryptUtil.encrypt(acc.password) : acc.password);
                accountObject.addProperty("Token", EncryptUtil.encrypt(acc.token));
                if (pass || !acc.token.isEmpty()) {
                    accountObject.addProperty("Skin", NetworkUtil.getBase64EncodedImage(NetworkUtil.getHead(acc.password, 512)));
                } else if (acc.isCracked()) {
                    accountObject.addProperty("Skin", getSteveHead());
                }
                categoryMap.put(acc.username, accountObject);
            }

            Vanta.instance.logger.info("Saving alt accounts {}", ACCOUNT_FILE.getName());
            Vanta.instance.configStorage.GSON.toJson(categoryMap, writer);
            writer.flush();
        } catch (IOException e) {
            Vanta.instance.logger.warn("Error occurred when saving {} {}", ACCOUNT_FILE.getName(), e.getMessage());
        }
    }

    public static String getSteveHead() {
        return "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAIAAAD2HxkiAAAE1UlEQVR4nOzVu4rdZRuH4XcyazL58uGmylgIsbdQ+5BS3JTaWwS08BBEW0WxsRHEytIgFlqkSpFaC1uFuCGoKUIgRMMks2ZkWnufu7muE/it/wv3ejbPPfPYGnR0PLl2arNzZnLucHs0OXf6pEfbybkzZ0bfc621t9mdnDs5mVw7Nf2gwL+IEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCG2OTquf8J/7Ohk9Av/f+7s5Nxa64mzO5Nz27U7ObfWunP/cHJue7KdnHMJoSdCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIIbYZ3vvo7TeGF/f3zk7O/e/c45Nza60Hf90b3dud/uO+++fvk3MffH1tcs4lhJ4IISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGI7X7775uTe+f39ybm11t+Hh5Nzu7vTH3j33t3JuYsHFybn1lq//vLj8OIwlxBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBim/P7+5N7r77zyeTcWuu1yx9Ozl25fHNybq118eDC5Nyn3x5Mzq21vrrx8eTc1ffempxzCaEnQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoTYzudXXpzcO3jq6cm5tdZm/8nJuW+uX5+cm/f6S5eGF2/99tPk3M3bdybnXELoiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIbe4/2k7uPbx1c3JurXX1xneTcy8/+8Lk3Lz3P/tiePGVS89Pzu2svck5lxB6IoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIbX7+4/bk3vHx8eTcvGs/fD+8uLu3GV4cdv/BdnLu8NHDyTmXEHoihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGIihJgIISZCiIkQYiKEmAghJkKIiRBiIoSYCCEmQoiJEGL/BAAA//+J4VhDvV4pYgAAAABJRU5ErkJggg==";
    }
}
