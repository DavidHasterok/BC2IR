import java.util.Vector;


public class BC2IR {

public static final short REGISTER = 0;        
public static final short INT_CONSTANT = 1;	
public static final short TINY_JAVA_REFERENCE = 2;
public static final short INTEGER = 3;
public static final short VOID = 4;
public static final short INT_RETURN = 5;
public static final short REF_RETURN = 6;
public static final short TRAP = 7;
public static final short BRANCH = 8;
public static final short FIELD = 9;
public static final short TYPEREF = 10;

Register returnRegister = new Register(999);
Instruction lastInstr = new Instruction(); //Initialisierung für Instruktionsfluss
BasicBlock firstBB = new BasicBlock(1);
int basicblockcounter = 1;
BasicBlock currentBB;
boolean endBB = false;
Heap heap;
ClassLoader classLoader;
FrameStack fs;
Frame currentFrame;
Vector<Register> temps = new Vector<Register>();
Vector<Frame> doneFrames = new Vector<Frame>();
Operand[] localVars = new Operand[1];


BC2IR(Heap heap, ClassLoader classLoader, Frame mainFrame){
currentBB = firstBB;
currentBB.first = lastInstr;
//currentBB.size = 1;
this.heap = heap;
this.classLoader = classLoader;
this.fs =  heap.fs;
currentFrame = mainFrame;
}



void produce ( Frame f, int start, int end, Register[] registers){
	Instruction s = new Instruction();
	int[] code = f.codeArray;
	OPstack stack = f.registerOpStack;	

//	boolean codeIsDone = false;
	
//	for(int j = 0; j<doneFrames.size(); j++){
	//	if(code.equals(doneFrames.elementAt(j).codeArray)){ 
		//	currentBB.branch = copyBB(doneFrames.elementAt(j).first); 
			//currentBB.last.next = currentBB.branch.first;
		//	currentBB = doneFrames.elementAt(j).last;
			//lastInstr = currentBB.last; 
			//codeIsDone = true;} 
		//}
			
//	if (codeIsDone){} 
	//else{
	for(int i=start; i<end; i++) {
		display(code[i]); 
		switch(code[i]){
		
		case BC.NOP: 			{break;}	//no operation

		case BC.ICONST_0:  	//push constant 0,...,5 onto stack
		case BC.ICONST_1: 
		case BC.ICONST_2: 
		case BC.ICONST_3:
		case BC.ICONST_4:
		case BC.ICONST_5:		{stack.push(new Operand(INT_CONSTANT, code[i] - BC.ICONST_0));
								break;}

		case BC.BIPUSH: 		{stack.push(new Operand(INT_CONSTANT, code[++i]));
								f.pc++;
								break;}	//push byte onto stack, treated as integer
		
		case BC.SIPUSH: 		{int shortval = code[++i]; 
								shortval = (shortval << 8) + code[++i];
								f.pc = f.pc+2;
								stack.push(new Operand(INT_CONSTANT,shortval));
								break;}	//push short onto stack, treated as integer
		
		case BC.LDC: 			{stack.push(new Operand(TINY_JAVA_REFERENCE,(f.rcp.getConst(code[++i])))); 
								f.pc++;
								break;}	//push constant from constant pool onto stack, index specified by following byte
		
		case BC.LDC_W: 			{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
								f.pc = f.pc+2;
								stack.push(new Operand(TINY_JAVA_REFERENCE,(f.rcp.getConst(cpIndex)))); 
								break;}	//same as LDC with increased range of indices, index specified by following two bytes (in big endian order)
			
		case BC.ILOAD: 			{do_iload(stack, code[++i], registers, f.locVars); 
					   			f.pc++;
								//if(registers[code[i]] == null) registers[code[i]]= stack.stack[stack.top].register;
					   			break;}		//push integer from local variable table onto stack, index specified by following byte
	
				
		case BC.ILOAD_0: 	//push integer from loc var tab at index 0,...,3 onto stack
		case BC.ILOAD_1: 
		case BC.ILOAD_2: 
		case BC.ILOAD_3: 		{do_iload(stack, code[i] - BC.ILOAD_0, registers, f.locVars);
								
								break;}

		case BC.ALOAD: 			{do_aload(stack, code[++i], registers, f.locVars); 
								f.pc++;
								break;}	//push reference from loc var tab onto stack, index in following byte
		
		case BC.ALOAD_0: 	//push reference from loc var tab at index 0,...3 onto stack
		case BC.ALOAD_1:
		case BC.ALOAD_2: 
		case BC.ALOAD_3: 		{do_aload(stack, code[i] - BC.ALOAD_0, registers, f.locVars); 
								break;}

		case BC.IALOAD: 		{Operand arrayIndex = stack.pop(); iftempremove(arrayIndex);
								Operand arrayRef = stack.pop(); iftempremove(arrayRef);
								s = aloadHelper(BC.IALOAD, arrayRef, arrayIndex, stack, temps);
								s.marker = f.pc;
								break;}		//push integer from array onto stack
				
		case BC.ISTORE: 		{Operand op = stack.pop();
								s = do_store(code[i], code[++i], op, registers); 
								iftempremove(op);
								f.pc++;
								s.marker = f.pc;
								break;} //store integer from stack into local variable at index specified by next byte
									
		case BC.ISTORE_0: 	//store integer from stack into loc var at index 0,...,3
		case BC.ISTORE_1: 
		case BC.ISTORE_2: 
		case BC.ISTORE_3: 		{Operand op = stack.pop();
								s = do_store(code[i], code[i] - BC.ISTORE_0, op, registers); 
								iftempremove(op);
								s.marker = f.pc;
								break;}

		
		case BC.ASTORE: 		{Operand op = stack.pop(); iftempremove(op);
								s = do_store(code[i], code[++i], op, registers);
								f.pc++;
								s.marker = f.pc-1;
								break;}	//store reference from stack into loc var at index in next byte
		
		case BC.ASTORE_0: 	//store reference from stack into loc var at index 0,...,3
		case BC.ASTORE_1: 
		case BC.ASTORE_2: 
		case BC.ASTORE_3: 		{Operand op = stack.pop(); 
								s = do_store(code[i], code[i] - BC.ASTORE_0, op, registers); iftempremove(op);
								s.marker = f.pc;
								break;}
		
		case BC.IASTORE: 		{Operand value = stack.pop(); 
								Operand index =  stack.pop(); 
								Operand array = stack.pop(); 
								s = do_iastore(value, index, array);iftempremove(value);iftempremove(index);iftempremove(array);
								s.marker = f.pc;
								break;}	//store integer from stack into array

			
		case BC.POP: 			{Operand op = stack.pop(); iftempremove(op);
								break;}	//delete topmost stack value (e.g. if method returns void and stack is not empty yet)
		
		case BC.POP2:			{Operand op = stack.pop(); iftempremove(op);
								Operand op2 = stack.pop(); iftempremove(op);
								break;}	//delete topmost two stack values
		
		case BC.DUP: 			{Operand top = stack.pop();
								stack.push(top);
								if (top.kind == REGISTER && !top.register.isLocal){
									Operand top2 = new Operand(REGISTER, getTempCount(temps));
									temps.add(top2.register.number, top2.register);
									stack.push(top2);
								}else stack.push(top);
								break;}	//duplicate topmost stack value
		
		case BC.DUP_X1: 		{Operand top = stack.pop();
								Operand sub = stack.pop();
								stack.push(top);
								stack.push(sub);
								if (top.kind == REGISTER && !top.register.isLocal){
									Operand top2 = new Operand(REGISTER, getTempCount(temps));
									temps.add(top2.register.number, top2.register);
									stack.push(top2);
								}else stack.push(top);
								break;}	//copy topmost stack value two places underneath
						
		case BC.DUP_X2: 		{Operand top = stack.pop();
								Operand sub = stack.pop();
								Operand third = stack.pop();
								stack.push(top);
								stack.push(third);
								stack.push(sub);
								if (top.kind == REGISTER && !top.register.isLocal){
									Operand top2 = new Operand(REGISTER, getTempCount(temps));
									temps.add(top2.register.number, top2.register);
									stack.push(top2);
								}else stack.push(top);
								break;}	//in TinyJava: copy topmost stack value three places underneath

		
		case BC.SWAP: 			{Operand top = stack.pop();
								Operand sub = stack.pop();
								stack.push(top);
								stack.push(sub);
								break;}		//swap two topmost stack values

		case BC.IADD: 			{Operand op2 = stack.pop(); 
        			  			Operand op1 = stack.pop(); 
        			  			s = binaryInstruction(BC.IADD, op1, op2, stack, "+", temps);iftempremove(op2);iftempremove(op1);
        			  			s.marker = f.pc;
        			  			break;}	//add two integers
        			  
		case BC.ISUB: 			{Operand op2 = stack.pop(); 
		  			  			Operand op1 = stack.pop(); 
		  			  			s = binaryInstruction(BC.ISUB, op1, op2, stack, "-", temps);iftempremove(op2);iftempremove(op1);
		  			  			s.marker = f.pc;
		  			  			break;}		//subtract one integer from another
				
		case BC.IMUL: 			{Operand op2 = stack.pop(); 
		  			  			Operand op1 = stack.pop(); 
		  			  			s = binaryInstruction(BC.IMUL, op1, op2, stack, "x", temps);iftempremove(op2);iftempremove(op1);
		  			  			s.marker = f.pc;
		  			  			break;}		//multiply two integers
				
		case BC.IDIV: 			{Operand op2 = stack.pop(); 
								if (op2.kind == INT_CONSTANT && op2.value.value == 0) {
									endBB = true; 
									s = trap("divByZero"); iftempremove(op2);
									s.marker = f.pc;
									break;} 
								else {
									Operand op1 = stack.pop(); 
									s = binaryInstruction(BC.IDIV, op1, op2, stack, "/", temps);iftempremove(op2);iftempremove(op1);
									s.marker = f.pc;
									break;}}		//divide one integer by another
				
		case BC.IREM: 			{Operand op2 = stack.pop(); 
								if (op2.kind == INT_CONSTANT && op2.value.value == 0) {
										endBB = true; 
										s = trap("divByZero"); iftempremove(op2);
										s.marker = f.pc;
										break;} 
								else { 	
										Operand op1 = stack.pop();
										s = binaryInstruction(BC.IREM, op1, op2, stack, "mod", temps);iftempremove(op2); iftempremove(op1);
										s.marker = f.pc;
										break;}}	//integer remainder
				
		case BC.INEG: 			{Operand op = stack.pop(); 
								s = unaryInstruction(BC.INEG, op, stack, "negated", temps);iftempremove(op);
								s.marker = f.pc;
								break;}		//negate integer
			
		case BC.ISHL: 			{Operand shiftbits = stack.pop();
								Operand val = stack.pop(); 
								s = binaryInstruction(BC.ISHL, val, shiftbits, stack, "shift left by", temps); iftempremove(shiftbits);iftempremove(val);
								s.marker = f.pc;
								break;}	//integer left shift
			
		case BC.ISHR: 			{Operand shiftbits = stack.pop();
								Operand val = stack.pop(); 
								s = binaryInstruction(BC.ISHR, val, shiftbits, stack, "shift right arithmetically by", temps);iftempremove(shiftbits);iftempremove(val);
								s.marker = f.pc;
								break;}	//integer right shift (arithmetic, keeping the sign bit and distinguishing b/w mantissa and exponent)
				
		case BC.IUSHR: 			{Operand shiftbits = stack.pop(); 
								Operand val = stack.pop();
								s = binaryInstruction(BC.IUSHR, val, shiftbits, stack, "shift right logically by", temps);iftempremove(shiftbits);iftempremove(val);
								s.marker = f.pc;
								break;}	//integer right shift (logical, regardless of sign bit/mantissa/exponent)
			
		case BC.IAND:			{Operand op1 = stack.pop(); 
								Operand op2 = stack.pop(); 
								s = binaryInstruction(BC.IAND, op1, op2, stack, "AND", temps);iftempremove(op1);iftempremove(op2);
								s.marker = f.pc;
								break;}	//bitwise AND on two integers
			
		case BC.IOR: 			{Operand op1 = stack.pop(); 
								Operand op2 = stack.pop(); 
								s = binaryInstruction(BC.IOR, op1, op2, stack, "OR", temps);iftempremove(op1);iftempremove(op2);
								s.marker = f.pc;
								break;}	//bitwise OR on two integers
			
		case BC.IXOR: 			{Operand op1 = stack.pop(); 
								Operand op2 = stack.pop();
								s = binaryInstruction(BC.IXOR, op1, op2, stack, "XOR", temps);iftempremove(op1);iftempremove(op2);
								s.marker = f.pc;
								break;}	//bitwise XOR on two integers
				
		case BC.IINC: 			{int zahl = code[++i];
								int inc = code[++i];
								s = do_iinc(zahl, inc, stack, "incremented by", registers);
								f.pc = f.pc+2;
								s.marker = f.pc-2;
								break;}	//increment integer in loc var table at index in next byte by constant in byte after index byte

		case BC.IFEQ: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop(); 
								f.pc = f.pc + 2;
								s = ifIntHelper(f,BC.IFEQ, offset, op, "=", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if topmost stack entry is 0, jump to instruction indicated by next two bytes
		
		case BC.IFNE: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifIntHelper(f, BC.IFNE, offset, op, "!=", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								
								break;}	//if topmost stack entry is not 0, jump to instruction indicated by next two bytes
		
		case BC.IFLT: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifIntHelper(f, BC.IFLT, offset, op, "<", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if topmost stack entry is less than 0, jump to instruction indicated by next two bytes
		
		case BC.IFGE: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifIntHelper(f, BC.IFGE, offset, op, ">=", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if topmost stack entry is greater or equal to 0, jump to instruction indicated by next two bytes
		
		case BC.IFGT: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifIntHelper(f, BC.IFGT, offset, op, ">", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if topmost stack entry is greater than 0, jump to instruction indicated by next two bytes
		
		case BC.IFLE: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifIntHelper(f, BC.IFGT, offset, op, "<=", i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if topmost stack entry is less or equal to 0, jump to instruction indicated by next two bytes

		case BC.IF_ICMPEQ: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPEQ, offset, op1, op2, "=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if topmost two stack integer values are equal, jump to instruction indicated by next two bytes
		
		case BC.IF_ICMPNE: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPNE, offset, op1, op2, "!=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								i = s.gotoadress-1;
								
								//s.marker = f.pc-2;
								break;}	//if topmost two stack integer values are not equal, jump to instruction indicated by next two bytes
		
		case BC.IF_ICMPLT: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPLT, offset, op1, op2, "<", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if integer in second position from top on stack is less than integer in topmost stack position, jump to instruction indicated by next two bytes
		
		case BC.IF_ICMPGE: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPGE, offset, op1, op2, ">=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if second integer is greater or equal to topmost integer, jump to instruction indicated by next two bytes
		
		case BC.IF_ICMPGT: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPGT, offset, op1, op2, ">", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if second integer is greater than the topmost integer, jump to instruction indicated by next two bytes
		
		case BC.IF_ICMPLE: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ICMPLE, offset, op1, op2, "<=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if second integer less or equal to the topmost integer, jump to instruction indicated by next two bytes

		case BC.IF_ACMPEQ: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ACMPEQ, offset, op1, op2, "=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if topmost references are equal, jump to instruction indicated by next two bytes
		
		case BC.IF_ACMPNE: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op1 = stack.pop();
								Operand op2 = stack.pop();
								f.pc = f.pc + 2;
								s = CmpIntHelper(f, BC.IF_ACMPNE, offset, op1, op2, "!=", i-2, f.pc);iftempremove(op1);iftempremove(op2);
								
								//s.marker = f.pc-2;
								i = s.gotoadress-1;
								break;}	//if topmost references are not equal, jump to instruction indicated by next two bytes

		case BC.GOTO: 			{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								s = goToHelper(BC.GOTO, offset, i, f.pc);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}	//go to instruction indicated by next two bytes

			
		case BC.IRETURN: 		{Operand op = stack.pop(); 
								s = returnHelper(f, (short) BC.RETURN, op);iftempremove(op);
								s.marker = f.pc;
								break;}			//return integer from a method
				
		case BC.RETURN: 		{s = returnHelper(f, (short)BC.RETURN, null); 
								s.marker = f.pc;
								break;}				//return void from a method

		case BC.GETSTATIC: 		{int cpIndex = code[++i];
								cpIndex = cpIndex << 8;
								cpIndex = cpIndex + code[++i];
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
								String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
								
								Operand field = new Operand(FIELD, fieldName + " in class " + className, 0);
								s = getStatic(BC.GETSTATIC, field, field.className, stack, " loaded onto Stack", temps);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}	//push static field value onto stack, constant pool index specified by next two bytes
	
		
		case BC.PUTSTATIC: 		{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
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
								String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
								Operand field = new Operand(FIELD, fieldName + " in class " + className, 0);
								Operand val = stack.pop();
								s = putStatic(BC.PUTSTATIC, val, field, stack, " stored", temps); iftempremove(val);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}	//store topmost stack value into static field at cp index specified by next two bytes
		
		case BC.GETFIELD: 		{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
								Operand objRef = stack.pop(); 
								String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
								s = getField(BC.GETFIELD, objRef, fieldName, stack, " pushed onto stack", temps);iftempremove(objRef);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}		//push field value in cp indicated by next two bytes onto stack
		
		case BC.PUTFIELD: 		{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
								Operand op = stack.pop();iftempremove(op);
								Operand objRef = stack.pop();
								String fieldName = f.rcp.getUTF8(f.rcp.getNameIndex(f.rcp.getNameTypeIndex(cpIndex)));
								s = putField(BC.PUTFIELD, op, objRef, stack, " stored in Field " + fieldName, temps);iftempremove(objRef);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}	//store topmost stack value into cp field indicated by next two bytes

		case BC.INVOKEVIRTUAL: 	{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
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
								 Operand[] firstLocalVars = new Operand[mte.parCount + 1];
								 
								for(int j = 0; j <= mte.parCount; j++) {
									
									Operand op = stack.pop(); 
									firstLocVars[mte.parCount - j] = op.value;
									firstLocalVars[mte.parCount -j] = op;
								}

								//check if "this" instance is null
							//	if(((TinyJVMRefType)firstLocVars[0]).instance == null) {

								//	throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
									//	"invokevirtual encountered a null reference to 'this' on the stack.");
								//} 

								//create and execute new frame				
								Frame newFrame = new Frame(constPool, false, methName, methType, 
									mte.maxStack, mte.maxLocals, firstLocalVars, firstLocVars, mte.code, false, true);
								heap.fs.pushFrame(newFrame);
								Operand method = new Operand(FIELD, methName, 0);
								s = invokeHelper(BC.INVOKEVIRTUAL, method);
								for(int j = 0; i < firstLocalVars.length; i++){
									iftempremove(firstLocalVars[j]);
								}
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
												
								System.out.println("Produce new Method");
								
								currentBB.last = lastInstr;
								BasicBlock tmp = currentBB;
								currentBB = new BasicBlock(++basicblockcounter);
								tmp.branch = currentBB;
								currentBB.first.prev = lastInstr;
								lastInstr.next = currentBB.first;
								lastInstr = currentBB.first;
								
								produce(newFrame, 0, newFrame.codeArray.length,registers);
								
								if(returnRegister.op != null) {stack.push(returnRegister.op); returnRegister.op = null;} //returnvalue
								
								break;}	//invoke virtual method at cp index specified by next two bytes
		case BC.INVOKESPECIAL: 	{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
								
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
									Operand[] firstLocalVars = new Operand[mte.parCount + 1];

									for(int j = 0; j <= mte.parCount; j++) {
										Operand op = stack.pop(); 
										firstLocalVars[mte.parCount - j] = op;
										firstLocVars[mte.parCount -j] = op.value;
									}

									//check if "this" instance is null
								//	if(((TinyJVMRefType)firstLocVars[0]).instance == null) {

									//	throw new NullPointerException("Exception at ExecutionEngine.execute(): " + 
										//	"invokespecial encountered a null reference to 'this' on the stack.");
									//}

									//create and execute new frame						
									Frame newFrame = new Frame(constPool, false, methName, methType, 
										mte.maxStack, mte.maxLocals, firstLocalVars, firstLocVars, mte.code, false, true);
									heap.fs.pushFrame(newFrame);
									Operand method = new Operand(FIELD, methName, 0);
									s = invokeHelper(BC.INVOKESPECIAL, method);
									for (int j = 0; j < firstLocalVars.length; j++){
										iftempremove(firstLocalVars[j]);
									}
									f.pc = f.pc + 2;
									s.marker = f.pc-2;
									System.out.println("Produce new Method");
									
									currentBB.last = lastInstr;
									BasicBlock tmp = currentBB;
									currentBB = new BasicBlock(++basicblockcounter);
									tmp.branch = currentBB;
									currentBB.first.prev = lastInstr;
									lastInstr.next = currentBB.first;
									lastInstr = currentBB.first;
									
									produce(newFrame, 0, newFrame.codeArray.length, registers);
									//int retValue = execute(newFrame, showStack, haltStack);

									//push return value onto stack
									if(returnRegister.op != null) {stack.push(returnRegister.op); returnRegister.op = null;} //returnvalue

								} else {

								Operand trash =	stack.pop(); iftempremove(trash);
								}
								break;}	//invoke instance method
				
		case BC.INVOKESTATIC: 	{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
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
								Operand[] firstLocalVars = new Operand[mte.parCount];
								
								for(int j = 0; j < mte.parCount; j++) {
									Operand op = stack.pop(); 
									firstLocVars[mte.parCount -1 - j] = op.value;
									firstLocalVars[mte.parCount - 1 - j] = op;
								}

								//create and execute new frame
								Frame newFrame = new Frame(constPool, false, methName, methType, 
									mte.maxStack, mte.maxLocals, firstLocalVars, firstLocVars, mte.code, false, true);
								heap.fs.pushFrame(newFrame);
								Operand method = new Operand(FIELD, methName, 0);
								s = invokeHelper(BC.INVOKESTATIC, method);
								for(int j = 0; j < firstLocalVars.length; j++){
									iftempremove(firstLocalVars[j]);
								}
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								
								System.out.println("Produce new Method");
								
								currentBB.last = lastInstr;
								BasicBlock tmp = currentBB;
								currentBB = new BasicBlock(++basicblockcounter);
								tmp.branch = currentBB;
								currentBB.first.prev = lastInstr;
								lastInstr.next = currentBB.first;
								lastInstr = currentBB.first;
								
								produce(newFrame, 0, newFrame.codeArray.length, registers);
								//int retValue = execute(newFrame, showStack, haltStack);

								//push return value onto stack
								if(returnRegister.op != null) {stack.push(returnRegister.op); returnRegister.op = null;} //returnvalue


								break;} //invoke static method

			
		case BC.NEW: 			{int cpIndex = code[++i];
								cpIndex = (cpIndex << 8) + code[++i];
								
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
								
								s = makeNew(type, className, stack, temps);
								f.pc = f.pc + 2;
								s.marker = f.pc-2;
								break;}	//create new object of a class at constant pool index specified by next two bytes
		
		case BC.NEWARRAY: 		{int typeIndex = code[++i];
								Operand size = stack.pop(); 
								
								//create new stack entry				
								TinyJVMRefType type = new TinyJVMRefType();
								type.isReference = true;

								//put new instance onto heap
								ArrayInstance ini = heap.addArray(size.value.value);
								
								type.instance = ini;
								s = makeNewArray(type, size, stack, temps);iftempremove(size);
								f.pc++;
								s.marker = f.pc-1;
								break;}		//create new array of primitive type specified by next byte

			
		case BC.ARRAYLENGTH: 	{Operand op = stack.pop();
								s = arrayLength(op, stack, temps); iftempremove(op);
								s.marker = f.pc;
								break;}	//push array length onto stack
		

			
		case BC.WIDE: 			{int opCode = code[++i];
								if (opCode == BC.IINC){
								int zahl = code[++i];
								zahl = (zahl << 8) + code[++i];
								int inc = code[++i];
								inc = (inc << 8) + code[++i];
								s = do_iinc(zahl, inc, stack, "incremented by", registers);
								f.pc = f.pc + 4;
								s.marker = f.pc-4;
								}else if(opCode == BC.ILOAD){
									int lvIndex = code[++i];
									lvIndex = (lvIndex << 8) + code[++i];
									s = do_iload(stack, lvIndex, registers, f.locVars);
									f.pc = f.pc +2;
									s.marker = f.pc - 2;
								} else if (opCode == BC.ALOAD){
									int lvIndex = code[++i];
									lvIndex = (lvIndex << 8) + code[++i];
									s = do_aload(stack, lvIndex, registers, f.locVars);
									f.pc = f.pc + 2;
									s.marker = f.pc - 2;
								} else if (opCode == BC.ISTORE){
									int lvIndex = code[++i];
									lvIndex = (lvIndex << 8) + code[++i];
									s = do_store(opCode, lvIndex, stack.pop(), registers);
									f.pc = f.pc + 2;
									s.marker = f.pc - 2;
								} else if (opCode == BC.ASTORE){
									int lvIndex = code[++i];
									lvIndex = (lvIndex << 8) + code[++i];
									s = do_store(opCode, lvIndex, stack.pop(), registers); 
									f.pc = f.pc + 2;
									s.marker = f.pc - 2;
								}
		
								break;}		//increase range for addressing local variables or number by which an integer should be incremented

		
		case BC.IFNULL: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset + code[++i] - 1;
								Operand op = stack.pop();
								f.pc = f.pc + 2;
								s = ifNullHelper(f, BC.IFNULL, offset, op, i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}		//if null reference on top of stack, jump to instruction indicated by next two bytes
		
		case BC.IFNONNULL: 		{int offset = code[++i];
								offset = offset << 8;
								offset = offset	+ code[++i] -1;
								Operand op = stack.pop(); 
								f.pc = f.pc + 2;
								s = ifNullHelper(f, BC.IFNONNULL, offset, op, i-2, f.pc);iftempremove(op);
								i = s.gotoadress-1;
								break;}	//if no null reference on top of stack, jump to instruction indicated by next two bytes

		case BC.GOTO_W : 		{int offset = code[++i] << 24;
								offset = offset + (code[++i] << 16);
								offset = offset + (code[++i] << 8);
								offset = offset + code[++i];
								s = goToHelper(BC.GOTO_W, offset, i-4, f.pc);
								f.pc = f.pc + 4;
								s.marker = f.pc-4;
								break;}
		default: 				System.out.println("Nicht definierte Instruktion.");
								break;
		}
		f.pc++;
		}
	doneFrames.add(f);
	if(start == 0) {System.out.println("Method done.");
		currentBB.last = lastInstr;
		BasicBlock tmp = currentBB;
		currentBB = new BasicBlock(++basicblockcounter);
		tmp.branch = currentBB;
		lastInstr.next = currentBB.first;
		currentBB.first.prev = lastInstr;
		lastInstr = currentBB.first;}
}


private Instruction ifNullHelper(Frame f,int kind, int offset, Operand check, int i, int pc){
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) kind;
	s.result = new Operand(BRANCH, ++basicblockcounter);
	s.ops = new Operand[1];
	s.ops[0] = check;
	if (kind == BC.IFNONNULL) {s.sign = "non NULL";} else {s.sign = "NULL";}
	s.marker = pc;
	lastInstr = s;
	currentBB.branch = s.result.bb;						//nächster BB ist das then
	BasicBlock tmp = currentBB;							//merke gegenwärtigen BB
	currentBB.last = lastInstr;
	currentBB = currentBB.branch;						//setze nächsten BB
	s.next = currentBB.first;
	currentBB.first.prev = s;
	lastInstr = currentBB.first;
	
	int goToAdress; 
	if (f.codeArray[i+offset-2] == BC.GOTO){
	goToAdress = f.codeArray[i+offset-1];
	goToAdress = goToAdress << 8;
	goToAdress = goToAdress + f.codeArray[i+offset]; 
	goToAdress = i + offset - 2 + goToAdress;
	s.gotoadress = goToAdress;}
	else {
		goToAdress = f.codeArray[i+offset-3];
		goToAdress= (goToAdress << 8) + f.codeArray[i+offset-2] ;
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset-1];
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset]; 
		goToAdress = i + offset - 4 + goToAdress;//hole Adresse an der then und else wieder zusammenkommen
		s.gotoadress = goToAdress;
	}
	System.out.println("THEN:");
	produce(f, i+offset+1, goToAdress, ExecutionEngine.registers ); //produziere then
	BasicBlock tmp2 = currentBB; // bb vom Ende vom thenzweig merken
	currentBB.last = lastInstr;
	currentBB = tmp; //setze current wieder auf BB von vorher
	BasicBlock bbElse = new BasicBlock(++basicblockcounter); //erstelle neuen BB für else
	currentBB.fail = bbElse;
	currentBB = currentBB.fail; //setze elsezweig als current
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	
	System.out.println("ELSE:");
	produce(f, i+3, i+offset+1, ExecutionEngine.registers); //produziere das else
	System.out.println("End If: ");
	BasicBlock tmp3 = currentBB; // merke ende vom elsezweig
	BasicBlock bbWeiter = new BasicBlock(++basicblockcounter); //neuer Basicblock für zusammenführung
	bbElse.stopAt = bbWeiter.number;
	currentBB.last = lastInstr;
	currentBB = bbWeiter;
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	tmp2.branch = currentBB;
	tmp3.branch = currentBB; //Zusammenführung
	currentBB.size++;
	return s;
}

