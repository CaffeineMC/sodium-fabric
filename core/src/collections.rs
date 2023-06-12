use std::mem::MaybeUninit;

#[derive(Clone)]
pub struct ArrayDeque<T: Copy, const CAPACITY: usize> {
    head: usize,
    tail: usize,
    elements: [MaybeUninit<T>; CAPACITY],
}

impl<T: Copy, const CAPACITY: usize> ArrayDeque<T, CAPACITY> {
    pub fn push(&mut self, value: T) {
        self.elements[self.tail] = MaybeUninit::new(value);
        self.tail += 1;
    }

    pub fn pop(&mut self) -> Option<T> {
        if self.head == self.tail {
            return None;
        }

        let value = unsafe { MaybeUninit::assume_init(self.elements[self.head]) };

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

impl<T: Copy, const CAPACITY: usize> Default for ArrayDeque<T, CAPACITY> {
    fn default() -> Self {
        Self {
            head: 0,
            tail: 0,
            elements: MaybeUninit::uninit_array(),
        }
    }
}
