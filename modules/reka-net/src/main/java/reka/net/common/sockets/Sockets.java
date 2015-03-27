package reka.net.common.sockets;

import reka.Identity;
import reka.api.IdentityKey;

public class Sockets {
	public static final IdentityKey<Identity> IDENTITY = IdentityKey.named("identity");
	public static final IdentityKey<String> BOO = IdentityKey.named("boo");
}
