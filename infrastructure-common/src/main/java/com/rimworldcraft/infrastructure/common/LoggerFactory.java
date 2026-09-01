package com.rimworldcraft.infrastructure.common;

import org.slf4j.Logger;

/** Infrastructure logging helper; see codestyle-and-solidd.md. */
public final class LoggerFactory {
    private LoggerFactory() { }
    public static Logger get(Class<?> type) { return org.slf4j.LoggerFactory.getLogger(type); }
}
