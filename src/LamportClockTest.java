import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LamportClockTest {

    private LamportClock lamportClock;

    @BeforeEach
    public void setup() {
        lamportClock = new LamportClock();
    }

    @Test
    public void testInitialTime() {
        assertEquals(0, lamportClock.getTime());
    }

    @Test
    public void testTick() {
        lamportClock.tick();
        assertEquals(1, lamportClock.getTime());
    }

    @Test
    public void testSend() {
        int timeSent = lamportClock.send();
        assertEquals(1, timeSent);
        assertEquals(1, lamportClock.getTime());
    }

    @Test
    public void testReceive_greaterTimestamp() {
        lamportClock.receive(10);
        assertEquals(11, lamportClock.getTime());
    }

    @Test
    public void testReceive_lowerTimestamp() {
        lamportClock.tick();
        lamportClock.receive(0);
        assertEquals(2, lamportClock.getTime());
    }
}
