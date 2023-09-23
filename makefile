# Variables
JAVA = java
JAVAC = javac
CP_PATH = -cp lib/json-20230227.jar:src/
SOURCES = src/GETClient.java \
					src/AggregationServer.java \
          src/JSONHandler.java \
          src/LamportClock.java \
          src/ContentServer.java \
          src/NetworkHandler.java \
          src/SocketNetworkHandler.java \

PORT = 2000

all:
	$(JAVAC) $(CP_PATH) $(SOURCES)

aggregation: all
	$(JAVA) $(CP_PATH) AggregationServer ${PORT}

content: all
	$(JAVA) $(CP_PATH) ContentServer localhost ${PORT} src/input.txt

client: all
	$(JAVA) $(CP_PATH) GETClient localhost:${PORT} IDS60901

clean:
	find . -name "*.class" -exec rm {} +

.PHONY: all clean