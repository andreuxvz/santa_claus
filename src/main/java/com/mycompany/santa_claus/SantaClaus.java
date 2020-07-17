package com.mycompany.santa_claus;


import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.System.out;


public class SantaClaus {

    private volatile boolean kidsStillBelieveInSanta = true;
    private final Semaphore disbelief = new Semaphore(0);
    private final static int END_OF_FAITH = 2012;
    private AtomicInteger year = new AtomicInteger(2006);
    private static Random generator = new Random();

    // problem dimensions
    private final static int NUMBER_OF_REINDEER = 9;
    private final static int NUMBER_OF_ELVES = 10;
    private final static int ELVES_NEEDED_TO_WAKE_SANTA = 3;

    // synchronisation variables
    private final Semaphore queueElves;
    private final CyclicBarrier threeElves;
    private final CyclicBarrier elvesAreInspired;
    private final CyclicBarrier allReindeers;
    private final CyclicBarrier sleigh;
    private final Semaphore santasAttention;
    private final static int LAST_REINDEER = 0;    // compares to CyclicBarrier.await()
    private final static int THIRD_ELF = 0;        // compares to CyclicBarrier.await()

    class Reindeer implements Runnable {
        int id;

        Reindeer(int id) { this.id = id; }

        public void run() {
            while (kidsStillBelieveInSanta) {
                try {
                    // wait until christmas comes
                    Thread.sleep(900 + generator.nextInt(200));

                    // only all reindeers together can wake Santa
                    int reindeer = allReindeers.await();
                    // the last reindeer to return to North Pole must wake Santa
                    if (reindeer == LAST_REINDEER) {
                        santasAttention.acquire();
                        out.println("=== Delivery for Christmas " + year + " ===");
                        if (year.incrementAndGet() == END_OF_FAITH)
                        {
                            kidsStillBelieveInSanta = false;
                            disbelief.release();
                        }
                    }

                    // to deliver toys, the reindeer must be harnessed to the sleigh
                    sleigh.await();
                    Thread.sleep(generator.nextInt(20));   // delivering is almost immediate

                    // unharnessing can use the same barrier as harnessing,
                    // because the barrier is cyclic
                    reindeer = sleigh.await();
                    if (reindeer == LAST_REINDEER) {
                        santasAttention.release();
                        out.println("=== Toys are delivered ===");
                    }
                } catch (InterruptedException e) {
                    // thread interrupted for program cleanup
                } catch (BrokenBarrierException e) {
                    // another thread in the barrier was interrupted
                }
            }
            out.println("Reindeer " + id + " retires");
        }
    }

    class Elf implements Runnable {
        int id;

        Elf(int id) { this.id = id; }

        public void run() {
            try {
                Thread.sleep(generator.nextInt(2000));

                while (kidsStillBelieveInSanta) {
                    // no more than three elves fit into Santa's office
                    queueElves.acquire();
                    out.println("Elf " + id + " ran out of ideas");

                    // wait until three elves have a problem
                    int elf = threeElves.await();

                    // the third elf acts for all three
                    if (elf == THIRD_ELF)
                        santasAttention.acquire();

                    // wait until all elves have new ideas
                    Thread.sleep(generator.nextInt(500));
                    out.println("Elf " + id + " got inspiration");
                    elvesAreInspired.await();

                    if (elf == THIRD_ELF)
                        santasAttention.release();

                    // other elves that ran out of ideas in the meantime
                    // may now gather and wake santa again
                    queueElves.release();

                    // manufacture toys until the inspiration is used up
                    Thread.sleep(generator.nextInt(2000));
                }
            } catch (InterruptedException e) {
                // thread interrupted for program cleanup
            } catch (BrokenBarrierException e) {
                // another thread in the barrier was interrupted
            }
            out.println("Elf " + id + " retires");
        }
    }

    class BarrierMessage implements Runnable {
        String msg;
        BarrierMessage(String msg) { this.msg = msg; }
        public void run() {
            out.println(msg);
        }
    }

    class Harnessing implements Runnable {
        boolean isSleighAttached;
        Harnessing() { isSleighAttached = false; }
        public void run() {
            isSleighAttached = !isSleighAttached;
            if (isSleighAttached)
                out.println("=== All reindeer harnessed ===");
            else
                out.println("=== All reindeer are back in the stable ===");
        }
    }

    public SantaClaus() {
        // use a fair semaphore for Santa to prevent that a second group
        // of elves might get Santas attention first if the reindeer are
        // waiting and Santa is consulting with a first group of elves.
        santasAttention = new Semaphore(1, true);
        queueElves = new Semaphore(ELVES_NEEDED_TO_WAKE_SANTA, true);    // use a fair semaphore
        threeElves = new CyclicBarrier(ELVES_NEEDED_TO_WAKE_SANTA,
                new BarrierMessage("--- " + ELVES_NEEDED_TO_WAKE_SANTA + " elves are knocking ---"));
        elvesAreInspired = new CyclicBarrier(ELVES_NEEDED_TO_WAKE_SANTA,
                new BarrierMessage("--- Elves return to work ---"));
        allReindeers = new CyclicBarrier(NUMBER_OF_REINDEER, new Runnable() {
            public void run() {
                out.println("=== Reindeer reunion for Christmas " + year +" ===");
            }});
        sleigh = new CyclicBarrier(NUMBER_OF_REINDEER, new Harnessing());

        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < NUMBER_OF_ELVES; ++i)
            threads.add(new Thread(new Elf(i)));
        for (int i = 0; i < NUMBER_OF_REINDEER; ++i)
            threads.add(new Thread(new Reindeer(i)));
        out.println("Once upon in the year " + year + " :");
        for (Thread t : threads)
            t.start();

        try {
            // wait until !kidsStillBelieveInSanta
            disbelief.acquire();
            out.println("Faith has vanished from the world");
            for (Thread t : threads)
                t.interrupt();
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.println("The End of Santa Claus");
    }

    public static void main(String[] args) {
        new SantaClaus();
    }
}
