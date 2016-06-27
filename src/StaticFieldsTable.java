import java.util.Vector;

class StaticFieldsTable {

	String className;
	Vector<StaticFieldsTableEntry> statsTabEntries;

	StaticFieldsTable(String name) {

		className = name;
		statsTabEntries = new Vector<StaticFieldsTableEntry>();
	}

	void addSFTEntry(StaticFieldsTableEntry sfte) {

		if(sfte != null) {

			statsTabEntries.add(sfte);

		} else {

			throw new InternalError("Error at StaticFieldsTable.addSFTEntry(): given StaticFieldsTableEntry reference was null");
		}
	}

	StaticFieldsTableEntry findEntry(int cpIndex) {

		int i = 0;
		while(i < statsTabEntries.size() && statsTabEntries.get(i).cpIndex != cpIndex) {

			i++;
		}

		return statsTabEntries.get(i);
	}

	StaticFieldsTableEntry findEntry(String name, String type) {

		int i = 0;
		while(i < statsTabEntries.size() && !(statsTabEntries.get(i).name.equals(name) && 
			statsTabEntries.get(i).type.equals(type))) {

			i++;
		}

		return statsTabEntries.get(i);
	}

	int getArrayEntry(int cpIndex, int arrayIndex) {

		SFTReferenceEntry entry = (SFTReferenceEntry)findEntry(cpIndex);
		return ((ArrayInstance)entry.staticInstance).intArray[arrayIndex];
	}

	void setArrayEntry(int cpIndex, int arrayIndex, int value) {

		SFTReferenceEntry entry = (SFTReferenceEntry)findEntry(cpIndex);
		((ArrayInstance)entry.staticInstance).intArray[arrayIndex] = value;
	}

	int getArraySize(int cpIndex) {

		return ((ArrayInstance)((SFTReferenceEntry)findEntry(cpIndex)).staticInstance).size;
	}
}