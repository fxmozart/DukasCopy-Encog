package jforex.plugin.ordertable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.table.AbstractTableModel;

import com.dukascopy.api.IContext;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;

@SuppressWarnings("serial")
class OrderTableModel extends AbstractTableModel {
    
    private Map<IOrder, String> orderNotes = new HashMap<IOrder, String>();
    
    private final IContext context;
    
    public OrderTableModel(IContext context){
    	this.context = context;
    }
    
    private static double round(double amount, int decimalPlaces) {
        return (new BigDecimal(amount)).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public double getLastPrice(Instrument instrument, boolean isLong){
        try {
            ITick tick = context.getHistory().getLastTick(instrument);
            return isLong ? tick.getAsk() : tick.getBid(); 
        } catch (JFException e) {
            e.printStackTrace();
        }
        return 0d;
    }
    
    //define 6 order columns and their behavior
    private final OrderColumn[] orderColumns = new OrderColumn[]{
         OrderColumn.newReadOnlyColumn(
            "Order label",               
            new IOrderGet() {
                @Override
                public String getValue(IOrder order) {
                    return order.getLabel();
                }
            }), 
         OrderColumn.newReadOnlyColumn(
        	"Instrument",               
        	new IOrderGet() {
        		@Override
                public String getValue(IOrder order) {
                	return order.getInstrument().toString();
                }
         }),
         OrderColumn.newEditableColumn(
            "Stop Loss",
            new IOrderGet() {
                @Override
                public String getValue(IOrder order) {
                    return order.getStopLossPrice() == 0 ? "-" : String.format("%.6f",order.getStopLossPrice());
                }
            }, 
            new IOrderSet() {
                @Override
                public void setValue(IOrder order, Object value) throws JFException{
                    order.setStopLossPrice(round(Double.valueOf(value.toString()),order.getInstrument().getPipScale() + 1));
                }
            }
        ), 
        OrderColumn.newEditableColumn(
            "Take Profit",
            new IOrderGet() {
                @Override
                public String getValue(IOrder order) {
                    return order.getTakeProfitPrice() == 0 ? "-" : String.format("%.6f",order.getTakeProfitPrice());
                }
            }, 
            new IOrderSet() {
                @Override
                public void setValue(IOrder order, Object value) throws JFException{
                    order.setTakeProfitPrice(round(Double.valueOf(value.toString()),order.getInstrument().getPipScale() + 1));
                }
            }
        ), 
        OrderColumn.newEditableColumn(
            "SL distance in pips",
            new IOrderGet() {
                @Override
                public String getValue(IOrder order) {
                    if(order.getStopLossPrice() == 0){
                        return "-";
                    }
                    double lastPrice = getLastPrice(order.getInstrument(), order.isLong());
                    return String.format("%.1f",Math.abs( lastPrice - order.getStopLossPrice()) / order.getInstrument().getPipValue());
                }
            }, 
            new IOrderSet() {
                @Override
                public void setValue(IOrder order, Object value) throws JFException{
                    double lastPrice = getLastPrice(order.getInstrument(), order.isLong());
                    double priceDelta = Double.valueOf(value.toString()) * order.getInstrument().getPipValue();
                    double price = order.isLong() ? lastPrice - priceDelta : lastPrice + priceDelta;
                    order.setStopLossPrice(round(price,order.getInstrument().getPipScale() + 1));
                }
            }
        ), 
        OrderColumn.newEditableColumn(
            "TP distance in pips", 
            new IOrderGet() {
                @Override
                public String getValue(IOrder order) {
                    if(order.getTakeProfitPrice() == 0){
                        return "-";
                    }
                    double lastPrice = getLastPrice(order.getInstrument(), order.isLong());
                    return String.format("%.1f", Math.abs(lastPrice - order.getTakeProfitPrice()) / order.getInstrument().getPipValue());
                }
            }, 
            new IOrderSet() {  
                @Override
                public void setValue(IOrder order, Object value) throws JFException{
                    double lastPrice = getLastPrice(order.getInstrument(), order.isLong());
                    double priceDelta = Double.valueOf(value.toString()) * order.getInstrument().getPipValue();
                    double price = order.isLong() ? lastPrice + priceDelta : lastPrice - priceDelta;
                    order.setTakeProfitPrice(round(price,order.getInstrument().getPipScale() + 1));
                
                }
            }
       ), 
       OrderColumn.newEditableColumn(
               "Order notes", 
               new IOrderGet() {
                   @Override
                   public String getValue(IOrder order) {
                       String note = orderNotes.get(order);
                       return note == null ? "" : note;
                   }
               }, 
               new IOrderSet() {  
                   @Override
                   public void setValue(IOrder order, Object value) throws JFException{
                       orderNotes.put(order, value.toString());
                   }
               }
          )            
    };
    
	private List<IOrder> orders = new ArrayList<IOrder>();

	public void setData(List<IOrder> orders) {
		this.orders = orders;
		fireTableDataChanged();
	}

	public int getRowCount() {
		return orders.size();
	}

	public int getColumnCount() {
		return orderColumns.length;
	}

	public Object getValueAt(int row, int column) {
		IOrder order = orders.get(row);
		return orderColumns[column].getOrderGet().getValue(order);
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return orderColumns[columnIndex].isEditable();
	}

	public void setValueAt(final Object aValue, int rowIndex, final int columnIndex) {
		final IOrder order = orders.get(rowIndex);
		// order operations need to be executed from the strategy thread
		context.executeTask(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				try {
					orderColumns[columnIndex].getOrderSet().setValue(order, aValue);
				} catch (JFException e) {
					context.getConsole().getErr().println(e);
				}
				return null;
			}
		});
	}

	public String getColumnName(int column) {
		return orderColumns[column].getName();
	}
}

