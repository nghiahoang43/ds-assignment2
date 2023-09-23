# ds-assignment2
# How to run the program
1. Open a terminal and go to the directory of the makefile
2. Run the command "make aggregation" to start the aggregation server
3. Open another terminal and go to the directory of the makefile
4. Run the command "make content" to start the content server
5. Open another terminal and go to the directory of the makefile
6. Run the command "make client" to start the client
7. The client will send a request to the content server and the content server will send the response to the aggregation server
8. The aggregation server will send the response to the client
9. The client will print the response