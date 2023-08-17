use std::mem::MaybeUninit;

pub struct ArrayDeque<T, const CAPACITY: usize> {
    head: usize,
    tail: usize,
    elements: [MaybeUninit<T>; CAPACITY],
}

impl<T, const CAPACITY: usize> ArrayDeque<T, CAPACITY> {
    pub fn push(&mut self, value: T) {
        self.elements[self.tail] = MaybeUninit::new(value);
        self.tail += 1;
    }

    pub unsafe fn push_conditionally_unchecked(&mut self, value: T, cond: bool) {
        let holder = self.elements.get_unchecked_mut(self.tail);
        *holder = MaybeUninit::new(value);

        self.tail += if cond { 1 } else { 0 };
    }

    pub fn pop(&mut self) -> Option<&T> {
        if self.head == self.tail {
            return None;
        }

        // the get_unchecked should be fine, because if we read past the array, it would've already
        // been a problem when we pushed an element past the array.
        let value = unsafe { MaybeUninit::assume_init_ref(self.elements.get_unchecked(self.head)) };
        self.head += 1;

        Some(value)
    }

    pub fn reset(&mut self) {
        self.head = 0;
        self.tail = 0;
    }

    pub fn is_empty(&self) -> bool {
        self.head == self.tail
    }
}

impl<T, const CAPACITY: usize> Default for ArrayDeque<T, CAPACITY> {
    fn default() -> Self {
        Self {
            head: 0,
            tail: 0,

            // MaybeUninit::uninit_array::<CAPACITY>()
            // https://github.com/rust-lang/rust/issues/96097
            elements: unsafe { MaybeUninit::<[MaybeUninit<T>; CAPACITY]>::uninit().assume_init() },
        }
    }
}
