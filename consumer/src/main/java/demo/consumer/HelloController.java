package demo.consumer;

import demo.api.Api;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author passer
 * @date 2021/5/15
 */
@RestController
public class HelloController {

    @DubboReference(version = "1.0")
    private Api api;
    private int taskCount = 100;
    private ExecutorService executor = Executors.newFixedThreadPool(taskCount);

    @GetMapping("/hello")
    public String hello() {
        return "hello world";
    }

    @GetMapping("/dubbo")
    public void dubbo() throws Exception {
        test();
    }

    @GetMapping("/local")
    public void local() throws Exception {
        testNative();
    }


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


    public void test() throws Exception {
        System.out.println("hello");
        testFunc(api);
    }

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
