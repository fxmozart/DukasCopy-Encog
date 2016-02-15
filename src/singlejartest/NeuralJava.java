/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package singlejartest;

/**
 *
 * @author Mel
 */

   import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IIndicators.MaType;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.indicators.IIndicator;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.pattern.ElmanPattern;
import org.encog.util.EngineArray;
import org.encog.util.arrayutil.NormalizeArray;
import org.encog.ml.train.strategy.RequiredImprovementStrategy;
/**
 *
 * @author Olivier
 */
public class NeuralJava  implements IStrategy {
    
    private IIndicators indicators;
    public static DecimalFormat df = new DecimalFormat("0.00000");
     List<IBar> barList;
     boolean logValues = true;
     private SimpleDateFormat sdf;
     IHistory ihistory;
     IEngine iengine;
     IContext icontext;
     IConsole iconsole;
   
  
     
     
     public double [][]  IBarsToArray(List<IBar> inputBars)
     {
         
         double [][] ohlcArray = new double[4][inputBars.size()];
        for (int i = 0; i < inputBars.size(); i++){
               /*         ohlcArray[0][i] = inputBars[i].getOpen();
                        ohlcArray[1][i] = inputBars[i].getHigh();
                        ohlcArray[2][i] = inputBars[i].getLow();
                        ohlcArray[3][i] = inputBars[i].getClose();
                        */
                        ohlcArray[0][i] = inputBars.get(i).getOpen();
                        ohlcArray[1][i] = inputBars.get(i).getHigh();
                        ohlcArray[2][i] = inputBars.get(i).getLow();
                        ohlcArray[3][i] = inputBars.get(i).getClose();
        }

     
        return ohlcArray;
     }
     private void print(Object o){
		System.out.println(o);
	}
	
	private void log(Object o){
		if(logValues){
			print(o);
		}
	}
	
	public static String arrayToString(double[] arr) {
		String str = "";
		for (int r = 0; r < arr.length; r++) {
			str += "[" + r + "] " + df.format(arr[r]) + "; ";
		}
		return str;
	}

    /**
	 * Calculates MA over an array, see more:
	 * http://www.dukascopy.com/wiki/index.php?title=Strategy_API:_Indicators#Calculate_indicator_on_array
	 * 
	 * @param timePeriod
	 * @param shift
	 * @param priceArr
	 * @return
	 */
	private double getMa(int timePeriod, int shift, double [] priceArr ) {
		IIndicator maIndicator = indicators.getIndicator("MA");
		 
		//set optional inputs
		maIndicator.setOptInputParameter(0, timePeriod);
		maIndicator.setOptInputParameter(1, MaType.SMA.ordinal());
		 
		//set inputs
		maIndicator.setInputParameter(0, priceArr);
		 
		//set outputs
		double [] resultArr = new double [priceArr.length];
		maIndicator.setOutputParameter(0, resultArr);
		 
		//calculate
		maIndicator.calculate(0, priceArr.length - 1);
		print("ma result array: " + arrayToString(resultArr));
		
		int index = resultArr.length - shift - 1 - maIndicator.getLookback();
		double result = resultArr[index];
		return result;
	}

    /**
     * Send the Array from dukascopy to neural analysis.
     * @param dukasArray
     * @return
     */
    public double[] GetWorkableArray(double [][] dukasArray, int whichArray)
        {
            double [] Arrayed = new double [dukasArray[whichArray].length];
            Arrayed = dukasArray[whichArray];
            return Arrayed;
        }
    
    /**
     * the array we will use to give data to encog.
     */
    public double[] SUNSPOTS  = new double[1500];
    
    
    //Max number of times we will let encog train.
   private int MaxEpochs = 500;
    public int NeuronInputSize = 5;
    public int NeuronOutputSize = 1;
    NormalizeArray norm = new NormalizeArray();
    
    /**
     * Full data (holds all the data , open , high, low , close)
     */
    public double[][] FullData  = new double[4][1500];
    
        /**
         * 
         */
        public final static int STARTING_YEAR = 100;
        /**
         * 
         */
        public final static int TRAIN_START = 0;
        /**
         * 
         */
        public final static int TRAIN_END = 1000;
        /**
         * 
         */
        public final static int EVALUATE_START = 5010;
        /**
         * 
         */
        public int EVALUATE_END = 0;
	
	/**
	 * This really should be lowered, I am setting it to a level here that will
	 * train in under a minute.
	 */
	public final static double MAX_ERROR = 0.01;

	private double[] normalizedSunspots;
	private double[] closedLoopSunspots;
	
        /**
         * 
         * @param lo
         * @param hi
         */
        public void normalizeSunspots(double lo, double hi) {
        
        norm.setNormalizedHigh( hi);
        norm.setNormalizedLow( lo);

        // create arrays to hold the normalized sunspots
        normalizedSunspots = norm.process(SUNSPOTS);
        closedLoopSunspots = EngineArray.arrayCopy(normalizedSunspots);
	}

	
        /**
         * Generates the training data from the array.
         * @return
         */
        public MLDataSet generateTraining()
	{
		MLDataSet result = new BasicMLDataSet();
				
		for(int year = TRAIN_START;year<TRAIN_END;year++)
		{
			MLData inputData = new BasicMLData(this.NeuronInputSize);
			MLData idealData = new BasicMLData(this.NeuronOutputSize);
			inputData.setData(0,this.normalizedSunspots[year]);
			idealData.setData(0,this.normalizedSunspots[year+1]);
			result.add(inputData,idealData);
		}
		
		return result;
	}
	
