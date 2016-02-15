package jforex.plugin.strategies;

import java.io.File;

import com.dukascopy.api.strategy.IStrategyDescriptor;

public abstract class StrategyAction implements Runnable {
	
	protected final IStrategyDescriptor descriptor;
	protected File file;

	public StrategyAction(IStrategyDescriptor descriptor){
		this.descriptor = descriptor;
	}
	
	public StrategyAction(IStrategyDescriptor descriptor, File file){
		this.descriptor = descriptor;
		this.file = file;
	}
	
}
