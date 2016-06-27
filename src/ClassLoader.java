import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.lang.NoClassDefFoundError;
import java.lang.ClassFormatError;
import java.lang.OutOfMemoryError;
import java.util.Vector;

class ClassLoader {

	String pathName;	//for finding classes in same directory as main class
	Heap heap;
  	//possible constant pool tags for TinyJava
	public static final byte
		CP_UTF8 = 1,			//Unicode string
		CP_INTEGER = 3,			//int
		CP_CLASS_REF = 7,		//index to class name string
		CP_FIELD_REF = 9,		//two indices: class ref + name and type desc
		CP_METHOD_REF = 10,		//two indices: class ref + name and type desc
		CP_NAME_TYPE_DESC = 12;	//two indices to utf-8 strings: name + encoded type descriptor

	ClassLoader(String s, Heap h) {

		this.pathName = s;
		this.heap = h;
	}

	boolean load(String fileName, boolean debug) {

		try {
			DataInputStream inputStream = new DataInputStream(new FileInputStream(pathName + fileName + ".class"));
			RuntimeConstantPool runtimeConstPool = new RuntimeConstantPool(fileName);
			MethodTable methTab = new MethodTable(fileName);
			StaticFieldsTable statsTab = new StaticFieldsTable(fileName);
			FieldSummary fieldSum = new FieldSummary(fileName);

			if(runtimeConstPool == null || methTab == null || statsTab == null) {

				throw new OutOfMemoryError("Error at constructor of class ClassLoader");
			}

			return readClassFile(inputStream, runtimeConstPool, methTab, statsTab, fieldSum, debug);

		} catch(FileNotFoundException e) {

			throw new NoClassDefFoundError("File \"" + fileName + ".class\" not found.");

		} catch(NullPointerException e) {

			throw new InternalError("No file stream has been initialized.");
		}
	}

	boolean readClassFile(DataInputStream inStream, RuntimeConstantPool rcp, MethodTable methTab, 
		StaticFieldsTable statsTab, FieldSummary fieldSum, boolean debug) {

		boolean mn = this.readMagicNumber(inStream, debug);
		boolean vn = this.readVersionNumber(inStream, debug);
		boolean cp = this.readConstantPool(inStream, rcp, debug);
		boolean af = this.readAccessFlags(inStream, debug);
		boolean ts = this.readThisSuper(inStream, rcp, debug);
		boolean i = this.readInterfaces(inStream, debug);
		boolean f = this.readFields(inStream, rcp, statsTab, fieldSum, debug);
		boolean m = this.readMethods(inStream, rcp, methTab, debug);
		boolean a = this.readAttributes(inStream, rcp, null, null, debug, 0);
		boolean eof = this.readEndOfFile(inStream, debug);

		boolean ret = mn && vn && cp && af && ts && i && f && m && a && eof;

		if(ret && debug) {

			System.out.println("Class loading successful!");

		} else if(!ret) {

			System.out.println("Class loading unsuccessful.");
		}

		return ret;
	}

	boolean readMagicNumber(DataInputStream inputStream, boolean debug) {

		boolean verified = false;

		try {
			//read from stream
			String ca = Integer.toHexString(inputStream.readUnsignedByte());
			String fe = Integer.toHexString(inputStream.readUnsignedByte());
			String ba = Integer.toHexString(inputStream.readUnsignedByte());
			String be = Integer.toHexString(inputStream.readUnsignedByte());

			if(ca.equals("ca") && fe.equals("fe") && ba.equals("ba") && be.equals("be")) {

				verified = true;
			}

			if(debug) {

				System.out.println("Correct magic number found: " + ca + fe + ba + be);
			}

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readMagicNumber()");
		}

		return verified;
	}

	boolean readVersionNumber(DataInputStream inputStream, boolean debug) {

		boolean verified = false;

		try {
			short minorVersion = inputStream.readShort();
			short majorVersion = inputStream.readShort();

			if(debug) {

				System.out.println("Minor file format version: " + minorVersion);
				System.out.println("Major file format version: " + majorVersion);
			}

			verified = true;

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readVersionNumber()");
		}

		return verified;
	}