private Instruction makeNew(TinyJVMRefType type, String className, OPstack stack, Vector<Register> temps){
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = BC.NEW;
	s.ops = new Operand[1];
	s.ops[0] = new Operand(TYPEREF, className, type);
	Operand t = new Operand(REGISTER, getTempCount(temps));
	s.sign ="new";
	s.result = t;
	lastInstr = s;
	currentBB.size++;
	stack.push(t);
	return s;
}

private Instruction makeNewArray(TinyJVMRefType type, Operand size, OPstack stack, Vector<Register> temps){
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = BC.NEWARRAY;
	s.ops = new Operand[2];
	s.ops[0] = new Operand(TYPEREF, "array", type);
	s.ops[1] = size;
	s.result = new Operand(REGISTER, getTempCount(temps));
	s.sign = "new Array";
	lastInstr = s;
	currentBB.size++;
	stack.push(s.result);	
	return s;
}

private Instruction arrayLength(Operand op, OPstack stack, Vector<Register> temps){
	Instruction s = new Instruction();
	s.prev = lastInstr;
	s.kind = BC.ARRAYLENGTH;
	s.ops = new Operand[1];
	s.ops[0] = op;
	s.result = new Operand(REGISTER, getTempCount(temps));
	return s;
}

private Instruction do_store(int kind, int index, Operand op1, Register[] registers){
Instruction s = new Instruction(); // neue Instruktion
s.prev = lastInstr;	//doppelte Verknüpfung
lastInstr.next = s;
s.kind = (short) kind;	//setze Art der Instr
s.ops = new Operand[1];	//erstelle Operandenarray
s.ops[0] = op1;			//ausfüllen
s.result = new Operand(REGISTER, index); //erstelle Register für Resultat
s.result.register.op = s.result;	//verknüpfe Operand mit Register
s.sign ="stored";					//zeichen für Darstellung
registers[index] = s.result.register;	//ersetze altes Register mit neuem
registers[index].isLocal = true;		//setze auf lokal
lastInstr = s;							//neue Instruktion als letzte erstellte setzen
currentBB.size++;						//Basic Block vergrößern
return s;
}  


