package org.ist.rsts;

import org.ist.rsts.tuple.Tuple;
import org.ist.rsts.tuple.TupleManager;
import org.ist.rsts.tuple.Type;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RSTSManager {

    LogManager logManager;
    TupleManager tupleManager;
    private final static Logger logger = Logger.getLogger(RSTSManager.class.getName());


    public RSTSManager(LogManager logManager, TupleManager tupleManager) {
        this.logManager = logManager;
        this.tupleManager = tupleManager;
    }

    public void transferState(byte[] newState) {
        FileOutputStream fos;
        try {
            File myLog = logManager.getLogFile();
            //TODO...... identify the newer part that should be done
            String logsDir = logManager.getLogDirPath();
            File newStateFile = new File(logsDir.concat("new_state.txt"));
            if(newStateFile.exists()) {
                newStateFile.delete();
                newStateFile.createNewFile();
            }
            fos = new FileOutputStream(newStateFile);
            fos.write(newState);
            fos.flush();
            fos.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //write to tuple space


        //make his log,my log :)
    }

    private void updateTupleSpace(File myLog, File newState) {
        //get last operation done from my log

        String sCurrentLine;
        String myLastLine = "";
        boolean startUpdate = false;
        try {
            BufferedReader br = new BufferedReader(new FileReader(myLog));
            while ((sCurrentLine = br.readLine()) != null) {
                myLastLine = sCurrentLine;
            }

            br = new BufferedReader(new FileReader(newState));
            while ((sCurrentLine = br.readLine()) != null) {
                //update should happen after the equal line
                if(startUpdate) {
                    //update tuple space
                    updateTuples(sCurrentLine);
                }
                if(sCurrentLine.equals(myLastLine)) {
                    startUpdate = true;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateTuples(String logLine) {
        //log line format
        //seqNo:WRITE/TAKE;value1,value2,value3
        System.out.println("log line >>>>>>> " + logLine);
        try {
            String operation = logLine.split(":")[1].split(";")[0];
            String[] values = logLine.split(";")[1].split(",");
            Tuple tuple = new Tuple(values[0], values[1], values[2]);

            if(Type.WRITE.name().equals(operation)) {
                System.out.println("...........WRITE update........");
                tupleManager.writeTuple(tuple);
            } else if(Type.TAKE.name().equals(operation)) {
                System.out.println("...........TAKE update...........");
                tupleManager.takeTuple(tuple);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "could not update tuple for log line: " + logLine);
        }
    }
}
