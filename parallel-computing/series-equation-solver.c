#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <mpi.h>

/* Daniel Fischer
* made with love and coffee
*/

// /***** Globals ******/
float *a;   /* The coefficients */
float *x;	/* The unknowns */
float *b;	/* The constants */
// float err;   /* The absolute relative error */
// int num = 0; /* number of unknowns */

/****** Function declarations */
void check_matrix(); /* Check whether the matrix will converge */
void get_input();	/* Read input from file */
// int solveCoefficients(); /* Solve system of equations */
// bool checkError(); /* Check error */

/********************************/

/* Function definitions: functions are ordered alphabetically ****/
/*****************************************************************/

/* 
   Conditions for convergence (diagonal dominance):
   1. diagonal element >= sum of all other elements of the row
   2. At least one diagonal element > sum of all other elements of the row
 */
void check_matrix(int num){
	int bigger = 0; /* Set to 1 if at least one diag element > sum  */
	int i, j;
	float sum = 0;
	float aii = 0;

	for (i = 0; i < num; i++)
	{
		sum = 0;
		aii = fabs(a[i*num + i]);

		for (j = 0; j < num; j++)
			if (j != i)
				sum += fabs(a[i*num + j]);

		if (aii < sum)
		{
			printf("The matrix will not converge.\n");
			exit(1);
		}

		if (aii > sum)
			bigger++;
	}

	if (!bigger)
	{
		printf("The matrix will not converge\n");
		exit(1);
	}
}

/******************************************************/
/* Read input from file */
/* After this function returns:
 * a[] will be filled with coefficients and you can access them using a[i*num + j] for element (i,j)
 * x[] will contain the initial values of x
 * b[] will contain the constants (i.e. the right-hand-side of the equations
 * num will have number of variables
 * err will have the absolute error that you need to reach
 */
