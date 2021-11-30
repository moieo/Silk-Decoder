#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char *argv[]){
    unsigned len = 60;
    char *bar = (char *)malloc(sizeof(char) * (len + 1));
    for (int i = 0; i < len + 1; ++i)
    {
        bar[i] = '#';
    }
    for (int i = 0; i < len; ++i)
    {
        printf("progress:[%s]%d%%\r", bar+len-i, i+1);
        fflush(stdout);
        usleep(100000);
        //sleep(1);
    }
    printf("\n");
    return 0;
}