	boolean readConstantPool(DataInputStream inputStream, RuntimeConstantPool runtimeConstPool, boolean debug) {

		boolean verified = false;

		try {
			short cpSize = inputStream.readShort();

			if(debug) {

				System.out.println("\nConstant pool size: " + cpSize);
			}

			verified = true;

			for(int i = 1; i < cpSize; i++) {

				//get type of constant pool entry
				byte tag = inputStream.readByte();

				RCPEntry rcpEntry = null;

				if(debug) {

					System.out.print(i + "\ttag " + tag + "\t");
				}

				//character string
				if(tag == CP_UTF8) {

					short utf8Size = inputStream.readShort();
					byte[] utf8Bytes = new byte[utf8Size];

					for(int j = 0; j < utf8Size; j++) {

						utf8Bytes[j] = inputStream.readByte();
					}

					String utf8String = new String(utf8Bytes, "UTF8");

					rcpEntry = new RCPUTF8Entry();
					((RCPUTF8Entry)rcpEntry).utf8String = utf8String;

					if(debug) {

						System.out.println("UTF-8 string: " + utf8String);
					}

				//integer constant
				} else if(tag == CP_INTEGER) {

					int val = inputStream.readInt();

					rcpEntry = new RCPIntEntry();
					((RCPIntEntry)rcpEntry).constValue = val;

					if(debug) {

						System.out.println("Integer value: " + val);
					}

				//class reference, class name: at index in following two bytes
				} else if(tag == CP_CLASS_REF) {

					short classNameIndex = inputStream.readShort();

					rcpEntry = new RCPClassEntry();
					((RCPClassEntry)rcpEntry).classNameIndex = classNameIndex;

					if(debug) {

						System.out.println("Class name reference: " + classNameIndex);
					}

				//field reference, with class, name and type (int, int[] or a reference to a TinyJava class)
				} else if(tag == CP_FIELD_REF) {

					rcpEntry = new RCPFieldOrMethEntry();

					//pointer to class reference
					short classRefIndex = inputStream.readShort();
					((RCPFieldOrMethEntry)rcpEntry).classIndex = classRefIndex;

					//pointer to name and type descriptor
					short nameTypeDescIndex = inputStream.readShort();
					((RCPFieldOrMethEntry)rcpEntry).nameTypeIndex = nameTypeDescIndex;

					if(debug) {

						System.out.print("Field reference\t\t");
						System.out.print("Class: " + classRefIndex + "\t");
						System.out.println("name and type: " + nameTypeDescIndex);
					}

				//method reference, with class, name and type (int, int[] or a reference to a TinyJava class)
				} else if(tag == CP_METHOD_REF) {

					rcpEntry = new RCPMethEntry();

					//pointer to class reference
					short classRefIndex = inputStream.readShort();
					((RCPMethEntry)rcpEntry).classIndex = classRefIndex;

					//pointer to name and type descriptor
					short nameTypeDescIndex = inputStream.readShort();
					((RCPMethEntry)rcpEntry).nameTypeIndex = nameTypeDescIndex;

					if(debug) {

						System.out.print("Method reference\t");
						System.out.print("Class: " + classRefIndex + "\t");
						System.out.println("name and type: " + nameTypeDescIndex);
					}

				//descriptor reference, with name and type (int, int[] or a reference to a TinyJava class)
				} else if(tag == CP_NAME_TYPE_DESC) {

					rcpEntry = new RCPNameTypeEntry();

					//get name index
					short name = inputStream.readShort();
					((RCPNameTypeEntry)rcpEntry).nameIndex = name;

					//get type index
					short typeDesc = inputStream.readShort();
					((RCPNameTypeEntry)rcpEntry).typeIndex = typeDesc;

					if(debug) {

						System.out.print("Name and type descriptor\t");
						System.out.print("Name: " + name + "\t");
						System.out.println("Type descriptor: " + typeDesc);
					}

				} else {

					//no other possible tags for TinyJava class file
					verified = false;
				}

				rcpEntry.cpIndex = i;
				rcpEntry.tag = tag;
				runtimeConstPool.addEntry(rcpEntry);
			}

			runtimeConstPool.verify();
			heap.addRCP(runtimeConstPool);

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readConstantPool(): IOException");

		} catch(NullPointerException e) {

			throw new ClassFormatError("Error at ClassLoader.readConstantPool(): " + 
				"Tag not known.");
		}

		return verified;
	}

	boolean readAccessFlags(DataInputStream inputStream, boolean debug) {

		boolean verified = false;

		try {
			short flags = inputStream.readShort();

			if(debug) {

				System.out.println("\nAccess flags: " + flags);
			}

			//for compatibility with JVMs prior to 1.1 
			//(concerning invocations of super class methods)
			if((flags & 0x0020) == 0x0020) {

				if(debug) {

					System.out.println("Access flag SUPER set");
				}

				verified = true;
			}

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readAccessFlags()");
		}

		return verified;
	}

