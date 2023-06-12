pub type PanicHandlerFn = extern "C" fn(data: *const u8, len: i32) -> !;

// static mut PANIC_HANDLER: Option<PanicHandlerFn> = None;

pub fn set_panic_handler(pfn: PanicHandlerFn) {
    // unsafe {
    //     PANIC_HANDLER = Some(pfn);
    // }
}

// #[panic_handler]
// #[cfg(not(test))]
// fn panic(info: &std::panic::PanicInfo) -> ! {
//     if let Some(handler) = unsafe { PANIC_HANDLER.as_ref() } {
//         signal_panic(info, handler)
//     }

//     std::process::abort();
// }

// #[cfg(not(test))]
// fn signal_panic(info: &std::panic::PanicInfo, handler: &PanicHandlerFn) -> ! {
//     use std::string::String;
//     use std::fmt::Write;

//     let mut message = String::new();
//     write!(&mut message, "{}", info).ok();

//     (*handler)(message.as_ptr(), message.len() as i32)
// }
