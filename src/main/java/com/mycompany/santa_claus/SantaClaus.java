package com.mycompany.santa_claus;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import static java.lang.System.out;


public class SantaClaus {

    private final Semaphore disbelief = new Semaphore(0);
    private final static int END_OF_FAITH = 2021;
    private AtomicInteger year = new AtomicInteger(2019);
    private static Random generator = new Random();
    private volatile boolean ninosConFeNavidad = true;

    private final static int NUM_RENOS = 9;
    private final static int NUM_ENANOS = 12;
    private final static int ENANOS_PARA_DESPERTAR = 3;

    private final Semaphore queueElves;
    private final CyclicBarrier threeElves;
    private final CyclicBarrier elvesAreInspired;
    private final CyclicBarrier allReindeers;
    private final CyclicBarrier sleigh;
    private final Semaphore santasAttention;
    private final static int ULTIMO_RENO = 0;
    private final static int TERCER_ELFO = 0;        

    class Reindeer implements Runnable {
        int id;

        Reindeer(int id) { this.id = id; }

        public void run() {
            while (ninosConFeNavidad) {
                try {
                   
                    Thread.sleep(900 + generator.nextInt(200));

                    // Mostrar mensaje cuando lleguen todos los renos
                    int reindeer = allReindeers.await();
                    // Validación cuando llega el último reno
                    if (reindeer == ULTIMO_RENO) {
                        santasAttention.acquire();
                        out.println("=== Listo para entregar los jueguetes por navidad " + year + " ===");
                        if (year.incrementAndGet() == END_OF_FAITH)
                        {
                            ninosConFeNavidad = false;
                            disbelief.release();
                        }
                    }

                    // el trineo espera que todos los renos esten amarrados
                    sleigh.await();
                    Thread.sleep(generator.nextInt(20));   // delivering is almost immediate

                    // Se liberan a los renos del trineo
                    // la clase barrier es ciclica
                    reindeer = sleigh.await();
                    if (reindeer == ULTIMO_RENO) {
                        santasAttention.release();
                        out.println("=== Todos los juguetes han sido entregados ===");
                    }
                } catch (InterruptedException e) {
                    // thread interrupted for program cleanup
                } catch (BrokenBarrierException e) {
                    // another thread in the barrier was interrupted
                }
            }
            out.println("Reno " + id + " se retira");
        }
    }

    class Elf implements Runnable {
        int id;

        Elf(int id) { this.id = id; }

        public void run() {
            try {
                Thread.sleep(generator.nextInt(2000));

                while (ninosConFeNavidad) {
                    // no more than three elves fit into Santa's office
                    queueElves.acquire();
                    out.println("Enano " + id + " con problema");

                    // Cola de atención de los 3 enanos
                    int elf = threeElves.await();

                    // Validación cuando llega el tercer enano
                    if (elf == TERCER_ELFO)
                        santasAttention.acquire();

                    // Esperamos que los enanos tengan nuevas ideas
                    Thread.sleep(generator.nextInt(500));
                    out.println("Enano " + id + " atendido");
                    elvesAreInspired.await();

                    if (elf == TERCER_ELFO)
                        santasAttention.release();

                    
                    // Liberamos la cola de los enanos
                    queueElves.release();

                   
                    Thread.sleep(generator.nextInt(2000));
                }
            } catch (InterruptedException e) {
                // thread interrupted for program cleanup
            } catch (BrokenBarrierException e) {
                // another thread in the barrier was interrupted
            }
            out.println("Enano " + id + " se retira");
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
                out.println("=== Todos los renos han sido amarrados al trineo ===");
            else
                out.println("=== Todos los renos estan en el establo ===");
        }
    }

    public SantaClaus() {
        santasAttention = new Semaphore(1, true);
        queueElves = new Semaphore(ENANOS_PARA_DESPERTAR, true);    // semaforo indicador
        threeElves = new CyclicBarrier(ENANOS_PARA_DESPERTAR,
                new BarrierMessage("--- " + ENANOS_PARA_DESPERTAR + " enanos necesitan ayuda ---"));
        elvesAreInspired = new CyclicBarrier(ENANOS_PARA_DESPERTAR,
                new BarrierMessage("--- Enanos regresan a trabajar ---"));
        allReindeers = new CyclicBarrier(NUM_RENOS, new Runnable() {
            public void run() {
                out.println("=== Todos los renos completos para navidad " + year +" ===");
            }});
        sleigh = new CyclicBarrier(NUM_RENOS, new Harnessing());

        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < NUM_ENANOS; ++i)
            threads.add(new Thread(new Elf(i)));
        for (int i = 0; i < NUM_RENOS; ++i)
            threads.add(new Thread(new Reindeer(i)));
        out.println("Una vez cada año " + year + " :");
        for (Thread t : threads)
            t.start();

        try {
            // Esperamos hasta que las personas dejen de creer en la navidad
            disbelief.acquire();
            out.println("La fe ha desaparecido del mundo");
            for (Thread t : threads)
                t.interrupt();
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.println("Fin de la navidad pra siempre");
    }

    public static void main(String[] args) {
        new SantaClaus();
    }
}
