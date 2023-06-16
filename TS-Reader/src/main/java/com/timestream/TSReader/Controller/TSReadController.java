package com.timestream.TSReader.Controller;

//import com.timestream.TSReader.Service.TSReadService;

import com.timestream.TSReader.Model.RequestModel;
import com.timestream.TSReader.Service.TSClients;
import com.timestream.TSReader.Service.TSReadService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamquery.endpoints.internal.Value;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ts")
public class TSReadController {
    TSReadService tsReadService = new TSReadService();
    public static final Logger hitQuiresLogger = LoggerFactory.getLogger("hitQuires");
    TimestreamQueryClient timestreamQueryClient = new TSClients().buildClient();

    UUID id;

    @PostConstruct
    public void startCounter(){
        this.id = UUID.randomUUID();
    }

    @GetMapping("/ohlc")
    private List<String> fetchOHLC(@RequestParam("query") String query){
        hitQuiresLogger.trace(id +" -> "+ query);
        System.out.println(query);
        return tsReadService.runQuery(timestreamQueryClient , query);
    }

    @GetMapping("/test")
    private String test(){
        return "Tested";
    }
}

//SELECT * FROM feedOHLC.OHLC WHERE measure_name = 'ohlc' AND SOURCE_ID = 'TDWL' AND time > ago(1h) ORDER BY time DESC