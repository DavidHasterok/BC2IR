class ExecutionEngine {
    BC2IR converter;
    public static Register[] registers;   //liste Lokaler Register
   	Heap heap;
	ClassLoader classLoader;

	ExecutionEngine(Heap h, ClassLoader cl) {

		heap = h;
		classLoader = cl;
	}

	//start by executing the main method
	void executeMain(boolean showStack, boolean haltStack) {

		MethodTableEntry mainMeth = null;

		//check if main class has at least one method
		if(!heap.methTabs.isEmpty()) {

			//first method table in heap: method table of main class
			mainMeth = heap.methTabs.get(0).findMethod("tinyMain", "()V");

		} else {

			throw new InternalError("Error at ExecutionEngine.executeMain(): No methods have been declared.");
		}

		//check method format
		if(mainMeth != null && mainMeth.isStatic) {

			Frame mainFrame = new Frame(heap.runtimeConstantPools.get(0), true, "tinyMain", "()V",
				mainMeth.maxStack, mainMeth.maxLocals, null, null, mainMeth.code, showStack, haltStack);

			heap.fs.pushFrame(mainFrame);
			converter = new BC2IR(heap, classLoader, mainFrame);
			registers = new Register[mainFrame.rcp.size()];
			converter.produce(mainFrame, 0, mainFrame.codeArray.length, registers);
			converter.showInstructions(converter.firstBB);
			//execute(mainFrame, showStack, haltStack);
			

		} else {

			throw new InternalError("Error at ExecutionEngine.executeMain(): " + 
				"static void tinyMain() method not found.");
		}
	}

	//execution of bytecode instructions
	int execute(Frame f, boolean showStack, boolean haltStack) {

		int bc = 0;
		int returnInt = 0;		//integer value to possibly be returned by a method
		boolean veri = false;

		while(f.pc < f.codeArray.length) {

			int startPC = f.pc;
			bc = f.codeArray[f.pc];

			if(bc == BC.NOP) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);

			} else if(bc == BC.ICONST_0) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(0));

			} else if(bc == BC.ICONST_1) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(1));

			} else if(bc == BC.ICONST_2) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(2));

			} else if(bc == BC.ICONST_3) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(3));

			} else if(bc == BC.ICONST_4) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(4));

			} else if(bc == BC.ICONST_5) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(5));

			} else if(bc == BC.BIPUSH) {

				//value in next array entry
				f.pc++;
				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.codeArray[f.pc]));

			} else if(bc == BC.SIPUSH) {

				//value in next two array entries
				int shortVal = f.codeArray[f.pc + 1];
				shortVal = (shortVal << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(shortVal));

			} else if(bc == BC.LDC) {

				//constant pool index in next array entry
				f.pc++;
				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.rcp.getConst(f.codeArray[f.pc]));

			} else if(bc == BC.LDC_W) {

				//constant pool index in next two entries
				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.rcp.getConst(cpIndex));

			} else if(bc == BC.ILOAD) {

				//local variable table index in next entry
				f.pc++;
				int lvIndex = f.codeArray[f.pc];

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[lvIndex]);

			} else if(bc == BC.ALOAD) {

				//local variable table index in next entry
				f.pc++;
				int lvIndex = f.codeArray[f.pc];

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[lvIndex]);

			} else if(bc == BC.ILOAD_0 || bc == BC.ALOAD_0) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[0]);

			} else if(bc == BC.ILOAD_1 || bc == BC.ALOAD_1) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[1]);

			} else if(bc == BC.ILOAD_2 || bc == BC.ALOAD_2) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[2]);

			} else if(bc == BC.ILOAD_3 || bc == BC.ALOAD_3) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(f.lVars[3]);

			} else if(bc == BC.IALOAD) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int arrayIndex = f.opStack.pop().value;
				TinyJVMType arrayRef = f.opStack.pop();		//resoluted by getstatic or getfield

				if(((TinyJVMRefType)arrayRef).instance != null) {

					//possible ArrayIndexOutOfBoundsException thrown by class ArrayInstance
					f.opStack.push(TinyJVMType.toJVMType(((ArrayInstance)(((
						TinyJVMRefType)arrayRef).instance)).getElement(arrayIndex)));

				} else {

					throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
							"An array reference was null.");
				}

			} else if(bc == BC.ISTORE || bc == BC.ASTORE) {

				//index into local variable table in next entry
				f.pc++;
				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int lvIndex = f.codeArray[f.pc];

				f.lVars[lvIndex] = f.opStack.pop();

			} else if(bc == BC.ISTORE_0 || bc == BC.ASTORE_0) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.lVars[0] = f.opStack.pop();

			} else if(bc == BC.ISTORE_1 || bc == BC.ASTORE_1) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.lVars[1] = f.opStack.pop();

			} else if(bc == BC.ISTORE_2 || bc == BC.ASTORE_2) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.lVars[2] = f.opStack.pop();

			} else if(bc == BC.ISTORE_3 || bc == BC.ASTORE_3) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.lVars[3] = f.opStack.pop();

			} else if(bc == BC.IASTORE) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int val = f.opStack.pop().value;
				int arrayIndex = f.opStack.pop().value;
				TinyJVMType arrayRef = f.opStack.pop();

				if(((TinyJVMRefType)arrayRef).instance != null) {

					((ArrayInstance)(((TinyJVMRefType)arrayRef).instance)).setElement(arrayIndex, val);

				} else {

					throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
						"An array reference was null.");
				}

			} else if(bc == BC.POP) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.pop();

			} else if(bc == BC.POP2) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.pop();
				f.opStack.pop();

			} else if(bc == BC.DUP) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				TinyJVMType top = f.opStack.pop();
				f.opStack.push(top);
				f.opStack.push(top);

			} else if(bc == BC.DUP_X1) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				TinyJVMType top = f.opStack.pop();
				TinyJVMType sub = f.opStack.pop();
				f.opStack.push(top);
				f.opStack.push(sub);
				f.opStack.push(top);

			} else if(bc == BC.DUP_X2) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				TinyJVMType top = f.opStack.pop();
				TinyJVMType sub = f.opStack.pop();
				TinyJVMType third = f.opStack.pop();
				f.opStack.push(top);
				f.opStack.push(third);
				f.opStack.push(sub);
				f.opStack.push(top);

			} else if(bc == BC.SWAP) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				TinyJVMType top = f.opStack.pop();
				TinyJVMType sub = f.opStack.pop();
				f.opStack.push(top);
				f.opStack.push(sub);

			} else if(bc == BC.IADD) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.opStack.pop().value + f.opStack.pop().value));

			} else if(bc == BC.ISUB) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int subtrahend = f.opStack.pop().value;
				int minuend = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(minuend - subtrahend));

			} else if(bc == BC.IMUL) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.opStack.pop().value * f.opStack.pop().value));

			} else if(bc == BC.IDIV) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int divisor = f.opStack.pop().value;

				if(divisor == 0) {

					throw new ArithmeticException("Division by 0!");
				}
				
				int dividend = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(dividend / divisor));

			} else if(bc == BC.IREM) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int divisor = f.opStack.pop().value;
				int dividend = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(dividend - (dividend / divisor) * divisor));

			} else if(bc == BC.INEG) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(-f.opStack.pop().value));

			} else if(bc == BC.ISHL) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int shiftBits = f.opStack.pop().value;
				int val = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(val << shiftBits));

			} else if(bc == BC.ISHR) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int shiftBits = f.opStack.pop().value;
				int val = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(val >> shiftBits));

			} else if(bc == BC.IUSHR) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int shiftBits = f.opStack.pop().value;
				int val = f.opStack.pop().value;
				f.opStack.push(TinyJVMType.toJVMType(val >>> shiftBits));

			} else if(bc == BC.IAND) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.opStack.pop().value & f.opStack.pop().value));

			} else if(bc == BC.IOR) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.opStack.pop().value | f.opStack.pop().value));

			} else if(bc == BC.IXOR) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.opStack.push(TinyJVMType.toJVMType(f.opStack.pop().value ^ f.opStack.pop().value));

			} else if(bc == BC.IINC) {

				//index in next entry
				f.pc++;
				int lvIndex = f.codeArray[f.pc];

				//increment value in next entry
				f.pc++;
				int incValue = f.codeArray[f.pc];

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				f.lVars[lvIndex].value = f.lVars[lvIndex].value + incValue;

			} else if(bc == BC.IFEQ) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value == 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFNE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value != 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFLT) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value < 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFGE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value >= 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFGT) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value > 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFLE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value <= 0) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPEQ) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value == f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPNE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value != f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPLT) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				//second value on stack < top stack value
				if(f.opStack.pop().value >= f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPGE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value < f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPGT) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value <= f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ICMPLE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().value > f.opStack.pop().value) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ACMPEQ) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(f.opStack.pop().equals(f.opStack.pop())) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IF_ACMPNE) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				if(!f.opStack.pop().equals(f.opStack.pop())) {

					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					//jump to one entry before actual instruction,
					//because pc will be incremented at end of method
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.GOTO) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);
				
				//jump to signed offset (from current instruction number) in next two entries
				short jOffset = (short)f.codeArray[f.pc + 1];
				jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

				//jump to one entry before actual instruction,
				//because pc will be incremented at end of method
				f.pc = f.pc + jOffset - 1;

			} else if(bc == BC.IRETURN) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				returnInt = heap.fs.popFrame(f, true);

			} else if(bc == BC.RETURN) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				heap.fs.popFrame(f, false);

			} else if(bc == BC.GETSTATIC) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				//constant pool index in next two bytes
				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//find out class, find out if already resolved
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(f.rcp.getClassIndex(cpIndex)));
				StaticFieldsTable statsTab = heap.findStaticTable(className);

				if(statsTab == null) {

					//check if own class
					if(className.equals(f.rcp.className)) {

						throw new LinkageError("No static table with class name " + 
							className + " was found for instruction getstatic.");
					}

					//load new constant pool
					if(!classLoader.load(className, false)) {

						throw new LinkageError("No class with class name " + 
							className + " could be loaded.");
					}

					//get static table
					statsTab = heap.findStaticTable(className);

					//error if no static table found
					if(statsTab == null) {

						throw new LinkageError("No static table with class name " + 
							className + " was found for instruction getstatic.");
					}
				}

				//get static fields table entry
				String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
				String fieldType = f.rcp.getUTF8(f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex)));
				StaticFieldsTableEntry entry = statsTab.findEntry(fieldName, fieldType);

				if(entry == null) {

					throw new VerifyError("No static table entry with name" + 
						fieldName + " and type " + fieldType + "was found " + 
						"in class " + className + "for instruction getstatic.");
				}
				
				TinyJVMType type = null;

				//integer
				if(fieldType.equals("I")) {

					type = new TinyJVMType();
					type.isReference = false;
					type.value = ((SFTIntEntry)entry).intValue;

				//array or reference to a TinyJava class
				} else {

					type = new TinyJVMRefType();
					type.isReference = true;
					type.value = cpIndex;
					SFTReferenceEntry refEn = (SFTReferenceEntry)entry;

					if(refEn.staticInstance == null) {

						//find out class, find out if already resolved
						String instanceClassName = f.rcp.getUTF8(f.rcp.getTypeIndex(
							f.rcp.getNameTypeIndex(cpIndex)));
						String correctedClassName = "";

						if(instanceClassName.endsWith(";")) {

							correctedClassName = instanceClassName.substring(
								1, instanceClassName.length() - 1);
								
						} else {

							correctedClassName = instanceClassName;
						}

						RuntimeConstantPool constPool = heap.findRCP(correctedClassName);

						if(constPool == null) {

							//load new constant pool
							classLoader.load(correctedClassName, false);
							constPool = heap.findRCP(correctedClassName);

							if(constPool == null) {

								throw new LinkageError("Error at ExecutionEngine.execute(): " + 
									"Constant pool " + correctedClassName + " could not be found.");
							}
						}

						//create static instance
						FieldSummary fs = heap.getFieldSummary(
							f.rcp, f.rcp.getClassEntryIndex(correctedClassName), correctedClassName);
						
						//put new instance onto heap
						refEn.staticInstance = heap.addInstance(fs);		
					}

					TinyJVMRefType t = (TinyJVMRefType)type;
					t.instance = refEn.staticInstance;
				}
				
				f.opStack.push(type);

			} else if(bc == BC.PUTSTATIC) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				//constant pool index in next two bytes
				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//find out class, find out if already resolved
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(f.rcp.getClassIndex(cpIndex)));
				StaticFieldsTable statsTab = heap.findStaticTable(className);

				if(statsTab == null) {

					//check if own class
					if(className.equals(f.rcp.className)) {

						throw new LinkageError("No static table with class name " + 
							className + " was found for instruction getstatic.");
					}

					//load new constant pool
					if(!classLoader.load(className, false)) {

						throw new LinkageError("No class with class name " + 
							className + " could be loaded.");
					}

					//get static table
					statsTab = heap.findStaticTable(className);
					
					//error if no static table found
					if(statsTab == null) {

						throw new LinkageError("No static table with class name " + 
							className + " was found for instruction getstatic.");
					}
				}

				//get static fields table entry
				String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
				String fieldType = f.rcp.getUTF8(f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex)));
				StaticFieldsTableEntry entry = statsTab.findEntry(fieldName, fieldType);
				
				if(entry == null) {

					throw new VerifyError("No static table entry with name" + 
						fieldName + " and type " + fieldType + " was found " + 
						"in class " + className + " for instruction getstatic.");
				}
				
				TinyJVMType type = f.opStack.pop();

				//integer
				if(fieldType.equals("I")) {

					((SFTIntEntry)entry).intValue = type.value;

				//array or reference to a TinyJava class
				} else {

					((SFTReferenceEntry)entry).staticInstance = ((TinyJVMRefType)type).instance;
				}

			} else if(bc == BC.GETFIELD) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				//constant pool index in next two bytes
				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//get object reference
				Instance ini = ((TinyJVMRefType)f.opStack.pop()).instance;

				//check if object is array
				if(ini.isArray) {

					throw new VerifyError("Error at ExecutionEngine.execute(): " + 
						"The stack value for instruction getfield was an array.");
				}

				if(ini != null) {

					//get instance field
					String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
					String fieldType = f.rcp.getUTF8(f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex)));
					Field field = ini.getField(fieldName, fieldType);

					if(field == null) {

						throw new NoSuchFieldError("Error at ExecutionEngine.execute(): " + 
							"There was no field of type " + fieldType + " and name " + 
							fieldName + ".");
					}
					
					TinyJVMType type = null;

					//integer
					if(fieldType.equals("I")) {

						type = new TinyJVMType();
						type.isReference = false;
						type.value = ((IntField)field).value;

					//array or reference to a TinyJava class
					} else {

						type = new TinyJVMRefType();
						type.isReference = true;
						type.value = cpIndex;
						((TinyJVMRefType)type).instance = ((ReferenceField)field).instance;
					}

					f.opStack.push(type);

				} else {

					throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
						"Object reference " + cpIndex + " on stack was null.");
				}

			} else if(bc == BC.PUTFIELD) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				//constant pool index in next two bytes
				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//get value
				TinyJVMType type = f.opStack.pop();

				//get object reference
				Instance ini = ((TinyJVMRefType)f.opStack.pop()).instance;

				if(ini != null) {

					//get instance field
					String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
					String fieldType = f.rcp.getUTF8(f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex)));
					Field field = ini.getField(fieldName, fieldType);

					//integer
					if(fieldType.equals("I")) {

						((IntField)field).value = type.value;

					//array or reference to a TinyJava class
					} else {

						((ReferenceField)field).instance = ((TinyJVMRefType)type).instance;
					}

				} else {

					throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
						"Object reference " + cpIndex + " on stack was null.");
				}

			} else if(bc == BC.INVOKEVIRTUAL) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//get method table entry -> if not linked yet: creation of a link in the constant pool entry
				MethodTableEntry mte = null;
				String methName = "";
				String methType = "";
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(f.rcp.getClassIndex(cpIndex)));
				RuntimeConstantPool constPool = heap.findRCP(className);

				//get constant pool entry
				RCPMethEntry rcpEntry = (RCPMethEntry)(f.rcp.getEntry(cpIndex));

				if(rcpEntry.isResoluted) {

					mte = rcpEntry.methTabEntry;
					methName = mte.name;
					methType = mte.type;

				} else {

					//find out if already resolved
					if(constPool == null) {

						throw new LinkageError("Error at ExecutionEngine.execute(): " + 
							"Class " + className + " was not yet resolved as " + 
							"required by instruction invokevirtual.");
					}
					
					//get data of method entry in constant pool
					int methNameIndex = f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex));
					int methTypeIndex = f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex));

					//get method table entry
					methName = f.rcp.getUTF8(methNameIndex);
					methType = f.rcp.getUTF8(methTypeIndex);
					MethodTable methTab = heap.findMethTab(className);

					if(methTab == null) {

						throw new NullPointerException("No method table " + 
							"was created for class " + className + ".");
					}
					
					mte = methTab.findMethod(methName, methType);

					if(mte == null) {

						throw new NoSuchMethodError("No method called " + methName + 
							" of type " + methType + " could be found in class " + className);
					}
				
					//link the method table entry in the constant pool entry
					rcpEntry.isResoluted = true;
					rcpEntry.methTabEntry = mte;
				}

				//pop arguments, reference to "this" is first argument
				TinyJVMType[] firstLocVars = new TinyJVMType[mte.parCount + 1];

				for(int i = 0; i <= mte.parCount; i++) {

					firstLocVars[mte.parCount - i] = f.opStack.pop();
				}

				//check if "this" instance is null
				if(((TinyJVMRefType)firstLocVars[0]).instance == null) {

					throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
						"invokevirtual encountered a null reference to 'this' on the stack.");
				}

				//create and execute new frame				
				Frame newFrame = new Frame(constPool, false, methName, methType, 
					mte.maxStack, mte.maxLocals, null, firstLocVars, mte.code, showStack, haltStack);
				heap.fs.pushFrame(newFrame);
				//converter.produce(newFrame, 0, newFrame.codeArray.length-1,registers);
				int retValue = execute(newFrame, showStack, haltStack);

				//push return value onto stack
				if(mte.returnsInt) {

					f.opStack.push(TinyJVMType.toJVMType(retValue));
				}

			} else if(bc == BC.INVOKESPECIAL) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//get class name of the method to be invoked
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(f.rcp.getClassIndex(cpIndex)));

				if(!className.equals("java/lang/Object")) {

					//get method table entry -> if not linked yet: linked in constant pool
					MethodTableEntry mte = null;

					//get data of method entry in constant pool
					int methNameIndex = f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex));
					int methTypeIndex = f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex));
					RuntimeConstantPool constPool = heap.findRCP(className);
					String methName = "";
					String methType = "";

					//get constant pool entry
					RCPMethEntry rcpEntry = (RCPMethEntry)(f.rcp.getEntryByNameType(
						className, ClassLoader.CP_METHOD_REF, methNameIndex, methTypeIndex));

					if(rcpEntry.isResoluted) {

						mte = rcpEntry.methTabEntry;
						methName = mte.name;
						methType = mte.type;

					} else {

						//find out if already resolved
						if(constPool == null) {

							throw new LinkageError("Error at ExecutionEngine.execute(): " + 
								"Class " + className + " was not yet resolved as " + 
								"required by instruction invokespecial.");
						}

						//get method table entry
						methName = f.rcp.getUTF8(methNameIndex);
						methType = f.rcp.getUTF8(methTypeIndex);
						MethodTable methTab = heap.findMethTab(className);

						if(methTab == null) {

							throw new NullPointerException("No method table " + 
								"was created for class " + className + ".");
						}
					
						mte = methTab.findMethod(methName, methType);

						if(mte == null) {

							throw new NullPointerException("No method called " + methName + 
								" of type " + methType + " could be found in class " + className);
						}
				
						//link the method table entry in the constant pool entry
						rcpEntry.isResoluted = true;
						rcpEntry.methTabEntry = mte;
					}
					
					//pop arguments, reference to "this" is first argument
					TinyJVMType[] firstLocVars = new TinyJVMType[mte.parCount + 1];

					for(int i = 0; i <= mte.parCount; i++) {

						firstLocVars[mte.parCount - i] = f.opStack.pop();
					}

					//check if "this" instance is null
					if(((TinyJVMRefType)firstLocVars[0]).instance == null) {

						throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
							"invokespecial encountered a null reference to 'this' on the stack.");
					}

					//create and execute new frame						
					Frame newFrame = new Frame(constPool, false, methName, methType, 
						mte.maxStack, mte.maxLocals, null, firstLocVars, mte.code, showStack, haltStack);
					heap.fs.pushFrame(newFrame);
					//converter.produce(newFrame, 0, newFrame.codeArray.length -1, registers);
					int retValue = execute(newFrame, showStack, haltStack);

					//push return value onto stack
					if(mte.returnsInt) {

						f.opStack.push(TinyJVMType.toJVMType(retValue));
					}

				} else {

					f.opStack.pop();
				}

			} else if(bc == BC.INVOKESTATIC) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//get method table entry -> if not linked yet: linked in constant pool
				MethodTableEntry mte = null;

				//get data of method entry in constant pool
				int methNameIndex = f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex));
				int methTypeIndex = f.rcp.getTypeIndex(f.rcp.getNameTypeIndex(cpIndex));
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(f.rcp.getClassIndex(cpIndex)));
				RuntimeConstantPool constPool = heap.findRCP(className);
				String methName = "";
				String methType = "";

				//get constant pool entry
				RCPMethEntry rcpEntry = (RCPMethEntry)(f.rcp.getEntryByNameType(
					className, ClassLoader.CP_METHOD_REF, methNameIndex, methTypeIndex));

				if(rcpEntry.isResoluted) {

					mte = rcpEntry.methTabEntry;
					methName = mte.name;
					methType = mte.type;

				} else {

					//find out if already resolved
					if(constPool == null) {

						//load new constant pool
						classLoader.load(className, false);
						constPool = heap.findRCP(className);

						if(constPool == null) {
						
							throw new LinkageError("Error at ExecutionEngine.execute(): " + 
								"Class " + className + " could not be loaded as " + 
								"required by instruction invokestatic.");
						}
					}

					//get method table entry
					methName = f.rcp.getUTF8(methNameIndex);
					methType = f.rcp.getUTF8(methTypeIndex);
					MethodTable methTab = heap.findMethTab(className);

					if(methTab == null) {

						throw new NullPointerException("No method table " + 
							"was created for class " + className + ".");
					}
					
					mte = methTab.findMethod(methName, methType);

					if(mte == null) {

						throw new NullPointerException("No method called " + methName + 
							" of type " + methType + " could be found in class " + className);
					}

					if(!mte.isStatic) {

						throw new IncompatibleClassChangeError("The method table " + 
							"entry " + methName + "  " + methType + 
							" did not contain a static method.");
					}
					
					//link the method table entry in the constant pool entry
					rcpEntry.isResoluted = true;
					rcpEntry.methTabEntry = mte;
				}
				
				//pop arguments
				TinyJVMType[] firstLocVars = new TinyJVMType[mte.parCount];

				for(int i = 0; i < mte.parCount; i++) {

					firstLocVars[mte.parCount - 1 - i] = f.opStack.pop();
				}

				//create and execute new frame
				Frame newFrame = new Frame(constPool, false, methName, methType, 
					mte.maxStack, mte.maxLocals, null, firstLocVars, mte.code, showStack, haltStack);
				heap.fs.pushFrame(newFrame);
				int retValue = execute(newFrame, showStack, haltStack);

				//push return value onto stack
				if(mte.returnsInt) {

					f.opStack.push(TinyJVMType.toJVMType(retValue));
				}

			} else if(bc == BC.NEW) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				int cpIndex = f.codeArray[f.pc + 1];
				cpIndex = (cpIndex << 8) + f.codeArray[f.pc + 2];
				f.pc = f.pc + 2;

				//find out class, find out if already resolved
				String className = f.rcp.getUTF8(f.rcp.getClassNameIndex(cpIndex));
				RuntimeConstantPool constPool = heap.findRCP(className);

				if(constPool == null) {

					//load new constant pool
					classLoader.load(className, false);
					constPool = heap.findRCP(className);

					if(constPool == null) {

						throw new LinkageError("Error at ExecutionEngine.execute(): " + 
							"Constant pool " + className + " could not be found.");
					}
				}

				//create new stack entry
				TinyJVMRefType type = new TinyJVMRefType();
				type.isReference = true;
				type.value = cpIndex;

				//put new instance onto heap
				FieldSummary fs = heap.getFieldSummary(f.rcp, cpIndex, constPool.className);
				type.instance = heap.addInstance(fs);
				f.opStack.push(type);

			} else if(bc == BC.NEWARRAY) {

				//array type indicated by next byte
				f.pc++;
				f.veri.displayBC(showStack, startPC, f.pc + 1);
				int typeIndex = f.codeArray[f.pc];

				int size = f.opStack.pop().value;

				//create new stack entry				
				TinyJVMRefType type = new TinyJVMRefType();
				type.isReference = true;

				//put new instance onto heap
				ArrayInstance ini = heap.addArray(size);
				
				type.instance = ini;
				f.opStack.push(type);

			} else if(bc == BC.ARRAYLENGTH) {

				f.veri.displayBC(showStack, startPC, f.pc + 1);
				TinyJVMType arrayRef = f.opStack.pop();
				RCPFieldOrMethEntry entry = (RCPFieldOrMethEntry)f.rcp.getEntry(arrayRef.value);

				if(entry.isStatic) {

					//load size of a static array
					f.opStack.push(TinyJVMType.toJVMType(heap.findStaticTable(f.rcp.getUTF8(f.rcp.getClassNameIndex(
						entry.classIndex))).getArraySize(arrayRef.value)));

				} else {

					if(((TinyJVMRefType)arrayRef).instance != null) {

						//load size of a non-static array
						f.opStack.push(TinyJVMType.toJVMType(((ArrayInstance)(((
							TinyJVMRefType)arrayRef).instance)).size));

					} else {

						throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
							"An array reference was null.");
					}
				}

			} else if(bc == BC.WIDE) {

				//opcode in next byte
				f.pc++;
				int opCode = f.codeArray[f.pc];

				if(opCode == BC.IINC) {

					f.veri.displayBC(showStack, startPC, f.pc + 5);

					//local variable table index in next two entries
					int lvIndex = f.codeArray[f.pc + 1];
					lvIndex = (lvIndex << 8) + f.codeArray[f.pc + 2];

					//increment value in next two entries
					int incValue = f.codeArray[f.pc + 3];
					incValue = (incValue << 8) + f.codeArray[f.pc + 4];

					f.lVars[lvIndex].value = f.lVars[lvIndex].value + incValue;
					f.pc = f.pc + 4;

				} else if(opCode == BC.ILOAD || opCode == BC.ALOAD) {

					f.veri.displayBC(showStack, startPC, f.pc + 3);

					//local variable table index in next two entries
					int lvIndex = f.codeArray[f.pc + 1];
					lvIndex = (lvIndex << 8) + f.codeArray[f.pc + 2];

					f.opStack.push(f.lVars[lvIndex]);
					f.pc = f.pc + 2;

				} else if(opCode == BC.ISTORE || opCode == BC.ASTORE) {

					f.veri.displayBC(showStack, startPC, f.pc + 3);

					//local variable table index in next two entries
					int lvIndex = f.codeArray[f.pc + 1];
					lvIndex = (lvIndex << 8) + f.codeArray[f.pc + 2];

					f.lVars[lvIndex] = f.opStack.pop();
					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFNULL) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				TinyJVMType type = f.opStack.pop();

				if(((TinyJVMRefType)type).instance == null) {

					//jump offset in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					int jIndex = f.pc + jOffset;
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.IFNONNULL) {

				f.veri.displayBC(showStack, startPC, f.pc + 3);

				TinyJVMType type = f.opStack.pop();

				if(((TinyJVMRefType)type).instance != null) {

					//jump offset in next two entries
					short jOffset = (short)f.codeArray[f.pc + 1];
					jOffset = (short)((jOffset << 8) + f.codeArray[f.pc + 2]);

					int jIndex = f.pc + jOffset;
					f.pc = f.pc + jOffset - 1;

				} else {

					f.pc = f.pc + 2;
				}

			} else if(bc == BC.GOTO_W) {

				f.veri.displayBC(showStack, startPC, f.pc + 5);

				//jump offset in next four entries
				int jOffset = (f.codeArray[f.pc + 1] << 24) + (f.codeArray[f.pc + 2] << 16)
					+ (f.codeArray[f.pc + 3] << 8) + (f.codeArray[f.pc + 4]);

				int jIndex = f.pc + jOffset;
				f.pc = f.pc + jOffset - 1;
			}

			f.pc++;
		}

		return returnInt;
	}
}
