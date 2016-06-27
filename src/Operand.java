
public class Operand {
	
	TinyJVMRefType type;
	TinyJVMType value;
	short kind;
	Instruction target;
	Register register;
	BasicBlock bb;
	String className;
Operand(short kind, int code){
	this.kind = kind;
	
	switch(kind) {
	case BC2IR.REGISTER: register = new Register(code); register.op = this; value = TinyJVMType.toJVMType(register.number); break;
	
	case BC2IR.INT_CONSTANT: value = TinyJVMType.toJVMType(code); break;
	
	case BC2IR.BRANCH: bb = new BasicBlock(code);
	}
	
}

Operand(short kind, String name, int code){
	this.kind = kind;
	
	switch(kind) {
	case BC2IR.FIELD: {this.value = TinyJVMType.toJVMType(code);
						this.className = name;}
						break;
	}
	
}

Operand(short kind, String name, TinyJVMRefType type){
	this.kind = kind;
	this.className = name;
	this.type = type;
}

Operand(short kind, TinyJVMType constant){
	this.kind = kind;
	value = constant;
}

Register getRegister(){
	return this.register;
}

void setLocal(){
	this.register.isLocal = true;
}

}
