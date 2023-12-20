import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

public class DataMover2 {
    public static AtomicInteger arrivalCount = new AtomicInteger(0);
    public static AtomicInteger totalSent = new AtomicInteger(0);
    public static AtomicInteger totalArrived = new AtomicInteger(0);

    public static ExecutorService pool;
    public static List<ArrayBlockingQueue<Integer>> queues = new ArrayList<ArrayBlockingQueue<Integer>>();
    public static List<Future<DataMover2Result>> moverResults = new ArrayList<Future<DataMover2Result>>();
    public static List<Integer> discards = new ArrayList<Integer>();

    private static int n;
    private static int[] threadSleepNumbers;
    private static int totalArrivalCount;

    public DataMover2(int size, int[] threadNums) {
        n = size;
        totalArrivalCount = 5*n;
        threadSleepNumbers = new int[n];
        for (int i = 0; i < n; ++i) {
            threadSleepNumbers[i] = threadNums[i];
            queues.add(new ArrayBlockingQueue<Integer>(100));
        }

        pool = Executors.newFixedThreadPool(100);       
    }

    private void waitForSomeTime(int index) {
        try {
            Thread.sleep(threadSleepNumbers[index]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void shutDown() throws InterruptedException {
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
    }

    private class DataMover2Result {
        public int count;
        public int data;
        public int forwarded;

        DataMover2Result() {
            this.count = 0;
            this.data = 0;
            this.forwarded = 0;
        }
    }

    public void moveData() {
        for (int i = 0; i < n; ++i) {
            int id = i;
            Future<DataMover2Result> future = pool.submit(() -> {
                DataMover2Result result = new DataMover2Result();
                try {
                    ArrayBlockingQueue<Integer> prevQueue = queues.get(id);
                    ArrayBlockingQueue<Integer> nextQueue;
                    if ((id + 1) == n) {
                        nextQueue = queues.get(0);
                    } else {
                        nextQueue = queues.get(id + 1);
                    }

                    while (arrivalCount.get() < totalArrivalCount) {
                        // I.)
                        Integer randomNumber = ThreadLocalRandom.current().nextInt(0, 10000);
                        System.out.println("total   " + arrivalCount + "/" + totalArrivalCount +  " | #" + id + " sends " + randomNumber);
                        nextQueue.put(randomNumber);
                        totalSent.addAndGet(randomNumber);

                        //II.)
                        int miliseconds = ThreadLocalRandom.current().nextInt(300, 1000);
                        Integer element = prevQueue.poll(miliseconds, TimeUnit.MILLISECONDS);
                        //synchronized (result) {
                            if (element == null) {
                                System.err.println("total   " + arrivalCount + "/" + totalArrivalCount +  " | #" + id + " got nothing...");
                            } else {
                                if (element%n == id) {
                                    System.out.println("total   " + arrivalCount + "/" + totalArrivalCount +  " | #" + id + " got " + element);
                                    arrivalCount.addAndGet(1);
                                    result.count += 1;
                                    result.data += element;
                                }
                                else {
                                    System.out.println("total   " + arrivalCount + "/" + totalArrivalCount +  " | #" + 
                                                        id + " forwards " + (element-1) + " [" + ((id+1) == n ? 0 : (id+1)) + "]");
                                    nextQueue.put(element-1);
                                    result.forwarded += 1;
                                }
                            }
                        //}
                        waitForSomeTime(id);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return result;
            });
            moverResults.add(future);
        }
    }

    private void summarize() throws InterruptedException, ExecutionException {
        int sumForwardedValue = 0;
        int sumArrivedValue = 0;
        
        for (Future<DataMover2Result> result : moverResults) {
            DataMover2Result resultGet = result.get();
            sumArrivedValue += resultGet.data;
            sumForwardedValue += resultGet.forwarded;
        }

        totalArrived.addAndGet(sumArrivedValue);
        totalArrived.addAndGet(sumForwardedValue);

        int totalDiscarded = 0;
        for (ArrayBlockingQueue<Integer> queue : queues) {
            for (Integer element : queue) {
                discards.add(element);
                totalDiscarded += element;
            }
        }
        System.out.println("discarded " + discards.toString() + " = " + totalDiscarded);

        if (totalSent.get() == (totalArrived.addAndGet(totalDiscarded))) {
            System.out.println("sent " + totalSent + " === got " + (totalArrived.get()) + " = " + 
                                (totalArrived.addAndGet(-totalDiscarded)) + " + discarded " + totalDiscarded);
        }
        else {
            System.out.println("WRONG sent " + totalSent + " !== got " + (totalArrived.get()) + " = " + 
                                (totalArrived.addAndGet(-totalDiscarded)) + " + discarded " + totalDiscarded);
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int arraySize;
        int[] threadNumbers;

        //meg lett adva mindenből
        if (args.length > 0) {
            arraySize = (args.length);
            threadNumbers = new int[arraySize];
            for (int i = 0; i < arraySize; ++i) {
                threadNumbers[i] = Integer.parseInt(args[i]);
            }
        }
        else { //nem lett argumentum megadva
            arraySize = 4;
            threadNumbers = new int[] {123, 111, 256, 404};
        }
        DataMover2 dm = new DataMover2(arraySize, threadNumbers);
        dm.moveData();
        dm.shutDown();
        dm.summarize();
    }
}
