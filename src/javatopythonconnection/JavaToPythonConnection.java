/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javatopythonconnection;

import java.io.File;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Rumee
 */
public class JavaToPythonConnection {

    static Logger logger = Logger.getLogger(JavaToPythonConnection.class);

    public static void main(String[] args) {
        String log4jConfigFile = System.getProperty("user.dir") + File.separator + "log4j.properties";
        PropertyConfigurator.configure(log4jConfigFile);

        logger.info("Main System Starting...");
        System.out.println("javatopythonconnection.JavaToPythonConnection.main()");
        // Temperature
        TemperatureThread temperatureThread = new TemperatureThread();
        Thread temperatureThreadOne = new Thread(temperatureThread);
        temperatureThreadOne.start();
        // Light ON/OFF       
        AutomaticLightSwitchingThread automaticLightSwitchingThread = new AutomaticLightSwitchingThread();
        Thread automaticLightSwitchingThreadOne = new Thread(automaticLightSwitchingThread);
        automaticLightSwitchingThreadOne.start();
    }
}
