package com.inpuzah.discordRelay.logs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ConsoleRelayAppender extends AbstractAppender {
    private final java.util.function.Consumer<String> consumer;

    public ConsoleRelayAppender(String name, java.util.function.Consumer<String> consumer) {
        super(name, null, PatternLayout.newBuilder().withPattern("%m").build(), false, Property.EMPTY_ARRAY);
        this.consumer = consumer;

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        this.start();
        cfg.addAppender(this);
        cfg.getRootLogger().addAppender(cfg.getAppender(name), null, null);
        ctx.updateLoggers();
    }

    @Override
    public void append(LogEvent event) {
        String msg = getLayout().toSerializable(event).toString();
        consumer.accept(msg);
    }
}
