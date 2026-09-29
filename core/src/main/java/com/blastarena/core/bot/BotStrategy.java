package com.blastarena.core.bot;

/** How a bot decides what to do next. Each difficulty is one strategy. */
@FunctionalInterface
public interface BotStrategy {

    Plan plan(BotContext context);
}
