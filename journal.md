10/26 - 11/1
===
I installed eclipse and NDK but haven't looked at any of the documentation yet. Luckily I've done a little Android development before so it shouldn't be too hard to pick up again.

11/2 - 11/8
===
Read through the NDK tutorial on the android website and loaded up some of the examples in Eclipse. I browsed through the examples and tried to make sense of them but it looked pretty complicated. Hello-jni seems simple but the ones using OpenGL are beyond my comprehension. I had trouble running the OpenGL examples as well and I eventually figured out that the Android emulator doesn't support OpenGL ES 2.0 and 3.0. This is pretty unfortunate. My app is a board game and I was thinking of rendering the board and pieces using OpenGL. I'll look at the benefits of this as well as other alternatives next week.

11/9 - 11/15
===
I couldn't make it to the Hackathon due to coursework and midterms. Did some more reading on NDK through some books I found online and various tutorials and after reading them I think for the UI I was thinking of writing everything in C using NativeActivity and NativeWindow but since it ends up calling the JVM anyways I might as well go with the standard Android API. The Android API for graphics is also far easier to use than OpenGL and other alternatives. I only need to draw circles, lines, and images anyways so anything else would probably be overkill and any performance gains would not be worth the increased development time. I'll plan out the app next week.

11/16 - 11/22
===
I planned out the basic interface of the app which consists of two ListViews that open up forms asking for information required to host or join a game. Afterwards there's a small handshake where the client connects to the host and views the parameters of the game before accepting/declining. The host then also has the option of accepting/declining and afterwards the app loads a View of the game which will just be a board with some basic buttons for passing and resigning.

11/23 - 11/29
===
It was Thanksgiving break so I took it a bit easy and just implemented the interface I came up with last week. I didn't finish the controls for the board which is taking longer than I expected. It took me a while to figure out how to handle touch events and I'm still finishing up the cross cursor so hopefully I'll finish that soon.

11/29 - 12/6
===
Finished up the interface and worked really hard on the backend. I implemented the "handshake" between server and client that I wrote about a couple weeks ago. I finally went in depth with NDK and I was surprised by how complicated it is. The names for everything were gigantic and calling a java method from C was actually actually quite confusing. Figuring out how type signatures for a java method took quite a while since the documentation is nearly nonexistent. I also didn't realize that separate threads need to cache the JVM pointer. However, I am glad that the NDK code continues to run in the background unlike Android Java code which can be reset in so many different ways and requires manually writing code to save the state. Anyways I managed to use a lot of material I learned in CS241. I used a good deal of synchronization to handle various clients and wait for user input. Unfortunately I'm running out of time and I still have to implement the logic of Go and additional networking and synchronization. Fortunately I decided not to reinvent the wheel and looked for libraries implementing standard Go routines. Most of them were pretty complicated but I found a short open source library on github called AndroidGo. It's not longer maitained and doesn't have methods for scoring the game but I should be able to handle it.

12/7 - 12/8
===
Managed to finish everything. Wrote the scoring methods I mentioned last week and finished up the game logic. Sadly I'm out of time because there's still a lot I can improve on. In terms of more features there are some pretty low hanging ones that wouldn't be too hard to implement given the networking code I already have set up, such as the support for spectators. Doubles games also shouldn't be too hard. The biggest issue however is that I haven't done a lot of error handling and so there are probably security issues and I didn't rigorously free all resources I allocated. Still, I've learned a lot from this project including basic Android SDK and NDK development and solidified my understanding of CS241 concepts. 
