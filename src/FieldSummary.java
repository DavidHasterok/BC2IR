import java.util.Vector;

//summarising all fields of a class in order to 
//be able to create instances more easily
class FieldSummary {

	String className;
	Vector<Field> fields;

	FieldSummary(String name) {

		className = name;
		fields = new Vector<Field>();
	}

	void addField(Field f) {

		if(f != null) {

			fields.add(f);

		} else {

			throw new InternalError("Error at FieldSummary.addField():" + 
				"Given Field reference was null.");
		}
	}
}