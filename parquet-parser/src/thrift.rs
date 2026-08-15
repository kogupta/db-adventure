use std::io::Cursor;

use anyhow::Result;
use bytes::Bytes;
use thrift::protocol::{TCompactInputProtocol, TSerializable};

/// Wrapper for reading parquet thrift metadata, returning the parsed data and the remaining Bytes.
pub fn read_thrift_metadata<T: TSerializable>(data: Bytes) -> Result<(T, Bytes)> {
    let mut cursor = Cursor::new(data.as_ref());
    let decoded = {
        let mut protocol = TCompactInputProtocol::new(&mut cursor);
        T::read_from_in_protocol(&mut protocol)?
    };
    let remaining = data.slice(cursor.position() as usize..);

    Ok((decoded, remaining))
}
