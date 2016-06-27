class TinyJVMType {

	int value;
	boolean isReference;

	static TinyJVMType toJVMType(int val) {

		TinyJVMType type = new TinyJVMType();
		type.value = val;
		type.isReference = false;
		return type;
	}
}