private Instruction do_iastore(Operand value, Operand index, Operand array){
	Instruction s = new Instruction(); //neue Inst
	s.prev = lastInstr;		//doppelt verknüpfen
	lastInstr.next = s;
	s.kind = (short) BC.IASTORE;	//Art
	s.result = value;		//Resultat setzen
	s.ops = new Operand[2];	//Operanden erstellen
	s.ops[0]= array;
	s.ops[1] = index;
	s.sign = "stored in Array at"; // Darstellung ist value stored in Array at array[index]
	lastInstr = s;
	currentBB.size++;
	return s;
	
}

private Instruction do_iload(OPstack stack, int index, Register[] registers, Operand[] lVars) {
    Operand r = registers[index].op;
    stack.push(r);
   return null;
  }

private Instruction do_aload(OPstack stack, int index, Register[] registers, Operand[] lVars) {
	Operand r;
	if (lVars[index] != null) {r = lVars[index]; stack.push(r);}
	else {r = registers[index].op; stack.push(r);}
	return null;
}

private Instruction aloadHelper(int kind, Operand array, Operand index, OPstack stack, Vector<Register> temps){
	Instruction s = new Instruction();
	Operand t = new Operand(REGISTER, getTempCount(temps));
	temps.add(t.register.number, t.register);
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) kind;
	s.ops = new Operand[2];
	s.ops[0] = array;
	s.ops[1] = index;
	s.result = t;  //Wert aus array[index] wird in temporären Register auf stack gepusht
	s.sign = "[";
	stack.push(t);
	lastInstr = s;
	currentBB.size++;
	return s;
}

