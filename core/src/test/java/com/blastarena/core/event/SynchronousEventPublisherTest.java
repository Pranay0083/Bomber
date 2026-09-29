package com.blastarena.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.blastarena.core.model.Position;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynchronousEventPublisherTest {

    private final SynchronousEventPublisher publisher = new SynchronousEventPublisher();

    @Test
    void deliversEventsToEveryListenerInSubscriptionOrder() {
        List<String> received = new ArrayList<>();
        publisher.subscribe(event -> received.add("first " + event));
        publisher.subscribe(event -> received.add("second " + event));
        GameEvent event = new CrateDestroyed(new Position(1, 1));

        publisher.publish(event);

        assertThat(received).containsExactly("first " + event, "second " + event);
    }

    @Test
    void unsubscribedListenersStopReceivingEvents() {
        List<GameEvent> received = new ArrayList<>();
        GameEventListener listener = received::add;
        publisher.subscribe(listener);
        publisher.unsubscribe(listener);

        publisher.publish(RoundEnded.draw());

        assertThat(received).isEmpty();
    }

    @Test
    void aListenerMayUnsubscribeWhileHandlingAnEvent() {
        List<GameEvent> received = new ArrayList<>();
        GameEventListener once = new GameEventListener() {
            @Override
            public void onEvent(GameEvent event) {
                received.add(event);
                publisher.unsubscribe(this);
            }
        };
        publisher.subscribe(once);

        publisher.publish(RoundEnded.draw());
        publisher.publish(RoundEnded.draw());

        assertThat(received).hasSize(1);
    }
}
