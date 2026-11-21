# Reader rendering geometry

This package contains calculations that connect PDF page coordinates to the displayed page: fitted page size, visible regions, render specifications, coordinate mapping, and link hit-testing.

Keep these calculations independent of screen composition where practical. Reader UI can use them to convert taps/selections and decide which page region needs a higher-resolution render.