private Instruction do_iinc(int zahl, int inc, OPstack stack, String sign, Register[] registers){
	Instruction s = new Instruction();
	Operand t = registers[zahl].op; //Inhalt von Register mit Nummer zahl soll erhöht werden
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = BC.IINC;
	s.result = t;		//ist sowohl ergebnis als auch operand da instruktion auf sich selber ausgeführt wird
	s.ops = new Operand[2];
	s.ops[0] = t;
	s.ops[1] = new Operand(INT_CONSTANT, inc); //zahl um die inkrementiert wird
	s.sign = sign;
	lastInstr = s;
	currentBB.size++;
	return s;
}

private Instruction putStatic(int operator, Operand val, Operand field, OPstack stack, String sign, Vector<Register> temps){
	Instruction s = new Instruction();
	s.result = field;
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	s.ops[0] = val;
	s.sign = sign;
	lastInstr = s;
	currentBB.size++;
	return s;
}

private Instruction putField(int operator, Operand op, Operand objRef, OPstack stack, String sign, Vector<Register> temps){
	Instruction s = new Instruction();
	s.result = objRef;
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	s.ops[0] = op;
	s.sign = sign;
	lastInstr = s;
	currentBB.size++;
	return s;
}

private Instruction getField(int operator, Operand objRef, String fieldName, OPstack stack, String sign, Vector<Register> temps){
	Instruction s = new Instruction();
	Operand t = new Operand(REGISTER, getTempCount(temps));
	temps.add(t.register.number, t.register);
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	s.ops[0] = objRef;
	s.result = t;
	s.sign = "field " + fieldName + " " + sign;
	lastInstr = s;
	stack.push(t);
	currentBB.size++;
	return s;
}

