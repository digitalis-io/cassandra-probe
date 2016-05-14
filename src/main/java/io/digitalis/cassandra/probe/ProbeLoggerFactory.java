package io.digitalis.cassandra.probe;

import ch.qos.logback.classic.Logger;

public class ProbeLoggerFactory {

	private static ProbeLogger DYNAMIC_FILE_LOGGER;

	public static Logger getLogger(String name) {
		if (DYNAMIC_FILE_LOGGER == null) {
			synchronized (ProbeLoggerFactory.class) {
				if (DYNAMIC_FILE_LOGGER == null) {
					DYNAMIC_FILE_LOGGER = new ProbeLogger();
				}
			}
		}
		return DYNAMIC_FILE_LOGGER.getLogger(name);
	}

	public static void init(String filePath, int maxHistory, int maxFileSizeMb) {
		DYNAMIC_FILE_LOGGER = new ProbeLogger(filePath, maxHistory, maxFileSizeMb);
	}

	public static void main(String[] args) {
		ProbeLoggerFactory.init("log/" + System.currentTimeMillis() + ".log", 10, 1);
		Logger logger2 = ProbeLoggerFactory.getLogger(ProbeLoggerFactory.class);
		logger2.info("test info");
		logger2.debug("test debug");
		logger2.warn("test warn");
		logger2.error("test error");
	}

	@SuppressWarnings("rawtypes")
	public static Logger getLogger(Class c) {
		return getLogger(c.getName());
	}
}