class Counter {

int fibo(int a, int b, int i){
int c = 0;
while (i >0) {
	c = a + b;
	a = b;
	b = c;
	i = i - 1;
        }
return c;
}
}
