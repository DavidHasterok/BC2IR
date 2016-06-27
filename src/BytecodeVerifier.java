import java.util.Vector;

class BytecodeVerifier {

	private int[] codeArray;	//code to be verified
	private int pc;				//programme counter

	private boolean retInt;	//return type of the method
	private int maxStack;	//for preventing stack overflow
	private int maxLocals;	//for preventing local variable access out of bounds

	private Vector<Integer> stack;	//1: integer on stack, 2: reference on stack
	private int[] locals;			//1: integer in local variable, 2: reference in local variable

	private Vector<Integer> instructionStarts;	//for checking if branch instructions point to a valid instruction start
	private RuntimeConstantPool rcp;			//runtime constant pool of the class with the method to be verifed
	String methodName;							//for checking if method is <init>

	public BytecodeVerifier(int[] code, RuntimeConstantPool constPool) {

		codeArray = code;
		pc = 0;
		instructionStarts = new Vector<Integer>();
		rcp = constPool;
	}

	//for printing the disassembled name of an instruction
	//boolean show: for not writing "if(show)" into every single instruction in ExecutionEngine
	public void displayBC(boolean show, int start, int stop) {

		if(show) {

			processBC(start, stop, true, false, false);
		}
	}

	//verification by class loader, just after reading the bytecode from the .class file
	public boolean verifyBC(boolean debug, int[] arguments, boolean returnsInt, 
		int stackDepth, int numOfLocals, String methName) {
		
		retInt = returnsInt;
		maxStack = stackDepth;
		maxLocals = numOfLocals;
		methodName = methName;
		stack = new Vector<Integer>(maxStack);	//for simulating the operand stack
		locals = new int[maxLocals];			//for simulating the local variable table

		for(int i = 0; i < arguments.length; i++) {

			locals[i] = arguments[i];	//the arguments a method takes get stored 
										//into the local variable table, possibly 
										//alongside a reference to the instance, 
										//on which the method is invoked
		}

		return processBC(0, codeArray.length, debug, true, false);
	}

