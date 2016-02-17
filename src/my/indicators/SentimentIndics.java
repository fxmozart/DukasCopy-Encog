/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.indicators;

/**
 *
 * @author Olivier.Guiglionda@gmail.com
 */


import java.awt.Color;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.swing.SwingConstants;

import com.dukascopy.api.ChartObjectEvent;
import com.dukascopy.api.ChartObjectListener;
import com.dukascopy.api.ICurrency;
import com.dukascopy.api.IDataService;
import com.dukascopy.api.IFXSentimentIndex;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IOhlcChartObject;
import com.dukascopy.api.drawings.IOhlcChartObject.OhlcAlignment;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.indicators.BooleanOptInputDescription;
import com.dukascopy.api.indicators.ColorListDescription;
import com.dukascopy.api.indicators.IIndicator;
import com.dukascopy.api.indicators.IIndicatorChartPanel;
import com.dukascopy.api.indicators.IIndicatorContext;
import com.dukascopy.api.indicators.IndicatorInfo;
import com.dukascopy.api.indicators.IndicatorResult;
import com.dukascopy.api.indicators.InputParameterInfo;
import com.dukascopy.api.indicators.IntegerListDescription;
import com.dukascopy.api.indicators.OptInputParameterInfo;
import com.dukascopy.api.indicators.OutputParameterInfo;

public class SentimentIndics implements IIndicator{
    //You can change the following settings.----------------------------------------->>>
    //Text Color List.
    private static final Color[] COLORS = {
        Color.blue, Color.green, Color.red, Color.cyan, Color.magenta, Color.yellow, Color.orange,
        Color.pink, new Color(180, 180, 255), new Color(180, 255, 180), Color.blue.darker(), Color.green.darker(), Color.red.darker(),
        Color.black, Color.darkGray, Color.gray, Color.lightGray, Color.white, 
    };
    //Ohlc default settings. setPosX() and setPosY()
    private static final Dimension INFORMER_SIZE = new Dimension(200, 300); //default size of the Informer object.
    private static final OhlcAlignment ALIGNMENT = OhlcAlignment.VERTICAL;// OhlcAlignment.AUTO, OhlcAlignment.HORIZONTAL, or OhlcAlignment.VERTICAL.
    private static final Color FILL_COLOR = null;//fill color of the Informer object.
    private static final float FILL_OPACITY = 0.0f;//fill opacity of the Informer object.
    
    private static final String TIME_PETTERN = "HH:mm:SS dd/MMM";//for the index time text
    private static final String SEPARATOR = " / ";//Separator for the compact mode. it will use like this, (LongValue +  SEPARATOR + TendencyValue + SEPARATTOR + ShortValue).
    private static final int[] OFFSETMINUTES_FOR_HISTORICAL = {30, 360, 1440, 7200};//30=30min, 360=6hour, 1440=1day, 7200=5d=1week ( 5 days will calculating as 1 week).
    private static final String[] OFFSETNAMES_FOR_HISTORICAL = {"30m", "6h", "1d", "1w"};
    //--------------------------------------------------------------------------<<<