        /**
         * 
         * @return
         */
        public BasicNetwork createNetwork()
	{
		ElmanPattern pattern = new ElmanPattern();
		pattern.setInputNeurons(this.NeuronInputSize);
		pattern.addHiddenLayer(30);
		pattern.setOutputNeurons(this.NeuronOutputSize);
		pattern.setActivationFunction(new ActivationTANH());
		return (BasicNetwork)pattern.generate();
	}
	
        /**
         * 
         * @param network
         * @param training
         */
        public void train(BasicNetwork network,MLDataSet training)
	{
		final Train train = new ResilientPropagation(network, training);

                RequiredImprovementStrategy improvSt = new RequiredImprovementStrategy(1);
                
                train.addStrategy(improvSt);
                
                
		int epoch = 1;

		do {
			train.iteration();
			System.out
					.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
		} while(train.getError() > MAX_ERROR && epoch < MaxEpochs);
		
		System.out.println( network.calculateError(training));
	}
	
        /**
         * 
         * @param network
         */
        public void predict(BasicNetwork network)
	{
		NumberFormat f = NumberFormat.getNumberInstance();
		f.setMaximumFractionDigits(4);
		f.setMinimumFractionDigits(4);
		
		System.out.println("Year\t\tActual\tPredict\tClosed Loop Predict\tReal Value\tPredcted Value");
		
		BasicNetwork regular = (BasicNetwork)network.clone();
		BasicNetwork closedLoop = (BasicNetwork)network.clone();
		
		regular.clearContext();
		closedLoop.clearContext();
		
		for(int year=1;year<this.normalizedSunspots.length;year++)
		{
			// calculate based on actual data
			MLData input = new BasicMLData(this.NeuronInputSize);
			input.setData(0, this.normalizedSunspots[year-1]);
			
			MLData output = regular.compute(input);
			double prediction = output.getData(0);
			this.closedLoopSunspots[year] = prediction;
			
			
			// calculate "closed loop", based on predicted data
			input.setData(0, this.closedLoopSunspots[year-1]);
			output = closedLoop.compute(input);
			double closedLoopPrediction = output.getData(0);
			double denormed = norm.getStats().deNormalize(prediction);
                        
			String t;
			if( year< EVALUATE_START ) {
				t = "Train:";
			} else {
				t = "Evalu:";
			}
			
			// display
			System.out.println( t + (STARTING_YEAR+year)
					+"\t"+f.format(this.normalizedSunspots[year])
					+"\t"+"\t"+f.format(prediction)
					+"\t"+f.format(closedLoopPrediction)
                                        +"\t"+f.format(SUNSPOTS[year])
                                        +"\t"+f.format(denormed)
                                
			);
			
		}
	}
	
        /**
         * 
         */
        public void run()
	{
		normalizeSunspots(0.1,0.9);
		BasicNetwork network = createNetwork();
		MLDataSet training = generateTraining();
		train(network,training);
		predict(network);
		
	}

     private int counter = new Random().nextInt(100);
    // notice the lack of fields to manage JForex objects
 
    @Override
    public void onStart(IContext context) throws JFException {
        // ** Essential steps **
        // must initialize objects once and for all
      
 
        this.icontext = context;
        this.ihistory=context.getHistory();
        this.iengine=context.getEngine();
        this.iconsole=context.getConsole();
        this.indicators=context.getIndicators();
        iconsole.getOut().println("Strategy Neural Started");
        Set<Instrument> set = new HashSet<Instrument>(context.getSubscribedInstruments());
        set = context.getSubscribedInstruments();   // get list of subscribed instruments
        // subscribe to transitional instruments for currency conversion calculations
     
        
      //  Calendar StartDate = Calendar.getInstance(); 
        try {
            //Lets get bars from the start date , 500 before and 1000 after (total 1500 bars)...Used for training.
          //   barList=  JForexContext.getHistory().getBars(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.BID , Filter.WEEKENDS , 500,StartDate.getTimeInMillis(),1000);
            
            barList = GetBars();
        } catch (java.text.ParseException ex) {
            Logger.getLogger(NeuralJava.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         //Lets get our bars into an dim array.
         FullData = IBarsToArray(barList);
         
         //Lets copy the closing values to sunspots
         SUNSPOTS = GetWorkableArray(FullData,3);
         
         EVALUATE_END = SUNSPOTS.length -1;
         //Now lets get to do some neural work....
          run();
         
    }
 public List<IBar> GetBars() throws java.text.ParseException, JFException
 {
     SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    Date dateFrom = dateFormat.parse("05/10/2008 00:00:00");
    Date dateTo = dateFormat.parse("04/10/2010 00:00:00");
    List<IBar> bars = icontext.getHistory().getBars(Instrument.EURUSD, Period.ONE_HOUR, OfferSide.ASK, dateFrom.getTime(), dateTo.getTime());
 
    return bars;
 }
    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar,
            IBar bidBar) throws JFException {
  
        
    }
 
    @Override
    public void onAccount(IAccount account) throws JFException {
      
 
   
    }
 
    @Override
    public void onStop() throws JFException {
          for (IOrder order : iengine.getOrders()) {
            order.close();
        }
        iconsole.getOut().println("Stopped");
        
        
    }

    public void onTick(Instrument instrument, ITick tick) throws JFException {
       
    }

    public void onMessage(IMessage message) throws JFException {
     
    }
    
}