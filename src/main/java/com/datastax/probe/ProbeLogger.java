package com.datastax.probe;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;

public class ProbeLogger {

    private LoggerContext loggerContext;
    @SuppressWarnings("rawtypes")
    private FileAppender fileAppender;
    private PatternLayoutEncoder encoder;

    boolean customFilePath = false;

    public ProbeLogger() {
	this(null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ProbeLogger(String filePath) {
	loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	if (StringUtils.isNotEmpty(filePath)) {
	    customFilePath = true;
	    fileAppender = new FileAppender();
	    fileAppender.setContext(loggerContext);
	    fileAppender.setName("CassandraProbeFileLogger");
	    // set the file name
	    fileAppender.setFile(filePath+"/cassandra-probe.log");

	    encoder = new PatternLayoutEncoder();
	    encoder.setContext(loggerContext);
	    encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
	    encoder.start();

	    fileAppender.setEncoder(encoder);
	    fileAppender.start();
	}
    }

    @SuppressWarnings("unchecked")
    public Logger getLogger(String name) {
	Logger logbackLogger = loggerContext.getLogger(name);
	if (customFilePath) {
	    logbackLogger.addAppender(fileAppender);
	}
	return logbackLogger;
    }

}