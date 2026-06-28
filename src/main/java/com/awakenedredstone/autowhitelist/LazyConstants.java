package com.awakenedredstone.autowhitelist;

import com.awakenedredstone.autowhitelist.network.geyser.GeyserProfileRepository;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public final class LazyConstants {
    public static final Supplier<GeyserProfileRepository> GEYSER_PROFILE_REPOSITORY = Suppliers.memoize(GeyserProfileRepository::new);
}
