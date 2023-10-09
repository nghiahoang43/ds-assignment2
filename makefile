# Variables
JAVA = java
JAVAC = javac
CP_PATH = -cp lib/gson-2.10.1.jar:src/
CPTEST = -cp lib/*:src/
MAIN_SOURCES = src/GETClient.java \
					src/AggregationServer.java \
          src/JSONHandler.java \
          src/LamportClock.java \
          src/ContentServer.java \
          src/NetworkHandler.java \
          src/SocketNetworkHandler.java \

TEST_SOURCES = src/AggregationServerTest.java \
					src/ContentServerTest.java \
					src/GETClientTest.java \
					src/JSONHandlerTest.java \
					src/LamportClockTest.java \
					src/IntegrationTest.java \


TEST_MAIN_CLASS = org.junit.platform.console.ConsoleLauncher

PORT = 4567

all:
	$(JAVAC) $(CP_PATH) $(MAIN_SOURCES)

compile-main:	
	$(JAVAC) $(CP_PATH) $(MAIN_SOURCES)

aggregation: all
	$(JAVA) $(CP_PATH) AggregationServer ${PORT}

content: all
	$(JAVA) $(CP_PATH) ContentServer localhost ${PORT} src/input.txt

client: all
	$(JAVA) $(CP_PATH) GETClient localhost:${PORT} IDS60901

compile-test: compile-main
	@$(JAVAC) $(CPTEST) $(MAIN_SOURCES) $(TEST_SOURCES)

test: compile-test
	@$(JAVA) $(CPTEST) $(TEST_MAIN_CLASS) --scan-classpath

clean:
	find . -name "*.class" -exec rm {} +

.PHONY: all clean