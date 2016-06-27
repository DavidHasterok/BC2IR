class MethodTableEntry {

	String name;
	String type;
	int parCount;
	int maxStack;
	int maxLocals;
	int codeLength;
	int[] code;
	int[] parameters;	//1: integer parameter, 2: reference parameter
	boolean returnsInt;	//true: int(), false: void()
	boolean isStatic;
}