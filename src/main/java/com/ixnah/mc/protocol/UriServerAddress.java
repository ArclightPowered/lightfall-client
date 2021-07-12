package com.ixnah.mc.protocol;

import net.minecraft.client.multiplayer.ServerAddress;

import java.net.URI;

/**
 * @author 寒兮
 * @version 1.0
 * @date 2021/7/12 10:55
 */
public class UriServerAddress extends ServerAddress {

    private final URI address;

    public UriServerAddress(URI address) {
        super(address.getHost(), CustomProtocol.getServerUriPort(address));
        this.address = address;
    }

    public URI getAddress() {
        return address;
    }
}
