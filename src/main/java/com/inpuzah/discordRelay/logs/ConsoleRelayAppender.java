package com.inpuzah.discordRelay.logs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.function.Consumer;

public class ConsoleRelayAppender extends AbstractAppender {
    private volatile Consumer<String> consumer;
    private LoggerContext ctx;
    private Configuration config;

    public ConsoleRelayAppender(String name, Consumer<String> consumer) {
        // ignoreExceptions=true prevents AppenderLoggingException from bubbling
        super(name, null,
                PatternLayout.newBuilder().withPattern("%m").build(),
                /* ignoreExceptions = */ true,
                Property.EMPTY_ARRAY);
        this.consumer = consumer;
    }

    /** Attach to root logger. Call during onEnable(). */
    public void attach() {
        ctx = (LoggerContext) LogManager.getContext(false);
        config = ctx.getConfiguration();

        if (!isStarted()) start();

        config.addAppender(this);
        LoggerConfig root = config.getRootLogger();
        if (!root.getAppenders().containsKey(getName())) {
            root.addAppender(this, null, null);
        }
        ctx.updateLoggers();
    }

    /** Detach from root logger. Call first thing in onDisable(). */
    public void detach() {
        if (ctx == null || config == null) return;
        LoggerConfig root = config.getRootLogger();
        if (root.getAppenders().containsKey(getName())) {
            root.removeAppender(getName());
        }
        ctx.updateLoggers();
        config.getAppenders().remove(getName());
    }

    /** After detaching, make the consumer a no-op to avoid any late calls. */
    public void mute() {
        this.consumer = s -> {};
    }

    @Override
    public void append(LogEvent event) {
        // Be extra defensive; never throw from here
        try {
            Consumer<String> c = this.consumer;
            if (c != null) {
                String msg = getLayout().toSerializable(event).toString();
                c.accept(msg);
            }
        } catch (Throwable ignored) {
            // swallow to avoid shutdown explosions
        }
    }
}
