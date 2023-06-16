package org.ohlcTS;

import com.mubasher.vas.Settings;
import org.ohlcTS.TimeStream.Clients.ClientImpl;
import org.ohlcTS.TimeStream.ServiceManager;
import org.ohlcTS.utils.Setting;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main {

    public static void main(String[] args) throws IOException {

        ExecutorService pool = Executors.newFixedThreadPool(Integer.parseInt((new Setting()).getSettings().get("MAX_THREADS")));
        KafkaManager kafkaManager = new KafkaManager(pool);
    }
}