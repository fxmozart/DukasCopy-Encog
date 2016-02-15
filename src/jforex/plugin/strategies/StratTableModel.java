package jforex.plugin.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import com.dukascopy.api.IContext;
import com.dukascopy.api.JFException;
import com.dukascopy.api.strategy.IStrategyDescriptor;
import com.dukascopy.api.strategy.local.ILocalStrategyDescriptor;
import com.dukascopy.api.util.DateUtils;

@SuppressWarnings("serial")
class StratTableModel extends AbstractTableModel {
        
    private final IContext context;
    private Set<IStrategyDescriptor> stopped = new HashSet<IStrategyDescriptor>();
    
    public StratTableModel(IContext context){
    	this.context = context;  
    }    
    
    private final Column[] columns = new Column[]{
         Column.newReadOnlyColumn(
            "Name",               
            new IGet() {
                @Override
                public String getValue(IStrategyDescriptor order) {
                    return order.getName();
                }
            }), 
         Column.newReadOnlyColumn(
        	"Start time",               
        	new IGet() {
        		@Override
                public String getValue(IStrategyDescriptor order) {
                	return DateUtils.format(order.getStartTime());
                }
         }),
         Column.newReadOnlyColumn(
        	"Params",               
        	new IGet() {
        		@Override
                public String getValue(IStrategyDescriptor order) {
                	return order.getParameters().toString();
                }
         }), 
         Column.newReadOnlyColumn(
        	"Mode",               
        	new IGet() {
        		@Override
                public String getValue(IStrategyDescriptor order) {
                	return order instanceof ILocalStrategyDescriptor ? "LOCAL" : "REMOTE";
                }
         }), 
    };
    
	private List<IStrategyDescriptor> strategyDescriptors = new ArrayList<IStrategyDescriptor>();

	public void resetData(Set<IStrategyDescriptor> strats) {		
		List<IStrategyDescriptor> stratList =new ArrayList<IStrategyDescriptor>(strats);
		Collections.sort(new ArrayList<IStrategyDescriptor>(strats), new Comparator<IStrategyDescriptor>(){
			@Override
			public int compare(IStrategyDescriptor o1, IStrategyDescriptor o2) {				
				return o1.getName().compareTo(o2.getName()) != 0 
							? o1.getName().compareTo(o2.getName())
							: Long.compare(o1.getStartTime(), o2.getStartTime());
			}});
		this.strategyDescriptors = stratList;
		stopped.clear();
		fireTableDataChanged();
	}
	
	public void addStrategy(IStrategyDescriptor strategyDescriptor) {
		this.strategyDescriptors.add(strategyDescriptor);
		stopped.remove(strategyDescriptor);
		fireTableDataChanged();
	}
	
	public void setStopped(IStrategyDescriptor strategyDescriptor) {
		stopped.add(strategyDescriptor);
		fireTableDataChanged();
	}
	
	public void removeStrategy(IStrategyDescriptor lastSelected) {
		strategyDescriptors.remove(lastSelected);
		fireTableDataChanged();
	}
	
	public IStrategyDescriptor getStrategy(int rowNr){
		if(rowNr >= strategyDescriptors.size()){
			return null;
		}
		return strategyDescriptors.get(rowNr);
	}
	
	public boolean isStopped(int rowNr){
		return stopped.contains(strategyDescriptors.get(rowNr));
	}
	
	public boolean isStopped(IStrategyDescriptor strat){
		return stopped.contains(strat);
	}

	public int getRowCount() {
		return strategyDescriptors.size();
	}

	public int getColumnCount() {
		return columns.length;
	}

	public Object getValueAt(int row, int column) {
		IStrategyDescriptor strat = strategyDescriptors.get(row);
		return columns[column].get().getValue(strat);
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columns[columnIndex].isEditable();
	}

	public void setValueAt(final Object aValue, int rowIndex, final int columnIndex) {
		IStrategyDescriptor strat = strategyDescriptors.get(rowIndex);
		try {
			columns[columnIndex].set().setValue(strat, aValue);
		} catch (Exception e) {
			context.getConsole().getErr().format("Could not set value %s to [%s;%s] - %s", aValue, rowIndex, columnIndex, e).println();
		}
	}

	public String getColumnName(int column) {
		return columns[column].getName();
	}



}

