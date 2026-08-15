use anyhow::Result;
use parquet::data_type::AsBytes;
use parquet_parser::{
    file_metadata::read_file_metadata, nulls::decode_definition_levels, page::read_pages,
};

use crate::make_parquet_bytes;

#[test]
fn data() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
my_col
one
two
""
""
three
"#,
        &[],
    )?;

    let file_metadata = read_file_metadata(parquet_data.clone())?;
    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();

    let pages = read_pages(parquet_data.clone(), column_metadata)?;

    // definition includes: length + rle-hybrid encoded data
    assert_eq!(
        pages.data_pages[0].definition_levels().unwrap(),
        [
            // need 2 bytes for rle-hybrid encoded data
            2u32.as_bytes(),
            // the first byte is the header: this is 1 byte bit packed => header = 3
            3u8.as_bytes(),
            // the second byte is the data: the nulls map
            // [1, 0, 0, 1, 1]
            0b10011u8.as_bytes()
        ]
        .concat()
    );

    Ok(())
}

#[test]
fn decode() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
my_col
one
two
""
""
three
"#,
        &[],
    )?;

    let file_metadata = read_file_metadata(parquet_data.clone())?;
    let column_metadata = file_metadata.row_groups[0].columns[0]
        .meta_data
        .as_ref()
        .unwrap();

    let pages = read_pages(parquet_data.clone(), column_metadata)?;
    assert_eq!(
        decode_definition_levels(&pages.data_pages[0])?,
        [true, true, false, false, true]
    );

    Ok(())
}
