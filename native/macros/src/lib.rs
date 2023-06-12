#![feature(proc_macro_diagnostic)]
extern crate proc_macro;

use proc_macro::TokenStream;
use proc_macro2::Ident;

use quote::ToTokens;
use syn::{parse_quote, Item, ItemFn, ItemMod};

#[proc_macro_attribute]
pub fn jni_export(args: TokenStream, input: TokenStream) -> TokenStream {
    jni_export_internal(args, input)
}

fn jni_export_internal(args: TokenStream, input: TokenStream) -> TokenStream {
    match syn::parse::<ItemFn>(input.clone()) {
        Ok(mut function) => {
            let mut func_name = String::from("Java_");
            func_name.push_str(&*args.to_string().replace(".", "_"));

            function.attrs.push(parse_quote! {
                #[allow(non_snake_case)]
            });
            function.attrs.push(parse_quote! {
                #[no_mangle]
            });

            function.sig.abi.replace(parse_quote! {
                extern "C"
            });

            function.sig.inputs.insert(
                0,
                parse_quote! {
                    _: *mut crate::jni::types::JClass
                },
            );

            function.sig.inputs.insert(
                0,
                parse_quote! {
                    _: *mut crate::jni::types::JEnv
                },
            );

            // if the name is invalid, this is a big uh-oh. don't be invalid here.
            function.sig.ident = syn::parse_str::<Ident>(&*func_name).unwrap();

            function.to_token_stream().into()
        }
        Err(_) => match syn::parse::<ItemMod>(input.clone()) {
            Ok(ref mut module) => {
                for item in &mut (module.content.as_mut().unwrap().1) {
                    if let Item::Fn(function) = item {
                        let new_fn_attrib = format!("{}.{}", args, (*function).sig.ident);

                        *function = syn::parse::<ItemFn>(jni_export_internal(
                            new_fn_attrib.parse().unwrap(),
                            function.to_token_stream().into(),
                        ))
                        .unwrap();
                    }
                }

                module.to_token_stream().into()
            }
            Err(err) => TokenStream::from(err.to_compile_error()),
        },
    }
}
