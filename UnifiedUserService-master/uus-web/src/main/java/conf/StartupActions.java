package conf;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dao.SetupDao;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.scheduler.Schedule;
import ninja.utils.NinjaProperties;

@Singleton
public class StartupActions {
    private NinjaProperties ninjaProperties;
    @Inject
    SetupDao setupDao;
  
	private static Logger log = LogManager.getLogger(StartupActions.class);


    @Inject
    public StartupActions(NinjaProperties ninjaProperties) {
//        this.ninjaProperties = ninjaProperties;
  //      System.setProperty("log4j.configurationFile", ninjaProperties.get("log4j.configurationFile"));
    }

	//@Schedule(delay=2, initialDelay=5, timeUnit=TimeUnit.SECONDS)
	public void logData() {
		log.debug(System.getProperty("log4j.configurationFile"));
		log.trace("trace");
		log.debug("debug");
		log.info("info");
		log.warn("warn");
		log.error("error");
		log.fatal("fatal" + " ===" + System.getProperty("log4j.configurationFile"));
	}
    
	@Start(order = 10)
	public void startService() {
		System.out.println("Service started");
	}
	
	/**
     * creates dummy data for the application
     */
	@Start(order = 100)
	public void generateDummyDataWhenInTest() {
		// always make sure free email provider table is filled
		//setupDao.setup();
	}

	@Dispose(order = 10)
	public void stopService() {
		System.out.println("Service Stopped");
	}
}