/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package competition;


import java.util.*;
import com.dukascopy.api.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.*;
import java.math.BigDecimal;


/*
 * Created by VisualJForex Generator, version 2.12
 * Date: 04.02.2016 21:56
 */
public class DT_Strategy implements IStrategy {

	private CopyOnWriteArrayList<TradeEventAction> tradeEventActions = new CopyOnWriteArrayList<TradeEventAction>();
	private static final String DATE_FORMAT_NOW = "yyyyMMdd_HHmmss";
	private IEngine engine;
	private IConsole console;
	private IHistory history;
	private IContext context;
	private IIndicators indicators;
	private IUserInterface userInterface;

	@Configurable("_defaultEMAPeriod:")
	public int _defaultEMAPeriod = 90;
	@Configurable("defaultSlippage:")
	public int defaultSlippage = 5;
	@Configurable("defaultTakeProfit:")
	public int defaultTakeProfit = 40;
	@Configurable("defaultPeriod:")
	public Period defaultPeriod = Period.FIFTEEN_MINS;
	@Configurable("defaultTradeAmount:")
	public double defaultTradeAmount = 6.0;
	@Configurable("defaultStopLoss:")
	public int defaultStopLoss = 155;
	@Configurable("defaultInstrument:")
	public Instrument defaultInstrument = Instrument.GBPJPY;

	private String AccountCurrency = "";
	private double Leverage;
	private double _prevEMA;
	private double _lowerStoch = 20.0;
	private double _takePofit;
	private double _currDStoch;
	private int _defaultKStochPeriod = 5;
	private int _defaultDStochPeriod = 3;
	private IOrder _currPosition =  null ;
	private Tick LastTick =  null ;
	private double _currEMA;
	private double _stopLoss;
	private int _currHour;
	private String AccountId = "";
	private double Equity;
	private double UseofLeverage;
	private List<IOrder> PendingPositions =  null ;
	private List<IOrder> AllPositions =  null ;
	private int OverWeekendEndLeverage;
	private int MarginCutLevel;
	private Candle LastAskCandle =  null ;
	private boolean GlobalAccount;
	private double _currKStoch;
	private List<IOrder> OpenPositions =  null ;
	private int _currDayOfWeek;
	private IMessage LastTradeEvent =  null ;
	private double _upperStoch = 80.0;
	private Candle LastBidCandle =  null ;


	public void onStart(IContext context) throws JFException {
		this.engine = context.getEngine();
		this.console = context.getConsole();
		this.history = context.getHistory();
		this.context = context;
		this.indicators = context.getIndicators();
		this.userInterface = context.getUserInterface();

		subscriptionInstrumentCheck(defaultInstrument);

		ITick lastITick = context.getHistory().getLastTick(defaultInstrument);
		LastTick = new Tick(lastITick, defaultInstrument);

		IBar bidBar = context.getHistory().getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 1);
		IBar askBar = context.getHistory().getBar(defaultInstrument, defaultPeriod, OfferSide.ASK, 1);
		LastAskCandle = new Candle(askBar, defaultPeriod, defaultInstrument, OfferSide.ASK);
		LastBidCandle = new Candle(bidBar, defaultPeriod, defaultInstrument, OfferSide.BID);

