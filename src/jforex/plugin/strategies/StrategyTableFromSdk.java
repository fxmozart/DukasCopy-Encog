package jforex.plugin.strategies;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JPanel;


import com.dukascopy.api.Instrument;
import com.dukascopy.api.plugins.PluginGuiListener;
import com.dukascopy.api.plugins.widget.IPluginWidget;
import com.dukascopy.api.plugins.widget.PluginWidgetListener;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;

public class StrategyTableFromSdk {

    //url of the DEMO jnlp
    private static String jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp";
    //user name
    private static String userName = "username";
    //password
    private static String password = "password";
	
    private static JFrame frame;
    private static UUID pluginId =null;
    
	public static void main(String[] args) throws Exception {
		
		final IClient client = ClientFactory.getDefaultInstance();
		client.connect(jnlpUrl, userName, password);
		
        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            System.err.println("Failed to connect Dukascopy servers");
            System.exit(1);
        }
        Set<Instrument> instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        System.out.println("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        
        pluginId = client.runPlugin(new StratTablePlugin(), null, new PluginGuiListener(){

			@Override
			public void onWidgetAdd(IPluginWidget pluginWidget) {
				frame = new JFrame("Strategy table");
				Toolkit tk = Toolkit.getDefaultToolkit();
			    Dimension screenSize = tk.getScreenSize();
			    int screenHeight = screenSize.height;
			    int screenWidth = screenSize.width;
			    frame.setSize(screenWidth / 2, screenHeight / 2);
			    frame.setLocation(screenWidth / 4, screenHeight / 4);
				
				JPanel panel = pluginWidget.getContentPanel();
				panel.setMinimumSize(new Dimension(600,100));
				panel.setPreferredSize(new Dimension(600,100));
				frame.add(panel);
				frame.pack();
				frame.setVisible(true);
			}
			
			@Override
			public void onWidgetListenerAdd(final PluginWidgetListener listener){
				frame.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosing(WindowEvent e) {
						listener.onWidgetClose();
						client.stopPlugin(pluginId);
						System.exit(0);
					}
				});
			}

        });
	}

}

