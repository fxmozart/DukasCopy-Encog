package jforex.plugin.strategies;

import com.dukascopy.api.JFException;
import com.dukascopy.api.strategy.IStrategyDescriptor;

//function delegates for order rows    
interface ISet{
    void setValue(IStrategyDescriptor descriptor, Object value) throws JFException;
}

