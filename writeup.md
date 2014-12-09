Writeup

Go is a two player board game where both players take turns placing stones of their color on the intersections a grid. A group of stones is a set of stones that is four-connected (not diagonals). A liberty is an empty intersection adjacent to a group. If a group has no liberties then its stones are removed from the board and awarded to the opposing player. The winner of the game is the player with the most territory at the end, that is, the most empty intersections surrounded by only stones of their color. 

My app lets two people play Go with each other over the internet with Tromp-Taylor scoring and custom settings for the board size and komi value. The way the networking works is that one phone acts as the server and the other as the client as opposed to communicating with a separate server. All networking is implemented in C using TCP sockets with a mix of synchronization. For example threading to handle multiple clients is written in NDK C code using pthreads and condition variables+mutexes are also used to wait for user input, in particular when initializing and playing the game. All UI and graphics are done with the Android API which communicates with the backend NDK code. Overall through this project I learned a lot more about the Android API (especially the subtle details regarding UI threading and invalidate() vs postInvalidate()) and gained valuable experience with networking, synchronization, and of course the Android NDK.