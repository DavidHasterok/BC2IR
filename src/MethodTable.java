import java.util.Vector;

class MethodTable {

	String className;
	Vector<MethodTableEntry> methTabEntries;

	MethodTable(String name) {

		className = name;
		methTabEntries = new Vector<MethodTableEntry>();
	}

	void addEntry(MethodTableEntry mte) {

		if(mte != null) {

			methTabEntries.add(mte);

		} else {

			throw new InternalError("Error at MethodTable.addMethTabEntry(): Given MethodTableEntry reference was null");
		}
	}

	MethodTableEntry findMethod(String methName, String methType) {

		MethodTableEntry mte = null;

		int i = 0;
		while(i < methTabEntries.size() && !(methTabEntries.get(i).name.equals(methName) && 
			methTabEntries.get(i).type.equals(methType))) {

			i++;
		}

		if(i < methTabEntries.size()) {

			mte = methTabEntries.get(i);

		} else {

			throw new NoSuchMethodError("Error at MethodTable.findMethod(): Method " + methName + " " + methType + " not found.");
		}

		return mte;
	}
}