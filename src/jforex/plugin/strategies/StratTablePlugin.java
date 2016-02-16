package jforex.plugin.strategies;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.JFException;
import com.dukascopy.api.plugins.IPluginContext;
import com.dukascopy.api.plugins.Plugin;
import com.dukascopy.api.plugins.widget.IPluginWidget;
import com.dukascopy.api.plugins.widget.PluginWidgetListener;
import com.dukascopy.api.plugins.widget.WidgetProperties;
import com.dukascopy.api.strategy.IStrategyDescriptor;
import com.dukascopy.api.strategy.IStrategyManager;
import com.dukascopy.api.strategy.IStrategyParameter;
import com.dukascopy.api.strategy.IStrategyResponse;
import com.dukascopy.api.strategy.local.ILocalStrategyDescriptor;
import com.dukascopy.api.strategy.local.ILocalStrategyManager;
import com.dukascopy.api.strategy.local.LocalStrategyListener;
import com.dukascopy.api.strategy.remote.IRemoteStrategyDescriptor;
import com.dukascopy.api.strategy.remote.IRemoteStrategyManager;
import com.dukascopy.api.strategy.remote.RemoteStrategyListener;

/**
 * The plugin demonstrates how one can create a customized table which allows
 * the user both to modify orders and attach some customized data to them.
 * 
 */
public class StratTablePlugin extends Plugin {

	@Configurable("Table tab title")
	public String widgetTitle = "Strategy table";
	@Configurable("Deactivate widget on table close") 
	public boolean deactivateOnTableClose = true;
	
	private static final SimpleDateFormat SEC_FORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private IPluginContext context;
	private StratTableModel tableModel;
	private JTable table;
	private IPluginWidget widget;

	private ILocalStrategyManager localStrategyManager;
	private IRemoteStrategyManager remoteStrategyManager;
	private IConsole console;
	private IStrategyDescriptor selected;
	
	//We execute strategy actions in an executor to be able to process the responses 
	//without delaying neither the plugin execution nor the GUI thread.
	//Note that remote actions take much longer time to execute, for extensive remote action
	//processing by using Future.get(), consider increasing the pool size.
	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	@Override
	public void onStart(IPluginContext context) throws JFException {
		this.context = context;
		this.console = context.getConsole();  
		this.localStrategyManager = context.getLocalStrategyManager();
		this.remoteStrategyManager = context.getRemoteStrategyManager();
		
		
		localStrategyManager.addStrategyListener(new LocalStrategyListener() {

			@Override
			public void onStrategyRun(ILocalStrategyDescriptor localStrategyDescriptor) {
				StratTablePlugin.this.onStrategyRun(localStrategyDescriptor, false);
			};

			@Override
			public void onStrategyStop(ILocalStrategyDescriptor localStrategyDescriptor) {
				StratTablePlugin.this.onStrategyStop(localStrategyDescriptor, false);
			};

		});
		
		remoteStrategyManager.addStrategyListener(new RemoteStrategyListener() {

			@Override
			public void onStrategyRun(IRemoteStrategyDescriptor remoteStrategyDescriptor) {
				StratTablePlugin.this.onStrategyRun(remoteStrategyDescriptor, true);
			};

			@Override
			public void onStrategyStop(IRemoteStrategyDescriptor remoteStrategyDescriptor) {
				StratTablePlugin.this.onStrategyStop(remoteStrategyDescriptor, true);
			};

		});
		

		placeControlsOnTab();

		executor.execute(new InitTableAction());
	}
	
