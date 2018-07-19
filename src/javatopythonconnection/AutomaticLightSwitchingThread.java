/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javatopythonconnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
    private static boolean lightOnStatus = true;
    private static boolean lightOffStatus = true;
    private static boolean movementStatusON = true;
    private static boolean movementStatusOFF = true;

    private static boolean connectionStatus = false;
    static Logger logger = Logger.getLogger(AutomaticLightSwitchingThread.class);

    @Override
    public void run() {
        try {
            logger.info("run()-Starting AutomaticLightSwitchingThread...");
            connection = createDBConnection();
            //!Thread.currentThread().isInterrupted()
            while (true) {
                //Thread.sleep(500);
                getMovementData();
                Thread.sleep(250);
                
                //controlLightStatusFromApp();
            }
        } catch (Exception ex) {
            logger.error("run()-AutomaticLightSwitchingThread Failed... " + ex);
            Thread.currentThread().interrupt();
        }
    }

    private synchronized static void getMovementData() throws SQLException {
        logger.info("-------------------------getMovementData() Start-------------------------");
        String path = "/home/pi/Desktop/Light.py";
        String command[] = {"python", path};
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = null;
        try {
            process = builder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            if ((line = bufferedReader.readLine()) != null) {
                logger.info("getMovementData()-Get movement data:" + line);
                if (line.equalsIgnoreCase("off")) {
                    count++;
                    if (count >= 750) { // count 20 = 1 min | 1.25
                        if (movementStatusOFF) { //lightOnStatus
                            movementStatusOFF = false;
                            movementStatusON = true;
                            updateMovementStatus(line); // off
                            updateAppsStatus(line);    // off
                        } else {
                            logger.info("getMovementData(OFF)-Data already Inserted, Light:" + line + ", count:" + count);
                        }
                    } else {
                        logger.info("getMovementData(OFF)-Data not Insert yet, Light:" + line + ", count:" + count);
                    }
                } else {
                    count = 0;
                    if (movementStatusON) { //lightOffStatus
                        movementStatusOFF = true;
                        movementStatusON = false;
                        updateMovementStatus(line); // on
                        updateAppsStatus(line);    // on
                    } else {
                        logger.info("getMovementData(ON)-Data already Inserted, Light:" + line + ", count:" + count);
                    }
                }
            }
            bufferedReader.close();
            process.destroy();
            controlLightStatusFromApp();
        } catch (Exception ex) {
            connection.close();
            logger.error("getMovementData()-Exception found:" + ex + ", Connection:" + connection);
        }
    }

    private static void controlLightStatusFromApp() throws SQLException {
        String sqlSelectQuery, a_status = null;
        Statement statement = null;
        try {
            if (connection.isClosed() || connection == null) {
                connectionStatus = false;
                logger.warn("controlLightStatusFromApp()-Connection closed:" + connection + " ,connectionStatus:" + connectionStatus);
                connection = createDBConnection();
            }
            sqlSelectQuery = "SELECT a_status FROM Movement_Reg WHERE reg_id = 6";
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlSelectQuery);
            while (resultSet.next()) {
                a_status = resultSet.getString(1);
                logger.info("controlLightStatusFromApp()-Get a_status from DB:" + resultSet.getString(1));
                if (a_status.equalsIgnoreCase("on")) {
                    lightOn();
                } else {
                    lightOff();
                }
            }
            statement.close();
        } catch (Exception ex) {
            statement.close();
            connection.close();
            logger.error("controlLightStatusFromApp()-Exception found:" + ex + ", Connection:" + connection);
        }
    }

    private static boolean updateMovementStatus(String value) throws SQLException {
        String updateQuery = "";
        try {
            Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            if (connection.isClosed() || connection == null) {
                logger.warn("updateMovementRegTable()-Connection closed:" + connection);
                connection = createDBConnection();
            }
            Statement statement = connection.createStatement();

            updateQuery = "UPDATE Movement_Reg SET m_status = '" + value + "', date_time = '" + currentTimestamp + "' "
                    + "WHERE reg_id = 6";

            if (statement.executeUpdate(updateQuery) > 0) {
                statement.close();
                logger.info("updateMovementRegTable()-Data Inserted, Movement:" + value);
                return true;
            } else {
                logger.warn("updateMovementRegTable()-Data Insertion failed:" + updateQuery);
                return false;
            }
        } catch (SQLException ex) {
            connection.close();
            logger.error("updateMovementRegTable()-Movement:" + value + ", inserted failed:" + ex + ", SQL:" + updateQuery + ", Connection:" + connection);
        }
        logger.warn("updateMovementRegTable()-Data:" + value + ", inserted failed:false, SQL:" + updateQuery);
        return false;
    }

    private static String getAppStatus() throws SQLException {
        String sqlSelectQuery, a_status = null;
        Statement statement = null;
        try {
            if (connection.isClosed() || connection == null) {
                connectionStatus = false;
                logger.warn("getAppStatus()-Connection closed:" + connection + " ,connectionStatus:" + connectionStatus);
                connection = createDBConnection();
            }
            // Get App status from DB
            sqlSelectQuery = "SELECT a_status FROM Movement_Reg WHERE reg_id = 6";
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlSelectQuery);
            while (resultSet.next()) {
                a_status = resultSet.getString(1);
                logger.info("getAppStatus()-Get >>>>>>>>>>>>>>>>a_status from DB:" + resultSet.getString(1));
            }
            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            statement.close();
            connection.close();
            logger.error("getAppStatus()-Exception found:" + ex + ", Connection:" + connection);
        }
        return a_status;
    }

    private static boolean updateAppsStatus(String value) throws SQLException {
        logger.info("setAppStatus()-App Status data:" + value);
        String setSqlQuery = null;
        Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        try {
            if (connection.isClosed() || connection == null) {
                connectionStatus = false;
                logger.warn("updateAppsStatus()-Connection closed:" + connection + " ,connectionStatus:" + connectionStatus);
                connection = createDBConnection();
            }
            Statement statement = connection.createStatement();

            setSqlQuery = "UPDATE Movement_Reg SET a_status = '" + value + "', date_time = '" + currentTimestamp + "' "
                    + "WHERE reg_id = 6";

            if (statement.executeUpdate(setSqlQuery) > 0) {
                statement.close();
                logger.info("updateAppsStatus()-Data Inserted, App Status:" + value);
                return true;
            } else {
                logger.warn("updateAppsStatus()-Data Insertion failed:" + setSqlQuery);
                return false;
            }
        } catch (SQLException ex) {
            connection.close();
            logger.error("updateAppsStatus()-Exception found:" + ex + ", Connection:" + connection);
        }
        return false;
    }

    private static void lightOn() {
        if (lightOffStatus) { //lightOffStatus
            String path = "/home/pi/Desktop/LightOn.py";
            String command[] = {"python", path};
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = null;
            try {
                process = builder.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                if ((line = bufferedReader.readLine()) != null) {
                    logger.info("lightOn()-Get light status :" + line);
                }
                bufferedReader.close();
                process.destroy();
                logger.info("lightOn()-Light is ON");
                lightOnStatus = true;
                lightOffStatus = false;
            } catch (IOException ex) {
                logger.error("lightOn()-Exception found:" + ex);
            }
        } else {
            logger.info("lightOn()-Light already ON");
        }
    }

    private static void lightOff() {
        if (lightOnStatus) { //lightOnStatus
            String path = "/home/pi/Desktop/LightOff.py";
            String command[] = {"python", path};
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = null;
            try {
                process = builder.start();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                if ((line = bufferedReader.readLine()) != null) {
                    logger.info("lightOff()-Get light status :" + line);
                }
                bufferedReader.close();
                process.destroy();
                logger.info("lightOff()-Light is OFF");
                lightOnStatus = false;
                lightOffStatus = true;
            } catch (IOException ex) {
                logger.error("lightOff()-Exception found:" + ex);
            }
        } else {
            logger.info("lightOff()-Light already OFF");
        }
    }

    private synchronized static Connection createDBConnection() {
//        boolean connectionStatus = false;
        try {
            while (!connectionStatus) {
                logger.info("createDBConnection()-Get DB connection...");
                connection = (Connection) DriverManager.getConnection(
                        "jdbc:sqlserver://etenderdb.database.windows.net:1433;database=IOTPOC;user=etenderAdmin@etenderdb;password=Eraetender1@;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30");

                logger.info("createDBConnection()-Database connectivity status:" + connection);

                if (connection == null) {
                    logger.info("createDBConnection()-Connection failed (null):" + connection);
                    connectionStatus = false;
                    continue;
                }
                if ((connection.isClosed())) {
                    logger.info("createDBConnection()-Connection failed (connection.isClosed()):" + connection.isClosed());
                    connectionStatus = false;
                    continue;
                }
                logger.info("createDBConnection()-Connection up:" + connection);
                connectionStatus = true;
                return connection;
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
        connectionStatus = false;
        return connection;
    }
}