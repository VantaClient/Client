package today.vanta.util.client.network.account;

public class Account {

    public String username, password;
    public String token = "";
    public String skin = AccountSavingUtil.getSteveHead();

    public Account(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Account(String username, String uuid, String token) {
        this(username, uuid);
        this.token = token;
    }

    public boolean isEmail() {
        return username.contains("@");
    }

    public boolean isCracked() {
        return password.isEmpty();
    }

}
