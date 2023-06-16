package org.ohlcTS.models;

import com.mubasher.regional.data.OHLC;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OHLCCandle extends OHLC {
    public long minute;
    public long interval;
}
