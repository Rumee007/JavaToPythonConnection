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
    static Logger logger = Logger.getLogger(AutomaticLightSwitchingThread.class);

    @Override
    public void run() {
        try {
            logger.info("run()-Starting AutomaticLightSwitchingThread...");
            connection = createDBConnection();
            //!Thread.currentThread().isInterrupted()
            while (true) {
                Thread.sleep(500);
                //getMovementData();
                controlLightStatusFromApp();

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
                logger.info("light lightOffStatus : " + lightOffStatus);
                logger.info("light lightOnStatus : " + lightOnStatus);
                if (line.equalsIgnoreCase("off")) {
                    count++;
                    if (count >= 20) { // count 20 = 1 min
                        if (lightOnStatus) {
                            insertData(line);
                            lightOff();
                            lightOnStatus = false;
                            lightOffStatus = true;
                            logger.info("getMovementData(OFF)-Data Inserted, Light:" + line + ", count:" + count);
                        } else {
                            logger.info("getMovementData(OFF)-Data already Inserted, Light:" + line + ", count:" + count);
                        }
                    } else {
                        logger.info("getMovementData(OFF)-Data Insert failed, Light:" + line + ", count:" + count);
                    }
                } else {
                    count = 0;
                    if (lightOffStatus) {
                        lightOn();
                        lightOnStatus = true;
                        lightOffStatus = false;
                    }
                    if (insertData(line)) {
                        logger.info("getMovementData(ON)-Data Inserted, Light:" + line + ", count:" + count);
                    } else {
                        logger.info("getMovementData(ON)-Data Insert failed, Light:" + line + ", count:" + count);
                    }
                }
            }
            bufferedReader.close();
            process.destroy();
        } catch (Exception ex) {
            connection.close();
            logger.error("getMovementData()-Exception found:" + ex + ", Connection:" + connection);
        }
    }

    private static void controlLightStatusFromApp() throws SQLException {
        String sqlSelectQuery, a_status = null;
        try {
            if (connection.isClosed()) {
                //connection.isClosed() || connection == null
                logger.warn("controlLightStatusFromApp()-Connection closed:" + connection);
                connection = createDBConnection();
            }
            sqlSelectQuery = "SELECT a_status FROM Movement_Reg WHERE reg_id = 1";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlSelectQuery);
            while (resultSet.next()) {
                a_status = resultSet.getString(1);
                if (a_status.equalsIgnoreCase("on")) {
                    lightOn();
                } else {
                    lightOff();
                }
                logger.info("controlLightStatusFromApp()-Get a_status from DB:" + resultSet.getString(1));
            }
            statement.close();
            connection.close();
            logger.info("controlLightStatusFromApp()-connection.isClosed():" + connection.isClosed());
        } catch (Exception ex) {
            connection.close();
            logger.error("controlLightStatusFromApp()-Inserted failed:" + ex + ", Connection:" + connection);
        }

    }

    private synchronized static boolean insertData(String value) throws SQLException {
        String sqlQuery = null;
        try {
            Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
            if (connection.isClosed() || connection == null) {
                logger.warn("insertData()-Connection closed:" + connection);
                connection = createDBConnection();
            }
            Statement statement = connection.createStatement();

            sqlQuery = "UPDATE Movement_Reg SET m_status = '" + value + "', date_time = '" + currentTimestamp + "' "
                    + "WHERE reg_id = 1";

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

    private static void lightOn() {
        if (lightOffStatus) {
            String path = "/home/pi/Desktop/LightOn.py";
            String command[] = {"python", path};
            ProcessBuilder builder = new ProcessBuilder(command);
            logger.info("lightOn()-System is running...");
            lightOnStatus = true;
            lightOffStatus = false;
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
            } catch (Exception ex) {
                logger.error("lightOn()-Exception found");
            }
        } else {
            logger.info("lightOn()-Light already ON");
        }
    }

    private static void lightOff() {
        if (lightOnStatus) {
            String path = "/home/pi/Desktop/LightOff.py";
            String command[] = {"python", path};
            ProcessBuilder builder = new ProcessBuilder(command);
            logger.info("lightOff()-System is running...");
            lightOnStatus = false;
            lightOffStatus = true;
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
            } catch (Exception ex) {
                logger.error("lightOff()-Exception found");
            }
        }
    }

    private static Connection createDBConnection() {
        boolean connectionStatus = false;
        while (!connectionStatus) {
            try {
            logger.info("createDBConnection()-Get DB connection");
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
