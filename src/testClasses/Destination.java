class Destination {

	final int home = 0;

	int target;

	Destination(int i) {

		if(target == home) {

			target = 1;

		} else {

			target = i;
		}
	}
}
