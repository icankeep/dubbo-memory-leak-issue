package demo.provider.api;

import demo.api.Api;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

@DubboService(version = "1.0")
public class ApiImpl implements Api {
    public long[] transfer(long[] array) {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return array;
    }
}
