package com.datastax.probe;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import com.google.common.base.Preconditions;

public class ProbeLogger {

    private LoggerContext loggerContext;
    private RollingFileAppender<ILoggingEvent> rollingFileAppender;

    boolean customFilePath = false;

    public ProbeLogger() {
	this(null, -1, -1);
    }

    public ProbeLogger(String filePath, int maxHistory, int maxFileSizeMb) {
	loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
	if (StringUtils.isNotEmpty(filePath)) {
	    Preconditions.checkArgument(maxHistory >= 1, "Max File History must be >= 1");
	    Preconditions.checkArgument(maxFileSizeMb >= 1, "Max File Size in MB must be >= 1");

	    
	    ch.qos.logback.classic.Logger templateLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ProbeLogger.class);
	    loggerContext = templateLogger.getLoggerContext();

	    customFilePath = true;

	    String logDir = filePath;
	    System.out.println("logDir = "+logDir);
	    String logFile = loggerContext.getProperty("MYAPP_LOG_FILE");
	    System.out.println("logFile = "+logFile);
	    
	    String pattern = loggerContext.getProperty("DEFAULT_PATTERN");
	    System.out.println("pattern = "+pattern);
	    
	    String rollingTemplate = loggerContext.getProperty("MYAPP_ROLLING_TEMPLATE");
	    System.out.println("rollingTemplate = "+rollingTemplate);


	    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	    encoder.setPattern(pattern);
	    encoder.setContext(loggerContext);

	    
	    //DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> timeBasedTriggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent>();

	    SizeAndTimeBasedFNATP<ILoggingEvent> timeBasedTriggeringPolicy = new SizeAndTimeBasedFNATP<ILoggingEvent>();
	    timeBasedTriggeringPolicy.setContext(loggerContext);
	    timeBasedTriggeringPolicy.setMaxFileSize(maxFileSizeMb+"MB");

	    TimeBasedRollingPolicy<ILoggingEvent> timeBasedRollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
	    timeBasedRollingPolicy.setContext(loggerContext);
	    timeBasedRollingPolicy.setFileNamePattern(logDir + "/" + logFile + rollingTemplate);
	    timeBasedRollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(timeBasedTriggeringPolicy);
	    timeBasedTriggeringPolicy.setTimeBasedRollingPolicy(timeBasedRollingPolicy);

	    rollingFileAppender = new RollingFileAppender<ILoggingEvent>();
	    rollingFileAppender.setAppend(true);
	    rollingFileAppender.setContext(loggerContext);
	    rollingFileAppender.setEncoder(encoder);
	    rollingFileAppender.setFile(logDir + "/" + logFile);
	    rollingFileAppender.setName("CassandraProbeAppender");
	    rollingFileAppender.setPrudent(false);
	    rollingFileAppender.setRollingPolicy(timeBasedRollingPolicy);
	    rollingFileAppender.setTriggeringPolicy(timeBasedTriggeringPolicy);

	    timeBasedRollingPolicy.setParent(rollingFileAppender);

	    encoder.start();
	    timeBasedRollingPolicy.start();

	    rollingFileAppender.stop();
	    rollingFileAppender.start();

	    //	    fileAppender = new RollingFileAppender();
	    //	    fileAppender.setContext(loggerContext);
	    //	    fileAppender.setName("CassandraProbeFileLogger");
	    //	    //fileAppender.setPrudent(true);
	    //	    fileAppender.setFile(filePath + "/cassandra-probe.log");
	    //
	    //	    //	    TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
	    //	    //	    policy.setMaxHistory((maxHistory < 1) ? 1 : maxHistory);
	    //	    //	    policy.setFileNamePattern("%d{yyyy-MM-dd}.cassandra-probe.log");
	    //	    //	    policy.setParent(fileAppender);
	    //	    //	    
	    //	    //	    fileAppender.setRollingPolicy(policy);
	    //
	    //	    // set the file name
	    //
	    //	    encoder = new PatternLayoutEncoder();
	    //	    encoder.setContext(loggerContext);
	    //	    encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
	    //	    encoder.start();
	    //
	    //	    fileAppender.setEncoder(encoder);
	    //	    fileAppender.start();
	}
    }

    //    public static Logger createLogger(String name) {
    //	ch.qos.logback.classic.Logger templateLogger = (ch.qos.logback.classic.Logger) LogUtil.getLogger("com.myapp");
    //	LoggerContext context = templateLogger.getLoggerContext();
    //
    //	String logDir = context.getProperty("HOME_PATH");
    //
    //	PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    //	encoder.setPattern(context.getProperty("DEFAULT_PATTERN"));
    //	encoder.setContext(context);
    //
    //	DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> timeBasedTriggeringPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent>();
    //	timeBasedTriggeringPolicy.setContext(context);
    //
    //	TimeBasedRollingPolicy<ILoggingEvent> timeBasedRollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
    //	timeBasedRollingPolicy.setContext(context);
    //	timeBasedRollingPolicy.setFileNamePattern(logDir + name + ".log." + context.getProperty("MYAPP_ROLLING_TEMPLATE"));
    //	timeBasedRollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(timeBasedTriggeringPolicy);
    //	timeBasedTriggeringPolicy.setTimeBasedRollingPolicy(timeBasedRollingPolicy);
    //
    //	RollingFileAppender<ILoggingEvent> rollingFileAppender = new RollingFileAppender<ILoggingEvent>();
    //	rollingFileAppender.setAppend(true);
    //	rollingFileAppender.setContext(context);
    //	rollingFileAppender.setEncoder(encoder);
    //	rollingFileAppender.setFile(logDir + name + ".log");
    //	rollingFileAppender.setName(name + "Appender");
    //	rollingFileAppender.setPrudent(false);
    //	rollingFileAppender.setRollingPolicy(timeBasedRollingPolicy);
    //	rollingFileAppender.setTriggeringPolicy(timeBasedTriggeringPolicy);
    //
    //	timeBasedRollingPolicy.setParent(rollingFileAppender);
    //
    //	encoder.start();
    //	timeBasedRollingPolicy.start();
    //
    //	rollingFileAppender.stop();
    //	rollingFileAppender.start();
    //
    //	ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) LogUtil.getLogger(name);
    //	logbackLogger.setLevel(templateLogger.getLevel());
    //	logbackLogger.setAdditive(false);
    //	logbackLogger.addAppender(rollingFileAppender);
    //
    //	return logbackLogger;
    //    }

    public Logger getLogger(String name) {
	Logger logbackLogger = loggerContext.getLogger(name);
	if (customFilePath) {
	    logbackLogger.setAdditive(false);
	    logbackLogger.addAppender(rollingFileAppender);
	}
	return logbackLogger;
    }

}