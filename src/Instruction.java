
public class Instruction {

	
	short kind;
	Operand result;
	Operand[] ops;
	Instruction next, prev;
	String sign = null;
	int marker;
	int gotoadress;
	
	Instruction(){
		this.kind = BC.NOP;
	}
	
	public String representation(){
		String line = null;
		String resultType;
		if (result != null && result.kind == BC2IR.FIELD) {
			resultType = result.className;
		}
		if ((kind == BC.RETURN && result == null) || (kind == BC.IRETURN && result != null)) {resultType = "RETURN ";}
		else if (result != null && result.register != null && result.register.isLocal ) { 
			resultType = "l"+ Integer.toString(result.register.number); } 
				else if (result != null && result.register != null) resultType = "t"+ Integer.toString(result.register.number); 
						else if(result != null && result.value != null) {resultType = result.value.toString();}
								else {resultType = "null";}
		
	String[] operandTypes;
	if (ops != null){
		operandTypes = new String[ops.length];
		int i = 0;
		
		if(kind != BC.RETURN){
			while (i < ops.length){
				if (ops[i].register != null && ops[i].register.isLocal) operandTypes[i] = "l"+ Integer.toString(ops[i].register.number);
				else if (ops[i].register != null) operandTypes[i] = "t"+ Integer.toString(ops[i].register.number); 
				else if (ops[i].kind == BC2IR.FIELD) {operandTypes[i] = ops[i].className;} 
				else if (ops[i].kind == BC2IR.INT_CONSTANT) {operandTypes[i] = Integer.toString(ops[i].value.value);}
			i++;
			}
		} else if(ops[0] != null && ops[0].kind == BC2IR.REGISTER)
					{if (ops[0].register.isLocal) {operandTypes[0] ="l"+ Integer.toString(ops[0].register.number);} 
						else {operandTypes[0] = "t" + Integer.toString(ops[0].register.number);}
					} else if(ops[0] != null) {operandTypes[0] = ops[0].className;}
				else  operandTypes[0] = "VOID";
	} else operandTypes = new String[1];
		if (resultType != "RETURN ")line = resultType + " = " + operandTypes[0] + " " + sign; else line = resultType + operandTypes[0] + "\n";
	
	if (operandTypes.length > 1){
		line = line + " " + operandTypes[1];
		if (kind == BC.ALOAD) { line = line + "]";}
		}
	if (kind == BC2IR.TRAP) {line = sign;}
	else if (kind == BC.IASTORE) {
		line = resultType + " " + sign + " " + operandTypes[0] + "[" + operandTypes[1] + "]";
	}
	else if (kind == BC.IALOAD) {
		line = resultType + " = " + operandTypes[0] + "[" + operandTypes[1] + "]";}
	else if (kind == BC.IFEQ || kind == BC.IFGE || kind == BC.IFGT || kind == BC.IFLE || kind == BC.IFLT || kind == BC.IFNE) {
		line = "Jump to BasicBlock " + Integer.toString(result.bb.number) + " if " + operandTypes[0] + " " + sign + " 0";
	}
	else if (kind == BC.IF_ICMPEQ || kind == BC.IF_ICMPGE || kind == BC.IF_ICMPGT || kind == BC.IF_ICMPLE || kind == BC.IF_ICMPLT || kind == BC.IF_ICMPNE || kind ==BC.IF_ACMPEQ || kind == BC.IF_ACMPNE) {
		line = "Jump to BasicBlock " + Integer.toString(result.bb.number) + " if " + operandTypes[0] + " " + sign + " " + operandTypes[1];
	}
	else if (kind == BC.IFNONNULL || kind == BC.IFNULL) {
		line = "Jump to BasicBlock " + Integer.toString(result.bb.number) + " if " + operandTypes[0] + " is " + sign;
	}
	else if (kind == BC.INVOKESPECIAL || kind == BC.INVOKESTATIC || kind == BC.INVOKEVIRTUAL){
		line = sign + " Method: " + result.className;
	}
	else if (kind == BC.NEW){
		line = resultType + " = " + sign + " " + ops[0].className;
	}
	else if (kind == BC.NEWARRAY){
		line = resultType + " = " + sign + " of size " + operandTypes[1];
	} else if (kind == BC.GOTO || kind == BC.GOTO_W){
		line = "Go to Marker " + sign; 
	} else if (kind == BC.GETSTATIC) {
		line = resultType + " = " + "Static Field " + operandTypes[0] + sign;
	} else if (kind == BC.PUTSTATIC) {
		line = "Static Field " + result.className + " = " + operandTypes[0] + " " +  sign;
	} else if (kind == BC.ILOAD || kind == BC.ALOAD){
		line = resultType + " " + sign;
	}
	line = Integer.toString(marker)+ ": " + line;
		return line;
	} 
}
