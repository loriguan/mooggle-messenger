# mooggle-messenger

First start Server:
cd mooggle-messenger
java -Dfile.encoding=UTF-8 -classpath bin:lib/netty-all-4.1.17.Final.jar com.mooggle.messenger.benchmarkserver.WebSocketServer


Second start Client to test multiple connection:
cd mooggle-messenger
java -Dfile.encoding=UTF-8 -classpath bin:lib/netty-all-4.1.17.Final.jar com.mooggle.messenger.client.MultipleWebSocketClient
