package today.vanta.client.module.impl.client;

import today.vanta.client.module.Category;
import today.vanta.client.module.Module;

public class Test extends Module {
    public Test() {
        super("Test", "Test module for developers.", Category.CLIENT);
        hideFromArraylist = true;
    }
}