	boolean readThisSuper(DataInputStream inputStream, RuntimeConstantPool runtimeConstPool, boolean debug) {

		boolean verified = false;

		try {
			//indices to "this" and "super" reference
			short thisIndex = inputStream.readShort();
			short superIndex = inputStream.readShort();
			runtimeConstPool.thisIndex = thisIndex;
			runtimeConstPool.superIndex = superIndex;

			if(debug) {

				//print index of "this" and find "this" class name
				System.out.print("Index to \"this\": " + thisIndex + " -> ");

				//find "this" class name index and class name
				int tcni = runtimeConstPool.getClassNameIndex(thisIndex);
				String tcn = runtimeConstPool.getUTF8(tcni);

				System.out.println(tcni + ": " + tcn);

				//same for "super"
				System.out.print("Index to \"super\": " + superIndex + " -> ");

				int scni = runtimeConstPool.getClassNameIndex(superIndex);
				String scn = runtimeConstPool.getUTF8(scni);

				System.out.println(scni + ": " + scn);
			}

			verified = true;

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readThisSuper(): IOException");
		}

		return verified;
	}

	boolean readInterfaces(DataInputStream inputStream, boolean debug) {

		boolean verified = false;

		try {
			short interfaceCount = inputStream.readShort();

			if(debug) {

				System.out.println("\nNumber of interfaces: " + interfaceCount);
			}

			//only in TinyJava
			if(interfaceCount == 0) {

				verified = true;
			}

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readInterfaces(): IOException");
		}

		return verified;
	}

	boolean readFields(DataInputStream inputStream, RuntimeConstantPool runtimeConstPool, 
		StaticFieldsTable statsTab, FieldSummary fieldSum, boolean debug) {

		boolean verified = false;

		try {
			short fieldCount = inputStream.readShort();

			if(debug) {

				System.out.println("\nNumber of fields: " + fieldCount);
			}

			for(int i = 0; i < fieldCount; i++) {

				short accFlags = inputStream.readShort();
				short fieldNameIndex = inputStream.readShort();
				short fieldTypeIndex = inputStream.readShort();		//return type + parameter types

				if(fieldNameIndex >= runtimeConstPool.size()) {

					throw new ClassFormatError("Error at ClassLoader.readFields(): fieldNameIndex " + 
						fieldNameIndex + " was out of bounds.");

				} else if(fieldTypeIndex >= runtimeConstPool.size()) {

					throw new ClassFormatError("Error at ClassLoader.readFields(): fieldTypeIndex " + 
						fieldTypeIndex + " was out of bounds.");
				}

				if(debug) {

					System.out.print(i + "\tflags: " + accFlags);

					if((accFlags & 0x0008) == 0x0008) {

						System.out.print(" (static)");
					}

					if((accFlags & 0x0010) == 0x0010) {

						System.out.print(" (final)");
					}

					String fieldName = runtimeConstPool.getUTF8(fieldNameIndex);
					String fieldType = runtimeConstPool.getUTF8(fieldTypeIndex);
					System.out.print("\tname: " + fieldName);
					System.out.print("\ttype: " + fieldType + "\t");
				}

				Field field = null;	//used if "final int" is used in source file

				//write static class variables into statics table, if field is static, but not final
				//(if both static and final in TinyJava: integer constant -> CP_INTEGER)
				if((accFlags & 0x0008) == 0x0008 && !((accFlags & 0x0010) == 0x0010)) {

					StaticFieldsTableEntry sftEntry = null;
					String typeString = runtimeConstPool.getUTF8(fieldTypeIndex);

					if(typeString.equals("I")) {

						//"I": constant pool representation of Integer
						sftEntry = new SFTIntEntry();

					} else {
						//either "[I" (int array) or "L<className>;" (reference to a TinyJava class)
						sftEntry = new SFTReferenceEntry();

						if(typeString.equals("[I")) {

							//if array: new array instance, length 0
							((SFTReferenceEntry)sftEntry).staticInstance = new ArrayInstance(0);
						
						}
					}

					//set constant pool entry's field isStatic
					RCPEntry rcpTemp = runtimeConstPool.getEntryByNameType(statsTab.className,
						CP_FIELD_REF, fieldNameIndex, fieldTypeIndex);

					if(rcpTemp != null) {
					
						((RCPFieldOrMethEntry)rcpTemp).isStatic = true;
						sftEntry.cpIndex = rcpTemp.cpIndex;
					}

					//save new entry into static fields table
					sftEntry.name = runtimeConstPool.getUTF8(fieldNameIndex);
					sftEntry.type = runtimeConstPool.getUTF8(fieldTypeIndex);
					statsTab.addSFTEntry(sftEntry);

				//not static, maybe final
				} else if(accFlags == 0 || 
					(((accFlags & 0x0010) == 0x0010) && !((accFlags & 0x0008) == 0x0008))) {

					String typeString = runtimeConstPool.getUTF8(fieldTypeIndex);

					if(typeString.equals("I")) {

						field = new IntField();

					} else {

						//final reference
						if((accFlags & 0x0010) == 0x0010) {

							throw new ClassFormatError("Error at ClassLoader.readFields(): " + 
								"final references not allowed in TinyJava.");
								
						} else {
						
							field = new ReferenceField();
						}
					}

					RCPEntry rcpTemp = runtimeConstPool.getEntryByNameType(runtimeConstPool.className,
						CP_FIELD_REF, fieldNameIndex, fieldTypeIndex);

					//if rcpTemp == null: field not used in class -> no constant pool entry
					if(rcpTemp != null) {

						if((accFlags & 0x0010) == 0x0010) {

							((RCPFieldOrMethEntry)rcpTemp).isFinal = true;
						}
					}
					
					field.name = runtimeConstPool.getUTF8(fieldNameIndex);
					field.type = runtimeConstPool.getUTF8(fieldTypeIndex);
					fieldSum.addField(field);
				}

				readAttributes(inputStream, runtimeConstPool, field, null, debug, 1);
			}

			heap.addStatsTab(statsTab);
			heap.addFieldSummary(fieldSum);

			verified = true;

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readFields(): IOException");

		} catch(NullPointerException e) {

			throw new InternalError("NullPointerException at ClassLoader.readFields()");
		}

		return verified;
	}

