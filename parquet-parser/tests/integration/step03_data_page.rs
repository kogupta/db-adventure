use anyhow::Result;
use parquet::data_type::AsBytes;
use parquet_parser::{file_metadata::read_file_metadata, page::read_page};

use crate::make_parquet_bytes;

#[test]
fn read_one_data_page() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
col1
1
2
3
"#,
        &[],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let offset = column_metadata.data_page_offset as usize;
    let length = column_metadata.total_compressed_size as usize;
    let page_bytes = parquet_data.slice(offset..offset + length);

    let (page, _) = read_page(page_bytes, column_metadata.codec)?;

    // header
    assert_eq!(page.num_values(), 3);
    assert!(!page.is_dictionary());

    // definition levels
    let definition_levels = page.definition_levels();
    assert!(definition_levels.is_some());
    // the definition levels doesn't make sense yet, just ensure you get the bytes correct
    assert_eq!(
        definition_levels.unwrap().as_ref(),
        [2u8, 0u8, 0u8, 0u8, 6u8, 1u8]
    );

    // encoded values
    assert_eq!(
        page.encoded_values(),
        [1i64.as_bytes(), 2i64.as_bytes(), 3i64.as_bytes()].concat()
    );

    Ok(())
}

#[test]
fn remaining_bytes_must_be_correct() -> Result<()> {
    // create a parquet data with two pages
    let parquet_data = make_parquet_bytes(
        r#"
col1
1
2
3
"#,
        &[&["--rows-per-page", "2"]],
    )?;
    let file_metadata = read_file_metadata(parquet_data.clone())?;

    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();
    let offset = column_metadata.data_page_offset as usize;
    let length = column_metadata.total_compressed_size as usize;
    let page_bytes = parquet_data.slice(offset..offset + length);

    // first page
    let (page, remaining_bytes) = read_page(page_bytes, column_metadata.codec)?;
    assert_eq!(page.num_values(), 2);
    assert_eq!(
        page.encoded_values(),
        [1i64.as_bytes(), 2i64.as_bytes()].concat()
    );

    // second page
    let (page, remaining_bytes) = read_page(remaining_bytes, column_metadata.codec)?;
    assert_eq!(page.num_values(), 1);
    assert_eq!(page.encoded_values(), [3i64.as_bytes()].concat());

    // there is no remaining page data!
    assert!(remaining_bytes.is_empty());

    Ok(())
}
