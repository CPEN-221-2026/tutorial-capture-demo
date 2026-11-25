package capture;

public class CaptureDemo {
    public static void main(String[] args) throws InterruptedException {

        // ---- Part 1: what the lambda captures, and how mutation shows up ----
        StringBuilder sb = new StringBuilder("main");   // mutable object (on the heap)
        int count = 1;                                  // primitive local
        String label = "A";                             // immutable object

        Thread t = new Thread(() -> {
            sb.append(" + thread");                     // mutates the SAME object main holds
            System.out.println("thread sees: " + label + " " + count + " " + sb);
            // count++;                                 // compile error: captured locals must be effectively final
            // sb = new StringBuilder();                // compile error: reassigning the variable is not allowed
        });
        // count++;                                     // compile error: captured locals must be effectively final
        // sb = new StringBuilder();                    // compile error: reassigning the variable is not allowed
        t.start();
        t.join();                                       // wait for the thread; join() also makes its writes visible
        System.out.println("main sees:   " + sb);       // main + thread
        System.out.println("----------");


        // ---- Part 2: a named class CAN reassign what it holds (a lambda can't) ----
        StringBuilder original = new StringBuilder("old");
        Task task = new Task(original, 1);              // the fields are copies of these values
        task.setCount(2);                               // reassigning a field before start(): allowed
        Thread t2 = new Thread(task);
        t2.start();                                     // start() makes everything above visible to the thread
        // task.setCount(99);                           // compiles, but after start() the thread may or may not see it
        t2.join();
        System.out.println("main sees:   " + task.getSb() + " " + task.getCount());   // replaced 3
        System.out.println("original is: " + original); // old + task: appending changed the shared object,
                                                        // but reassigning the field did not touch this variable
        System.out.println("----------");


//         ---- Part 3: two threads mutating one shared StringBuilder ----
        StringBuilder shared = new StringBuilder();
        Runnable work = () -> {
            for (int i = 0; i < 1_000_000; i++) {
                shared.append('x');                     // StringBuilder is NOT thread-safe
            }
        };
        Thread a = new Thread(work);
        Thread b = new Thread(work);
        a.start();
        b.start();
        a.join();
        b.join();
        System.out.println("expected 2000000, got " + shared.length());
    }

    /** Same job as a lambda, written as a named class with NON-final fields. */
    static class Task implements Runnable {
        private StringBuilder sb;                       // non-final: can be reassigned
        private int count;                              // non-final: can be changed

        Task(StringBuilder sb, int count) {
            this.sb = sb;                               // copies the reference (same object as main's)
            this.count = count;                         // copies the value
        }

        void setCount(int count) { this.count = count; }
        StringBuilder getSb() { return sb; }
        int getCount() { return count; }

        @Override
        public void run() {
            System.out.println("thread sees: " + sb + " " + count);   // old 2
            sb.append(" + task");                       // mutates the object main also holds
            count++;                                    // allowed: a field, not a captured local
            sb = new StringBuilder("replaced");         // allowed: now the field points elsewhere
        }
    }
}