	public void onStrategyRun(final IStrategyDescriptor strategyDescriptor, boolean isRemote) {
		(isRemote ? console.getNotif() : console.getInfo())
			.format("%s start %s", isRemote ? "remote" : "local", strategyDescriptor).println();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tableModel.addStrategy(strategyDescriptor);
				widget.getContentPanel().validate();
			}
		});
	};

	public void onStrategyStop(final IStrategyDescriptor strategyDescriptor, boolean isRemote) {
		(isRemote ? console.getNotif() : console.getInfo())
			.format("%s stop %s", isRemote ? "remote" : "local", strategyDescriptor).println();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				tableModel.setStopped(strategyDescriptor);
				widget.getContentPanel().validate();
			}
		});
	};

	@Override
	public void onStop() throws JFException {
		context.removeWidget(widget);
		context.getConsole().getOut().println("Plugin stop");
	}
	
	private IStrategyManager<?,?> getManager(IStrategyDescriptor descriptor){		
		return descriptor instanceof ILocalStrategyDescriptor 
				? localStrategyManager 
				: remoteStrategyManager;		
	}	

	private void placeControlsOnTab() {
		widget = context.addWidget(widgetTitle, WidgetProperties.newInstance().position(SwingConstants.NORTH));
		JPanel mainPanel = widget.getContentPanel();
		mainPanel.setLayout(new BorderLayout());

		tableModel = new StratTableModel(context);
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
		    @Override
		    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		        c.setBackground(tableModel.isStopped(row)  
		        		? (isSelected ? Color.DARK_GRAY : Color.LIGHT_GRAY)   
		        		: (isSelected ? Color.BLUE.darker().darker() : Color.WHITE));
		        return c;
		    }
		});
		
        final JPopupMenu popup = createPopup();

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int rowNumber = table.rowAtPoint(e.getPoint());
					table.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
					selected = tableModel.getStrategy(rowNumber);
				}
				if (selected != null && e.isPopupTrigger() && e.getComponent() instanceof JTable) {
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		
		mainPanel.add(createToolbar(), BorderLayout.NORTH);
		mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		widget.addPluginWidgetListener(new PluginWidgetListener() {
			public void onWidgetClose() {
				context.getConsole().getOut().println("Widget closed!");
				if (deactivateOnTableClose) {
					context.stop();
				}
			}
		});
	}
	
	private JPopupMenu createPopup(){
		JPopupMenu popup = new JPopupMenu();
        final JMenuItem itemStartLocal = new JMenuItem("Local Run");
        final JMenuItem itemStartRemote = new JMenuItem("Remote Run");
        final JMenuItem itemStop = new JMenuItem("Stop");
        final JMenuItem itemCompile = new JMenuItem("Recompile");
        
        itemStartLocal.addActionListener(new ExecutorActionListener(new StartSelectedAction(localStrategyManager)));   
        itemStartRemote.addActionListener(new ExecutorActionListener(new StartSelectedAction(remoteStrategyManager)));  
        itemStop.addActionListener(new ExecutorActionListener(new StopAction()));
        itemCompile.addActionListener(new ExecutorActionListener(new RecompileAction()));  


        popup.add(itemStartLocal);
        popup.add(itemStartRemote);
        popup.add(itemStop);
        popup.add(itemCompile);
        popup.addPopupMenuListener(new PopupMenuListener(){

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				boolean isStoppedLocalStrategy = selected != null && tableModel.isStopped(selected) 
						&& selected instanceof ILocalStrategyDescriptor && ((ILocalStrategyDescriptor)selected).getFile() != null;
	        	itemStartLocal.setEnabled(isStoppedLocalStrategy);
	        	itemStartRemote.setEnabled(isStoppedLocalStrategy);
	        	itemCompile.setEnabled(isStoppedLocalStrategy);
	        	itemStop.setEnabled(selected != null && !tableModel.isStopped(selected));
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}
			
		});
        return popup;
	}
	
	
	private JPanel createToolbar(){
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton btnReset = new JButton("Reset");
		btnReset.addActionListener(new ExecutorActionListener(new InitTableAction()));
		JButton btnStartOut = new JButton("Local Open and start");
		btnStartOut.addActionListener(new ExecutorActionListener(new OpenAndStartAction(localStrategyManager)));
		JButton btnStartOutRemote = new JButton("Remote Open and start");
		btnStartOutRemote.addActionListener(new ExecutorActionListener(new OpenAndStartAction(remoteStrategyManager)));

		toolbar.add(btnReset);
		toolbar.add(btnStartOut);
		toolbar.add(btnStartOutRemote);
		return toolbar;
	}
	
	private class ExecutorActionListener implements ActionListener{
		
		final Runnable runnable;
		
		ExecutorActionListener(Runnable runnable){
			this.runnable = runnable;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			executor.execute(runnable);
		}
		
	}

	private class InitTableAction implements Runnable {

		public void run() {
			try {
				final Set<IStrategyDescriptor> strats = (Set) localStrategyManager.getStartedStrategies().get().getResult();
				strats.addAll((Set) remoteStrategyManager.getStartedStrategies().get().getResult());

				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							tableModel.resetData(strats);
							widget.getContentPanel().validate();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace(console.getErr());
			}
		}
	}

	
	private class StartAction implements Runnable{
		
		final File file;
		final Object[] params;
		final IStrategyManager<?,?> strategyManager;

		StartAction(IStrategyManager<?,?> strategyManager, File file, Object[] params){
			this.file = file;
			this.params = params;
			this.strategyManager = strategyManager;
		}
		
		public void run() {
			try {
                            /*				IStrategyResponse<UUID> startResponse = strategyManager.startStrategy(file, params).get();
				if(startResponse.isError()){
					console.getErr().println(file.getName() + " strategy start failed: " + startResponse.getErrorMessage());
					return;
				}
*/
			} catch (Exception e1) {
				console.getErr().println(e1);
				e1.printStackTrace(console.getErr());
			}
		}	
	}
	
	private class OpenAndStartAction implements Runnable {

		final IStrategyManager<?,?> strategyManager;

		OpenAndStartAction(IStrategyManager<?,?> strategyManager){
			this.strategyManager = strategyManager;
		}
		
		
		public void run() {

			final JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileFilter() {

				@Override
				public boolean accept(File f) {
					return f.getAbsolutePath().endsWith(".jfx");
				}

				@Override
				public String getDescription() {
					return "Choose a copliled strategy .jfx file";
				}
			});
			int result = fc.showOpenDialog(null);
			if (result != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File file = fc.getSelectedFile();
			new StartAction(strategyManager, file, getModifiedParams(file)).run();
		}
	}
	
	private class StartSelectedAction implements Runnable {
		
		final IStrategyManager<?,?> strategyManager;

		StartSelectedAction(IStrategyManager<?,?> strategyManager){
			this.strategyManager = strategyManager;
		}

		public void run() {
			File file = ((ILocalStrategyDescriptor)selected).getFile();
			new StartAction(strategyManager, file, getModifiedParams(file)).run();
		}
	}
	
	private class StopAction implements Runnable{
		
		public void run() {
			try {
				IStrategyResponse<Void> startResponse = getManager(selected).stopStrategy(selected.getId()).get();
				if(startResponse.isError()){
					console.getErr().println(selected.getName() + " strategy stop failed: " + startResponse.getErrorMessage());
					return;
				}		
			} catch (Exception e1) {
				console.getErr().println(e1);
				e1.printStackTrace(console.getErr());
			}
		}	
	}
	
	private class RecompileAction implements Runnable{
		
		public void run() {
			try {
				File file = ((ILocalStrategyDescriptor)selected).getFile();
				File srcFile = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(".jfx")) + ".java");
				if(!srcFile.exists()){
					console.getWarn().println("can't compile - file not found - " + srcFile);
					return;
				}
				localStrategyManager.compileStrategy(srcFile);
			} catch (Exception e1) {
				console.getErr().println(e1);
				e1.printStackTrace(console.getErr());
			}
		}	
	}
	
	//we modify the strategy parameters to our own if they are of int, double or String type
	//consider here implementing some more meaningful parameter processing.
	private Object[] getModifiedParams(File file) {
		try {
			List<IStrategyParameter> defaultParams = localStrategyManager.getDefaultParameters(file);
			Object [] params = new Object[defaultParams.size()];
			int i = 0;
			for(IStrategyParameter p : defaultParams){
				Class<?> type = p.getType();
				params[i++] = type.equals(int.class) || type.equals(Integer.class) ? 42 
						: type.equals(double.class) || type.equals(Double.class) ? 42d
						: type.equals(String.class) ? "str_" + SEC_FORMAT.format(System.currentTimeMillis())
						: p.getValue();
			}
			return params;
		} catch (Exception e) {
			e.printStackTrace(console.getErr());
		} 
		return null;
	}

}
