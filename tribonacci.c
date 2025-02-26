#include <stdio.h>
#include <stdlib.h>

int main(int argc, char const *argv[]) {
    // DO SELF TESTER
    char const *trib_input = argv[1];
    int trib_number = atoi(trib_input);
    
    int c = 0;
    int t1 = 1; int t2 = 1; int t3 = 2;
    int temp = 0;
    while (c < trib_number) {
        printf("%d\n",t1);
        temp = t1 + t2 + t3;
        t1 = t2; t2 = t3; t3 = temp;
        c++;
    }
    
    return EXIT_SUCCESS;
}