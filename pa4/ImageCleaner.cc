#include "ImageCleaner.h"
#include <math.h>
#include <sys/time.h>
#include <stdio.h>
#include <omp.h>

#define PI	3.14159265

void cpu_fftx(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    unsigned int x;
    unsigned int y;
    unsigned int n;
    unsigned int term;
    unsigned int xIndex;
    
    #pragma omp parallel shared(size_x, size_y, real_image, imag_image, fft_real, fft_imag) private(x, y, term, n, xIndex)
    {
        #pragma omp for schedule(guided)
        for(x = 0; x < size_x; x++)
        {
            xIndex = x * size_x;
            float *realOutBuffer = new float[size_x];
            float *imagOutBuffer = new float[size_x];
            
            for(y = 0; y < size_y; y++)
            {
                realOutBuffer[y] = 0.0f;
                imagOutBuffer[y] = 0.0f;
                // Compute the frequencies for this index
                for(n = 0; n < size_y; n++)
                {
                    term = (y * n) % size_y;
                    realOutBuffer[y] += (real_image[xIndex + n] * fft_real[term]) - (imag_image[xIndex + n] * fft_imag[term]);
                    imagOutBuffer[y] += (imag_image[xIndex + n] * fft_real[term]) + (real_image[xIndex + n] * fft_imag[term]);

                }

            }
            // Write the buffer back to were the original values were
            for(y = 0; y < size_y; y++)
            {
                real_image[xIndex + y] = realOutBuffer[y];
                imag_image[xIndex + y] = imagOutBuffer[y];
            }
            delete [] realOutBuffer;
            delete [] imagOutBuffer;
        }
    }
}

// This is the same as the thing above, except it has a scaling factor added to it
void cpu_ifftx(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    unsigned int x;
    unsigned int y;
    unsigned int n;
    unsigned int term;
    unsigned int xIndex;
    
    #pragma omp parallel shared(size_x, size_y, real_image, imag_image, fft_real, fft_imag) private(x, y, term, n, xIndex)
    {
        #pragma omp for schedule(guided)
        for(x = 0; x < size_x; x++)
        {
            xIndex = x * size_x;
            float *realOutBuffer = new float[size_x];
            float *imagOutBuffer = new float[size_x];
            for(y = 0; y < size_y; y++)
            {
                realOutBuffer[y] = 0.0f;
                imagOutBuffer[y] = 0.0f;
                for(n = 0; n < size_y; n++)
                {
                    // Compute the frequencies for this index
                    term = (y * n) % size_y;
                    realOutBuffer[y] += (real_image[xIndex + n] * fft_real[term]) - (imag_image[xIndex + n] * -fft_imag[term]);
                    imagOutBuffer[y] += (imag_image[xIndex + n] * fft_real[term]) + (real_image[xIndex + n] * -fft_imag[term]);
                }
                
                // Incoporate the scaling factor here
                realOutBuffer[y] /= size_y;
                imagOutBuffer[y] /= size_y;
            }
            // Write the buffer back to were the original values were
            for(y = 0; y < size_y; y++)
            {
                real_image[xIndex + y] = realOutBuffer[y];
                imag_image[xIndex + y] = imagOutBuffer[y];
            }
            delete [] realOutBuffer;
            delete [] imagOutBuffer;
        }
    }
}

void cpu_ffty(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    unsigned int x;
    unsigned int y;
    unsigned int n;
    unsigned int term;
    unsigned int yIndex;
    
    #pragma omp parallel shared(size_x, size_y, real_image, imag_image, fft_real, fft_imag) private(x, y, term, n, yIndex)
    {
        #pragma omp for schedule(guided)
        for(y = 0; y < size_y; y++)
        {
            float *realOutBuffer = new float[size_y];
            float *imagOutBuffer = new float[size_y];
            for(x = 0; x < size_x; x++)
            {
                
                realOutBuffer[x] = 0.0f;
                imagOutBuffer[x] = 0.0f;
                for(n = 0; n < size_y; n++)
                {
                    term = (x * n) % size_x;
                    yIndex = n*size_x + y;
                    realOutBuffer[x] += (real_image[yIndex] * fft_real[term]) - (imag_image[yIndex] * fft_imag[term]);
                    imagOutBuffer[x] += (imag_image[yIndex] * fft_real[term]) + (real_image[yIndex] * fft_imag[term]);
                }
            }
            // Write the buffer back to were the original values were
            for(x = 0; x < size_x; x++)
            {
                real_image[x*size_x + y] = realOutBuffer[x];
                imag_image[x*size_x + y] = imagOutBuffer[x];
            }
            delete [] realOutBuffer;
            delete [] imagOutBuffer;
        }
    }
}

