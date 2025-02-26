#include <stdio.h>
#include <stdlib.h>
int recurse(int N);

int main(int argc, char const *argv[]) {
    // DO SELF TESTER
    char const *_input = argv[1];
    int _number = atoi(_input);
    int _output = recurse(_number);
    printf("%d\n", _output);
    return EXIT_SUCCESS;
}
int recurse(int N) {
    if (N == 0) {
        return 2;
    }
    else {
        return 7 + 3*N - 2*(recurse(N-1));
    }
}