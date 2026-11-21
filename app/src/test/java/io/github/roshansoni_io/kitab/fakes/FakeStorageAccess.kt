package io.github.roshansoni_io.kitab.fakes

import io.github.roshansoni_io.kitab.core.platform.StorageAccess

class FakeStorageAccess(
    var granted: Boolean = true,
    override val usesLegacyPermission: Boolean = false,
) : StorageAccess {
    override fun hasAccess(): Boolean = granted
}
