/*
 * Copyright (c) 2009 Dukascopy (Suisse) SA. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * 
 * Neither the name of Dukascopy (Suisse) SA or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. DUKASCOPY (SUISSE) SA ("DUKASCOPY")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL DUKASCOPY OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF DUKASCOPY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package singlejartest;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dukascopy.api.IChart;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterExecutionControl;
import com.dukascopy.api.system.tester.ITesterGui;
import com.dukascopy.api.system.tester.ITesterUserInterface;

/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy in GUI mode
 */
@SuppressWarnings("serial")
public class TesterMainGUIMode extends JFrame implements ITesterUserInterface, ITesterExecution {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesterMainGUIMode.class);

    private final int frameWidth = 1000;
    private final int frameHeight = 600;
    private final int controlPanelHeight = 40;
    
    private JPanel currentChartPanel = null;
    private ITesterExecutionControl executionControl = null;
    
    private JPanel controlPanel = null;
    private JButton startStrategyButton = null;
    private JButton pauseButton = null;
    private JButton continueButton = null;
    private JButton cancelButton = null;
    
	//url of the DEMO jnlp
    private static String jnlpUrl = "http://platform.dukascopy.com/demo/jforex.jnlp";
    //user name
    private static String userName = "username";
    //password
    private static String password = "password";
    
    private Instrument instrument = Instrument.EURUSD;
    
    public TesterMainGUIMode(){
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    }
    
    @Override
    public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
        for(Map.Entry<IChart, ITesterGui> entry : chartPanels.entrySet()){
            IChart chart = entry.getKey();
            JPanel chartPanel = entry.getValue().getChartPanel();
            if(chart.getFeedDescriptor().getInstrument().equals(instrument)){
                setTitle(chart.getFeedDescriptor().toString());   
                addChartPanel(chartPanel);
                break;
            }
        }
    }

	@Override
	public void setExecutionControl(ITesterExecutionControl executionControl) {
		this.executionControl = executionControl;
	}
	
    public void startStrategy() throws Exception {
        //get the instance of the IClient interface
        final ITesterClient client = TesterFactory.getDefaultInstance();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
                updateButtons();
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                resetButtons();
                
                File reportFile = new File("C:\\report.html");
                try {
                    client.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (client.getStartedStrategies().size() == 0) {
                    //Do nothing
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

        LOGGER.info("Connecting...");
        //connect to the server using jnlp, user name and password
        //connection is needed for data downloading
        client.connect(jnlpUrl, userName, password);

        //wait for it to connect
        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }

        //set instruments that will be used in testing
        final Set<Instrument> instruments = new HashSet<>();
        instruments.add(instrument);
        
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
        //setting initial deposit
        client.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 50000);
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = client.downloadData(null);
        //wait for downloading to complete
        future.get();
        //start the strategy
        LOGGER.info("Starting strategy");

        client.startStrategy(
    		new MA_Play(),
    		new LoadingProgressListener() {
    			@Override
    			public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
    				LOGGER.info(information);
    			}

    			@Override
    			public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
    			}

    			@Override
    			public boolean stopJob() {
    				return false;
    			}
    		}, this, this
        );
        //now it's running
    }
    
	/**
	 * Center a frame on the screen 
	 */
	private void centerFrame(){
		Toolkit tk = Toolkit.getDefaultToolkit();
	    Dimension screenSize = tk.getScreenSize();
	    int screenHeight = screenSize.height;
	    int screenWidth = screenSize.width;
	    setSize(screenWidth / 2, screenHeight / 2);
	    setLocation(screenWidth / 4, screenHeight / 4);
	}
	
	/**
	 * Add chart panel to the frame
	 */
	private void addChartPanel(JPanel chartPanel) {
		removecurrentChartPanel();
		
		this.currentChartPanel = chartPanel;
		chartPanel.setPreferredSize(new Dimension(frameWidth, frameHeight - controlPanelHeight));
		chartPanel.setMinimumSize(new Dimension(frameWidth, 200));
		chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		getContentPane().add(chartPanel);
		this.validate();
		chartPanel.repaint();
	}

	/**
	 * Add buttons to start/pause/continue/cancel actions 
	 */
	private void addControlPanel() {
		controlPanel = new JPanel();
		FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
		controlPanel.setLayout(flowLayout);
		controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
		controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
		controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelHeight));

		startStrategyButton = new JButton("Start strategy");
		startStrategyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startStrategyButton.setEnabled(false);
				Runnable r = new Runnable() {
					public void run() {
						try {
							startStrategy();
						} catch (Exception e2) {
							LOGGER.error(e2.getMessage(), e2);
							e2.printStackTrace();
							resetButtons();
						}
					}
				};
				Thread t = new Thread(r);
				t.start();
			}
		});
		
		pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(executionControl != null){
					executionControl.pauseExecution();
					updateButtons();
				}
			}
		});
		
		continueButton = new JButton("Continue");
		continueButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(executionControl != null){
					executionControl.continueExecution();
					updateButtons();
				}
			}
		});
		
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(executionControl != null){
					executionControl.cancelExecution();
					updateButtons();
				}
			}
		});
		
		controlPanel.add(startStrategyButton);
		controlPanel.add(pauseButton);
		controlPanel.add(continueButton);
		controlPanel.add(cancelButton);
		getContentPane().add(controlPanel);
		
		pauseButton.setEnabled(false);
		continueButton.setEnabled(false);
		cancelButton.setEnabled(false);
	}

	private void updateButtons(){
		if(executionControl != null){
			startStrategyButton.setEnabled(executionControl.isExecutionCanceled());
			pauseButton.setEnabled(!executionControl.isExecutionPaused() && !executionControl.isExecutionCanceled());
			cancelButton.setEnabled(!executionControl.isExecutionCanceled());
			continueButton.setEnabled(executionControl.isExecutionPaused());
		}
	}

	private void resetButtons(){
        startStrategyButton.setEnabled(true);
        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
	}
	
    private void removecurrentChartPanel(){
		if(this.currentChartPanel != null){
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						TesterMainGUIMode.this.getContentPane().remove(TesterMainGUIMode.this.currentChartPanel);
						TesterMainGUIMode.this.getContentPane().repaint();
					}
				});				
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
    }

	public void showChartFrame(){
		setSize(frameWidth, frameHeight);
		centerFrame();
		addControlPanel();
		setVisible(true);
	}
    
    public static void main(String[] args) throws Exception {
    	TesterMainGUIMode testerMainGUI = new TesterMainGUIMode();
    	testerMainGUI.showChartFrame();
    }
}