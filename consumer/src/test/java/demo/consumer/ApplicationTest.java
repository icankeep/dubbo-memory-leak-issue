package demo.consumer;

import demo.api.Api;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(classes = Application.class)
@RunWith(SpringRunner.class)
public class ApplicationTest {

    @DubboReference(version = "1.0", url = "dubbo://127.0.0.1:12345")
    private Api api;

    private Api apiNative = new Api() {
        @Override
        public long[] transfer(long[] data) {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return data;
        }
    };

    private int taskCount = 100;

    private ExecutorService executor = Executors.newFixedThreadPool(taskCount);

    @Test
    public void test() throws Exception {
        testFunc(api);
    }


    @Test
    public void testNative() throws Exception {
        testFunc(apiNative);
    }

    public void testFunc(Api api) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        AtomicInteger oomCounter = new AtomicInteger();
        for (int i = 0; i < taskCount / 10; i++) {
            for (int k = 0; k < 10; k++) {
                CompletableFuture.runAsync(() -> {
                    try {
                        System.out.println(Thread.currentThread().getName());
                        Random random = new Random();
                        long[] data = new long[512 * 1024];
                        for (int j = 0; j < data.length; j++) {
                            data[j] = random.nextLong();
                        }
                        long[] transfer = api.transfer(data);
                    } catch (OutOfMemoryError e) {
                        oomCounter.incrementAndGet();
                    } finally {
                        countDownLatch.countDown();
                    }
                }, executor);
            }
            TimeUnit.SECONDS.sleep(2);
            System.gc();
        }
        countDownLatch.await();
        System.out.println(oomCounter);
    }
}