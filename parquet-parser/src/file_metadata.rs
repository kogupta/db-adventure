use crate::format::FileMetaData;
use crate::thrift::read_thrift_metadata;
use anyhow::Result;
use bytes::{Buf, Bytes};

/// Read a parquet file's [`FileMetaData`].
///
/// ```text
/// ...
/// File Metadata
/// 4-byte length in bytes of file metadata (little endian)
/// 4-byte magic number "PAR1"
/// ```
///
/// [file-format]: https://parquet.apache.org/docs/file-format/
#[allow(unused_variables)]
pub fn read_file_metadata(data: Bytes) -> Result<FileMetaData> {
    let from = data.len() - 8;
    let meta_len = (&data[from..from + 4]).get_u32_le() as usize;

    let meta_bytes = data.slice(from - meta_len..from);

    let (meta, _) = read_thrift_metadata::<FileMetaData>(meta_bytes)?;

    Ok(meta)
}
