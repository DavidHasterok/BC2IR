class Euklid {

    static int ggt(int a, int b) {

	if (b == 0) {

	    return a ;

	} else {

	    if (a == 0) {

		return b ;

	    } else {

		if (a > b) {

		    return ggt (a-b, b);

		} else {

		}
	    }
	}

	return ggt (a, b-a);
    }
}