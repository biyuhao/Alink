# ALS：相似items推荐 (AlsSimilarItemsRecommender)
Java 类名：com.alibaba.alink.pipeline.recommendation.AlsSimilarItemsRecommender

Python 类名：AlsSimilarItemsRecommender


## 功能介绍
使用ALS (Alternating Lease Square）model 对相似的item的进行实时推荐。

## 参数说明

| 名称 | 中文名称 | 描述 | 类型 | 是否必须？ | 默认值 |
| --- | --- | --- | --- | --- | --- |
| itemCol | Item列列名 | Item列列名 | String | ✓ |  |
| recommCol | 推荐结果列名 | 推荐结果列名 | String | ✓ |  |
| initRecommCol | 初始推荐列列名 | 初始推荐列列名 | String |  | null |
| k | 推荐TOP数量 | 推荐TOP数量 | Integer |  | 10 |
| reservedCols | 算法保留列名 | 算法保留列 | String[] |  | null |
| numThreads | 组件多线程线程个数 | 组件多线程线程个数 | Integer |  | 1 |
| modelStreamFilePath | 模型流的文件路径 | 模型流的文件路径 | String |  | null |
| modelStreamScanInterval | 扫描模型路径的时间间隔 | 描模型路径的时间间隔，单位秒 | Integer |  | 10 |
| modelStreamStartTime | 模型流的起始时间 | 模型流的起始时间。默认从当前时刻开始读。使用yyyy-mm-dd hh:mm:ss.fffffffff格式，详见Timestamp.valueOf(String s) | String |  | null |


## 代码示例
### Python 代码
```python
from pyalink.alink import *

import pandas as pd

useLocalEnv(1)

df_data = pd.DataFrame([
    [1, 1, 0.6],
    [2, 2, 0.8],
    [2, 3, 0.6],
    [4, 1, 0.6],
    [4, 2, 0.3],
    [4, 3, 0.4],
])

data = BatchOperator.fromDataframe(df_data, schemaStr='user bigint, item bigint, rating double')
sdata = StreamOperator.fromDataframe(df_data, schemaStr='user bigint, item bigint, rating double')

als = AlsTrainBatchOp().setUserCol("user").setItemCol("item").setRateCol("rating") \
    .setNumIter(10).setRank(10).setLambda(0.01)

model = als.linkFrom(data)
alsRec = AlsSimilarItemsRecommender().setModelData(model) \
    .setItemCol("item").setRecommCol("rec").setK(1).setReservedCols(["item"])

alsRec.transform(data).print();
```
### Java 代码
```java
import org.apache.flink.types.Row;

import com.alibaba.alink.operator.batch.BatchOperator;
import com.alibaba.alink.operator.batch.recommendation.AlsTrainBatchOp;
import com.alibaba.alink.operator.batch.source.MemSourceBatchOp;
import com.alibaba.alink.operator.stream.StreamOperator;
import com.alibaba.alink.operator.stream.source.MemSourceStreamOp;
import com.alibaba.alink.pipeline.recommendation.AlsSimilarItemsRecommender;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AlsSimilarItemsRecommenderTest {
	@Test
	public void testAlsSimilarItemsRecommender() throws Exception {
		List <Row> df_data = Arrays.asList(
			Row.of(1, 1, 0.6),
			Row.of(2, 2, 0.8),
			Row.of(2, 3, 0.6),
			Row.of(4, 1, 0.6),
			Row.of(4, 2, 0.3),
			Row.of(4, 3, 0.4)
		);
		BatchOperator <?> data = new MemSourceBatchOp(df_data, "user int, item int, rating double");
		StreamOperator <?> sdata = new MemSourceStreamOp(df_data, "user int, item int, rating double");
		BatchOperator <?> als = new AlsTrainBatchOp().setUserCol("user").setItemCol("item").setRateCol("rating")
			.setNumIter(10).setRank(10).setLambda(0.01);
		BatchOperator model = als.linkFrom(data);
		AlsSimilarItemsRecommender alsRec = new AlsSimilarItemsRecommender().setModelData(model)
			.setItemCol("item").setRecommCol("rec").setK(1).setReservedCols("item");
		alsRec.transform(data).print();
	}
}
```

### 运行结果

item| rec
----|-------
1	|{"object":"[3]","score":"[0.8821980357170105]"}
2	|{"object":"[3]","score":"[0.9917739629745483]"}
3	|{"object":"[2]","score":"[0.9917739629745483]"}
1	|{"object":"[3]","score":"[0.8821980357170105]"}
2	|{"object":"[3]","score":"[0.9917739629745483]"}
3	|{"object":"[2]","score":"[0.9917739629745483]"}
