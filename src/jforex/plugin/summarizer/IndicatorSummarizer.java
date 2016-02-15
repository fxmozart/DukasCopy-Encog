package jforex.plugin.summarizer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.DataType;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.util.TimePeriodAggregationFeedDescriptor;
import com.dukascopy.api.indicators.IIndicatorAppearanceInfo;
import com.dukascopy.api.plugins.IPluginContext;
import com.dukascopy.api.plugins.Plugin;
import com.dukascopy.api.util.DateUtils;

/**
 * The plugin summarizes the visible indicators on the chart by printing
 * their values, reversed values, minimums and maximums
 *
 */
public class IndicatorSummarizer extends Plugin {
	
	@Configurable("calculatable element count")
	public int count = 10;
		
	private IConsole console;
	private IChart chart;
	private IHistory history;
	private IIndicators indicators;
	
	@Override
	public void onStart(IPluginContext context) throws JFException {
		console = context.getConsole();
        chart = context.getLastActiveChart();
        indicators = context.getIndicators();
        history = context.getHistory();
        
        if (chart == null) {
            context.getConsole().getErr().println("No chart opened!");
            context.stop();
        }
        
        if(chart.getIndicatorApperanceInfos().size() == 0){
        	console.getWarn().println("There are no indicators on the chart, nothing to summarize!");
        	context.stop();
        }
        
        IFeedDescriptor feedDescriptor = chart.getFeedDescriptor().getDataType() == DataType.TICKS
                ? new TimePeriodAggregationFeedDescriptor(chart.getFeedDescriptor().getInstrument(), Period.ONE_SEC, chart.getSelectedOfferSide())
                : chart.getFeedDescriptor();
        long lastElTime = history.getFeedData(feedDescriptor, 0).getTime();
        for(IIndicatorAppearanceInfo info : chart.getIndicatorApperanceInfos()){
			Object[] result = indicators.calculateIndicator(feedDescriptor, info.getOfferSidesForTicks(), info.getName(), info.getAppliedPricesForCandles(), info
					.getOptParams(), count, lastElTime, 0);
			int index = 0;
			for(Object arr : result){
				if(!(arr instanceof double[])){
					index++; continue; 
				}
				double[] indResult = (double[])arr;
				double[] indResultReversed = ArrayUtils.clone(indResult);
				ArrayUtils.reverse(indResultReversed);
				double min = NumberUtils.min(indResult);
				double max = NumberUtils.max(indResult);
				long minTime = history.getFeedData(feedDescriptor, ArrayUtils.indexOf(indResultReversed, min)).getTime();
				long maxTime = history.getFeedData(feedDescriptor, ArrayUtils.indexOf(indResultReversed, max)).getTime();
				console.getOut().format("%s \"%s\" for the last %s feed elements \nasc: %s \ndesc:%s " +
						"\nmax=%.7f at %s min=%.7f at %s \n_____",
						info.getName(),info.getIndicator().getOutputParameterInfo(index).getName(), indResult.length, toString(indResult), toString(indResultReversed), 
						max, DateUtils.format(maxTime), min, DateUtils.format(minTime)).println();
				index++;
			}
        }
        context.stop();
	}
	
	private static String toString(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < arr.length; r++) {
            sb.append(String.format("[%s] %.7f; ",r, arr[r]));
        }
        return sb.toString();
    }

	@Override
	public void onStop() throws JFException {}


}
