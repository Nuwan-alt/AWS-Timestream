package com.timestream.TSReader.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.timestreamquery.TimestreamQueryClient;
import software.amazon.awssdk.services.timestreamquery.model.*;
import software.amazon.awssdk.services.timestreamquery.paginators.QueryIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TSReadService {
    long ONE_GB_IN_BYTES = 1048576L;
    public static final Logger progressEvalLogger = LoggerFactory.getLogger("progressEval");
    public static final Logger resultsLogger = LoggerFactory.getLogger("results");
    public static final Logger queriesLogger = LoggerFactory.getLogger("queries");

    public List<String> runQuery(TimestreamQueryClient timestreamQueryClient, String queryString) {
        List<String> results = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        queriesLogger.info("ID:-  " +uuid + queryString);
        progressEvalLogger.info("ID:-  " +uuid + " ==== Data fetching started ====");

        try {
            QueryRequest queryRequest = QueryRequest.builder().queryString(queryString).build();
            final QueryIterable queryResponseIterator = timestreamQueryClient.queryPaginator(queryRequest);
            for (QueryResponse queryResponse : queryResponseIterator) {
                parseQueryResult(queryResponse, String.valueOf(uuid),results);
                progressEvalLogger.info("ID:-  " +uuid + " ==== Data fetching finished ====");
                return results;
            }
        } catch (Exception e) {
            // Some queries might fail with 500 if the result of a sequence function has more than 10000 entries

            progressEvalLogger.error("ID:-  " + uuid,e);
            progressEvalLogger.error("ID:-  " +uuid + " *** Data fetching failed ***");

        }
        return results;
    }
    private void parseQueryResult(QueryResponse response, String uuid,List<String> results) {

        final QueryStatus queryStatus = response.queryStatus();
        progressEvalLogger.info("Query progress so far: " + queryStatus.progressPercentage() + "%");

        double bytesScannedSoFar = (double) queryStatus.cumulativeBytesScanned() / ONE_GB_IN_BYTES;
        progressEvalLogger.info("Data scanned so far: " + bytesScannedSoFar + " MB");

        double bytesMeteredSoFar = (double) queryStatus.cumulativeBytesMetered() / ONE_GB_IN_BYTES;
        progressEvalLogger.info("Data metered so far: " + bytesMeteredSoFar + " MB");

        List<ColumnInfo> columnInfo = response.columnInfo();
        List<Row> rows = response.rows();

        resultsLogger.info("Metadata: " + columnInfo);
        resultsLogger.info("Data: ");

        // ======================================== Display the output ===============================================
        for (Row row : rows) {
            resultsLogger.info("ID:-  "+uuid + "  "+parseRow(columnInfo, row));
            results.add(parseRow(columnInfo, row));
        }
    }

    private String parseRow(List<ColumnInfo> columnInfo, Row row) {
        List<Datum> data = row.data();
        List<String> rowOutput = new ArrayList<>();
        // iterate every column per row
        for (int j = 0; j < data.size(); j++) {
            ColumnInfo info = columnInfo.get(j);
            Datum datum = data.get(j);
            rowOutput.add(parseDatum(info, datum));
        }
        return String.format("{%s}", rowOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    private String parseDatum(ColumnInfo info, Datum datum) {
        if (datum.nullValue() != null && datum.nullValue()) {
            return info.name() + "=" + "NULL";
        }
        Type columnType = info.type();
        // If the column is of TimeSeries Type
        if (columnType.timeSeriesMeasureValueColumnInfo() != null) {
            return parseTimeSeries(info, datum);
        }
        // If the column is of Array Type
        else if (columnType.arrayColumnInfo() != null) {
            List<Datum> arrayValues = datum.arrayValue();
            return info.name() + "=" + parseArray(info.type().arrayColumnInfo(), arrayValues);
        }
        // If the column is of Row Type
        else if (columnType.rowColumnInfo() != null && columnType.rowColumnInfo().size() > 0) {
            List<ColumnInfo> rowColumnInfo = info.type().rowColumnInfo();
            Row rowValues = datum.rowValue();
            return parseRow(rowColumnInfo, rowValues);
        }
        // If the column is of Scalar Type
        else {
            return parseScalarType(info, datum);
        }
    }

    private String parseScalarType(ColumnInfo info, Datum datum) {
        return parseColumnName(info) + datum.scalarValue();
    }
    private String parseColumnName(ColumnInfo info) {
        return info.name() == null ? "" : info.name() + "=";
    }

    private String parseArray(ColumnInfo arrayColumnInfo, List<Datum> arrayValues) {
        List<String> arrayOutput = new ArrayList<>();
        for (Datum datum : arrayValues) {
            arrayOutput.add(parseDatum(arrayColumnInfo, datum));
        }
        return String.format("[%s]", arrayOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }

    private String parseTimeSeries(ColumnInfo info, Datum datum) {
        List<String> timeSeriesOutput = new ArrayList<>();
        for (TimeSeriesDataPoint dataPoint : datum.timeSeriesValue()) {
            timeSeriesOutput.add("{time=" + dataPoint.time() + ", value=" +
                    parseDatum(info.type().timeSeriesMeasureValueColumnInfo(), dataPoint.value()) + "}");
        }
        return String.format("[%s]", timeSeriesOutput.stream().map(Object::toString).collect(Collectors.joining(",")));
    }
}
