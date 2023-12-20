
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataMover {
    public static int[] data;
    public static List<Thread> movers;

    private static int arrayMovingTime;
    private static int arraySize;

    DataMover(int time, int size, int[] threadNumbers) {
        arrayMovingTime = time;
        arraySize = size;
        //megfelelő méretű tömbök létrehozása
        data = new int[arraySize];
        movers = new ArrayList<Thread>(arraySize);

        for (int i = 0; i < arraySize; ++i) {
            data[i] = i*1000;
            movers.add(new MovingThread(i, threadNumbers[i]));
            movers.get(i).start();
        }
    }

    /* tömbelem mozgatási idejének szimulálása 
     * (közös várakozási idő)
    */
    private static void simulateMovingArray() {
        try {
            Thread.sleep(arrayMovingTime);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class MovingThread extends Thread {
        private int id;
        private int waitingTime;

        MovingThread(int id, int waitingTime) {
            this.id = id;
            this.waitingTime = waitingTime;
        }

        /*A szál a hozzá rendelt ideig várakozik */
        private void waitCertainTime() {
            try {
                Thread.sleep(this.waitingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < 10; ++i) {
                //sorszámának megfelelő ideig várakozik
                waitCertainTime();
                //kritikus szakasz
                //lehetne synchronized (DataMover.class)
                synchronized (data) { //azért kell, mert itt actually egy tömbelemhez több szál is hozzáfér!
                    data[this.id] -= this.id;
                    System.out.println("#" + this.id + ": data " + this.id + " == " + data[this.id]);
                    simulateMovingArray();
                    int next;
                    if ((this.id+1) == arraySize) {
                        data[0] += this.id;
                        next = 0;
                    }
                    else {
                        data[(this.id+1)] += this.id;
                        next = this.id+1;
                    }
                    System.out.println("#" + this.id + ": data " + next + " -> " + data[next]);
                }
            }
        }
    }

    
    public static void main(String[] args) {
        int arrayMovingTime;
        int arraySize;
        int[] threadNumbers;
        //meg lett adva mindenből
        if (args.length > 1) {
            arrayMovingTime = Integer.parseInt(args[0]);
            arraySize = (args.length-1);
            threadNumbers = new int[arraySize];
            for (int i = 0; i < arraySize; ++i) {
                threadNumbers[i] = Integer.parseInt(args[i+1]);
            }
        }
        else {
            if (args.length == 1) { //csak a közös várakozási idő lett megadva
                arrayMovingTime = Integer.parseInt(args[0]);
            }
            else { //semmi nem lett megadva
                arrayMovingTime = 123; //123ms
            }
            arraySize = 3;
            threadNumbers = new int[] {111, 256, 404};
        }

        new DataMover(arrayMovingTime, arraySize, threadNumbers);

        for (Thread mover : movers) {
            try {
                mover.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("Data moving over.");
        System.out.println(Arrays.toString(data));
    }

}