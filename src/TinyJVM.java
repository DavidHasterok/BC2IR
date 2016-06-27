class TinyJVM {

	//storage of data structures
	private static Heap heap;

	//bootstrap class loader
	private static ClassLoader bscl;

	//interpreter
	private static ExecutionEngine exEngine;

	//calling TinyJVM for the main class file
	public static void main(String[] args) {

		boolean debugLoading = false;
		boolean debugExecution = false; //das auf false setzen für meins
		boolean stopOperandStack = true;
		heap = new Heap();

		//loading main class
		try {
			String firstArg = args[0];
			String pathName = "";
			String fileName = firstArg;

			if(firstArg.contains("/")) {

				pathName = firstArg.substring(0, firstArg.lastIndexOf("/") + 1);
				fileName = firstArg.substring(firstArg.lastIndexOf("/") + 1);
			}

			bscl = new ClassLoader(pathName, heap);
			bscl.load(fileName, debugLoading);

		} catch(ArrayIndexOutOfBoundsException e) {

			System.out.println("No class file was specified.");
		}

		//executing tinyMain()
		try {
			exEngine = new ExecutionEngine(heap, bscl);
			exEngine.executeMain(debugExecution, stopOperandStack);

		} catch(Exception e) {

			e.printStackTrace();
		}
	}
}