	//bytecode visualisation and verification
	//boolean secondCall: check branch or goto instructions for pointing to a
	//			valid start of an instrucion in vector instructionStarts
	private boolean processBC(int start, int stop, boolean debug, boolean verify, boolean secondCall) {

		pc = start;
		int bc = 0;
		String space = "\t\t  ";	//for printing the
		String pcString = "";		//disassembled
		String bcString = "";		//instrucion

		while(pc < stop) {

			if(!secondCall) {

				//this is an instruction beginning, therefore a valid target for a branch instruction
				instructionStarts.add(pc);
			}

			//for printing neatly
			if(pc < 10) {

				pcString = "  " + pc + ": ";

			} else if(pc < 100) {

				pcString = " " + pc + ": ";

			} else {

				pcString = pc + ": ";
			}

			//get the bytecoded instruction
			bc = codeArray[pc];

			//execute the instruction
			if(bc == BC.NOP) {

				bcString = "nop";

			} else if(bc >= BC.ICONST_0 && bc <= BC.ICONST_5 || bc == BC.BIPUSH || bc == BC.SIPUSH) {

				if(bc == BC.ICONST_0) {

					bcString = "iconst_0";

				} else if(bc == BC.ICONST_1) {

					bcString = "iconst_1";

				} else if(bc == BC.ICONST_2) {

					bcString = "iconst_2";

				} else if(bc == BC.ICONST_3) {

					bcString = "iconst_3";

				} else if(bc == BC.ICONST_4) {

					bcString = "iconst_4";

				} else if(bc == BC.ICONST_5) {

					bcString = "iconst_5";

				} else if(bc == BC.BIPUSH) {

					try {
						//value in next array entry
						pc++;
						int byteVal = codeArray[pc];
						bcString = "bipush\t\t" + byteVal;

					} catch(ArrayIndexOutOfBoundsException e) {

						throw new VerifyError("The code array ended in the " + 
							" middle of instruction bipush at index " + pc);
					}

				} else if(bc == BC.SIPUSH) {

					try {
						//value in next two array entries
						int shortVal = codeArray[pc + 1];
						shortVal = (shortVal << 8) + codeArray[pc + 2];
						pc = pc + 2;

						bcString = "sipush\t\t" + shortVal;

					} catch(ArrayIndexOutOfBoundsException e) {

						throw new VerifyError("The code array ended in the " + 
							"middle of instruction sipush at index " + pc);
					}
				}

				if(verify) {

					//check if stack would overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.LDC || bc == BC.LDC_W) {

				int cpIndex = 0;

				if(bc == BC.LDC) {

					try {
						//constant pool index in next array entry
						pc++;
						cpIndex = codeArray[pc];
						bcString = "ldc\t\tcp#" + cpIndex;

					} catch(ArrayIndexOutOfBoundsException e) {

						throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
							"ended in the middle of instruction ldc at index " + pc);
					}

				} else if(bc == BC.LDC_W) {

					try {
						//constant pool index in next two entries
						cpIndex = codeArray[pc + 1];
						cpIndex = (cpIndex << 8) + codeArray[pc + 2];
						pc = pc + 2;
						bcString = "ldc_w\t\tcp#" + cpIndex;

					} catch(ArrayIndexOutOfBoundsException e) {

						throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
							"ended in the middle of instruction ldc_w at index " + pc);
					}
				}

				if(verify) {

					//check if cpIndex is a valid index into the constant pool
					if(cpIndex < 1 || cpIndex >= rcp.size()) {

						throw new VerifyError("The constant pool index " + 
							"of instrucion " + bcString + " was out of bounds.");
					}

					//check for appropriate constant pool entry type
					if(rcp.getEntry(cpIndex).tag != ClassLoader.CP_INTEGER) {

						throw new VerifyError("The constant pool entry at " + 
							cpIndex + " did not contain an " + 
							"appropriate type for instruction" + bcString + ".");
					}

					//check if stack would overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ILOAD) {

				try {
					//local variable table index in next entry
					pc++;
					int lvIndex = codeArray[pc];
					bcString = "iload\t\tlv#" + lvIndex;

					if(verify) {

						//check if valid index into local variable table
						if(lvIndex >= maxLocals || lvIndex < 0) {

							throw new VerifyError("Local variable index at instruction " + 
								bcString + " was out of bounds.");
						}

						//check if variable table entry is of type integer (1)
						if(locals[lvIndex] != 1) {

							throw new VerifyError("Local variable at instruction " + 
								bcString + " did not contain an integer type.");
						}

						//look out for stack overflow
						if(stack.size() < maxStack) {

							//simulate a simplified stack
							stack.add(0, 1);

						} else {

							throw new VerifyError("Executing " + bcString + 
								" would lead to stack overflow.");
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction iload at index " + pc);
				}

			} else if(bc == BC.ALOAD) {

				try {
					//local variable table index in next entry
					pc++;
					int lvIndex = codeArray[pc];
					bcString = "aload\t\tlv#" + lvIndex;

					if(verify) {

						//check if valid index into local variable table
						if(lvIndex >= maxLocals || lvIndex < 0) {

							throw new VerifyError("Local variable index at instruction " + 
								bcString + " was out of bounds.");
						}

						//check if variable table entry is of type reference (2)
						if(locals[lvIndex] != 2) {

							throw new VerifyError("Local variable at instruction " + 
								bcString + " did not contain a reference type.");
						}


						//look out for stack overflow
						if(stack.size() < maxStack) {

							//simulate a simplified stack
							stack.add(0, 2);

						} else {

							throw new VerifyError("Executing " + bcString + 
								" would lead to stack overflow.");
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction aload at index " + pc + ".");
				}

			} else if(bc == BC.ILOAD_0) {

				bcString = "iload_0";

				if(verify) {

					//check if valid index into local variable table
					if(0 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type integer (1)
					if(locals[0] != 1) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain an integer type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ILOAD_1) {

				bcString = "iload_1";

				if(verify) {

					//check if valid index into local variable table
					if(1 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type integer (1)
					if(locals[1] != 1) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain an integer type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ILOAD_2) {

				bcString = "iload_2";

				if(verify) {

					//check if valid index into local variable table
					if(2 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type integer (1)
					if(locals[2] != 1) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain an integer type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ILOAD_3) {

				bcString = "iload_3";

				if(verify) {

					//check if valid index into local variable table
					if(3 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type integer (1)
					if(locals[3] != 1) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain an integer type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ALOAD_0) {

				bcString = "aload_0";

				if(verify) {

					//check if valid index into local variable table
					if(0 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type reference (2)
					if(locals[0] != 2) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain a reference type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 2);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ALOAD_1) {

				bcString = "aload_1";

				if(verify) {

					//check if valid index into local variable table
					if(1 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type reference (2)
					if(locals[1] != 2) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain a reference type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 2);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ALOAD_2) {

				bcString = "aload_2";

				if(verify) {

					//check if valid index into local variable table
					if(2 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type reference (2)
					if(locals[2] != 2) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain a reference type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 2);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ALOAD_3) {

				bcString = "aload_3";

				if(verify) {

					//check if valid index into local variable table
					if(3 >= maxLocals) {

						throw new VerifyError("Local variable index at instruction " + 
							bcString + " was out of bounds.");
					}

					//check if variable table entry is of type reference (2)
					if(locals[3] != 2) {

						throw new VerifyError("Local variable at instruction " + 
							bcString + " did not contain a reference type.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 2);

					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.IALOAD) {

				bcString = "iaload";

				//checking of index inside array bounds: at runtime -> in ExecutionEngine
				if(verify) {

					//check if two stack items can be taken away
					if(stack.size() < 2) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if array index is of type integer (1)
					if(stack.remove(0) != 1) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not an integer.");
					}

					//check if array reference is of type reference (2)
					if(stack.remove(0) != 2) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not a reference.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.ISTORE) {

				try {
					//index into local variable table in next entry
					pc++;
					int lvIndex = codeArray[pc];
					bcString = "istore\t\tlv#" + lvIndex;

					if(verify) {

						//check if stack item can be taken away
						if(stack.size() < 1) {

							throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
						}

						//check if value to be stored is of type integer
						if(stack.remove(0) != 1) {

							throw new VerifyError("Value on the stack for instruction " + 
								bcString + " was not an integer.");
						}

						//check if lvIndex is inside bounds
						if(lvIndex >= maxLocals || lvIndex < 0) {

							throw new VerifyError("Local variable index of instruction " + 
								bcString + " was out of bounds.");

						} else {

							locals[lvIndex] = 1;
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction istore at index " + pc);
				}

			} else if(bc == BC.ASTORE) {

				try {
					//index in next entry
					pc++;
					int lvIndex = codeArray[pc];
					bcString = "astore\t\tlv#" + lvIndex;

					if(verify) {

						//check if stack item can be taken away
						if(stack.size() < 1) {

							throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
						}

						//check if value to be stored is of type reference
						if(stack.remove(0) != 2) {

							throw new VerifyError("Value on the stack for instruction " + 
								bcString + " was not an integer.");
						}

						//check if lvIndex is inside bounds
						if(lvIndex >= maxLocals || lvIndex < 0) {

							throw new VerifyError("Local variable index of instruction " + 
								bcString + " was out of bounds.");
						
						} else {

							locals[lvIndex] = 2;
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction astore at index " + pc);
				}

			} else if(bc >= BC.ISTORE_0 && bc <= BC.ISTORE_3) {

				int index = 0;

				if(bc == BC.ISTORE_0) {

					bcString = "istore_0";

				} else if(bc == BC.ISTORE_1) {

					bcString = "istore_1";
					index = 1;

				} else if(bc == BC.ISTORE_2) {

					bcString = "istore_2";
					index = 2;

				} else if(bc == BC.ISTORE_3) {

					bcString = "istore_3";
					index = 3;
				}

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if value to be stored is of type integer
					if(stack.remove(0) != 1) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not an integer.");
					}

					//check if lvIndex is inside bounds
					if(index >= maxLocals || index < 0) {

						throw new VerifyError("Local variable index of instruction " + 
							bcString + " was out of bounds.");
					
					} else {

						locals[index] = 1;
					}
				}

			} else if(bc >= BC.ASTORE_0 && bc <= BC.ASTORE_3) {

				int index = 0;

				if(bc == BC.ASTORE_0) {

					bcString = "astore_0";

				} else if(bc == BC.ASTORE_1) {

					bcString = "astore_1";
					index = 1;

				} else if(bc == BC.ASTORE_2) {

					bcString = "astore_2";
					index = 2;

				} else if(bc == BC.ASTORE_3) {

					bcString = "astore_3";
					index = 3;
				}

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if value to be stored is of type reference
					if(stack.remove(0) != 2) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not a reference.");
					}

					//check if lvIndex is inside bounds
					if(index >= maxLocals || index < 0) {

						throw new VerifyError("Local variable index of instruction " + 
							bcString + " was out of bounds.");
					
					} else {

						locals[index] = 2;
					}
				}

			} else if(bc == BC.IASTORE) {

				bcString = "iastore";

				//checking of index inside array bounds: at runtime -> in ExecutionEngine
				if(verify) {

					//check if three stack items can be taken away
					if(stack.size() < 3) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if array value is of type integer (1)
					if(stack.remove(0) != 1) {

						throw new VerifyError("Array value for instruction " + bcString + 
							" was not an integer.");
					}

					//check if array index is of type integer (1)
					if(stack.remove(0) != 1) {

						throw new VerifyError("Array index for instruction " + bcString + 
							" was not an integer.");
					}

					//check if array reference is of type reference (2)
					if(stack.remove(0) != 2) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not a reference.");
					}
				}

			} else if(bc == BC.POP) {

				bcString = "pop";

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");

					} else {

						//simulate a simplified stack
						stack.remove(0);
					}
				}

			} else if(bc == BC.POP2) {

				bcString = "pop2";

				if(verify) {

					//check if 2 stack items can be taken away
					if(stack.size() < 2) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");

					} else {

						//simulate a simplified stack
						stack.remove(0);
						stack.remove(0);
					}
				}

			} else if(bc == BC.DUP) {

				bcString = "dup";

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 2) {

						//receive type of stack entry
						int type = stack.remove(0);

						//simulate a simplified stack
						if(type == 1 || type == 2) {

							stack.add(0, type);
							stack.add(0, type);

						} else {

							throw new VerifyError("A type on the operand stack " + 
								"was neither integer nor reference! What could have happened? :)");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.DUP_X1) {

				bcString = "dup_x1";

				if(verify) {

					//check if 2 stack items can be taken away
					if(stack.size() < 2) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 3) {

						//receive types of stack entries
						int top = stack.remove(0);
						int sub = stack.remove(0);

						//simulate a simplified stack
						if((top == 1 || top == 2) && (sub == 1 || sub == 2)) {

							stack.add(0, top);
							stack.add(0, sub);
							stack.add(0, top);

						} else {

							throw new VerifyError("A type on the operand stack " + 
								"was neither integer nor reference! What could have happened? :)");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.DUP_X2) {

				bcString = "dup_x2";

				if(verify) {

					//check if 3 stack items can be taken away
					if(stack.size() < 3) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 4) {

						//receive types of stack entries
						int top = stack.remove(0);
						int sub = stack.remove(0);
						int third = stack.remove(0);

						//simulate a simplified stack
						if((top == 1 || top == 2) && (sub == 1 || sub == 2) &&
							(third == 1 || third == 2)) {

							stack.add(0, top);
							stack.add(0, third);
							stack.add(0, sub);
							stack.add(0, top);

						} else {

							throw new VerifyError("A type on the operand stack " + 
								"was neither integer nor reference! What could have happened? :)");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.SWAP) {

				bcString = "swap";

				if(verify) {

					//check if 2 stack items can be taken away
					if(stack.size() < 2) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 2) {

						//receive types of stack entries
						int top = stack.remove(0);
						int sub = stack.remove(0);

						//simulate a simplified stack
						if((top == 1 || top == 2) && (sub == 1 || sub == 2)) {
							
							stack.add(0, top);
							stack.add(0, sub);

						} else {

							throw new VerifyError("A type on the operand stack " + 
								"was neither integer nor reference! What could have happened? :)");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.IADD || bc == BC.ISUB || 
				bc == BC.IMUL || bc == BC.IDIV || bc == BC.IREM) {

				if(bc == BC.IADD) {

					bcString = "iadd";

				} else if(bc == BC.ISUB) {

					bcString = "isub";

				} else if(bc == BC.IMUL) {

					bcString = "imul";

				} else if(bc == BC.IDIV) {

					bcString = "idiv";

				} else if(bc == BC.IREM) {

					bcString = "irem";

				} else if(bc == BC.ISHL) {

					bcString = "ishl";

				} else if(bc == BC.ISHR) {

					bcString = "ishr";

				} else if(bc == BC.IUSHR) {

					bcString = "iushr";

				} else if(bc == BC.IAND) {

					bcString = "iand";

				} else if(bc == BC.IOR) {

					bcString = "ior";

				} else if(bc == BC.IXOR) {

					bcString = "ixor";
				}

				if(verify) {

					//check if 2 stack items can be taken away
					if(stack.size() < 2) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 1) {

						//receive types of stack entries
						int top = stack.remove(0);
						int sub = stack.remove(0);

						//check if both values are of type integer
						if(top == 1 && sub == 1) {
							
							stack.add(0, 1);

						} else {

							throw new VerifyError("Operands for instruction " + 
								bcString + " were not of type integer.");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.INEG) {

				bcString = "ineg";

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//look out for stack overflow
					if(maxStack >= 1) {

						//receive types of stack entries
						int top = stack.remove(0);

						//check if both values are of type integer
						if(top == 1) {
							
							stack.add(0, 1);

						} else {

							throw new VerifyError("Operands for instruction " + 
								bcString + " were not of type integer.");
						}
					
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.IINC) {

				try {
					//index in next entry
					pc++;
					int lvIndex = codeArray[pc];

					//increment value in next entry
					pc++;
					int incValue = codeArray[pc];

					bcString = "iinc\t\tlv#" + lvIndex + "\t" + incValue;

					if(verify) {

						//check if lvIndex is a valid index into local variable table
						if(lvIndex < 0 || lvIndex >= maxLocals) {

							throw new VerifyError("Local variable index of instruction " + 
								bcString + " was out of bounds.");
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction iinc at index " + pc);
				}

			} else if(bc >= BC.IFEQ && bc <= BC.GOTO) {

				try {
					//jump to signed offset (from current instruction number) in next two entries
					short jOffset = (short)codeArray[pc + 1];
					jOffset = (short)((jOffset << 8) + codeArray[pc + 2]);

					int jIndex = pc + jOffset;
					pc = pc + 2;

					if(secondCall && !instructionStarts.contains(jIndex)) {

						throw new VerifyError(": Jump index " + jIndex + 
							" does not indicate the start of an instruction.");
					}

					if(verify) {

						if(bc >= BC.IFEQ && bc <= BC.IFLE) {

							//check if stack item can be taken away
							if(stack.size() < 1) {

								throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
							}

							//check if value on stack is of type integer
							if(stack.remove(0) != 1) {

								throw new VerifyError("Stack value for instruction " + 
									bcString + " was not an integer.");
							}

						} else if(bc >= BC.IF_ICMPEQ && bc <= BC.IF_ICMPLE) {

							//check if 2 stack items can be taken away
							if(stack.size() < 2) {

								throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
							}

							//check if values on stack are of type integer
							if(stack.remove(0) != 1) {

								throw new VerifyError("First stack value for instruction " + 
									bcString + " was not an integer.");

							} else if(stack.remove(0) != 1) {

								throw new VerifyError("Second stack value for instruction " + 
									bcString + " was not an integer.");
							}

						} else if(bc == BC.IF_ACMPEQ || bc == BC.IF_ACMPNE) {

							//check if 2 stack items can be taken away
							if(stack.size() < 2) {

								throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
							}

							//check if values on stack are of type reference
							if(stack.remove(0) != 2) {

								throw new VerifyError("Frist stack value for instruction " + 
									bcString + " was not a reference.");

							} else if(stack.remove(0) != 2) {

								throw new VerifyError("Second stack value for instruction " + 
									bcString + " was not a reference.");
							}
						}
					}

					if(bc == BC.IFEQ) {

						bcString = "ifeq\t\tbc#" + jIndex;

					} else if(bc == BC.IFNE) {

						bcString = "ifne\t\tbc#" + jIndex;

					} else if(bc == BC.IFLT) {

						bcString = "iflt\t\tbc#" + jIndex;

					} else if(bc == BC.IFGE) {

						bcString = "ifge\t\tbc#" + jIndex;

					} else if(bc == BC.IFGT) {

						bcString = "ifgt\t\tbc#" + jIndex;

					} else if(bc == BC.IFLE) {

						bcString = "ifle\t\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPEQ) {

						bcString = "if_icmpeq\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPNE) {

						bcString = "if_icmpne\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPLT) {

						bcString = "if_icmplt\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPGE) {

						bcString = "if_icmpge\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPGT) {

						bcString = "if_icmpgt\tbc#" + jIndex;

					} else if(bc == BC.IF_ICMPLE) {

						bcString = "if_icmple\tbc#" + jIndex;

					} else if(bc == BC.IF_ACMPEQ) {

						bcString = "if_acmpeq\tbc#" + jIndex;

					} else if(bc == BC.IF_ACMPNE) {

						bcString = "if_acmpne\tbc#" + jIndex;

					} else if(bc == BC.GOTO) {

						bcString = "goto\t\tbc#" + jIndex;
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("The code array ended in the " + 
						"middle of a jump instruction at index " + pc);
				}

			} else if(bc == BC.IRETURN) {

				bcString = "ireturn";

				if(verify) {

					//check if return type is integer (ret == true)
					if(!retInt) {

						throw new VerifyError("The method did not have an integer " + 
							"return type as required by instruction " + bcString + ".");
					}

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if value on stack is integer
					if(stack.remove(0) != 1) {

						throw new VerifyError("The stack entry to be returned " + 
							"did not contain an integer type.");
					}

					//delete all other items in the stack
					stack.clear();
				}

			} else if(bc == BC.RETURN) {

				bcString = "return";

				if(verify) {

					//check if return type is void (ret == false)
					if(retInt) {

						throw new VerifyError("The method did not have an empty " + 
							"return type as required by instruction " + bcString + ".");
					}

					//delete all other items in the stack
					stack.clear();
				}

			} else if(bc >= BC.GETSTATIC && bc <= BC.NEW) {

				try {
					//constant pool index in next two bytes
					int cpIndex = codeArray[pc + 1];
					cpIndex = (cpIndex << 8) + codeArray[pc + 2];
					pc = pc + 2;

					if(bc == BC.GETSTATIC) {

						bcString = "getstatic\tcp#" + cpIndex;
						RCPFieldOrMethEntry entry = null;

						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							entry = (RCPFieldOrMethEntry)(rcp.getEntry(cpIndex));

							//check for appropriate constant pool entry type
							if(entry.tag != ClassLoader.CP_FIELD_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an appropriate " + 
									"type for instruction" + bcString + ".");
							}

							//check if static, only if field of own class
							//(if not: check at runtime)
							boolean ownClass = rcp.getUTF8(rcp.getClassNameIndex(
								rcp.getClassIndex(cpIndex))).equals(rcp.className);
							
							if(ownClass) {

								if(!entry.isStatic) {

									throw new IncompatibleClassChangeError("The constant pool " + 
										"entry at " + cpIndex + " for instruction " + bcString + 
										" did not contain a static field.");
								}
							}

							//check if stack would overflow
							if(stack.size() < maxStack) {

								//check type of field
								if(rcp.getUTF8(rcp.getTypeIndex(
									rcp.getNameTypeIndex(cpIndex))).equals("I")) {

									//simulate a simplified stack
									stack.add(0, 1);

								} else {

									stack.add(0, 2);
								}

							} else {

								throw new VerifyError("Executing " + bcString + 
									" would lead to stack overflow.");
							}								
						}

					} else if(bc == BC.PUTSTATIC) {

						bcString = "putstatic\tcp#" + cpIndex;
						RCPFieldOrMethEntry entry = (RCPFieldOrMethEntry)(rcp.getEntry(cpIndex));

						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							//check for appropriate constant pool entry type
							if(entry.tag != ClassLoader.CP_FIELD_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an appropriate " + 
									"type for instruction" + bcString + ".");
							}

							//check if static
							if(!entry.isStatic) {

								throw new IncompatibleClassChangeError("The constant pool " + 
									"entry at " + cpIndex + " for instruction " + bcString + 
										" did not contain a static field.");
							}

							//check if stack item can be taken away
							if(stack.size() < 1) {

								throw new VerifyError("Stack would not contain enough " + 
									"entries for instruction " + bcString + ".");
							}

							//check if type of field matches type of stack entry
							if(rcp.getUTF8(rcp.getTypeIndex(
								rcp.getNameTypeIndex(cpIndex))).equals("I")) {

								if(stack.remove(0) != 1) {

									throw new VerifyError("Value on the stack for " + 
										"instruction " + bcString + " was not an integer, " +
										"when the required constant pool entry was an integer field.");
								}

							} else {

								if(stack.remove(0) != 2) {

									throw new VerifyError("Value on the stack for " + 
										"instruction " + bcString + " was not a reference, " +
										"when the required constant pool entry was a reference field.");
								}
							}

							//check if final field
							if(entry.isFinal) {

								throw new IllegalAccessError(bcString + 
									" must not reference a final field.");
							}		
						}

					} else if(bc == BC.GETFIELD) {

						bcString = "getfield\t\tcp#" + cpIndex;
						RCPFieldOrMethEntry entry = (RCPFieldOrMethEntry)(rcp.getEntry(cpIndex));

						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							//check for appropriate constant pool entry type
							if(entry.tag != ClassLoader.CP_FIELD_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an appropriate " + 
									"type for instruction" + bcString + ".");
							}

							//check if static
							if(entry.isStatic) {

								throw new IncompatibleClassChangeError("The constant pool " + 
									"entry at " + cpIndex + " for instruction " + bcString + 
										" did contain a static field.");
							}

							//check if stack item can be taken away
							if(stack.size() < 1) {

								throw new VerifyError("Stack would not contain enough " + 
									"entries for instruction " + bcString + ".");
							}

							//check object reference on stack
							if(stack.remove(0) != 2) {

								throw new VerifyError("Value on the stack for instruction " + 
									bcString + " was not a reference.");
							}

							//check if stack would overflow
							if(stack.size() < maxStack) {

								//check type of field
								if(rcp.getUTF8(rcp.getTypeIndex(
									rcp.getNameTypeIndex(cpIndex))).equals("I")) {

									//simulate a simplified stack
									stack.add(0, 1);

								} else {

									stack.add(0, 2);
								}

							} else {

								throw new VerifyError("Executing " + bcString + 
									" would lead to stack overflow.");
							}								
						}

					} else if(bc == BC.PUTFIELD) {

						bcString = "putfield\t\tcp#" + cpIndex;
						RCPFieldOrMethEntry entry = (RCPFieldOrMethEntry)(rcp.getEntry(cpIndex));

						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							//check for appropriate constant pool entry type
							if(entry.tag != ClassLoader.CP_FIELD_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an appropriate " + 
									"type for instruction" + bcString + ".");
							}

							//check if static
							if(entry.isStatic) {

								throw new IncompatibleClassChangeError("The constant pool " + 
									"entry at " + cpIndex + " for instruction " + bcString + 
										" did contain a static field.");
							}

							//check if 2 stack items can be taken away
							if(stack.size() < 2) {

								throw new VerifyError("Stack would not contain enough " + 
									"entries for instruction " + bcString + ".");
							}

							//check if type of field matches type of stack entry
							if(rcp.getUTF8(rcp.getTypeIndex(
								rcp.getNameTypeIndex(cpIndex))).equals("I")) {

								if(stack.remove(0) != 1) {

									throw new VerifyError("Value on the stack for " + 
										"instruction " + bcString + " was not an integer, " +
										"when the required constant pool entry was an integer field.");
								}

							} else {

								if(stack.remove(0) != 2) {

									throw new VerifyError("Value on the stack for " + 
										"instruction " + bcString + " was not a reference, " +
										"when the required constant pool entry was a reference field.");
								}
							}

							//check if reference on stack
							if(stack.remove(0) != 2) {

								throw new VerifyError("Argument on the stack for instruction " + 
									bcString + " was not of type reference.");
							}

							//check if final field
							if(entry.isFinal && !methodName.equals("<init>")) {

								throw new IllegalAccessError(bcString + 
									" must not reference a final field.");
							}
						}

					} else if(bc == BC.INVOKEVIRTUAL || bc == BC.INVOKESPECIAL || 
						bc == BC.INVOKESTATIC) {

						if(bc == BC.INVOKEVIRTUAL) {

							bcString = "invokevirtual\tcp#" + cpIndex;
						
						} else if(bc == BC.INVOKESPECIAL) {

							bcString = "invokespecial\tcp#" + cpIndex;

						} else if(bc == BC.INVOKESTATIC) {

							bcString = "invokestatic\tcp#" + cpIndex;
						}

						RCPFieldOrMethEntry entry = null;

						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							entry = (RCPFieldOrMethEntry)(rcp.getEntry(cpIndex));
							
							//check for appropriate constant pool entry type
							if(entry.tag != ClassLoader.CP_METHOD_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an " + 
									"appropriate type for instruction" + bcString + ".");
							}
							
							String methType = rcp.getUTF8(rcp.getTypeIndex(
								rcp.getNameTypeIndex(cpIndex)));

							//find out number and type of parameters
							//to know number and type of stack items to remove
							String paras = methType.substring(methType.indexOf("(") + 1, methType.indexOf(")"));
							int parCount = 0;

							int j = 0;
							while(j < paras.length()) {

								int paraType = 0;

								//integer
								if(paras.charAt(j) == 'I') {

									j++;
									paraType = 1;

								//integer array
								} else if(paras.charAt(j) == '[') {

									j = j + 2;
									paraType = 2;

								//reference
								} else {

									j = paras.indexOf(";", j) + 1;
									paraType = 2;
								}

								//check if stack item can be taken away
								if(stack.size() < 1) {

									throw new VerifyError("Stack would not contain enough " + 
										"entries for instruction " + bcString + ".");
								}

								//check if type of parameter matches type of stack entry
								if(stack.remove(0) != paraType) {

									String t = (paraType == 1) ? "integer" : "reference";
									throw new VerifyError("Argument on the stack for " + 
										"instruction " + bcString + " was not of type " +
										t + "when the required parameter type was " + t + ".");
								}
							}

							//if not invokestatic: take reference from stack
							if(bc != BC.INVOKESTATIC) {
							
								//check if stack item can be taken away
								if(stack.size() < 1) {

									throw new VerifyError("Stack would not contain enough " + 
										"entries for instruction " + bcString + ".");
								}

								//check if reference on stack
								if(stack.remove(0) != 2) {

									throw new VerifyError("Argument on the stack for instruction " + 
										bcString + " was not of type reference.");
								}
							}

							//find out return type
							String ret = methType.substring(methType.indexOf(")") + 1, methType.length());

							if(ret.equals("I")) {

								//check if stack would overflow
								if(stack.size() < maxStack) {

									//simulate a simplified stack
									stack.add(0, 1);

								} else {

									throw new VerifyError("Executing " + bcString + 
										" would lead to stack overflow.");
								}
							}
							
							if(bc != BC.INVOKESTATIC) {

								//check if static
								if(entry.isStatic) {

									throw new IncompatibleClassChangeError("The constant pool " + 
										"entry at " + cpIndex + " for instruction " + bcString + 
										" did contain a static method.");
								}

								if(bc == BC.INVOKEVIRTUAL) {

									//check if name is <init>
									String methName = rcp.getUTF8(rcp.getNameIndex(
										rcp.getNameTypeIndex(cpIndex)));

									if(methName.equals("<init>")) {

										throw new VerifyError("The method name for instruction " + 
											bcString + " was <init>, which can only be invoked " + 
											"by instruction invokespecial.");
									}
								}
							}
						}

					} else if(bc == BC.NEW) {

						bcString = "new\t\tcp#" + cpIndex;
						
						if(verify) {

							//check if cpIndex is a valid index into the constant pool
							if(cpIndex < 1 || cpIndex >= rcp.size()) {

								throw new VerifyError("The constant pool index " + 
									"of instrucion " + bcString + " was out of bounds.");
							}

							//check for appropriate constant pool entry type
							if(rcp.getEntry(cpIndex).tag != ClassLoader.CP_CLASS_REF) {

								throw new VerifyError("The constant pool entry at " + 
									cpIndex + " did not contain an " + 
									"appropriate type for instruction" + bcString + ".");
							}

							//check if stack would overflow
							if(stack.size() < maxStack) {

								//simulate a simplified stack
								stack.add(0, 2);

							} else {

								throw new VerifyError("Executing " + bcString + 
									" would lead to stack overflow.");
							}
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("The code array ended in the middle " + 
						"of instruction " + bcString + "at index " + pc);
				}

			} else if(bc == BC.NEWARRAY) {

				try {
					//array type encoded in next byte
					pc++;
					int typeIndex = codeArray[pc];

					//type code for int: 10
					if(typeIndex == 10) {

						bcString = "newarray\t\tint";
						
					} else {

						throw new VerifyError("The array type " + typeIndex + 
							"did not represent integer.");
					}

					if(verify) {

						//check if stack item can be taken away
						if(stack.size() < 1) {

							throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
						}

						//check if array size is of type integer (1)
						if(stack.remove(0) != 1) {

							throw new VerifyError("Value on the stack for instruction " + 
								bcString + " was not an integer.");
						}

						//look out for stack overflow
						if(stack.size() < maxStack) {

							//simulate a simplified stack
							stack.add(0, 2);
					
						} else {

							throw new VerifyError("Executing " + bcString + 
								" would lead to stack overflow.");
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("The code array ended in the middle " + 
						"of instruction newarray at index " + pc);
				}

			} else if(bc == BC.ARRAYLENGTH) {

				bcString = "arraylength";

				if(verify) {

					//check if stack item can be taken away
					if(stack.size() < 1) {

						throw new VerifyError("Stack would not contain enough " + 
							"entries for instruction " + bcString + ".");
					}

					//check if array reference is of type reference (2)
					if(stack.remove(0) != 2) {

						throw new VerifyError("Value on the stack for instruction " + 
							bcString + " was not a reference.");
					}

					//look out for stack overflow
					if(stack.size() < maxStack) {

						//simulate a simplified stack
						stack.add(0, 1);
				
					} else {

						throw new VerifyError("Executing " + bcString + 
							" would lead to stack overflow.");
					}
				}

			} else if(bc == BC.WIDE) {

				bcString = "wide\n";

				try {
					//opcode in next byte
					pc++;
					int opCode = codeArray[pc];

					if(opCode == BC.IINC) {

						//local variable table index in next two entries
						int lvIndex = codeArray[pc + 1];
						lvIndex = (lvIndex << 8) + codeArray[pc + 2];

						//increment value in next two entries
						int incValue = codeArray[pc + 3];
						incValue = (incValue << 8) + codeArray[pc + 4];

						bcString = bcString + "iinc\t\tlv#" + lvIndex + "\t" + incValue;
						pc = pc + 4;

						if(verify) {

							//check if lvIndex is a valid index into local variable table
							if(lvIndex < 0 || lvIndex >= maxLocals) {

								throw new VerifyError("Local variable index of instruction " + 
									bcString + " was out of bounds.");
							}
						}

					} else if(opCode == BC.ILOAD) {

						//local variable table index in next two entries
						int lvIndex = codeArray[pc + 1];
						lvIndex = (lvIndex << 8) + codeArray[pc + 2];

						bcString = bcString + "iload\t\tlv#" + lvIndex;
						pc = pc + 2;

						if(verify) {

							//check if valid index into local variable table
							if(lvIndex >= maxLocals || lvIndex < 0) {

								throw new VerifyError("Local variable index at instruction " + 
									bcString + " was out of bounds.");
							}

							//check if variable table entry is of type integer (1)
							if(locals[lvIndex] != 1) {

								throw new VerifyError("Local variable at instruction " + 
									bcString + " did not contain an integer type.");
							}

							//look out for stack overflow
							if(stack.size() < maxStack) {

								//simulate a simplified stack
								stack.add(0, 1);

							} else {

								throw new VerifyError("Executing " + bcString + 
									" would lead to stack overflow.");
							}
						}

					} else if(opCode == BC.ALOAD) {

						int lvIndex = codeArray[pc + 1];
						lvIndex = (lvIndex << 8) + codeArray[pc + 2];

						bcString = bcString + "aload\t\tlv#" + lvIndex;
						pc = pc + 2;

						if(verify) {

							//check if valid index into local variable table
							if(lvIndex >= maxLocals || lvIndex < 0) {

								throw new VerifyError("Local variable index at instruction " + 
									bcString + " was out of bounds.");
							}

							//check if variable table entry is of type reference (2)
							if(locals[lvIndex] != 2) {

								throw new VerifyError("Local variable at instruction " + 
									bcString + " did not contain an integer type.");
							}

							//look out for stack overflow
							if(stack.size() < maxStack) {

								//simulate a simplified stack
								stack.add(0, 2);

							} else {

								throw new VerifyError("Executing " + bcString + 
									" would lead to stack overflow.");
							}
						}

					} else if(opCode == BC.ISTORE) {

						int lvIndex = codeArray[pc + 1];
						lvIndex = (lvIndex << 8) + codeArray[pc + 2];

						bcString = bcString + "istore\t\tlv#" + lvIndex;
						pc = pc + 2;

						if(verify) {

							//check if stack item can be taken away
							if(stack.size() < 1) {

								throw new VerifyError("Stack would not contain enough " + 
									"entries for instruction " + bcString + ".");
							}

							//check if value to be stored is of type integer (1)
							if(stack.remove(0) != 1) {

								throw new VerifyError("Value on the stack for instruction " + 
									bcString + " was not an integer.");
							}

							//check if lvIndex is inside bounds
							if(lvIndex >= maxLocals || lvIndex < 0) {

								throw new VerifyError("Local variable index of instruction " + 
									bcString + " was out of bounds.");

							} else {

								locals[lvIndex] = 1;
							}
						}

					} else if(opCode == BC.ASTORE) {

						int lvIndex = codeArray[pc + 1];
						lvIndex = (lvIndex << 8) + codeArray[pc + 2];

						bcString = bcString + "istore\t\tlv#" + lvIndex;
						pc = pc + 2;

						if(verify) {

							//check if stack item can be taken away
							if(stack.size() < 1) {

								throw new VerifyError("Stack would not contain enough " + 
									"entries for instruction " + bcString + ".");
							}

							//check if value to be stored is of type reference (2)
							if(stack.remove(0) != 2) {

								throw new VerifyError("Value on the stack for instruction " + 
									bcString + " was not an integer.");
							}

							//check if lvIndex is inside bounds
							if(lvIndex >= maxLocals || lvIndex < 0) {

								throw new VerifyError("Local variable index of instruction " + 
									bcString + " was out of bounds.");

							} else {

								locals[lvIndex] = 2;
							}
						}
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("The code array ended in " + 
						"the middle of instruction wide at index " + pc);
				}

			} else if(bc == BC.IFNULL || bc == BC.IFNONNULL) {

				try {
					//signed jump offset in next two entries
					short jOffset = (short)codeArray[pc + 1];
					jOffset = (short)((jOffset << 8) + codeArray[pc + 2]);
					int jIndex = pc + jOffset;

					if(bc == BC.IFNULL) {

						bcString = "ifnull\tbc#" + jIndex;

					} else {

						bcString = "ifnonnull\tbc#" + jIndex;
					}

					if(secondCall && !instructionStarts.contains(jIndex)) {

						throw new VerifyError("Jump index " + jIndex + " of instruction " + 
							bcString + " did not indicate the start of an instruction.");
					}

					if(verify) {


						//check if stack item can be taken away
						if(stack.size() < 1) {

							throw new VerifyError("Stack would not contain enough " + 
								"entries for instruction " + bcString + ".");
						}

						//check if value to be stored is of type integer
						if(stack.remove(0) != 2) {

							throw new VerifyError("Value on the stack for instruction " + 
								bcString + " was not a reference.");
						}
					}
					
				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("Error at BytecodeVerifier.processBC(): The code array " + 
						"ended in the middle of instruction ifnull at index " + pc);
				}

				pc = pc + 2;
				
			} else if(bc == BC.GOTO_W) {

				try {
					//signed jump offset in next four entries
					int jOffset = (codeArray[pc + 1] << 24) + (codeArray[pc + 2] << 16)
						+ (codeArray[pc + 3] << 8) + (codeArray[pc + 4]);

					int jIndex = pc + jOffset;
					bcString = "goto_w\tbc#" + jIndex;

					if(secondCall && !instructionStarts.contains(jIndex)) {

						throw new VerifyError("Error at BytecodeVerifier.processBC(): Jump index " + 
							jIndex + " does not indicate the start of an instruction.");
					}

				} catch(ArrayIndexOutOfBoundsException e) {

					throw new VerifyError("The code array " + 
						"ended in the middle of instruction wide at index " + pc);
				}

				pc = pc + 4;

			} else {

				bcString = "no bytecode of TinyJava";
			}

			if(debug && !secondCall) {

				System.out.println(space + pcString + bcString);
			}

			pc++;
		}

		if(verify && !secondCall) {

			processBC(start, stop, debug, verify, true);
		}

		return true;
	}
}
