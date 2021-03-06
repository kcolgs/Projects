package xyz.ablue.wscaller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.xml.namespace.QName;
import javax.xml.rpc.encoding.Deserializer;
import javax.xml.rpc.encoding.DeserializerFactory;

import org.apache.axis.Constants;
import org.apache.axis.client.Call;
import org.apache.axis.encoding.ser.SimpleDeserializer;
import org.apache.axis.wsdl.gen.Parser;
import org.apache.axis.wsdl.symbolTable.BaseType;
import org.apache.axis.wsdl.symbolTable.BindingEntry;
import org.apache.axis.wsdl.symbolTable.Parameter;
import org.apache.axis.wsdl.symbolTable.Parameters;
import org.apache.axis.wsdl.symbolTable.ServiceEntry;
import org.apache.axis.wsdl.symbolTable.SymTabEntry;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.axis.wsdl.symbolTable.TypeEntry;

/**
 * Web Service Dynamic Invoker
 * 
 * @version 20170904
 */
public class DynamicInvoker {
	private Parser wsdlParser = null;
	private Map<String, SymTabEntry> services = null;

	public DynamicInvoker(String wsdlURL) throws Exception {
		wsdlParser = new Parser();
		try {
			wsdlParser.run(wsdlURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		services = enumSymTabEntry(ServiceEntry.class);
	}

	public Map<String, Object> invoke(String serviceName, String portName, String operationName,
			Vector<?> parameterValues) throws Exception {
		Vector<Object> inputs = new Vector<Object>();
		String returnName = null;

		ServiceEntry serviceEntry = (ServiceEntry) services.get(serviceName);
		Service service = serviceEntry.getService();
		org.apache.axis.client.Service clientService = new org.apache.axis.client.Service(wsdlParser,
				service.getQName());

		Call call = (Call) clientService.createCall(QName.valueOf(portName), QName.valueOf(operationName));
		call.setTimeout(new Integer(15 * 1000));

		BindingEntry bindingEntry = getBindingEntry(serviceName, portName);
		Operation o = getOperation(bindingEntry, operationName);
		Parameters parameters = bindingEntry.getParameters(o);

		if (parameters.returnParam != null) {
			// QName returnType =
			// org.apache.axis.wsdl.toJava.Utils.getXSIType(parameters.returnParam);
			QName returnQName = parameters.returnParam.getQName();
			returnName = returnQName.getLocalPart();
		}

		int size = parameters.list.size();
		for (int i = 0; i < size; i++) {
			Parameter p = (Parameter) parameters.list.get(i);
			switch (p.getMode()) {
			case Parameter.IN:
				inputs.add(getParamData((org.apache.axis.client.Call) call, p, (String) parameterValues.elementAt(i)));
				break;
			case Parameter.OUT:
				break;
			case Parameter.INOUT:
				inputs.add(getParamData((org.apache.axis.client.Call) call, p, (String) parameterValues.elementAt(i)));
				break;
			}
		}

		Object ret = call.invoke(inputs.toArray());
		Map<?, ?> outputs = call.getOutputParams();
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (ret != null && returnName != null) {
			map.put(returnName, ret);
		}
		if (outputs != null) {
			for (Iterator<?> i = outputs.keySet().iterator(); i.hasNext();) {
				Object obj = i.next();
				String name;
				Object value;
				if (obj.getClass().getName().equals("java.lang.String")) {
					name = (String) obj;
				} else {
					name = ((QName) obj).getLocalPart();
				}
				value = outputs.get(obj);
				map.put(name, value);
			}
		}
		return map;
	}

	public Vector<String> enumServiceNames() {
		Vector<String> v = new Vector<String>();
		Iterator<String> i = services.keySet().iterator();
		while (i.hasNext()) {
			String s = (String) i.next();
			v.addElement(s);
		}
		return v;
	}

	/**
	 * 获取所有服务端口名
	 * 
	 * @param serviceName
	 * @return
	 */
	public Vector<String> enumPortNames(String serviceName) {
		Vector<String> v = new Vector<String>();
		ServiceEntry serviceEntry = (ServiceEntry) services.get(serviceName);
		Map<?, ?> ports = serviceEntry.getService().getPorts();
		Iterator<?> i = ports.keySet().iterator();
		while (i.hasNext()) {
			String s = (String) i.next();
			v.addElement(s);
		}
		return v;
	}

	public Vector<String> enumOperationNames(String serviceName, String portName) {
		Vector<String> v = new Vector<String>();
		BindingEntry entry = getBindingEntry(serviceName, portName);
		Set<?> operations = entry.getOperations();
		Iterator<?> i = operations.iterator();
		while (i.hasNext()) {
			Operation o = (Operation) i.next();
			String s = o.getName();
			v.addElement(s);
		}
		return v;
	}

	public Parameters enumParameters(String serviceName, String portName, String operationName) {
		BindingEntry entry = getBindingEntry(serviceName, portName);
		Operation o = getOperation(entry, operationName);
		Parameters parameters = entry.getParameters(o);
		return parameters;
	}

	public String getParameterModeString(Parameter p) {
		String ret = null;
		switch (p.getMode()) {
		case Parameter.IN:
			ret = "[IN]";
			break;
		case Parameter.INOUT:
			ret = "[IN, OUT]";
			break;
		case Parameter.OUT:
			ret = "[OUT]";
			break;
		}
		return ret;
	}

	private BindingEntry getBindingEntry(String serviceName, String portName) {
		ServiceEntry serviceEntry = (ServiceEntry) services.get(serviceName);
		Port port = serviceEntry.getService().getPort(portName);
		Binding binding = port.getBinding();
		SymbolTable table = wsdlParser.getSymbolTable();
		return table.getBindingEntry(binding.getQName());
	}

	private Operation getOperation(BindingEntry entry, String operationName) {
		Set<?> operations = entry.getOperations();
		Iterator<?> i = operations.iterator();
		while (i.hasNext()) {
			Operation o = (Operation) i.next();
			if (operationName.equals(o.getName())) {
				return o;
			}
		}
		return null;
	}

	// return Map of <QName.getLocalPart, SymTabEntry>
	private Map<String, SymTabEntry> enumSymTabEntry(Class<ServiceEntry> cls) {
		HashMap<String, SymTabEntry> ret = new HashMap<String, SymTabEntry>();
		HashMap<?, ?> map = wsdlParser.getSymbolTable().getHashMap();
		Iterator<?> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			@SuppressWarnings("rawtypes")
			Map.Entry entry = (Map.Entry) iterator.next();
			QName key = (QName) entry.getKey();
			Vector<?> v = (Vector<?>) entry.getValue();
			int size = v.size();
			for (int i = 0; i < size; ++i) {
				SymTabEntry symTabEntry = (SymTabEntry) v.elementAt(i);
				if (cls.isInstance(symTabEntry)) {
					ret.put(key.getLocalPart(), symTabEntry);
				}
			}
		}
		return ret;
	}

	private Object getParamData(org.apache.axis.client.Call c, Parameter p, String arg) throws Exception {
		// Get the QName representing the parameter type
		QName paramType = org.apache.axis.wsdl.toJava.Utils.getXSIType(p);

		TypeEntry type = p.getType();
		if (type instanceof BaseType && ((BaseType) type).isBaseType()) {
			DeserializerFactory factory = c.getTypeMapping().getDeserializer(paramType);
			Deserializer deserializer = factory.getDeserializerAs(Constants.AXIS_SAX);
			if (deserializer instanceof SimpleDeserializer) {
				return ((SimpleDeserializer) deserializer).makeValue(arg);
			}
		}
		throw new RuntimeException("not know how to convert '" + arg + "' into " + c);
	}
}