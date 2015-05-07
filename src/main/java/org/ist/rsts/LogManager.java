package org.ist.rsts;

import org.ist.rsts.tuple.TupleMessage;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogManager {

    private final static Logger logger = Logger.getLogger(LogManager.class.getName());
    private PrintWriter writer;
    File logFile;
    private String logDirPath = "./src/main/resources/logs/";

    public LogManager(String logId) {
        logFile = new File(logDirPath.concat("server_logs_").concat(logId).concat(".txt"));
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            writer = new PrintWriter(logFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create server logs logFile at " + logFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    public void writeLog(int seqNo, TupleMessage msg) {
        if(writer == null) {
            logger.log(Level.SEVERE, "Log writer not found. Cannot log the operation");
            return;
        }
        //log will be in the format of
        //seqNo : WRITE/TAKE ; value1,value2,value3
        try {
            String[] tupleValues = msg.getTuple().getValues();
            writer.write(String.valueOf(seqNo).concat(":").concat(msg.getType().name()).concat(";").
                    concat(tupleValues[0]).concat(",").concat(tupleValues[1]).concat(",").concat(tupleValues[2]).concat("\n"));
            writer.flush();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not write log for sequence no " + seqNo);
        }
    }

    public File getLogFile() {
        return this.logFile;
    }

    public String getLogDirPath() {
        return logDirPath;
    }
}