    private static final String[] COLORNAMES = new String[COLORS.length];
    static{
        for(int i = COLORS.length; --i>=0;){
            COLORNAMES[i] = COLORS[i].toString().substring(9);
        }
    }
    private enum CurrencyType {
        INSTRUMENT(true, false), PRIMARY(false, true), SECONDLY(false, false);
        private final boolean isPair;
        private final boolean isPrimary;
        private CurrencyType(final boolean isPair, final boolean isPrimary){
            this.isPair = isPair;
            this.isPrimary = isPrimary;
        }
        public boolean isPair(){
            return isPair;
        }
        @SuppressWarnings("unused")
        public boolean isPrimary(){
            return isPrimary;
        }
        public ICurrency getCurrency(Instrument instrument){
            return isPrimary? instrument.getPrimaryJFCurrency(): instrument.getSecondaryJFCurrency();
        }
    };
    private static final CurrencyType[] CTS = CurrencyType.values();
    private static final String[] OPTNAMESCT = new String[CTS.length];
    private static final int[] OPNUMSCT = new int[CTS.length];
    private enum DisplayType {COMPACT, FULL};
    private static final DisplayType[] DTS = DisplayType.values();
    private static final String[] OPTNAMESDT = new String[DTS.length];
    private static final int[] OPNUMSDT = new int[DTS.length];
    static{
        initializeArrays(CTS, OPTNAMESCT, OPNUMSCT, 0);
        initializeArrays(DTS, OPTNAMESDT, OPNUMSDT, 0);
    }
    private static final String[] INSTRUMENT_NAMES;
    private static final int[] INSTRUMENT_NUMS;
    static{
        Instrument[] instruments = Instrument.values();
        int len = instruments.length + 1;
        INSTRUMENT_NAMES = new String[len];
        INSTRUMENT_NUMS = new int[len];
        INSTRUMENT_NAMES[0] = "Chart";
        INSTRUMENT_NUMS[0] = 0;
        initializeArrays(instruments, INSTRUMENT_NAMES, INSTRUMENT_NUMS, 1);
    }
    private IndicatorInfo indicatorInfo;
    private OptInputParameterInfo[] optInputParameterInfos;
    private OutputParameterInfo outputParameterInfo;
    private IIndicatorContext context;
    private IOhlcChartObject cobj = null;
    private CurrencyType selectedCT = CurrencyType.INSTRUMENT;
    private DisplayType selectedDT = DisplayType.FULL;
    private boolean showHistorical = true;
    private Color colorE = Color.black;
    private Color colorP = Color.green;
    private Color colorN = Color.red;
    private Instrument selectedInstrument = null;
    private boolean isAlive = false;
    
    private static void initializeArrays(Enum<?>[] enums, String[] names, int[] nums, int offset){
        for(int i = enums.length; --i >= offset;){
            names[i] = enums[i-offset].name();
            nums[i] = i;
        }
    }
    
    @Override
    public void onStart(IIndicatorContext context) {
        this.context = context;
        indicatorInfo = new IndicatorInfo("SII", "(SWFX) Sentiment Index Informer", "My indicators",
                true, false, false, 0, 7, 1){{
                    setRecalculateAll(false);
                    setRecalculateOnNewCandleOnly(true);}};        
        optInputParameterInfos = new OptInputParameterInfo[] {
                new OptInputParameterInfo("Instrument", OptInputParameterInfo.Type.OTHER, new IntegerListDescription(0, INSTRUMENT_NUMS, INSTRUMENT_NAMES)),
                new OptInputParameterInfo("Target", OptInputParameterInfo.Type.OTHER, new IntegerListDescription(0, OPNUMSCT, OPTNAMESCT)),
                new OptInputParameterInfo("Display", OptInputParameterInfo.Type.OTHER, new IntegerListDescription(1, OPNUMSDT, OPTNAMESDT)),
                new OptInputParameterInfo("Historical", OptInputParameterInfo.Type.OTHER, new BooleanOptInputDescription(true)),
                new OptInputParameterInfo("Color:Positive", OptInputParameterInfo.Type.OTHER, new ColorListDescription(Color.green, COLORS, COLORNAMES)),
                new OptInputParameterInfo("Color:Even", OptInputParameterInfo.Type.OTHER, new ColorListDescription(Color.black, COLORS, COLORNAMES)),
                new OptInputParameterInfo("Color:Negative", OptInputParameterInfo.Type.OTHER, new ColorListDescription(Color.red, COLORS, COLORNAMES)),
                };
        outputParameterInfo = new OutputParameterInfo("Dummy", OutputParameterInfo.Type.OBJECT, OutputParameterInfo.DrawingStyle.NONE);
        setInformer();//basically, chart is null, in this timing. so it's Okay to delete this code, when you were use this indicator on the normal JForex platform.
    }

