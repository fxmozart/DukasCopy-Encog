package jforex.plugin.ordertable;

import com.dukascopy.api.IOrder;

interface IOrderGet {
    String getValue(IOrder order);
}