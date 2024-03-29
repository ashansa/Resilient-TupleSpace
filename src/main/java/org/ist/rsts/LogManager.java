package org.ist.rsts;

import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleMessage;
import org.ist.rsts.tuple.Type;

import java.io.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {

    private final static Logger logger = Logger.getLogger(LogManager.class.getName());
    String logDirPath;
    int corePoolSize = 5;
    int maxPoolSize = 20;
    long keepAliveTime = 5;
    String logId;

    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MINUTES, queue);
    public LogManager(String logId) {
        this.logId = logId;
        this.logDirPath = "log" + File.separator + logId;
        File logDirectory = new File(logDirPath);
        if (logDirectory.exists()) {
            File[] files = logDirectory.listFiles();
            for (File file : files) {
                file.delete();
            }
        } else {
            boolean dirCreated = logDirectory.mkdirs();
        }
    }

    public void writeLog(TupleMessage tupleMessage, int viewId) {
        executorService.execute(new LogWriteTask(viewId, logId, tupleMessage, logDirPath));
    }

    public void writeLog(Tuple tuple, String operation, int viewId) {
        Type type = null;
        if (Type.WRITE.name().equals(operation))
            type = Type.WRITE;
        else if (Type.TAKE.name().equals(operation))
            type = Type.TAKE;

        executor.execute(new LogWriteTask(viewId, logId, tuple, type, logDirPath));
    }

    public void clearLogFile(int lastPresentViewId) {
        File logFile = new File(logDirPath.concat(File.separator).concat("log").concat("-").
                concat(String.valueOf(lastPresentViewId)));
        if(logFile.exists())
            logFile.delete();
    }


    private class LogWriteTask implements Runnable {
        private int viewId;
        private String logId;
        File logFile;
        String logDirPath;
        Tuple tuple;
        Type operationType;

        public LogWriteTask(int viewId, String logId, TupleMessage msg, String logDirPath) {
            this.logDirPath = logDirPath;
            this.tuple = msg.getTuple();
            this.operationType = msg.getType();
            this.viewId = viewId;
        }

        public LogWriteTask(int viewId, String logId, Tuple tuple, Type type, String logDirPath) {
            this.logDirPath = logDirPath;
            this.tuple = tuple;
            this.operationType = type;
            this.viewId = viewId;
        }

        @Override
        public void run() {

            //Log format log-<serverid>-<viewid> eg: log-2-10
            logFile = new File(logDirPath.concat(File.separator).
                    concat("log").concat("-").concat(String.valueOf(viewId)));
            try {
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                FileWriter writer = new FileWriter(logFile, true);
                System.out.println("Writing to the log");
                writeLog(tuple, operationType, writer);

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not create server logs logFile at " + logFile.getAbsolutePath());
                e.printStackTrace();
            }
        }

        private void writeLog(Tuple tuple, Type operationType, FileWriter writer) {
            if (writer == null) {
                logger.log(Level.SEVERE, "Log writer not found. Cannot log the operation");
                return;
            }
            //log will be in the format of
            //seqNo : WRITE/TAKE ; value1,value2,value3
            try {
                String[] tupleValues = tuple.getValues();
                BufferedWriter bufferedWriter = new BufferedWriter(writer);
                bufferedWriter.write(operationType.name().concat(":").
                        concat(tupleValues[0]).concat(",").concat(tupleValues[1]).concat(",").concat(tupleValues[2]).concat("\n"));
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not write log");
            }
        }
    }

    public String getLogForView(int viewId) throws IOException {

        String log = "";
        File logForView = new File(logDirPath.concat(File.separator).concat("log-").concat(String.valueOf(viewId)));
        if(logForView.exists()) {
            FileReader reader = new FileReader(logForView);

            BufferedReader br = new BufferedReader(reader);

            String line;

            while ((line = br.readLine()) != null) {
                log = log + line + "\n";
            }

            br.close();
        }

        System.out.println("log for view ====>" + viewId + "    :" + log);
        return log;
    }
}
