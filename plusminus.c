#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
//at this point in time, everything works
typedef struct PlayerStats {
    char name[64];
    int points_for;
    int points_against;
    int points_diff;
    struct PlayerStats* next;
}Player;
void sort(Player** begin) {
    bool not_sorted = true;

    while (not_sorted) {
        Player* curr = *begin;
        Player* prev = NULL;

        not_sorted = false;

        while (curr != NULL) {
            Player* next = curr->next;
            if(next!=NULL){if (
                (curr->points_diff < next->points_diff) || //points_diff
                (next->points_diff == curr->points_diff && strcmp(curr->name, next->name)>0)) //naming
            {
                not_sorted = true;

                /*SWAP*/
                curr->next = next->next;
                next->next = curr;
                if (prev==NULL) {*begin = next;}
                else {prev->next = next;}
            }
            prev = curr;
            curr = next;}
            else {curr=NULL;}
        }
    }
}

int main(int argc, char const *argv[]) {
    FILE *fp;
    char str[64];
    int total_entries = 0;

    fp = fopen(argv[1], "r");
    if(fp==NULL) {
        perror("Error Opening File");
        return EXIT_FAILURE;
    }
    
    char temp_name[64];
    Player* head; 
    bool first_iteration = true;

    while(1) {

        strcpy(temp_name, fgets(str, 63, fp));

        char *nullchar = strchr(temp_name, '\n');
        *nullchar = '\0';

        if(strcmp(temp_name,"DONE")==0) {
            break;
        }
        
        Player* curr = (Player*)malloc(sizeof(Player));

        strcpy(curr->name, temp_name);
        curr->points_for = atoi(fgets(str, 63, fp));
        curr->points_against = atoi(fgets(str, 63, fp));
        curr->points_diff = curr->points_for - curr->points_against;

        // adds each element to the front of the list
        if (first_iteration) {head = curr; curr->next = NULL; first_iteration=false;}
        else {curr->next = head; head = curr;}
        
        total_entries++;
    }    
    sort(&head);

    Player* print_iterator = head;
    for (int i=0;i<total_entries;i++) {
        printf("%s %d\n",print_iterator->name, print_iterator->points_diff);
        print_iterator=print_iterator->next;
        free(head);
        head = print_iterator;
    }
    free(print_iterator);
    fclose(fp);
    return EXIT_SUCCESS;
}