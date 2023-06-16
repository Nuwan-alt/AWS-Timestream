package com.timestream.TSReader.Service;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;


public class TSClients {

    public TimestreamQueryClient buildClient(){
            return  TimestreamQueryClient.builder().region(Region.US_EAST_1).build();
        }

}
