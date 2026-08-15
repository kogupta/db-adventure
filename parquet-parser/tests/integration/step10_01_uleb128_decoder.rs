use anyhow::Result;
use bytes::Bytes;
use parquet_parser::decoder::uleb128::uleb128_decode;

#[test]
fn ok() -> Result<()> {
    // Example from wikipedia: https://en.wikipedia.org/wiki/LEB128
    let data = Bytes::from(vec![0xE5, 0x8E, 0x26]);
    let (decoded, remaining) = uleb128_decode(data)?;
    assert_eq!(decoded, 624485u64);
    assert_eq!(remaining.len(), 0);
    Ok(())
}

#[test]
fn data_does_not_contain_lsb_0() -> Result<()> {
    // Example from wikipedia: https://en.wikipedia.org/wiki/LEB128
    let data = Bytes::from(vec![0xE5, 0x8E, 0x8E]);
    assert!(uleb128_decode(data).is_err());
    Ok(())
}
