import java.util.Vector;

class Heap {

	Vector<RuntimeConstantPool> runtimeConstantPools;
	Vector<MethodTable> methTabs;
	Vector<StaticFieldsTable> staticTabs;
	Vector<Instance> objects;		//arrays and TinyJava objects
	Vector<FieldSummary> instanceFieldsTab;
	FrameStack fs;

	Heap() {

		runtimeConstantPools = new Vector<RuntimeConstantPool>();
		methTabs = new Vector<MethodTable>();
		staticTabs = new Vector<StaticFieldsTable>();
		objects = new Vector<Instance>();
		instanceFieldsTab = new Vector<FieldSummary>();
		fs = new FrameStack();
	}

	void addRCP(RuntimeConstantPool rcp) {

		if(rcp != null) {

			runtimeConstantPools.add(rcp);

		} else {

			throw new InternalError("Error at Heap.addRCP(): Given RuntimeConstantPool reference was null.");
		}
	}

	RuntimeConstantPool findRCP(String name) {

		RuntimeConstantPool rcp = null;

		int i = 0;
		while(i < runtimeConstantPools.size() && !runtimeConstantPools.get(i).className.equals(name)) {

			i++;
		}

		if(i < runtimeConstantPools.size()) {

			rcp = runtimeConstantPools.get(i);
		}

		return rcp;
	}

	void addMethTab(MethodTable mt) {

		if(mt != null) {

			methTabs.add(mt);

		} else {

			throw new InternalError("Error at Heap.addMethTab(): Given MethodTable reference was null.");
		}
	}

	MethodTable findMethTab(String name) {

		MethodTable tab = null;

		int i = 0;
		while(i < methTabs.size() && !methTabs.get(i).className.equals(name)) {

			i++;
		}

		if(i < methTabs.size()) {

			tab = methTabs.get(i);
		}

		return tab;
	}

	void addStatsTab(StaticFieldsTable st) {

		if(st != null) {

			staticTabs.add(st);

		} else {

			throw new InternalError("Error at Heap.addStatsTab(): Given StaticFieldsTable reference was null.");
		}
	}

	StaticFieldsTable findStaticTable(String className) {

		int i = 0;
		while(i < staticTabs.size() && !staticTabs.get(i).className.equals(className)) {

			i++;
		}

		if(i >= staticTabs.size()) {

			throw new NullPointerException("Exception at Heap.getStaticTable(): " +
				"Requested table of class " + className + " has not yet been initialised.");
		}

		return staticTabs.get(i);
	}

	Instance addInstance(FieldSummary fs) {

		Instance ini = new Instance(fs);
		ini.className = fs.className;
		objects.add(ini);
		return ini;
	}

	ArrayInstance addArray(int size) {

		if(size < 0) {

			throw new NegativeArraySizeException("Error at Heap.addArray(): " + 
				"Array size " + size + " was negative.");
		}

		ArrayInstance ini = new ArrayInstance(size);
		ini.className = "int[" + size + "]";
		objects.add(ini);
		return ini;
	}

	void addFieldSummary(FieldSummary fs) {

		if(fs != null) {

			instanceFieldsTab.add(fs);

		} else {

			throw new InternalError("Error at Heap.addFieldSummary: Given FieldSummary reference was null.");
		}
	}

	FieldSummary getFieldSummary(RuntimeConstantPool rcp, int cpIndex, String className) {

		RCPEntry entry = rcp.getEntry(cpIndex);
		FieldSummary fs = null;

		if(entry.tag == ClassLoader.CP_CLASS_REF) {

			RCPClassEntry classEntry = (RCPClassEntry)entry;
			if(classEntry.isResoluted) {

				fs = classEntry.fieldSum;

			} else {

				int i = 0;
				while(i < instanceFieldsTab.size() && !className.equals(instanceFieldsTab.get(i).className)) {

					i++;
				}

				if(i != instanceFieldsTab.size()) {

					fs = instanceFieldsTab.get(i);
				}

				classEntry.isResoluted = true;
			}

		} else {

			throw new InternalError("Error at Heap.getFieldSummary(): " + 
				"Index " + cpIndex + " did not indicate a class reference entry in class " + rcp.className + ".");
		}

		return fs;
	}
}
