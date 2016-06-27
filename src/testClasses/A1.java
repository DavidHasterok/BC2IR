class A1{

	int a;

	int doA() {

		return a;
	}

	static void m(int a, int b){

		int l;
		int k;
		l = 0;
		k = 0;

		A1 c = A.a;
		a = c.a;
		a = 3 + 5 + a + b;

		if(a > 3){

			k = 6;

			while(k > b){

				k = k + m1(1)  + 1;
				m(9,0);
				return;
			}

		} else {

			l = 7;
		} 
	}

	static int m1(int a){

		int l;
		int k;
		l = 0;
		a = 3 + 5;

		if(a > 3){

			k = 6;
			return 1;

		} else {

			l = 7;

			if(a == 3){

				return 1;

			} else {

				k = 7;
				return 1;
			}
		}	
	}
}