import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;


public class OPstack {

	private Vector<String> visual;	
	Operand[] stack;
	int top;
	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));;
	
	OPstack(int size){
		visual = new Vector<String>(size);
		stack = new Operand[size];
		top = 0;
	}
	
	void push(Operand op){
		this.displayStack(op);
		stack[top++] = op;
	}
	Operand pop(){
		this.displayStack();
		return stack[--top];
	}
	
	void displayStack(Operand op){
		System.out.println();

			//integer entry
				if(op.kind == BC2IR.REGISTER) {
						if(op.register.isLocal){
					visual.add(0, "Lokales Register l"+Integer.toString(op.register.number));}
						else {visual.add(0, "temporäres Register t"+Integer.toString(op.register.number));}

				//reference entry
				} else {

					visual.add(0, Integer.toString(op.value.value));
				}

				System.out.println("\t\t       push()\n");

			//remove topmost stack entry
			
			//print the content of visual stack
			if(visual.size() == 0) {

				System.out.println("\t\t       |  (empty)");
				System.out.println("\t\t       ---------------------");
			}

			for(int i = 0; i < visual.size(); i++) {

				System.out.println("\t\t       |  " + visual.get(i));
				System.out.println("\t\t       ---------------------");
			}

		try {
			reader.readLine();

		} catch(IOException e) {

			e.printStackTrace();
		}
	
	}
	
	void displayStack() {
		System.out.println();
			visual.remove(0);
			System.out.println("\t\t       pop()\n");
		

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
		
			try {
				reader.readLine();

			} catch(IOException e) {

				e.printStackTrace();
			}
			}
		
}
