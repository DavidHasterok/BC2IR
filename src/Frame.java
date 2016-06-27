class Frame {

	RuntimeConstantPool rcp;	//constant pool of the class of the current method
	BytecodeVerifier veri;		//for stack visualisation
	boolean isStatic;		//for stack visualisation
	String methName;		//for stack visualisation
	String methType;		//for stack visualisation
	OperandStack opStack;
	OPstack registerOpStack;
	TinyJVMType[] lVars;		//local variables
	Operand[] locVars;
	int[] codeArray;
	int pc;				//programme counter
	BasicBlock first;
	BasicBlock last;

/**	Frame(RuntimeConstantPool constPool, boolean isStaticMeth, String name, String type, 
		int opStackSize, int maxLocals, TinyJVMType[] firstLocVars, 
		int[] code, boolean showStack, boolean haltStack) {

		veri = new BytecodeVerifier(code, constPool);
		rcp = constPool;
		isStatic = isStaticMeth;
		methName = name;
		methType = type;
		opStack = new OperandStack(opStackSize, constPool, this, showStack, haltStack);
		registerOpStack = new OPstack(opStackSize);
		codeArray = code;
		pc = 0;

		//if non-static method: object reference in first local variable
		//next local variables: arguments on method invokation
		lVars = new TinyJVMType[maxLocals];

		if(firstLocVars != null) {

			for(int i = 0; i < firstLocVars.length; i++) {

				lVars[i] = firstLocVars[i];
			}
		}
	} **/
	
	Frame(RuntimeConstantPool constPool, boolean isStaticMeth, String name, String type, 
			int opStackSize, int maxLocals, Operand[] firstLocalVars, TinyJVMType[] firstLocVars,
			int[] code, boolean showStack, boolean haltStack) {

			veri = new BytecodeVerifier(code, constPool);
			rcp = constPool;
			isStatic = isStaticMeth;
			methName = name;
			methType = type;
			opStack = new OperandStack(opStackSize, constPool, this, showStack, haltStack);
			registerOpStack = new OPstack(opStackSize);
			codeArray = code;
			pc = 0;

			//if non-static method: object reference in first local variable
			//next local variables: arguments on method invokation
			locVars = new Operand[maxLocals];
			lVars = new TinyJVMType[maxLocals];
			
			if(firstLocVars != null) {

				for(int i = 0; i < firstLocVars.length; i++) {
					lVars[i] = firstLocVars[i];
					locVars[i] = firstLocalVars[i];
				}
			}
		}
	
}