	boolean readMethods(DataInputStream inputStream, RuntimeConstantPool runtimeConstPool, MethodTable methTab, boolean debug) {

		boolean verified = false;

		try {
			short methCount = inputStream.readShort();

			if(debug) {

				System.out.println("\nNumber of methods: " + methCount);
			}

			//create method table entries
			for(int i = 0; i < methCount; i++) {

				short accFlags = inputStream.readShort();
				short methNameIndex = inputStream.readShort();
				short methTypeIndex = inputStream.readShort();

				MethodTableEntry methTabEntry = new MethodTableEntry();
				methTabEntry.name = runtimeConstPool.getUTF8(methNameIndex);
				String methType = runtimeConstPool.getUTF8(methTypeIndex);
				methTabEntry.type = methType;

				//find out return type
				String ret = methType.substring(methType.indexOf(")") + 1, methType.length());

				if(ret.equals("I")) {

					methTabEntry.returnsInt = true;

				} else if(!ret.equals("I") && !ret.equals("V")) {

					throw new ClassFormatError("Error at ClassLoader.readMethods(): " + 
						"Return type of method " + methTabEntry.name + " " +
						methType + "was neither integer nor void.");
				}

				//find out number of parameters and static attribute to know number of stack items to remove
				Vector<Integer> paraTypes = new Vector<Integer>();

				//static method
				if((accFlags & 0x0008) == 0x0008) {

					methTabEntry.isStatic = true;

				} else {

					//if instance method: take reference (2) to instance from stack
					paraTypes.add(2);
				}

				//find out number and types of parameters
				String paras = methType.substring(methType.indexOf("(") + 1, methType.indexOf(")"));
				int parCount = 0;
				int j = 0;

				while(j < paras.length()) {

					if(paras.charAt(j) == 'I') {

						j++;
						parCount++;
						paraTypes.add(1);

					} else if(paras.charAt(j) == '[') {

						j = j + 2;
						parCount++;
						paraTypes.add(2);

					} else if(paras.charAt(j) == 'L') {

						j = paras.indexOf(";", j) + 1;
						parCount++;
						paraTypes.add(2);

					} else {

						throw new ClassFormatError("Error at ClassLoader.reatMethods(): " + 
							"A parameter in method " + methTabEntry.name + " " + 
							methType + "was of no valid type of TinyJava.");
					}
				}

				int[] paraTypesInt = new int[paraTypes.size()];

				for(int k = 0; k < paraTypes.size(); k++) {

					paraTypesInt[k] = paraTypes.get(k);
				}

				//save entry to method table
				methTabEntry.parCount = parCount;
				methTabEntry.parameters = paraTypesInt;
				methTab.addEntry(methTabEntry);

				//print
				if(debug) {

					System.out.print(i + "\tflags: " + accFlags);

					if((accFlags & 0x0008) == 0x0008) {

						System.out.print(" (static)");
					}

					//print method name
					System.out.print("\tname: " + methTabEntry.name);

					//print method type
					System.out.print("\ttype: " + methType + "\t");
				}

				readAttributes(inputStream, runtimeConstPool, null, methTabEntry, debug, 1);
			}

			verified = true;
			heap.addMethTab(methTab);

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readMethods(): IOException");
		}

		return verified;
	}