private Instruction getStatic(int operator, Operand objRef, String fieldName, OPstack stack, String sign, Vector<Register> temps){
	Instruction s = new Instruction();
	Operand t = new Operand(REGISTER, getTempCount(temps));
	temps.add(t.register.number, t.register);
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	s.ops[0] = objRef;
	s.result = t;
	s.sign = sign;
	lastInstr = s;
	stack.push(t);
	currentBB.size++;
	return s;
}

private Instruction unaryInstruction(int operator, Operand op, OPstack stack, String sign, Vector<Register> temps){
	Instruction s = new Instruction();
	Operand t = new Operand(REGISTER, getTempCount(temps));
	temps.add(t.register.number, t.register);
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	s.ops[0] = op;
	s.result = t;
	s.sign = sign;
	stack.push(t);
	lastInstr = s;
	currentBB.size++;
	return s;
	
}

private Instruction binaryInstruction(int operator, Operand op1, Operand op2, OPstack stack, String sign,Vector<Register> temps) {
Operand t = new Operand(REGISTER, getTempCount(temps));
temps.add(t.register.number, t.register);
Instruction s = new Instruction();
s.prev = lastInstr;
lastInstr.next = s;
s.kind = (short) operator;
s.ops = new Operand[2];
s.ops[0] = op1;
s.ops[1] = op2;
s.result = t;
s.sign = sign;
stack.push(t);
lastInstr = s;
currentBB.size++;
return s;
}

