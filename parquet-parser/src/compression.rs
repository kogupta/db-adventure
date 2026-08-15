use anyhow::Result;
use bytes::Bytes;

use crate::format::CompressionCodec;

/// Decompress bytes into a new allocated [`Bytes`].
///
/// **SNAPPY**: https://docs.rs/snap/latest/snap/raw/struct.Decoder.html#method.decompress_vec
#[allow(unused_variables)]
pub fn decompress(compressed_data: Bytes, codec: CompressionCodec) -> Result<Bytes> {
    match codec {
        CompressionCodec::UNCOMPRESSED => todo!("step13: implement compression"),
        CompressionCodec::SNAPPY => todo!("step13: implement compression"),
        _ => unimplemented!("Unsupported codec: {}", codec.0),
    }
}
