class Modifiers {

	int nonModified;
	static int staticInt;
	final int finalInt = 19;
	static final int staticFinalInt = 48;

	void method1(int a, int b) {

		int c = finalInt / a + b;
	}

	static int method2() {

		return staticInt;
	}

	static void tinyMain() {

		staticInt = 4 * staticInt - staticFinalInt;

		Modifiers m = new Modifiers();
		m.method1(staticInt, 9);
		m.method2();
	}
}