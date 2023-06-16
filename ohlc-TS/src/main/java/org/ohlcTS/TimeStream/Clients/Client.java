package org.ohlcTS.TimeStream.Clients;

import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient;

public interface Client {

    TimestreamWriteClient buildWriteClient();
    TimestreamQueryClient buildQueryClient();

}