    @Override
    public IndicatorResult calculate(int startIndex, int endIndex) {
        IndicatorResult dummyResult = new IndicatorResult(startIndex, endIndex - startIndex + 1);
        Instrument instrument;
        if(selectedInstrument == null){
            IFeedDescriptor feed = context.getFeedDescriptor();
            if(feed == null) return dummyResult;
            instrument = feed.getInstrument();
        }else instrument = selectedInstrument;
        if(!isAlive){
            context.getIndicatorChartPanel().add(cobj);
            isAlive = true;
        }
        refreshMessage("Now Updating.");
        IDataService dataservice = context.getDataService();
        String id = "";
        IFXSentimentIndex si;
        if(selectedCT.isPair()){
            id = instrument.name();
            si = dataservice.getFXSentimentIndex(instrument);
        }else{
             ICurrency currency = selectedCT.getCurrency(instrument);
             id = currency.getCurrencyCode();
             si = dataservice.getFXSentimentIndex(currency);
        }
        double longVal = si.getIndexValue();
        long siTime = si.getIndexTime();
        String valL = round(si.getIndexValue(), 2);
        String valT = addPlusSign(si.getIndexTendency());
        String valS = round(100 - longVal, 2);
        double[] valsHist = new double[OFFSETMINUTES_FOR_HISTORICAL.length];
        
        if(showHistorical){
            int len = valsHist.length;
            long[] histTmes = new long[len];
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("EET"));
            cal.setTimeInMillis(si.getIndexTime());
            weekendsOffset(cal, -OFFSETMINUTES_FOR_HISTORICAL[0]);
            histTmes[0] = cal.getTimeInMillis();
            for(int i = len; --i >= 1;){
                weekendsOffset(cal, OFFSETMINUTES_FOR_HISTORICAL[i-1]-OFFSETMINUTES_FOR_HISTORICAL[1]);
                histTmes[i] = cal.getTimeInMillis();
            }
            final String[] ENDPERIODS = {"..", "...", "."};
            if(selectedCT.isPair()){
                for(int i = len; --i >= 0;){
                    refreshMessage("Now Updating" + ENDPERIODS[i%2]);
                    valsHist[i] = longVal - dataservice.getFXSentimentIndex(instrument, histTmes[i]).getIndexValue();
                }
            }else{
                ICurrency currency = selectedCT.getCurrency(instrument);
                for(int i = len; --i >= 0;){
                    refreshMessage("Now Updating" + ENDPERIODS[i%2]);
                    valsHist[i] = longVal - dataservice.getFXSentimentIndex(currency, histTmes[i]).getIndexValue();
                }
            }
        }
        
