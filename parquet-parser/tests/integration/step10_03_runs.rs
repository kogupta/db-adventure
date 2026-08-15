use anyhow::Result;
use bytes::Bytes;
use parquet::data_type::AsBytes;
use parquet_parser::decoder::rle_bit_packing_hybrid::{RleBitPackedRun, read_rle_bit_packed_runs};

#[test]
fn no_prepended_length() -> Result<()> {
    let data = Bytes::from(
        [
            // first bit-packed run, header = 5, length = 2, num_values = 2 * 8 = 16, needed bits = 16
            0b0000101u8.as_bytes(),
            10u16.as_bytes(),
            // second bit-packed run, header = 9, length = 4, num_values = 4 * 8 = 32, needed bits = 32
            0b0001001u8.as_bytes(),
            10u32.as_bytes(),
            // first rle-run run, header = 4, num_values = 2
            0b0000100u8.as_bytes(),
            10u8.as_bytes(),
            // second rle-run run, header = 8, num_values = 4
            0b0001000u8.as_bytes(),
            10u8.as_bytes(),
        ]
        .concat(),
    );

    let runs = read_rle_bit_packed_runs(data, 1, false)?;

    assert_eq!(
        runs,
        [
            RleBitPackedRun::BitPacked {
                run_len: 16,
                bit_width: 1,
                encoded_values: Bytes::from(10u16.as_bytes()),
            },
            RleBitPackedRun::BitPacked {
                run_len: 32,
                bit_width: 1,
                encoded_values: Bytes::from(10u32.as_bytes()),
            },
            RleBitPackedRun::Rle {
                run_len: 2,
                bit_width: 1,
                encoded_values: Bytes::from(10u8.as_bytes())
            },
            RleBitPackedRun::Rle {
                run_len: 4,
                bit_width: 1,
                encoded_values: Bytes::from(10u8.as_bytes())
            }
        ]
    );

    Ok(())
}

#[test]
fn has_prepended_length() -> Result<()> {
    let data = [
        // first bit-packed run, header = 5, length = 2, num_values = 2 * 8 = 16, needed bits = 16
        0b0000101u8.as_bytes(),
        10u16.as_bytes(),
        // second bit-packed run, header = 9, length = 4, num_values = 4 * 8 = 32, needed bits = 32
        0b0001001u8.as_bytes(),
        10u32.as_bytes(),
        // first rle-run run, header = 4, num_values = 2
        0b0000100u8.as_bytes(),
        10u8.as_bytes(),
        // second rle-run run, header = 8, num_values = 4
        0b0001000u8.as_bytes(),
        10u8.as_bytes(),
    ]
    .concat();

    // prepend the length
    let data = Bytes::from([(data.len() as u32).as_bytes(), &data].concat());
    let runs = read_rle_bit_packed_runs(data, 1, true)?;

    assert_eq!(
        runs,
        [
            RleBitPackedRun::BitPacked {
                run_len: 16,
                bit_width: 1,
                encoded_values: Bytes::from(10u16.as_bytes()),
            },
            RleBitPackedRun::BitPacked {
                run_len: 32,
                bit_width: 1,
                encoded_values: Bytes::from(10u32.as_bytes()),
            },
            RleBitPackedRun::Rle {
                run_len: 2,
                bit_width: 1,
                encoded_values: Bytes::from(10u8.as_bytes())
            },
            RleBitPackedRun::Rle {
                run_len: 4,
                bit_width: 1,
                encoded_values: Bytes::from(10u8.as_bytes())
            }
        ]
    );

    Ok(())
}
