package team.gothaj.client.api.proxy;

import lombok.AllArgsConstructor;

public record Proxy(String domain, String port, String username, String password, ProxyRepository.Type type) { }
