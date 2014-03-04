Students: ckaymaz (Cagla Kaymaz), vkwong (Victoria Kwong)


Our algorithm runs through the entire query text in the configure method of the mapper. Here we create a HashSet of all the n-grams that will be used throughout the rest of the job. Since we only go through the query text that takes O(q) time. Then in the actual mapping method, we run through each page and create all the n-grams in that page and compare them to the n-grams from the query text in the HashSet. This mapping is done in parallel through P processors. Since we only go through each page once to create the n-grams and the total size of the input pages n this takes O(n/P) time. The mapper then outputs a pair value with ONE as the key, and the title of the page concatenated with the score as the value. This whole process gives us O(q + n/P)
In the reduce, we go through each concatenated string of title and score once using an iterator. Even though Hadoop’s API is an iterator, internally it reduces in logarithmic time, giving us O(log(p)). 
Overall, our map reduce run time is O(q + n/P + log(P)) and we only do map reduce once. 


EXTRA CREDIT
For extra credit, in order to compute the 20 best matches and their scores, we put the match score pair we get from the map into a priority queue. To make sure this queue is at a fixed size, we remove the head of the queue (hence the page with the lowest score). 
We reverse this queue using another one to make sure the head of the queue now gives us the page with the best score and we write them to the output file in that order. Since the mapping requires the same amount of work as above, the map phase is done in O(q + n/P) time. However, since now the reduce phase needs to account for k = 20 top scores, the reduction now takes O(log k * log P). Since k is a constant, the reduction only requires O(log P) time. As such the total algorithm runs in O(q + n/P + log P).