private Instruction ifIntHelper(Frame f, int kind, int offset, Operand op, String sign, int i, int pc){
	Instruction s = new Instruction();						//erstelle Instruktion
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) kind;
	s.result = new Operand(BRANCH, ++basicblockcounter);
	s.ops = new Operand[1];
	s.ops[0] = op;
	s.sign = sign;
	s.marker = pc - 2;
	lastInstr = s;
	currentBB.last = s;
	currentBB.branch = s.result.bb;						//nächster BB ist das then
	BasicBlock tmp = currentBB;							//merke gegenwärtigen BB
	currentBB = currentBB.branch;						//setze nächsten BB
	s.next = currentBB.first;
	currentBB.first.prev = s;
	lastInstr = currentBB.first;
	
	int goToAdress; 
	if (f.codeArray[i+offset-2] == BC.GOTO){
	goToAdress = f.codeArray[i+offset-1];
	goToAdress = goToAdress << 8;
	goToAdress = goToAdress + f.codeArray[i+offset]; 
	goToAdress = i + offset - 2 + goToAdress;
	s.gotoadress = goToAdress;}
	else {
		goToAdress = f.codeArray[i+offset-3];
		goToAdress= (goToAdress << 8) + f.codeArray[i+offset-2] ;
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset-1];
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset]; 
		goToAdress = i + offset - 4 + goToAdress;//hole Adresse an der then und else wieder zusammenkommen
		s.gotoadress = goToAdress;
	}
	System.out.println("THEN:");
	produce(f, i+offset+1, goToAdress, ExecutionEngine.registers ); //produziere then
	BasicBlock tmp2 = currentBB; // bb vom Ende vom thenzweig merken
	currentBB.last = lastInstr;
	currentBB = tmp; //setze current wieder auf BB von vorher
	BasicBlock bbElse = new BasicBlock(++basicblockcounter); //erstelle neuen BB für else
	currentBB.fail = bbElse;
	currentBB = currentBB.fail; //setze elsezweig als current
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	
	System.out.println("ELSE:");
	produce(f, i+3, i+offset+1, ExecutionEngine.registers); //produziere das else
	System.out.println("End If: ");
	BasicBlock tmp3 = currentBB; // merke ende vom elsezweig
	BasicBlock bbWeiter = new BasicBlock(++basicblockcounter); //neuer Basicblock für zusammenführung
	bbElse.stopAt = bbWeiter.number;
	currentBB.last = lastInstr;
	currentBB = bbWeiter;
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	tmp2.branch = currentBB;
	tmp3.branch = currentBB; //Zusammenführung
	//currentBB.size++;
	return s;
}


