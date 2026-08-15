pub mod bit_packed;
pub mod dictionary;
pub mod plain;
pub mod rle;
pub mod rle_bit_packing_hybrid;
pub mod uleb128;

use anyhow::Result;
use polars::prelude::*;

use crate::{
    format::{Encoding, Type},
    page::Page,
};

/// Decode a page into a vector of [`Scalar`] using a correct decoder.
#[allow(unused_variables)]
pub fn decode_page(page: &Page, parquet_type: Type, num_values: usize) -> Result<Vec<Scalar>> {
    match page.encoding() {
        Encoding::PLAIN => todo!("step05: plain decoder"),
        Encoding::RLE => todo!("step10-05: rle bit-packing hybrid decoder"),
        Encoding::RLE_DICTIONARY => todo!("step12-02: dictionary decoder"),
        e => unimplemented!("decode_data_page: unsupported encoding {:?}", e),
    }
}
