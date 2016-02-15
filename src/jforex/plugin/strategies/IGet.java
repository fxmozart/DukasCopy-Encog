package jforex.plugin.strategies;

import com.dukascopy.api.strategy.IStrategyDescriptor;

interface IGet {
    String getValue(IStrategyDescriptor descriptor);
}