private Instruction CmpIntHelper(Frame f, int kind, int offset, Operand op1, Operand op2, String sign, int i, int pc){
	Instruction s = new Instruction();						//erstelle Instruktion
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) kind;
	s.result = new Operand(BRANCH, ++basicblockcounter);
	s.ops = new Operand[2];
	s.ops[0] = op1;
	s.ops[1] = op2;
	s.sign = sign;
	s.marker = pc - 2;
	lastInstr = s;
	currentBB.last = lastInstr;
	currentBB.branch = s.result.bb;						//nächster BB ist das then
	BasicBlock tmp = currentBB;							//merke gegenwärtigen BB
	currentBB = currentBB.branch;						//setze nächsten BB
	s.next = currentBB.first;
	currentBB.first.prev = s;
	lastInstr = currentBB.first;
	
	
	int goToAdress; 
	if (f.codeArray[i+offset-2] == BC.GOTO){
	goToAdress = f.codeArray[i+offset-1];
	goToAdress = goToAdress << 8;
	goToAdress = goToAdress + f.codeArray[i+offset]; 
	goToAdress = i + offset - 2 + goToAdress;
	s.gotoadress = goToAdress;}
	else {
		goToAdress = f.codeArray[i+offset-3];
		goToAdress= (goToAdress << 8) + f.codeArray[i+offset-2] ;
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset-1];
		goToAdress = (goToAdress << 8) + f.codeArray[i+offset]; 
		goToAdress = i + offset - 4 + goToAdress;//hole Adresse an der then und else wieder zusammenkommen
		s.gotoadress = goToAdress;
	}
	System.out.println("THEN:");
	produce(f, i+offset+1, goToAdress, ExecutionEngine.registers ); //produziere then
	BasicBlock tmp2 = currentBB; // bb vom Ende vom thenzweig merken
	currentBB.last = lastInstr;
	currentBB = tmp; //setze current wieder auf BB von vorher
	BasicBlock bbElse = new BasicBlock(++basicblockcounter); //erstelle neuen BB für else
	currentBB.fail = bbElse;
	currentBB = currentBB.fail; //setze elsezweig als current
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	
	System.out.println("ELSE:");
	produce(f, i+3, i+offset+1, ExecutionEngine.registers); //produziere das else
	System.out.println("End If: ");
	BasicBlock tmp3 = currentBB; // merke ende vom elsezweig
	BasicBlock bbWeiter = new BasicBlock(++basicblockcounter); //neuer Basicblock für zusammenführung
	bbElse.stopAt = bbWeiter.number;
	currentBB.last = lastInstr;
	currentBB = bbWeiter;
	lastInstr.next = currentBB.first;
	currentBB.first.prev = lastInstr;
	lastInstr = currentBB.first;
	tmp2.branch = currentBB;
	tmp3.branch = currentBB; //Zusammenführung
	//currentBB.size++;
	return s;
}

private Instruction goToHelper(int kind, int offset, int i, int marker){
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) kind;
	s.sign = Integer.toString(i + offset + 1);
	lastInstr = s;
	currentBB.size++;
	return s;
}

private Instruction returnHelper(Frame f, short operator, Operand val) {
    
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = (short) operator;
	s.ops = new Operand[1];
	heap.fs.popFrame(f, false);
	s.ops[0] = val;
	s.sign = "returned";
	returnRegister.op = val;
	lastInstr = s;
	currentBB.size++;
    return s;
  }

private Instruction invokeHelper(int kind, Operand method){
	Instruction s = new Instruction();
	s.result = method;
	s.kind = (short)kind;
	switch (kind){
	case BC.INVOKESPECIAL: {s.sign = "Invoke Special"; break;}
	case BC.INVOKESTATIC: {s.sign = "Invoke Static"; break;}
	case BC.INVOKEVIRTUAL: {s.sign = "Invoke Virtual"; break;}
	}
	s.prev = lastInstr;
	lastInstr.next = s;
	lastInstr = s;
    currentBB.size++;
	return s;
}

private int getTempCount(Vector<Register> temps){ //sucht erstes unbenutztes temp Register
	int i = 0;
	if(temps.size() != 0){
	while(i< temps.size() &&temps.elementAt(i) != null && i == temps.elementAt(i).number ){
		i++;
	}
	}
	return i;
}

private Instruction trap(String cause) { //für exceptions wie division durch 0
	Instruction s = new Instruction();
	s.prev = lastInstr;
	lastInstr.next = s;
	s.kind = TRAP;
	s.sign = cause;
	return s;
}

private BasicBlock copyBB(BasicBlock bb){ //kopiert einen Basic Block wenn bereits umgewandelter code existiert
	BasicBlock copy = new BasicBlock(bb.number);
	copy.first = bb.first;
	copy.last = bb.last;
	copy.first.prev = null;
	copy.last.next = null;
	return copy;
}

void iftempremove(Operand op){  //gibt temporäres Register wieder frei
	if (op.register != null && !(op.register.isLocal)){
		int i = 0;
		while (i < temps.size()){
			if(temps.elementAt(i).number == op.register.number) temps.remove(i);
			i++;
		}
		
	}

}


