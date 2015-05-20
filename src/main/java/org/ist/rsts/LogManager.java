package org.ist.rsts;

import org.ist.rsts.tuple.TupleMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {

    private final static Logger logger = Logger.getLogger(LogManager.class.getName());
    private String logDirPath = "logs";
    int corePoolSize = 5;
    int maxPoolSize = 10;
    long keepAliveTime = 5;
    String logId;

    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES, queue);


    public LogManager(String logId) {
        this.logId = logId;
        File logDirectory = new File(logDirPath);
        if (!logDirectory.exists()) {
            logDirectory.mkdir();
        }

    }

    public void writeLog(TupleMessage tupleMessage, String viewId) {
        executor.execute(new LogWriteTask(viewId, logId, tupleMessage, logDirPath));
    }


    public File getLogFile() {
        throw new UnsupportedOperationException();
    }

    public String getLogDirPath() {
        return logDirPath;
    }


    private class LogWriteTask implements Runnable {
        private String viewId;
        private String logId;
        File logFile;
        String logDirPath;
        TupleMessage tupleMessage;

        public LogWriteTask(String viewId, String logId, TupleMessage msg, String logDirPath) {
            this.logDirPath = logDirPath;
            this.tupleMessage = msg;
            this.viewId = viewId;
            this.logId = logId;

        }

        @Override
        public void run() {

            //Log format log-<serverid>-<viewid> eg: log-2-10
            logFile = new File(logDirPath.concat(File.separator).
                    concat("log-").concat(logId).concat("-").concat(viewId));
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                FileWriter writer = new FileWriter(logFile, true);
                writeLog(tupleMessage, writer);

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not create server logs logFile at " + logFile.getAbsolutePath());
                e.printStackTrace();
            }
        }

        private void writeLog(TupleMessage msg, FileWriter writer) {
            if (writer == null) {
                logger.log(Level.SEVERE, "Log writer not found. Cannot log the operation");
                return;
            }
            //log will be in the format of
            //seqNo : WRITE/TAKE ; value1,value2,value3
            try {
                String[] tupleValues = msg.getTuple().getValues();
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                bufferedWriter.write(msg.getType().name().concat(";").
                        concat(tupleValues[0]).concat(",").concat(tupleValues[1]).concat(",").concat(tupleValues[2]).concat("\n"));
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not write log");
            }
        }
    }
}
