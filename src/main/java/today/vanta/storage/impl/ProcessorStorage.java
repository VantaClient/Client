package today.vanta.storage.impl;

import today.vanta.client.processor.Processor;
import today.vanta.client.processor.impl.*;
import today.vanta.storage.Storage;

public class ProcessorStorage extends Storage<Processor> {
    @Override
    public void subscribe() {
        super.subscribe();

        list.add(new VersionProcessor());
        list.add(new RotationProcessor());
        list.add(new TargetProcessor());
        list.add(new ScreenProcessor());
        list.add(new ChatProcessor());
        list.forEach(Processor::onInitialize);
    }
}