        cobj.clearUserMessages();
        if(selectedDT == DisplayType.COMPACT){
            String pack1 = valL + SEPARATOR + valT + "%" +  SEPARATOR + valS;
            cobj.addUserMessage(id, pack1, getColor(longVal-50));
            if(showHistorical){
                StringBuffer buf = new StringBuffer();
                int pnTotal = 0;
                for(double vals: valsHist){
                    buf.append(SEPARATOR);
                    buf.append(addPlusSign(vals));
                    if(vals > 0) pnTotal++;
                    else if(vals < 0) pnTotal--;
                }
                cobj.addUserMessage("H", buf.substring(SEPARATOR.length()), getColor(pnTotal));
            }
        }else{
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_PETTERN);
            cobj.addUserMessage(sdf.format(siTime), colorE, SwingConstants.CENTER, true);
            cobj.addUserMessage(id, colorE, SwingConstants.CENTER, true);
            cobj.addUserMessage("Long", valL+"%", colorP);
            cobj.addUserMessage("Tendency", valT+"%", getColor(longVal-50));
            cobj.addUserMessage("Short", valS+"%", colorN);
            if(showHistorical){
                cobj.addUserMessage("Historical", colorE, SwingConstants.CENTER, true);
                for(int i = 0, j = valsHist.length; i < j; i++){
                    cobj.addUserMessage(OFFSETNAMES_FOR_HISTORICAL[i], addPlusSign(valsHist[i])+"%", getColor(valsHist[i]));
                }
            }
        }
        return dummyResult;
    }
    
    private void refreshMessage(String message){
        cobj.clearUserMessages();
        cobj.addUserMessage(message, colorE, SwingConstants.CENTER, true);
    }
    
    private Color getColor(double val){
        if(val == 0) return colorE;
        if(val > 0) return colorP;
        else return colorN;
    }

    private String addPlusSign(double val){
        String str = val >= 0? "+": "";
        return str + round(val, 2);
    }

    private void weekendsOffset(Calendar cal, int minutesOffset){
        //System.out.println("from: " + cal.getTime().toString());
        long dayOffset = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, minutesOffset);
        //System.out.println("to(before weekend offset): " + cal.getTime().toString());
        dayOffset = (cal.getTimeInMillis() - dayOffset) / (1000 * 60 * 60 * 24 * 7 / 2);
        cal.add(Calendar.DAY_OF_YEAR, (int) dayOffset);
        dayOffset = minutesOffset > 0? 2: -2;//skip the Saturday and Sunday
        minutesOffset = cal.get(Calendar.DAY_OF_WEEK);//day of week
        if(minutesOffset == Calendar.SATURDAY || minutesOffset == Calendar.SUNDAY) cal.add(Calendar.DAY_OF_YEAR, (int)dayOffset);
        //System.out.println("to(after weekend offset): " + cal.getTime().toString());
    }
    
    @Override
    public IndicatorInfo getIndicatorInfo() {
        return indicatorInfo;
    }
    
    @Override
    public InputParameterInfo getInputParameterInfo(int index) {
        return null;
    }

    private String round(double value, int scale) throws NumberFormatException{
        if(Double.isNaN(value)) return "";
        BigDecimal big = new BigDecimal("" + value);
        big = big.setScale(scale, BigDecimal.ROUND_HALF_UP); 
        return big.toString();
    }
    
    @Override
    public void setInputParameter(int index, Object array) {
    }

    @Override
    public OptInputParameterInfo getOptInputParameterInfo(int index) {
        if (index <= optInputParameterInfos.length) {
            return optInputParameterInfos[index];
        }
        return null;
    }

    @Override
    public OutputParameterInfo getOutputParameterInfo(int index) {
        return outputParameterInfo;
    }
    
    @Override
    public void setOptInputParameter(int index, Object value) {
        switch (index){
        case 0:
            index = (Integer) value;
            if(index > 0){
                selectedInstrument = Instrument.values()[--index];
            }
            else{
                selectedInstrument = null;
            }
            break;
        case 1:
            CurrencyType ct = CurrencyType.values()[(Integer) value];
            if(selectedCT == ct) return;
            selectedCT = ct;
            break;
        case 2:
            DisplayType dt = DisplayType.values()[(Integer) value];
            if(selectedDT == dt) return;
            selectedDT = dt;
            break;
        case 3:
            if(OFFSETMINUTES_FOR_HISTORICAL.length < 1){
                showHistorical = false;
                break;
            }
            boolean bool = (Boolean) value;
            if(showHistorical == bool) return;
            showHistorical = bool;
            break;
        case 4:
            if(colorP == (Color)value) return;
            colorP = (Color)value;
            break;
        case 5:
            if(colorE == (Color)value) return;
            colorE = (Color)value;
            break;
        case 6:
            if(colorN == (Color)value) return;
            colorN = (Color)value;
            break;
        }
        if(cobj == null) setInformer();
    }

    private void setInformer(){
        //I recommend you to not use setPosX() and setPosY(). Because those method calls a bug which to make a informer invisible.
        IIndicatorChartPanel chart = context.getIndicatorChartPanel();
        if(chart == null) return;
        IChartObjectFactory objf = chart.getChartObjectFactory();
        cobj = objf.createOhlcInformer();
        if(FILL_COLOR != null){
            cobj.setFillColor(FILL_COLOR);//I don't konow why, but setFillColor() couldn't working...
        }
        cobj.setFillOpacity(FILL_OPACITY);
        if(INFORMER_SIZE != null) cobj.setPreferredSize(INFORMER_SIZE);
        IFeedDescriptor feed = context.getFeedDescriptor();
        for(Enum<?> e: cobj.getAllInfoParamsByDataType(feed.getDataType())){
            cobj.setParamVisibility(e, false);
        }
        cobj.setAlignment(ALIGNMENT);
        cobj.setChartObjectListener(new ChartObjectListener() {
            @Override
            public void deleted(ChartObjectEvent e) {
                isAlive = false;
            }
            @Override public void selected(ChartObjectEvent e) {}
            @Override public void moved(ChartObjectEvent e) {}
            @Override public void highlightingRemoved(ChartObjectEvent e) {}
            @Override public void highlighted(ChartObjectEvent e) {}
            @Override public void deselected(ChartObjectEvent e) {}
            @Override public void attrChanged(ChartObjectEvent e) {}
        });
        chart.add(cobj);
        isAlive = true;
    }
    
    @Override
    public void setOutputParameter(int index, Object array) {
    }

    @Override
    public int getLookback() {
        return 0;
    }

    @Override
    public int getLookforward() {
        return 0;
    }
}
