package com.blastarena.core.event;

/** Where the engine sends its events. The engine only knows this interface, never who is listening. */
public interface EventPublisher {

    void publish(GameEvent event);

    void subscribe(GameEventListener listener);

    void unsubscribe(GameEventListener listener);
}
