package com.blastarena.core.engine;

/** Play continues past the round length. Closing walls arrive in Phase 10. */
public record SuddenDeath(int elapsedTicks) implements GamePhase {

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public GamePhase next(PhaseContext context) {
        return context.winConditionChecker().check(context.world())
                .<GamePhase>map(RoundOver::new)
                .orElseGet(() -> new SuddenDeath(elapsedTicks + 1));
    }
}
