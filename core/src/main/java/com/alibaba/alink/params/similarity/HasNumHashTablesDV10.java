package com.alibaba.alink.params.similarity;

import org.apache.flink.ml.api.misc.param.ParamInfo;
import org.apache.flink.ml.api.misc.param.ParamInfoFactory;
import org.apache.flink.ml.api.misc.param.WithParams;

/**
 * Param: number of hash tables.
 */
public interface HasNumHashTablesDV10<T> extends
	WithParams <T> {

	/**
	 * @cn-name 哈希表个数
	 * @cn 哈希表的数目
	 */
	ParamInfo <Integer> NUM_HASH_TABLES = ParamInfoFactory
		.createParamInfo("numHashTables", Integer.class)
		.setDescription("The number of hash tables")
		.setHasDefaultValue(10)
		.setAlias(new String[] {"minHashK"})
		.build();

	default Integer getNumHashTables() {
		return get(NUM_HASH_TABLES);
	}

	default T setNumHashTables(Integer value) {
		return set(NUM_HASH_TABLES, value);
	}
}
