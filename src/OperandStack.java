import java.util.Vector;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

class OperandStack {

	private Vector<TinyJVMType> stack;
	private Vector<String> visual;		//for stack visualisation
	private RuntimeConstantPool rcp;	//for stack visualisation
	private boolean showStack;		//for stack visualisation
	private boolean haltStack;		//for stack visualisation
	private Frame frame;			//for stack visualisation
	BufferedReader reader;			//for single stack visualisation

	OperandStack(int size, RuntimeConstantPool constPool, Frame f, boolean show, boolean halt) {

		stack = new Vector<TinyJVMType>(size);		
		showStack = show;
		haltStack = halt;

		if(show) {

			visual = new Vector<String>(size);
			rcp = constPool;
			frame = f;

			if(halt) {

				reader = new BufferedReader(new InputStreamReader(System.in));
			}
		}
	}

	void push(TinyJVMType jvmType) {

		if(showStack) {

			//add jvmType to visual stack
			printStack(jvmType);
		}

		//first position: top stack element
		stack.add(0, jvmType);
	}

	TinyJVMType pop() {

		if(showStack) {

			printStack();
		}

		//return topmost stack entry and discard it from stack		
		return stack.remove(0);
	}

	//for stack visualisation
	void beginFrame() {

		if(showStack) {

			//print properties of the method
			String methString = "";

			if(frame.isStatic) {

				methString = methString + "static ";
			}

			int paranIndex1 = frame.methType.indexOf("(");
			int paranIndex2 = frame.methType.indexOf(")");

			//print return type and name
			if(frame.methType.substring(paranIndex2 + 1).equals("V")) {

				methString = methString + "void " + frame.methName + "(";

			} else {

				methString = methString + "int " + frame.methName + "(";
			}

			//print the parameters
			int i = paranIndex1 + 1;
			while(i < paranIndex2) {

				String paraString = frame.methType.substring(i, i + 1);

				if(paraString.equals("I")) {

					methString = methString + "int, ";
					i++;

				} else if(paraString.equals("[")) {

					methString = methString + "int[] ,";
					i = i + 2;

				} else {

					int endIndex = paraString.indexOf(";", i);
					String sub = paraString.substring(i + 1, endIndex);
					methString = methString + sub;
					i = i + sub.length() + 2;
				}
			}

			if(paranIndex1 != paranIndex2 - 1) {

				//delete the last ", ", if method has parameters
				methString = methString + "\b\b";

			}

			methString = methString + ") of class " + frame.rcp.className;

			//begin with empty stack
			System.out.println("\nNew operand stack: method " + methString + "\n");
			System.out.println("\t\t       |  (empty)");
			System.out.println("\t\t       ---------------------");
		}
	}

	void endFrame() {

		if(showStack) {

			System.out.println("\nReturn to previous frame.\n");
		}
	}

	void endExecution() {

		if(showStack) {

			System.out.println("\nExecution finished.");
		}
	}

	//TinyJVMType...: for handling 0 or 1 argument (or more, but not used)
	void printStack(TinyJVMType... type) {
		System.out.println();

		//push given entry onto visual stack
		if(type.length != 0) {

			//integer entry
			if(!type[0].isReference) {

				visual.add(0, Integer.toString(type[0].value));

			//reference entry
			} else {

				visual.add(0, ((TinyJVMRefType)type[0]).instance.className);
			}

			System.out.println("\t\t       push()\n");

		//remove topmost stack entry
		} else {

			visual.remove(0);
			System.out.println("\t\t       pop()\n");
		}

		//print the content of visual stack
		if(visual.size() == 0) {

			System.out.println("\t\t       |  (empty)");
			System.out.println("\t\t       ---------------------");
		}

		for(int i = 0; i < visual.size(); i++) {

			System.out.println("\t\t       |  " + visual.get(i));
			System.out.println("\t\t       ---------------------");
		}

		//pause stack printing if haltStack true
		if(haltStack) {

			try {
				reader.readLine();

			} catch(IOException e) {

				e.printStackTrace();
			}
			}
		}
	
}
