Students: ckaymaz (Cagla Kaymaz), vkwong (Victoria Kwong)

Our implementation is fairly simple. In the ChatState, all we did was synchronize the blocks where we are reading the recent messages and writing a new message. We synchronized on history to prevent that data structure from altering in the middle of read and writes. Additionally, we altered the sleep() call to a wait() call to be able to return messages as quickly as they were added.

In the ChatServer class, we first created 8 new threads. These threads would simply wait until there was a request present in which case they would be notified and the thread would grab the request off the queue. Since there is a possibility that more requests are made than there are threads and to not lose any of the requests, we created a queue to store all the connection requests. We are synchronizing on the queue, to make sure nothing changes when we are reading or writing to it. Additionally, we are synchronizing on stateByName to prevent errors in the case where we have multiple chat rooms or non-yet existing chat room is being opened in two or more tabs. We believe this will work because we prevent anything can be mutable from changing when we are accessing it.  

EXTRA CREDIT
We have also implemented the level two extra credit portion of the assignment. We have created a new class named Work that can represent a Socket connection or a String message or ChatState state.  message/state pair represents the messages that need to be posted to other existing rooms after the message has been posted to the room "all". The "isConnection" boolean of the class represents whether the instance of the class is a Socket connection or message/state pair. This way we can queue up the requests and the messages to be posted in the same queue to be processed by the existing pool of threads. 

When we are handling a push request to room "all", after posting to "all", we create an instance of the Work class and add it to the queue to be processed by the pool of threads.  We notify the queue so the waiting threads can act on it. 

When we are handling a push request to any room that is not "all", we check if the room "all" exists, and if so we post to it, otherwise we don't do anything. 

This is a level 2 extra credit because we have reduced lock contention. We still have a single lock to protect the lookups on ChatState objects, however when we post to all, the posts to other rooms can be done in parallel by different threads since we add it to the queue to be handled by the thread pool. 