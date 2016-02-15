package jforex.plugin.ordertable;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;

//function delegates for order rows    
interface IOrderSet{
    void setValue(IOrder order, Object value) throws JFException;
}