		if (indicators.getIndicator("EMA") == null) {
			indicators.registerDownloadableIndicator("1324","EMA");
		}
		if (indicators.getIndicator("EMA") == null) {
			indicators.registerDownloadableIndicator("1324","EMA");
		}
		if (indicators.getIndicator("STOCH") == null) {
			indicators.registerDownloadableIndicator("1279","STOCH");
		}
		subscriptionInstrumentCheck(Instrument.fromString("GBP/JPY"));

	}

	public void onAccount(IAccount account) throws JFException {
		AccountCurrency = account.getCurrency().toString();
		Leverage = account.getLeverage();
		AccountId= account.getAccountId();
		Equity = account.getEquity();
		UseofLeverage = account.getUseOfLeverage();
		OverWeekendEndLeverage = account.getOverWeekEndLeverage();
		MarginCutLevel = account.getMarginCutLevel();
		GlobalAccount = account.isGlobal();
	}

	private void updateVariables(Instrument instrument) {
		try {
			AllPositions = engine.getOrders();
			List<IOrder> listMarket = new ArrayList<IOrder>();
			for (IOrder order: AllPositions) {
				if (order.getState().equals(IOrder.State.FILLED)){
					listMarket.add(order);
				}
			}
			List<IOrder> listPending = new ArrayList<IOrder>();
			for (IOrder order: AllPositions) {
				if (order.getState().equals(IOrder.State.OPENED)){
					listPending.add(order);
				}
			}
			OpenPositions = listMarket;
			PendingPositions = listPending;
		} catch(JFException e) {
			e.printStackTrace();
		}
	}

	public void onMessage(IMessage message) throws JFException {
		if (message.getOrder() != null) {
			updateVariables(message.getOrder().getInstrument());
			LastTradeEvent = message;
			for (TradeEventAction event :  tradeEventActions) {
				IOrder order = message.getOrder();
				if (order != null && event != null && message.getType().equals(event.getMessageType())&& order.getLabel().equals(event.getPositionLabel())) {
					Method method;
					try {
						method = this.getClass().getDeclaredMethod(event.getNextBlockId(), Integer.class);
						method.invoke(this, new Integer[] {event.getFlowId()});
					} catch (SecurityException e) {
							e.printStackTrace();
					} catch (NoSuchMethodException e) {
						  e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					} 
					tradeEventActions.remove(event); 
				}
			}   
		}
	}

	public void onStop() throws JFException {
	}

	public void onTick(Instrument instrument, ITick tick) throws JFException {
		LastTick = new Tick(tick, instrument);
		updateVariables(instrument);


	}

	public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
		LastAskCandle = new Candle(askBar, period, instrument, OfferSide.ASK);
		LastBidCandle = new Candle(bidBar, period, instrument, OfferSide.BID);
		updateVariables(instrument);
			If_block_30(1);

	}

    public void subscriptionInstrumentCheck(Instrument instrument) {
		try {
		      if (!context.getSubscribedInstruments().contains(instrument)) {
		          Set<Instrument> instruments = new HashSet<Instrument>();
		          instruments.add(instrument);
		          context.setSubscribedInstruments(instruments, true);
		          Thread.sleep(100);
		      }
		  } catch (InterruptedException e) {
		      e.printStackTrace();
		  }
		}

    public double round(double price, Instrument instrument) {
		BigDecimal big = new BigDecimal("" + price); 
		big = big.setScale(instrument.getPipScale() + 1, BigDecimal.ROUND_HALF_UP); 
		return big.doubleValue(); 
	}

    public ITick getLastTick(Instrument instrument) {
		try { 
			return (context.getHistory().getTick(instrument, 0)); 
		} catch (JFException e) { 
			 e.printStackTrace();  
		 } 
		 return null; 
	}

	private  void OpenatMarket_block_21(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		double argument_2 = defaultTradeAmount;
		int argument_3 = defaultSlippage;
		double argument_4 = _stopLoss;
		double argument_5 = _takePofit;
		String argument_6 = "";
		ITick tick = getLastTick(argument_1);

		IEngine.OrderCommand command = IEngine.OrderCommand.SELL;

		double stopLoss = round(argument_4, argument_1);
		double takeProfit = round(argument_5, argument_1);
		
           try {
               String label = getLabel();           
               IOrder order = context.getEngine().submitOrder(label, argument_1, command, argument_2, 0, argument_3,  stopLoss, takeProfit, 0, argument_6);
		        } catch (JFException e) {
            e.printStackTrace();
        }
	}

	private  void OpenatMarket_block_23(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		double argument_2 = defaultTradeAmount;
		int argument_3 = defaultSlippage;
		double argument_4 = _stopLoss;
		double argument_5 = _takePofit;
		String argument_6 = "";
		ITick tick = getLastTick(argument_1);

		IEngine.OrderCommand command = IEngine.OrderCommand.BUY;

		double stopLoss = round(argument_4, argument_1);
		double takeProfit = round(argument_5, argument_1);
		
           try {
               String label = getLabel();           
               IOrder order = context.getEngine().submitOrder(label, argument_1, command, argument_2, 0, argument_3,  stopLoss, takeProfit, 0, argument_6);
		        } catch (JFException e) {
            e.printStackTrace();
        }
	}

	private  void If_block_30(Integer flow) {
		Period argument_1 = LastBidCandle.getPeriod();
		Period argument_2 = defaultPeriod;
		if (argument_1!= null && !argument_1.equals(argument_2)) {
		}
		else if (argument_1!= null && argument_1.equals(argument_2)) {
			If_block_44(flow);
		}
	}

	private  void If_block_44(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		Instrument argument_2 = LastBidCandle.getInstrument();
		if (argument_1!= null && !argument_1.equals(argument_2)) {
		}
		else if (argument_1!= null && argument_1.equals(argument_2)) {
			GetTimeUnit_block_48(flow);
		}
	}

	private  void GetTimeUnit_block_48(Integer flow) {
		long argument_1 = LastBidCandle.getTime();
		Date date = new Date(argument_1);
		Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
		calendar.setTime(date);
		_currDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		If_block_54(flow);
	}

	private  void If_block_49(Integer flow) {
		int argument_1 = _currDayOfWeek;
		int argument_2 = 6;
		if (argument_1< argument_2) {
			STOCH_block_94(flow);
		}
		else if (argument_1> argument_2) {
		}
		else if (argument_1== argument_2) {
			STOCH_block_94(flow);
		}
	}

	private  void If_block_54(Integer flow) {
		int argument_1 = _currDayOfWeek;
		int argument_2 = 2;
		if (argument_1< argument_2) {
		}
		else if (argument_1> argument_2) {
			If_block_49(flow);
		}
		else if (argument_1== argument_2) {
			If_block_49(flow);
		}
	}

	private  void If_block_56(Integer flow) {
		int argument_1 = AllPositions.size();
		double argument_2 = 0.0;
		if (argument_1< argument_2) {
		}
		else if (argument_1> argument_2) {
			PositionsViewer_block_97(flow);
		}
		else if (argument_1== argument_2) {
			EMA_block_72(flow);
		}
	}

	private void CalculationExpression_block_60(Integer flow) {
		_takePofit = LastBidCandle.getClose()-(defaultTakeProfit*defaultInstrument.getPipValue());
		OpenatMarket_block_21(flow);
	}

	private void CalculationExpression_block_61(Integer flow) {
		_stopLoss = LastBidCandle.getLow()-(defaultStopLoss*defaultInstrument.getPipValue());
		CalculationExpression_block_62(flow);
	}

	private void CalculationExpression_block_62(Integer flow) {
		_takePofit = LastBidCandle.getClose()+(defaultTakeProfit*defaultInstrument.getPipValue());
		OpenatMarket_block_23(flow);
	}

	private void EMA_block_72(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		Period argument_2 = defaultPeriod;
		int argument_3 = 0;
		int argument_4 = _defaultEMAPeriod;
		OfferSide[] offerside = new OfferSide[1];
		IIndicators.AppliedPrice[] appliedPrice = new IIndicators.AppliedPrice[1];
		offerside[0] = OfferSide.BID;
		appliedPrice[0] = IIndicators.AppliedPrice.MEDIAN_PRICE;
		Object[] params = new Object[1];
		params[0] = _defaultEMAPeriod;
		try {
			subscriptionInstrumentCheck(argument_1);
			long time = context.getHistory().getBar(argument_1, argument_2, OfferSide.BID, argument_3).getTime();
			Object[] indicatorResult = context.getIndicators().calculateIndicator(argument_1, argument_2, offerside,
					"EMA", appliedPrice, params, Filter.WEEKENDS, 1, time, 0);
			if ((new Double(((double [])indicatorResult[0])[0])) == null) {
				this._currEMA = Double.NaN;
			} else { 
				this._currEMA = (((double [])indicatorResult[0])[0]);
			} 
		} catch (JFException e) {
			e.printStackTrace();
		}
		EMA_block_73(flow);
	}

	private void EMA_block_73(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		Period argument_2 = defaultPeriod;
		int argument_3 = 1;
		int argument_4 = _defaultEMAPeriod;
		OfferSide[] offerside = new OfferSide[1];
		IIndicators.AppliedPrice[] appliedPrice = new IIndicators.AppliedPrice[1];
		offerside[0] = OfferSide.BID;
		appliedPrice[0] = IIndicators.AppliedPrice.MEDIAN_PRICE;
		Object[] params = new Object[1];
		params[0] = _defaultEMAPeriod;
		try {
			subscriptionInstrumentCheck(argument_1);
			long time = context.getHistory().getBar(argument_1, argument_2, OfferSide.BID, argument_3).getTime();
			Object[] indicatorResult = context.getIndicators().calculateIndicator(argument_1, argument_2, offerside,
					"EMA", appliedPrice, params, Filter.WEEKENDS, 1, time, 0);
			if ((new Double(((double [])indicatorResult[0])[0])) == null) {
				this._prevEMA = Double.NaN;
			} else { 
				this._prevEMA = (((double [])indicatorResult[0])[0]);
			} 
		} catch (JFException e) {
			e.printStackTrace();
		}
		If_block_74(flow);
	}

	private  void If_block_74(Integer flow) {
		double argument_1 = _prevEMA;
		double argument_2 = _currEMA;
		if (argument_1< argument_2) {
			If_block_95(flow);
		}
		else if (argument_1> argument_2) {
			If_block_96(flow);
		}
		else if (argument_1== argument_2) {
		}
	}

	private void STOCH_block_94(Integer flow) {
		Instrument argument_1 = defaultInstrument;
		Period argument_2 = defaultPeriod;
		int argument_3 = 0;
		int argument_4 = _defaultKStochPeriod;
		int argument_5 = _defaultDStochPeriod;
		IIndicators.MaType argument_6 = IIndicators.MaType.SMA;
		int argument_7 = 3;
		IIndicators.MaType argument_8 = IIndicators.MaType.SMA;
		OfferSide[] offerside = new OfferSide[1];
		IIndicators.AppliedPrice[] appliedPrice = new IIndicators.AppliedPrice[1];
		offerside[0] = OfferSide.BID;
		appliedPrice[0] = IIndicators.AppliedPrice.CLOSE;
		Object[] params = new Object[5];
		params[0] = _defaultKStochPeriod;
		params[1] = _defaultDStochPeriod;
		params[2] = 0;
		params[3] = 3;
		params[4] = 0;
		try {
			subscriptionInstrumentCheck(argument_1);
			long time = context.getHistory().getBar(argument_1, argument_2, OfferSide.BID, argument_3).getTime();
			Object[] indicatorResult = context.getIndicators().calculateIndicator(argument_1, argument_2, offerside,
					"STOCH", appliedPrice, params, Filter.WEEKENDS, 1, time, 0);
			if ((new Double(((double [])indicatorResult[0])[0])) == null) {
				this._currKStoch = Double.NaN;
			} else { 
				this._currKStoch = (((double [])indicatorResult[0])[0]);
			} 
			if ((new Double(((double [])indicatorResult[1])[0])) == null) {
				this._currDStoch = Double.NaN;
			} else { 
				this._currDStoch = (((double [])indicatorResult[1])[0]);
			} 
		} catch (JFException e) {
			e.printStackTrace();
		}
		If_block_56(flow);
	}

	private  void If_block_95(Integer flow) {
		double argument_1 = _currDStoch;
		double argument_2 = _lowerStoch;
		if (argument_1< argument_2) {
			CalculationExpression_block_61(flow);
		}
		else if (argument_1> argument_2) {
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void If_block_96(Integer flow) {
		double argument_1 = _currDStoch;
		double argument_2 = _upperStoch;
		if (argument_1< argument_2) {
		}
		else if (argument_1> argument_2) {
			CalculationExpression_block_103(flow);
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void PositionsViewer_block_97(Integer flow) {
		List<IOrder> argument_1 = AllPositions;
		for (IOrder order : argument_1){
			if (order.getState() == IOrder.State.OPENED||order.getState() == IOrder.State.FILLED){
				_currPosition = order;
				If_block_98(flow);
			}
		}
	}

	private  void If_block_98(Integer flow) {
		boolean argument_1 = _currPosition.isLong();
		boolean argument_2 = true;
		if (argument_1!= argument_2) {
			If_block_100(flow);
		}
		else if (argument_1 == argument_2) {
			If_block_101(flow);
		}
	}

	private  void If_block_100(Integer flow) {
		double argument_1 = _currDStoch;
		double argument_2 = _lowerStoch;
		if (argument_1< argument_2) {
			CloseandCancelPosition_block_102(flow);
		}
		else if (argument_1> argument_2) {
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void If_block_101(Integer flow) {
		double argument_1 = _currDStoch;
		double argument_2 = _upperStoch;
		if (argument_1< argument_2) {
		}
		else if (argument_1> argument_2) {
			CloseandCancelPosition_block_102(flow);
		}
		else if (argument_1== argument_2) {
		}
	}

	private  void CloseandCancelPosition_block_102(Integer flow) {
		try {
			if (_currPosition != null && (_currPosition.getState() == IOrder.State.OPENED||_currPosition.getState() == IOrder.State.FILLED)){
				_currPosition.close();
			}
		} catch (JFException e)  {
			e.printStackTrace();
		}
	}

	private void CalculationExpression_block_103(Integer flow) {
		_stopLoss = LastAskCandle.getHigh()+(defaultStopLoss*defaultInstrument.getPipValue());
		CalculationExpression_block_60(flow);
	}

class Candle  {

    IBar bar;
    Period period;
    Instrument instrument;
    OfferSide offerSide;

    public Candle(IBar bar, Period period, Instrument instrument, OfferSide offerSide) {
        this.bar = bar;
        this.period = period;
        this.instrument = instrument;
        this.offerSide = offerSide;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public OfferSide getOfferSide() {
        return offerSide;
    }

    public void setOfferSide(OfferSide offerSide) {
        this.offerSide = offerSide;
    }

    public IBar getBar() {
        return bar;
    }

    public void setBar(IBar bar) {
        this.bar = bar;
    }

    public long getTime() {
        return bar.getTime();
    }

    public double getOpen() {
        return bar.getOpen();
    }

    public double getClose() {
        return bar.getClose();
    }

    public double getLow() {
        return bar.getLow();
    }

    public double getHigh() {
        return bar.getHigh();
    }

    public double getVolume() {
        return bar.getVolume();
    }
}
class Tick {

    private ITick tick;
    private Instrument instrument;

    public Tick(ITick tick, Instrument instrument){
        this.instrument = instrument;
        this.tick = tick;
    }

    public Instrument getInstrument(){
       return  instrument;
    }

    public double getAsk(){
       return  tick.getAsk();
    }

    public double getBid(){
       return  tick.getBid();
    }

    public double getAskVolume(){
       return  tick.getAskVolume();
    }

    public double getBidVolume(){
        return tick.getBidVolume();
    }

   public long getTime(){
       return  tick.getTime();
    }

   public ITick getTick(){
       return  tick;
    }
}

    protected String getLabel() {
        String label;
        label = "IVF" + getCurrentTime(LastTick.getTime()) + generateRandom(10000) + generateRandom(10000);
        return label;
    }

    private String getCurrentTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(time);
    }

    private static String generateRandom(int n) {
        int randomNumber = (int) (Math.random() * n);
        String answer = "" + randomNumber;
        if (answer.length() > 3) {
            answer = answer.substring(0, 4);
        }
        return answer;
    }

    class TradeEventAction {
		private IMessage.Type messageType;
		private String nextBlockId = "";
		private String positionLabel = "";
		private int flowId = 0;

        public IMessage.Type getMessageType() {
            return messageType;
        }

        public void setMessageType(IMessage.Type messageType) {
            this.messageType = messageType;
        }

        public String getNextBlockId() {
            return nextBlockId;
        }

        public void setNextBlockId(String nextBlockId) {
            this.nextBlockId = nextBlockId;
        }
        public String getPositionLabel() {
            return positionLabel;
       }

        public void setPositionLabel(String positionLabel) {
            this.positionLabel = positionLabel;
        }
        public int getFlowId() {
            return flowId;
        }
        public void setFlowId(int flowId) {
            this.flowId = flowId;
        }
    }
}