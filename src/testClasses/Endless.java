class Endless {

	int a;

	void m(int a, int b) {

		int l;
		int k;
		l = 0;
		k = 0;

		a = 3 + 5 + a + b;

		if(a > 3) {

			k = 6;

			while(k > b) {
			
				k = k + m1(1) + 1;
				m(9,0);
				return;
			}
			
		} else {
		
			l = 7;
		}
		
		while (k > 1) {
		
			k = k + 1;
		}
		
		m(1,2);
	}

	int m1(int a) {
	
		int l;
		int k;
		l = 0;
		a = 3 + 5;
		
		if(a > 3) {
		
			k = 6;
			return 1;
			
		} else {
		
			l = 7;
			
			if(a == 3) {

				return 1;
				
			} else {

				k = 7;
				return 1;
			}
		}
	}

	static void tinyMain() {

		Endless calculate = new Endless();
		calculate.a = 16;
		calculate.m(8, 4);
	}
}
