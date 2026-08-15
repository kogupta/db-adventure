use anyhow::Result;
use bytes::Bytes;
use parquet::data_type::AsBytes;
use parquet_parser::decoder::rle_bit_packing_hybrid::{RleBitPackedRun, read_rle_bit_packed_run};

#[test]
fn bit_packed_run_width_1() -> Result<()> {
    let data = Bytes::from(
        [
            // first run, header = 5, length = 2, num_values = 2 * 8 = 16, needed bits = 16
            0b0000101u8.as_bytes(),
            10u16.as_bytes(),
            // second run, header = 9, length = 4, num_values = 4 * 8 = 32, needed bits = 32
            0b0001001u8.as_bytes(),
            10u32.as_bytes(),
        ]
        .concat(),
    );

    // first run
    let (run, remaining) = read_rle_bit_packed_run(data, 1)?;
    assert_eq!(
        run,
        RleBitPackedRun::BitPacked {
            run_len: 16,
            bit_width: 1,
            encoded_values: Bytes::from(10u16.as_bytes()),
        }
    );

    // second run
    let (run, remaining) = read_rle_bit_packed_run(remaining, 1)?;
    assert_eq!(
        run,
        RleBitPackedRun::BitPacked {
            run_len: 32,
            bit_width: 1,
            encoded_values: Bytes::from(10u32.as_bytes()),
        }
    );

    assert!(remaining.is_empty());

    Ok(())
}

#[test]
fn bit_packed_run_width_3() -> Result<()> {
    let data = Bytes::from(
        [
            // first run, header = 5, length = 2, num_values = 2 * 8 = 16, needed bits = 48
            0b0000101u8.as_bytes(),
            10u16.as_bytes(),
            20u32.as_bytes(),
            // second run, header = 9, length = 4, num_values = 4 * 8 = 32, needed bits = 96
            0b0001001u8.as_bytes(),
            10u32.as_bytes(),
            20u64.as_bytes(),
        ]
        .concat(),
    );

    // first run
    let (run, remaining) = read_rle_bit_packed_run(data, 3)?;
    assert_eq!(
        run,
        RleBitPackedRun::BitPacked {
            run_len: 16,
            bit_width: 3,
            encoded_values: Bytes::from([10u16.as_bytes(), 20u32.as_bytes()].concat()),
        }
    );

    // second run
    let (run, remaining) = read_rle_bit_packed_run(remaining, 3)?;
    assert_eq!(
        run,
        RleBitPackedRun::BitPacked {
            run_len: 32,
            bit_width: 3,
            encoded_values: Bytes::from([10u32.as_bytes(), 20u64.as_bytes()].concat()),
        }
    );

    assert!(remaining.is_empty());

    Ok(())
}

#[test]
fn rle_run_width_1() -> Result<()> {
    let data = Bytes::from(
        [
            // first run, header = 4, num_values = 2
            0b0000100u8.as_bytes(),
            10u8.as_bytes(),
            // second run, header = 8, num_values = 4
            0b0001000u8.as_bytes(),
            10u8.as_bytes(),
        ]
        .concat(),
    );

    // first run
    let (run, remaining) = read_rle_bit_packed_run(data, 1)?;
    assert_eq!(
        run,
        RleBitPackedRun::Rle {
            run_len: 2,
            bit_width: 1,
            encoded_values: Bytes::from(10u8.as_bytes())
        }
    );

    // second run
    let (run, remaining) = read_rle_bit_packed_run(remaining, 1)?;
    assert_eq!(
        run,
        RleBitPackedRun::Rle {
            run_len: 4,
            bit_width: 1,
            encoded_values: Bytes::from(10u8.as_bytes())
        }
    );

    assert!(remaining.is_empty());

    Ok(())
}

#[test]
fn rle_run_width_10() -> Result<()> {
    let data = Bytes::from(
        [
            // first run, header = 4, num_values = 2, needed bits = ceil16(10) = 16
            0b0000100u8.as_bytes(),
            10u16.as_bytes(),
            // second run, header = 8, num_values = 4, needed bits = ceil16(10) = 16
            0b0001000u8.as_bytes(),
            10u16.as_bytes(),
        ]
        .concat(),
    );

    // first run
    let (run, remaining) = read_rle_bit_packed_run(data, 10)?;
    assert_eq!(
        run,
        RleBitPackedRun::Rle {
            run_len: 2,
            bit_width: 10,
            encoded_values: Bytes::from(10u16.as_bytes())
        }
    );

    // second run
    let (run, remaining) = read_rle_bit_packed_run(remaining, 10)?;
    assert_eq!(
        run,
        RleBitPackedRun::Rle {
            run_len: 4,
            bit_width: 10,
            encoded_values: Bytes::from(10u16.as_bytes())
        }
    );

    assert!(remaining.is_empty());

    Ok(())
}
