use anyhow::Result;
use parquet_parser::file_metadata::read_file_metadata;

use crate::make_parquet_bytes;

#[test]
fn file_metadata() -> Result<()> {
    let parquet_data = make_parquet_bytes(
        r#"
col1,col2
1,one
2,two
3,three
4,four
5,five
"#,
        &[],
    )?;
    let file_metadata = read_file_metadata(parquet_data)?;
    assert_eq!(file_metadata.version, 1);
    assert_eq!(file_metadata.created_by, Some("Hello parquet!".to_string()));
    assert_eq!(file_metadata.num_rows, 5);
    assert_eq!(file_metadata.schema[1].name, "col1");
    assert_eq!(file_metadata.schema[2].name, "col2");

    Ok(())
}
