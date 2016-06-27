import java.util.Vector;

class RuntimeConstantPool {

	String className;
	Vector<RCPEntry> rcpEntries;	//usage of same indices like in 
					//original bytecode constant pool
	int thisIndex;
	int superIndex;

	RuntimeConstantPool(String name) {

		className = name;
		rcpEntries = new Vector<RCPEntry>();

		//no constant pool entry at index 0
		if(rcpEntries.size() == 0) {

			RCPEntry firstEntry = null;
			rcpEntries.add(firstEntry);

		} else {

			throw new InternalError("Error at RuntimeConstantPool.init(): initial constant pool was not empty.");
		}
	}

	int size() {

		return rcpEntries.size();
	}

	void addEntry(RCPEntry rcpe) {

		if(rcpe != null) {

			rcpEntries.add(rcpe);

		} else {

			throw new InternalError("Error at RuntimeConstantPool.addEntry(): RCPEntry reference was null.");
		}
	}

	//get an entry from the constant pool
	RCPEntry getEntry(int index) {

		RCPEntry entry = null;

		if(index > 0 && index < rcpEntries.size()) {

			entry = rcpEntries.get(index);

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getEntry(): index " + index + " was greater than the constant pool size.");
		}

		return entry;
	}
	
	RCPEntry getEntryByNameType(String origClassName, int cpTag, int name, int type) {

		RCPEntry entry = null;

		//find the descriptor entry
		int descrIndex = 0;
		boolean equalName = false;
		boolean equalType = false;
		RCPEntry rcpTemp = null;

		while(descrIndex <  rcpEntries.size() - 1 && !(equalName && equalType)) {

			descrIndex++;
			rcpTemp = getEntry(descrIndex);

			if(rcpTemp.tag == ClassLoader.CP_NAME_TYPE_DESC) {

				equalName = ((RCPNameTypeEntry)rcpTemp).nameIndex == name;
				equalType = ((RCPNameTypeEntry)rcpTemp).typeIndex == type;
			}
		}

		if((equalName && equalType)) {

			//find the field or method entry with descriptor descrIndex
			int fieldMethIndex = 0;
			boolean equalClassName = false;
			boolean equalDescr = false;

			while(fieldMethIndex < rcpEntries.size() - 1 && !(equalClassName && equalDescr)) {

				fieldMethIndex++;

				entry = getEntry(fieldMethIndex);
				if(entry.tag == cpTag) {

					int cni = getClassNameIndex(((RCPFieldOrMethEntry)entry).classIndex);
					String utf8String = getUTF8(cni);

					equalClassName = utf8String.equals(origClassName);
					equalDescr = ((RCPFieldOrMethEntry)entry).nameTypeIndex == descrIndex;
				}
			}

			if(!(equalClassName && equalDescr)) {

				String errStringGen = "Error at RuntimeConstantPool.getEntryByNameType(): ";
				String errStringSpe = "appropriate entry with tag " + cpTag + " not found in constant pool table.";
				throw new InternalError(errStringGen + errStringSpe);
			}
		}

		//returning of null, if no entry was found
		return entry;
	}

	//get String from an RCPUTF8Entry
	String getUTF8(int index) {

		RCPEntry utf8Entry = rcpEntries.get(index);
		String utf8 = "";

		if(utf8Entry.tag == ClassLoader.CP_UTF8) {

			utf8 = ((RCPUTF8Entry)utf8Entry).utf8String;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getUTF8(): Index " + index + " did not indicate a utf8 entry.");
		}

		return utf8;
	}

	//get name index of an RCPClassEntry
	int getClassNameIndex(int index) {

		RCPEntry classEntry = rcpEntries.get(index);
		int cni = 0;

		if(classEntry.tag == ClassLoader.CP_CLASS_REF) {

			cni = ((RCPClassEntry)classEntry).classNameIndex;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getClassNameIndex(): Index " + index + " did not indicate a class entry.");
		}

		return cni;
	}

	//get class index of an RCPFieldOrMethEntry
	int getClassIndex(int index) {

		RCPEntry entry = rcpEntries.get(index);
		int ci = 0;

		if(entry.tag == ClassLoader.CP_FIELD_REF || entry.tag == ClassLoader.CP_METHOD_REF) {

			ci = ((RCPFieldOrMethEntry)entry).classIndex;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getClassIndex(): Index " 
				+ index + " did neither indicate a field entry nor a method entry nor a class entry.");
		}

