public class LamportClock {
  private volatile int time; // Mark as volatile for visibility in multi-threaded context

  public LamportClock() {
    this.time = 0;
  }

  // Tick the clock due to an internal event
  public synchronized void tick() { // Synchronize to avoid race conditions
    time++;
  }

  // Simulates sending a message by returning the current time
  public synchronized int send() { // Synchronize to avoid race conditions
    tick();
    return time;
  }

  // Simulates receiving a message
  public synchronized void receive(int receivedTimestamp) { // Synchronize to avoid race conditions
    time = Math.max(time, receivedTimestamp) + 1;
  }

  public int getTime() {
    return time;
  }

  @Override
  public String toString() {
    return "LamportClock [time=" + time + "]";
  }
}
