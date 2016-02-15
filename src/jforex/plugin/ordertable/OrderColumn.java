package jforex.plugin.ordertable;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;

//each order column has its name, value representation function and value update function
class OrderColumn {

	private final String name;
	private final boolean editable;
	private final IOrderGet orderGet;
	private final IOrderSet orderSet;

	static OrderColumn newReadOnlyColumn(String name, IOrderGet getValueFunc) {
		return new OrderColumn(name, false, getValueFunc, new IOrderSet() {
			public void setValue(IOrder order, Object value) throws JFException {
			}
		});
	}

	static OrderColumn newEditableColumn(String name, IOrderGet getValueFunc, IOrderSet onChangeFunc) {
		return new OrderColumn(name, true, getValueFunc, onChangeFunc);
	}

	private OrderColumn(String name, boolean editable, IOrderGet getValueFunc, IOrderSet onChangeFunc) {
		this.name = name;
		this.editable = editable;
		this.orderGet = getValueFunc;
		this.orderSet = onChangeFunc;
	}

	public String getName() {
		return name;
	}

	public boolean isEditable() {
		return editable;
	}

	public IOrderGet getOrderGet() {
		return orderGet;
	}

	public IOrderSet getOrderSet() {
		return orderSet;
	}
}