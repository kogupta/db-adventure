use anyhow::Result;
use parquet_parser::{
    decoder::decode_page, file_metadata::read_file_metadata, format::Type, page::read_pages,
};
use polars::prelude::*;

use crate::make_parquet_bytes;

#[test]
fn i32_ok() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
i32
10
20
30
"#,
        &[&["--dtypes", "i32=int32"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;
    let decoded = decode_page(&pages.data_pages[0], Type::INT32, 3)?;
    assert_eq!(
        decoded,
        [
            Scalar::from(10i32),
            Scalar::from(20i32),
            Scalar::from(30i32)
        ]
    );

    Ok(())
}

#[test]
fn i64_ok() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
i64
10
20
30
"#,
        &[&["--dtypes", "i64=int64"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;
    let decoded = decode_page(&pages.data_pages[0], Type::INT64, 3)?;
    assert_eq!(
        decoded,
        [
            Scalar::from(10i64),
            Scalar::from(20i64),
            Scalar::from(30i64)
        ]
    );

    Ok(())
}

#[test]
fn float_ok() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
float
10
20
30
"#,
        &[&["--dtypes", "float=float"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;
    let decoded = decode_page(&pages.data_pages[0], Type::FLOAT, 3)?;
    assert_eq!(
        decoded,
        [
            Scalar::from(10f32),
            Scalar::from(20f32),
            Scalar::from(30f32)
        ]
    );

    Ok(())
}

#[test]
fn double_ok() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
double
10
20
30
"#,
        &[&["--dtypes", "double=double"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;
    let decoded = decode_page(&pages.data_pages[0], Type::DOUBLE, 3)?;
    assert_eq!(
        decoded,
        [
            Scalar::from(10f64),
            Scalar::from(20f64),
            Scalar::from(30f64)
        ]
    );

    Ok(())
}

#[test]
fn string_ok() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
string
one
two
three
"#,
        &[&["--dtypes", "string=string"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;
    let decoded = decode_page(&pages.data_pages[0], Type::BYTE_ARRAY, 3)?;
    assert_eq!(
        decoded,
        [
            Scalar::from(PlSmallStr::from_static("one")),
            Scalar::from(PlSmallStr::from_static("two")),
            Scalar::from(PlSmallStr::from_static("three")),
        ]
    );

    Ok(())
}

#[test]
fn correct_rows_per_page() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
i32
10
20
30
"#,
        &[&["--dtypes", "i32=int32"], &["--rows-per-page", "1"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let pages = read_pages(parquet_data, column_metadata)?;

    let decoded = decode_page(&pages.data_pages[0], Type::INT32, 1)?;
    assert_eq!(decoded, [Scalar::from(10i32),]);

    let decoded = decode_page(&pages.data_pages[1], Type::INT32, 1)?;
    assert_eq!(decoded, [Scalar::from(20i32),]);

    let decoded = decode_page(&pages.data_pages[2], Type::INT32, 1)?;
    assert_eq!(decoded, [Scalar::from(30i32),]);

    Ok(())
}
