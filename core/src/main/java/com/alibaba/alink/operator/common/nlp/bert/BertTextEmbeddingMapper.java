package com.alibaba.alink.operator.common.nlp.bert;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.ml.api.misc.param.Params;
import org.apache.flink.table.api.TableSchema;

import com.alibaba.alink.common.linalg.tensor.TensorTypes;
import com.alibaba.alink.common.mapper.ComboMapper;
import com.alibaba.alink.common.mapper.Mapper;
import com.alibaba.alink.common.dl.BertResources;
import com.alibaba.alink.operator.common.nlp.bert.tokenizer.EncodingKeys;
import com.alibaba.alink.operator.common.tensorflow.TFSavedModelPredictMapper;
import com.alibaba.alink.operator.common.tensorflow.TFSavedModelPredictRowMapper;
import com.alibaba.alink.operator.common.io.csv.CsvUtil;
import com.alibaba.alink.params.tensorflow.bert.BertTextEmbeddingParams;
import com.alibaba.alink.params.tensorflow.bert.HasMaxSeqLength;
import com.alibaba.alink.params.tensorflow.bert.HasMaxSeqLengthDefaultAsNull;
import com.alibaba.alink.params.tensorflow.bert.HasHiddenStatesCol;
import com.alibaba.alink.params.tensorflow.bert.HasLengthCol;
import com.alibaba.alink.params.tensorflow.savedmodel.HasOutputBatchAxes;
import com.alibaba.alink.params.tensorflow.savedmodel.TFSavedModelPredictParams;
import com.alibaba.alink.params.shared.colname.HasReservedColsDefaultAsNull;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Similar with {@link TFSavedModelPredictMapper}, but tokenization is applied prior to TFSavedModelMapper.
 */
public class BertTextEmbeddingMapper extends ComboMapper {

	private static final String HIDDEN_STATES_COL = "hidden_states";

	private static final String[] MODEL_INPUTS = new String[] {
		EncodingKeys.INPUT_IDS_KEY.label,
		EncodingKeys.TOKEN_TYPE_IDS_KEY.label,
		EncodingKeys.ATTENTION_MASK_KEY.label};
	private static final String[] MODEL_OUTPUTS = new String[] {HIDDEN_STATES_COL};

	public BertTextEmbeddingMapper(TableSchema dataSchema, Params params) {
		super(dataSchema, params);
	}

	@Override
	public List <Mapper> getLoadedMapperList() {
		String[] reservedCols = params.contains(BertTextEmbeddingParams.RESERVED_COLS)
			? params.get(BertTextEmbeddingParams.RESERVED_COLS)
			: getDataSchema().getFieldNames();

		Params tokenizerParams = params.clone();
		tokenizerParams.set(HasReservedColsDefaultAsNull.RESERVED_COLS, reservedCols);
		// By default, BertTokenizerMapper do not pad sequence, MAX_SEQ_LENGTH has to be passed explicitly.
		tokenizerParams.set(
			HasMaxSeqLengthDefaultAsNull.MAX_SEQ_LENGTH,
			params.get(HasMaxSeqLength.MAX_SEQ_LENGTH)
		);
		BertTokenizerMapper tokenizerMapper = new BertTokenizerMapper(getDataSchema(), tokenizerParams);

		Params tfParams = params.clone();
		if (!tfParams.contains(TFSavedModelPredictParams.MODEL_PATH)) {
			tfParams.set(TFSavedModelPredictParams.MODEL_PATH,
				BertResources.getBertSavedModel(tfParams.get(BertTextEmbeddingParams.BERT_MODEL_NAME)));
		}
		tfParams.set(TFSavedModelPredictParams.SELECTED_COLS,
			Arrays.stream(MODEL_INPUTS).map(BertTokenizerMapper::prependPrefix).toArray(String[]::new));
		tfParams.set(TFSavedModelPredictParams.INPUT_SIGNATURE_DEFS, MODEL_INPUTS);
		tfParams.set(TFSavedModelPredictParams.OUTPUT_SCHEMA_STR,
			CsvUtil.schema2SchemaStr(
				TableSchema.builder()
					.field(tokenizerMapper.prependPrefix(HIDDEN_STATES_COL), TensorTypes.FLOAT_TENSOR)
					.build()));
		tfParams.set(TFSavedModelPredictParams.OUTPUT_SIGNATURE_DEFS, MODEL_OUTPUTS);
		tfParams.set(TFSavedModelPredictParams.RESERVED_COLS,
			ArrayUtils.add(reservedCols, tokenizerMapper.prependPrefix(EncodingKeys.LENGTH_KEY.label)));
		tfParams.set(HasOutputBatchAxes.OUTPUT_BATCH_AXES, new int[]{1});
		TFSavedModelPredictRowMapper tfMapper = new TFSavedModelPredictRowMapper(tokenizerMapper.getOutputSchema(),
			tfParams);

		Params extractorParams = params.clone();
		extractorParams.set(HasHiddenStatesCol.HIDDEN_STATES_COL, tokenizerMapper.prependPrefix(HIDDEN_STATES_COL));
		extractorParams.set(HasLengthCol.LENGTH_COL, tokenizerMapper.prependPrefix(EncodingKeys.LENGTH_KEY.label));
		extractorParams.set(HasReservedColsDefaultAsNull.RESERVED_COLS, reservedCols);
		BertEmbeddingExtractorMapper extractorMapper = new BertEmbeddingExtractorMapper(tfMapper.getOutputSchema(),
			extractorParams);

		return Arrays.asList(tokenizerMapper, tfMapper, extractorMapper);
	}

	@Override
	protected Tuple4 <String[], String[], TypeInformation <?>[], String[]> prepareIoSchema(TableSchema dataSchema,
																						   Params params) {
		String textCol = params.get(BertTextEmbeddingParams.SELECTED_COL);
		String outputCol = params.get(BertTextEmbeddingParams.OUTPUT_COL);
		String[] reservedCols = params.get(BertTextEmbeddingParams.RESERVED_COLS);

		return Tuple4.of(
			new String[] {textCol},
			new String[] {outputCol},
			new TypeInformation <?>[] {Types.STRING},
			reservedCols
		);
	}
}
