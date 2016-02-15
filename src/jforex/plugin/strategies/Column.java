package jforex.plugin.strategies;

import com.dukascopy.api.JFException;
import com.dukascopy.api.strategy.IStrategyDescriptor;

//each order column has its name, value representation function and value update function
class Column {

	private final String name;
	private final boolean editable;
	private final IGet get;
	private final ISet set;

	static Column newReadOnlyColumn(String name, IGet getValueFunc) {
		return new Column(name, false, getValueFunc, new ISet() {
			public void setValue(IStrategyDescriptor order, Object value) throws JFException {
			}
		});
	}

	static Column newEditableColumn(String name, IGet getValueFunc, ISet onChangeFunc) {
		return new Column(name, true, getValueFunc, onChangeFunc);
	}

	private Column(String name, boolean editable, IGet getValueFunc, ISet onChangeFunc) {
		this.name = name;
		this.editable = editable;
		this.get = getValueFunc;
		this.set = onChangeFunc;
	}

	public String getName() {
		return name;
	}

	public boolean isEditable() {
		return editable;
	}

	public IGet get() {
		return get;
	}

	public ISet set() {
		return set;
	}
}