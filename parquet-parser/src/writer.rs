use std::{collections::HashMap, io::Cursor, sync::Arc};

use anyhow::Result;
use arrow::{
    csv::{ReaderBuilder, reader::Format},
    datatypes::{DataType, Field, Schema},
};
use parquet::{
    arrow::ArrowWriter,
    basic::{Compression, Encoding},
    file::properties::{DEFAULT_DATA_PAGE_ROW_COUNT_LIMIT, WriterProperties},
};

use crate::format::Type;

fn parquet_type_to_arrow_type(parquet_type: Type) -> DataType {
    match parquet_type {
        Type::BOOLEAN => DataType::Boolean,
        Type::INT32 => DataType::Int32,
        Type::INT64 => DataType::Int64,
        Type::FLOAT => DataType::Float32,
        Type::DOUBLE => DataType::Float64,
        Type::BYTE_ARRAY => DataType::Utf8,
        _ => unreachable!(),
    }
}

pub fn write_parquet(
    input: Vec<u8>,
    dictionary_enabled: bool,
    encodings: &[(String, Encoding)],
    compression: Compression,
    rows_per_page: Option<usize>,
    rows_per_group: Option<usize>,
    data_types_override: Option<HashMap<String, Type>>,
) -> Result<Vec<u8>> {
    let mut props = WriterProperties::builder()
        .set_compression(compression)
        .set_dictionary_enabled(dictionary_enabled)
        .set_created_by("Hello parquet!".to_string())
        .set_data_page_row_count_limit(rows_per_page.unwrap_or(DEFAULT_DATA_PAGE_ROW_COUNT_LIMIT))
        .set_write_batch_size(1) // ensure we don't write across page boundary
        .set_max_row_group_row_count(rows_per_group);

    for (column, encoding) in encodings {
        props = props.set_column_encoding(column.clone().into(), *encoding)
    }
    let props = props.build();

    let mut cursor = Cursor::new(input);
    let format = Format::default().with_header(true);
    let (mut schema, _) = format.infer_schema(&mut cursor, None)?;

    if let Some(data_types_override) = data_types_override {
        let fields: Vec<Arc<Field>> = schema
            .fields()
            .iter()
            .map(|field| {
                let field_name = field.name().as_str();
                match data_types_override.get(field_name) {
                    Some(parquet_type) => Arc::new(Field::new(
                        field_name,
                        parquet_type_to_arrow_type(*parquet_type),
                        field.is_nullable(),
                    )),
                    _ => Arc::clone(field),
                }
            })
            .collect();

        schema = Schema::new_with_metadata(fields, schema.metadata().clone());
    }

    let schema = Arc::new(schema);

    cursor.set_position(0);
    let reader = ReaderBuilder::new(Arc::clone(&schema))
        .with_header(true)
        .with_delimiter(b',')
        .build(cursor)?;

    let mut out = Vec::new();
    {
        let mut writer = ArrowWriter::try_new(&mut out, Arc::clone(&schema), Some(props))?;
        for batch in reader {
            let batch = batch?;
            writer.write(&batch)?;
        }
        writer.close()?;
    }

    Ok(out)
}
