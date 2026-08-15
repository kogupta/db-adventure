use anyhow::Result;
use bytes::Bytes;
use parquet::data_type::AsBytes;
use parquet_parser::decoder::rle::rle_decode;
use parquet_parser::decoder::rle_bit_packing_hybrid::{
    RleBitPackedRun, rle_bit_packing_hybrid_run_decode,
};
use parquet_parser::format::Type;
use polars::prelude::*;

#[test]
fn rle_decode_true() -> Result<()> {
    let data = Bytes::from(0b00000001u8.as_bytes());
    let scalars = rle_decode(data, Type::BOOLEAN, 1, 3)?;
    assert_eq!(
        scalars,
        [Scalar::from(true), Scalar::from(true), Scalar::from(true),]
    );
    Ok(())
}

#[test]
fn rle_decode_false() -> Result<()> {
    let data = Bytes::from(0b00000010u8.as_bytes());
    let scalars = rle_decode(data, Type::BOOLEAN, 1, 3)?;
    assert_eq!(
        scalars,
        [
            Scalar::from(false),
            Scalar::from(false),
            Scalar::from(false),
        ]
    );
    Ok(())
}

#[test]
fn rle_run() -> Result<()> {
    let run = RleBitPackedRun::Rle {
        run_len: 3,
        bit_width: 1,
        encoded_values: Bytes::from(0b1u8.as_bytes()),
    };

    let scalars = rle_bit_packing_hybrid_run_decode(run, Type::BOOLEAN)?;
    assert_eq!(
        scalars,
        [Scalar::from(true), Scalar::from(true), Scalar::from(true),]
    );
    Ok(())
}

#[test]
fn bit_packed_run() -> Result<()> {
    let run = RleBitPackedRun::BitPacked {
        run_len: 5,
        bit_width: 1,
        encoded_values: Bytes::from(0b10011u8.as_bytes()),
    };

    let scalars = rle_bit_packing_hybrid_run_decode(run, Type::BOOLEAN)?;
    assert_eq!(
        scalars,
        [
            Scalar::from(true),
            Scalar::from(true),
            Scalar::from(false),
            Scalar::from(false),
            Scalar::from(true)
        ]
    );
    Ok(())
}
