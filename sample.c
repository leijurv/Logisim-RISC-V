#include <stdio.h>
int factorial(int x){
	if(x<=1){
		return 1;
	}
	return x*factorial(x-1);
}
int fibo(int x){
	if(x<2){
		return 1;
	}
	return fibo(x-1)+fibo(x-2);
}
int main(void){
int sum=0;
for(int i=0; i<1000; i++){
if((i&1)==0 || ((i&11)==0)){
sum+=i;
}
if(i>3 && i<7 && (i&3)<3){
	sum+=(int)factorial((fibo(i)-5));
}
}

//printf("%x %d \n",a,a);
//printf("%x %d \n",sum,sum);
int* death= malloc(40);
for(int i=0; i<10; i++){
	death[i]=(int)(sum+((float)i)*5.76f);
	//printf("%x ",death[i]); // uncomment this line and run normally to verify what the correct output should be
}
return death;
//return sum;
}