void get_input(char filename[], int myRank, int commSize, int *num, float *err){
	
	//core 0 handles file input
	if (myRank == 0){
		FILE *fp;
		int i, j;

		fp = fopen(filename, "r");
		if (!fp)
		{
			printf("Cannot open file %s\n", filename);
			exit(1);
		}

		fscanf(fp, "%d ", num);
		fscanf(fp, "%f ", err);

		a = (float *)malloc(*num * *num * sizeof(float *));
		if (!a)
		{
			printf("Cannot allocate a!\n");
			exit(1);
		}

		x = (float *)malloc(*num * sizeof(float));
		if (!x)
		{
			printf("Cannot allocate x!\n");
			exit(1);
		}

		b = (float *)malloc(*num * sizeof(float));
		if (!b)
		{
			printf("Cannot allocate b!\n");
			exit(1);
		}

		/* Now .. Filling the blanks */

		/* The initial values of Xs */
		for (i = 0; i < *num; i++){
			fscanf(fp, "%f ", &x[i]);
		}

		for (i = 0; i < *num; i++)
		{
			for (j = 0; j < *num; j++){
				fscanf(fp, "%f ", &a[i * *num + j]);
			}

			/* reading the b element */
			fscanf(fp, "%f ", &b[i]);
		}

		fclose(fp);	
	} // end if core 0


	MPI_Bcast(num, 1, MPI_INT, 0, MPI_COMM_WORLD);
	MPI_Bcast(err, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

	if (myRank != 0){
		a = (float *)malloc(*num * *num * sizeof(float *));
		if (!a)
		{
			printf("Cannot allocate a!\n");
			exit(1);
		}

		x = (float *)malloc(*num * sizeof(float));
		if (!x)
		{
			printf("Cannot allocate x!\n");
			exit(1);
		}

		b = (float *)malloc(*num * sizeof(float));
		if (!b)
		{
			printf("Cannot allocate b!\n");
			exit(1);
		}
	}


	//send out arrays 
	MPI_Bcast(a, *num * *num, MPI_FLOAT, 0, MPI_COMM_WORLD);
	MPI_Bcast(x, *num, MPI_FLOAT, 0, MPI_COMM_WORLD);
   	MPI_Bcast(b, *num, MPI_FLOAT, 0, MPI_COMM_WORLD);

}

/************************************************************/

char checkError(int start, int end, float *xNew, float *xOld, float err)
{
	for (int i = start; i < end; i++)
	{
		if (fabsf((xNew[i] - xOld[i]) / xNew[i]) > err){
			return 1;
		}
	}
	//printf("%d, %d, %f, %f\n", start, end, xNew[start], xNew[end-1]);
	return 0;
}

int solveCoefficients(int start, int end, int *problemLen, int *displs, int num, float err, int myRank)
{
	int i, j;
	int cycle = 0;
	int length = end - start;
	char errorGreater, globalError;
	//printf("%d, %d, %d\n", myRank, start, end);


	//allocate alternating buffers for x_new and x_old
	float *xBuff1 = (float *)malloc(num * sizeof(float));
	float *xBuff2 = (float *)malloc(num * sizeof(float));

	if (!xBuff1 || !xBuff2){
		printf("Cannot allocate x buffers!");
		exit(1);
	}

	//copy initial vals of x into buffer 2
	for (i = 0; i < num; i++){
		xBuff2[i] = x[i];
	}

	while (1){
		cycle++;
		for (i = start; i < end; i++)
		{
			xBuff1[i] = b[i];

			for (j = 0; j < num; j++)
			{
				if (j == i)
					continue;
				xBuff1[i] -= a[i*num + j] * xBuff2[j];
			}
			xBuff1[i] /= a[i*num + i];
		}

		//determine error status for this subset
		errorGreater = checkError(start, end, xBuff1, xBuff2, err);
		//printf("%d, %d, %d\n", myRank, cycle, errorGreater);
		MPI_Allreduce(&errorGreater, &globalError, 1, MPI_CHAR, MPI_LOR, MPI_COMM_WORLD);
		//printf("%d: x+%d, %d, %d, %d\n", myRank, start, length, problemLen[myRank], displs[myRank]);
		MPI_Allgatherv(xBuff1 + start, length, MPI_FLOAT, xBuff2, problemLen, displs, MPI_FLOAT, MPI_COMM_WORLD);

		if (!globalError){
			MPI_Gatherv(xBuff1 + start, length, MPI_FLOAT, x, problemLen, displs, MPI_FLOAT, 0, MPI_COMM_WORLD);
			free(xBuff2);
			break;
		}
	}
	return cycle;
}

int main(int argc, char *argv[])
{
	int myRank, commSize, num, i, myStart, myEnd;
	float err;
	int nit = 0; /* number of iterations */
	FILE *fp;
	char output[100] = "";

	if (argc != 2)
	{
		printf("Usage: ./gsref filename\n");
		exit(1);
	}

	MPI_Init(NULL, NULL);
   	MPI_Comm_rank(MPI_COMM_WORLD, &myRank);
   	MPI_Comm_size(MPI_COMM_WORLD, &commSize);


	/* Read the input file and fill the global data structure above */
	get_input(argv[1], myRank, commSize, &num, &err);



	/* Check for convergence condition */
	/* This function will exit the program if the coffeicient will never converge to 
  * the needed absolute error. 
  * This is not expected to happen for this programming assignment.
  */
    //if (myRank == 0)
		//check_matrix(a, num);


	//calculate start and end indices for each process

	int remain = num % commSize;
	int commFloor = (int) num / commSize;

	int* problemLen = (int *) malloc(commSize * sizeof(int));
	int* displs = (int *) malloc(commSize * sizeof(int));

	displs[0] = 0;

	for(i = 0; i < commSize; i++){
		problemLen[i] = commFloor;
		if (i < remain)
			problemLen[i] += 1;
		if (i >= 1)
			displs[i] = displs[i-1] + problemLen[i-1];
	}

	if (myRank < remain){
		myStart = myRank*commFloor + myRank;
		myEnd = (myRank + 1)*commFloor + myRank + 1;
	}
	else{
		myStart = myRank*commFloor + remain;
		myEnd = (myRank + 1)*commFloor + remain;
	}

	nit = solveCoefficients(myStart, myEnd, problemLen, displs, num, err, myRank);

	if (myRank == 0){
		/* Writing results to file */
		sprintf(output, "%d.sol", num);
		fp = fopen(output, "w");
		if (!fp)
		{
			printf("Cannot create the file %s\n", output);
			exit(1);
		}

		for (i = 0; i < num; i++)
			fprintf(fp, "%f\n", x[i]);

		printf("total number of iterations: %d\n", nit);

		fclose(fp);
	}
	MPI_Finalize();
	exit(0);
}
