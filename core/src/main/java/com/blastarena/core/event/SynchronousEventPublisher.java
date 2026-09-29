package com.blastarena.core.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Delivers each event to every listener straight away, in the order they subscribed. */
public final class SynchronousEventPublisher implements EventPublisher {

    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(GameEvent event) {
        Objects.requireNonNull(event, "event");
        for (GameEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public void subscribe(GameEventListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void unsubscribe(GameEventListener listener) {
        listeners.remove(listener);
    }
}
