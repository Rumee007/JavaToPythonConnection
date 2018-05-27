/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javatopythonconnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import org.apache.log4j.Logger;

/**
 *
 * @author Rumee
 */
public class AutomaticLightSwitchingThread implements Runnable {

    private static Connection connection = null;
    private static int count = 0;
    static Logger logger = Logger.getLogger(AutomaticLightSwitchingThread.class);

    @Override
    public void run() {
        try {
            //connection = createDBConnection();
            logger.info("run()-AutomaticLightSwitchingThread Running...");
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
                getMovementData();
            }
        } catch (Exception ex) {
            logger.error("run()-AutomaticLightSwitchingThread Failed... " + ex);
            Thread.currentThread().interrupt();
        }
    }

    private static void getMovementData() throws SQLException {
        String path = "/home/pi/Desktop/Light.py";
        String command[] = {"python", path};
        ProcessBuilder builder = new ProcessBuilder(command);
        System.out.println("getMovementData()-System is running...");
        Process process = null;
        try {
            process = builder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            if ((line = bufferedReader.readLine()) != null) {
                logger.info("getMovementData()-Get movement data:" + line);

                if (line.equalsIgnoreCase("off")) {
                    count++;
                    // count 20 = 1 min
                    if (count >= 20) {
                        //insertData(line);
                        logger.info("getMovementData()-Data Insert successfully, Light:OFF, count:" + count);
                    } else {
                        logger.info("getMovementData()-Data Insert failed, Light:ON, count:" + count);
                    }
                } else {
                    count = 0;
                    logger.info("getMovementData()-Data Insert failed, Light:ON, count:" + count);
                }
            }
            bufferedReader.close();
            process.destroy();
        } catch (Exception ex) {
            connection.close();
            logger.error("getMovementData()-Exception found:" + ex + ", Connection:" + connection);
        }
    }

    private static boolean insertData(String value) throws SQLException {
        String sqlQuery = null;
        try {
            Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            if (connection.isClosed() || connection == null) {
                logger.warn("insertData()-Connection closed:" + connection);
                connection = createDBConnection();
            }
            Statement statement = connection.createStatement();
            sqlQuery = "INSERT INTO weather (temperature, currdatetime) VALUES "
                    + "('" + value + "', '" + currentTimestamp + "')";

            if (statement.executeUpdate(sqlQuery) > 0) {
                statement.close();
                return true;
            } else {
                logger.warn("insertData()-Data Insertion failed:" + sqlQuery);
            }
        } catch (SQLException ex) {
            connection.close();
            logger.error("insertData()-Data:" + value + ", inserted failed:" + ex + ", SQL:" + sqlQuery + ", Connection:" + connection);
        }
        logger.warn("insertData()-Data:" + value + ", inserted failed:false, SQL:" + sqlQuery);
        return false;
    }

    private static Connection createDBConnection() {
        boolean connectionStatus = false;
        while (!connectionStatus) {
            try {
                connection = (Connection) DriverManager.getConnection(
                        "jdbc:sqlserver://etenderdb.database.windows.net:1433;database=IOTPOC;user=etenderAdmin@etenderdb;password=Eraetender1@;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30");

                if ((!connection.isClosed() || connection != null)) {
                    logger.info("createDBConnection()-Connection up");
                    connectionStatus = true;
                    return connection;
                } else {
                    logger.info("createDBConnection()-Connection failed");
                    connectionStatus = false;
                }
            } catch (Exception ex) {
                if (ex instanceof NullPointerException) {
                    connectionStatus = false;
                    logger.error("createDBConnection()-NullPointerException");
                } else if (ex instanceof ClassNotFoundException) {
                    connectionStatus = false;
                    logger.error("createDBConnection()-ClassNotFoundException");
                } else {
                    connectionStatus = false;
                    logger.error("createDBConnection()-Failed to create connection to database " + ex);
                }
            }
        }
        return connection;
    }
}
