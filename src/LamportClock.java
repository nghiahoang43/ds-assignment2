public class LamportClock {
  private volatile int time; // Mark as volatile for visibility in multi-threaded context

  public LamportClock() {
    this.time = 0;
  }

  public synchronized int send() {
    tick();
    return time;
  }

  public int getTime() {
    return time;
  }

  public synchronized void tick() {
    time++;
  }

  public synchronized void receive(int receivedTimestamp) {
    time = Math.max(time, receivedTimestamp) + 1;
  }

  @Override
  public String toString() {
    return "LamportClock [time=" + time + "]";
  }
}
