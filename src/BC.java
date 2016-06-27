class BC {

	public static final int

		NOP = 0,	//no operation

	//	ACONST_NULL,	//no null reference in TinyJava
	//	INCONST_M1,	//no negative numbers

		ICONST_0 = 3,	//push constant 0,...,5 onto stack
		ICONST_1 = 4,
		ICONST_2 = 5,
		ICONST_3 = 6,
		ICONST_4 = 7,
		ICONST_5 = 8,

	//	LCONST_i,	//i = 0,1, no long type in TinyJava
	//	FCONST_i,	//i = 0,1,2, no float type in TinyJava
	//	DCONST_i,	//i = 0,1, no double type in TinyJava

		BIPUSH = 16,	//push byte onto stack, treated as integer
		SIPUSH = 17,	//push short onto stack, treated as integer
		LDC = 18,	//push constant from constant pool onto stack, index specified by following byte
		LDC_W = 19,	//same as LDC with increased range of indices, index specified by following two bytes (in big endian order)
	//	LDC2_W,		//only for long or double values

		ILOAD = 21,		//push integer from local variable table onto stack, index specified by following byte
	//	LLOAD, FLOAD, DLOAD,	//not needed

		ALOAD = 25,	//push reference from loc var tab onto stack, index in following byte
		ILOAD_0 = 26,	//push integer from loc var tab at index 0,...,3 onto stack
		ILOAD_1 = 27,
		ILOAD_2 = 28,
		ILOAD_3 = 29,

	//	LLOAD_0,...,3,	//not needed
	//	FLOAD_0,...,3,
	//	DLOAD_0,...,3,

		ALOAD_0 = 42,	//push reference from loc var tab at index 0,...3 onto stack
		ALOAD_1 = 43,
		ALOAD_2 = 44,
		ALOAD_3 = 45,

		IALOAD = 46,		//push integer from array onto stack
	//	LALOAD, FALOAD, DALOAD,
	//	AALOAD			//no object arrays in TinyJava
	//	BALOAD, CALOAD, SALOAD,	//not needed for TinyJava

		ISTORE = 54,		//store integer from stack into local variable at index specified by next byte
	//	LSTORE, FSTORE, DSTORE,	//not needed

		ASTORE = 58,	//store reference from stack into loc var at index in next byte
		ISTORE_0 = 59,	//store integer from stack into loc var at index 0,...,3
		ISTORE_1 = 60,
		ISTORE_2 = 61,
		ISTORE_3 = 62,

	//	LSTORE_0,...3, FSTORE_0,...3, DSTORE_0,...3,

		ASTORE_0 = 75,	//store reference from stack into loc var at index 0,...,3
		ASTORE_1 = 76,
		ASTORE_2 = 77,
		ASTORE_3 = 78,
		IASTORE = 79,	//store integer from stack into array

	//	LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE,

		POP = 87,	//delete topmost stack value (e.g. if method returns void and stack is not empty yet)
		POP2 = 88,	//delete topmost two stack values
		DUP = 89,	//duplicate topmost stack value
		DUP_X1 = 90,	//copy topmost stack value two places underneath
		DUP_X2 = 91,	//in TinyJava: copy topmost stack value three places underneath

	//	DUP2, DUP_X1, DUP_X2,	//same as above, mainly for long and float values
		SWAP = 95,		//swap two topmost stack values

		IADD = 96,		//add two integers
	//	LADD, FADD, DADD,

		ISUB = 100,		//subtract one integer from another
	//	LSUB, FSUB, DSUB,

		IMUL = 104,		//multiply two integers
	//	LMUL, FMUL, DMUL,

		IDIV = 108,		//divide one integer by another
	//	LDIV, FDIV, DDIV,

		IREM = 112,		//integer remainder
	//	LREM, FREM, DREM,

		INEG = 116,		//negate integer
	//	LNEG, FNEG, DNEG,

		ISHL = 120,	//integer left shift
	//	LSHL,

		ISHR = 122,	//integer right shift (arithmetic, keeping the sign bit and distinguishing b/w mantissa and exponent)
	//	LSHR,

		IUSHR = 124,	//integer right shift (logical, regardless of sign bit/mantissa/exponent)
	//	LUSHR,

		IAND = 126,	//bitwise AND on two integers
	//	LAND,

		IOR = 128,	//bitwise OR on two integers
	//	LOR,

		IXOR = 130,	//bitwise XOR on two integers
	//	LXOR,

		IINC = 132,	//increment integer in loc var table at index in next byte by constant in byte after index byte

	//	I2L, I2F, I2D,	//conversions of integers
	//	L2I, L2F, L2D,	//conversions of long values
	//	F2I, F2L, F2D,	//converisons of float values
	//	D2I, D2L, D2F,	//conversions of double values
	//	I2B, I2C, I2S,	//more conversions of integers
	//	LCMP,		//comparing of two long values
	//	FCMPL, FCMPG,	//comparing of two float values
	//	DCMPL, DCMPG,	//comparing of two double values

		IFEQ = 153,	//if topmost stack entry is 0, jump to instruction indicated by next two bytes
		IFNE = 154,	//if topmost stack entry is not 0, jump to instruction indicated by next two bytes
		IFLT = 155,	//if topmost stack entry is less than 0, jump to instruction indicated by next two bytes
		IFGE = 156,	//if topmost stack entry is greater or equal to 0, jump to instruction indicated by next two bytes
		IFGT = 157,	//if topmost stack entry is greater than 0, jump to instruction indicated by next two bytes
		IFLE = 158,	//if topmost stack entry is less or equal to 0, jump to instruction indicated by next two bytes

		IF_ICMPEQ = 159,	//if topmost two stack integer values are equal, jump to instruction indicated by next two bytes
		IF_ICMPNE = 160,	//if topmost two stack integer values are not equal, jump to instruction indicated by next two bytes
		IF_ICMPLT = 161,	//if integer in second position from top on stack is less than integer in topmost stack position, jump to instruction indicated by next two bytes
		IF_ICMPGE = 162,	//if second integer is greater or equal to topmost integer, jump to instruction indicated by next two bytes
		IF_ICMPGT = 163,	//if second integer is greater than the topmost integer, jump to instruction indicated by next two bytes
		IF_ICMPLE = 164,	//if second integer less or equal to the topmost integer, jump to instruction indicated by next two bytes

		IF_ACMPEQ = 165,	//if topmost references are equal, jump to instruction indicated by next two bytes
		IF_ACMPNE = 166,	//if topmost references are not equal, jump to instruction indicated by next two bytes

		GOTO = 167,		//go to instruction indicated by next two bytes

	//	JSR, RET,			mainly used for finally clauses
	//	TABLESWITCH, LOOKUPSWITCH,	used for switch statement, not available in TinyJava

		IRETURN = 172,				//return integer from a method
	//	LRETURN, FRETURN, DRETURN, ARETURN	//only integer or void allowed as return types
		RETURN = 177,				//return void from a method

		GETSTATIC = 178,	//push static field value onto stack, constant pool index specified by next two bytes
		PUTSTATIC = 179,	//store topmost stack value into static field at cp index specified by next two bytes
		GETFIELD = 180,		//push field value in cp indicated by next two bytes onto stack
		PUTFIELD = 181,		//store topmost stack value into cp field indicated by next two bytes

		INVOKEVIRTUAL = 182,	//invoke virtual method at cp index specified by next two bytes
		INVOKESPECIAL = 183,	//invoke instance method
					//There are only instance methods in Tiny Java, because no abstract methods can be declared.
					//But neither is there a possibility to declare final classes, so consequently the compiler
					//compiles all the methods of a TinyJava source code file into virtual methods, except for
					//constructors, which are always final methods and therefore compiled into INVOKESPECIAL.
		INVOKESTATIC = 184,	//invoke static method

	//	INVOKEINTERFACE,	//no interfaces
	//	INVOKEDYNAMIC,		//no dynamic methods

		NEW = 187,		//create new object of a class at constant pool index specified by next two bytes
		NEWARRAY = 188,		//create new array of primitive type specified by next byte

	//	ANEWARRAY,		//no arrays of objects in TinyJava

		ARRAYLENGTH = 190,	//push array length onto stack

	//	ATHROW,			//no exceptions in TinyJava
	//	CHECKCAST,		//no casts in TinyJava

	//	INSTANCEOF,			//no class inheritance in TinyJava
	//	MONITORENTER, MONITOREXIT,	//no threads in TinyJava

		WIDE = 196,		//increase range for addressing local variables or number by which an integer should be incremented

	//	MULTIANEWARRAY,		//only one-dimensional arrays in TinyJava

		IFNULL = 198,		//if null reference on top of stack, jump to instruction indicated by next two bytes
		IFNONNULL = 199,	//if no null reference on top of stack, jump to instruction indicated by next two bytes

		GOTO_W = 200;	//go to instruction specified by next four bytes

	//	JSR_W,		//no subroutines (other than methods) in TinyJava

	//	instructions 202 to 255 are reserved for debuggers or future instructions
}