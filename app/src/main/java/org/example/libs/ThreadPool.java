package org.example.libs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

class ThreadClose {
    public boolean threadClose = false;
}

public class ThreadPool {
    ConcurrentLinkedQueue<Runnable> channels = new ConcurrentLinkedQueue<>();
    List<Worker> workers = new ArrayList<Worker>();
    ThreadClose threadClose = new ThreadClose();
    int nWorkers;
    public ThreadPool(int nWorkers) {
        this.nWorkers = nWorkers;
        for(int i=0;i<nWorkers;i++) {
            workers.add(new Worker(i,channels, threadClose));
        }
    }

    public void execute(Runnable r) throws Exception {
        channels.add(r);
    }

    public void close() throws Exception {
        threadClose.threadClose = true;
        for(int i=0;i<nWorkers;i++) {
            System.out.println("Shutting down the worker " + i + "...");
            workers.get(i).thread.join();
        }
    }
}

class Worker {
    int id;
    ConcurrentLinkedQueue<Runnable> channels;
    Thread thread;
    public Worker(int id, ConcurrentLinkedQueue<Runnable> channels, ThreadClose threadClose) {
        this.channels = channels;
        this.thread = new Thread(() -> {
            try {
                while(!threadClose.threadClose) {
                    Runnable job = channels.poll();
                    if(job != null) {
                        System.out.println("Worker " 
                                + id 
                                + " got a job.executing...");
                        job.run();
                    }
                }
                System.out.println("worker " + id + " closed.");
            }catch(Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }
}
