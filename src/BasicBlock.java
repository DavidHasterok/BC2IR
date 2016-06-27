import java.io.IOException;
import java.util.Vector;


public class BasicBlock {

	BasicBlock branch, fail;
	Instruction first, last;
	int size = 0;
	int number;
	int stopAt;
	private Vector<String> visual;
	
	
	BasicBlock( int i){
		this.first = new Instruction();
		this.number = i;
	}
	
void displayInstructions(){
Instruction tmp = first;
visual = new Vector<String>(size);
String line = null;

int i = 0;
while(tmp != null && tmp != last){
	tmp = tmp.next;
	if (tmp != null && tmp.kind != BC.NOP){	
		line = tmp.representation();
	   visual.add(line);}
	i++;	
	
}
System.out.println("BasicBlock " + Integer.toString(number)+":");	
	if(visual.size() == 0) {

		System.out.println("\t\t      (empty)");		
	}

	for( i = 0; i < visual.size(); i++) {

		System.out.println("\t\t      " + visual.get(i));
	}



}
}