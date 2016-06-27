class ArrayInstance extends Instance {

	int size;
	int[] intArray;

	ArrayInstance(int length) {

		isArray = true;
		intArray = new int[length];
		size = length;
	}

	int getElement(int index) {

		if(index >= size || index < 0) {

			throw new IndexOutOfBoundsException("Exception at ArrayInstace.getElement(): " + 
				"index " + index + " was out of bounds for an array of size" + size);
		}

		return intArray[index];
	}

	void setElement(int index, int value) {

		if(index >= size || index < 0) {

			throw new IndexOutOfBoundsException("Exception at ArrayInstance.setElement(): " + 
				"index " + index + " was out of bounds for an array of size " + size);
		}

		intArray[index] = value;
	}
}