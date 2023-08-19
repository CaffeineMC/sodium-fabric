#![allow(dead_code)]

pub mod types {
    use std::marker::PhantomData;

    pub type JEnv = std::ffi::c_void;
    pub type JClass = std::ffi::c_void;

    pub type Jbyte = i8;
    pub type Jshort = i16;
    pub type Jint = i32;
    pub type Jlong = i64;

    pub type Jfloat = f32;
    pub type Jdouble = f64;

    pub type Jchar = u16;

    pub type Jboolean = bool;

    #[repr(transparent)]
    pub struct JPtr<T> {
        addr: Jlong,
        _type: PhantomData<T>,
    }

    impl<T: Sized> JPtr<T> {
        /// SAFETY: The pointer must be of type T
        pub unsafe fn as_ref(&self) -> &T {
            let ptr = self.as_ptr();
            ptr.as_ref().expect("ptr must not be null")
        }

        /// SAFETY: The pointer must be of type T
        pub unsafe fn as_ptr(&self) -> *const T {
            self.addr as *const T
        }
    }

    #[repr(transparent)]
    pub struct JPtrMut<T> {
        addr: Jlong,
        _type: PhantomData<T>,
    }

    impl<T: Sized> JPtrMut<T> {
        /// SAFETY: The pointer must be of type T
        /// SAFETY: The backing memory of the pointer must allow mutating
        /// SAFETY: This consumes the JPtrMut instance to uphold borrow checker rules
        pub unsafe fn into_mut_ref<'a>(self) -> &'a mut T {
            self.into_mut_ptr().as_mut().expect("ptr must not be null")
        }

        /// SAFETY: The pointer must be of type T
        /// SAFETY: The backing memory of the pointer must allow mutating
        /// SAFETY: This consumes the JPtrMut instance to uphold borrow checker rules
        pub unsafe fn into_mut_ptr(self) -> *mut T {
            self.addr as *mut T
        }
    }
}