	boolean readAttributes(DataInputStream inputStream, RuntimeConstantPool runtimeConstPool, 
		Field field, MethodTableEntry mte, boolean debug, int outputDepth) {

		boolean verified = false;

		try {
			short attriCount = inputStream.readShort();

			if(debug) {

				System.out.println("#attributes: " + attriCount);
			}

			for(int i = 0; i < attriCount; i++) {

				short attriNameIndex = inputStream.readShort();
				String attriString = runtimeConstPool.getUTF8(attriNameIndex);

				int attriLength = inputStream.readInt();

				String tabs = "";

				if(debug) {

					for(int j = 0; j < outputDepth; j++) {

						tabs = tabs + "\t";
					}

					System.out.print(tabs + i + "\tname: " + attriString);
					System.out.println("\tlength: " + attriLength + " Bytes");

					tabs = tabs + "\t";
				}

				//final [static] int
				if(attriString.equals("ConstantValue")) {

					short index = inputStream.readShort();

					if(debug) {

						int constVal = runtimeConstPool.getConst(index).value;

						if(field != null) {
						
							((IntField)field).value = constVal;
						}
						
						System.out.println(tabs + "const value: " + constVal);
					}

				//index to a constant pool utf-8 entry with the name of the source file
				} else if(attriString.equals("SourceFile")) {

					short index = inputStream.readShort();

					if(debug) {

						String fileName = runtimeConstPool.getUTF8(index);
						System.out.println(tabs + "source file: " + fileName);
					}

				//line number table useful for debugging
				} else if(attriString.equals("LineNumberTable")) {

					short tableLength = inputStream.readShort();

					if(debug) {

						System.out.println(tabs + "line number table length: " + tableLength);
					}

					for(int j = 0; j < tableLength; j++) {

						//get bytecode programme counters and associated lines in the source file
						short startPc = inputStream.readShort();
						short lineNum = inputStream.readShort();

						if(debug) {

							System.out.print(tabs + "start pc: " + startPc);
							System.out.println("\tline number: " + lineNum);
						}
					}

					if(debug) {

						System.out.println();
					}

				//code attribute of a method, containing the bytecode instructions
				//and additional information for efficient execution
				} else if(attriString.equals("Code")) {

					short maxStack = inputStream.readShort();	//maximum stack depth
					short maxLocals = inputStream.readShort();	//maximum number of local variables
					int codeLength = inputStream.readInt();

					int[] code = new int[codeLength];

					for(int j = 0; j < codeLength; j++) {

						code[j] = inputStream.readUnsignedByte();
					}

					if(debug) {

						System.out.print(tabs + "max stack: " + maxStack);
						System.out.print("\tmax locals: " + maxLocals);
						System.out.println("\tcode length: " + codeLength + " Bytes");

						System.out.println(tabs + "Code:");
					}

					//get the precious information saved into the method table
					mte.maxStack = maxStack;
					mte.maxLocals = maxLocals;
					mte.codeLength = codeLength;
					mte.code = code;

					//formally verify the bytecode
					BytecodeVerifier bcVerifier = new BytecodeVerifier(mte.code, runtimeConstPool);
					boolean veri = bcVerifier.verifyBC(debug, mte.parameters, 
						mte.returnsInt, mte.maxStack, mte.maxLocals, mte.name);
				
					if(debug && veri) {

						System.out.println("Verification of bytecode successful!");
					}

					//exception table, not used in TinyJava files
					short exceptTableLength = inputStream.readShort();

					for(int j = 0; j < exceptTableLength; j++) {

						for(int k = 0; k < 4; k++) {

							inputStream.readShort();
						}
					}
					
					readAttributes(inputStream, runtimeConstPool, null, null, debug, outputDepth + 1);

				} else {

					if(debug) {

						System.out.println(tabs + "other attribute:");

						for(int j = 0; j < attriLength; j++) {

							System.out.println(tabs + inputStream.readByte());
						}

					} else {

						for(int j = 0; j < attriLength; j++) {

							inputStream.readByte();
						}
					}
				}
			}

			verified = true;

		} catch(IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readAttributes(): IOException");
		}

		return verified;
	}

	boolean readEndOfFile(DataInputStream inputStream, boolean debug) {

		boolean verified = false;

		try {
			inputStream.readByte();

		} catch (EOFException e) {

			if(debug) {

				System.out.println("\nEnd of file found.");
			}

			verified = true;

		} catch (IOException e) {

			throw new ClassFormatError("Error at ClassLoader.readEndOfFile(): IOException");
		}

		return verified;
	}
}