// This is the same as the thing about it, but it includes a scaling factor
void cpu_iffty(float *real_image, float *imag_image, int size_x, int size_y, float *fft_real, float *fft_imag)
{
    unsigned int x;
    unsigned int y;
    unsigned int n;
    unsigned int term;
    unsigned int yIndex;
    
    #pragma omp parallel shared(size_x, size_y, real_image, imag_image, fft_real, fft_imag) private(x, y, term, n, yIndex)
    {
        #pragma omp for schedule(guided)
        for(y = 0; y < size_y; y++)
        {
            
            float *realOutBuffer = new float[size_y];
            float *imagOutBuffer = new float[size_y];
            
            for(x = 0; x < size_x; x++)
            {
                realOutBuffer[x] = 0.0f;
                imagOutBuffer[x] = 0.0f;
                // Compute the frequencies for this index
                for(n = 0; n < size_y; n++)
                {
                    // Note that the negative sign goes away for the term
                    term = (x * n) % size_x;
                    yIndex = n*size_x +y;
                    realOutBuffer[x] += (real_image[yIndex] * fft_real[term]) - (imag_image[yIndex] * -fft_imag[term]);
                    imagOutBuffer[x] += (imag_image[yIndex] * fft_real[term]) + (real_image[yIndex] * -fft_imag[term]);
                }
                
                // Incorporate the scaling factor here
                realOutBuffer[x] /= size_x;
                imagOutBuffer[x] /= size_x;
            }
            // Write the buffer back to were the original values were
            for(x = 0; x < size_x; x++)
            {
                real_image[x*size_x + y] = realOutBuffer[x];
                imag_image[x*size_x + y] = imagOutBuffer[x];
            }
            delete [] realOutBuffer;
            delete [] imagOutBuffer;
        }
    }
}

void cpu_filter(float *real_image, float *imag_image, int size_x, int size_y)
{
    int eightX = size_x/8;
    int eight7X = size_x - eightX;
    int eightY = size_y/8;
    int eight7Y = size_y - eightY;
    
    unsigned int x;
    unsigned int y;
    
    #pragma omp parallel shared(eightX, eight7X, eightY, eight7Y) private(x, y)
    {
        #pragma omp for schedule(guided)
        for(x = 0; x < size_x; x++)
        {
            for(y = 0; y < size_y; y++)
            {
                if(!(x < eightX && y < eightY) &&
                   !(x < eightX && y >= eight7Y) &&
                   !(x >= eight7X && y < eightY) &&
                   !(x >= eight7X && y >= eight7Y))
                {
                    // Zero out these values
                    real_image[y*size_x + x] = 0;
                    imag_image[y*size_x + x] = 0;
                }
            }
        }
    }
}

float imageCleaner(float *real_image, float *imag_image, int size_x, int size_y)
{
    // These are used for timing
    struct timeval tv1, tv2;
    struct timezone tz1, tz2;
    
    // Start timing
    gettimeofday(&tv1,&tz1);
    
    float *fft_real = new float[size_x];
    float *fft_imag = new float[size_x];
    
    for (int n = 0; n < size_x; ++n) {
        float term = -2 * PI * n / size_x;
        fft_real[n] = cos(term);
        fft_imag[n] = sin(term);
    }
    
    // Perform fft with respect to the x direction
    cpu_fftx(real_image, imag_image, size_x, size_y, fft_real, fft_imag);
    // Perform fft with respect to the y direction
    cpu_ffty(real_image, imag_image, size_x, size_y, fft_real, fft_imag);
    
    // Filter the transformed image
    cpu_filter(real_image, imag_image, size_x, size_y);
    
    // Perform an inverse fft with respect to the x direction
    cpu_ifftx(real_image, imag_image, size_x, size_y, fft_real, fft_imag);
    // Perform an inverse fft with respect to the y direction
    cpu_iffty(real_image, imag_image, size_x, size_y, fft_real, fft_imag);
    
    // End timing
    gettimeofday(&tv2,&tz2);
    
    // Compute the time difference in micro-seconds
    float execution = ((tv2.tv_sec-tv1.tv_sec)*1000000+(tv2.tv_usec-tv1.tv_usec));
    // Convert to milli-seconds
    execution /= 1000;
    // Print some output
    printf("OPTIMIZED IMPLEMENTATION STATISTICS:\n");
    printf("  Optimized Kernel Execution Time: %f ms\n\n", execution);
    return execution;
}
