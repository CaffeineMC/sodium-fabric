# What?

This folder contains a handful of classes which have been placed in the `net.minecraft` package as a *horrible* hack.
The class names in this package have been prefixed with `Sodium_` to avoid name conflicts. There _should_ be no way this
can cause issues, but again, it is obviously a sub-optimal situation.

# Why?

:scream:

The immediate question comes up: Why on earth are you doing this?

The short answer is: Because there's no other way to patch some things in the game.

The longer answer is that there a few situations where we need to either extend types with package-private constructors
or shadow fields which have package-private types. This is not currently possible in Fabric without package-relocation
hacks as demonstrated here.

Unfortunately, there's not really a good solution for this. While we could use ASM to modify the access flags of these
types, it only solves the problem in run-time, not compile-time. It would be necessary to also patch our workspace
sources, and writing that tool is outside the scope of responsibilities I'm willing to take on.

There does exist a fork of the Fabric Loom plugin for Gradle, but it currently does not support the latest version of
the Fabric loader or Mixins 0.8, making it a non-option.

If you have a better solution on how to handle this, please consider opening an issue with details. I'm very receptive
to alternative workarounds.

