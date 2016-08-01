package io.digitalis.cassandra.probe.job;

import io.digitalis.cassandra.probe.ProbeLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import ch.qos.logback.classic.Logger;

import com.datastax.driver.core.ConsistencyLevel;
import io.digitalis.cassandra.probe.Prober;

@DisallowConcurrentExecution
public class ProbeJob implements Job {

    private static final Logger LOG = ProbeLoggerFactory.getLogger(ProbeJob.class);

    private static Prober app = null;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("ProbeJob running...");
        StopWatch stopWatch = new StopWatch();

        JobKey key = context.getJobDetail()
                            .getKey();
        JobDataMap dataMap = context.getJobDetail()
                                    .getJobDataMap();
        String yamlPath = dataMap.getString("yamlPath");
        String[] contactPoints = (String[]) dataMap.get("contactPoints");
        String cqlshrcPath = dataMap.getString("cqlshrcPath");
        String username = dataMap.getString("username");
        String password = dataMap.getString("password");
        boolean nativeProbe = dataMap.getBoolean("nativeProbe");
        boolean thriftProbe = dataMap.getBoolean("thriftProbe");
        boolean storageProbe = dataMap.getBoolean("storageProbe");
        boolean pingProbe = dataMap.getBoolean("pingProbe");

        String testCql = dataMap.getString("testCql");
        ConsistencyLevel consistency = (ConsistencyLevel) dataMap.get("consistency");
        boolean tracingEnabled = dataMap.getBoolean("tracingEnabled");

        int storagePort = dataMap.getInt("storagePort");
        int thriftPort = dataMap.getInt("thriftPort");
        int nativePort = dataMap.getInt("nativePort");

        LOG.info("Instance " + key + " of ProbeJob yamlPath: " + yamlPath + ", and cqlshrcPath is: " + cqlshrcPath);

        try {
            stopWatch.start();
            if (app == null) {
                if (StringUtils.isNotBlank(cqlshrcPath)) {
                    app = new Prober(storagePort, nativePort, thriftPort, contactPoints, yamlPath, cqlshrcPath, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql,
                            consistency, tracingEnabled);
                } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                    app = new Prober(storagePort, nativePort, thriftPort, contactPoints, yamlPath, username, password, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql,
                            consistency, tracingEnabled);
                } else {
                    app = new Prober(storagePort, nativePort, thriftPort, contactPoints, yamlPath, nativeProbe, thriftProbe, storageProbe, pingProbe, testCql, consistency,
                            tracingEnabled);
                }
            } else {
                LOG.info("Reusing ProbeJob");
            }
            app.probe();
        } catch (Exception e) {
            String message = "Problem encountered with probing cluster: " + e.getMessage();
            System.err.println(message);
            e.printStackTrace(System.err);
            LOG.error(message, e);
            throw new RuntimeException(message, e);
        } finally {
            stopWatch.stop();
        }

    }

}