void display(int i){  //Für Anzeige welcher Bytecode verwendet wird 
String bcString;
switch(i) {
case BC.ICONST_0: 			bcString = "ICONST_0"; break; 	
case BC.ICONST_1: 			bcString = "ICONST_1"; break;
case BC.ICONST_2: 			bcString = "ICONST_2"; break;
case BC.ICONST_3: 			bcString = "ICONST_3"; break;
case BC.ICONST_4: 			bcString = "ICONST_4"; break;
case BC.ICONST_5: 			bcString = "ICONST_5"; break;
case BC.ISTORE:	  			bcString = "ISTORE"; break;
case BC.ISTORE_0: 			bcString = "ISTORE_0"; break;
case BC.ISTORE_1: 			bcString = "ISTORE_1"; break;
case BC.ISTORE_2: 			bcString = "ISTORE_2"; break;
case BC.ISTORE_3: 			bcString = "ISTORE_3"; break;
case BC.ILOAD:	 			bcString = "ILOAD"; break;
case BC.ILOAD_0: 			bcString = "ILOAD_0"; break;
case BC.ILOAD_1: 			bcString = "ILOAD_1"; break;
case BC.ILOAD_2: 			bcString = "ILOAD_2"; break;
case BC.ILOAD_3: 			bcString = "ILOAD_3"; break;
case BC.IADD:    			bcString = "IADD"; break;
case BC.ISUB:				bcString = "ISUB"; break;
case BC.IMUL:				bcString = "IMUL"; break;
case BC.IDIV:				bcString = "IDIV"; break;
case BC.IREM:				bcString = "IREM"; break;
case BC.ALOAD:				bcString = "ALOAD"; break;
case BC.ALOAD_0:			bcString = "ALOAD_0"; break;
case BC.ALOAD_1:			bcString = "ALOAD_1"; break;
case BC.ALOAD_2:			bcString = "ALOAD_2"; break;
case BC.ALOAD_3:			bcString = "ALOAD_3"; break;
case BC.ARRAYLENGTH:		bcString = "ARRAYLENGTH"; break;
case BC.ASTORE:				bcString = "ASTORE"; break;
case BC.ASTORE_0:			bcString = "ASTORE_0"; break;
case BC.ASTORE_1:			bcString = "ASTORE_1"; break;
case BC.ASTORE_2:			bcString = "ASTORE_2"; break;
case BC.ASTORE_3:			bcString = "ASTORE_3"; break;
case BC.BIPUSH:				bcString = "BIPUSH"; break;
case BC.DUP:				bcString = "DUP"; break;
case BC.DUP_X1:				bcString = "DUP_X1"; break;
case BC.DUP_X2:				bcString = "DUP_X2"; break;
case BC.GETFIELD:			bcString = "GETFIELD"; break;
case BC.GETSTATIC:			bcString = "GETSTATIC"; break;
case BC.GOTO:				bcString = "GOTO"; break;
case BC.GOTO_W:				bcString = "GOTO_W"; break;
case BC.IALOAD:				bcString = "IALOAD"; break;
case BC.IAND:				bcString = "IAND"; break;
case BC.IASTORE:			bcString = "IASTORE"; break;
case BC.IF_ACMPEQ:			bcString = "IF_ACMPEQ"; break;
case BC.IF_ACMPNE:			bcString = "IF_ACMPNE"; break;
case BC.IF_ICMPEQ:			bcString = "IF_ICMPEQ"; break;
case BC.IF_ICMPGE:			bcString = "IF_ICMPGE"; break;
case BC.IF_ICMPGT:			bcString = "IF_ICMPGT"; break;
case BC.IF_ICMPLE:			bcString = "IF_ICMPLE"; break;
case BC.IF_ICMPLT:			bcString = "IF_ICMPLT"; break;
case BC.IF_ICMPNE:			bcString = "IT_ICMPNE"; break;
case BC.IFEQ:				bcString = "IFEQ"; break;
case BC.IFNE:				bcString = "IFNE"; break;
case BC.IFLT:				bcString = "IFLT"; break;
case BC.IFLE:				bcString = "IFLE"; break;
case BC.IFGT:				bcString = "IFGT"; break;
case BC.IFGE:				bcString = "IFGE"; break;
case BC.IFNONNULL:			bcString = "IFNONNULL"; break;
case BC.IFNULL:				bcString = "IFNULL"; break;
case BC.IINC:				bcString = "IINC"; break;
case BC.INEG:				bcString = "INEG"; break;
case BC.INVOKESPECIAL:		bcString = "INVOKESPECIAL"; break;
case BC.INVOKESTATIC:		bcString = "INVOKESTATIC"; break;
case BC.INVOKEVIRTUAL:		bcString = "INVOKEVIRTUAL"; break;
case BC.IOR:				bcString = "IOR"; break;
case BC.IRETURN:			bcString = "IRETURN"; break;
case BC.ISHL:				bcString = "ISHL"; break;
case BC.ISHR:				bcString = "ISHR"; break;
case BC.IUSHR:				bcString = "IUSHR"; break;
case BC.IXOR:				bcString = "IXOR"; break;
case BC.LDC:				bcString = "LDC"; break;
case BC.LDC_W:				bcString = "LDC_W"; break;
case BC.NEW:				bcString = "NEW"; break;
case BC.NEWARRAY:			bcString = "NEWARRAY"; break;
case BC.POP:				bcString = "POP"; break;
case BC.POP2:				bcString = "POP2"; break;
case BC.PUTFIELD:			bcString = "PUTFIELD"; break;
case BC.PUTSTATIC:			bcString = "PUTSTATIC"; break;
case BC.SIPUSH:				bcString = "SIPUSH"; break;
case BC.SWAP:				bcString = "SWAP"; break;
case BC.WIDE:				bcString = "WIDE"; break;
case BC.RETURN: 			bcString = "RETURN"; break;
default: 					bcString = "undefined instruction"; break;
}
System.out.println();
System.out.println("\t\t" + bcString);
}

void showInstructions(BasicBlock bb){
	
	BasicBlock tmp = bb;
	
while(tmp != null && tmp.number != bb.stopAt){
	if (tmp.first.next != null){
		tmp.displayInstructions();
		if (tmp.fail != null) {showInstructions(tmp.fail);}
		tmp = tmp.branch;
		} 
	else tmp = tmp.branch;}
}

}
