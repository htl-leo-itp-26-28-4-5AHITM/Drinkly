package at.htl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@ApplicationScoped
public class EntryEvents {

    private final BroadcastProcessor<String> processor = BroadcastProcessor.create();

    public Multi<String> stream() {
        // "connected" is sent right away so the browser knows the stream is open
        return Multi.createBy().merging()
                .streams(Multi.createFrom().item("connected"), processor);
    }

    // runs only after the database transaction has committed,
    // so pages never reload before the new data is visible
    void onChange(@Observes(during = TransactionPhase.AFTER_SUCCESS) EntriesChanged event) {
        processor.onNext("changed");
    }
}