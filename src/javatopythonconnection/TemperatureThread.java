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
public class TemperatureThread implements Runnable {

    private static Connection connection = null;
    static Logger logger = Logger.getLogger(TemperatureThread.class);

    @Override
    public void run() {
        try {
            connection = createDBConnection();
            logger.info("run()-temperatureThread Running...");
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000);
                getTemperature();
            }

        } catch (Exception ex) {
            logger.error("run()-Failed... " + ex);
            Thread.currentThread().interrupt();
        }
    }

    private static void getTemperature() throws SQLException {
        String path = "/home/pi/Desktop/temperature.py";
        String command[] = {"python", path};
        ProcessBuilder builder = new ProcessBuilder(command);
        System.out.println("getTemperature()-System is running...");
        Process process = null;
        try {
            process = builder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            if ((line = bufferedReader.readLine()) != null) {
                logger.info("getTemperature()-Get temperature data:"+ line);
                if (line.equalsIgnoreCase("ERROR")) {
                    line = "0.00";
                }
                if (insertData(line)) {
                    logger.info("getTemperature()-Data Insert successfully:"+ line);
                } else {
                    logger.warn("getTemperature()-Data Insert failed:"+ line);
                }
            }
            bufferedReader.close();
            process.destroy();
        } catch (Exception ex) {
            connection.close();
            logger.error("getTemperature()-Exception found:"+ ex +", Connection:"+ connection);
        }
    }

    private static boolean insertData(String value) throws SQLException {
        String sqlQuery = null;
        try {
            Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            if (connection.isClosed() || connection == null) {
                logger.warn("insertData()-Connection closed:"+ connection);
                connection = createDBConnection();
            }
            Statement statement = connection.createStatement();
            sqlQuery = "INSERT INTO weather (temperature, currdatetime) VALUES "
                    + "('" + value + "', '" + currentTimestamp + "')";

            if (statement.executeUpdate(sqlQuery) > 0) {
                statement.close();
                return true;
            }else {
                logger.warn("insertData()-Data Insertion failed:"+ sqlQuery);
            }
        } catch (SQLException ex) {
            connection.close();
            logger.error("insertData()-Data:"+ value +", inserted failed:"+ ex +", SQL:"+ sqlQuery +", Connection:"+ connection);
        }
        logger.warn("insertData()-Data:"+ value +", inserted failed:false,SQL :" + sqlQuery);
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