import java.util.Vector;

class Instance {

	String className;
	boolean isArray;

	Vector<Field> fields;

	Instance(FieldSummary fieldSum) {

		isArray = false;
		fields = new Vector<Field>();

		for(int i = 0; i < fieldSum.fields.size(); i++) {

			fields.add(fieldSum.fields.get(i));
		}
	}

	//for ArrayInstance() to be able to call a superclass constructor
	Instance() {
	}

	Field getField(String fieldName, String fieldType) {

		int i = 0;

		while(i < fields.size() && !(fields.get(i).name.equals(fieldName) && 
			fields.get(i).type.equals(fieldType))) {

			i++;
		}

		if(i >= fields.size()) {

			throw new NoSuchFieldError("Error at Instance.getField(): " + 
				"field " + fieldType + " " + fieldName + " was not found.");
		}

		return fields.get(i);
	}
}
