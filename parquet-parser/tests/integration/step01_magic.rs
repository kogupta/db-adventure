use bytes::Bytes;
use parquet_parser::magic::ensure_header_footer_magic;

#[test]
fn magic_correct_header_and_footer() {
    let data = Bytes::from("PAR1xyzPAR1");
    assert!(ensure_header_footer_magic(data).is_ok());
}

#[test]
fn magic_missing_both_header_and_footer() {
    let data = Bytes::from("xyz");
    assert!(ensure_header_footer_magic(data).is_err());
}

#[test]
fn magic_missing_header() {
    let data = Bytes::from("xyzPAR1");
    assert!(ensure_header_footer_magic(data).is_err());
}

#[test]
fn magic_missing_footer() {
    let data = Bytes::from("PAR1xyz");
    assert!(ensure_header_footer_magic(data).is_err());
}

#[test]
fn magic_not_enough_bytes() {
    let data = Bytes::from("PAR1");
    assert!(
        ensure_header_footer_magic(data).is_err(),
        "Parquet file must contains both header and footer"
    );
}

#[test]
fn magic_empty_data() {
    let data = Bytes::from("");
    assert!(ensure_header_footer_magic(data).is_err());
}