		return ci;
	}

	//get constant value of an RCPIntEntry or constant reference of an RCPField(OrMeth)Entry
	TinyJVMType getConst(int index) {

		RCPEntry constEntry = rcpEntries.get(index);
		TinyJVMType type = new TinyJVMType();

		if(constEntry.tag == ClassLoader.CP_INTEGER) {

			int val = ((RCPIntEntry)constEntry).constValue;
			type = TinyJVMType.toJVMType(val);

		}

		return type;
	}

	//get name and type index of an RCPFieldOrMethEntry
	int getNameTypeIndex(int index) {

		RCPEntry fieldMethEntry = rcpEntries.get(index);
		int nti = 0;

		if(fieldMethEntry.tag == ClassLoader.CP_FIELD_REF || fieldMethEntry.tag == ClassLoader.CP_METHOD_REF) {

			nti = ((RCPFieldOrMethEntry)fieldMethEntry).nameTypeIndex;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getNameTypeIndex(): Index " + index + " did neither indicate a field entry nor a method entry.");
		}

		return nti;
	}

	//get name index of an RCPNameTypeEntry
	int getNameIndex(int index) {

		RCPEntry nameTypeEntry = rcpEntries.get(index);
		int ni = 0;

		if(nameTypeEntry.tag == ClassLoader.CP_NAME_TYPE_DESC) {

			ni = ((RCPNameTypeEntry)nameTypeEntry).nameIndex;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getNameIndex(): Index " + index + " did not indicate a name-and-type entry.");
		}

		return ni;
	}

	//get type index of an RCPNameTypeEntry
	int getTypeIndex(int index) {

		RCPEntry nameTypeEntry = rcpEntries.get(index);
		int ti = 0;

		if(nameTypeEntry.tag == ClassLoader.CP_NAME_TYPE_DESC) {

			ti = ((RCPNameTypeEntry)nameTypeEntry).typeIndex;

		} else {

			throw new InternalError("Error at RuntimeConstantPool.getNameIndex(): Index " + index + 
				" did not indicate a name-and-type entry.");
		}

		return ti;
	}

	//get class index of a known string
	int getClassEntryIndex(String className) {

		int cei = 1;
		RCPEntry rcpTemp = getEntry(cei);
		boolean rightEntry = false;

		while(cei <  rcpEntries.size() - 1 && !rightEntry) {

			if(rcpTemp.tag == ClassLoader.CP_CLASS_REF && className.equals(getUTF8(getClassNameIndex(cei)))) {

				rightEntry = true;

			} else {

				cei++;
				rcpTemp = getEntry(cei);
			}
		}

		if(cei >= rcpEntries.size() - 1) {

			throw new InternalError("Error at RuntimeConstantPool.getClassEntryIndex(): Index " + cei + 
				"is greater than the valid count of the constant pool.");
		}

		return cei;
	}

	//verify the format of the constant pool
	void verify() {

		for(int i = 1; i < rcpEntries.size(); i++) {

			RCPEntry entry = rcpEntries.get(i);
			int tag = entry.tag;

			if(tag == ClassLoader.CP_UTF8 && ((RCPUTF8Entry)entry).utf8String.length() == 0) {

				throw new VerifyError("Constant pool UTF-8 entry at index " + 
					entry.cpIndex + "was malformed.");

			} else if(tag == ClassLoader.CP_CLASS_REF && rcpEntries.get(((
				RCPClassEntry)entry).classNameIndex).tag != ClassLoader.CP_UTF8) {

				throw new VerifyError("Constant pool class entry at index " + 
					entry.cpIndex + "was malformed.");

			} else if(tag == ClassLoader.CP_FIELD_REF) {

				boolean verified = true;

				if(rcpEntries.get(((RCPFieldOrMethEntry)entry).classIndex).tag 
					!= ClassLoader.CP_CLASS_REF) {

					verified = false;

				} else if(rcpEntries.get(((RCPFieldOrMethEntry)entry).nameTypeIndex).tag
					!= ClassLoader.CP_NAME_TYPE_DESC) {

					verified = false;
				}

				String fieldString = getUTF8(getTypeIndex(((RCPFieldOrMethEntry)entry).nameTypeIndex));

				if(!fieldString.equals("I") && !fieldString.equals("[I") && 
					!(fieldString.startsWith("L") && fieldString.endsWith(";"))) {

					verified = false;
				}

				if(!verified) {

					throw new VerifyError("Constant pool field entry at index " + 
						entry.cpIndex + "was malformed.");
				}

			} else if(tag == ClassLoader.CP_METHOD_REF) {

				boolean verified = true;

				if(rcpEntries.get(((RCPFieldOrMethEntry)entry).classIndex).tag 
					!= ClassLoader.CP_CLASS_REF) {

					verified = false;

				} else if(rcpEntries.get(((RCPFieldOrMethEntry)entry).nameTypeIndex).tag
					!= ClassLoader.CP_NAME_TYPE_DESC) {

					verified = false;
				}

				String methString = getUTF8(getTypeIndex(((RCPFieldOrMethEntry)entry).nameTypeIndex));

				if(!(methString.startsWith("(") && methString.contains(")") && 
					methString.indexOf(")") != methString.length() - 1)) {

					verified = false;
				}

				if(!verified) {

					throw new VerifyError("Constant pool method entry at index " + 
						entry.cpIndex + "was malformed.");
				}

				String paraString = methString.substring(methString.indexOf("(") + 1, 
					methString.indexOf(")"));

				int j = 0;
				while(j < paraString.length()) {

					char paraChar = paraString.charAt(j);
					if(paraChar == 'I') {

						j++;

					} else if(paraChar == '[') {

						j = j + 2;

					} else if(paraChar == 'L') {

						int endIndex = paraString.indexOf(";", j);

						if(endIndex > 1) {

							String sub = paraString.substring(j + 1, endIndex);
							j = j + sub.length() + 2;

						} else {

							verified = false;
						}

					} else {

						verified = false;
						j = paraString.length();
					}
				}

				String returnString = methString.substring(methString.indexOf(")") + 1);

				if(!returnString.equals("I") && !returnString.equals("V")) {

					verified = false;
				}

				if(!verified) {

					throw new VerifyError("Constant pool method entry at index " + 
						entry.cpIndex + "was malformed.");
				}

			} else if(tag == ClassLoader.CP_NAME_TYPE_DESC && !(
				rcpEntries.get(((RCPNameTypeEntry)entry).nameIndex).tag == ClassLoader.CP_UTF8 && 
				rcpEntries.get(((RCPNameTypeEntry)entry).typeIndex).tag == ClassLoader.CP_UTF8)) {

				throw new VerifyError("Constant pool name and type entry at index " + 
					entry.cpIndex + "was malformed.");
			}
		}
	}
}
