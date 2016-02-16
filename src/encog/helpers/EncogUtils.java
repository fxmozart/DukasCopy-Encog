/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package encog.helpers;

/**
 *
 * @author Mel
 */
import java.io.File;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.market.MarketMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.simple.EncogUtility;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fxmozart
 */
public class EncogUtils {
    
    
     /**
         * Saves a network to a filepath for later retrieval
         * @param network
         * @param file 
         */
        public static void saveNetwork(BasicNetwork network,String file)
        {      
          EncogDirectoryPersistence.saveObject(new File(file),network);
        }
        /**
         * Loads a network from a file path
         * @param file
         * @return 
         */
        public static BasicNetwork loadNetwork(String file)
        {      
          BasicNetwork network = (BasicNetwork)EncogDirectoryPersistence.loadObject(new File(file));
          return network;
        }
        
        /***
         * Saves a market dataset to file
         * @param dataDir the directory where you want to save your dataset
         * @param TRAINING_FILE the file holding the dataset
         * @param market the market dataset
         */
        public static void SaveMarketDataSet(String dataDir,String TRAINING_FILE,MarketMLDataSet market)
        {
            EncogUtility.saveEGB(new File(dataDir,TRAINING_FILE), market);
        }
        /***
         * Saves any MLDataset to file
         * @param dataDir the directory where you want to save your dataset
         * @param TRAINING_FILE the file holding the dataset
         * @param dataset the Dataset dataset
         */
        public static void SaveMarketDataSet(String dataDir,String TRAINING_FILE,MLDataSet dataset)
        {
            EncogUtility.saveEGB(new File(dataDir,TRAINING_FILE), dataset);
        }
        
        /***
         * Loads a dataset to memory
         * @param file
         * @return 
         */
        public static MLDataSet LoadMLDataSet(String file)
        {
           return EncogUtility.loadEGB2Memory(new File(file));
        }
}