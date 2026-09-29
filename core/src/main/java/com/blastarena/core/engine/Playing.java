package com.blastarena.core.engine;

/** Normal play. After {@code roundLengthTicks} without a result, sudden death starts. */
public record Playing(int elapsedTicks, int roundLengthTicks) implements GamePhase {

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public GamePhase next(PhaseContext context) {
        return context.winConditionChecker().check(context.world())
                .<GamePhase>map(RoundOver::new)
                .orElseGet(() -> elapsedTicks + 1 >= roundLengthTicks
                        ? new SuddenDeath(0)
                        : new Playing(elapsedTicks + 1, roundLengthTicks));
    }
}
