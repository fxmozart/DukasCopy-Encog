
package encogdukascopy;


import NeuralStrategies.NeuralStrategy;
import com.dukascopy.api.Instrument;

import com.dukascopy.api.LoadingProgressListener;

import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.io.File;
import org.apache.log4j.BasicConfigurator;





/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy
 */
public class NeuralMainTrainer {
    private static final Logger LOGGER = LoggerFactory.getLogger("encogdukascopy.NeuralMainTrainer.class");
  public Instrument selectedInstrument;
        public double price     ;
        public double takeProfit;
    //url of the DEMO jnlp
    private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    //user name
    private static String userName = "UserName";
    //password
    private static String password = "UserPass";
//No generics

    
    
    
    
    /**
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        
        BasicConfigurator.configure();
        //get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                File reportFile = new File("C:\\report.html");
                try {
                    client.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
    
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        //setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryCurrency(), 50000);
        //load data
        
        
        LOGGER.info("Downloading data");
        
      //  Future<?> future = client.downloadData(null);
        //wait for downloading to complete
     //   future.get();
        //start the strategy
        LOGGER.info("Starting strategy");
    
        client.startStrategy(new NeuralStrategy(),  new LoadingProgressListener() {
            
         
             
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
              
                
                LOGGER.info("Loaded data from "+startTime +" with information :" + information);
                
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                
                 LOGGER.info("Loaded data finished from "+startTime +" all data loaded :" + allDataLoaded);
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        });
      /*
        client.startStrategy(new DeCiDEA(),  new LoadingProgressListener() {
            @Override
            public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                LOGGER.info(information);
                
            }

            @Override
            public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                
                 
            }

            @Override
            public boolean stopJob() {
                return false;
            }
        });
         * 
         * */
        
        //now it's running
    }








}
