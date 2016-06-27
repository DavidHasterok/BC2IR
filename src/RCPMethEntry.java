class RCPMethEntry extends RCPFieldOrMethEntry {

	//for a direct reference to a method 
	//table entry inside the constant pool 
	//(dynamic linking)
	boolean isResoluted;
	MethodTableEntry methTabEntry;
}
