package jforex.plugin.ordertable;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.feed.ITickFeedListener;
import com.dukascopy.api.plugins.IMessageListener;
import com.dukascopy.api.plugins.IPluginContext;
import com.dukascopy.api.plugins.Plugin;
import com.dukascopy.api.plugins.widget.IPluginWidget;
import com.dukascopy.api.plugins.widget.PluginWidgetListener;
import com.dukascopy.api.plugins.widget.WidgetProperties;

/**
 * The plugin demonstrates how one can create a customized table
 * which allows the user both to modify orders and attach some customized
 * data to them.
 *
 */
public class OrderTablePlugin extends Plugin implements ITickFeedListener, IMessageListener {
	
	@Configurable("Table tab title")
    public String widgetTitle = "Order Update Table 2.0";
	@Configurable("Deactivate widget on table close")
    public boolean deactivateOnTableClose = true;
	
    private IPluginContext context;
    private OrderTableModel tableModel;
    private JTable table;
    private IEngine engine;
    private IPluginWidget widget;
    
    @Override
    public void onStart(IPluginContext context) throws JFException {
        this.context = context;
        this.engine = context.getEngine();
        context.subscribeToMessages(this);
        context.getConsole().getOut().println(widgetTitle);
        //subscribe to ticks of those instruments that we have orders from - to follow their price movements
        Set<Instrument> instruments = new HashSet<Instrument>();
        for(IOrder o : engine.getOrders()){
        	instruments.add(o.getInstrument());
        }
        for(Instrument instrument : instruments){
            context.subscribeToTicksFeed(instrument, this);
        }

        placeControlsOnTab();
        updateTable();
    }
    

    @Override
    public void onStop() throws JFException {
    	context.removeWidget(widget);
    	context.getConsole().getOut().println("Plugin stop");
    }
    
	@Override
	public void onMessage(IMessage message) throws JFException {
		IOrder order = message.getOrder();
		if(message.getOrder() == null){
			return;
		}
		//this is the first order of this instrument - subscribe to its ticks
		if(message.getType() == IMessage.Type.ORDER_SUBMIT_OK
				&& engine.getOrders(order.getInstrument()).size() == 1){
			context.subscribeToTicksFeed(order.getInstrument(), this);
		}
		context.getConsole().getInfo().println(message);
        try {
			if(engine.getOrders().size() > 0){
			    updateTable();
			}
		} catch (JFException e) {
			e.printStackTrace();
		}
	}
	
    @Override
    public void onTick(Instrument instrument, final ITick tick){
    	try {
			updateTable();
		} catch (JFException e) {
			e.printStackTrace(context.getConsole().getErr());
		}
    }

    private void placeControlsOnTab() {
    	widget = context.addWidget(widgetTitle, WidgetProperties.newInstance().position(SwingConstants.NORTH));
        JPanel mainPanel = widget.getContentPanel();
        mainPanel.setLayout(new BorderLayout());
        tableModel = new OrderTableModel(context);
        table = new JTable(tableModel);
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        widget.addPluginWidgetListener(new PluginWidgetListener(){
        	public void onWidgetClose(){
        		context.getConsole().getOut().println("Widget closed!");
        		if(deactivateOnTableClose){
        			context.stop();
        		}
        	}        	
        });
    }

    private void updateTable() throws JFException {

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        tableModel.setData(engine.getOrders());
                        widget.getContentPanel().validate();
                    } catch (JFException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();            
        }
    }
}
