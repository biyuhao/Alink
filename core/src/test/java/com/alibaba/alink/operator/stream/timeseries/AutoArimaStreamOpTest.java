package com.alibaba.alink.operator.stream.timeseries;

import org.apache.flink.types.Row;

import com.alibaba.alink.operator.stream.StreamOperator;
import com.alibaba.alink.operator.stream.feature.OverCountWindowStreamOp;
import com.alibaba.alink.operator.stream.source.MemSourceStreamOp;
import com.alibaba.alink.testutil.AlinkTestBase;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

public class AutoArimaStreamOpTest extends AlinkTestBase {

	@Test
	public void test() throws Exception {
		List <Row> mTableData = Arrays.asList(
			Row.of(1, new Timestamp(1), 10.0),
			Row.of(1, new Timestamp(2), 11.0),
			Row.of(1, new Timestamp(3), 12.0),
			Row.of(1, new Timestamp(4), 13.0),
			Row.of(1, new Timestamp(5), 14.0),
			Row.of(1, new Timestamp(6), 15.0),
			Row.of(1, new Timestamp(7), 16.0),
			Row.of(1, new Timestamp(8), 17.0),
			Row.of(1, new Timestamp(9), 18.0),
			Row.of(1, new Timestamp(10), 19.0)
		);

		MemSourceStreamOp source = new MemSourceStreamOp(mTableData, new String[] {"id", "ts", "val"});

		source.link(
			new OverCountWindowStreamOp()
				.setPartitionCols("id")
				.setTimeCol("ts")
				.setPrecedingRows(5)
				.setClause("mtable_agg(ts, val) as data")
		).link(
			new AutoArimaStreamOp()
				.setValueCol("data")
				.setPredictionCol("predict")
				.setPredictNum(12)
		).link(
			new LookupValueInTimeSeriesStreamOp()
				.setTimeCol("ts")
				.setTimeSeriesCol("predict")
				.setOutputCol("out")
		).print();

		StreamOperator.execute();
	}
}