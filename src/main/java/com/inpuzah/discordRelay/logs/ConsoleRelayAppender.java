package com.inpuzah.discordRelay.logs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ConsoleRelayAppender extends AbstractAppender {
    private final java.util.function.Consumer<String> consumer;

    // Keep references so we can detach later
    private LoggerContext ctx;
    private Configuration config;

    public ConsoleRelayAppender(String name, java.util.function.Consumer<String> consumer) {
        super(name, null, PatternLayout.newBuilder().withPattern("%m").build(), false, Property.EMPTY_ARRAY);
        this.consumer = consumer;
    }

    /** Attach this appender to the root logger. Call in onEnable(). */
    public void attach() {
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();

        // start this appender exactly once
        if (!isStarted()) {
            start();
        }

        config.addAppender(this);
        LoggerConfig root = config.getRootLogger();
        if (!root.getAppenders().containsKey(getName())) {
            root.addAppender(this, null, null);
        }
        ctx.updateLoggers(); // make it live
    }

    /** Detach from the root logger. Call before stop() in onDisable(). */
    public void detach() {
        if (ctx == null || config == null) return;
        LoggerConfig root = config.getRootLogger();
        if (root.getAppenders().containsKey(getName())) {
            root.removeAppender(getName());
        }
        ctx.updateLoggers();               // propagate removal
        config.getAppenders().remove(getName()); // drop from config map
    }

    @Override
    public void append(LogEvent event) {
        String msg = getLayout().toSerializable(event).toString();
        consumer.accept(msg);
    }
}
