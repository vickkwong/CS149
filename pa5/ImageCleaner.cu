#include "ImageCleaner.h"

#ifndef SIZEX
#error Please define SIZEX.
#endif
#ifndef SIZEY
#error Please define SIZEY.
#endif

#define PI	3.14159265

//----------------------------------------------------------------
// TODO:  CREATE NEW KERNELS HERE.  YOU CAN PLACE YOUR CALLS TO
//        THEM IN THE INDICATED SECTION INSIDE THE 'filterImage'
//        FUNCTION.
//
// BEGIN ADD KERNEL DEFINITIONS
//----------------------------------------------------------------

__global__ void fftx(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{

    unsigned int xIndex = blockIdx.x * size_x;

    __shared__ float fftReal[SIZEX];
    __shared__ float fftImag[SIZEX];
    __shared__ float realImage[SIZEX];
    __shared__ float imagImage[SIZEX];
    
    fftReal[threadIdx.x] = fft_real[threadIdx.x];
    fftImag[threadIdx.x] = fft_imag[threadIdx.x];
    
    realImage[threadIdx.x] = real_image[xIndex + threadIdx.x];
    imagImage[threadIdx.x] = imag_image[xIndex + threadIdx.x];
    
    __syncthreads();

    float realOut = 0.0f;
    float imagOut = 0.0f;
    
    for (unsigned int n = 0; n < size_y; ++n) {
        unsigned int term = (threadIdx.x * n) % size_y;
        realOut += (realImage[n] * fftReal[term]) - (imagImage[n] * fftImag[term]);
        imagOut += (imagImage[n] * fftReal[term]) + (realImage[n] * fftImag[term]);
    }
    
    __syncthreads();
    
    real_image[xIndex + threadIdx.x] = realOut;
    imag_image[xIndex + threadIdx.x] = imagOut;
}

__global__ void ifftx(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    unsigned int xIndex = blockIdx.x * size_x;

    __shared__ float fftReal[SIZEX];
    __shared__ float fftImag[SIZEX];
    __shared__ float realImage[SIZEX];
    __shared__ float imagImage[SIZEX];
    
    fftReal[threadIdx.x] = fft_real[threadIdx.x];
    fftImag[threadIdx.x] = fft_imag[threadIdx.x];
    
    realImage[threadIdx.x] = real_image[xIndex + threadIdx.x];
    imagImage[threadIdx.x] = imag_image[xIndex + threadIdx.x];
    
    __syncthreads();

    float realOut = 0.0f;
    float imagOut = 0.0f;
    for (unsigned int n = 0; n < size_y; ++n) {
        unsigned int term = (threadIdx.x * n) % size_y;
        realOut += (realImage[n] * fftReal[term]) - (imagImage[n] * -fftImag[term]);
        imagOut += (imagImage[n] * fftReal[term]) + (realImage[n] * -fftImag[term]);
    }
    
    __syncthreads();

    realOut /= size_y;
    imagOut /= size_y;
    
    real_image[xIndex + threadIdx.x] = realOut;
    imag_image[xIndex + threadIdx.x] = imagOut;

}

__global__ void ffty(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    __shared__ float fftReal[SIZEX];
    __shared__ float fftImag[SIZEX];
    __shared__ float realImage[SIZEX];
    __shared__ float imagImage[SIZEX];
    
    fftReal[threadIdx.x] = fft_real[threadIdx.x];
    fftImag[threadIdx.x] = fft_imag[threadIdx.x];
    
    realImage[threadIdx.x] = real_image[threadIdx.x * size_x + blockIdx.x];
    imagImage[threadIdx.x] = imag_image[threadIdx.x * size_x + blockIdx.x];

    __syncthreads();

    float realOut = 0.0f;
    float imagOut = 0.0f;
    for (unsigned int n = 0; n < size_y; ++n) {
        unsigned int term = (threadIdx.x * n) % size_x;
        realOut += (realImage[n] * fftReal[term]) - (imagImage[n] * fftImag[term]);
        imagOut += (imagImage[n] * fftReal[term]) + (realImage[n] * fftImag[term]);
    }
    
    __syncthreads();

    unsigned int xIndex = (threadIdx.x * size_x) + blockIdx.x;
    real_image[xIndex] = realOut;
    imag_image[xIndex] = imagOut;

}

__global__ void iffty(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    __shared__ float fftReal[SIZEX];
    __shared__ float fftImag[SIZEX];
    __shared__ float realImage[SIZEX];
    __shared__ float imagImage[SIZEX];
    
    fftReal[threadIdx.x] = fft_real[threadIdx.x];
    fftImag[threadIdx.x] = fft_imag[threadIdx.x];
    
    realImage[threadIdx.x] = real_image[threadIdx.x * size_x + blockIdx.x];
    imagImage[threadIdx.x] = imag_image[threadIdx.x * size_x + blockIdx.x];
    
    __syncthreads();

    float realOut = 0.0f;
    float imagOut = 0.0f;

    for (unsigned int n = 0; n < size_y; ++n) {
        unsigned int term = (threadIdx.x * n) % size_x;
        realOut += (realImage[n] * fftReal[term]) - (imagImage[n] * -fftImag[term]);
        imagOut += (imagImage[n] * fftReal[term]) + (realImage[n] * -fftImag[term]);
    }
    
    __syncthreads();
    
    realOut /= size_x;
    imagOut /= size_x;
    
    unsigned int xIndex = (threadIdx.x * size_x) + blockIdx.x;
    real_image[xIndex] = realOut;
    imag_image[xIndex] = imagOut;

}

__global__ void filter(float *real_image, float *imag_image, int size_x, int size_y)
{
    int eightX = size_x/8;
    int eight7X = size_x - eightX;
    int eightY = size_y/8;
    int eight7Y = size_y - eightY;
    
    unsigned int x = blockIdx.x;
    unsigned int y = threadIdx.x;
    
    if(!(x < eightX && y < eightY) &&
       !(x < eightX && y >= eight7Y) &&
       !(x >= eight7X && y < eightY) &&
       !(x >= eight7X && y >= eight7Y))
    {
        unsigned int yIndex = (y * size_x) + x;
        real_image[yIndex] = 0;
        imag_image[yIndex] = 0;
    }
}

//----------------------------------------------------------------
// END ADD KERNEL DEFINTIONS
//----------------------------------------------------------------

__host__ float filterImage(float *real_image, float *imag_image, int size_x, int size_y)
{
  // check that the sizes match up
  assert(size_x == SIZEX);
  assert(size_y == SIZEY);

  int matSize = size_x * size_y * sizeof(float);
  int vectorSize = size_x * sizeof(float);
  
  float *fft_real = new float[size_x];
  float *fft_imag = new float[size_x];
  
  for (unsigned int n = 0; n < size_x; ++n) {
    float term = -2 * PI * n / size_x;
    fft_real[n] = cos(term);
    fft_imag[n] = sin(term);
  }

  // These variables are for timing purposes
  float transferDown = 0, transferUp = 0, execution = 0;
  cudaEvent_t start,stop;

  CUDA_ERROR_CHECK(cudaEventCreate(&start));
  CUDA_ERROR_CHECK(cudaEventCreate(&stop));

  // Create a stream and initialize it
  cudaStream_t filterStream;
  CUDA_ERROR_CHECK(cudaStreamCreate(&filterStream));

  // Alloc space on the device
  float *device_real, *device_imag;
  CUDA_ERROR_CHECK(cudaMalloc((void**)&device_real, matSize));
  CUDA_ERROR_CHECK(cudaMalloc((void**)&device_imag, matSize));
  
  float *device_fft_real, *device_fft_imag;
  CUDA_ERROR_CHECK(cudaMalloc((void**)&device_fft_real, vectorSize));
  CUDA_ERROR_CHECK(cudaMalloc((void**)&device_fft_imag, vectorSize));

  // Start timing for transfer down
  CUDA_ERROR_CHECK(cudaEventRecord(start,filterStream));
  
  // Here is where we copy matrices down to the device 
  CUDA_ERROR_CHECK(cudaMemcpy(device_real,real_image,matSize,cudaMemcpyHostToDevice));
  CUDA_ERROR_CHECK(cudaMemcpy(device_imag,imag_image,matSize,cudaMemcpyHostToDevice));
  
  CUDA_ERROR_CHECK(cudaMemcpy(device_fft_real,fft_real,vectorSize,cudaMemcpyHostToDevice));
  CUDA_ERROR_CHECK(cudaMemcpy(device_fft_imag,fft_imag,vectorSize,cudaMemcpyHostToDevice));
  
  // Stop timing for transfer down
  CUDA_ERROR_CHECK(cudaEventRecord(stop,filterStream));
  CUDA_ERROR_CHECK(cudaEventSynchronize(stop));
  CUDA_ERROR_CHECK(cudaEventElapsedTime(&transferDown,start,stop));

  // Start timing for the execution
  CUDA_ERROR_CHECK(cudaEventRecord(start,filterStream));

  //----------------------------------------------------------------
  // TODO: YOU SHOULD PLACE ALL YOUR KERNEL EXECUTIONS
  //        HERE BETWEEN THE CALLS FOR STARTING AND
  //        FINISHING TIMING FOR THE EXECUTION PHASE
  // BEGIN ADD KERNEL CALLS
  //----------------------------------------------------------------

  // This is an example kernel call, you should feel free to create
  // as many kernel calls as you feel are needed for your program
  // Each of the parameters are as follows:
  //    1. Number of thread blocks, can be either int or dim3 (see CUDA manual)
  //    2. Number of threads per thread block, can be either int or dim3 (see CUDA manual)
  //    3. Always should be '0' unless you read the CUDA manual and learn about dynamically allocating shared memory
  //    4. Stream to execute kernel on, should always be 'filterStream'
  //
  // Also note that you pass the pointers to the device memory to the kernel call
  //exampleKernel<<<1,128,0,filterStream>>>(device_real,device_imag,size_x,size_y);
  
  fftx<<<size_x,size_y,0,filterStream>>>(device_real,device_imag,size_x,size_y,device_fft_real, device_fft_imag);
  ffty<<<size_x,size_y,0,filterStream>>>(device_real,device_imag,size_x,size_y,device_fft_real, device_fft_imag);
  
  filter<<<size_x,size_y,0,filterStream>>>(device_real,device_imag,size_x,size_y);
  
  ifftx<<<size_x,size_y,0,filterStream>>>(device_real,device_imag,size_x,size_y,device_fft_real, device_fft_imag);
  iffty<<<size_x,size_y,0,filterStream>>>(device_real,device_imag,size_x,size_y,device_fft_real, device_fft_imag);

  //---------------------------------------------------------------- 
  // END ADD KERNEL CALLS
  //----------------------------------------------------------------

  // Finish timimg for the execution 
  CUDA_ERROR_CHECK(cudaEventRecord(stop,filterStream));
  CUDA_ERROR_CHECK(cudaEventSynchronize(stop));
  CUDA_ERROR_CHECK(cudaEventElapsedTime(&execution,start,stop));

  // Start timing for the transfer up
  CUDA_ERROR_CHECK(cudaEventRecord(start,filterStream));

  // Here is where we copy matrices back from the device 
  CUDA_ERROR_CHECK(cudaMemcpy(real_image,device_real,matSize,cudaMemcpyDeviceToHost));
  CUDA_ERROR_CHECK(cudaMemcpy(imag_image,device_imag,matSize,cudaMemcpyDeviceToHost));

  // Finish timing for transfer up
  CUDA_ERROR_CHECK(cudaEventRecord(stop,filterStream));
  CUDA_ERROR_CHECK(cudaEventSynchronize(stop));
  CUDA_ERROR_CHECK(cudaEventElapsedTime(&transferUp,start,stop));

  // Synchronize the stream
  CUDA_ERROR_CHECK(cudaStreamSynchronize(filterStream));
  // Destroy the stream
  CUDA_ERROR_CHECK(cudaStreamDestroy(filterStream));
  // Destroy the events
  CUDA_ERROR_CHECK(cudaEventDestroy(start));
  CUDA_ERROR_CHECK(cudaEventDestroy(stop));

  // Free the memory
  CUDA_ERROR_CHECK(cudaFree(device_real));
  CUDA_ERROR_CHECK(cudaFree(device_imag));
  CUDA_ERROR_CHECK(cudaFree(device_fft_real));
  CUDA_ERROR_CHECK(cudaFree(device_fft_imag));


  // Dump some usage statistics
  printf("CUDA IMPLEMENTATION STATISTICS:\n");
  printf("  Host to Device Transfer Time: %f ms\n", transferDown);
  printf("  Kernel(s) Execution Time: %f ms\n", execution);
  printf("  Device to Host Transfer Time: %f ms\n", transferUp);
  float totalTime = transferDown + execution + transferUp;
  printf("  Total CUDA Execution Time: %f ms\n\n", totalTime);
  // Return the total time to transfer and execute
  return totalTime;
}

